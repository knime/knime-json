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
 *   14 Sept. 2014 (Gabor): created
 */
package org.knime.json.node.jsonpath.multi;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.json.JsonValue;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.MissingCell;
import org.knime.core.data.blob.BinaryObjectCellFactory;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.BooleanCell.BooleanCellFactory;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
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
import org.knime.core.node.streamable.simple.SimpleStreamableFunctionNodeModel;
import org.knime.json.node.jsonpath.util.JsonPathUtils;
import org.knime.json.node.util.ErrorHandling;
import org.knime.json.util.OutputType;

import com.fasterxml.jackson.core.Base64Variants;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BinaryNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.github.fge.jackson.JacksonUtils;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.mapper.MappingProvider;

/**
 * This is the model implementation of JSONPath. Selects certain paths from the selected JSON column.
 *
 * @author Gabor Bakos
 */
public class JSONPathNodeModel extends SimpleStreamableFunctionNodeModel {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(JSONPathNodeModel.class);

    private final JSONPathSettings m_settings = new JSONPathSettings(LOGGER);

    /**
     * Constructor for the node model.
     */
    protected JSONPathNodeModel() {
        super();
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
        m_settings.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_settings.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        new JSONPathSettings(LOGGER).loadSettingsFrom(settings);
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
     * This method gets called when no column was selected.
     *
     * @param tableSpec The input table spec.
     * @return The autoguessed input column spec.
     * @throws InvalidSettingsException Could not find proper column.
     */
    protected DataColumnSpec handleNonSetColumn(final DataTableSpec tableSpec) throws InvalidSettingsException {
        List<String> compatibleCols = new ArrayList<>();
        for (DataColumnSpec c : tableSpec) {
            if (c.getType().isCompatible(JSONValue.class)) {
                compatibleCols.add(c.getName());
            }
        }
        if (compatibleCols.size() == 1) {
            // auto-configure
            m_settings.setInputColumnName(compatibleCols.get(0));
            return tableSpec.getColumnSpec(compatibleCols.get(0));
        } else if (compatibleCols.size() > 1) {
            // auto-guessing
            m_settings.setInputColumnName(compatibleCols.get(0));
            setWarningMessage("Auto guessing: using column \"" + compatibleCols.get(0) + "\".");
            return tableSpec.getColumnSpec(compatibleCols.get(0));
        } else {
            throw new InvalidSettingsException("No suitable (JSON) column found: " + tableSpec);
        }
    }

    /**
     * Based on the settings it creates the rearranger. <br/>
     * Override if you need more control on how to create the results.
     *
     * @param inSpecs The input table spec.
     * @return The {@link ColumnRearranger}.
     * @throws InvalidSettingsException When autoguessing of column name failed or problem during initialization of
     *             {@link ColumnRearranger}..
     */
    @Override
    protected ColumnRearranger createColumnRearranger(final DataTableSpec inSpecs) throws InvalidSettingsException {
        if (!m_settings.getOutputSettings().iterator().hasNext()) {
            throw new InvalidSettingsException("No JSON path was specified, please enter at least one expression!");
        }
        ColumnRearranger ret = new ColumnRearranger(inSpecs);
        String input = m_settings.getInputColumnName();
        if (input == null || !inSpecs.containsName(input)) {
            input = handleNonSetColumn(inSpecs).getName();
        }
        final int inputIndex = inSpecs.findColumnIndex(input);
        int index = 0;
        for (SingleSetting setting : m_settings.getOutputSettings()) {
            String outputColName = DataTableSpec.getUniqueColumnName(ret.createSpec(), setting.getNewColumnName());
            DataColumnSpec output = createOutputSpec(outputColName, index++);
            CellFactory factory;
            try {
                factory = createCellFactory(setting, output, inputIndex);
                ret.append(factory);
            } catch (RuntimeException e) {
                throw new InvalidSettingsException(e);
            }
        }
        if (m_settings.isRemoveInputColumn()) {
            ret.remove(input);
        }
        return ret;
    }

    /**
     * @param setting The settings to generate a single column.
     * @param output The output column's {@link DataColumnSpec}.
     * @param inputIndex The input column's index.
     * @return The {@link CellFactory} generating the column.
     */
    protected CellFactory createCellFactory(final SingleSetting setting, final DataColumnSpec output,
        final int inputIndex) {
        final JacksonConversions conv = JacksonConversions.getInstance();
        Configuration jsonPathConfiguration =
            Configuration.builder().build();
        List<Option> options = new ArrayList<>();
        if (setting.isReturnPaths()) {
            options.add(Option.AS_PATH_LIST);
        }
        options.add(Option.ALWAYS_RETURN_LIST);
        options.add(Option.DEFAULT_PATH_LEAF_TO_NULL);
        final Configuration config = jsonPathConfiguration.setOptions(options.toArray(new Option[0]));
        final MappingProvider mappingProvider = config.mappingProvider();
        final JsonPath jsonPath = JsonPath.compile(setting.getJsonPath());
        final OutputType returnType = setting.getReturnType();
        final boolean resultIsList = setting.isResultIsList();
        return new SingleCellFactory(true, output) {
            private Runnable m_setWarning =
                () -> setWarningMessage("Large value cannot be stored in an integer column");

            @Override
            public DataCell getCell(final DataRow row) {
                DataCell cell = row.getCell(inputIndex);
                if (cell instanceof JSONValue) {
                    JSONValue jsonCell = (JSONValue)cell;
                    JsonValue jsonValue = jsonCell.getJsonValue();
                    Object readObject;
                    try {
                        if (config.jsonProvider().getClass().getName().contains("JacksonJsonNode")) {
                            readObject = jsonPath.read(conv.toJackson(jsonValue), config);
                        } else {
                            readObject = jsonPath.read(jsonValue.toString(), config);
                        }
                    } catch (RuntimeException e) {
                        return new MissingCell(e.getMessage());
                    }
                    Iterable<?> read = config.jsonProvider().toIterable(readObject);
                    List<Object> values = new ArrayList<>();
                    for (Object object : read) {
                        values.add(object);
                    }
                    List<DataCell> cells = new ArrayList<>();
                    if (setting.isReturnPaths() || (returnType == OutputType.String && resultIsList)) {
                        for (Object v : values) {
                            if (v != null) {
                                cells.add(new StringCell(v.toString()));
                            }
                        }
                        return CollectionCellFactory.createListCell(cells);
                    }
                    if (!resultIsList && values.size() > 1) {
                        if (returnType == OutputType.Json) {
                            JsonNodeFactory nodeFactory = JacksonUtils.nodeFactory();
                            ArrayNode array = nodeFactory.arrayNode();
                            for (Object value : values) {
                                array.add(org.knime.json.node.jsonpath.JsonPathUtil.toJackson(
                                    nodeFactory, value));
                            }
                            return convertObjectToReturnType(array);
                        }
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
             * @param object An Object which should be converted to a JSONValue.
             * @return The converted cell.
             */
            private DataCell convertObjectToReturnType(final Object object) {
                try {
                    switch (returnType) {
                        case Boolean:
                            Boolean bool = mappingProvider.map(object, Boolean.class, config);
                            if (bool == null) {
                                return BooleanCellFactory.create(object.toString());
                            }
                            return BooleanCellFactory.create(bool.booleanValue());
                        case Integer:
                            if (JsonPathUtils.checkLongProblem(returnType, object, m_setWarning)) {
                                return new MissingCell("Value " + object + " is too large for an integer");
                            }

                            Integer integer = mappingProvider.map(object, Integer.class, config);
                            if (integer == null) {
                                return new IntCell(Integer.parseInt(object.toString()));
                            }
                            return new IntCell(integer.intValue());
                        case Long:
                            Long longVal = mappingProvider.map(object, Long.class, config);
                            if (longVal == null) {
                                return new LongCell(Long.parseLong(object.toString()));
                            }
                            return new LongCell(longVal.longValue());
                        case Json:
                            return asJson(object);
                        case Double:
                            Double d = mappingProvider.map(object, Double.class, config);
                            if (d == null) {
                                return DataType.getMissingCell();
                            }
                            return new DoubleCell(d.doubleValue());
                        case String:
                            return object == null ? DataType.getMissingCell() : new StringCell(object.toString());
                        case Base64:
                            if (object == null) {
                                return DataType.getMissingCell();
                            }
                            byte[] arr;
                            if (object instanceof byte[]) {
                                arr = (byte[])object;
                            } else if (object instanceof String) {
                                String str = (String)object;
                                arr = Base64Variants.getDefaultVariant().decode(str);
                            } else if (object instanceof BinaryNode) {
                                BinaryNode node = (BinaryNode)object;
                                arr = node.binaryValue();
                            } else {
                                throw new IllegalArgumentException("Unkown binary type: " + object.getClass());
                            }
                            return new BinaryObjectCellFactory().create(arr);
                        default:
                            throw new UnsupportedOperationException("Unsupported return type: " + returnType);
                    }
                } catch (RuntimeException | IOException e) {
                    return new MissingCell(e.getMessage());
                }
            }

            /**
             * @param object
             * @return The {@code object} as a JSON {@link DataCell}.
             */
            private DataCell asJson(final Object object) {
                if (object instanceof JsonNode) {
                    return JSONCellFactory.create(conv.toJSR353((JsonNode)object));
                }
                try {
                    return JSONCellFactory.create(conv.toJSR353(org.knime.json.node.jsonpath.JsonPathUtil.toJackson(
                        JacksonUtils.nodeFactory(), object)));
                } catch (RuntimeException e) {
                    return new MissingCell(e.getMessage());
                }
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
        return ErrorHandling.shorten(string, atMost);
    }

    /**
     * @param outputColName The name of the output column.
     * @param index The index (within the new columns) of the column to generate.
     * @return The {@link DataColumnSpec} of the output column.
     */
    protected DataColumnSpec createOutputSpec(final String outputColName, final int index) {
        SingleSetting outputSetting = m_settings.getOutputSetting(index);
        final DataType type =
            outputSetting.isReturnPaths() ? StringCell.TYPE : outputSetting.getReturnType().getDataType();
        return new DataColumnSpecCreator(outputColName, outputSetting.isResultIsList() || outputSetting.isReturnPaths()
            ? ListCell.getCollectionType(type) : type).createSpec();
    }

    /**
     * @return Default {@link JSONPathSettings}.
     */
    static JSONPathSettings createJSONPathProjectionSettings() {
        return new JSONPathSettings(LOGGER);
    }
}
