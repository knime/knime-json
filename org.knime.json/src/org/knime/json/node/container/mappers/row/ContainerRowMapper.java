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
 *   Dec 13, 2018 (Tobias Urhaug, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.json.node.container.mappers.row;

import static org.knime.json.node.container.mappers.row.inputhandling.MissingColumnHandling.FILL_WITH_DEFAULT_VALUE;
import static org.knime.json.node.container.mappers.row.inputhandling.MissingColumnHandling.FILL_WITH_MISSING_VALUE;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.knime.base.node.io.filereader.DataCellFactory;
import org.knime.core.data.BooleanValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.LongValue;
import org.knime.core.data.RowIterator;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.json.JSONCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.util.CheckUtils;
import org.knime.json.node.container.mappers.row.inputhandling.ContainerRowMapperInputHandling;
import org.knime.json.node.container.mappers.row.inputhandling.MissingColumnHandling;
import org.knime.json.node.container.mappers.row.inputhandling.MissingValuesHandling;
import org.knime.json.util.JSONUtil;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.json.JsonValue;

/**
 * Class that converts simple {@link JsonValue} to {@link DataTable} and vice versa.
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 * @since 4.0
 */
public class ContainerRowMapper {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private ContainerRowMapper() {
        // Private on purpose to prevent instantiations of the class
    }

    /**
     * Converts a JsonValue input containing key value pairs to a single row table where the key of each pair is the
     * column name and the value the corresponding cell value. Each column gets its type inferred from the cell value,
     * where only the primitive types String, Boolean, Integer, Long and Double are valid. <br>
     * <br>
     * Only simple JsonValues are allowed. JSON Arrays and JSON objects will throw InvalidSettingsException.
     *
     * @param input JsonValue representing a data table row
     * @param exec the execution context
     * @return a single row data table representing the input
     * @throws InvalidSettingsException if input is null or contains JSON arrays or objects
     */
    public static BufferedDataTable toDataTable(final JsonValue input, final ExecutionContext exec)
            throws InvalidSettingsException {
        CheckUtils.checkSettingNotNull(input, "Can not map null to a data table");

        DataTableSpec rowSpecification = toTableSpec(input);
        BufferedDataContainer dataContainer = exec.createDataContainer(rowSpecification);
        DataCell[] dataCells = createDataCells(input, rowSpecification, exec);
        dataContainer.addRowToTable(new DefaultRow(RowKey.createRowKey(0l), dataCells));
        dataContainer.close();
        return dataContainer.getTable();
    }

    /**
     * Converts a JsonValue input containing key value pairs to a data table spec, where the column names of the spec
     * are the keys of the json input and the column types are inferred from the values of each pair.
     *
     * @param input JsonValue representing a data table row
     * @return a {@link DataTableSpec} of the input
     * @throws InvalidSettingsException if the input is not well formed
     */
    public static DataTableSpec toTableSpec(final JsonValue input) throws InvalidSettingsException {
        CheckUtils.checkSettingNotNull(input, "Can not map null to a data table spec");

        Map<String, Object> jsonRow = parseJsonToMap(input);

        int numberOfColumns = jsonRow.size();
        String[] columnNames = new String[numberOfColumns];
        DataType[] columnTypes = new DataType[numberOfColumns];

        int i = 0;
        for (Entry<String, Object> jsonCell : jsonRow.entrySet()) {
            Object cellValue = jsonCell.getValue();
            DataType columnType = inferColumnTypeFromCellObject(cellValue);

            columnTypes[i] = columnType;
            columnNames[i] = jsonCell.getKey();
            i++;
        }

        DataColumnSpec[] columnSpec = DataTableSpec.createColumnSpecs(columnNames, columnTypes);
        return new DataTableSpec(columnSpec);
    }

    private static DataType inferColumnTypeFromCellObject(final Object cellValue) throws InvalidSettingsException {
        if (cellValue == null) {
            return StringCell.TYPE;
        }

        DataType columnType = null;
        if (cellValue instanceof Double) {
            columnType = DoubleCell.TYPE;
        } else if (cellValue instanceof String) {
            columnType = StringCell.TYPE;
        } else if (cellValue instanceof Boolean) {
            columnType = BooleanCell.TYPE;
        } else if (cellValue instanceof Integer) {
            columnType = IntCell.TYPE;
        } else if (cellValue instanceof Long) {
            columnType = LongCell.TYPE;
        } else if (cellValue instanceof ArrayList) {
            throw new InvalidSettingsException("JSON arrays are not supported");
        } else if (cellValue instanceof BigInteger) {
            throw new InvalidSettingsException("Numeric integer values larger than 2^64 are not supported");
        } else if (cellValue instanceof Map) {
            // this indicates that the cell has a json object structure and should be parsed as a json string
            columnType = StringCell.TYPE;
        } else {
            throw new InvalidSettingsException("The type of input cell '" + cellValue + "' could not be infered.");
        }

        return columnType;
    }

    private static DataCell[] createDataCells(
            final JsonValue input,
            final DataTableSpec rowSpec,
            final ExecutionContext exec) throws InvalidSettingsException {
        Map<String, Object> jsonRow = parseJsonToMap(input);

        DataCellFactory factory = new DataCellFactory(exec);
        List<DataCell> dataCells = new ArrayList<>();
        for (int i = 0; i < rowSpec.getNumColumns(); i++) {
            DataColumnSpec columnSpec = rowSpec.getColumnSpec(i);
            String columnName = columnSpec.getName();
            if (jsonRow.containsKey(columnName)) {
                DataType columnType = columnSpec.getType();
                Object jsonCell = jsonRow.get(columnName);
                if (jsonCell == null) {
                    dataCells.add(DataType.getMissingCell());
                } else {
                    String stringCell = getStringRepresentation(jsonCell);
                    DataCell dataCell = factory.createDataCellOfType(columnType, stringCell);
                    dataCells.add(dataCell);
                }
            } else {
                dataCells.add(DataType.getMissingCell());
            }
        }

        DataCell[] result = new DataCell[dataCells.size()];
        return dataCells.toArray(result);
    }

    private static String getStringRepresentation(final Object jsonCell) throws InvalidSettingsException {
        String result = jsonCell.toString();
        if (jsonCell instanceof Map) { // checks if the cell is a json object and can be parsed to its string value
            @SuppressWarnings("unchecked")
            Map<String, Object> jsonMap = (Map<String, Object>)jsonCell;
            try {
                result = OBJECT_MAPPER.writeValueAsString(jsonMap);
            } catch (JsonProcessingException e) {
                throw new InvalidSettingsException("An error occured while parsing the input", e);
            }
        }

        return result;
    }

    /**
     * Converts a JsonValue input containing key value pairs to a single row table where the key of each pair is the
     * column name and the value is the corresponding cell value. Each value of a column will be be parsed according to
     * the given template row specification if compatible, otherwise an exception will be thrown. <br>
     * <br>
     * Only simple JsonValues are allowed. JSON Arrays and JSON objects will throw InvalidSettingsException.
     *
     * @param input JsonValue representing a data table row
     * @param templateRow the template row
     * @param inputHandling the strategies for handling the input
     * @param exec the execution context
     * @return a single row data table representing the input and parsed according to the row specification
     * @throws InvalidSettingsException if input is null or contains JSON arrays or objects
     */
    public static BufferedDataTable toDataTable(
            final JsonValue input,
            final BufferedDataTable templateRow,
            final ContainerRowMapperInputHandling inputHandling,
            final ExecutionContext exec) throws InvalidSettingsException {
        CheckUtils.checkSettingNotNull(input, "Can not map null to a data table");

        DataTableSpec rowSpecification = toTableSpec(input, templateRow.getDataTableSpec(), inputHandling);
        BufferedDataContainer dataContainer = exec.createDataContainer(rowSpecification);
        DataCell[] dataCells = createDataCells(input, rowSpecification, templateRow, inputHandling, exec);
        dataContainer.addRowToTable(new DefaultRow(RowKey.createRowKey(0l), dataCells));
        dataContainer.close();
        return dataContainer.getTable();
    }

    /**
     * Creates a {@link DataTableSpec} according to a template specification and an input.
     *
     * If the input does not contain all the columns specified in the template specification, the parameter
     * MissingColumnHandling decides whether the specification should remain equal to the template specification, if the
     * missing columns should be ignored and removed or if an error should be thrown.
     *
     * @param input JsonValue representing a data table row
     * @param templateRowSpec the template row specification
     * @param inputHandling the strategies for handling the input
     * @return a {@link DataTableSpec} of the input
     * @throws InvalidSettingsException if the input is not well formed
     */
    public static DataTableSpec toTableSpec(
            final JsonValue input,
            final DataTableSpec templateRowSpec,
            final ContainerRowMapperInputHandling inputHandling) throws InvalidSettingsException {
        CheckUtils.checkSettingNotNull(input, "Can not map null to a data table spec");

        Map<String, Object> jsonRow = parseJsonToMap(input);
        DataTableSpec result = templateRowSpec;
        result = handleMissingColumns(result, inputHandling.missingColumnHandling(), jsonRow);
        if (inputHandling.appendUnknownColumns()) {
            result = appendSuperfluousColumns(result, jsonRow);
        }

        return result;
    }

    private static DataTableSpec handleMissingColumns(
            final DataTableSpec spec,
            final MissingColumnHandling missingColumnHandling,
            final Map<String, Object> jsonRow) throws InvalidSettingsException {
        ColumnRearranger columnRearranger = new ColumnRearranger(spec);
        for (int i = 0; i < spec.getNumColumns(); i++) {
            DataColumnSpec columnSpec = spec.getColumnSpec(i);
            String columnName = columnSpec.getName();
            if (!jsonRow.containsKey(columnName)) {
                if (missingColumnHandling == MissingColumnHandling.REMOVE) {
                    columnRearranger.remove(columnName);
                } else if (missingColumnHandling == MissingColumnHandling.FAIL) {
                    throw new InvalidSettingsException(
                        "The injected row does not contain all the columns specified in the template. "
                            + "The node is configured to fail on missing columns.");
                }
            }
        }

        return columnRearranger.createSpec();
    }

    private static DataTableSpec appendSuperfluousColumns(final DataTableSpec spec, final Map<String, Object> jsonRow)
            throws InvalidSettingsException {
        DataTableSpecCreator dataTableSpecCreator = new DataTableSpecCreator(spec);

        for (Entry<String, Object> entry : jsonRow.entrySet()) {
            String columnName = entry.getKey();
            if (!spec.containsName(columnName)) {
                DataColumnSpec columnSpec =
                    new DataColumnSpecCreator(columnName, inferColumnTypeFromCellObject(entry.getValue())).createSpec();
                dataTableSpecCreator.addColumns(columnSpec);
            }
        }
        return dataTableSpecCreator.createSpec();
    }

    private static DataCell[] createDataCells(
            final JsonValue input,
            final DataTableSpec rowSpec,
            final BufferedDataTable templateTable,
            final ContainerRowMapperInputHandling inputHandling,
            final ExecutionContext exec) throws InvalidSettingsException {
        Map<String, Object> jsonRow = parseJsonToMap(input);

        DataCellFactory factory = new DataCellFactory(exec);
        List<DataCell> dataCellList = new ArrayList<>();
        for (int i = 0; i < rowSpec.getNumColumns(); i++) {
            DataColumnSpec columnSpec = rowSpec.getColumnSpec(i);
            String columnName = columnSpec.getName();
            if (jsonRow.containsKey(columnName)) {
                DataType columnType = columnSpec.getType();
                Object jsonCell = jsonRow.get(columnName);
                DataCell parsedDataCell =
                        parseDataCell(
                            jsonCell,
                            factory,
                            columnType,
                            columnName,
                            templateTable,
                            inputHandling.missingValuesHandling()
                        );
                dataCellList.add(parsedDataCell);
            } else { // missing columns handling
                DataCell dataCell =
                    createDataCellForMissingColumn(templateTable, inputHandling.missingColumnHandling(), columnName);
                dataCellList.add(dataCell);
            }
        }

        DataCell[] result = new DataCell[dataCellList.size()];
        return dataCellList.toArray(result);
    }

    private static DataCell parseDataCell(
            final Object jsonCell,
            final DataCellFactory factory,
            final DataType columnType,
            final String columnName,
            final BufferedDataTable templateTable,
            final MissingValuesHandling missingValuesHandling) throws InvalidSettingsException {
        if (jsonCell == null) {
            return handleMissingDataCell(columnName, templateTable, missingValuesHandling);
        } else {
            String stringCell = getStringRepresentation(jsonCell);
            DataCell result = factory.createDataCellOfType(columnType, stringCell);
            if (result == null) {
                throw new InvalidSettingsException("The value '" + jsonCell + "' of column '" + columnName
                    + "' cannot be parsed to the expected '" + columnType + "' type");
            } else {
                return result;
            }
        }
    }

    private static DataCell handleMissingDataCell(
            final String columnName,
            final BufferedDataTable templateTable,
            final MissingValuesHandling missingValuesHandling) throws InvalidSettingsException {
        DataCell result = null;
        if (missingValuesHandling == MissingValuesHandling.ACCEPT) {
            result = DataType.getMissingCell();
        } else if (missingValuesHandling == MissingValuesHandling.FILL_WITH_DEFAULT) {
            DataTableSpec dataTableSpec = templateTable.getDataTableSpec();
            int columnIndex = dataTableSpec.findColumnIndex(columnName);
            if (columnIndex == -1) {
                //This means we are parsing a null data cell in an unknown column which there's no default for
                result = DataType.getMissingCell();
            } else {
                result = getDataCellByColumnName(templateTable, columnName);
            }
        } else {
            throw new InvalidSettingsException("The injected row contains missing values. "
                + "The node is configured to not accept missing values in the input.");
        }

        return result;
    }

    private static DataCell createDataCellForMissingColumn(
            final BufferedDataTable templateTable,
            final MissingColumnHandling missingColumnHandling,
            final String columnName) {
        if (missingColumnHandling == FILL_WITH_MISSING_VALUE) {
            return DataType.getMissingCell();
        } else if (missingColumnHandling == FILL_WITH_DEFAULT_VALUE) {
            return getDataCellByColumnName(templateTable, columnName);
        } else {
            return null;
        }
    }

    private static DataCell getDataCellByColumnName(final BufferedDataTable templateTable, final String columnName) {
        DataCell result = null;
        DataTableSpec templateRowSpec = templateTable.getDataTableSpec();
        int columnIndex = templateRowSpec.findColumnIndex(columnName);
        try (CloseableRowIterator iterator = templateTable.iterator()) {
            if (iterator.hasNext()) {
                DataRow row = iterator.next();
                result = row.getCell(columnIndex);
            }
        }
        return result;
    }

    private static Map<String, Object> parseJsonToMap(final JsonValue input) throws InvalidSettingsException {
        try {
            return OBJECT_MAPPER.readValue(input.toString(), new TypeReference<LinkedHashMap<String, Object>>() {});
        } catch (IOException e) {
            throw new InvalidSettingsException("Error when parsing input. The input must have a simple map format "
                + "(only key/value pairs) and lists are not allowed.", e);
        }
    }

    /**
     * Maps a {@link BufferedDataTable} to a simple json representation of its first row.
     *
     * Each column name is mapped to the key of a json object and the first cell of the corresponding column is mapped
     * to the value of the same json object. Primitive types are preserved, all complex types are mapped to their
     * string representation.
     *
     * @param input the table to be mapped
     * @return a json representation of the first row of the input table
     * @throws IOException if json mapping fails
     */
    public static JsonValue firstRowToJsonValue(final DataTable input) throws IOException {
        CheckUtils.checkArgumentNotNull(input);

        Map<String, Object> rowMap = new LinkedHashMap<>();
        DataRow firstRow = firstRow(input);
        if (firstRow != null) {
            String[] columnNames = input.getDataTableSpec().getColumnNames();
            for (int i = 0; i < firstRow.getNumCells(); i++) {
                DataCell dataCell = firstRow.getCell(i);
                rowMap.put(columnNames[i], asObject(dataCell));
            }
        }

        String rowString = new ObjectMapper().writeValueAsString(rowMap);
        return JSONUtil.parseJSONValue(rowString);
    }

    private static DataRow firstRow(final DataTable input) {
        DataRow firstRow = null;
        RowIterator iterator = input.iterator();
        if (iterator.hasNext()) {
            firstRow = iterator.next();
        }
        if (iterator instanceof CloseableRowIterator) {
            ((CloseableRowIterator) iterator).close();
        }
        return firstRow;
    }

    private static Object asObject(final DataCell dataCell) throws IOException {
        Object cellObject;
        if (dataCell.isMissing()) {
            cellObject = null;
        } else if (dataCell.getType().getCellClass().equals(IntCell.class)) {
            cellObject = ((IntValue) dataCell).getIntValue();
        } else if (dataCell.getType().getCellClass().equals(DoubleCell.class)) {
            cellObject = ((DoubleValue) dataCell).getDoubleValue();
        } else if (dataCell.getType().getCellClass().equals(LongCell.class)) {
            cellObject = ((LongValue) dataCell).getLongValue();
        } else if (dataCell.getType().getCellClass().equals(BooleanCell.class)) {
            cellObject = ((BooleanValue) dataCell).getBooleanValue();
        } else if (dataCell.getType().getCellClass().equals(JSONCell.class)) {
            JsonValue jsonValue = ((JSONCell) dataCell).getJsonValue();
            cellObject = new ObjectMapper().readTree(jsonValue.toString());
        } else {
            cellObject = dataCell.toString();
        }
        return cellObject;
    }

}
