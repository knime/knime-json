package org.knime.json.node.jsonpath.dict;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import javax.json.JsonValue;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.MissingCell;
import org.knime.core.data.RowKey;
import org.knime.core.data.StringValue;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.collection.SetCell;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.json.JSONValue;
import org.knime.core.data.json.JacksonConversions;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.json.node.jsonpath.JsonPathUtil;
import org.knime.json.node.jsonpath.util.JsonPathUtils;
import org.knime.json.node.util.ErrorHandling;
import org.knime.json.util.OutputType;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.github.fge.jackson.JacksonUtils;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;

/**
 * This is the model implementation of JSONPathDict.
 * Collect parts of JSON documents based on JSONPath values specified in the second input port.
 *
 * @author Gabor Bakos
 */
@SuppressWarnings("restriction")
public class JSONPathDictNodeModel extends NodeModel {
    /**
     * The key for the remove or not the source/input column boolean value ({@code true} means remove).
     */
    static final String REMOVE_SOURCE = "remove.input.column";

    private static final boolean DEFAULT_REMOVE_SOURCE = false;

    /**
     * The type meaning return paths instead of the values.
     */
    private static final String PATH = "Path";
    /**
     * Index of dictionary table.
     */
    static final int DICT_TABLE = 1;
    /**
     * Index of JSON input table.
     */
    static final int INPUT_TABLE = 0;
    private SettingsModelString m_inputColumn = createInputColumn();
    private SettingsModelString m_pathColumn = createPathColumn();
    private SettingsModelString m_typeColumn = createTypeColumn();
    private SettingsModelString m_outputColumn = createOutputColumn();
    private SettingsModelBoolean m_removeSourceColumn = createRemoveSourceColumn();
    //private SettingsModelBoolean m_onInvalidFail = createOnInvalidFail();

    /**
     * Constructor for the node model.
     */
    protected JSONPathDictNodeModel() {
        super(2, 1);
    }

    /**
     * @return
     */
    static SettingsModelBoolean createRemoveSourceColumn() {
        return new SettingsModelBoolean(REMOVE_SOURCE, DEFAULT_REMOVE_SOURCE);
    }

    /**
     * @return {@link SettingsModelBoolean} for failing on invalid input.
     */
    static SettingsModelBoolean createOnInvalidFail() {
        return new SettingsModelBoolean("onInvalidFail", false);
    }

    /**
     * @return {@link SettingsModelString} for the output column name (in dictionary table).
     */
    static SettingsModelString createOutputColumn() {
        return new SettingsModelString("output", "");
    }

    /**
     * @return {@link SettingsModelString} for the type column (in dictionary table).
     */
    static SettingsModelString createTypeColumn() {
        return new SettingsModelString("type", "");
    }

    /**
     * @return {@link SettingsModelString} for the path column (in dictionary table).
     */
    static SettingsModelString createPathColumn() {
        return new SettingsModelString("path", "");
    }

    /**
     * @return {@link SettingsModelString} for the input (JSON) column.
     */
    static SettingsModelString createInputColumn() {
        return new SettingsModelString("input", "");
    }

