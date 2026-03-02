/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   Mar 2, 2026 (lw): created
 */
package org.knime.json.node.tojson;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import org.knime.core.data.BooleanValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.LongValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.blob.BinaryObjectDataValue;
import org.knime.core.data.collection.CollectionDataValue;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.json.JSONCell;
import org.knime.core.data.json.JSONCellFactory;
import org.knime.core.data.json.JSONValue;
import org.knime.core.data.json.JacksonConversions;
import org.knime.core.data.vector.bytevector.ByteVectorValue;
import org.knime.core.node.InvalidSettingsException;
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
 * Was previously an inline, anonymous {@link SingleCellFactory} in the {@link ColumnsToJsonNodeModel}.
 * It was extracted for readability and maintainability.
 *
 * @author Gabor Bakos
 * @author Leon Wenzler, KNIME GmbH, Konstanz, Germany
 */
final class ColumnsToJsonCellFactory extends SingleCellFactory {

    private JacksonConversions m_conv = JacksonConversions.getInstance();

    private JsonNodeFactory m_nodeFactory = JacksonUtils.nodeFactory();

    //sparse, from column indices to output types.
    private Map<Integer, OutputType> m_types = new TreeMap<>();

    //sparse, from column indices to expected depths.
    private Map<Integer, Integer> m_depths = new TreeMap<>();

    private String[] m_dataBoundKeys;

    private int[] m_indices;

    private int m_keyIndex;

    private final ColumnsToJsonSettings m_settings;

    public ColumnsToJsonCellFactory(final DataTableSpec spec, final ColumnsToJsonSettings settings)
        throws InvalidSettingsException {
        super(new DataColumnSpecCreator(DataTableSpec.getUniqueColumnName(spec, settings.getOutputColumnName()), //
            JSONCell.TYPE).createSpec());
        m_settings = settings;

        final FilterResult result = m_settings.getDataBoundColumnsAutoConfiguration().applyTo(spec);
        final String[] includes = result.getIncludes();
        int customKeyDataBoundValueLength = m_settings.getDataBoundKeyColumns().length;
        m_indices = new int[customKeyDataBoundValueLength + includes.length];
        for (int i = customKeyDataBoundValueLength - 1; i >= 0; i--) {
            final var colName = m_settings.getDataBoundKeyColumns()[i];
            final var index = spec.findColumnIndex(colName);
            CheckUtils.checkSetting(index >= 0,
                "Could not determine the index of column \"%s\", it is not available anymore. "
                    + "Ensure that \"%s\" is present in the input.", colName, colName);
            m_indices[i] = index;
        }
        m_dataBoundKeys = new String[customKeyDataBoundValueLength + includes.length];
        System.arraycopy(m_settings.getDataBoundKeyNames(), 0, m_dataBoundKeys, 0, customKeyDataBoundValueLength);
        for (int i = customKeyDataBoundValueLength - 1; i >= 0; i--) {
            if (m_dataBoundKeys[i] == null || m_dataBoundKeys[i].isEmpty()) {
                m_dataBoundKeys[i] = m_settings.getDataBoundKeyColumns()[i];
            }
        }
        for (int i = includes.length - 1; i >= 0; i--) {
            final var colName = includes[i];
            final var index = spec.findColumnIndex(colName);
            CheckUtils.checkSetting(index >= 0,
                "Could not determine the index of column \"%s\", it is not available anymore. "
                    + "Ensure that \"%s\" is present in the input.", colName, colName);
            m_indices[customKeyDataBoundValueLength + i] = index;
            m_dataBoundKeys[customKeyDataBoundValueLength + i] = colName;
        }
        for (int i = m_indices.length - 1; i >= 0; i--) {
            final var colSpec = spec.getColumnSpec(m_indices[i]);
            Pair<OutputType, Integer> pair = ColumnsToJsonNodeModel.outputType(colSpec);
            CheckUtils.checkNotNull(pair.getFirst(),
                "Could not determine the output type (as JSON) for the column \"%s\". "
                    + "The input type \"%s\" may not be supported.", colSpec.getName(), colSpec.getType());
            m_types.put(m_indices[i], pair.getFirst());
            m_depths.put(m_indices[i], pair.getSecond());
        }

        m_keyIndex = spec.findColumnIndex(m_settings.getKeyNameColumn());
    }

    @Override
    public DataCell getCell(final DataRow row) {
        final var rootKey = switch (m_settings.getRootKeyType()) { // NOSONAR
            case Constant -> m_settings.getKeyName();
            case DataBound -> row.getCell(m_keyIndex).toString();
            case Unnamed -> null;
        };
        final var json = m_nodeFactory.objectNode();
        final var inner = json.objectNode();
        for (var i = 0; i < m_indices.length; ++i) {
            final var cell = row.getCell(m_indices[i]);
            final var key = m_dataBoundKeys[i];
            final var outputType = m_types.get(m_indices[i]);
            final var depth = m_depths.get(m_indices[i]);
            setValue(inner, cell, key, outputType, depth);
        }
        for (var i = 0; i < m_settings.getKeyNames().length; ++i) {
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
                    } else if (cell instanceof ByteVectorValue bvv) {
                        byte[] bytes = JsonPathUtils.toBytes(bvv);
                        json.put(key, bytes);
                    } else if (cell instanceof BinaryObjectDataValue bodv) {
                        try {
                            json.put(key, JsonPathUtils.toBytes(bodv));
                        } catch (IOException e) {
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
                case Long: // intentional fall-through
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
                final var cdv = (CollectionDataValue)cell;
                final var array = m_nodeFactory.arrayNode();
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
                        } else if (cell instanceof ByteVectorValue bvv) {
                            byte[] bytes = JsonPathUtils.toBytes(bvv);
                            array.add(bytes);
                        } else if (cell instanceof BinaryObjectDataValue bodv) {
                            try {
                                array.add(JsonPathUtils.toBytes(bodv));
                            } catch (IOException e) {
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
                    case Long: // intentional fall-through
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
                if (cell instanceof CollectionDataValue cdv) {
                    final var newArray = m_nodeFactory.arrayNode();
                    array.add(newArray);
                    setArrayValue(newArray, cdv, outputType, depth - 1);
                } else {
                    array.addNull();
                }
            }
        }
    }
}
