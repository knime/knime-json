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
package org.knime.json.node.container.mappers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashMap;

import javax.json.JsonValue;

import org.junit.Test;
import org.knime.base.node.io.filereader.DataCellFactory;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowIterator;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.container.ContainerTable;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.json.container.table.ContainerTableColumnSpec;
import org.knime.core.data.json.container.table.ContainerTableJsonSchema;
import org.knime.core.data.time.duration.DurationCellFactory;
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
import org.knime.json.node.container.ContainerTableBuilder;

/**
 * Test suite for converting a {@link ContainerTableJsonSchema} to a {@link BufferedDataTable}.
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 */
public class ContainerTableToBufferedDataTableTest extends ContainerTableMapperTest {

    /**
     * Checks that null is not allowed as service input.
     * @throws InvalidSettingsException
     */
    @Test(expected = InvalidSettingsException.class)
    public void testNullAsServiceInputIsNotAllowed() throws InvalidSettingsException {
        ContainerTableMapper.toBufferedDataTable((ContainerTableJsonSchema) null, getTestExecutionCtx());
    }

    /**
     * Checks that a service input with valid column specs is converted into a
     * BufferedDataTable with these specs.
     *
     * @throws Exception
     */
    @Test
    public void testServiceInputWithValidSpecsCreatesTableWithTheSpecs() throws Exception {
        ContainerTableJsonSchema tableWithSpec = //
            new ContainerTableBuilder()//
                .withColumnSpec("column-string", "string")//
                .withColumnSpec("column-int", "int")//
                .withColumnSpec("column-double", "double")//
                .withColumnSpec("column-localdate", "localdate")//
                .build();//

        BufferedDataTable[] dataTable = ContainerTableMapper.toBufferedDataTable(tableWithSpec, getTestExecutionCtx());

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
    @Test(expected = InvalidSettingsException.class)
    public void testServiceInputWithDupliceSpecNamesThrowsAnException() throws Exception {
        ContainerTableJsonSchema tableWithSpec = //
            new ContainerTableBuilder()//
                .withColumnSpec("column-string", "string")//
                .withColumnSpec("column-string", "string")//
                .build();//

        ContainerTableMapper.toBufferedDataTable(tableWithSpec, getTestExecutionCtx());
    }

    /**
     * Checks that a name to any implementation of {@link DataCell} can be used
     * as the column type in the table-spec
     *
     * @throws Exception
     */
    @Test
    public void testServiceInputWithFullyQualifiedTypeNameAsColumnSepcTypeIsResolved() throws Exception {
        ContainerTableJsonSchema tableWithSpec = //
            new ContainerTableBuilder()//
                .withColumnSpec("column-FQ-name", "Duration")//
                .build();//

        BufferedDataTable[] dataTable = ContainerTableMapper.toBufferedDataTable(tableWithSpec, getTestExecutionCtx());
        DataTableSpec createdSpecs = dataTable[0].getSpec();

        assertTrue(createdSpecs.getColumnSpec(0).getType() == DurationCellFactory.TYPE);
    }

    /**
     * Checks that an exception is thrown when the table spec in the service input is null.
     *
     * @throws Exception
     */
    @Test(expected = InvalidSettingsException.class)
    public void testServiceInputWithNullTableSpecThrowsException() throws Exception {
        ContainerTableJsonSchema serviceInput = //
            new ContainerTableBuilder()//
                .withNullTableSpec()//
                .build();//

        ContainerTableMapper.toBufferedDataTable(serviceInput, getTestExecutionCtx());
    }

    /**
     * Checks that a service input with valid data rows is converted into a
     * BufferedDataTable with these rows.
     *
     * @throws Exception
     */
    @Test
    public void testServiceInputWithValidDataRowsCreatesTableWithTheRows() throws Exception {
        ContainerTableJsonSchema serviceInput = //
            new ContainerTableBuilder()//
                .withColumnSpecs(Arrays.asList(//
                    new ContainerTableColumnSpec("column-string", "string"), //
                    new ContainerTableColumnSpec("column-int", "int"), //
                    new ContainerTableColumnSpec("column-double", "double"), //
                    new ContainerTableColumnSpec("column-localdate", "localdate")))//
                .withTableRow("value1", 123, 4.5, "2018-03-27")//
                .withTableRow("value2", 432, 0.4, "2018-03-28")//
                .build();//

        BufferedDataTable[] dataTable = ContainerTableMapper.toBufferedDataTable(serviceInput, getTestExecutionCtx());

        try (CloseableRowIterator iterator = dataTable[0].iterator()) {
            assertTrue("First row should have been created", iterator.hasNext());
            DataRow firstRow = iterator.next();
            assertDataRow(firstRow, "value1", 123, 4.5, "2018-03-27");
            assertTrue("Second row should have been created", iterator.hasNext());
            DataRow secondRow = iterator.next();
            assertDataRow(secondRow, "value2", 432, 0.4, "2018-03-28");
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
        ContainerTableJsonSchema serviceInput = //
            new ContainerTableBuilder()//
                .withColumnSpec("column-unsupported", "not supported type")//
                .build();//

        ContainerTableMapper.toBufferedDataTable(serviceInput, getTestExecutionCtx());
    }

    /**
     * Checks that an exception is thrown when data in a row does not comply to the column specs.
     *
     * @throws Exception
     */
    @Test
    public void testColumnExpectingStringObjectsCastsCompatibleTypes() throws Exception {
        ContainerTableJsonSchema serviceInput = //
            new ContainerTableBuilder()//
                .withColumnSpec("column-string", "string")//
                .withTableRow(1)//
                .build();//

        ContainerTableMapper.toBufferedDataTable(serviceInput, getTestExecutionCtx());
    }

    /**
     * Checks that an exception is thrown when data in a row does not comply to the column specs.
     *
     * @throws Exception
     */
    @Test(expected = InvalidSettingsException.class)
    public void testCellWithWrongDataTypeThrowsException() throws Exception {
        ContainerTableJsonSchema serviceInput = //
            new ContainerTableBuilder()//
                .withColumnSpec("column-int", "int")//
                .withTableRow("Hello int column!")//
                .build();//

        ContainerTableMapper.toBufferedDataTable(serviceInput, getTestExecutionCtx());
    }

    /**
     * Checks that an null cell is parsed to missing value.
     *
     * @throws Exception
     */
    @Test
    public void testInputCellWithNullValueIsParsedToMissingValue() throws Exception {
        ContainerTableJsonSchema serviceInput = //
            new ContainerTableBuilder()//
                .withColumnSpec("column-localdate", "localdate")//
                .withTableRow((String) null)//
                .build();//

        BufferedDataTable[] dataTable = ContainerTableMapper.toBufferedDataTable(serviceInput, getTestExecutionCtx());
        try (CloseableRowIterator iterator = dataTable[0].iterator()) {
            assertTrue("First row should have been created", iterator.hasNext());
            DataRow dataRow = iterator.next();
            assertDataRow(dataRow, "missing value");
        }
    }

    /**
     * Checks that the columns are created in correct order
     *
     * @throws Exception
     */
    @Test
    public void testThatInputColumnOrderIsMaintained() throws Exception {
        ContainerTableJsonSchema serviceInput = //
            new ContainerTableBuilder()//
                .withColumnSpec("column-int1", "int")//
                .withColumnSpec("column-int2", "int")//
                .withColumnSpec("column-int3", "int")//
                .withTableRow(1, 2, 3)//
                .build();//

        BufferedDataTable[] dataTable = ContainerTableMapper.toBufferedDataTable(serviceInput, getTestExecutionCtx());

        try (CloseableRowIterator iterator = dataTable[0].iterator()) {
            assertTrue("Rows should have been created", iterator.hasNext());
            DataRow dataRow = iterator.next();
            assertDataRow(dataRow, 1, 2, 3);
        }
    }

     /**
      * Checks that a DataTable[] can be created for simple types without an ExecutionContext.
      *
     * @throws Exception
     */
    @Test
    public void testMappingToDataTableWithoutExecutionContext() throws Exception {
        ContainerTableJsonSchema containerTableJson = //
            new ContainerTableBuilder()//
                .withColumnSpec("column-int", "int")//
                .withColumnSpec("column-string", "string")//
                .withTableRow(1, "two")//
                .build();//

        DataTable[] dataTable = ContainerTableMapper.toDataTable(containerTableJson);

        RowIterator iterator = dataTable[0].iterator();
        assertTrue("Rows should have been created", iterator.hasNext());
        DataRow dataRow = iterator.next();
        assertDataRow(dataRow, 1, "two");
    }

    /**
    * Test that when a json input contains the table-spec object it is parsed according to that spec.
    *
    * @throws Exception
    */
   @Test
   public void testInJsonInputWithTableSpecRowsAreParsedAccordingToTableSpec() throws Exception {
       JsonValue inputWithSpecs = //
           new ContainerTableBuilder()//
               .withColumnSpec("column1", "int")
               .withColumnSpec("column2", "string")
               .withTableRow(1, "row 1")//
               .withTableRow(2, "row 2")//
               .buildAsJson();//

       JsonValue fallbackTable =
           new ContainerTableBuilder()
               .withColumnSpec("column1", "boolean")
               .withTableRow(true)//
               .buildAsJson();

       BufferedDataTable[] dataTable =
           ContainerTableMapper.toBufferedDataTable(inputWithSpecs, fallbackTable, getTestExecutionCtx());

       DataTableSpec createdSpecs = dataTable[0].getSpec();
       DataColumnSpec[] expectedColumnSpecs = //
           DataTableSpec.createColumnSpecs(//
               new String[]{"column1", "column2"}, //
               new DataType[]{IntCell.TYPE, StringCell.TYPE}//
           );//
       DataTableSpec expectedTableSpecs = new DataTableSpec(expectedColumnSpecs);

       assertTrue(createdSpecs.equalStructure(expectedTableSpecs));

       try (CloseableRowIterator iterator = dataTable[0].iterator()) {
           assertTrue("Rows should have been created", iterator.hasNext());
           DataRow firstRow = iterator.next();
           assertDataRow(firstRow, 1, "row 1");
           DataRow secondRow = iterator.next();
           assertDataRow(secondRow, 2, "row 2");
       }
   }

    /**
     * Test that when a json input does not contain the table-spec object it is parsed according to a provided
     * fall back tables spec.
     *
     * @throws Exception
     */
    @Test
    public void testInJsonInputWithoutTableSpecRowsAreParsedAccordingToFallbackTableSpec() throws Exception {
        JsonValue inputWithoutSpecs = //
            new ContainerTableBuilder()//
                .withNullTableSpec()
                .withTableRow(1, "row 1")//
                .withTableRow(2, "row 2")//
                .buildAsJson();//

        JsonValue fallbackTable =
            new ContainerTableBuilder()
                .withColumnSpec("column1", "int")
                .withColumnSpec("column2", "string")
                .withTableRow(123, "Should not be mapped!")//
                .buildAsJson();

        BufferedDataTable[] dataTable =
            ContainerTableMapper.toBufferedDataTable(inputWithoutSpecs, fallbackTable, getTestExecutionCtx());

        DataTableSpec createdSpecs = dataTable[0].getSpec();
        DataColumnSpec[] expectedColumnSpecs = //
            DataTableSpec.createColumnSpecs(//
                new String[]{"column1", "column2"}, //
                new DataType[]{IntCell.TYPE, StringCell.TYPE}//w
            );//
        DataTableSpec expectedTableSpecs = new DataTableSpec(expectedColumnSpecs);

        assertTrue(createdSpecs.equalStructure(expectedTableSpecs));

        try (CloseableRowIterator iterator = dataTable[0].iterator()) {
            assertTrue("Rows should have been created", iterator.hasNext());
            DataRow firstRow = iterator.next();
            assertDataRow(firstRow, 1, "row 1");
            DataRow secondRow = iterator.next();
            assertDataRow(secondRow, 2, "row 2");
        }
    }

    /**
     * Test that when a json input does not contain the table-spec object it is parsed according to a provided
     * fall back tables spec.
     *
     * @throws Exception
     */
    @Test(expected = InvalidSettingsException.class)
    public void testCellsNotComplyingToFallbackTableSpecThrowsException() throws Exception {
        JsonValue inputWithoutSpecs =
            new ContainerTableBuilder()
                .withNullTableSpec()
                .withTableRow("not an int value", "row 1")
                .buildAsJson();

        JsonValue fallbackTable =
            new ContainerTableBuilder()
                .withColumnSpec("column1", "int")
                .withColumnSpec("column2", "string")
                .withTableRow(1337, "Should not be mapped!")
                .buildAsJson();

        ContainerTableMapper.toBufferedDataTable(inputWithoutSpecs, fallbackTable, getTestExecutionCtx());
    }

    /**
     * Test that the fall back table must contain a table spec.
     *
     * @throws Exception
     */
    @Test(expected = InvalidSettingsException.class)
    public void testFallBackTableWithoutTableSpecThrowsAnException() throws Exception {
        JsonValue inputWithoutSpecs =
            new ContainerTableBuilder()
                .withNullTableSpec()
                .withTableRow("row 1")
                .buildAsJson();

        JsonValue fallbackTable =
            new ContainerTableBuilder()
                .withNullTableSpec()
                .withTableRow("Should not be mapped!")
                .buildAsJson();

        ContainerTableMapper.toBufferedDataTable(inputWithoutSpecs, fallbackTable, getTestExecutionCtx());
    }

    /**
     * Tests that the fall back is ignored when input has valid spec.
     *
     * @throws Exception
     */
    @Test
    public void testFallbackIsIgnoredWhenInputHasTableSpec() throws Exception {
        JsonValue inputWithSpecs =
            new ContainerTableBuilder()
                .withColumnSpec("column1", "string")
                .withColumnSpec("column2", "boolean")
                .withColumnSpec("column3", "double")
                .buildAsJson();

        JsonValue fallbackTable =
            new ContainerTableBuilder()
                .withNullTableSpec()
                .buildAsJson();

        DataTableSpec createdSpecs = ContainerTableMapper.toTableSpec(inputWithSpecs, fallbackTable);
        DataColumnSpec[] expectedColumnSpecs = //
                DataTableSpec.createColumnSpecs(//
                    new String[]{"column1", "column2", "column3"}, //
                    new DataType[]{StringCell.TYPE, BooleanCell.TYPE, DoubleCell.TYPE} //
                        );//
        DataTableSpec expectedTableSpecs = new DataTableSpec(expectedColumnSpecs);

        assertTrue(createdSpecs.equalStructure(expectedTableSpecs));
    }

    /**
     * Tests that the fall back is used when input has no spec.
     *
     * @throws Exception
     */
    @Test
    public void testFallbackIsUsedWhenInputHasNoTableSpec() throws Exception {
        JsonValue inputWithoutSpecs =
            new ContainerTableBuilder()
                .withNullTableSpec()
                .buildAsJson();

        JsonValue fallbackTable =
            new ContainerTableBuilder()
                .withColumnSpec("column1", "string")
                .withColumnSpec("column2", "boolean")
                .withColumnSpec("column3", "double")
                .buildAsJson();

        DataTableSpec createdSpecs = ContainerTableMapper.toTableSpec(inputWithoutSpecs, fallbackTable);
        DataColumnSpec[] expectedColumnSpecs = //
            DataTableSpec.createColumnSpecs(//
                new String[]{"column1", "column2", "column3"}, //
                new DataType[]{StringCell.TYPE, BooleanCell.TYPE, DoubleCell.TYPE} //
            );//
        DataTableSpec expectedTableSpecs = new DataTableSpec(expectedColumnSpecs);

        assertTrue(createdSpecs.equalStructure(expectedTableSpecs));
    }

    private static void assertDataRow(final DataRow actualDataRow, final Object... expectedDataCells)
            throws InvalidSettingsException {
        assertEquals("Actual row has unexpected size" ,expectedDataCells.length, actualDataRow.getNumCells());
        for (int i = 0; i < expectedDataCells.length; i++) {
            DataCell actualDataCell = actualDataRow.getCell(i);
            Object expectedDataCellObject = expectedDataCells[i];

            DataCell expectedDataCell;
            if (expectedDataCellObject instanceof String && "missing value".equals(expectedDataCellObject)) {
                expectedDataCell = DataType.getMissingCell();
            } else {
                DataCellFactory factory = new DataCellFactory(getTestExecutionCtx());
                expectedDataCell =
                    factory.createDataCellOfType(actualDataCell.getType(), expectedDataCellObject.toString());
            }
            assertEquals("Cells in column " + i + " missmatch", expectedDataCell, actualDataCell);
        }
    }

    @SuppressWarnings("deprecation")
    private static ExecutionContext getTestExecutionCtx() {
        @SuppressWarnings({"unchecked", "rawtypes"})
        NodeFactory<NodeModel> dummyFactory =
            (NodeFactory)new VirtualParallelizedChunkPortObjectInNodeFactory(new PortType[0]);
        return new ExecutionContext(new DefaultNodeProgressMonitor(), new Node(dummyFactory),
            SingleNodeContainer.MemoryPolicy.CacheOnDisc, new HashMap<Integer, ContainerTable>());
    }

}
