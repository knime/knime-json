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
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import org.junit.Test;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.container.ContainerTable;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
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
import org.knime.json.node.container.DataTableAssert;
import org.knime.json.node.container.mappers.BufferedDataTableToContainerTableTest.TestBufferedDataTableBuilder;
import org.knime.json.util.JSONUtil;

/**
 * Tests suite for mapping {@link JsonValue} representing a single row of data to a single row {@link DataTable}.
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 */
public class ContainerRowMapperTest {

    /**
     * Tests that a null input throws an exception.
     *
     * @throws Exception
     */
    @Test(expected = InvalidSettingsException.class)
    public void testNullInputThrowsException() throws Exception {
        ContainerRowMapper.toDataTable(null, null);
    }

    /**
     * Tests that an input representing a row containing a single string cell is mapped to a string column.
     *
     * @throws Exception
     */
    @Test
    public void testStringValueIsMappedToString() throws Exception {
        JsonValue input = new JsonValueBuilder().withStringObject("column1", "A string value").build();

        ExecutionContext testExec = getTestExecutionCtx();
        DataTable dataTable = ContainerRowMapper.toDataTable(input, testExec);
        DataTableSpec dataTableSpec = dataTable.getDataTableSpec();

        DataTableAssert.assertColumnNames(dataTableSpec, "column1");
        DataTableAssert.assertColumnTypes(dataTableSpec, StringCell.TYPE);
        for (DataRow row : dataTable) {
            DataTableAssert.assertDataRow(testExec, row, "A string value");
        }
    }

    /**
     * Tests that an input representing a row containing a single double cell is mapped to a double column.
     *
     * @throws Exception
     */
    @Test
    public void testNumericFloatingPointValueIsMappedToDouble() throws Exception {
        JsonValue input = new JsonValueBuilder().withDoubleObject("double-column", 123.4).build();

        ExecutionContext testExec = getTestExecutionCtx();
        DataTable dataTable = ContainerRowMapper.toDataTable(input, testExec);
        DataTableSpec dataTableSpec = dataTable.getDataTableSpec();

        DataTableAssert.assertColumnNames(dataTableSpec, "double-column");
        DataTableAssert.assertColumnTypes(dataTableSpec, DoubleCell.TYPE);
        for (DataRow row : dataTable) {
            DataTableAssert.assertDataRow(testExec, row, 123.4);
        }
    }

    /**
     * Tests that an input representing a row containing a single integer-valued double cell
     * is mapped to a double column.
     *
     * @throws Exception
     */
    @Test
    public void testNumericFloatingPointIntegerValueIsMappedToDouble() throws Exception {
        JsonValue input = new JsonValueBuilder().withDoubleObject("double-column", 400.0).build();

        ExecutionContext testExec = getTestExecutionCtx();
        DataTable dataTable = ContainerRowMapper.toDataTable(input, testExec);
        DataTableSpec dataTableSpec = dataTable.getDataTableSpec();

        DataTableAssert.assertColumnNames(dataTableSpec, "double-column");
        DataTableAssert.assertColumnTypes(dataTableSpec, DoubleCell.TYPE);
        for (DataRow row : dataTable) {
            DataTableAssert.assertDataRow(testExec, row, 400.0);
        }
    }

    /**
     * Tests that an input representing a row containing a single boolean cell is mapped to a Boolean column.
     *
     * @throws Exception
     */
    @Test
    public void testBooleanValueIsMappedToBoolean() throws Exception {
        JsonValue input = new JsonValueBuilder().withBooleanObject("boolean-column", true).build();

        ExecutionContext testExec = getTestExecutionCtx();
        DataTable dataTable = ContainerRowMapper.toDataTable(input, testExec);
        DataTableSpec dataTableSpec = dataTable.getDataTableSpec();

        DataTableAssert.assertColumnNames(dataTableSpec, "boolean-column");
        DataTableAssert.assertColumnTypes(dataTableSpec, BooleanCell.TYPE);
        for (DataRow row : dataTable) {
            DataTableAssert.assertDataRow(testExec, row, true);
        }
    }

