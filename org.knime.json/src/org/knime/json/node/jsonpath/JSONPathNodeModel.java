package org.knime.json.node.jsonpath;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
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
import org.knime.core.data.date.DateAndTimeCell;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.json.JSONCellFactory;
import org.knime.core.data.json.JSONValue;
import org.knime.core.data.json.JacksonConversions;
import org.knime.core.data.vector.bytevector.DenseByteVectorCellFactory;
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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.mapper.MappingProvider;

/**
 * This is the model implementation of JSONPath. Selects certain paths from the selected JSON column.
 *
 * @author Gabor Bakos
 */
public class JSONPathNodeModel extends SingleColumnReplaceOrAddNodeModel<JSONPathSettings> {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(JSONPathNodeModel.class);

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
        Configuration jsonPathConfiguration = Activator.getInstance().getJsonPathConfiguration();
        List<Option> options = new ArrayList<>();
        if (getSettings().isReturnPaths()) {
            options.add(Option.AS_PATH_LIST);
        }
        options.add(Option.ALWAYS_RETURN_LIST);
        options.add(Option.DEFAULT_PATH_LEAF_TO_NULL);
        final Configuration config = jsonPathConfiguration.setOptions(options.toArray(new Option[0]));
        final MappingProvider mappingProvider = config.mappingProvider();
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
                        Object readObject;
                        if (config.jsonProvider().getClass().getName().contains("JacksonTree")) {
                            readObject = jsonPath.read(conv.toJackson(jsonValue), config);
                        } else {
                            readObject = jsonPath.read(jsonValue.toString(), config);
                        }
                        Iterable<?> read = config.jsonProvider().toIterable(readObject);
                        values = new ArrayList<>();
                        for (Object object : read) {
                            values.add(object);
                        }
                        List<DataCell> cells = new ArrayList<>();
                        OutputType returnType = getSettings().getReturnType();
                        if (getSettings().isReturnPaths()
                            || (returnType == OutputType.String && getSettings().isResultIsList())) {
                            for (Object v : values) {
                                if (v != null) {
                                    cells.add(new StringCell(v.toString()));
                                }
                            }
                            return CollectionCellFactory.createListCell(cells);
                        }
                        if (getSettings().isResultIsList()) {
                            return CollectionCellFactory.createListCell(cells);
                        }
                        if (getSettings().getOnMultipleResults().supportsConcatenate(returnType) && values.size() > 1) {
                            //Concatenate
                            JsonNodeFactory jsonNodeFactory = JsonNodeFactory.withExactBigDecimals(true);
                            switch (returnType) {
                                case Json:
                                    try {
                                        ArrayNode array = new ArrayNode(jsonNodeFactory);
                                        try {
                                            for (Object object : values) {
                                                if (object instanceof String) {
                                                    String str = (String)object;
                                                    array.add(str);
                                                } else if (object instanceof Boolean) {
                                                    Boolean bool = (Boolean)object;
                                                    array.add(bool);
                                                } else if (object instanceof Integer) {
                                                    Integer integer = (Integer)object;
                                                    array.add(integer);
                                                } else if (object instanceof Number) {
                                                    Number num = (Number)object;
                                                    array.add(num.doubleValue());
                                                } else if (object instanceof JsonNode) {
                                                    JsonNode node = (JsonNode)object;
                                                    array.add(node);
                                                } else if (object == null) {
                                                    array.addNull();
                                                } else {
                                                    array.addPOJO(object);
                                                }
                                            }
                                        } catch (RuntimeException e) {
                                            array.add(jsonNodeFactory.nullNode());
                                        }
                                        return JSONCellFactory.create(array.toString(), false);
                                    } catch (IOException | RuntimeException e) {
                                        return new MissingCell(e.getMessage());
                                    }
                                case String:
                                    StringBuilder builder = new StringBuilder("[");
                                    for (Object object : values) {
                                        builder.append(object).append(',');
                                    }
                                    if (builder.length() > 2) {
                                        builder.setCharAt(builder.length() - 1, ']');
                                    }
                                    return new StringCell(builder.toString());
                                default:
                                    getLogger().coding(
                                        "The result type " + returnType + " does not support concatenation.");
                                    throw new UnsupportedOperationException("Not supported: " + returnType);
                            }
                        } else {
                            for (final Object object : values) {
                                try {
                                    switch (returnType) {
                                        case Bool:
                                            Boolean bool =
                                                mappingProvider.map(object, Boolean.class, config);
                                            if (bool == null) {
                                                cells.add(DataType.getMissingCell());
                                            } else {
                                                cells.add(BooleanCell.get(bool.booleanValue()));
                                            }
                                            break;
                                        case DateTime:
                                            Date date = mappingProvider.map(object, Date.class, config);
                                            if (date == null) {
                                                cells.add(DataType.getMissingCell());
                                            } else {
                                                cells.add(new DateAndTimeCell(date.getTime(),
                                                    getSettings().isHasDate(), getSettings().isHasTime(), getSettings()
                                                        .isHasMillis()));
                                            }
                                            break;
                                        case Int:
                                            Integer integer =
                                                mappingProvider.map(object, Integer.class, config);
                                            if (integer == null) {
                                                cells.add(DataType.getMissingCell());
                                            } else {
                                                cells.add(new IntCell(integer.intValue()));
                                            }
                                            break;
                                        case Json:
                                            cells.add(object == null ? DataType.getMissingCell() : JSONCellFactory
                                                .create(object.toString(), false));
                                            break;
                                        case Real:
                                            Double d = mappingProvider.map(object, Double.class, config);
                                            if (d == null) {
                                                cells.add(DataType.getMissingCell());
                                            } else {
                                                cells.add(new DoubleCell(d.doubleValue()));
                                            }
                                            break;
                                        case String:
                                            cells.add(object == null ? DataType.getMissingCell() : new StringCell(
                                                object.toString()));
                                            break;
                                        case Binary:
                                            if (object == null) {
                                                cells.add(DataType.getMissingCell());
                                            } else {
                                                byte[] arr = (byte[])object;
                                                DenseByteVectorCellFactory factory = new DenseByteVectorCellFactory(arr.length);
                                                for(int i = arr.length; i-->0;) {
                                                    factory.setValue(i, arr[i] < 0 ? arr[i] + 256 : arr[i]);
                                                }
                                                cells.add(factory.createDataCell());
                                            }
                                        default:
                                            throw new UnsupportedOperationException("Unsupported return type: "
                                                + returnType);
                                    }
                                } catch (IOException | RuntimeException e) {
                                    cells.add(new MissingCell(e.getMessage()));
                                }
                            }
                        }
                        if (cells.size() > 1) {
                            switch (getSettings().getOnMultipleResults()) {
                                case Fail:
                                    throw new IllegalStateException("Expected at most one result, but got: "
                                        + cells.size() + " [" + cells + "]" + "\n   in row: " + row.getKey());
                                case First:
                                    return cells.get(0);
                                case Last:
                                    return cells.get(cells.size() - 1);
                                case Concatenate:
                                    throw new IllegalStateException("Should already be concatenated if supported.");
                                case Missing:
                                    return new MissingCell(cells.toString());
                                default:
                                    throw new UnsupportedOperationException(
                                        "Not supported on multiple results strategy: "
                                            + getSettings().getOnMultipleResults());
                            }
                        }
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
        final DataType type =
            getSettings().isReturnPaths() ? StringCell.TYPE : getSettings().getReturnType().getDataType();
        return new DataColumnSpecCreator(outputColName, getSettings().isResultIsList() || getSettings().isReturnPaths()
            ? ListCell.getCollectionType(type) : type).createSpec();
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
        return new JSONPathSettings(LOGGER);
    }
}
