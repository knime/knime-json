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
package org.knime.json.node.serviceinputtable;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.Test;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.ContainerTable;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.json.servicetable.ServiceTable;
import org.knime.core.data.json.servicetable.ServiceTableColumnSpec;
import org.knime.core.data.json.servicetable.ServiceTableData;
import org.knime.core.data.json.servicetable.ServiceTableRow;
import org.knime.core.data.time.duration.DurationCellFactory;
import org.knime.core.data.time.localdate.LocalDateCellFactory;
import org.knime.core.data.time.localdatetime.LocalDateTimeCellFactory;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeCellFactory;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.DefaultNodeProgressMonitor;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.Node;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.SingleNodeContainer;
import org.knime.core.node.workflow.virtual.parchunk.VirtualParallelizedChunkPortObjectInNodeFactory;
import org.knime.json.node.service.output.table.ServiceOutputMapper;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Test suite for converting {@link BufferedDataTable} to {@link ServiceTable}.
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 */
public class BufferedDataTableToServiceTableTest {
    /**
     * Checks that an empty input table results in an empty ServiceTable.
     * @throws Exception
     */
    @Test
    public void testEmptyInputCreatesEmptyOutput() throws Exception {
        ExecutionContext exec = getTestExecutionContext();

        BufferedDataTable table = //
            new TestBufferedDataTableBuilder() //
                .build(exec); //

        ServiceTable serviceTable = ServiceOutputMapper.toServiceTable(table);
        List<ServiceTableRow> tableRows = serviceTable.getServiceTableData().getServiceTableRows();
        List<ServiceTableColumnSpec> tableSpecs = serviceTable.getServiceTableSpec().getServiceTableColumnSpecs();

        assertThat(tableRows, hasSize(0));
        assertThat(tableSpecs, hasSize(0));
    }

    /**
     * Checks that a string table spec from an input table is converted to an equivalent service table spec.
     * @throws Exception
     */
    @Test
    public void testStringColumnSpecIsCorrectlyConverted() throws Exception {
        ExecutionContext exec = getTestExecutionContext();

        BufferedDataTable table = //
            new TestBufferedDataTableBuilder() //
                .withColumnNames("column-string") //
                .withColumnTypes(StringCell.TYPE) //
                .build(exec); //

        ServiceTable serviceTable = ServiceOutputMapper.toServiceTable(table);
        List<ServiceTableColumnSpec> tableSpecs = serviceTable.getServiceTableSpec().getServiceTableColumnSpecs();

        ServiceTableColumnSpec stringColumnSpec = tableSpecs.get(0);
        assertThat(stringColumnSpec.getName(), equalTo("column-string"));
        assertThat(stringColumnSpec.getType(), equalTo("string"));
    }

    /**
     * Checks that a boolean table spec from an input table is converted to an equivalent service table spec.
     * @throws Exception
     */
    @Test
    public void testBooleanColumnSpecIsCorrectlyConverted() throws Exception {
        ExecutionContext exec = getTestExecutionContext();

        BufferedDataTable table = //
            new TestBufferedDataTableBuilder() //
                .withColumnNames("column-boolean") //
                .withColumnTypes(BooleanCell.TYPE) //
                .build(exec); //

        ServiceTable serviceTable = ServiceOutputMapper.toServiceTable(table);
        List<ServiceTableColumnSpec> tableSpecs = serviceTable.getServiceTableSpec().getServiceTableColumnSpecs();

        ServiceTableColumnSpec stringColumnSpec = tableSpecs.get(0);
        assertThat(stringColumnSpec.getName(), equalTo("column-boolean"));
        assertThat(stringColumnSpec.getType(), equalTo("boolean"));
    }

    /**
     * Checks that numeric column specs from the input table are converted to equivalent service table specs.
     * @throws Exception
     */
    @Test
    public void testNumericColumnSpecsAreCorrectlyConverted() throws Exception {
        ExecutionContext exec = getTestExecutionContext();

        BufferedDataTable table = //
            new TestBufferedDataTableBuilder() //
                .withColumnNames("column-int", "column-double", "column-long") //
                .withColumnTypes(IntCell.TYPE, DoubleCell.TYPE, LongCell.TYPE) //
                .build(exec); //

        ServiceTable serviceTable = ServiceOutputMapper.toServiceTable(table);
        List<ServiceTableColumnSpec> tableSpecs = serviceTable.getServiceTableSpec().getServiceTableColumnSpecs();

        ServiceTableColumnSpec intColumnSpec = tableSpecs.get(0);
        assertThat(intColumnSpec.getName(), equalTo("column-int"));
        assertThat(intColumnSpec.getType(), equalTo("int"));

        ServiceTableColumnSpec doubleColumnSpec = tableSpecs.get(1);
        assertThat(doubleColumnSpec.getName(), equalTo("column-double"));
        assertThat(doubleColumnSpec.getType(), equalTo("double"));

        ServiceTableColumnSpec longColumnSpec = tableSpecs.get(2);
        assertThat(longColumnSpec.getName(), equalTo("column-long"));
        assertThat(longColumnSpec.getType(), equalTo("long"));
    }