    /**
     * Tests that an input representing a row containing a single int cell is mapped to an Integer column.
     *
     * @throws Exception
     */
    @Test
    public void testNumericValueInIntegerRangeIsMappedToInteger() throws Exception {
        JsonValue input = new JsonValueBuilder().withIntObject("integer-column", 42).build();

        ExecutionContext testExec = getTestExecutionCtx();
        DataTable dataTable = ContainerRowMapper.toDataTable(input, testExec);
        DataTableSpec dataTableSpec = dataTable.getDataTableSpec();

        DataTableAssert.assertColumnNames(dataTableSpec, "integer-column");
        DataTableAssert.assertColumnTypes(dataTableSpec, IntCell.TYPE);
        for (DataRow row : dataTable) {
            DataTableAssert.assertDataRow(testExec, row, 42);
        }
    }

    /**
     * Tests that an input representing a row containing a single long cell is mapped to a Long column.
     *
     * @throws Exception
     */
    @Test
    public void testNumericValueOverMaxIntegerIsMappedToLong() throws Exception {
        Long bigLong = Long.MAX_VALUE;
        JsonValue input = new JsonValueBuilder().withLongObject("big-long-column", bigLong).build();

        ExecutionContext testExec = getTestExecutionCtx();
        DataTable dataTable = ContainerRowMapper.toDataTable(input, testExec);
        DataTableSpec dataTableSpec = dataTable.getDataTableSpec();

        DataTableAssert.assertColumnNames(dataTableSpec, "big-long-column");
        DataTableAssert.assertColumnTypes(dataTableSpec, LongCell.TYPE);
        for (DataRow row : dataTable) {
            DataTableAssert.assertDataRow(testExec, row, bigLong);
        }
    }

    /**
     * Tests that a numeric value larger than java.Long.MAX_VALUE throws exception.
     *
     * @throws Exception
     */
    @Test(expected = InvalidSettingsException.class)
    public void testNumericValueOverMaxLongThrowsException() throws Exception {
        BigInteger aboveJavaLong = new BigDecimal("2E20").toBigInteger();
        JsonValue input = new JsonValueBuilder().withBigIntegerObject("above-java-long-column", aboveJavaLong).build();

        ExecutionContext testExec = getTestExecutionCtx();
        ContainerRowMapper.toDataTable(input, testExec);
    }

    /**
     * Tests that an input containing multiple objects is parsed correctly.
     *
     * @throws Exception
     */
    @Test
    public void testRowWithMultipleCellsIsParsed() throws Exception {
        JsonValue input =
            new JsonValueBuilder()
                .withLongObject("long-column", Long.MAX_VALUE)
                .withBooleanObject("boolean-column", false)
                .withStringObject("string-column", "a string")
                .withDoubleObject("double-column", 4321.1234)
                .build();

        ExecutionContext testExec = getTestExecutionCtx();
        DataTable dataTable = ContainerRowMapper.toDataTable(input, testExec);
        DataTableSpec dataTableSpec = dataTable.getDataTableSpec();

        String[] expectedColumnNames =
            new String[]{"long-column", "boolean-column", "string-column", "double-column"};
        DataTableAssert.assertColumnNames(dataTableSpec, expectedColumnNames);

        DataType[] expectedColumnTypes =
            new DataType[]{LongCell.TYPE, BooleanCell.TYPE, StringCell.TYPE, DoubleCell.TYPE};
        DataTableAssert.assertColumnTypes(dataTableSpec, expectedColumnTypes);

        for (DataRow row : dataTable) {
            DataTableAssert.assertDataRow(testExec, row, Long.MAX_VALUE, false, "a string", 4321.1234);
        }
    }

    /**
     * Tests input containing an array throws an exception.
     *
     * @throws Exception
     */
    @Test(expected = InvalidSettingsException.class)
    public void testArrayValueIsNotAllowed() throws Exception {
        JsonValue input =
            new JsonValueBuilder()
                .withStringArrayObject("string-array-column", "A", "B", "C")
                .build();

        ExecutionContext testExec = getTestExecutionCtx();
        ContainerRowMapper.toDataTable(input, testExec);
    }

