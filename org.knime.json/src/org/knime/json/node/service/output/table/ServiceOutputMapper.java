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
package org.knime.json.node.service.output.table;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.json.JsonValue;

import org.knime.core.data.BooleanValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.LongValue;
import org.knime.core.data.json.servicetable.ServiceTable;
import org.knime.core.data.json.servicetable.ServiceTableColumnSpec;
import org.knime.core.data.json.servicetable.ServiceTableData;
import org.knime.core.data.json.servicetable.ServiceTableRow;
import org.knime.core.data.json.servicetable.ServiceTableSpec;
import org.knime.core.data.json.servicetable.ServiceTableValidDataTypes;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.InvalidSettingsException;
import org.knime.json.util.JSONUtil;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Class that converts a {@link BufferedDataTable} to a {@link ServiceTable}.
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 * @since 3.6
 */
public final class ServiceOutputMapper {

    private ServiceOutputMapper() {

    }

    /**
     * Converts the incoming table to a json value conforming to the {@link ServiceTable} structure.
     *
     * @param table table to be converted to a json value
     * @return json value representing the input table
     * @throws InvalidSettingsException if the table could not be mapped to a conforming Json value
     */
    public static JsonValue toServiceTableJsonValue(final BufferedDataTable table) throws InvalidSettingsException {
        JsonValue result = null;
        try {
            ServiceTable serviceTable = toServiceTable(table);
            ObjectMapper objectMapper = new ObjectMapper();
            String serviceTableJson = objectMapper.writeValueAsString(serviceTable);
            result = JSONUtil.parseJSONValue(serviceTableJson);
        } catch (IOException e) {
            throw new InvalidSettingsException("Could not parse JsonValue to a Buffered Data Table", e);
        }

        return result;
    }

    /**
     * Converts the given {@link BufferedDataTable} to a {@link ServiceTable}.
     *
     * @param table table to be converted to a ServiceTable
     * @return ServiceTable
     */
    public static ServiceTable toServiceTable(final BufferedDataTable table) {
        return new ServiceTable(createServiceTableSpecs(table), createServiceTableData(table));
    }

    private static ServiceTableSpec createServiceTableSpecs(final BufferedDataTable table) {
        DataTableSpec dataTableSpec = table.getDataTableSpec();
        List<ServiceTableColumnSpec> serviceTableColumnSpecs = new ArrayList<>();
        for (DataColumnSpec columnSpec : dataTableSpec) {
            String name = columnSpec.getName();
            String type = ServiceTableValidDataTypes.parse(columnSpec.getType());
            serviceTableColumnSpecs.add(new ServiceTableColumnSpec(name, type));
        }
        return new ServiceTableSpec(serviceTableColumnSpecs);
    }
    private static ServiceTableData createServiceTableData(final BufferedDataTable table) {
        List<ServiceTableRow> serviceTableRows = new ArrayList<>();
        DataTableSpec dataTableSpec = table.getDataTableSpec();

        for (DataRow row : table) {
            serviceTableRows.add(createServiceTableRow(row, dataTableSpec));
        }

        return new ServiceTableData(serviceTableRows);
    }

    private static ServiceTableRow createServiceTableRow(final DataRow originRow, final DataTableSpec dataTableSpec) {
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
        return new ServiceTableRow(resultRow);
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
        } else {
            return dataCell.toString();
        }
    }
}
