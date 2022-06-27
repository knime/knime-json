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
 *   Created on Feb 15, 2015 by wiswedel
 */
package org.knime.json.node.container.output.raw;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Optional;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.StringValue;
import org.knime.core.data.blob.BinaryObjectDataCell;
import org.knime.core.data.blob.BinaryObjectDataValue;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnName;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.dialog.ExternalNodeData;
import org.knime.core.node.dialog.ExternalNodeData.ExternalNodeDataBuilder;
import org.knime.core.node.dialog.OutputNode;
import org.knime.core.util.FileUtil;
import org.knime.core.util.ThreadUtils.ThreadWithContext;
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

    private static NodeLogger logger = NodeLogger.getLogger(RawHTTPOutputNodeModel.class);

    private static final String CFG_STATUS_CODE = "statusCode";

    private static final String CFG_BODY_COLUMN = "bodyColumn";

    private static final String PARAMETER_NAME = "raw-http-output";
    private URI m_resourceURI;
    private boolean m_isDataURI = false;
    private JsonObject m_json;

    static final String TEMP_FILE_PREFIX = "knimetemp-";

    private static final String URL_THIS_WORKFLOW_DATA = RelativeTo.WORKFLOW_DATA.getSettingsValue();

    RawHTTPOutputNodeModel() {
        super(2, 0);
    }

    static SettingsModelInteger createStatusCodeSettingsModel() {
        return new SettingsModelInteger(CFG_STATUS_CODE, 200);
    }

    static SettingsModelColumnName createBodyColumnSettingsModel() {
        return new SettingsModelColumnName(CFG_BODY_COLUMN, null);
    }

    private SettingsModelInteger m_statusCode = createStatusCodeSettingsModel();

    private SettingsModelColumnName m_bodyColumn = createBodyColumnSettingsModel();

    /** {@inheritDoc} */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {

        DataTableSpec bodySpec = inSpecs[0];
        // Auto-guess body column when non is set. We prefer a binary column.
        if (m_bodyColumn.getColumnName() == null) {
            int idx = findBinaryColumnIndex(bodySpec);
            if (idx < 0) {
                idx = findStringColumnIndex(bodySpec);
                if (idx < 0) {
                    throw new InvalidSettingsException("No binary or string column in input table");
                }
            }
            String col = bodySpec.getColumnSpec(idx).getName();
            setWarningMessage("No body column configured. Falling back to \"" + col + "\"");
            m_bodyColumn.setStringValue(col);
        }
        return new DataTableSpec[0];
    }

    /** {@inheritDoc} */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
        final ExecutionContext exec) throws Exception {

        JsonObjectBuilder headerBuilder = Json.createObjectBuilder();

        String mimetype = "application/octet-stream";
        boolean hasContentType = false;
        // Read headers from the second table and remember the content-type
        BufferedDataTable headerTable = inData[1];
        CloseableRowIterator headerIter = headerTable.iterator();
        // We need one row
        if (headerIter.hasNext()) {
            DataTableSpec headerInSpec = headerTable.getDataTableSpec();
            DataRow row = headerIter.next();
            // Each column is a header
            for (int i = 0; i < headerInSpec.getNumColumns(); i++) {
                DataColumnSpec cspec = headerInSpec.getColumnSpec(i);
                if (cspec.getType().isCompatible(StringValue.class)) {
                    String name = cspec.getName().toLowerCase();
                    String value = ((StringValue)row.getCell(i)).getStringValue();
                    if (name.equals("content-type")) {
                        mimetype = value;
                        hasContentType = true;
                    }
                    headerBuilder.add(name, value);
                }
            }
        }
        // If the user did not specify a content-type header, we use a default one
        if (!hasContentType) {
            headerBuilder.add("content-type", mimetype);
        }

        // Read the binary data from the first row in the first binary column
        BufferedDataTable dataTable = inData[0];
        DataTableSpec bodySpec = dataTable.getDataTableSpec();
        int bodyColumnIndex = bodySpec.findColumnIndex(m_bodyColumn.getColumnName());
        boolean isBinaryColumn = bodySpec.getColumnSpec(bodyColumnIndex).getType().isCompatible(BinaryObjectDataValue.class);
        CloseableRowIterator dataIter = dataTable.iterator();
        if (dataIter.hasNext()) {
            DataRow row = dataIter.next();
            InputStream stream = null;
            long size;
            try {
                if (isBinaryColumn) {
                    BinaryObjectDataCell cell = (BinaryObjectDataCell)row.getCell(bodyColumnIndex);
                    stream = cell.openInputStream();
                    size = cell.length();
                } else {
                    String s = ((StringValue)row.getCell(bodyColumnIndex)).getStringValue();
                    byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
                    stream = new ByteArrayInputStream(bytes);
                    size = bytes.length;
                }
                m_isDataURI = size <= 1024*1024; // 1MB

                if (m_isDataURI) {
                    m_resourceURI = createDataURI(stream, mimetype, (int)size);
                } else {
                    m_resourceURI = createTmpFileURI(stream);
                }
            } finally {
                if (stream != null) {
                    stream.close();
                }
            }

        }

        JsonObjectBuilder mainBuilder = Json.createObjectBuilder();
        mainBuilder.add("status_code", m_statusCode.getIntValue());

        mainBuilder.add("headers", headerBuilder);
        m_json = mainBuilder.build();

        return new BufferedDataTable[0];
    }

    private static int findBinaryColumnIndex(final DataTableSpec spec) {
        for (int i = 0; i < spec.getNumColumns(); i++) {
            DataColumnSpec cspec = spec.getColumnSpec(i);
            if (cspec.getType().isCompatible(BinaryObjectDataValue.class)) {
                return i;
            }
        }
        return -1;
    }

    private static int findStringColumnIndex(final DataTableSpec spec) {
        for (int i = 0; i < spec.getNumColumns(); i++) {
            DataColumnSpec cspec = spec.getColumnSpec(i);
            if (cspec.getType().isCompatible(StringValue.class)) {
                return i;
            }
        }
        return -1;
    }

    /** {@inheritDoc} */
    @Override
    protected void reset() {
        m_json = null;
        cleanup();
        m_resourceURI = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onDispose() {
        super.onDispose();
        cleanup();
    }

    private void cleanup() {
        if(!m_isDataURI && m_resourceURI != null) {
            Deleter.runNew(Path.of(m_resourceURI));
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_statusCode.saveSettingsTo(settings);
        m_bodyColumn.saveSettingsTo(settings);
    }

    /** {@inheritDoc} */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_statusCode.validateSettings(settings);
        m_bodyColumn.validateSettings(settings);
    }

    /** {@inheritDoc} */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_statusCode.loadSettingsFrom(settings);
        m_bodyColumn.loadSettingsFrom(settings);
    }

    /** {@inheritDoc} */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
        // no op
    }

    /** {@inheritDoc} */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
        // no op
    }

    private static FSLocation getTempLocation() throws InvalidSettingsException {

        final String reativeType = URL_THIS_WORKFLOW_DATA;
        final String relativeLocation = "";

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

    // Turns the data into a base64 string and passes it to the outside as data URI
    private static URI createDataURI(final InputStream stream, final String mimetype, final int size) throws IOException {
        byte[] bytes = new byte[size];
        stream.read(bytes, 0, bytes.length);

        return createDataURI(bytes, mimetype);
    }

    private static URI createDataURI(final byte[] data, final String mimetype) throws IOException {
        String b64Str = Base64.getEncoder().encodeToString(data);
        String uriStr = "data:" + mimetype + ";charset=utf8;base64," + b64Str;
        return URI.create(uriStr);
    }

    private static URI createTmpFileURI(final InputStream stream)
        throws InvalidSettingsException {
        final var tempDir = getTempLocation();

        try (final var factory = FSPathProviderFactory.newFactory(Optional.empty(), tempDir);
                final var provider = factory.create(tempDir)) {

            // Create a temporary file that can be passed on to the server as response body
            final var targetLocation = provider.getPath().resolve(new String[] {"raw-http-output.bin"});

            try(OutputStream fos = FSFiles.newOutputStream(targetLocation)) {
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

    /**
     * {@inheritDoc}
     */
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
        return true;
    }

    private static final class Deleter extends ThreadWithContext {
        private final Path m_toDelete;

        private Deleter(final Path toDelete) {
            m_toDelete = toDelete;
        }

        @Override
        protected void runWithContext() {
            deleteLocalFile(m_toDelete);
        }

        private static void deleteLocalFile(final Path location) {
            try {
                if (Files.exists(location)) {
                    Files.delete(location);
                }
            } catch (IOException e) {
                throw new IllegalStateException("Could not clean up local file!", e);
            }
        }

        static void runNew(final Path location) {
            new Deleter(location).start();
        }
    }
}