    /**
     * Tests input containing a json object parses its string representation.
     *
     * @throws Exception
     */
    @Test
    public void testJsonObjectValueIsMappedToItsStringRepresentation() throws Exception {
        JsonValue input =
                new JsonValueBuilder()
                    .withStringObject("string", "abc")
                    .withJsonPersonObject()
                    .withBooleanObject("boolean", true)
                    .build();

        ExecutionContext testExec = getTestExecutionCtx();
        BufferedDataTable dataTable = ContainerRowMapper.toDataTable(input, testExec);

        DataTableSpec dataTableSpec = dataTable.getDataTableSpec();

        String[] expectedColumnNames = new String[]{"string", "person", "boolean"};
        DataTableAssert.assertColumnNames(dataTableSpec, expectedColumnNames);

        DataType[] expectedColumnTypes = new DataType[]{StringCell.TYPE, StringCell.TYPE, BooleanCell.TYPE};
        DataTableAssert.assertColumnTypes(dataTableSpec, expectedColumnTypes);

        for (DataRow row : dataTable) {
            DataTableAssert.assertDataRow(testExec, row, "abc", "{\"name\":\"Flodve\",\"age\":32}", true);
        }
    }

    /**
     * Tests that an input is correctly parsed to its spec
     *
     * @throws Exception
     */
    @Test
    public void testDataTableSpecIsCorrectlyParsed() throws Exception {
        JsonValue input =
            new JsonValueBuilder()
                .withLongObject("long-column", Long.MAX_VALUE)
                .withBooleanObject("boolean-column", false)
                .withStringObject("string-column", "a string")
                .withDoubleObject("double-column", 4321.1234)
                .build();

        DataTableSpec dataTableSpec = ContainerRowMapper.toTableSpec(input);

        String[] expectedColumnNames =
            new String[]{"long-column", "boolean-column", "string-column", "double-column"};
        DataTableAssert.assertColumnNames(dataTableSpec, expectedColumnNames);

        DataType[] expectedColumnTypes =
            new DataType[]{LongCell.TYPE, BooleanCell.TYPE, StringCell.TYPE, DoubleCell.TYPE};
        DataTableAssert.assertColumnTypes(dataTableSpec, expectedColumnTypes);
    }

    /**
     * Tests that a row is parsed according to the types of a template row.
     *
     * @throws Exception
     */
    @Test
    public void testRowIsParsedAccordingToTemplateWhenPresent() throws Exception {
        ExecutionContext testExec = getTestExecutionCtx();

        BufferedDataTable templateRow =
            new TestBufferedDataTableBuilder()
                .withColumnNames("string-column", "local-date-column")
                .withColumnTypes(StringCell.TYPE, LocalDateCellFactory.TYPE)
                .withTableRow(new StringCell("template"), LocalDateCellFactory.create("1987-01-21"))
                .build(testExec);

        JsonValue input =
            new JsonValueBuilder()
                .withStringObject("string-column", "2018-02-01")
                .withStringObject("local-date-column", "2018-01-31")
                .build();

        ContainerRowMapperInputHandling containerRowInputHandling =
            new ContainerRowMapperInputHandling(MissingColumnHandling.FILL_WITH_MISSING_VALUE, false, false);

        BufferedDataTable dataTable =
            ContainerRowMapper.toDataTable(
                input,
                templateRow.getDataTableSpec(),
                containerRowInputHandling,
                testExec
            );
        DataTableSpec dataTableSpec = dataTable.getDataTableSpec();

        String[] expectedColumnNames = new String[]{"string-column", "local-date-column"};
        DataTableAssert.assertColumnNames(dataTableSpec, expectedColumnNames);

        DataType[] expectedColumnTypes = new DataType[]{StringCell.TYPE, LocalDateCellFactory.TYPE};
        DataTableAssert.assertColumnTypes(dataTableSpec, expectedColumnTypes);

        for (DataRow row : dataTable) {
            DataTableAssert.assertDataRow(
                testExec,
                row,
                new StringCell("2018-02-01"), LocalDateCellFactory.create("2018-01-31")
            );
        }
    }

