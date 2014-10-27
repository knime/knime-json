package org.knime.json.node.fromstring;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.MissingCell;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.json.JSONCell;
import org.knime.core.data.json.JSONCellFactory;
import org.knime.core.data.json.JSONValue;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.json.node.util.SingleColumnReplaceOrAddNodeModel;

/**
 * This is the model implementation of StringToJSON. Converts {@link StringValue}s to {@link JSONValue}s.
 *
 * @author Gabor Bakos
 */
final class StringToJSONNodeModel extends SingleColumnReplaceOrAddNodeModel<StringToJSONSettings> {
    /**
     * Constructor for the node model.
     */
    protected StringToJSONNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        DataTableSpec[] res = super.configure(inSpecs);
        if (JSONCell.TYPE.isASuperTypeOf(inSpecs[getSettings().inputTableIndex()].getColumnSpec(
            getSettings().inputColumnName()).getType())) {
            String warningPrefix = getWarningMessage();
            if (warningPrefix == null) {
                warningPrefix = "";
            }
            if (!warningPrefix.isEmpty()) {
                warningPrefix += "\n";
            }
            setWarningMessage(warningPrefix + "The selected column (" + getSettings().inputColumnName()
                + ") is already a JSON column.");
        }
        return res;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CellFactory createCellFactory(final DataColumnSpec output, final int inputIndex, final int... otherColumns) {
        CellFactory factory = new SingleCellFactory(true, output) {
            @Override
            public DataCell getCell(final DataRow row) {
                DataCell cell = row.getCell(inputIndex);
                if (cell.isMissing()) {
                    return DataType.getMissingCell();
                }
                try {
                    if (cell instanceof JSONValue) {
                        return cell;
                    }
                    if (cell instanceof StringValue) {
                        StringValue sv = (StringValue)cell;
                        return JSONCellFactory.create(sv.getStringValue(), getSettings().isAllowComments());
                    }
                    if (getSettings().isFailOnError()) {
                        throw new IllegalStateException("Not supported type: " + cell.getType());
                    }
                    return DataType.getMissingCell();
                } catch (IOException | RuntimeException e) {
                    if (getSettings().isFailOnError()) {
                        throw (e instanceof RuntimeException ? (RuntimeException)e : new RuntimeException(e));
                    }
                    return new MissingCell(e.getMessage());
                }
            }
        };
        return factory;
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
        super.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.loadValidatedSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.validateSettings(settings);
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
     * {@inheritDoc}
     */
    @Override
    protected StringToJSONSettings createSettings() {
        return createStringToJSONSettings();
    }

    /**
     * @return A new {@link StringToJSONSettings}.
     */
    static StringToJSONSettings createStringToJSONSettings() {
        return new StringToJSONSettings();
    }
}
