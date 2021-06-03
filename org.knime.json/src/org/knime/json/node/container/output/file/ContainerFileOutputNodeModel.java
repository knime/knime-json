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
 *   27.05.2021 (loescher): created
 */
package org.knime.json.node.container.output.file;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.dialog.ExternalNodeData;
import org.knime.core.node.dialog.OutputNode;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.flowvariable.FlowVariablePortObject;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.VariableType;
import org.knime.core.util.FileUtil;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.connections.location.FSPathProvider;
import org.knime.filehandling.core.connections.location.FSPathProviderFactory;
import org.knime.filehandling.core.connections.uriexport.URIExporter;
import org.knime.filehandling.core.connections.uriexport.URIExporterIDs;
import org.knime.filehandling.core.connections.uriexport.noconfig.NoConfigURIExporterFactory;
import org.knime.filehandling.core.data.location.variable.FSLocationVariableType;
import org.knime.filehandling.core.defaultnodesettings.ValidationUtils;
import org.knime.json.node.container.input.file.ContainerNodeSharedConfiguration;

/**
 * The model for the “Container Output (File)” node.
 *
 * @author Jannik Löscher, KNIME GmbH, Konstanz, Germany
 */
final class ContainerFileOutputNodeModel extends NodeModel implements OutputNode {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(ContainerFileOutputNodeModel.class);

    static final VariableType<?>[] SUPPORTED_VAR_TYPES = {FSLocationVariableType.INSTANCE};

    static final String CFG_PARAMETER_DEFAULT = "output-file";

    private final ContainerNodeSharedConfiguration m_sharedConfig;

    private final NodeConfiguration m_config;

    /**
     * Creates a new {@link ContainerFileOutputNodeModel} with no input ports and one flow variable output port.
     */
    ContainerFileOutputNodeModel() {
        super(new PortType[]{FlowVariablePortObject.TYPE}, new PortType[0]);
        m_sharedConfig = new ContainerNodeSharedConfiguration(CFG_PARAMETER_DEFAULT);
        m_config = new NodeConfiguration();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        getPathFromVariable();

        return new PortObjectSpec[0];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        getPathFromVariable();

        return new PortObject[0];
    }

    private URI getPathFromVariable() throws InvalidSettingsException {
        final var varName = m_config.getFlowVariableName();
        if (varName.isEmpty() || varName.get().isEmpty()) {
            throw new InvalidSettingsException("Invalid (empty) variable name");
        }

        String value;
        final Map<String, FlowVariable> flowVariables = getAvailableFlowVariables(SUPPORTED_VAR_TYPES);
        final var flowVariable = varName.map(flowVariables::get);

        if (flowVariable.isEmpty()) {
            throw new InvalidSettingsException("No variable with selected name \"" + varName.get() + "\" found.");
        }

        value = checkVariable(flowVariable.get());

        Path filePath = null;
        Path workflowPath = null;
        try {
            workflowPath = FileUtil.resolveToPath(FileUtil.toURL("knime://knime.workflow/data/"));
        } catch (IOException | URISyntaxException | InvalidPathException e) {
            throw new IllegalStateException("Workflow data area path does not denote a valid file.", e);
        }
        try {
            filePath = FileUtil.resolveToPath(FileUtil.toURL(value));
        } catch (IOException | URISyntaxException | InvalidPathException e) {
            throw new InvalidSettingsException(
                "Variable \"" + varName.get() + "\" does not denote a valid file: " + value, e);
        }

        CheckUtils.checkState(workflowPath != null && Files.exists(workflowPath),
            "Workflow path does not denote an existing file: %s", workflowPath);
        CheckUtils.checkSetting(filePath != null,
            "Variable \"%s\" does not denote an existing file or is pointing outside of the workflow data area: %s",
            varName.get(), value);

        try {
            // try to give a possible attacker not too much information about the file system
            CheckUtils.checkSetting(
                Files.exists(filePath) && filePath.toRealPath().startsWith(workflowPath.toRealPath()), // NOSONAR: non-null state is checked above
                "Variable \"%s\" does not denote an existing file or is pointing outside of the workflow data area: %s",
                varName.get(), value);
        } catch (IOException | InvalidPathException e) {
            throw new InvalidSettingsException(
                "Could not resolve workflow data area or file path (\"" + value + "\") to real file.", e);
        }

        CheckUtils.checkSetting(Files.isRegularFile(filePath), "Variable \"%s\" does not denote a regular file: %s",
            varName.get(), value);

        return filePath.toUri(); // NOSONAR: non-null state is checked above by CheckUtils
    }

    private static String checkVariable(final FlowVariable variable) throws InvalidSettingsException {
        final var fsLocation = variable.getValue(FSLocationVariableType.INSTANCE);
        switch (fsLocation.getFSCategory()) {
            case RELATIVE:
                return getRelativePath(fsLocation);
            case LOCAL:
                ValidationUtils.validateLocalFsAccess();
                return fsLocation.getPath();
            case CUSTOM_URL:
                ValidationUtils.validateCustomURLLocation(fsLocation);
                return fsLocation.getPath();
            default:
                // MOUNTPOINT, CONNECTION
                throw new InvalidSettingsException("File system category is not yet supported");
        }
    }

    private static String getRelativePath(final FSLocation fsLocation) {
        try (final FSPathProviderFactory factory = FSPathProviderFactory.newFactory(Optional.empty(), fsLocation);
                final FSPathProvider pathProvider = factory.create(fsLocation);
                final FSConnection fsConnection = pathProvider.getFSConnection();) {

            final URIExporter exporter =
                ((NoConfigURIExporterFactory)fsConnection.getURIExporterFactory(URIExporterIDs.LEGACY_KNIME_URL))
                    .getExporter();
            final FSPath path = pathProvider.getPath();
            return exporter.toUri(path).toString();
        } catch (IOException | URISyntaxException e) {
            throw new IllegalArgumentException(String.format("The path '%s' could not be converted to a KNIME URL: %s",
                fsLocation.getPath(), e.getMessage()), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isUseAlwaysFullyQualifiedParameterName() {
        return m_sharedConfig.hasFullyQualifiedName();
    }

    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // No internal state
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // No internal state
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExternalNodeData getExternalOutput() {
        final var data =
            ExternalNodeData.builder(m_sharedConfig.getParameter()).description(m_sharedConfig.getDescription());
        try {
            return data.resource(getPathFromVariable()).build();
        } catch (InvalidSettingsException e) {
            LOGGER.error("Could not fetch resource: " + e.getMessage(), e);
            return data.resource(null).build();
        }
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_sharedConfig.saveSettingsTo(settings);
        m_config.saveSettingsTo(settings);

    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        ContainerNodeSharedConfiguration.validateSettings(settings);
        NodeConfiguration.validateSettings(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_sharedConfig.loadValidatedSettingsFrom(settings);
        m_config.loadValidatedSettingsFrom(settings);
    }

    @Override
    protected void reset() {
        // Nothing to do
    }
}
