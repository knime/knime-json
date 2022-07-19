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
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.connections.RelativeTo;
import org.knime.filehandling.core.connections.location.FSPathProvider;
import org.knime.filehandling.core.connections.location.FSPathProviderFactory;
import org.knime.filehandling.core.connections.uriexport.URIExporterIDs;
import org.knime.filehandling.core.connections.uriexport.noconfig.NoConfigURIExporterFactory;
import org.knime.filehandling.core.data.location.variable.FSLocationVariableType;
import org.knime.json.node.container.input.file.ContainerNodeSharedConfiguration;

/**
 * The model for the “Container Output (File)” node.
 *
 * @author Jannik Löscher, KNIME GmbH, Konstanz, Germany
 */
final class ContainerFileOutputNodeModel extends NodeModel implements OutputNode {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(ContainerFileOutputNodeModel.class);

    static final VariableType<?>[] SUPPORTED_VAR_TYPES = {FSLocationVariableType.INSTANCE};

    static final String CFG_PARAMETER_DEFAULT = "file-output";

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
            throw new InvalidSettingsException("Invalid (empty) path variable name");
        }

        final Map<String, FlowVariable> flowVariables = getAvailableFlowVariables(SUPPORTED_VAR_TYPES);
        final var flowVariable = varName.map(flowVariables::get);

        if (flowVariable.isEmpty()) {
            throw new InvalidSettingsException("No variable with selected name \"" + varName.get() + "\" found.");
        }

        final var fsLocation = flowVariable.get().getValue(FSLocationVariableType.INSTANCE);
        return checkFSLocationAndGetUri(fsLocation, varName.get());
    }

    private static URI checkFSLocationAndGetUri(final FSLocation fsLocation, final String varName)
        throws InvalidSettingsException {
        try (final FSPathProviderFactory factory = FSPathProviderFactory.newFactory(Optional.empty(), fsLocation);
                final FSPathProvider pathProvider = factory.create(fsLocation);
                final FSConnection pathConnection = pathProvider.getFSConnection()) {

            Path filePath = pathProvider.getPath().normalize();

            checkIsWorkflowDataAreaRelativeLocation(fsLocation, varName);
            checkIsWithinWorkflowDataArea(filePath, fsLocation, varName);

            CheckUtils.checkSetting(Files.exists(filePath.toAbsolutePath()),
                "Path variable \"%s\" does not denote an existing file: %s", varName, fsLocation);

            CheckUtils.checkSetting(Files.isRegularFile(filePath),
                "Path variable \"%s\" does not denote a regular file: %s", varName, filePath);

            return resolveToURI(pathProvider.getPath(), pathConnection);
        } catch (IOException | URISyntaxException e) {
            throw new InvalidSettingsException(String.format("The location '%s' could not be converted to an URI: %s",
                fsLocation.getPath(), e.getMessage()), e);
        }

    }

    private static URI resolveToURI(final FSPath filePath, final FSConnection connection)
        throws IOException, URISyntaxException {
        final var exporter =
            ((NoConfigURIExporterFactory)connection.getURIExporterFactory(URIExporterIDs.LEGACY_KNIME_URL))
                .getExporter();
        return FileUtil.resolveToPath(FileUtil.toURL(exporter.toUri(filePath).toString())).toUri();
    }

    private static void checkIsWorkflowDataAreaRelativeLocation(final FSLocation fsLocation, final String varName)
        throws InvalidSettingsException {
        CheckUtils.checkSetting(isWorkflowDataAreaRelativeLocation(fsLocation),
            "Path variable \"%s\" does not denote a 'workflow data area'-relative file location: %s", varName,
            fsLocation);
    }

    private static boolean isWorkflowDataAreaRelativeLocation(final FSLocation fsLocation) {
        return fsLocation.getFSCategory() == FSCategory.RELATIVE && fsLocation.getFileSystemSpecifier()
            .map(RelativeTo.WORKFLOW_DATA.getSettingsValue()::equals).orElse(Boolean.FALSE);
    }

    private static void checkIsWithinWorkflowDataArea(final Path filePath, final FSLocation fsLocation,
        final String varName) throws InvalidSettingsException {
        // try to give a possible attacker not too much information about the file system
        CheckUtils.checkSetting(filePath.toAbsolutePath().normalize().equals(filePath.toAbsolutePath()),
            "Path variable \"%s\" is pointing outside of the 'workflow data area': %s", varName, fsLocation);
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
            data.resource(getPathFromVariable());
        } catch (InvalidSettingsException e) {
            LOGGER.error("Could not fetch resource: " + e.getMessage(), e);
            try {
                data.resource(new URI("unknown-filename"));
            } catch (URISyntaxException e1) {
                LOGGER.error("Error while creating resource URI for unknown file: " + e1.getMessage(), e1);
            }
        }

        return data.build();
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
