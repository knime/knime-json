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
package org.knime.json.node.container.mappers;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.json.JsonValue;

import org.knime.base.node.io.filereader.DataCellFactory;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.util.CheckUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Class that converts simple {@link JsonValue} to {@link DataTable} and vice versa.
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 * @since 3.8
 */
public class ContainerRowMapper {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private ContainerRowMapper() {
        // Private on purpose to prevent instantiations of the class
    }

    /**
     * Converts a JsonValue input containing key value pairs to a single row table where the key of each pair is the
     * column name and the value the corresponding cell value. Each column gets its type inferred from the cell value,
     * where only the primitive types String, Boolean, Integer, Long and Double are valid.
     * <br>
     * <br>
     * Only simple JsonValues are allowed. JSON Arrays and JSON objects will throw InvalidSettingsException.
     *
     * @param input JsonValue representing a data table row
     * @param exec the execution context
     * @return a single row data table representing the input
     * @throws InvalidSettingsException if input is null
     */
    public static BufferedDataTable toDataTable(final JsonValue input, final ExecutionContext exec)
            throws InvalidSettingsException {
        CheckUtils.checkSettingNotNull(input, "Can not map null to a data table");

        BufferedDataContainer dataContainer = exec.createDataContainer(toTableSpec(input));
        dataContainer.addRowToTable(new DefaultRow(RowKey.createRowKey(0l), createDataCells(input, exec)));
        dataContainer.close();
        return dataContainer.getTable();
    }

    /**
     * Converts a JsonValue input containing key value pairs to a data table spec, where the column names of the spec
     * are the keys of the json input and the column types are inferred from the values of each pair.
     *
     * @param input json value
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
        } else if (cellValue instanceof BigInteger){
            throw new InvalidSettingsException("Numeric integer values larger than 2^64 are not supported");
        } else if (cellValue instanceof Map) {
            throw new InvalidSettingsException("JSON objects are not supported");
        }
        return columnType;
    }

    private static DataCell[] createDataCells(final JsonValue input, final ExecutionContext exec)
        throws InvalidSettingsException {
        Map<String, Object> jsonRow = parseJsonToMap(input);

        int numberOfColumns = jsonRow.size();
        DataCell[] dataCells = new DataCell[numberOfColumns];

        int i = 0;
        for (Entry<String, Object> jsonCell : jsonRow.entrySet()) {
            Object cellValue = jsonCell.getValue();
            DataType columnType = inferColumnTypeFromCellObject(cellValue);

            DataCellFactory factory = new DataCellFactory(exec);
            DataCell dataCell = factory.createDataCellOfType(columnType, cellValue.toString());

            dataCells[i] = dataCell;
            i++;
        }
        return dataCells;
    }

    private static Map<String, Object> parseJsonToMap(final JsonValue input)throws InvalidSettingsException {
        try {
            return OBJECT_MAPPER.readValue(input.toString(), new TypeReference<LinkedHashMap<String, Object>>(){});
        } catch (IOException e) {
            throw new InvalidSettingsException("Could not parse input to json", e);
        }
    }

}
