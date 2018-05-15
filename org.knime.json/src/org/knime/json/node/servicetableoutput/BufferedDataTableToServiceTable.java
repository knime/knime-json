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
package org.knime.json.node.servicetableoutput;

import java.util.ArrayList;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.json.servicetable.ServiceTable;
import org.knime.core.data.json.servicetable.ServiceTableColumnSpec;
import org.knime.core.data.json.servicetable.ServiceTableData;
import org.knime.core.data.json.servicetable.ServiceTableValidDataTypes;
import org.knime.core.data.json.servicetable.ServiceTableRow;
import org.knime.core.data.json.servicetable.ServiceTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.util.CheckUtils;

/**
 * Class that converts a {@link BufferedDataTable} to a {@link ServiceTable}.
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 */
public class BufferedDataTableToServiceTable {

    /**
     * Converts the given {@link BufferedDataTable} to a {@link ServiceTable}.
     *
     * @param tableInput table to be converted to a ServiceTable
     * @return ServiceTable
     * @throws InvalidSettingsException
     */
    public static ServiceTable toServiceTable(final BufferedDataTable[] tableInput) throws InvalidSettingsException {
        CheckUtils.checkSettingNotNull(tableInput, "Table input cannot be null");
        CheckUtils.checkArgument(tableInput.length == 1, "Table input must contain only one table");
        CheckUtils.checkSettingNotNull(tableInput[0], "Table cannot be null");
        BufferedDataTable table = tableInput[0];
        return new ServiceTable(createServiceTableSpecs(table), createServiceTableData(table));
    }

    private static ServiceTableSpec createServiceTableSpecs(final BufferedDataTable table) {
        return new ServiceTableSpec(createServiceTableColumnSpecs(table));
    }

    private static List<ServiceTableColumnSpec> createServiceTableColumnSpecs(final BufferedDataTable table) {
        DataTableSpec dataTableSpec = table.getDataTableSpec();
        List<ServiceTableColumnSpec> serviceTableColumnSpecs = new ArrayList<>();
        for (DataColumnSpec columnSpec : dataTableSpec) {
            String name = columnSpec.getName();
            String type = ServiceTableValidDataTypes.parse(columnSpec.getType());
            serviceTableColumnSpecs.add(new ServiceTableColumnSpec(name, type));
        }
        return serviceTableColumnSpecs;
    }

    private static ServiceTableData createServiceTableData(final BufferedDataTable table) {
        return new ServiceTableData(createServiceTableRows(table));
    }
    private static List<ServiceTableRow> createServiceTableRows(final BufferedDataTable table) {
        List<ServiceTableRow> serviceTableRows = new ArrayList<>();
        DataTableSpec dataTableSpec = table.getDataTableSpec();
        table.forEach(row -> serviceTableRows.add(createServiceTableRow(row, dataTableSpec)));
        return serviceTableRows;
    }

    private static ServiceTableRow createServiceTableRow(final DataRow originRow, final DataTableSpec dataTableSpec) {
        List<Object> resultRow = new ArrayList<>();
        int numColumns = dataTableSpec.getNumColumns();
        for (int i = 0; i < numColumns; i++) {
            DataCell dataCell = originRow.getCell(i);
            if (dataCell.isMissing()) {
                resultRow.add(null);
            }
            DataType columnType = dataTableSpec.getColumnSpec(i).getType();
            resultRow.add(parse(dataCell, columnType));
        }
        return new ServiceTableRow(resultRow);
    }

    private static Object parse(final DataCell dataCell, final DataType targetType) {
        Object cellObject = null;

        if (targetType.getCellClass().equals(DoubleCell.class)) {
            if (dataCell.getClass().equals(DoubleCell.class)) {
                cellObject = ((DoubleCell) dataCell).getDoubleValue();
            }
        } else if (targetType.getCellClass().equals(IntCell.class)) {
            if (dataCell.getClass().equals(IntCell.class)) {
                cellObject = ((IntCell) dataCell).getIntValue();
            }
        } else if (targetType.getCellClass().equals(LongCell.class)) {
            if (dataCell.getClass().equals(LongCell.class)) {
                cellObject = ((LongCell) dataCell).getLongValue();
            }
        } else if (targetType.getCellClass().equals(BooleanCell.class)) {
            if (dataCell.getClass().equals(BooleanCell.class)) {
                cellObject = ((BooleanCell) dataCell).getBooleanValue();
            }
        } else {
            cellObject = dataCell.toString();
        }

        return cellObject;
    }

}
