/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   24 Jun 2022 (alexander): created
 */
package org.knime.json.node.container.input.raw;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Base64;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.RowKey;
import org.knime.core.data.blob.BinaryObjectCellFactory;
import org.knime.core.data.blob.BinaryObjectDataCell;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.util.CancellableReportingInputStream;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.core.node.dialog.ExternalNodeData;
import org.knime.core.node.dialog.InputNode;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.util.CheckUtils;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.connections.FSFiles;
import org.knime.filehandling.core.defaultnodesettings.EnumConfig;
import org.knime.filehandling.core.defaultnodesettings.ExceptionUtil;
import org.knime.filehandling.core.defaultnodesettings.FileSystemHelper;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.SettingsModelReaderFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filesystemchooser.FixedPortsConfiguration;
import org.knime.filehandling.core.defaultnodesettings.filtermode.SettingsModelFilterMode.FilterMode;

import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;

/**
 * This is the model implementation of the Container Input (Raw HTTP) node.
 *
 * @author Alexander Fillbrunn, KNIME GmbH, Konstanz, Germany
 */
final class RawHTTPInputNodeModel extends NodeModel implements InputNode {

    private static final String PARAM_NAME = "raw-http-input";
    private static final String SELECTOR_NAME = "file-selection";

    private RawHTTPInputNodeConfiguration m_configuration = new RawHTTPInputNodeConfiguration();

    private final SettingsModelReaderFileChooser m_bodyFileModel;

    // internal state: resource URI, headers, and query parameters received from server
    private URI m_resourceURI;
    private JsonObject m_headers;
    private JsonObject m_queryParams;

    // Regex to use for a data URI and extracting the base64 data
    private static final String DATA_URI_REGEX = "^data:(?<mediatype>[^;,]+(?:;charset=[^;,]+)?)?(?:;(?<encoding>[^,]+))?,(?<data>.*)$";
    private static final Pattern DATA_URI_PATTERN = Pattern.compile(DATA_URI_REGEX);

    /**
     * Constructor for the node model.
     */
    RawHTTPInputNodeModel() {
        super(0, 3);

        m_bodyFileModel = createDefaultFileChooserModel();
    }

    /**
     * Constructor if a ports configuration was specified, for connecting to a file system.
     * @param portsConfig
     */
    protected RawHTTPInputNodeModel(final PortsConfiguration portsConfig) {
        super(portsConfig.getInputPorts(),
            new PortType[]{BufferedDataTable.TYPE, BufferedDataTable.TYPE, BufferedDataTable.TYPE});

        m_bodyFileModel = createFileChooserModel(portsConfig);
    }

    /**
     * Creates a default settings model with an empty specific port configuration.
     *
     * @return default SettingsModelReaderFileChooser
     */
    static SettingsModelReaderFileChooser createDefaultFileChooserModel() {
        return createFileChooserModel(new FixedPortsConfiguration.FixedPortsConfigurationBuilder().build());
    }

    /**
     * Creates a settings model, configured with the specified port configuration.
     *
     * @param portsConfig PortsConfiguration
     * @return configured SettingsModelReaderFileChooser
     */
    static SettingsModelReaderFileChooser createFileChooserModel(final PortsConfiguration portsConfig) {
        return new SettingsModelReaderFileChooser(SELECTOR_NAME, portsConfig, RawHTTPInputNodeFactory.FS_CONNECT_GRP_ID,
            EnumConfig.create(FilterMode.FILE));
    }

