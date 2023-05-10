package org.knime.json.node.combine.row;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.StringValue;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.json.JSONCell;
import org.knime.core.data.json.JSONCellFactory;
import org.knime.core.data.json.JSONValue;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.util.JsonUtil;

import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;

/**
 * This is the model implementation of RowCombineJson. Appends JSON values in the rows to a single JSON value.
 *
 * @author Gabor Bakos
 */
class RowCombineJsonNodeModel extends NodeModel {
    private final RowCombineJsonSettings m_settings = new RowCombineJsonSettings();

    /**
     * Constructor for the node model.
     */
    protected RowCombineJsonNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws Exception {
        BufferedDataTable table = inData[0];
        BufferedDataContainer container = exec.createDataContainer(configure(new DataTableSpec[]{table.getSpec()})[0]);
        int counter = 0;
        final double all = table.size();
        final int idx = table.getSpec().findColumnIndex(m_settings.getInputColumn());
        final JsonArrayBuilder builder = JsonUtil.getProvider().createArrayBuilder();
        final JsonObjectBuilder innerObjectBuilder = JsonUtil.getProvider().createObjectBuilder();
        final String[] keys = m_settings.getKeys();
        final String[] values = m_settings.getValues();
        final int objectKeyIndex = table.getSpec().findColumnIndex(m_settings.getObjectKeyColumn());
        final Set<String> processedKeys = new HashSet<>();
        for (DataRow row : table) {
            exec.checkCanceled();
            exec.setProgress(counter++ / all, "Processing row: " + row.getKey().getString());
            DataCell cell = row.getCell(idx);
            switch (m_settings.getObjectOrArray()) {
                case Array:
                    if (cell.isMissing() || !(cell instanceof JSONValue)) {
                        builder.addNull();
                    } else {
                        builder.add(((JSONValue)cell).getJsonValue());
                    }
                    break;
                case Object:
                    if (m_settings.isObjectKeyIsRowID()) {
                        if (cell.isMissing() || !(cell instanceof JSONValue)) {
                            innerObjectBuilder.addNull(row.getKey().getString());
                        } else {
                            innerObjectBuilder.add(row.getKey().getString(), ((JSONValue)cell).getJsonValue());
                        }
                    } else {
                        CheckUtils.checkState(-1 != objectKeyIndex,
                            "Not found column: " + m_settings.getObjectKeyColumn());
                        final DataCell cell2 = row.getCell(objectKeyIndex);
                        CheckUtils.checkState(!cell2.isMissing(), "Key cell is missing in row: " + row.getKey());
                        CheckUtils.checkState(cell2 instanceof StringValue,
                            "The value for key is not a String in row: " + row.getKey());
                        final StringValue sv = (StringValue)cell2;
                        CheckUtils.checkState(
                            processedKeys.add(sv.getStringValue()),
                            "The value \"" + sv.getStringValue() + "\" for the key was already present in row: "
                                + row.getKey());
                        if (cell.isMissing() || !(cell instanceof JSONValue)) {
                            innerObjectBuilder.addNull(sv.getStringValue());
                        } else {
                            innerObjectBuilder.add(sv.getStringValue(), ((JSONValue)cell).getJsonValue());
                        }
                    }
                    break;
                default:
                    CheckUtils.checkState(false, "Not supported collection type: " + m_settings.getObjectOrArray());
            }
        }
        final DataCell cell;
        if (m_settings.isAddRootKey()) {
            JsonObjectBuilder objectBuilder = JsonUtil.getProvider().createObjectBuilder();
            switch (m_settings.getObjectOrArray()) {
                case Array:
                    objectBuilder.add(m_settings.getRootKey(), builder.build());
                    break;
                case Object:
                    objectBuilder.add(m_settings.getRootKey(), innerObjectBuilder.build());
                    break;
                default:
                    CheckUtils.checkState(false, "Not supported collection type: " + m_settings.getObjectOrArray());
            }
            for (int i = 0; i < keys.length; ++i) {
                objectBuilder.add(keys[i], values[i]);
            }
            cell = JSONCellFactory.create(objectBuilder.build());
        } else {
            switch (m_settings.getObjectOrArray()) {
                case Array:
                    cell = JSONCellFactory.create(builder.build());
                    break;
                case Object:
                    cell = JSONCellFactory.create(innerObjectBuilder.build());
                    break;
                default:
                    CheckUtils.checkState(false, "Not supported collection type: " + m_settings.getObjectOrArray());
                    throw new IllegalStateException("To make the compiler happy");
            }
        }
        container.addRowToTable(new DefaultRow(RowKey.createRowKey(1L), cell));
        container.close();
        return new BufferedDataTable[]{container.getTable()};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        //No internal state
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        // validate settings for the JSON column
        String warning = m_settings.autoConfigure(inSpecs[0]);
        if (warning != null && !warning.isEmpty()) {
            setWarningMessage(warning);
        }
        if (m_settings.getNewColumn() == null || m_settings.getNewColumn().isEmpty()) {
            throw new InvalidSettingsException("No output column name was specified!");
        }
        return new DataTableSpec[]{new DataTableSpec(
            new DataColumnSpecCreator(m_settings.getNewColumn(), JSONCell.TYPE).createSpec())};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_settings.loadSettingsModel(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        new RowCombineJsonSettings().loadSettingsModel(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
        //No internal state
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
        //No internal state
    }

}