    /**
     * Tests that missing columns are parsed as missing values.
     *
     * @throws Exception
     */
    @Test
    public void testMissingColumnsShouldBeReplacedByMissingValues() throws Exception {
        ExecutionContext testExec = getTestExecutionCtx();

        BufferedDataTable templateRow =
            new TestBufferedDataTableBuilder()
                .withColumnNames("A", "B")
                .withColumnTypes(StringCell.TYPE, IntCell.TYPE)
                .withTableRow(new StringCell("a"), new IntCell(1))
                .build(testExec);

        JsonValue input =
            new JsonValueBuilder()
                .withStringObject("A", "input string")
                .build();

        ContainerRowMapperInputHandling containerRowInputHandling =
                new ContainerRowMapperInputHandling(MissingColumnHandling.FILL_WITH_MISSING_VALUE, false, false);

        BufferedDataTable dataTable =
            ContainerRowMapper.toDataTable(input, templateRow.getDataTableSpec(), containerRowInputHandling, testExec);
        DataTableSpec dataTableSpec = dataTable.getDataTableSpec();

        String[] expectedColumnNames = new String[]{"A", "B"};
        DataTableAssert.assertColumnNames(dataTableSpec, expectedColumnNames);

        DataType[] expectedColumnTypes = new DataType[]{StringCell.TYPE, IntCell.TYPE};
        DataTableAssert.assertColumnTypes(dataTableSpec, expectedColumnTypes);

        for (DataRow row : dataTable) {
            DataTableAssert.assertDataRow(testExec, row, new StringCell("input string"), DataType.getMissingCell());
        }
    }

    /**
     * Tests that missing columns are ignored in the output.
     *
     * @throws Exception
     */
    @Test
    public void testMissingColumnsShouldBeIgnoredInOutput() throws Exception {
        ExecutionContext testExec = getTestExecutionCtx();

        BufferedDataTable templateRow =
            new TestBufferedDataTableBuilder()
                .withColumnNames("A", "B")
                .withColumnTypes(StringCell.TYPE, IntCell.TYPE)
                .withTableRow(new StringCell("a"), new IntCell(1))
                .build(testExec);

        JsonValue input =
            new JsonValueBuilder()
                .withStringObject("A", "input string")
                .build();

        ContainerRowMapperInputHandling containerRowInputHandling =
                new ContainerRowMapperInputHandling(MissingColumnHandling.IGNORE, false, false);

        BufferedDataTable dataTable =
                ContainerRowMapper.toDataTable(input, templateRow.getDataTableSpec(), containerRowInputHandling, testExec);
        DataTableSpec dataTableSpec = dataTable.getDataTableSpec();

        String[] expectedColumnNames = new String[]{"A"};
        DataTableAssert.assertColumnNames(dataTableSpec, expectedColumnNames);

        DataType[] expectedColumnTypes = new DataType[]{StringCell.TYPE};
        DataTableAssert.assertColumnTypes(dataTableSpec, expectedColumnTypes);

        for (DataRow row : dataTable) {
            DataTableAssert.assertDataRow(testExec, row, new StringCell("input string"));
        }
    }

    /**
     * Tests that an exception is thrown when missing columns are expected to fail.
     *
     * @throws Exception
     */
    @Test(expected = InvalidSettingsException.class)
    public void testMissingColumnsShouldThrowException() throws Exception {
        ExecutionContext testExec = getTestExecutionCtx();

        BufferedDataTable templateRow =
            new TestBufferedDataTableBuilder()
                .withColumnNames("A", "B")
                .withColumnTypes(StringCell.TYPE, IntCell.TYPE)
                .withTableRow(new StringCell("a"), new IntCell(1))
                .build(testExec);

        JsonValue input =
            new JsonValueBuilder()
                .withStringObject("A", "input string")
                .build();

        ContainerRowMapperInputHandling containerRowInputHandling =
                new ContainerRowMapperInputHandling(MissingColumnHandling.FAIL, false, false);

        ContainerRowMapper.toDataTable(input, templateRow.getDataTableSpec(), containerRowInputHandling, testExec);
    }

