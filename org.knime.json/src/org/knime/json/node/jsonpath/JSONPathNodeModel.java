package org.knime.json.node.jsonpath;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
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
import com.fasterxml.jackson.databind.node.BigIntegerNode;
import com.fasterxml.jackson.databind.node.BinaryNode;
import com.fasterxml.jackson.databind.node.DecimalNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.TextNode;
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
        final JacksonConversions conv = JacksonConversions.getInstance();
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
        final OutputType returnType = getSettings().getReturnType();
        final boolean resultIsList = getSettings().isResultIsList();
        return new SingleCellFactory(output) {
            @Override
            public DataCell getCell(final DataRow row) {
                DataCell cell = row.getCell(inputIndex);
                if (cell instanceof JSONValue) {
                    JSONValue jsonCell = (JSONValue)cell;
                    JsonValue jsonValue = jsonCell.getJsonValue();
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
                    if (getSettings().isReturnPaths() || (returnType == OutputType.String && resultIsList)) {
                        for (Object v : values) {
                            if (v != null) {
                                cells.add(new StringCell(v.toString()));
                            }
                        }
                        return CollectionCellFactory.createListCell(cells);
                    }
                    if (!resultIsList && values.size() > 1) {
                        throw new IllegalStateException("Expected at most one result, but got: " + values.size() + " ["
                            + shorten(values.toString()) + "]" + "\n   in row: " + row.getKey());
                    } else {
                        for (final Object object : values) {
                            cells.add(convertObjectToReturnType(object));
                        }
                    }
                    if (resultIsList) {
                        return CollectionCellFactory.createListCell(cells);
                    }
                    //At most one element
                    if (cells.size() == 1) {
                        return cells.get(0);
                    }
                }
                return DataType.getMissingCell();
            }

            /**
             * @param object
             * @return
             */
            private DataCell convertObjectToReturnType(final Object object) {
                try {
                    switch (returnType) {
                        case Bool:
                            Boolean bool = mappingProvider.map(object, Boolean.class, config);
                            if (bool == null) {
                                return BooleanCell.get(Boolean.parseBoolean(object.toString()));
                            }
                            return BooleanCell.get(bool.booleanValue());
                        case Int:
                            Integer integer = mappingProvider.map(object, Integer.class, config);
                            if (integer == null) {
                                return new IntCell(Integer.parseInt(object.toString()));
                            }
                            return new IntCell(integer.intValue());
                        case Json:
                            return asJson(object);
                        case Real:
                            Double d = mappingProvider.map(object, Double.class, config);
                            if (d == null) {
                                return DataType.getMissingCell();
                            }
                            return new DoubleCell(d.doubleValue());
                        case String:
                            return object == null ? DataType.getMissingCell() : new StringCell(object.toString());
                        case Binary:
                            if (object == null) {
                                return DataType.getMissingCell();
                            }
                            byte[] arr = (byte[])object;
                            DenseByteVectorCellFactory factory = new DenseByteVectorCellFactory(arr.length);
                            for (int i = arr.length; i-- > 0;) {
                                factory.setValue(i, arr[i] < 0 ? arr[i] + 256 : arr[i]);
                            }
                            return factory.createDataCell();
                        default:
                            throw new UnsupportedOperationException("Unsupported return type: " + returnType);
                    }
                } catch (IOException | RuntimeException e) {
                    return new MissingCell(e.getMessage());
                }
            }

            /**
             * @param object
             * @return
             * @throws IOException
             */
            private DataCell asJson(final Object object) throws IOException {
                if (object instanceof JsonNode) {
                    return JSONCellFactory.create(conv.toJSR353((JsonNode)object));
                }
                if (object instanceof String) {
                    String str = (String)object;
                    return JSONCellFactory.create(conv.toJSR353(new TextNode(str)));
                }
                if (object instanceof Number) {
                    Number num = (Number)object;
                    if (num instanceof Short || num instanceof Integer || num instanceof Long
                        || num instanceof BigInteger) {
                        //integral
                        return JSONCellFactory
                            .create(conv.toJSR353(new BigIntegerNode(new BigInteger(num.toString()))));
                    }
                    try {
                        return JSONCellFactory.create(conv.toJSR353(new DecimalNode(new BigDecimal(num.toString()))));
                    } catch (NumberFormatException e) {
                        //Probably NaN or Infinity
                        return JSONCellFactory.create(conv.toJSR353(new DoubleNode(num.doubleValue())));
                    }
                }
                if (object instanceof byte[]) {
                    byte[] bs = (byte[])object;
                    return JSONCellFactory.create(conv.toJSR353(new BinaryNode(bs)));
                }
                if (object instanceof Boolean) {
                    Boolean b = (Boolean)object;
                    return JSONCellFactory.create(Boolean.toString(b), false);
                }
                return object == null ? DataType.getMissingCell() : JSONCellFactory.create(object.toString(), false);
            }
        };
    }

    /**
     * @param string A possibly long {@link String}.
     * @return The shortened (at most 33 character) long version of {@code string}.
     */
    protected String shorten(final String string) {
        return shorten(string, 33);
    }
    /**
     * @param string A possibly long {@link String}.
     * @param atMost The longest possible returned {@link String}.
     * @return The shortened (at most {@code atMost} character) long version of {@code string}.
     */
    protected String shorten(final String string, final int atMost) {
        return string == null ? "null" : (string.length() > atMost) ? string.substring(0, atMost / 2) + "\u2026"
            + string.substring(string.length() - atMost / 2) : string;
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
