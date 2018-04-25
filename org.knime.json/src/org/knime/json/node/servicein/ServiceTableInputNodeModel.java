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
package org.knime.json.node.servicein;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;

import javax.json.JsonValue;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.json.servicetable.ServiceTable;
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
import org.knime.core.node.port.PortType;
import org.knime.core.util.FileUtil;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The model implementation of the service table input node.
 * Creates a knime table of a json input conforming to a set schema.
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 */
public class ServiceTableInputNodeModel extends NodeModel implements InputNode {

    private final ObjectMapper m_objectMapper;

    private JsonValue m_externalValue;
    private ServiceTableInputNodeConfiguration m_configuration = new ServiceTableInputNodeConfiguration();

    /**
     * Constructor for the node model.
     */
    protected ServiceTableInputNodeModel() {
        super(
            new PortType[]{BufferedDataTable.TYPE_OPTIONAL},
            new PortType[]{BufferedDataTable.TYPE});
        m_objectMapper = new ObjectMapper();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
            throws Exception {
        ServiceTable externalServiceInput = getExternalServiceInput();
        if (externalServiceInput != null) {
            return ServiceTableConverter.toBufferedDataTable(externalServiceInput, exec);
        } else {
            if (inData[0] != null) {
                return inData;
            } else {
                String defaultJson = ServiceTableInputDefaultJsonStructure.asString();
                ServiceTable defaultTable = mapJsonToServiceTable(defaultJson);
                return ServiceTableConverter.toBufferedDataTable(defaultTable, exec);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        ServiceTable externalServiceInput = getExternalServiceInput();
        if (externalServiceInput != null) {
            return new DataTableSpec[]{ServiceTableConverter.toTableSpec(externalServiceInput)};
        } else {
            if (inSpecs[0] != null) {
                return inSpecs;
            } else {
                String defaultJson = ServiceTableInputDefaultJsonStructure.asString();
                ServiceTable defaultTable = mapJsonToServiceTable(defaultJson);
                return new DataTableSpec[]{ServiceTableConverter.toTableSpec(defaultTable)};
            }
        }
    }

    private ServiceTable getExternalServiceInput() throws InvalidSettingsException {
        String externalInput = null;
        String inputFileName = m_configuration.getFileName();
        if (!StringUtils.isEmpty(inputFileName)) {
            try {
                File inputFile = FileUtil.getFileFromURL(new URL(inputFileName));
                externalInput = new String(Files.readAllBytes(inputFile.toPath()));
            } catch (IOException  e) {
                throw new InvalidSettingsException("Input path \"" + inputFileName + "\" could not be resolved" , e);
            }
        } else if (m_externalValue != null) {
            externalInput = m_externalValue.toString();
        }
        return externalInput == null ? null : mapJsonToServiceTable(externalInput);
    }

    private ServiceTable mapJsonToServiceTable(final String jsonInput) throws InvalidSettingsException {
        try {
            return m_objectMapper.readValue(jsonInput, ServiceTable.class);
        } catch (IOException e) {
            throw new InvalidSettingsException("Input could not be parsed to table: " + e.getMessage(), e);
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
        m_configuration = new ServiceTableInputNodeConfiguration().loadInModel(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        new ServiceTableInputNodeConfiguration().loadInModel(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExternalNodeData getInputData() {
        JsonValue value = m_externalValue != null ? m_externalValue : ServiceTableInputDefaultJsonStructure.asJsonValue();
        return ExternalNodeData.builder(m_configuration.getParameterName())
                .description(m_configuration.getDescription())
                .jsonValue(value)
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

}