    /**
     * Tests that superfluous columns are appended at the end.
     *
     * @throws Exception
     */
    @Test
    public void testSuperfluousColumnsShouldBeAppendedAtTheEnd() throws Exception {
        ExecutionContext testExec = getTestExecutionCtx();

        BufferedDataTable templateRow =
            new TestBufferedDataTableBuilder()
                .withColumnNames("A", "B")
                .withColumnTypes(StringCell.TYPE, IntCell.TYPE)
                .withTableRow(new StringCell("a"), new IntCell(1))
                .build(testExec);

        JsonValue input =
            new JsonValueBuilder()
                .withStringObject("A", "input string")
                .withStringObject("superfluous", "append me at the end!")
                .withIntObject("B", 444)
                .build();

        boolean appendSuperfluousColumns = true;
        ContainerRowMapperInputHandling containerRowInputHandling =
            new ContainerRowMapperInputHandling(MissingColumnHandling.FAIL, appendSuperfluousColumns, false);

        BufferedDataTable dataTable =
            ContainerRowMapper.toDataTable(input, templateRow.getDataTableSpec(), containerRowInputHandling, testExec);
        DataTableSpec dataTableSpec = dataTable.getDataTableSpec();

        String[] expectedColumnNames = new String[]{"A", "B", "superfluous"};
        DataTableAssert.assertColumnNames(dataTableSpec, expectedColumnNames);

        DataType[] expectedColumnTypes = new DataType[]{StringCell.TYPE, IntCell.TYPE, StringCell.TYPE};
        DataTableAssert.assertColumnTypes(dataTableSpec, expectedColumnTypes);

        for (DataRow row : dataTable) {
            DataTableAssert.assertDataRow(testExec, row, "input string", 444, "append me at the end!");
        }
    }

    /**
     * Tests that superfluous columns are ignored.
     *
     * @throws Exception
     */
    @Test
    public void testSuperfluousColumnsShouldBeIgnored() throws Exception {
        ExecutionContext testExec = getTestExecutionCtx();

        BufferedDataTable templateRow =
            new TestBufferedDataTableBuilder()
                .withColumnNames("A", "B")
                .withColumnTypes(StringCell.TYPE, IntCell.TYPE)
                .withTableRow(new StringCell("a"), new IntCell(1))
                .build(testExec);

        JsonValue input =
            new JsonValueBuilder()
                .withStringObject("A", "input string")
                .withStringObject("superfluous", "ignore me!")
                .withIntObject("B", 444)
                .build();

        boolean appendSuperfluousColumns = false;
        ContainerRowMapperInputHandling containerRowInputHandling =
                new ContainerRowMapperInputHandling(MissingColumnHandling.FAIL, appendSuperfluousColumns, false);

        BufferedDataTable dataTable =
            ContainerRowMapper.toDataTable(input, templateRow.getDataTableSpec(), containerRowInputHandling, testExec);
        DataTableSpec dataTableSpec = dataTable.getDataTableSpec();

        String[] expectedColumnNames = new String[]{"A", "B"};
        DataTableAssert.assertColumnNames(dataTableSpec, expectedColumnNames);

        DataType[] expectedColumnTypes = new DataType[]{StringCell.TYPE, IntCell.TYPE};
        DataTableAssert.assertColumnTypes(dataTableSpec, expectedColumnTypes);

        for (DataRow row : dataTable) {
            DataTableAssert.assertDataRow(testExec, row, "input string", 444);
        }
    }