    /**
     * {@inheritDoc}
     * @throws CanceledExecutionException Cancel called.
     * @throws InvalidSettingsException Wrong column name in the dictionary table.
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws CanceledExecutionException, InvalidSettingsException {
        final ColumnRearranger rearranger = new ColumnRearranger(inData[0].getSpec());
        final JacksonConversions conv = JacksonConversions.getInstance();
        final int inputIdx = inData[INPUT_TABLE].getSpec().findColumnIndex(m_inputColumn.getStringValue());
        int pathIdx = inData[DICT_TABLE].getSpec().findColumnIndex(m_pathColumn.getStringValue());
        int typeIdx = inData[DICT_TABLE].getSpec().findColumnIndex(m_typeColumn.getStringValue());
        int outputIdx = inData[DICT_TABLE].getSpec().findColumnIndex(m_outputColumn.getStringValue());
        double allDict = inData[DICT_TABLE].getRowCount(), allRows = inData[INPUT_TABLE].getRowCount();
        double fraction = allDict + allRows == 0 ? 1 : allDict / (allDict * (1 + allRows));
        ExecutionMonitor init = exec.createSubProgress(fraction);
        int i = 0;
        for (DataRow dictRow : inData[DICT_TABLE]) {
            init.checkCanceled();
            init.setProgress(i++/allDict, dictRow.getKey().getString());
            DataCell pathCell = dictRow.getCell(pathIdx);
            DataCell typeCell = dictRow.getCell(typeIdx);
            DataCell outputCell = dictRow.getCell(outputIdx);
            if (outputCell instanceof StringValue) {
                StringValue outputValue = (StringValue)outputCell;
                final JsonPath jsonPath;
                if (pathCell instanceof StringValue) {
                    StringValue pathValue = (StringValue)pathCell;
                    jsonPath = JsonPath.compile(pathValue.getStringValue());
                } else {
                    throw new IllegalStateException("Path is not String: " + pathCell + " (" + pathCell.getType() + ")");
                }

                DataTableSpec spec = rearranger.createSpec();
                String name = DataTableSpec.getUniqueColumnName(spec, outputValue.getStringValue());
                final boolean returnList, returnSet;
                DataType type;
                final OutputType outputType;
                final Configuration config;
                if (typeCell instanceof StringValue) {
                    StringValue typeValue = (StringValue)typeCell;
                    String t = typeValue.getStringValue();
                    EnumSet<Option> options = EnumSet.of(Option.ALWAYS_RETURN_LIST, Option.DEFAULT_PATH_LEAF_TO_NULL);
                    if (t.contains(PATH)) {
                        options.add(Option.AS_PATH_LIST);
                    }
                    config = Configuration.builder().options(options).build();
                    if (t.startsWith("List(")) {
                        t = t.substring("List(".length(), t.length() - 1);
                        outputType = computeOutputType(t, i, dictRow.getKey());
                        type = ListCell.getCollectionType(outputType.getDataType());
                        returnList = true;
                        returnSet = false;
                    } else if (t.startsWith("Set(")) {
                        t = t.substring("Set(".length(), t.length() - 1);
                        outputType = computeOutputType(t, i, dictRow.getKey());
                        type = SetCell.getCollectionType(outputType.getDataType());
                        returnSet = true;
                        returnList = false;
                    } else {
                        outputType = computeOutputType(t, i, dictRow.getKey());
                        type = outputType.getDataType();
                        returnList = false;
                        returnSet = false;
                    }
                } else {
                    throw new IllegalStateException("Output type is not String: " + typeCell + " (" + typeCell.getType() + ")");
                }
                rearranger.append(new SingleCellFactory(new DataColumnSpecCreator(name, type).createSpec()) {
                    @Override
                    public DataCell getCell(final DataRow row) {
                        DataCell cell = row.getCell(inputIdx);
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
                            if (!(returnList || returnSet) && values.size() > 1) {
                                if (outputType == OutputType.Json) {
                                    JsonNodeFactory nodeFactory = JacksonUtils.nodeFactory();
                                    ArrayNode array = nodeFactory.arrayNode();
                                    for (Object value : values) {
                                        array.add(JsonPathUtil.toJackson(
                                            nodeFactory, value));
                                    }
                                    return JsonPathUtils.convertObjectToReturnType(array, outputType, config, conv);
                                }
                                throw new IllegalStateException("Expected at most one result, but got: " + values.size() + " ["
                                    + ErrorHandling.shorten(values.toString(), 33) + "]" + "\n   in row: " + row.getKey());
                            } else {
                                for (final Object object : values) {
                                    cells.add(JsonPathUtils.convertObjectToReturnType(object, outputType, config, conv));
                                }
                            }
                            if (returnList) {
                                return CollectionCellFactory.createListCell(cells);
                            }
                            if (returnSet) {
                                return CollectionCellFactory.createSetCell(cells);
                            }
                            //At most one element
                            if (cells.size() == 1) {
                                return cells.get(0);
                            }
                        }
                        return DataType.getMissingCell();
                    }

                }
                    );
            } else {
                throw new InvalidSettingsException("Output column name is not valid: " + outputCell + " (" + outputCell.getType() + ")");
            }
        }
        if (m_removeSourceColumn.getBooleanValue()) {
            rearranger.remove(inputIdx);
        }
        final ExecutionContext rest = exec.createSubExecutionContext(1-fraction);
        return new BufferedDataTable[]{rest.createColumnRearrangeTable(inData[INPUT_TABLE], rearranger, exec)};
    }

    /**
     * @param type The single type definition as {@link String}.
     * @param rowIndex The row's position.
     * @param rowKey The key of the row which might contain problem.
     * @return The {@link OutputType} specified by that definition.
     * @throws IllegalArgumentException if {@link OutputType} has
     *         no constant with the specified name
     */
    private OutputType computeOutputType(final String type, final int rowIndex, final RowKey rowKey) {
        try {
            return type.equals(PATH) ? OutputType.String : OutputType.valueOf(type);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Wrong output type: " + type + " in row " + rowIndex + " ("
                + rowKey.getString() + ")");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        //No internal state
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        if (null == m_inputColumn.getStringValue()) {
            List<String> compatibleCols = new ArrayList<String>();
            for (DataColumnSpec c : inSpecs[INPUT_TABLE]) {
                if (c.getType().isCompatible(JSONValue.class)) {
                    compatibleCols.add(c.getName());
                }
            }
            if (compatibleCols.size() == 1) {
                // auto-configure
                m_inputColumn.setStringValue(compatibleCols.get(0));
            } else if (compatibleCols.size() > 1) {
                // auto-guessing
                m_inputColumn.setStringValue(compatibleCols.get(0));
                setWarningMessage("Auto guessing: using column \"" + compatibleCols.get(0) + "\".");
            } else {
                throw new InvalidSettingsException("No JSON " + "column in input table."
                    + " Try using the Columns to JSON node before this node.");
            }
        }
        if (inSpecs[INPUT_TABLE].findColumnIndex(m_inputColumn.getStringValue()) < 0) {
            throw new InvalidSettingsException("Cannot find column '" + m_inputColumn.getStringValue() + "' in the input table.");
        }
        //Cannot tell in advance the output.
        return new DataTableSpec[]{null};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_inputColumn.saveSettingsTo(settings);
        m_pathColumn.saveSettingsTo(settings);
        m_typeColumn.saveSettingsTo(settings);
        m_outputColumn.saveSettingsTo(settings);
        m_removeSourceColumn.saveSettingsTo(settings);
        //m_onInvalidFail.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_inputColumn.loadSettingsFrom(settings);
        m_pathColumn.loadSettingsFrom(settings);
        m_typeColumn.loadSettingsFrom(settings);
        m_outputColumn.loadSettingsFrom(settings);
        m_removeSourceColumn.loadSettingsFrom(settings);
        //m_onInvalidFail.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_inputColumn.validateSettings(settings);
        m_pathColumn.validateSettings(settings);
        m_typeColumn.validateSettings(settings);
        m_outputColumn.validateSettings(settings);
        m_removeSourceColumn.validateSettings(settings);
        //m_onInvalidFail.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        //No internal state
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        //No internal state
    }

}

