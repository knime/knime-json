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
package org.knime.json.node.container;

import static org.junit.Assert.assertEquals;

import org.knime.base.node.io.filereader.DataCellFactory;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.node.ExecutionContext;

/**
 * Helper class for asserting that a data row contains a given set of cells.
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 */
public class DataTableAssert {

    /**
     * Asserts that a data row contains all the provided cells.
     *
     * @param exec execution context
     * @param actualDataRow the actual data row
     * @param expectedDataCells the expected cells
     */
    public static void assertDataRow(
            final ExecutionContext exec,
            final DataRow actualDataRow,
            final DataCell... expectedDataCells) {
        assertEquals("Actual row has unexpected size", expectedDataCells.length, actualDataRow.getNumCells());
        for (int i = 0; i < expectedDataCells.length; i++) {
            DataCell actualDataCell = actualDataRow.getCell(i);
            DataCell expectedDataCell = expectedDataCells[i];
            assertEquals("Cells in column " + i + " missmatch", expectedDataCell, actualDataCell);
        }
    }

    /**
     * Asserts that a data row contains all the provided cells.
     *
     * @param exec execution context
     * @param actualDataRow the actual data row
     * @param expectedDataCells the expected cells
     */
    public static void assertDataRow(
            final ExecutionContext exec,
            final DataRow actualDataRow,
            final Object... expectedDataCells) {
        assertEquals("Actual row has unexpected size", expectedDataCells.length, actualDataRow.getNumCells());
        for (int i = 0; i < expectedDataCells.length; i++) {
            DataCell actualDataCell = actualDataRow.getCell(i);
            Object expectedDataCellObject = expectedDataCells[i];
            DataCell expectedDataCell = getExpectedDataCell(exec, actualDataCell, expectedDataCellObject);
            assertEquals("Cells in column " + i + " missmatch", expectedDataCell, actualDataCell);
        }
    }

    private static DataCell getExpectedDataCell(
            final ExecutionContext exec,
            final DataCell actualDataCell,
            final Object expectedDataCellObject) {
        if (expectedDataCellObject instanceof String && "missing value".equals(expectedDataCellObject)) {
            return DataType.getMissingCell();
        } else {
            DataCellFactory factory = new DataCellFactory(exec);
            return factory.createDataCellOfType(actualDataCell.getType(), expectedDataCellObject.toString());
        }
    }

    /**
     * Asserts that a data table spec contains all the given names in the given order.
     *
     * @param dataTableSpec actual spec
     * @param expectedNames expected names
     */
    public static void assertColumnNames(final DataTableSpec dataTableSpec, final String... expectedNames) {
        String[] actualNames = dataTableSpec.getColumnNames();
        assertEquals("The number of column names differ", expectedNames.length, actualNames.length);

        for (int i = 0; i < actualNames.length; i++) {
            assertEquals("Column names at index " + i + " should be equal", expectedNames[i], actualNames[i]);
        }
    }

    /**
     * Asserts that a data table spec contains all the given types in the given order.
     *
     * @param dataTableSpec actual spec
     * @param expectedTypes expected types
     */
    public static void assertColumnTypes(final DataTableSpec dataTableSpec, final DataType... expectedTypes) {
        assertEquals("The number of column types differ", expectedTypes.length, dataTableSpec.getNumColumns());

        for (int i = 0; i < dataTableSpec.getNumColumns(); i++) {
            assertEquals("Unexpected type in column " + i, expectedTypes[i], dataTableSpec.getColumnSpec(i).getType());
        }
    }
}
