/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright by KNIME AG, Zurich, Switzerland
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.com
 * email: contact@knime.com
 * ---------------------------------------------------------------------
 *
 * History
 *   24 Jun 2022 (alexander): created
 */
package org.knime.json.node.container.output.raw;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Optional;
import java.util.function.Supplier;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.StringValue;
import org.knime.core.data.blob.BinaryObjectDataValue;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnName;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.dialog.ExternalNodeData;
import org.knime.core.node.dialog.ExternalNodeData.ExternalNodeDataBuilder;
import org.knime.core.node.dialog.OutputNode;
import org.knime.core.node.port.PortType;
import org.knime.core.util.FileUtil;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.FSFiles;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.connections.RelativeTo;
import org.knime.filehandling.core.connections.location.FSPathProviderFactory;
import org.knime.filehandling.core.connections.uriexport.URIExporterIDs;
import org.knime.filehandling.core.connections.uriexport.noconfig.NoConfigURIExporterFactory;

/**
 * Node model for the Container Output (Raw HTTP) node.
 *
 * @author Alexander Fillbrunn, KNIME GmbH, Konstanz, Germany
 */
final class RawHTTPOutputNodeModel extends NodeModel implements OutputNode {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(RawHTTPOutputNodeModel.class);

    private static final String CFG_STATUS_CODE = "statusCode";

    private static final String CFG_BODY_COLUMN = "bodyColumn";

    private static final String PARAMETER_NAME = "raw-http-output";
    private URI m_resourceURI;
    private boolean m_isDataURI = false;
    private JsonObject m_json;

    static final String TEMP_FILE_PREFIX = "knimetemp-";

    private static final String URL_THIS_WORKFLOW_DATA = RelativeTo.WORKFLOW_DATA.getSettingsValue();

    RawHTTPOutputNodeModel() {
        super(new PortType[]{BufferedDataTable.TYPE_OPTIONAL, BufferedDataTable.TYPE_OPTIONAL}, new PortType[0]);
    }

    static SettingsModelInteger createStatusCodeSettingsModel() {
        return new SettingsModelInteger(CFG_STATUS_CODE, 200);
    }

    static SettingsModelColumnName createBodyColumnSettingsModel() {
        return new SettingsModelColumnName(CFG_BODY_COLUMN, null);
    }

    private SettingsModelInteger m_statusCode = createStatusCodeSettingsModel();