    @Override
    protected BufferedDataTable[] execute(final PortObject[] inObjects, final ExecutionContext exec)
        throws Exception {

        DataTableSpec bodySpec = createBodySpec();

        // -- body table as binary object cell --
        final BufferedDataContainer bodyContainer = exec.createDataContainer(bodySpec);
        DataCell binaryData;
        // No external value: use config
        if (m_resourceURI == null) {
            // if a body file location has been specified, retrieve its content
            var bodyData = new byte[0];
            if (m_bodyFileModel.isEnabled() && m_bodyFileModel.getLocation() != null) {
                bodyData = retrieveBodyFileContent(m_bodyFileModel, exec);
            }
            binaryData = new BinaryObjectCellFactory().create(bodyData);
        } else {
            var m = DATA_URI_PATTERN.matcher(m_resourceURI.toString());
            if (m.matches()) {
                String data = m.group("data");
                binaryData = new BinaryObjectCellFactory().create(Base64.getDecoder().decode(data));
            } else {
                try (var fis = new FileInputStream(new File(m_resourceURI))) {
                    binaryData = new BinaryObjectCellFactory().create(fis);
                }
            }
        }
        bodyContainer.addRowToTable(new DefaultRow(RowKey.createRowKey(0L), binaryData));
        bodyContainer.close();

        // -- header table as a single row, one column for each attribute --
        BufferedDataContainer headerContainer;
        if (m_headers != null) {
            DataTableSpec headerSpec = createSpecFromJsonObject(m_headers, true);
            headerContainer = exec.createDataContainer(headerSpec);
            headerContainer.addRowToTable(jsonToRow(m_headers));
        } else {
            DataTableSpec headerSpec = createHeaderSpec();
            headerContainer = exec.createDataContainer(headerSpec);
            headerContainer.addRowToTable(mapToRow(m_configuration.getHeaders()));
        }
        headerContainer.close();

        // -- query parameter table as a single row, one column for each attribute --
        BufferedDataContainer qpContainer;
        if (m_queryParams != null) {
            DataTableSpec qpSpec = createSpecFromJsonObject(m_queryParams, false);
            qpContainer = exec.createDataContainer(qpSpec);
            qpContainer.addRowToTable(jsonToRow(m_queryParams));
        } else {
            DataTableSpec qpSpec = createQueryParamSpec();
            qpContainer = exec.createDataContainer(qpSpec);
            qpContainer.addRowToTable(mapToRow(m_configuration.getQueryParams()));
        }
        qpContainer.close();

        return new BufferedDataTable[]{bodyContainer.getTable(), headerContainer.getTable(), qpContainer.getTable()};
    }

    /**
     * Retrieves the body's content from the settings model.
     * The settings model contains an existing connection and the FSLocation of the selected file.
     * Those are used to open an InputStream on the file and read the contents to a byte array.
     * If the file cannot be resolved, an exception is thrown.
     *
     * @param fileSelector the SettingsModel from the file chooser
     * @param exec ExecutionContext for potentially canceling the InputStream
     * @return byte array of read file contents
     * @throws InvalidSettingsException
     * @throws IOException
     */
    private static byte[] retrieveBodyFileContent(final SettingsModelReaderFileChooser fileSelector,
        final ExecutionContext exec) throws InvalidSettingsException, IOException {
        var fsLocation = fileSelector.getLocation();

        // opening specific fs connection, depending on the FSCategory
        try (FSConnection specificConnection =
            FileSystemHelper.retrieveFSConnection(Optional.ofNullable(fileSelector.getConnection()), fsLocation)
                .orElseThrow(IllegalStateException::new)) {

            // retrieving the actual filesystem from the connection
            try (var fs = specificConnection.getFileSystem()) {
                var rootPath = fs.getPath(fsLocation);

                // making sure, we can access the file
                CheckUtils.checkSetting(!rootPath.toString().trim().isEmpty(), "Please specify a file.");
                CheckUtils.checkSetting(FSFiles.exists(rootPath), "The specified file %s does not exist.", rootPath);
                if (!Files.isReadable(rootPath)) {
                    throw ExceptionUtil.createAccessDeniedException(rootPath);
                }
                final BasicFileAttributes attr = Files.readAttributes(rootPath, BasicFileAttributes.class);
                CheckUtils.checkSetting(attr.isRegularFile(), "%s is not a regular file. Please specify a file.",
                    rootPath);

                // opening an InputStream on the resolved rootPath and reading contents directly to String
                final long fileSize = Files.readAttributes(rootPath, BasicFileAttributes.class).size();
                try (final InputStream sourceStream =
                    new CancellableReportingInputStream(Files.newInputStream(rootPath), exec, fileSize)) {
                    return sourceStream.readAllBytes();
                } catch (EOFException e) {
                    throw new InvalidSettingsException("The end of the file has been reached unexpectedly. ", e);
                }
            }
        }
    }

