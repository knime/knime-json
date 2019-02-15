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
package org.knime.json.node.container.input.row;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import javax.json.JsonValue;

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
import org.knime.json.node.container.mappers.ContainerRowMapper;
import org.knime.json.node.container.mappers.ContainerTableMapper;
import org.knime.json.node.container.mappers.MissingColumnHandling;

/**
 * The model implementation of the Container Input (Row) node.
 * Creates a single row KNIME table of a json input.
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 */
final class ContainerRowInputNodeModel extends NodeModel implements InputNode, ValueControlledNode {

    private JsonValue m_externalValue;
    private ContainerRowInputNodeConfiguration m_configuration = new ContainerRowInputNodeConfiguration();

    /**
     * Constructor for the node model.
     */
    ContainerRowInputNodeModel() {
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
        JsonValue externalInput = getExternalInput();

        if (externalInput != null) {
            if (m_configuration.getUseTemplateAsSpec()) {
                BufferedDataTable[] templateRow = templateRow(exec);
                DataTableSpec templateRowSpec = templateRow[0].getDataTableSpec();
                MissingColumnHandling missingColumnHandling = m_configuration.getMissingColumnHandling();

                BufferedDataTable dataTable =
                    ContainerRowMapper.toDataTable(externalInput, templateRowSpec, missingColumnHandling, exec);

                return new BufferedDataTable[] {dataTable};
            } else {
                return new BufferedDataTable[] {ContainerRowMapper.toDataTable(externalInput, exec)};
            }
        } else {
            if (inData[0] != null) {
                return inData;
            } else {
                setWarningMessage("Configured template row is output");
                return templateRow(exec);
            }
        }
    }

    private BufferedDataTable[] templateRow(final ExecutionContext exec) throws InvalidSettingsException {
        return ContainerTableMapper.toBufferedDataTable(m_configuration.getTemplateRow(), exec);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        JsonValue externalInput = getExternalInput();
        DataTableSpec templateRowSpec = ContainerTableMapper.toTableSpec(m_configuration.getTemplateRow());
        if (externalInput != null) {
            if (m_configuration.getUseTemplateAsSpec()) {
                return new DataTableSpec[] {getTemplateTableSpec(externalInput, templateRowSpec)};
            } else {
                return new DataTableSpec[]{ContainerRowMapper.toTableSpec(externalInput)};
            }
        } else {
            if (inSpecs[0] != null) {
                return inSpecs;
            } else {
                return new DataTableSpec[] {templateRowSpec};
            }
        }
    }

    private DataTableSpec getTemplateTableSpec(final JsonValue externalInput, final DataTableSpec templateRowSpec)
            throws InvalidSettingsException {
        return
            ContainerRowMapper.toTableSpec(externalInput, templateRowSpec, m_configuration.getMissingColumnHandling());
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
        m_configuration = new ContainerRowInputNodeConfiguration().loadInModel(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        new ContainerRowInputNodeConfiguration().loadInModel(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExternalNodeData getInputData() {
        return ExternalNodeData
                .builder(m_configuration.getParameterName())
                .description(m_configuration.getDescription())
                .jsonValue(m_configuration.getTemplateRow())
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
    public void saveCurrentValue(final NodeSettingsWO content) {
        if (m_externalValue != null) {
            String infoMessage = "The output table has been injected from an external caller";
            content.addString("infoMessage", infoMessage);
        }
    }

}