    /**
     * Checks that date column specs from the input table are converted to equivalent service table specs.
     * @throws Exception
     */
    @Test
    public void testDateColumnSpecsAreCorrectlyConverted() throws Exception {
        ExecutionContext exec = getTestExecutionContext();

        BufferedDataTable table = //
            new TestBufferedDataTableBuilder() //
                .withColumnNames("column-localdate", "column-localdatetime", "column-zoneddatetime") //
                .withColumnTypes(LocalDateCellFactory.TYPE, LocalDateTimeCellFactory.TYPE, ZonedDateTimeCellFactory.TYPE) //
                .build(exec); //

        ServiceTable serviceTable = ServiceOutputMapper.toServiceTable(table);
        List<ServiceTableColumnSpec> tableSpecs = serviceTable.getServiceTableSpec().getServiceTableColumnSpecs();

        ServiceTableColumnSpec localDateColumnSpec = tableSpecs.get(0);
        assertThat(localDateColumnSpec.getName(), equalTo("column-localdate"));
        assertThat(localDateColumnSpec.getType(), equalTo("localdate"));

        ServiceTableColumnSpec localDateTimeColumnSpec = tableSpecs.get(1);
        assertThat(localDateTimeColumnSpec.getName(), equalTo("column-localdatetime"));
        assertThat(localDateTimeColumnSpec.getType(), equalTo("localdatetime"));

        ServiceTableColumnSpec zonedDateTimeColumnSpec = tableSpecs.get(2);
        assertThat(zonedDateTimeColumnSpec.getName(), equalTo("column-zoneddatetime"));
        assertThat(zonedDateTimeColumnSpec.getType(), equalTo("zoneddatetime"));
    }

    /**
     * Checks that non primitive types use their names as column spec type.
     * @throws Exception
     */
    @Test
    public void testNonPrimitiveColumnSpecUsesTheirNameAsColumnSpecType() throws Exception {
        ExecutionContext exec = getTestExecutionContext();

        BufferedDataTable table = //
            new TestBufferedDataTableBuilder() //
                .withColumnNames("column-duration") //
                .withColumnTypes(DurationCellFactory.TYPE) //
                .build(exec); //

        ServiceTable serviceTable = ServiceOutputMapper.toServiceTable(table);
        List<ServiceTableColumnSpec> tableSpecs = serviceTable.getServiceTableSpec().getServiceTableColumnSpecs();

        ServiceTableColumnSpec durationColumnSpec = tableSpecs.get(0);
        assertThat(durationColumnSpec.getName(), equalTo("column-duration"));
        assertThat(durationColumnSpec.getType(), equalTo("Duration"));
    }

    /**
     * Checks that a data table row in a BufferedDataTable is converted to equivalent Service Table rows.
     * @throws Exception
     */
    @Test
    public void testRowsAreCorrectlyConverted() throws Exception {
        ExecutionContext exec = getTestExecutionContext();

        BufferedDataTable table = //
            new TestBufferedDataTableBuilder() //
                .withColumnNames("column-string", "column-int", "column-double", "column-boolean") //
                .withColumnTypes(StringCell.TYPE, IntCell.TYPE, DoubleCell.TYPE, BooleanCell.TYPE) //
                .withTableRow(new StringCell("first row"), new IntCell(111), new DoubleCell(1.1), BooleanCell.TRUE) //
                .withTableRow(new StringCell("second row"), new IntCell(222), new DoubleCell(2.2), BooleanCell.FALSE) //
                .build(exec); //

        ServiceTable serviceTable = ServiceOutputMapper.toServiceTable(table);
        ServiceTableData serviceTableData = serviceTable.getServiceTableData();
        List<ServiceTableRow> serviceTableRows = serviceTableData.getServiceTableRows();

        assertThat(serviceTableRows, hasSize(2));

        List<Object> firstRow = serviceTableRows.get(0).getDataCellObjects();
        assertThat(firstRow.get(0), is("first row"));
        assertThat((Integer) firstRow.get(1), is(111));
        assertThat((Double) firstRow.get(2), is(1.1));
        assertThat((Boolean) firstRow.get(3), is(true));

        List<Object> secondRow = serviceTableRows.get(1).getDataCellObjects();
        assertThat(secondRow.get(0), is("second row"));
        assertThat((Integer) secondRow.get(1), is(222));
        assertThat((Double) secondRow.get(2), is(2.2));
        assertThat((Boolean) secondRow.get(3), is(false));
    }