    /**
     * Converts from JsonObject to a single DefaultRow.
     * @param json
     * @return DataRow
     */
    private static DataRow jsonToRow(final JsonObject json) {
        return new DefaultRow(RowKey.createRowKey(0L),
            StreamSupport.stream(json.entrySet().spliterator(), false)
            .map(v -> new StringCell(((JsonString)v.getValue()).getString()))
            .collect(Collectors.toList()));
    }

    /**
     * Converts from a Map to a single DefaultRow.
     * @param map
     * @return DataRow
     */
    private static DataRow mapToRow(final Map<String, String> map) {
        return new DefaultRow(RowKey.createRowKey(0L),
            StreamSupport.stream(map.entrySet().spliterator(), false)
            .map(v -> new StringCell(v.getValue()))
            .collect(Collectors.toList()));
    }

    @Override
    protected void reset() {
        //No internal state.
    }

    private static DataTableSpec createSpecFromJsonObject(final JsonObject o, final boolean lowerCase) {
        var creator = new DataTableSpecCreator();
        var index = 0;
        for (Entry<String, JsonValue> key : o.entrySet()) {
            String name = lowerCase ? key.getKey().toLowerCase() : key.getKey();
            if (name.isEmpty()) {
                name = "<empty_" + index + ">";
            }
            creator.addColumns(new DataColumnSpecCreator(name, StringCell.TYPE).createSpec());
        }
        return creator.createSpec();
    }

    private static DataTableSpec createBodySpec() {
        return new DataTableSpecCreator().addColumns(
            new DataColumnSpecCreator("body", BinaryObjectDataCell.TYPE).createSpec()).createSpec();
    }

    private DataTableSpec createHeaderSpec() {
        var creator = new DataTableSpecCreator();
        var index = 0;
        for (Entry<String, String> header : m_configuration.getHeaders().entrySet()) {
            String name = header.getKey().toLowerCase();
            if (name.isEmpty()) {
                name = "<empty_" + index + ">";
            }
            creator.addColumns(new DataColumnSpecCreator(name, StringCell.TYPE).createSpec());
        }
        return creator.createSpec();
    }

    private DataTableSpec createQueryParamSpec() {
        var creator = new DataTableSpecCreator();
        for (Entry<String, String> param : m_configuration.getQueryParams().entrySet()) {
            creator.addColumns(new DataColumnSpecCreator(param.getKey(), StringCell.TYPE).createSpec());
        }
        return creator.createSpec();
    }

    @Override
    protected DataTableSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        m_bodyFileModel.configureInModel(inSpecs, m -> {});

        return new DataTableSpec[]{
            createBodySpec(),
            m_headers != null ? createSpecFromJsonObject(m_headers, true) : createHeaderSpec(),
            m_queryParams != null ? createSpecFromJsonObject(m_queryParams, false) : createQueryParamSpec()
        };
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_configuration.save(settings);
        m_bodyFileModel.saveSettingsTo(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_configuration = new RawHTTPInputNodeConfiguration().loadInModel(settings);
        m_bodyFileModel.loadSettingsFrom(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        new RawHTTPInputNodeConfiguration().loadInModel(settings);
        createDefaultFileChooserModel().loadSettingsFrom(settings);
    }

    @Override
    protected void loadInternals(final File internDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
        //No internal state.
    }

    @Override
    protected void saveInternals(final File internDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
        //No internal state.
    }

    @Override
    public ExternalNodeData getInputData() {
        return ExternalNodeData.builder(PARAM_NAME)
                .resource(URI.create("data:application/octet-stream;base64,ABC"))
                .description("")
                .build();
    }

    @Override
    public void validateInputData(final ExternalNodeData inputData) throws InvalidSettingsException {
        // nothing to do
    }

    @Override
    public void setInputData(final ExternalNodeData inputData) throws InvalidSettingsException {
        if (inputData != null) {
            if (inputData.getResource() != null) {
                m_resourceURI = inputData.getResource();
            }
            var jsonValue = inputData.getJSONValue();

            if (jsonValue != null) {
                m_headers = jsonValue.asJsonObject().getJsonObject("headers");
                m_queryParams = jsonValue.asJsonObject().getJsonObject("query_parameters");
            }
        }
    }

    @Override
    public boolean isInputDataRequired() {
        return false;
    }

    @Override
    public boolean isUseAlwaysFullyQualifiedParameterName() {
        return false;
    }
}
