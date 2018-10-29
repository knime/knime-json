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
 *   Aug 2, 2018 (Tobias Urhaug, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.json.node.container.input.credentials;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import javax.json.JsonValue;

import org.apache.commons.io.IOUtils;
import org.knime.core.data.json.container.credentials.ContainerCredential;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.dialog.ExternalNodeData;
import org.knime.core.node.dialog.InputNode;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.flowvariable.FlowVariablePortObject;
import org.knime.core.node.port.flowvariable.FlowVariablePortObjectSpec;
import org.knime.core.node.workflow.CredentialsProvider;
import org.knime.core.node.workflow.CredentialsStore.CredentialsNode;
import org.knime.core.node.workflow.WorkflowLoadHelper;
import org.knime.core.util.FileUtil;
import org.knime.json.util.JSONUtil;

/**
 * Node model for the Container Input (Credentials) node.
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 * @since 3.7
 */
final class ContainerCredentialsInputNodeModel extends NodeModel implements InputNode, CredentialsNode {

    private JsonValue m_externalValue;
    private ContainerCredentialsInputNodeConfiguration m_configuration =
            new ContainerCredentialsInputNodeConfiguration();

    /**
     * Constructor for the node model.
     */
    ContainerCredentialsInputNodeModel() {
        super(
            null,
            new PortType[]{FlowVariablePortObject.TYPE}
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return new PortObjectSpec[]{FlowVariablePortObjectSpec.INSTANCE};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData, final ExecutionContext exec)
            throws Exception {
        JsonValue externalInput = getExternalServiceInput();
        List<ContainerCredential> credentials = mapToContainerCredentials(externalInput);
        for (ContainerCredential containerCredential : credentials) {
            pushCredentialsFlowVariable(
                containerCredential.getId(),
                containerCredential.getUser(),
                containerCredential.getPassword()
            );
        }
        return new PortObject[]{FlowVariablePortObject.INSTANCE};
    }

    private JsonValue getExternalServiceInput() throws InvalidSettingsException {
        Optional<String> inputFileNameOptional = m_configuration.getInputPathOrUrl();
        if (inputFileNameOptional.isPresent()) {
            return getInputFromFile(inputFileNameOptional.get());
        } else if (m_externalValue != null) {
            return m_externalValue;
        } else {
            return null;
        }
    }

    private static JsonValue getInputFromFile(final String inputFileName) throws InvalidSettingsException {
        try (InputStream inputStream = FileUtil.openInputStream(inputFileName)){
            String externalJsonString = IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
            return JSONUtil.parseJSONValue(externalJsonString);
        } catch (IOException  e) {
            throw new InvalidSettingsException("Input file path \"" + inputFileName + "\" could not be resolved "
                + "or the input is not a valid json file");
        }
    }

    private List<ContainerCredential> mapToContainerCredentials(final JsonValue externalInput)
            throws InvalidSettingsException {
        if (externalInput != null) {
            return ContainerCredentialMapper.toContainerCredentials(externalInput);
        } else {
            setWarningMessage("Template credentials are output");
            return ContainerCredentialMapper.toContainerCredentials(m_configuration.getExampleInput());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_configuration.save(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_configuration = new ContainerCredentialsInputNodeConfiguration().loadInModel(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        new ContainerCredentialsInputNodeConfiguration().loadInModel(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExternalNodeData getInputData() {
        return ExternalNodeData
                .builder(m_configuration.getParameterName())
                .description(m_configuration.getDescription())
                .jsonValue(m_configuration.getExampleInput())
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validateInputData(final ExternalNodeData inputData) throws InvalidSettingsException {
        if (inputData.getJSONValue() == null) {
            throw new InvalidSettingsException("No JSON input provided (is null)");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setInputData(final ExternalNodeData inputData) throws InvalidSettingsException {
        m_externalValue = inputData.getJSONValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        //No internal state.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        //No internal state.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        //No internal state.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void doAfterLoadFromDisc(
            final WorkflowLoadHelper loadHelper,
            final CredentialsProvider credProvider,
            final boolean isExecuted,
            final boolean isInactive) {

        // intentionally left blank, no need to prompt the user for passwords as passwords are only injected from an
        // external caller and not controlled by this node.

    }

}
