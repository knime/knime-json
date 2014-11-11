package org.knime.json.node.schema.check;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.json.JSONCellFactory;
import org.knime.core.data.json.JSONValue;
import org.knime.core.data.json.JacksonConversions;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.json.internal.Activator;
import org.knime.json.node.util.ReplaceOrAddColumnSettings;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;

/**
 * This is the model implementation of the "JSONSchemaCheck" node. Checks a JSON column's values against a Schema and
 * fails if it do not match.
 *
 * @author Gabor Bakos
 */
public final class JSONSchemaCheckNodeModel extends NodeModel {
    private JSONSchemaCheckSettings m_settings = createJSONSchemaSettings();

    /**
     * Constructor for the node model.
     */
    protected JSONSchemaCheckNodeModel() {
        super(1, 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        if (m_settings.getInputColumn() == null) {
            m_settings.setInputColumn(handleNonSetColumn(inSpecs[0]).getName());
        }
        return new DataTableSpec[0];
    }

    /**
     * This method gets called when no column was selected.
     *
     * @param tableSpec The input table spec.
     * @return The autoguessed input column spec.
     * @throws InvalidSettingsException Could not find proper column.
     */
    protected DataColumnSpec handleNonSetColumn(final DataTableSpec tableSpec) throws InvalidSettingsException {
        List<String> compatibleCols = new ArrayList<>();
        for (DataColumnSpec c : tableSpec) {
            if (c.getType().isCompatible(JSONValue.class)) {
                compatibleCols.add(c.getName());
            }
        }
        if (compatibleCols.size() == 1) {
            // auto-configure
            m_settings.setInputColumn(compatibleCols.get(0));
            return tableSpec.getColumnSpec(compatibleCols.get(0));
        } else if (compatibleCols.size() > 1) {
            // auto-guessing
            m_settings.setInputColumn(compatibleCols.get(0));
            setWarningMessage("Auto guessing: using column \"" + compatibleCols.get(0) + "\".");
            return tableSpec.getColumnSpec(compatibleCols.get(0));
        } else {
            throw new InvalidSettingsException(
                ReplaceOrAddColumnSettings.NO_JSON_COLUMNS_USE_FOR_EXAMPLE_THE_STRING_TO_JSON_NODE_TO_CREATE_ONE);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws Exception {
        final JacksonConversions conv = JacksonConversions.getInstance();
        final JsonSchema schema;
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(Activator.getInstance().getJsonSchemaCoreClassLoader());
            schema =
                JsonSchemaFactory.byDefault().getJsonSchema(
                    conv.toJackson(((JSONValue)JSONCellFactory.create(m_settings.getInputSchema(), false))
                        .getJsonValue()));
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
        }
        BufferedDataTable table = inData[0];
        int index = inData[0].getSpec().findColumnIndex(m_settings.getInputColumn());
        for (DataRow row : table) {
            DataCell cell = row.getCell(index);
            if (cell instanceof JSONValue) {
                JSONValue jv = (JSONValue)cell;
                JsonNode json = conv.toJackson(jv.getJsonValue());
                ProcessingReport report = schema.validate(json);
                if (!report.isSuccess()) {
                    RuntimeException ex =
                        new RuntimeException("Failed to validate row: " + row.getKey() + "\n" + report);
                    for (ProcessingMessage message : report) {
                        ex.addSuppressed(message.asException());
                    }
                    throw ex;
                }
            }
        }
        return new BufferedDataTable[0];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // No internal state to reset.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_settings.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_settings.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
        //No internal state.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
        //No internal state.
    }

    /**
     * @return
     */
    static JSONSchemaCheckSettings createJSONSchemaSettings() {
        return new JSONSchemaCheckSettings();
    }
}
