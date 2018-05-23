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
 *   May 4, 2018 (Tobias Urhaug, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.json.node.servicetableoutput;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.json.JsonValue;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.json.servicetable.ServiceTable;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.BufferedDataTableHolder;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.dialog.ExternalNodeData;
import org.knime.core.node.dialog.OutputNode;
import org.knime.json.util.JSONUtil;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The model implementation of the service table output node.
 * Creates a json output conforming to a set schema for the incoming table
 * and makes it available for a caller through the {@link OutputNode} interface.
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 */
public class ServiceTableOutputNodeModel extends NodeModel implements BufferedDataTableHolder, OutputNode {

    private ServiceTableOutputNodeConfiguration m_configuration = new ServiceTableOutputNodeConfiguration();
    private BufferedDataTable m_table;

    /**
     * Constructor for the node model.
     */
    protected ServiceTableOutputNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
            throws Exception {
        m_table = inData[0];
        writeInputAsJsonFileIfPathPresent();

        return inData;
    }

    private void writeInputAsJsonFileIfPathPresent() throws IOException, JsonGenerationException, JsonMappingException {
        String outputFilePath = m_configuration.getOutputFilePath();
        if (StringUtils.isNotEmpty(outputFilePath)) {
            Path path = Paths.get(outputFilePath);
            try (OutputStream outputStream = Files.newOutputStream(path)) {
                new ObjectMapper().writeValue(outputStream, BufferedDataTableToServiceTable.toServiceTable(m_table));
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        return inSpecs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExternalNodeData getExternalOutput() {
        return
            ExternalNodeData.builder(m_configuration.getParameterName())
                .description(m_configuration.getDescription())
                .jsonValue(getJsonValue())
                .build();
    }

    private JsonValue getJsonValue() {
        JsonValue jsonValue = null;
        try {
            if (m_table == null) {
                jsonValue = JSONUtil.parseJSONValue("{}");
            } else {
                ServiceTable inputTable = BufferedDataTableToServiceTable.toServiceTable(m_table);
                jsonValue = JSONUtil.parseJSONValue(new ObjectMapper().writeValueAsString(inputTable));
            }
        } catch (IOException e) {
            throw new RuntimeException("Error while parsing JSON", e);
        }
        return jsonValue;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        // Handled by BufferedDataTableHolder
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        // Handled by BufferedDataTableHolder
    }

    /** {@inheritDoc} */
    @Override
    public BufferedDataTable[] getInternalTables() {
        return new BufferedDataTable[] {m_table};
    }

    /** {@inheritDoc} */
    @Override
    public void setInternalTables(final BufferedDataTable[] tables) {
        m_table = tables[0];
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
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        new ServiceTableOutputNodeConfiguration().loadInModel(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_configuration = new ServiceTableOutputNodeConfiguration().loadInModel(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_table = null;
    }

}
