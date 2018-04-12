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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashMap;

import org.junit.Test;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.container.ContainerTable;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.time.localdate.LocalDateCellFactory;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.DefaultNodeProgressMonitor;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.Node;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.SingleNodeContainer;
import org.knime.core.node.workflow.virtual.parchunk.VirtualParallelizedChunkPortObjectInNodeFactory;

/**
 *
 * @author Tobias Urhaug
 */
public class ServiceInToTableTest {

    /**
     * Checks that null is not allowed as service input.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testNullAsServiceInputIsNotAllowed() {
        ServiceInput serviceInput = null;
        new ServiceInputToTable(serviceInput, getTestExecutionContext());
    }

    /**
     * Checks that a service input with valid column specs is converted into a
     * BufferedDataTable with these specs.
     *
     * @throws Exception
     */
    @Test
    public void testServiceInputWithValidSpecsCreatesTableWithTheSpecs() throws Exception {
        ServiceInput tableWithSpec = //
            new ServiceInputBuilder()//
                .withColumnSpec("column-string", "string")//
                .withColumnSpec("column-int", "int")//
                .withColumnSpec("column-double", "double")//
                .withColumnSpec("column-localdate", "localdate")//
                .build();//

        ServiceInputToTable serviceInputToTable = new ServiceInputToTable(tableWithSpec, getTestExecutionContext());
        BufferedDataTable[] dataTable = serviceInputToTable.toBufferedDataTable();

        DataTableSpec createdSpecs = dataTable[0].getSpec();
        DataColumnSpec[] expectedColumnSpecs = //
            DataTableSpec.createColumnSpecs(//
                new String[]{"column-string", "column-int", "column-double", "column-localdate"}, //
                new DataType[]{StringCell.TYPE, IntCell.TYPE, DoubleCell.TYPE, LocalDateCellFactory.TYPE});//
        DataTableSpec expectedTableSpecs = new DataTableSpec(expectedColumnSpecs);

        assertTrue(createdSpecs.equalStructure(expectedTableSpecs));
    }

    /**
     * Checks that a service input with valid column specs is converted into a
     * BufferedDataTable with these specs.
     *
     * @throws Exception
     */
    @Test
    public void testServiceInputWithDupliceSpecNamesAreHandled() throws Exception {
        ServiceInput tableWithSpec = //
            new ServiceInputBuilder()//
                .withColumnSpec("column-string", "string")//
                .withColumnSpec("column-string", "string")//
                .build();//

        ServiceInputToTable serviceInputToTable = new ServiceInputToTable(tableWithSpec, getTestExecutionContext());
        BufferedDataTable[] dataTable = serviceInputToTable.toBufferedDataTable();

        DataTableSpec createdSpecs = dataTable[0].getSpec();

        String[] columnNames = createdSpecs.getColumnNames();
        assertEquals("column-string", columnNames[0]);
        assertEquals("column-string (#1)", columnNames[1]);
    }

    /**
     * Checks that an exception is thrown when the table spec in the service input is null.
     *
     * @throws Exception
     */
    @Test(expected = InvalidSettingsException.class)
    public void testServiceInputWithNullTableSpecThrowsException() throws Exception {
        ServiceInput serviceInput = //
                new ServiceInputBuilder()//
                .withNullTableSpec()//
                .build();//

        ServiceInputToTable tableCreator = new ServiceInputToTable(serviceInput, getTestExecutionContext());
        tableCreator.toBufferedDataTable();
    }

    /**
     * Checks that a service input with valid data rows is converted into a
     * BufferedDataTable with these rows.
     *
     * @throws Exception
     */
    @Test
    public void testServiceInputWithValidDataRowsCreatesTableWithTheRows() throws Exception {
        ServiceInput serviceInput = //
            new ServiceInputBuilder()//
                .withColumnSpecs(Arrays.asList(//
                    new ServiceInputColumnSpec("column-string", "string"), //
                    new ServiceInputColumnSpec("column-int", "int"), //
                    new ServiceInputColumnSpec("column-double", "double"), //
                    new ServiceInputColumnSpec("column-localdate", "localdate")))//
                .withTableRow("value1", 123, 4.5, "2018-03-27")//
                .withTableRow("value2", 432, 0.4, "2018-03-28")//
                .build();//

        ServiceInputToTable serviceInputToTable = new ServiceInputToTable(serviceInput, getTestExecutionContext());
        BufferedDataTable[] dataTable = serviceInputToTable.toBufferedDataTable();

        CloseableRowIterator iterator = dataTable[0].iterator();
        assertTrue("Rows should have been created", iterator.hasNext());
        if (iterator.hasNext()) {
            DataRow dataRow = iterator.next();
            assertDataRow(dataRow, "value1", 123, 4.5, "2018-03-27");
        }
        if (iterator.hasNext()) {
            DataRow dataRow = iterator.next();
            assertDataRow(dataRow, "value2", 432, 0.4, "2018-03-28");
        }
    }

    /**
     * Checks that the table conversion correctly throws an Exception when an unsupported data type is used as column
     * spec.
     *
     * @throws Exception
     */
    @Test(expected = InvalidSettingsException.class)
    public void testUnsupportedDataTypeColumnSpecThrowsException() throws Exception {
        ServiceInput serviceInput = //
            new ServiceInputBuilder()//
                .withColumnSpec("column-unsupported", "not supported type")//
                .build();//

        ServiceInputToTable tableCreator = new ServiceInputToTable(serviceInput, getTestExecutionContext());
        tableCreator.toBufferedDataTable();
    }

