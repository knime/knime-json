package org.knime.json.node.patch.create;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.json.JSONCellFactory;
import org.knime.core.data.json.JSONValue;
import org.knime.core.data.json.JacksonConversions;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.json.internal.Activator;
import org.knime.json.node.util.SingleColumnReplaceOrAddNodeModel;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonpatch.diff.JsonDiff;

/**
 * This is the model implementation of JSONDiff. Compares {@link JSONValue}s.
 *
 * @author Gabor Bakos
 */
public final class JSONPatchCreateNodeModel extends SingleColumnReplaceOrAddNodeModel<JSONPatchCreateSettings> {

    /**
     * Constructor for the node model.
     */
    protected JSONPatchCreateNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     *
     * @throws IOException When cannot read JSON values.
     */
    @Override
    protected CellFactory createCellFactory(final DataColumnSpec output, final int inputIndex,
        final int... otherColumns) throws IOException {
        final int rightIndex = otherColumns[0];
        final JacksonConversions conv = Activator.getInstance().getJacksonConversions();
        //final JsonPatch patch = JsonPatch.fromJson(patchNode);
        return new SingleCellFactory(output) {
            @Override
            public DataCell getCell(final DataRow row) {
                DataCell cell = row.getCell(inputIndex);
                if (cell instanceof JSONValue) {
                    JSONValue jsonCell = (JSONValue)cell;
                    JsonNode jsonNode = conv.toJackson(jsonCell.getJsonValue());
                    DataCell otherCol = row.getCell(rightIndex);
                    if (otherCol instanceof JSONValue) {
                        JSONValue otherJsonValue = (JSONValue)otherCol;
                        JsonNode other = conv.toJackson(otherJsonValue.getJsonValue());
                        JsonNode diff = JsonDiff.asJson(jsonNode, other);
                        return JSONCellFactory.create(conv.toJSR353(diff));
                    }
                }
                return DataType.getMissingCell();
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        DataTableSpec[] res = super.configure(inSpecs);
        if (getSettings().getInputColumnRight().isEmpty()) {
            getSettings().setInputColumnRight(handleNonSetColumn(inSpecs[0]).getName());
        }
        String warningPrefix = getWarningMessage();
        if (warningPrefix == null) {
            warningPrefix = "";
        }
        if (!warningPrefix.isEmpty()) {
            warningPrefix += "\n";
        }
        if (getSettings().getInputColumnRight().equals(getSettings().getInputColumnLeft())) {
            setWarningMessage(warningPrefix + "The same column (" + getSettings().getInputColumnLeft()
                + ") was selected both for the source and target columns.");
        }
        return res;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int[] findOtherIndices(final DataTableSpec inSpecs) {
        return new int[]{inSpecs.findColumnIndex(getSettings().getInputColumnRight())};
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
    protected JSONPatchCreateSettings createSettings() {
        return createJSONPatchCreateSettings();
    }

    /**
     * @return
     */
    static JSONPatchCreateSettings createJSONPatchCreateSettings() {
        return new JSONPatchCreateSettings();
    }
}
