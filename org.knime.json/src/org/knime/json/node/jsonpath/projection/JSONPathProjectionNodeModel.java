package org.knime.json.node.jsonpath.projection;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.data.MissingCell;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.StringCell;
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
import com.github.fge.jackson.jsonpointer.JsonPointer;
import com.github.fge.jackson.jsonpointer.JsonPointerException;
import com.jayway.jsonpath.JsonPath;

/**
 * This is the model implementation of JSONPathProjection. Selects certain paths from the selected JSON column.
 *
 * @author Gabor Bakos
 */
public class JSONPathProjectionNodeModel extends SingleColumnReplaceOrAddNodeModel<JSONPathProjectionSettings> {
    /**
     * Constructor for the node model.
     */
    protected JSONPathProjectionNodeModel() {
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
        final JsonPath jsonPath;
        final String pathType = getSettings().getPathType();
        switch (pathType) {
            case JSONPathProjectionSettings.JSON_PATH_OPTION:
                jsonPath = JsonPath.compile(getSettings().getJsonPath());
                pointer = null;
                break;
            case JSONPathProjectionSettings.JSON_POINTER_OPTION:
                jsonPath = null;
                try {
                    pointer = new JsonPointer(getSettings().getJsonPath());
                } catch (JsonPointerException e) {
                    throw new IllegalStateException("Invalid pointer: " + e.getMessage(), e);
                }
                break;
            default:
                throw new IllegalStateException("Not supported path format: " + pathType);
        }
        return new SingleCellFactory(output) {

            @Override
            public DataCell getCell(final DataRow row) {
                DataCell cell = row.getCell(inputIndex);
                if (cell instanceof JSONValue) {
                    JSONValue jsonCell = (JSONValue)cell;
                    Object jsonValue = jsonCell.getJsonValue();
                    try {
                        List<Object> values;
                        if (jsonPath == null) {
                            if (pointer == null) {
                                throw new IllegalStateException("Pointer should not be null at this point.");
                            }
                            JsonNode value = pointer.path(conv.toJackson(jsonCell.getJsonValue()));
                            values = Collections.<Object> singletonList(value);
                        } else {
                            values =
                                jsonPath.read(jsonValue.toString(), Activator.getInstance().getJsonPathConfiguration());
                        }
                        List<DataCell> cells = new ArrayList<>();
                        for (Object v : values) {
                            if (v != null) {
                                cells.add(new StringCell(v.toString()));
                            }
                        }
                        return CollectionCellFactory.createListCell(cells);
                    } catch (RuntimeException e) {
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
        return new DataColumnSpecCreator(outputColName, ListCell.getCollectionType(StringCell.TYPE)).createSpec();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected JSONPathProjectionSettings createSettings() {
        return createJSONPathProjectionSettings();
    }

    /**
     * @return
     */
    static JSONPathProjectionSettings createJSONPathProjectionSettings() {
        return new JSONPathProjectionSettings();
    }
}
