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
 *   Apr 4, 2018 (Tobias Urhaug): created
 */
package org.knime.json.node.servicein;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.knime.base.node.io.filereader.DataCellFactory;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.MissingCell;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.json.servicetable.ServiceTable;
import org.knime.core.data.json.servicetable.ServiceTableColumnSpec;
import org.knime.core.data.json.servicetable.ServiceTableData;
import org.knime.core.data.json.servicetable.ServiceTableValidDataTypes;
import org.knime.core.data.json.servicetable.ServiceTableRow;
import org.knime.core.data.json.servicetable.ServiceTableSpec;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.util.CheckUtils;

/**
 * Utility class for converting {@link ServiceTable}.
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 */
final class ServiceTableConverter {

    /**
     * Creates a buffered data table of the service input.
     *
     * @param serviceInput input of which the table is created from
     * @param exec the execution context
     * @return a BufferedDataTable[] from the service input
     * @throws InvalidSettingsException
     */
    public static BufferedDataTable[] toBufferedDataTable(final ServiceTable serviceInput, final ExecutionContext exec)
            throws InvalidSettingsException {
        CheckUtils.checkSettingNotNull(serviceInput, "Service input cannot be null");
        BufferedDataContainer dataContainer = exec.createDataContainer(toTableSpec(serviceInput));
        addDataRows(dataContainer, serviceInput, exec);
        dataContainer.close();
        return new BufferedDataTable[]{dataContainer.getTable()};
    }

    /**
     * Creates a data table spec of the service input.
     *
     * @param serviceInput input of which the table spec is created from
     * @return a DataTableSpec from the service input
     * @throws InvalidSettingsException
     */
    public static DataTableSpec toTableSpec(final ServiceTable serviceInput) throws InvalidSettingsException {
        ServiceTableSpec tableSpec =
            CheckUtils.checkSettingNotNull(serviceInput.getServiceTableSpec(), "table spec cannot be null");
        int size = tableSpec.size();
        String[] columnNames = new String[size];
        DataType[] columnTypes = new DataType[size];

        Set<String> alreadyUsedColumnNames = new HashSet<>();
        Map<String, Integer> usedColumnNamesToIndex = new HashMap<>();
        for (int i = 0; i < size; i++) {
            ServiceTableColumnSpec serviceInputColumnSpec = tableSpec.getServiceTableColumnSpecs().get(i);
            String columnName = serviceInputColumnSpec.getName();
            if (alreadyUsedColumnNames.add(columnName)) {
                usedColumnNamesToIndex.put(columnName, i);
                columnNames[i] = columnName;
                String columnType = serviceInputColumnSpec.getType();
                columnTypes[i] = ServiceTableValidDataTypes.parse(columnType);
            } else {
                throw new InvalidSettingsException("Columns \"" + usedColumnNamesToIndex.get(columnName) + "\" and \"" + i
                    + "\" have equal names. Duplicate column names in input are not allowed.");
            }
        }

        DataColumnSpec[] columnSpec = DataTableSpec.createColumnSpecs(columnNames, columnTypes);
        return new DataTableSpec(columnSpec);
    }

    private static void addDataRows(final BufferedDataContainer dataContainer, final ServiceTable serviceInput, final ExecutionContext exec)
            throws InvalidSettingsException {
        long rowKeyIndex = 0L;
        DataTableSpec tableSpec = dataContainer.getTableSpec();
        ServiceTableData tableData = serviceInput.getServiceTableData();
        for (ServiceTableRow tableRow : tableData.getServiceTableRows()) {
            dataContainer.addRowToTable(new DefaultRow(RowKey.createRowKey(rowKeyIndex++), getDataCells(tableRow, tableSpec, exec)));
        }
    }

    private static DataCell[] getDataCells(final ServiceTableRow tableRow, final DataTableSpec tableSpec, final ExecutionContext exec)
            throws InvalidSettingsException {
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
                    dataCell = new MissingCell("Could not parse: \"" + cellObjectString + "\" to type: \"" + columnType + "\"");
                }
                dataCells[i] = dataCell;
            }
        }
        return dataCells;
    }

}