    /**
     * Checks that a service table created by the parse method conforms to the Service Table JSON structure.
     * @throws Exception
     */
    @Test
    public void testSerializingCreatedTable() throws Exception {
        ExecutionContext exec = getTestExecutionContext();

        BufferedDataTable table = //
            new TestBufferedDataTableBuilder() //
                .withColumnNames("column-string", "column-int", "column-double", "column-long", "column-boolean") //
                .withColumnTypes(StringCell.TYPE, IntCell.TYPE, DoubleCell.TYPE, LongCell.TYPE, BooleanCell.TYPE) //
                .withTableRow(new StringCell("test"), new IntCell(123), new DoubleCell(2.3), new LongCell(3000), BooleanCell.TRUE) //
                .build(exec); //

        ServiceTable serviceTable = ServiceOutputMapper.toServiceTable(table);

        ObjectMapper objectMapper = new ObjectMapper();
        String actualJson = objectMapper.writeValueAsString(serviceTable);
        String expectedJson =
            "{\"table-spec\":[{\"column-string\":\"string\"},{\"column-int\":\"int\"},{\"column-double\":\"double\"},"
                + "{\"column-long\":\"long\"},"
                + "{\"column-boolean\":\"boolean\"}],\"table-data\":[[\"test\",123,2.3,3000,true]]}";

        assertThat(actualJson, is(expectedJson));
    }

    /**
     * Checks that missing values are serialized as null.
     * @throws Exception
     */
    @Test
    public void testSerializedMissingValuesAreNull() throws Exception {
        ExecutionContext exec = getTestExecutionContext();

        BufferedDataTable table = //
            new TestBufferedDataTableBuilder() //
                .withColumnNames("column-string") //
                .withColumnTypes(StringCell.TYPE) //
                .withTableRow(DataType.getMissingCell()) //
                .build(exec); //

        ServiceTable serviceTable = ServiceOutputMapper.toServiceTable(table);

        ObjectMapper objectMapper = new ObjectMapper();
        String actualJson = objectMapper.writeValueAsString(serviceTable);
        String expectedJson =
                "{\"table-spec\":[{\"column-string\":\"string\"}],\"table-data\":[[null]]}";

        assertThat(actualJson, is(expectedJson));
    }

    /**
     * Helper class to build {@link BufferedDataTable} with desired specs and rows in a testing context.
     * Simplifies setting up text fixtures in a fluent and readable way.
     *
     * ONLY to be used in tests for setting up test fixtures!
     *
     * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
     */
    public static class TestBufferedDataTableBuilder {

        private String[] m_columnNames;
        private DataType[] m_columnTypes;
        private List<DataCell[]> m_tableRows;

        /**
         * Initializes an empty builder.
         */
        public TestBufferedDataTableBuilder() {
            m_columnNames = new String[0];
            m_columnTypes = new DataType[0];
            m_tableRows = new ArrayList<>();
        }

        /**
         * Sets the column names of this builder.
         * @param columnNames names to be set
         * @return this builder with the names set
         */
        public TestBufferedDataTableBuilder withColumnNames(final String... columnNames) {
            m_columnNames = columnNames;
            return this;
        }

        /**
         * Sets the column types of this builder.
         * @param columnTypes types to be set
         * @return this builder with the types set
         */
        public TestBufferedDataTableBuilder withColumnTypes(final DataType... columnTypes) {
            m_columnTypes = columnTypes;
            return this;
        }

        /**
         * Inserts the provided row to the builders rows. Does not perform any validity
         * checks, i.e. if the data cells in the row complies to the data types in the column types.
         *
         * @param row to be inserted in this builder
         * @return this builder with the row added
         */
        public TestBufferedDataTableBuilder withTableRow(final DataCell... row) {
            m_tableRows.add(row);
            return this;
        }

        /**
         * Builds a BufferedDataContainer within the provided execution context.
         * @param exec context in which the container is created
         * @return a BufferedDataContainer with the builders column names and types
         */
        public BufferedDataTable build(final ExecutionContext exec) {
            DataColumnSpec[] columnSpecs = DataTableSpec.createColumnSpecs(m_columnNames, m_columnTypes);
            DataTableSpec dataTableSpec = new DataTableSpec(columnSpecs);
            BufferedDataContainer dataContainer = exec.createDataContainer(dataTableSpec);

            long rowKeyIndex = 0L;
            for (DataCell[] row : m_tableRows) {
                dataContainer.addRowToTable(new DefaultRow(RowKey.createRowKey(rowKeyIndex++), row));
            }
            dataContainer.close();
            return dataContainer.getTable();
        }

    }

    private static ExecutionContext getTestExecutionContext() {
        @SuppressWarnings({"unchecked", "rawtypes"})
        NodeFactory<NodeModel> dummyFactory =
            (NodeFactory)new VirtualParallelizedChunkPortObjectInNodeFactory(new PortType[0]);
        return new ExecutionContext(new DefaultNodeProgressMonitor(), new Node(dummyFactory),
            SingleNodeContainer.MemoryPolicy.CacheOnDisc, new HashMap<Integer, ContainerTable>());
    }
}
