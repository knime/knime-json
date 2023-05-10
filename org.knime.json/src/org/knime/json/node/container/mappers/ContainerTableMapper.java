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
 *   May 7, 2018 (Tobias Urhaug, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.json.node.container.mappers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.knime.base.node.io.filereader.DataCellFactory;
import org.knime.core.data.BooleanValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.LongValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.CloseableTable;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.json.container.table.ContainerTableColumnSpec;
import org.knime.core.data.json.container.table.ContainerTableData;
import org.knime.core.data.json.container.table.ContainerTableJsonSchema;
import org.knime.core.data.json.container.table.ContainerTableRow;
import org.knime.core.data.json.container.table.ContainerTableSpec;
import org.knime.core.data.json.container.table.ContainerTableValidDataTypes;
import org.knime.core.data.uri.URIDataValue;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.util.CheckUtils;
import org.knime.json.util.JSONUtil;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.json.JsonValue;

/**
 * Class that converts a {@link BufferedDataTable} to a {@link ContainerTableJsonSchema}.
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 * @since 3.6
 */
public final class ContainerTableMapper {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private ContainerTableMapper() {

    }

    /**
     * Converts a JsonValue conforming to the structure of {@link ContainerTableJsonSchema}
     * to a {@link BufferedDataTable}.
     *
     * @param json json representation of a {@link ContainerTableJsonSchema}
     * @param exec context in which the call has been made
     * @return a Buffered data table corresponding to the json input
     * @throws InvalidSettingsException
     * @since 5.1
     */
    public static BufferedDataTable[] toBufferedDataTable(
            final JsonValue json,
            final ExecutionContext exec) throws InvalidSettingsException {
        return toBufferedDataTable(toContainerTableJsonSchema(json), exec);
    }

    /**
     * Maps a JsonValue to a {@link ContainerTableJsonSchema}.
     *
     * @param json json value to be mapped
     * @return a {@link ContainerTableJsonSchema} of the json value, if well formed
     * @throws InvalidSettingsException if the json value does not conform to {@link ContainerTableJsonSchema}
     * @since 5.1
     */
    public static ContainerTableJsonSchema toContainerTableJsonSchema(final JsonValue json) throws InvalidSettingsException {
        try {
            return OBJECT_MAPPER.readValue(json.toString(), ContainerTableJsonSchema.class);
        } catch (IOException e) {
            throw new InvalidSettingsException("Could not parse JsonValue to a Buffered Data Table", e);
        }
    }

    /**
     * Creates a buffered data table of the container input.
     *
     * @param containerTable input of which the table is created from
     * @param exec the execution context
     * @return a BufferedDataTable[] from the container table input
     * @throws InvalidSettingsException
     */
    public static BufferedDataTable[] toBufferedDataTable(
            final ContainerTableJsonSchema containerTable,
            final ExecutionContext exec) throws InvalidSettingsException {
        CheckUtils.checkSettingNotNull(containerTable, "Container Table cannot be null");
        return createTableBasedOnSpecs(containerTable, toTableSpec(containerTable), exec);
    }

    /**
     * Creates a buffered data table of the input table. If the input table does not have a table-spec,
     * all cells in each row of the input table are parsed according to the fall back tables table-spec.
     *
     * @param inputTable json representation of a table
     * @param fallbackTable json representation of a fall back table
     * @param exec the execution context
     * @return a BufferedDataTable array with the mapped table
     * @throws InvalidSettingsException if any of the json inputs are not well formed
     * @since 5.1
     */
    public static BufferedDataTable[] toBufferedDataTable(
            final JsonValue inputTable,
            final JsonValue fallbackTable,
            final ExecutionContext exec) throws InvalidSettingsException {
        ContainerTableJsonSchema inputContainerTable = toContainerTableJsonSchema(inputTable);
        ContainerTableSpec inputContainerTableSpec = inputContainerTable.getContainerTableSpec();

        return inputContainerTableSpec == null
                ? createTableBasedOnSpecs(
                    inputContainerTable,
                    toTableSpec(toContainerTableJsonSchema(fallbackTable)),
                    exec
                )
                : toBufferedDataTable(inputContainerTable, exec);
    }

    private static BufferedDataTable[] createTableBasedOnSpecs(
            final ContainerTableJsonSchema containerTable,
            final DataTableSpec tableSpec,
            final ExecutionContext exec) throws InvalidSettingsException {
        BufferedDataContainer dataContainer = exec.createDataContainer(tableSpec);
        addDataRows(dataContainer, containerTable, exec);
        dataContainer.close();
        return new BufferedDataTable[]{dataContainer.getTable()};
    }

    /**
     * Converts a JsonValue conforming to the structure of {@link ContainerTableJsonSchema} to a {@link CloseableTable}.
     *
     * @param json json representation of a {@link ContainerTableJsonSchema}
     * @return a CloseableTable[] corresponding to the json input
     * @throws InvalidSettingsException if the json input does not conform to {@link ContainerTableJsonSchema}
     * @since 5.1
     */
    public static CloseableTable[] toDataTable(final JsonValue json) throws InvalidSettingsException {
        return toDataTable(toContainerTableJsonSchema(json));
    }

    /**
     * Creates a data table from the container input.
     *
     * @param containerTable input of which the table is created from
     * @return a CloseableTable[] from the container table input
     * @throws InvalidSettingsException
     * @since 4.2
     */
    public static CloseableTable[] toDataTable(final ContainerTableJsonSchema containerTable)
            throws InvalidSettingsException {
        CheckUtils.checkSettingNotNull(containerTable, "Container Table cannot be null");
        DataTableSpec tableSpec = toTableSpec(containerTable);
        DataContainer dataContainer = new DataContainer(tableSpec);
        addDataRows(dataContainer, containerTable, null);
        dataContainer.close();
        return new CloseableTable[]{dataContainer.getCloseableTable()};
    }


    /**
     * Creates a data table spec of the input. If the input does not have a table-spec, the fall back input table-spec
     * is used.
     *
     * @param input json representing a table
     * @param fallback json representing a fall back table
     * @return DataTableSpec of the input if present, of the fall back otherwise
     * @throws InvalidSettingsException if the mapping of either of the json inputs fails
     * @since 5.1
     */
    public static DataTableSpec toTableSpec(final JsonValue input, final JsonValue fallback)
            throws InvalidSettingsException {
        ContainerTableJsonSchema inputTable = toContainerTableJsonSchema(input);
        ContainerTableSpec containerTableSpec = inputTable.getContainerTableSpec();

        return containerTableSpec != null
                ? toTableSpec(inputTable)
                : toTableSpec(fallback);
    }

    /**
     * Creates a data table spec of the container table json.
     *
     * @param jsonInput input of which the table spec is created from
     * @return a DataTableSpec from the json input
     * @throws InvalidSettingsException
     * @since 5.1
     */
    public static DataTableSpec toTableSpec(final JsonValue jsonInput) throws InvalidSettingsException {
        return toTableSpec(toContainerTableJsonSchema(jsonInput));
    }

    /**
     * Creates a data table spec of a container table.
     *
     * @param containerTable input of which the table spec is created from
     * @return a DataTableSpec from the container table input
     * @throws InvalidSettingsException
     */
    public static DataTableSpec toTableSpec(final ContainerTableJsonSchema containerTable)
            throws InvalidSettingsException {
        ContainerTableSpec tableSpec =
            CheckUtils.checkSettingNotNull(containerTable.getContainerTableSpec(), "table spec cannot be null");
        int size = tableSpec.size();
        String[] columnNames = new String[size];
        DataType[] columnTypes = new DataType[size];

        Set<String> alreadyUsedColumnNames = new HashSet<>();
        Map<String, Integer> usedColumnNamesToIndex = new HashMap<>();
        for (int i = 0; i < size; i++) {
            ContainerTableColumnSpec containerTableColumnSpec = tableSpec.getContainerTableColumnSpecs().get(i);
            String columnName = containerTableColumnSpec.getName();
            if (alreadyUsedColumnNames.add(columnName)) {
                usedColumnNamesToIndex.put(columnName, i);
                columnNames[i] = columnName;
                String columnType = containerTableColumnSpec.getType();
                columnTypes[i] = ContainerTableValidDataTypes.parse(columnType);
            } else {
                throw new InvalidSettingsException("Columns \"" + usedColumnNamesToIndex.get(columnName) +
                    "\" and \"" + i + "\" have equal names. Duplicate column names in input are not allowed.");
            }
        }

        DataColumnSpec[] columnSpec = DataTableSpec.createColumnSpecs(columnNames, columnTypes);
        return new DataTableSpec(columnSpec);
    }

    private static void addDataRows(
            final DataContainer dataContainer,
            final ContainerTableJsonSchema containerTable,
            final ExecutionContext exec) throws InvalidSettingsException {
        long rowKeyIndex = 0L;
        DataTableSpec tableSpec = dataContainer.getTableSpec();
        ContainerTableData tableData = containerTable.getContainerTableData();
        int numColumns = tableSpec.getNumColumns();
        for (ContainerTableRow tableRow : tableData.getContainerTableRows()) {
            int numberOfCells = tableRow.size();
            if (numberOfCells != numColumns) {
                throw new InvalidSettingsException("Invalid input. Template table spec defines " + numColumns + " "
                    + "columns, but input row " + rowKeyIndex + " has " + numberOfCells + " cells.");
            }
            dataContainer.addRowToTable(
                new DefaultRow(
                    RowKey.createRowKey(rowKeyIndex++),
                    getDataCells(tableRow, tableSpec, exec)
                )
            );
        }
    }

    private static DataCell[] getDataCells(
            final ContainerTableRow tableRow,
            final DataTableSpec tableSpec,
            final ExecutionContext exec) throws InvalidSettingsException {
        DataCell[] dataCells = new DataCell[tableRow.size()];
        List<Object> cells = tableRow.getDataCellObjects();
        DataCellFactory cellFactory = new DataCellFactory(exec);
        for (int i = 0; i < cells.size(); i++) {
            DataType columnType = tableSpec.getColumnSpec(i).getType();
            Object cellObject = cells.get(i);
            if (cellObject == null) {
                dataCells[i] = DataType.getMissingCell();
            } else {
                String cellObjectString = cellObject.toString();
                DataCell dataCell = cellFactory.createDataCellOfType(columnType, cellObjectString);
                if (dataCell == null) {
                    String message = "Could not parse: \"" + cellObjectString + "\" to type: \"" + columnType + "\"";
                    throw new InvalidSettingsException(message);
                }
                dataCells[i] = dataCell;
            }
        }
        return dataCells;
    }

    /**
     * Converts the incoming table to a json value conforming to {@link ContainerTableJsonSchema}.
     *
     * @param table table to be converted to a json value
     * @return json value representing the input table, conforming to {@link ContainerTableJsonSchema}
     * @throws InvalidSettingsException if the table could not be mapped to a conforming Json value
     * @since 5.1
     */
    public static JsonValue toContainerTableJsonValue(final BufferedDataTable table) throws InvalidSettingsException {
        return toContainerTableJsonValueFromDataTable(table);
    }

    /**
     * Converts the incoming table to a json value conforming to {@link ContainerTableJsonSchema}.
     *
     * @param table table to be converted to a json value
     * @return json value representing the input table, conforming to {@link ContainerTableJsonSchema}
     * @throws InvalidSettingsException if the table could not be mapped to a conforming Json value
     * @since 5.1
     */
    public static JsonValue toContainerTableJsonValueFromDataTable(final DataTable table)
            throws InvalidSettingsException {
        JsonValue result = null;
        try {
            ContainerTableJsonSchema containerTable = toContainerTableFromDataTable(table);
            ObjectMapper objectMapper = new ObjectMapper();
            String containerTableJson = objectMapper.writeValueAsString(containerTable);
            result = JSONUtil.parseJSONValue(containerTableJson);
        } catch (IOException e) {
            throw new InvalidSettingsException("Could not parse the table to JsonValue", e);
        }

        return result;
    }

    /**
     * Converts the given {@link BufferedDataTable} to a {@link ContainerTableJsonSchema}.
     *
     * @param table table to be converted to a {@link ContainerTableJsonSchema}
     * @return ContainerTableJsonSchema of the input table
     * @throws InvalidSettingsException if the table contains column types not compatible with
     * {@link ContainerTableJsonSchema}
     */
    public static ContainerTableJsonSchema toContainerTable(final BufferedDataTable table)
            throws InvalidSettingsException {
        return toContainerTableFromDataTable(table);
    }

    /**
     * Converts the given {@link BufferedDataTable} to a {@link ContainerTableJsonSchema}.
     *
     * @param table table to be converted to a {@link ContainerTableJsonSchema}
     * @return ContainerTableJsonSchema of the input table
     * @throws InvalidSettingsException if the table contains column types not compatible with
     * {@link ContainerTableJsonSchema}
     * @since 3.7
     */
    public static ContainerTableJsonSchema toContainerTableFromDataTable(final DataTable table)
            throws InvalidSettingsException {
        return new ContainerTableJsonSchema(createContainerTableSpecs(table), createContainerTableData(table));
    }

    private static ContainerTableSpec createContainerTableSpecs(final DataTable table) throws InvalidSettingsException {
        DataTableSpec dataTableSpec = table.getDataTableSpec();
        List<ContainerTableColumnSpec> containerTableColumnSpecs = new ArrayList<>();
        for (DataColumnSpec columnSpec : dataTableSpec) {
            String name = columnSpec.getName();
            String type = ContainerTableValidDataTypes.parse(columnSpec.getType());
            containerTableColumnSpecs.add(new ContainerTableColumnSpec(name, type));
        }
        return new ContainerTableSpec(containerTableColumnSpecs);
    }

    private static ContainerTableData createContainerTableData(final DataTable table) {
        List<ContainerTableRow> containerTableRows = new ArrayList<>();
        DataTableSpec dataTableSpec = table.getDataTableSpec();

        for (DataRow row : table) {
            containerTableRows.add(createContainerTableRow(row, dataTableSpec));
        }

        return new ContainerTableData(containerTableRows);
    }

    private static ContainerTableRow createContainerTableRow(
            final DataRow originRow,
            final DataTableSpec dataTableSpec) {
        List<Object> resultRow = new ArrayList<>();
        int numColumns = dataTableSpec.getNumColumns();
        for (int i = 0; i < numColumns; i++) {
            DataType type = dataTableSpec.getColumnSpec(i).getType();
            DataCell dataCell = originRow.getCell(i);
            if (dataCell.isMissing()) {
                resultRow.add(null);
            } else {
                resultRow.add(parse(dataCell, type));
            }
        }
        return new ContainerTableRow(resultRow);
    }

    private static Object parse(final DataCell dataCell, final DataType type) {
        if (DoubleValue.class.equals(type.getPreferredValueClass())) {
            return ((DoubleValue) dataCell).getDoubleValue();
        }  else if (IntValue.class.equals(type.getPreferredValueClass())) {
            return ((IntValue) dataCell).getIntValue();
        } else if (LongValue.class.equals(type.getPreferredValueClass())) {
            return ((LongValue) dataCell).getLongValue();
        } else if (BooleanValue.class.equals(type.getPreferredValueClass())) {
            return ((BooleanValue) dataCell).getBooleanValue();
        } else if (URIDataValue.class.equals(type.getPreferredValueClass())){
            URIDataValue uri = (URIDataValue) dataCell;
            return uri.getURIContent().getURI();
        } else {
            return dataCell.toString();
        }
    }

}
