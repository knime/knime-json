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
package org.knime.json.node.output;

import java.io.File;
import java.io.IOException;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonValue;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.json.JSONValue;
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
import org.knime.core.node.dialog.ExternalNodeData.ExternalNodeDataBuilder;
import org.knime.core.node.dialog.OutputNode;
import org.knime.core.node.util.CheckUtils;

/**
 * This is the model for the JSON output node.
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
final class JSONOutputNodeModel extends NodeModel implements BufferedDataTableHolder, OutputNode {
    private JSONOutputConfiguration m_configuration;
    private BufferedDataTable m_table;

    JSONOutputNodeModel() {
        super(1, 0);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("null")
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        CheckUtils.checkSetting(m_configuration != null, "No configuration set - confirm in dialog");
        DataColumnSpec jsonCol = inSpecs[0].getColumnSpec(m_configuration.getJsonColumnName());
        CheckUtils.checkSetting(jsonCol != null,
                "Selected column '%s' does not exist", m_configuration.getJsonColumnName());
        CheckUtils.checkSetting(jsonCol.getType().isCompatible(JSONValue.class), "Selected column '%s' not "
            + "json compatible, it's ", m_configuration.getJsonColumnName(), jsonCol.getType());
        return new DataTableSpec[0];
    }

    /** {@inheritDoc} */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
        final ExecutionContext exec) throws Exception {
        ColumnRearranger r = new ColumnRearranger(inData[0].getDataTableSpec());
        r.keepOnly(m_configuration.getJsonColumnName());
        m_table = exec.createColumnRearrangeTable(inData[0], r, exec);
        return new BufferedDataTable[0];
    }

    /**
     * Read a table into a {@link JsonValue}.
     *
     * @param table The table to read from
     * @param allowStaleState TODO
     * @param keepOneRowTablesSimple if <code>true</code>, the top-level array will be ommitted for single row tables.
     * @return A json value containing the data of the table.
     */
    static JsonValue readIntoJsonValue(final BufferedDataTable table, final boolean allowStaleState,
        final boolean keepOneRowTablesSimple) {
        CheckUtils.checkState(allowStaleState || table != null, "No table set, JSON output node must be executed");
        if (table == null) {
            return Json.createArrayBuilder().build();
        }

        JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
        final long rowCount = table.size();
        try (CloseableRowIterator it = allowStaleState ? table.iteratorFailProve() : table.iterator()) {
            while (it.hasNext()) {
                DataRow r = it.next();
                DataCell cell = r.getCell(0);
                if (cell.isMissing()) {
                    arrayBuilder.addNull();
                } else {
                    JsonValue jsonValue = ((JSONValue)cell).getJsonValue();
                    if ((rowCount == 1) && keepOneRowTablesSimple) {
                        return jsonValue;
                    } else {
                        arrayBuilder.add(jsonValue);
                    }
                }
            }
        }
        return arrayBuilder.build();
    }

    JsonValue getViewJSONObject() {
        return readIntoJsonValue(m_table, true, m_configuration.isKeepOneRowTablesSimple());
    }

    /** {@inheritDoc} */
    @Override
    protected void reset() {
        m_table = null;
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

    /** {@inheritDoc} */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        if (m_configuration != null) {
            final JsonValue exampleJson = m_configuration.getExampleJson();
            if (exampleJson == null) {
                m_configuration
                    .setExampleJson(readIntoJsonValue(m_table, true, m_configuration.isKeepOneRowTablesSimple()));
            }

            m_configuration.save(settings);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        new JSONOutputConfiguration().loadInModel(settings);
    }

    /** {@inheritDoc} */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_configuration = new JSONOutputConfiguration().loadInModel(settings);
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

    /**
     * {@inheritDoc}
     */
    @Override
    public ExternalNodeData getExternalOutput() {
        if (m_configuration == null) {
            return ExternalNodeData.builder("").build();
        }

        ExternalNodeDataBuilder builder = ExternalNodeData.builder(m_configuration.getParameterName());
        if (m_table != null) {
            builder.jsonValue(readIntoJsonValue(m_table, false, m_configuration.isKeepOneRowTablesSimple()));
        } else if (m_configuration.getExampleJson() != null) {
            builder.jsonValue(m_configuration.getExampleJson());
        } else {
            builder.jsonValue(JsonValue.NULL);
        }

        builder.description(m_configuration.getDescription());
        return builder.build();
    }
}