    private SettingsModelColumnName m_bodyColumn = createBodyColumnSettingsModel();

    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        return new DataTableSpec[0];
    }

    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
        final ExecutionContext exec) throws Exception {

        var headerBuilder = Json.createObjectBuilder();

        var mimetype = "application/octet-stream";
        // Read headers from the second table and remember the content-type
        BufferedDataTable headerTable = inData[1];
        if (headerTable != null) {
            try (CloseableRowIterator headerIter = headerTable.iterator()) {
                // We need one row
                if (headerIter.hasNext()) {
                    mimetype = buildHeaders(headerBuilder, headerTable, headerIter.next());
                }
            }
        } else {
            headerBuilder.add("content-type", mimetype);
        }

        // Read the binary data from the first row in the first binary column
        BufferedDataTable dataTable = inData[0];
        if (dataTable != null) {
            var bodyColumnIndex = dataTable.getDataTableSpec().findColumnIndex(m_bodyColumn.getColumnName());
            if (bodyColumnIndex >= 0) {
                buildBody(dataTable, bodyColumnIndex, mimetype);
            }
        } else {
            // if no body was specified via an input table, we use an empty body
            var emptyBody = "{}".getBytes(StandardCharsets.UTF_8);
            createOutData(() -> new ByteArrayInputStream(emptyBody), emptyBody.length, mimetype);
        }

        JsonObjectBuilder mainBuilder = Json.createObjectBuilder();
        mainBuilder.add("status_code", m_statusCode.getIntValue());

        mainBuilder.add("headers", headerBuilder);
        m_json = mainBuilder.build();

        return new BufferedDataTable[0];
    }

    /**
     * Builds the headers out of the single, inputed DataRow.
     * If a content-type attribute is not detected, the default content-type
     * will be set. Returns the final, used type as String.
     *
     * @param headerBuilder
     * @param headerTable
     * @param row
     */
    private static String buildHeaders(final JsonObjectBuilder headerBuilder, final BufferedDataTable headerTable,
        final DataRow row) {
        var headerInSpec = headerTable.getDataTableSpec();
        var mimetype = "application/octet-stream";
        var hasContentType = false;

        // Each column is a header
        for (var i = 0; i < headerInSpec.getNumColumns(); i++) {
            DataColumnSpec cspec = headerInSpec.getColumnSpec(i);
            if (cspec.getType().isCompatible(StringValue.class)) {
                String name = cspec.getName().toLowerCase();
                var value = ((StringValue)row.getCell(i)).getStringValue();
                if (name.equals("content-type")) {
                    mimetype = value;
                    hasContentType = true;
                }
                headerBuilder.add(name, value);
            }
        }

        // If the user did not specify a content-type header, we use a default one
        if (!hasContentType) {
            headerBuilder.add("content-type", mimetype);
        }
        return mimetype;
    }

    /**
     * Builds the response body out of the first DataRow.
     * Body can either be built from a string or binary value.
     *
     * @param dataTable the input dataTable
     * @param index body column index which holds the body content
     * @param mimetype the MimeType
     * @throws IOException
     * @throws InvalidSettingsException
     */
    private void buildBody(final BufferedDataTable dataTable, final int index, final String mimetype)
        throws IOException, InvalidSettingsException {
        var bodySpec = dataTable.getDataTableSpec();
        var isBinaryColumn = bodySpec.getColumnSpec(index).getType().isCompatible(BinaryObjectDataValue.class);
        try (CloseableRowIterator dataIter = dataTable.iterator()) {
            if (dataIter.hasNext()) {
                var row = dataIter.next();

                if (isBinaryColumn) {
                    var cell = (BinaryObjectDataValue)row.getCell(index);
                    createOutData(cell::openInputStream, cell.length(), mimetype);
                } else {
                    var s = ((StringValue)row.getCell(index)).getStringValue();
                    byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
                    createOutData(() -> new ByteArrayInputStream(bytes), bytes.length, mimetype);
                }
            }
        }
    }

    /**
     * Retrieves the actual data from streams. For small files (<1MB), a data URI is used
     * and for larger inputs a temp file URI is used.
     *
     * @param streamSupplier
     * @param size
     * @param mimetype
     * @throws IOException
     * @throws InvalidSettingsException
     */
    private void createOutData(final IOSupplier<InputStream> streamSupplier, final long size, final String mimetype)
        throws IOException, InvalidSettingsException {

        try (var stream = streamSupplier.getWithException()) {
            m_isDataURI = size <= 1024 * 1024; // 1MB

            if (m_isDataURI) {
                m_resourceURI = createDataURI(stream, mimetype, (int)size);
            } else {
                m_resourceURI = createTmpFileURI(stream);
            }
        }
    }

    @Override
    protected void reset() {
        m_json = null;
        cleanup();
        m_resourceURI = null;
    }

    @Override
    protected void onDispose() {
        super.onDispose();
        cleanup();
    }

    private void cleanup() {
        if(!m_isDataURI && m_resourceURI != null) {
            deleteAsync(Path.of(m_resourceURI));
        }
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_statusCode.saveSettingsTo(settings);
        m_bodyColumn.saveSettingsTo(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_statusCode.validateSettings(settings);
        m_bodyColumn.validateSettings(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_statusCode.loadSettingsFrom(settings);
        m_bodyColumn.loadSettingsFrom(settings);
    }

    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
        // no op
    }

    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
        // no op
    }

    /**
     * Sets up a location where the temp file can be placed.
     *
     * @return FSLocation for the temp file.
     * @throws InvalidSettingsException
     */
    private static FSLocation getTempLocation() throws InvalidSettingsException {
        final var reativeType = URL_THIS_WORKFLOW_DATA;
        final var relativeLocation = "";

        final var entrypoint = new FSLocation(FSCategory.RELATIVE.toString(), reativeType, relativeLocation);

        try (final var factory = FSPathProviderFactory.newFactory(Optional.empty(), entrypoint);
                final var provider = factory.create(entrypoint)) {

            // find dir and create it if necessary
            final var dirPath = provider.getPath();
            final var tempDirPath = FSFiles.createRandomizedDirectory(dirPath, TEMP_FILE_PREFIX, "");
            return new FSLocation(entrypoint.getFileSystemCategory(), reativeType,
                Paths.get(entrypoint.getPath(), tempDirPath.getFileName().toString()).toString());
        } catch (IOException e) {
            throw new InvalidSettingsException("Could not create temp file: " + e.getMessage(), e);
        }
    }

    /**
     * Turns the data into a base64 string and passes it to the outside as data URI
     *
     * @param stream
     * @param mimetype
     * @param size
     * @return
     * @throws IOException
     */
    private static URI createDataURI(final InputStream stream, final String mimetype, final int size) throws IOException {
        var bytes = new byte[size];
        stream.read(bytes, 0, bytes.length);

        var b64Str = Base64.getEncoder().encodeToString(bytes);
        var uriStr = "data:" + mimetype + ";charset=utf8;base64," + b64Str;
        return URI.create(uriStr);
    }

    /**
     * Encodes the data in binary and passes it to the outside as a temp file URI
     *
     * @param stream
     * @return
     * @throws InvalidSettingsException
     */
    private static URI createTmpFileURI(final InputStream stream)
        throws InvalidSettingsException {
        final var tempDir = getTempLocation();

        try (final var factory = FSPathProviderFactory.newFactory(Optional.empty(), tempDir);
                final var provider = factory.create(tempDir)) {

            // Create a temporary file that can be passed on to the server as response body
            final var targetLocation = provider.getPath().resolve(new String[] {"raw-http-output.bin"});

            try(var fos = FSFiles.newOutputStream(targetLocation)) {
                stream.transferTo(fos);
            }

            // We need an absolute URI to the file to give to the outside
            @SuppressWarnings("resource")
            var exporter = ((NoConfigURIExporterFactory)provider.getFSConnection().getURIExporterFactory(URIExporterIDs.LEGACY_KNIME_URL))
            .getExporter();

            return FileUtil.resolveToPath(FileUtil.toURL(exporter.toUri(targetLocation).toString())).toUri();

        } catch (IOException | URISyntaxException e) {
            throw new InvalidSettingsException("Could not write file:" + e.getMessage(), e);
        }
    }

    @Override
    public ExternalNodeData getExternalOutput() {
        ExternalNodeDataBuilder builder = ExternalNodeData.builder(PARAMETER_NAME);
        if (m_resourceURI != null) {
            builder.resource(m_resourceURI);
        }
        if (m_json != null) {
            builder.jsonValue(m_json);
        }

        return builder.build();
    }

    @Override
    public boolean isUseAlwaysFullyQualifiedParameterName() {
        return false;
    }

    /**
     * Deletes a file asynchronously. Used for cleaning up the temporary file.
     *
     * @param location file path
     */
    private static void deleteAsync(final Path location) {
        var r = org.knime.core.util.ThreadUtils.runnableWithContext(() -> {
            try {
                if (Files.exists(location)) {
                    Files.delete(location);
                }
            } catch (IOException e) {
                LOGGER.warn(String.format("Could not clean up local file \"%s\": %s", location, e.getMessage()), e);
            }
        });
        KNIMEConstants.GLOBAL_THREAD_POOL.enqueue(r);
    }

    /**
     * Convenience interface that equals the {@link Supplier} interface,
     * except for the possibility of an IOException being thrown at the get method.
     *
     * @author Leon Wenzler
     */
    @FunctionalInterface
    private interface IOSupplier<T> {
        public abstract T getWithException() throws IOException;
    }
}
