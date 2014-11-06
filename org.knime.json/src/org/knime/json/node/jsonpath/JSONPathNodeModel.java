package org.knime.json.node.jsonpath;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.json.JsonValue;

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

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;

/**
 * This is the model implementation of JSONPath. Selects certain paths from the selected JSON column.
 *
 * @author Gabor Bakos
 */
public class JSONPathNodeModel extends SingleColumnReplaceOrAddNodeModel<JSONPathSettings> {
    /**
     * Constructor for the node model.
     */
    protected JSONPathNodeModel() {
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
        final JsonPath jsonPath = JsonPath.compile(getSettings().getJsonPath());
        return new SingleCellFactory(output) {

            @Override
            public DataCell getCell(final DataRow row) {
                DataCell cell = row.getCell(inputIndex);
                if (cell instanceof JSONValue) {
                    JSONValue jsonCell = (JSONValue)cell;
                    JsonValue jsonValue = jsonCell.getJsonValue();
                    try {
                        List<Object> values;
                        Configuration jsonPathConfiguration = Activator.getInstance().getJsonPathConfiguration();
                        Object read0;
                        if (jsonPathConfiguration.jsonProvider().getClass().getName().contains("JacksonTree")) {
                            read0 = jsonPath.read(conv.toJackson(jsonValue), jsonPathConfiguration);
                        } else {
                            read0 = jsonPath.read(jsonValue.toString(), jsonPathConfiguration);
                        }
                        Iterable<?> read = jsonPathConfiguration.jsonProvider().toIterable(read0);
                        values = new ArrayList<>();
                        for (Object object : read) {
                            values.add(object);
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
    protected JSONPathSettings createSettings() {
        return createJSONPathProjectionSettings();
    }

    /**
     * @return
     */
    static JSONPathSettings createJSONPathProjectionSettings() {
        return new JSONPathSettings();
    }
}