    /**
     * Tests that null values in the input are accepted and cast to missing values, when that option is selected.
     *
     * @throws Exception
     */
    @Test
    public void testNullValuesShouldBeAccepted() throws Exception {
        ExecutionContext testExec = getTestExecutionCtx();

        BufferedDataTable templateRow =
                new TestBufferedDataTableBuilder()
                    .withColumnNames("A", "B")
                    .withColumnTypes(StringCell.TYPE, IntCell.TYPE)
                    .withTableRow(new StringCell("a"), new IntCell(1))
                    .build(testExec);

        JsonValue input =
                new JsonValueBuilder()
                    .withNullObject("A")
                    .withIntObject("B", 444)
                    .build();

        boolean acceptMissingValues = true;
        ContainerRowMapperInputHandling containerRowInputHandling =
                new ContainerRowMapperInputHandling(MissingColumnHandling.FAIL, false, acceptMissingValues);

        BufferedDataTable dataTable =
                ContainerRowMapper.toDataTable(
                    input,
                    templateRow.getDataTableSpec(),
                    containerRowInputHandling,
                    testExec
                );

        DataTableSpec dataTableSpec = dataTable.getDataTableSpec();

        String[] expectedColumnNames = new String[]{"A", "B"};
        DataTableAssert.assertColumnNames(dataTableSpec, expectedColumnNames);

        DataType[] expectedColumnTypes = new DataType[]{StringCell.TYPE, IntCell.TYPE};
        DataTableAssert.assertColumnTypes(dataTableSpec, expectedColumnTypes);

        for (DataRow row : dataTable) {
            DataTableAssert.assertDataRow(testExec, row, "missing value", 444);
        }
    }

    /**
     * Tests that null values in the input are not accepted and throws an exception, when that option is selected.
     *
     * @throws Exception
     */
    @Test(expected = InvalidSettingsException.class)
    public void testNullValuesShouldFail() throws Exception {
        ExecutionContext testExec = getTestExecutionCtx();

        BufferedDataTable templateRow =
                new TestBufferedDataTableBuilder()
                    .withColumnNames("A", "B")
                    .withColumnTypes(StringCell.TYPE, IntCell.TYPE)
                    .withTableRow(new StringCell("a"), new IntCell(1))
                    .build(testExec);

        JsonValue input =
                new JsonValueBuilder()
                    .withNullObject("A")
                    .withIntObject("B", 444)
                    .build();

        boolean acceptMissingValues = true;
        ContainerRowMapperInputHandling containerRowInputHandling =
                new ContainerRowMapperInputHandling(MissingColumnHandling.FAIL, false, !acceptMissingValues);

        ContainerRowMapper.toDataTable(
            input,
            templateRow.getDataTableSpec(),
            containerRowInputHandling,
            testExec
        );
    }

    private class JsonValueBuilder {

        private final JsonObjectBuilder m_builder;
        private final JsonBuilderFactory m_factory;
        private final JsonArrayBuilder m_arrayBuilder;

        JsonValueBuilder() {
            m_factory = Json.createBuilderFactory(null);
            m_builder = m_factory.createObjectBuilder();
            m_arrayBuilder = m_factory.createArrayBuilder();
        }

        JsonValueBuilder withStringObject(final String key, final String value) {
            m_builder.add(key, value);
            return this;
        }

        JsonValueBuilder withDoubleObject(final String key, final double value) {
            m_builder.add(key, value);
            return this;
        }

        JsonValueBuilder withIntObject(final String key, final int value) {
            m_builder.add(key, value);
            return this;
        }

        JsonValueBuilder withLongObject(final String key, final long value) {
            m_builder.add(key, value);
            return this;
        }

        JsonValueBuilder withBooleanObject(final String key, final boolean value) {
            m_builder.add(key, value);
            return this;
        }

        JsonValueBuilder withStringArrayObject(final String key, final String... values) {
            for (String value : values) {
                m_arrayBuilder.add(value);
            }
            m_builder.add(key, m_arrayBuilder);
            return this;
        }

        JsonValueBuilder withBigIntegerObject(final String key, final BigInteger value) {
            m_builder.add(key, value);
            return this;
        }

        public JsonValueBuilder withJsonPersonObject() {
            JsonObjectBuilder personBuilder = m_factory.createObjectBuilder();
            JsonObject personObject = personBuilder.add("name", "Flodve").add("age", 32).build();
            m_builder.add("person", personObject);
            return this;
        }

        JsonValueBuilder withNullObject(final String key) {
            m_builder.addNull(key);
            return this;
        }

        JsonValue build() throws IOException {
            return JSONUtil.parseJSONValue(m_builder.build().toString());
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