    /**
     * Checks that an exception is thrown when data in a row does not comply to the column specs.
     *
     * @throws Exception
     */
    @Test(expected = InvalidSettingsException.class)
    public void testColumnExpectingStringObjectsThrowsExceptionWhenGivenOtherDataType() throws Exception {
        ServiceInput serviceInput = //
            new ServiceInputBuilder()//
                .withColumnSpec("column-string", "string")//
                .withTableRow(2.0)//
                .build();//

        ServiceInputToTable tableCreator = new ServiceInputToTable(serviceInput, getTestExecutionContext());
        tableCreator.toBufferedDataTable();
    }

    /**
     * Checks that an exception is thrown when data in a row does not comply to the column specs.
     *
     * @throws Exception
     */
    @Test(expected = InvalidSettingsException.class)
    public void testColumnExpectingDoubleObjectsThrowsExceptionWhenGivenWrongDataType() throws Exception {
        ServiceInput serviceInput = //
            new ServiceInputBuilder()//
                .withColumnSpec("column-double", "double")//
                .withTableRow(2)//
                .build();//

        ServiceInputToTable tableCreator = new ServiceInputToTable(serviceInput, getTestExecutionContext());
        tableCreator.toBufferedDataTable();
    }

    /**
     * Checks that an exception is thrown when data in a row does not comply to the column specs.
     *
     * @throws Exception
     */
    @Test(expected = InvalidSettingsException.class)
    public void testColumnExpectingIntegerObjectsThrowsExceptionWhenGivenWrongDataType() throws Exception {
        ServiceInput serviceInput = //
            new ServiceInputBuilder()//
                .withColumnSpec("column-int", "int")//
                .withTableRow("Hello int column!")//
                .build();//

        ServiceInputToTable tableCreator = new ServiceInputToTable(serviceInput, getTestExecutionContext());
        tableCreator.toBufferedDataTable();
    }

    /**
     * Checks that an exception is thrown when data in a row does not comply to the column specs.
     *
     * @throws Exception
     */
    @Test(expected = InvalidSettingsException.class)
    public void testColumnExpectingLocalDatesThrowsExceptionWhenGivenWrongDataType() throws Exception {
        ServiceInput serviceInput = //
            new ServiceInputBuilder()//
                .withColumnSpec("column-localdate", "localdate")//
                .withTableRow(2.4)//
                .build();//

        ServiceInputToTable tableCreator = new ServiceInputToTable(serviceInput, getTestExecutionContext());
        tableCreator.toBufferedDataTable();
    }

    /**
     * Checks that an exception is thrown when data in a row does not comply to the column specs.
     *
     * @throws Exception
     */
    @Test(expected = InvalidSettingsException.class)
    public void testColumnExpectingLocalDatesThrowsExceptionWhenGivenStringNotRepresentingALocalDate() throws Exception {
        ServiceInput serviceInput = //
            new ServiceInputBuilder()//
                .withColumnSpec("column-localdate", "localdate")//
                .withTableRow("this is not a local date!")//
                .build();//

        // TODO TU: which exception should be thrown here? Date time parsing exception gives better context.

        ServiceInputToTable tableCreator = new ServiceInputToTable(serviceInput, getTestExecutionContext());
        tableCreator.toBufferedDataTable();
    }

    /**
     * Checks that the columns are created in correct order
     *
     * @throws Exception
     */
    @Test
    public void testThatInputColumnOrderIsMaintained() throws Exception {
        ServiceInput serviceInput = //
            new ServiceInputBuilder()//
                .withColumnSpec("column-int1", "int")//
                .withColumnSpec("column-int2", "int")//
                .withColumnSpec("column-int3", "int")//
                .withTableRow(1, 2, 3)//
                .build();//

        ServiceInputToTable serviceInputToTable = new ServiceInputToTable(serviceInput, getTestExecutionContext());
        BufferedDataTable[] dataTable = serviceInputToTable.toBufferedDataTable();

        CloseableRowIterator iterator = dataTable[0].iterator();
        assertTrue("Rows should have been created", iterator.hasNext());
        if (iterator.hasNext()) {
            DataRow dataRow = iterator.next();
            assertDataRow(dataRow, 1, 2, 3);
        }
    }




    /*
     * TODO TU:
     *
     * Some interesting test cases
     *
     *  - null values / missing values
     *  - local dates of different valid formats
     *  - string column type with an input that can be cast to string? Boolean, Integer etc. What should happen??
     *
     *
     *  # go over documentation
     */

    private void assertDataRow(final DataRow actualDataRow, final Object... expectedDataCells) throws InvalidSettingsException {
        assertEquals("Actual row has unexpected size" ,expectedDataCells.length, actualDataRow.getNumCells());
        for (int i = 0; i < expectedDataCells.length; i++) {
            DataCell actualDataCell = actualDataRow.getCell(i);
            Object expectedDataCellObject = expectedDataCells[i];
            DataCell expectedDataCell = ServiceInputValidDataTypeFactory.of(actualDataCell.getType()).parseToDataCall(expectedDataCellObject);
            assertEquals(expectedDataCell, actualDataCell);
        }
    }

    private ExecutionContext getTestExecutionContext() {
        @SuppressWarnings({"unchecked", "rawtypes"})
        NodeFactory<NodeModel> dummyFactory =
            (NodeFactory) new VirtualParallelizedChunkPortObjectInNodeFactory(new PortType[0]);
        return new ExecutionContext(//
            new DefaultNodeProgressMonitor(), //
            new Node(dummyFactory), //
            SingleNodeContainer.MemoryPolicy.CacheOnDisc, //
            new HashMap<Integer, ContainerTable>());//
    }

}
