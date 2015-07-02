package org.knime.json.node.tojson;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.knime.core.data.BooleanValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.LongValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.blob.BinaryObjectDataValue;
import org.knime.core.data.collection.CollectionDataValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.json.JSONCell;
import org.knime.core.data.json.JSONCellFactory;
import org.knime.core.data.json.JSONValue;
import org.knime.core.data.json.JacksonConversions;
import org.knime.core.data.vector.bytevector.ByteVectorValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.streamable.simple.SimpleStreamableFunctionNodeModel;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.util.filter.NameFilterConfiguration.FilterResult;
import org.knime.core.util.Pair;
import org.knime.json.node.jsonpath.util.JsonPathUtils;
import org.knime.json.util.OutputType;
import org.knime.json.util.RootKeyType;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jackson.JacksonUtils;

/**
 * This is the model implementation of ColumnsToJson. Converts contents of columns to JSON values rowwise.
 *
 * @author Gabor Bakos
 */
public class ColumnsToJsonNodeModel extends SimpleStreamableFunctionNodeModel {
    private final ColumnsToJsonSettings m_settings = new ColumnsToJsonSettings();

    /**
     * Constructor for the node model.
     */
    protected ColumnsToJsonNodeModel() {
        super();
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
        new ColumnsToJsonSettings().loadSettingsModel(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ColumnRearranger createColumnRearranger(final DataTableSpec spec) throws InvalidSettingsException {
        if (m_settings.isNotConfigured()) {
            throw new InvalidSettingsException("The node is not configured yet");
        }
        ColumnRearranger ret = new ColumnRearranger(spec);
        final int keyIndex = spec.findColumnIndex(m_settings.getKeyNameColumn());
        if (m_settings.getRootKeyType() == RootKeyType.DataBound) {
            CheckUtils.checkSetting(keyIndex >= 0,
                "Unknown databound key column name: " + m_settings.getKeyNameColumn());
            CheckUtils.checkSetting(spec.getColumnSpec(keyIndex).getType().isCompatible(StringValue.class),
                "Wrong databound key column: " + m_settings.getKeyNameColumn());
        }
        ret.append(new SingleCellFactory(new DataColumnSpecCreator(DataTableSpec.getUniqueColumnName(spec,
            m_settings.getOutputColumnName()), JSONCell.TYPE).createSpec()) {
            private JacksonConversions m_conv = JacksonConversions.getInstance();

            private JsonNodeFactory m_nodeFactory = JacksonUtils.nodeFactory();

            //sparse, from column indices to output types.
            private Map<Integer, OutputType> m_types = new TreeMap<>();

            //sparse, from column indices to expected depths.
            private Map<Integer, Integer> m_depths = new TreeMap<>();

            private String[] m_dataBoundKeys;

            private int[] m_indices;
            {
                final FilterResult result = m_settings.getDataBoundColumnsAutoConfiguration().applyTo(spec);
                final String[] includes = result.getIncludes();
                int customKeyDataBoundValueLength = m_settings.getDataBoundKeyColumns().length;
                m_indices = new int[customKeyDataBoundValueLength + includes.length];
                for (int i = customKeyDataBoundValueLength; i-- > 0;) {
                    String colName = m_settings.getDataBoundKeyColumns()[i];
                    m_indices[i] = spec.findColumnIndex(colName);
                }
                m_dataBoundKeys = new String[customKeyDataBoundValueLength + includes.length];
                System.arraycopy(m_settings.getDataBoundKeyNames(), 0, m_dataBoundKeys, 0,
                    customKeyDataBoundValueLength);
                for (int i = customKeyDataBoundValueLength; i-->0;) {
                    if (m_dataBoundKeys[i] == null || m_dataBoundKeys[i].isEmpty()) {
                        m_dataBoundKeys[i] = m_settings.getDataBoundKeyColumns()[i];
                    }
                }
                for (int i = includes.length; i-- > 0;) {
                    m_indices[customKeyDataBoundValueLength + i] = spec.findColumnIndex(includes[i]);
                    m_dataBoundKeys[customKeyDataBoundValueLength + i] = includes[i];
                }
                for (int i = m_indices.length; i-- > 0;) {
                    Pair<OutputType, Integer> pair = outputType(spec.getColumnSpec(m_indices[i]));
                    if (pair.getFirst() == null) {
                        throw new IllegalStateException("Could not figure out the output type for "
                            + spec.getColumnSpec(m_indices[i]));
                    }
                    m_types.put(m_indices[i], pair.getFirst());
                    m_depths.put(m_indices[i], pair.getSecond());
                }
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public DataCell getCell(final DataRow row) {
                final String rootKey;
                switch (m_settings.getRootKeyType()) {
                    case Constant:
                        rootKey = m_settings.getKeyName();
                        break;
                    case DataBound:
                        rootKey = row.getCell(keyIndex).toString();
                        break;
                    case Unnamed:
                        rootKey = null;
                        break;
                    default:
                        throw new UnsupportedOperationException("Unknown strategy to set root: "
                            + m_settings.getRootKeyType());
                }
                ObjectNode json = m_nodeFactory.objectNode();
                ObjectNode inner = json.objectNode();
                for (int i = 0; i < m_indices.length; ++i) {
                    DataCell cell = row.getCell(m_indices[i]);
                    String key = m_dataBoundKeys[i];
                    OutputType outputType = m_types.get(m_indices[i]);
                    int depth = m_depths.get(m_indices[i]);
                    setValue(inner, cell, key, outputType, depth);
                }
                for (int i = 0; i < m_settings.getKeyNames().length; ++i) {
                    inner.put(m_settings.getKeyNames()[i], m_settings.getKeyValues()[i]);
                }
                if (m_settings.getRootKeyType() == RootKeyType.Unnamed) {
                    return JSONCellFactory.create(m_conv.toJSR353(inner));
                }
                json.set(rootKey, inner);
                return JSONCellFactory.create(m_conv.toJSR353(json));
            }

            /**
             * Sets a simple value at {@code depth == 0}, else delves deeper (missings and wrong types are represented
             * as {@code null}).
             *
             * @param json The output value.
             * @param cell The input.
             * @param key The key for the output.
             * @param outputType The expected output type.
             * @param depth The expected output depth.
             */
            private void setValue(final ObjectNode json, final DataCell cell, final String key,
                final OutputType outputType, final int depth) {
                if (depth == 0) {
                    switch (outputType) {
                        case Base64:
                            if (cell.isMissing()) {
                                json.set(key, m_nodeFactory.nullNode());
                            } if (cell instanceof ByteVectorValue) {
                                ByteVectorValue bvv = (ByteVectorValue)cell;
                                byte[] bytes = JsonPathUtils.toBytes(bvv);
                                json.put(key, bytes);
                            } else if (cell instanceof BinaryObjectDataValue) {
                                BinaryObjectDataValue bodv = (BinaryObjectDataValue)cell;
                                try {
                                    json.put(key, JsonPathUtils.toBytes(bodv));
                                } catch (IOException e) {
                                    //TODO throw new UncheckedIOException(e); or add null
                                    throw new RuntimeException(e.getMessage(), e);
                                }
                            } else {
                                json.set(key, m_nodeFactory.nullNode());
                            }
                            break;
                        case Boolean:
                            if (cell.isMissing() || !(cell instanceof BooleanValue)) {
                                json.set(key, m_nodeFactory.nullNode());
                            } else {
                                json.put(key, ((BooleanValue)cell).getBooleanValue());
                            }
                            break;
                        case Integer:
                            if (cell.isMissing() || !(cell instanceof LongValue)) {
                                json.set(key, m_nodeFactory.nullNode());
                            } else {
                                json.put(key, ((LongValue)cell).getLongValue());
                            }
                            break;
                        case Double:
                            if (cell.isMissing() || !(cell instanceof DoubleValue)) {
                                json.set(key, m_nodeFactory.nullNode());
                            } else {
                                json.put(key, ((DoubleValue)cell).getDoubleValue());
                            }
                            break;
                        case String:
                            if (cell.isMissing() || !(cell instanceof StringValue)) {
                                json.set(key, m_nodeFactory.nullNode());
                            } else {
                                json.put(key, ((StringValue)cell).getStringValue());
                            }
                            break;
                        case Json:
                            if (cell.isMissing() || !(cell instanceof JSONValue)) {
                                json.set(key, m_nodeFactory.nullNode());
                            } else {
                                json.set(key, m_conv.toJackson(((JSONValue)cell).getJsonValue()));
                            }
                            break;
                        default:
                            throw new UnsupportedOperationException("Not supported output type: " + outputType);
                    }
                } else {
                    if (cell.isMissing() || !(cell instanceof CollectionDataValue)) {
                        json.set(key, m_nodeFactory.nullNode());
                    } else {
                        CollectionDataValue cdv = (CollectionDataValue)cell;
                        ArrayNode array = m_nodeFactory.arrayNode();
                        json.set(key, array);
                        setArrayValue(array, cdv, outputType, depth - 1);
                    }
                }
            }

            /**
             * Sets the array value at {@code depth == 0}, else goes down (missings, invalids are represented as
             * {@code null}).
             *
             * @param array An {@link ArrayNode}.
             * @param dataCell The {@link DataValue} to transform.
             * @param outputType Expected output type.
             * @param depth The expected depth.
             */
            private void setArrayValue(final ArrayNode array, final CollectionDataValue dataCell,
                final OutputType outputType, final int depth) {
                for (DataCell cell : dataCell) {
                    if (cell.isMissing()) {
                        array.addNull();
                    } else if (depth == 0) {
                        switch (outputType) {
                            case Base64:
                                if (cell.isMissing()) {
                                    array.add(m_nodeFactory.nullNode());
                                } if (cell instanceof ByteVectorValue) {
                                    ByteVectorValue bvv = (ByteVectorValue)cell;
                                    byte[] bytes = JsonPathUtils.toBytes(bvv);
                                    array.add(bytes);
                                } else if (cell instanceof BinaryObjectDataValue) {
                                    BinaryObjectDataValue bodv = (BinaryObjectDataValue)cell;
                                    try {
                                        array.add(JsonPathUtils.toBytes(bodv));
                                    } catch (IOException e) {
                                        //TODO throw new UncheckedIOException(e); or add null
                                        throw new RuntimeException(e.getMessage(), e);
                                    }
                                } else {
                                    array.add(m_nodeFactory.nullNode());
                                }
                                break;
                            case Boolean:
                                if (cell.isMissing() || !(cell instanceof BooleanValue)) {
                                    array.add(m_nodeFactory.nullNode());
                                } else {
                                    array.add(((BooleanValue)cell).getBooleanValue());
                                }
                                break;
                            case Integer:
                                if (cell.isMissing() || !(cell instanceof LongValue)) {
                                    array.add(m_nodeFactory.nullNode());
                                } else {
                                    array.add(((LongValue)cell).getLongValue());
                                }
                                break;
                            case Json:
                                if (cell.isMissing() || !(cell instanceof JSONValue)) {
                                    array.add(m_nodeFactory.nullNode());
                                } else {
                                    array.add(m_conv.toJackson(((JSONValue)cell).getJsonValue()));
                                }
                                break;
                            case Double:
                                if (cell.isMissing() || !(cell instanceof DoubleValue)) {
                                    array.add(m_nodeFactory.nullNode());
                                } else {
                                    array.add(((DoubleValue)cell).getDoubleValue());
                                }
                                break;
                            case String:
                                if (cell.isMissing() || !(cell instanceof StringValue)) {
                                    array.add(m_nodeFactory.nullNode());
                                } else {
                                    array.add(((StringValue)cell).getStringValue());
                                }
                                break;
                            default:
                                throw new UnsupportedOperationException("Unknown type: " + outputType);
                        }
                    } else {
                        if (cell instanceof CollectionDataValue) {
                            CollectionDataValue cdv = (CollectionDataValue)cell;
                            ArrayNode newArray = m_nodeFactory.arrayNode();
                            array.add(newArray);
                            setArrayValue(newArray, cdv, outputType, depth - 1);
                        } else {
                            array.addNull();
                        }
                    }
                }
            }
        });
        if (m_settings.isRemoveSourceColumns()) {
            final Set<String> colsToRemove = new HashSet<>();
            if (m_settings.getRootKeyType() == RootKeyType.DataBound) {
                colsToRemove.add(m_settings.getKeyNameColumn());
            }
            for (String col : m_settings.getDataBoundKeyColumns()) {
                colsToRemove.add(col);
            }
            for (String col : m_settings.getDataBoundColumnsAutoConfiguration().applyTo(spec).getIncludes()) {
                colsToRemove.add(col);
            }
            for (String col : colsToRemove) {
                ret.remove(col);
            }
        }
        return ret;
    }

    /**
     * @param columnSpec
     * @return A pair of {@link OutputType} and depth of nested collections.
     * @throws UnsupportedOperationException when not supported type was selected.
     */
    protected Pair<OutputType, Integer> outputType(final DataColumnSpec columnSpec) {
        return outputType(columnSpec.getType(), 0);
    }

    /**
     * Collects the output type with the depth of {@link CollectionDataValue}s.
     *
     * @param type A possibly {@link CollectionDataValue}'s {@link DataType}.
     * @param depth The previous depth of nesting.
     * @return The {@link Pair} of {@link OutputType} and the depth of nesting.
     */
    private Pair<OutputType, Integer> outputType(final DataType type, final int depth) {
        if (type.isCollectionType()) {
            return outputType(type.getCollectionElementType(), depth + 1);
        }
        OutputType oType;
        if (type.isCompatible(BooleanValue.class)) {
            oType = OutputType.Boolean;
        } else if (type.isCompatible(LongValue.class)) {
            oType = OutputType.Integer;
        } else if (type.isCompatible(DoubleValue.class)) {
            oType = OutputType.Double;
        } else if (type.isCompatible(JSONValue.class)) {
            oType = OutputType.Json;
        } else if (type.isCompatible(StringValue.class)) {
            oType = OutputType.String;
        } else if (type.isCompatible(ByteVectorValue.class)) {
            oType = OutputType.Base64;
        } else if (type.isCompatible(BinaryObjectDataValue.class)) {
            oType = OutputType.Base64;
        } else {
            throw new UnsupportedOperationException("Not supported type: " + type);
        }
        return Pair.create(oType, depth);
    }
}
