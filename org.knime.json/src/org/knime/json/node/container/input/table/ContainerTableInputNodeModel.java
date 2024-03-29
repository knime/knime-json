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
 *   Mar 29, 2018 (Tobias Urhaug): created
 */
package org.knime.json.node.container.input.table;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.dialog.ExternalNodeData;
import org.knime.core.node.dialog.InputNode;
import org.knime.core.node.dialog.ValueControlledNode;
import org.knime.core.node.port.PortType;
import org.knime.json.node.container.io.FilePathOrURLReader;
import org.knime.json.node.container.mappers.ContainerTableMapper;

import jakarta.json.JsonValue;

/**
 * The model implementation of the Container Input (Table) node.
 * Creates a KNIME table of a json input conforming to a set schema.
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 * @since 4.2
 */
public final class ContainerTableInputNodeModel extends NodeModel implements InputNode, ValueControlledNode {

    private JsonValue m_externalValue;
    private ContainerTableInputNodeConfiguration m_configuration = new ContainerTableInputNodeConfiguration();

    /**
     * Helper to save the node's configuration pre-configured with some values to a nodes settings object.
     *
     * @param settings
     * @param parameterName
     * @param isUseAlwaysFullyQualifiedParameterName If true, use fully-qualified names as parameter name (added as part
     *            of AP-14686)
     * @param table a example table to be set as template, can be <code>null</code> if not available
     * @throws InvalidSettingsException
     */
    public static void saveConfigAsNodeSettings(final NodeSettingsWO settings, final String parameterName,
        final boolean isUseAlwaysFullyQualifiedParameterName, final DataTable table) throws InvalidSettingsException {
        ContainerTableInputNodeConfiguration config = new ContainerTableInputNodeConfiguration();
        config.setParameterName(parameterName);
        config.setUseFQNParamName(isUseAlwaysFullyQualifiedParameterName);
        if (table != null) {
            config.getTemplateConfiguration()
                .setTemplate(ContainerTableMapper.toContainerTableJsonValueFromDataTable(table));
            config.getTemplateConfiguration().setUseEntireTable(true);
        }
        config.save(settings);
    }

    /**
     * Constructor for the node model.
     */
    ContainerTableInputNodeModel() {
        super(
            new PortType[]{BufferedDataTable.TYPE_OPTIONAL},
            new PortType[]{BufferedDataTable.TYPE}
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
            throws Exception {
        JsonValue externalServiceInput = getExternalInput();
        if (externalServiceInput != null) {
            return ContainerTableMapper.toBufferedDataTable(
                externalServiceInput,
                m_configuration.getTemplateTable(),
                exec
            );
        } else {
            if (inData[0] != null) {
                // Perform mapping to validate the incoming table. Throws InvalidSettingsException if not valid.
                ContainerTableMapper.toContainerTable(inData[0]);
                return inData;
            } else {
                setWarningMessage("Configured template table is output");
                return ContainerTableMapper.toBufferedDataTable(m_configuration.getTemplateTable(), exec);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        JsonValue externalServiceInput = getExternalInput();
        if (externalServiceInput != null) {
            final DataTableSpec tableSpec =
                ContainerTableMapper.toTableSpec(externalServiceInput, m_configuration.getTemplateTable());
            return new DataTableSpec[]{tableSpec};
        } else {
            if (inSpecs[0] != null) {
                return inSpecs;
            } else {
                return new DataTableSpec[]{ContainerTableMapper.toTableSpec(m_configuration.getTemplateTable())};
            }
        }
    }

    private JsonValue getExternalInput() throws InvalidSettingsException {
        Optional<String> inputPathOrUrl = m_configuration.getInputPathOrUrl();
        if (inputPathOrUrl.isPresent()) {
            return FilePathOrURLReader.resolveToJson(inputPathOrUrl.get());
        } else {
            return m_externalValue;
        }
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
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_configuration.save(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_configuration = new ContainerTableInputNodeConfiguration().loadInModel(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        new ContainerTableInputNodeConfiguration().loadInModel(settings);
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
        // nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setInputData(final ExternalNodeData inputData) throws InvalidSettingsException {
        m_externalValue = inputData == null ? null : inputData.getJSONValue();
    }

    @Override
    public boolean isInputDataRequired() {
        return false;
    }

    /**
     * {@inheritDoc}
     * @since 4.3
     */
    @Override
    public boolean isUseAlwaysFullyQualifiedParameterName() {
        return m_configuration.isUseFQNParamName();
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
    public void saveCurrentValue(final NodeSettingsWO content) {
        if (m_externalValue != null) {
            String infoMessage = "The output table has been injected from an external caller";
            content.addString("infoMessage", infoMessage);
        }
    }

}
