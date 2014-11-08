package org.knime.json.node.jsonpointer;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;

import javax.json.JsonValue;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.data.MissingCell;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.date.DateAndTimeCell;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.json.JSONCellFactory;
import org.knime.core.data.json.JSONValue;
import org.knime.core.data.json.JacksonConversions;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.json.internal.Activator;
import org.knime.json.node.util.OutputType;
import org.knime.json.node.util.SingleColumnReplaceOrAddNodeModel;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.jsonpointer.JsonPointer;
import com.github.fge.jackson.jsonpointer.JsonPointerException;

/**
 * This is the model implementation of JSONPointer. Selects certain pointers from the selected JSON column.
 *
 * @author Gabor Bakos
 */
public class JSONPointerNodeModel extends SingleColumnReplaceOrAddNodeModel<JSONPointerSettings> {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(JSONPointerNodeModel.class);
    /**
     * Constructor for the node model.
     */
    protected JSONPointerNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // No internal state
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
        // No internal state
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
        // No internal state
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected CellFactory createCellFactory(final DataColumnSpec output, final int inputIndex,
        final int... otherColumns) {
        final JacksonConversions conv = Activator.getInstance().getJacksonConversions();
        final JsonPointer pointer;
        try {
            pointer = new JsonPointer(getSettings().getJsonPointer());
        } catch (JsonPointerException e) {
            throw new IllegalStateException("Invalid pointer: " + e.getMessage(), e);
        }
        final OutputType returnType = getSettings().getReturnType();
        return new SingleCellFactory(output) {

            @Override
            public DataCell getCell(final DataRow row) {
                DataCell cell = row.getCell(inputIndex);
                if (cell instanceof JSONValue) {
                    JSONValue jsonCell = (JSONValue)cell;
                    JsonValue jsonValue = jsonCell.getJsonValue();
                    try {
                        JsonNode value = pointer.path(conv.toJackson(jsonValue));
                        if (value.isMissingNode()) {
                            return DataType.getMissingCell();
                        }
                        switch (returnType) {
                            case Bool:
                                return BooleanCell.get(value.asBoolean());
                            case Int:
                                return new IntCell(value.asInt());
                            case DateTime:
                                return new DateAndTimeCell(DateFormat.getInstance().parse(value.asText()).getTime(), getSettings().isHasDate(), getSettings().isHasTime(), getSettings().isHasMillis());
                            case Json:
                                return JSONCellFactory.create(conv.toJSR353(value));
                            case Real:
                                return new DoubleCell(value.asDouble());
                            case String:
                                return new StringCell(value.toString());
                            default:
                                throw new UnsupportedOperationException("Not supported return type: " + returnType);
                        }
                    } catch (ParseException | RuntimeException e) {
                        return new MissingCell(e.getMessage());
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
    protected DataColumnSpec createOutputSpec(final String outputColName) {
        return new DataColumnSpecCreator(outputColName, getSettings().getReturnType().getDataType()).createSpec();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected JSONPointerSettings createSettings() {
        return createJSONPathProjectionSettings();
    }

    /**
     * @return
     */
    static JSONPointerSettings createJSONPathProjectionSettings() {
        return new JSONPointerSettings(LOGGER);
    }
}
