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
 *   Apr 12, 2019 (Tobias Urhaug, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.json.node.container.mappers.row;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataType;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.json.JSONCell;
import org.knime.core.data.json.JSONCellFactory;
import org.knime.core.data.time.localdate.LocalDateCellFactory;
import org.knime.core.node.BufferedDataTable;
import org.knime.json.node.container.mappers.BufferedDataTableToContainerTableTest.TestBufferedDataTableBuilder;

import jakarta.json.JsonValue;

/**
 * Tests suite for mapping the first row of a {@link DataTable} to a simple {@link JsonValue} representing the row.
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 */
public class DataTableToJsonRowTest extends ContainerRowMapperTest {

    /**
     * Tests that a null input throws an exception.
     *
     * @throws Exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void testNullInputThrowsException() throws Exception {
        ContainerRowMapper.firstRowToJsonValue(null);
    }

    /**
     * Tests that an empty table is mapped to an empty json value.
     *
     * @throws Exception
     */
    @Test
    public void testAnEmptyTableIsMappedToAnEmptyJsonValue() throws Exception {
        BufferedDataTable emptyTable = new TestBufferedDataTableBuilder().build(getTestExecutionCtx());
        JsonValue jsonValueRow = ContainerRowMapper.firstRowToJsonValue(emptyTable);
        JsonValue expectedJson = new JsonValueBuilder().build();
        assertEquals(expectedJson, jsonValueRow);
    }

    /**
     * Tests that a simple table containing a single missing value in a string column is mapped to its json
     * representation.
     *
     * @throws Exception
     */
    @Test
    public void testASingleStringCellMissingValueTable() throws Exception {
        BufferedDataTable simpleTable =
                new TestBufferedDataTableBuilder()
                    .withColumnNames("string-column")
                    .withColumnTypes(StringCell.TYPE)
                    .withTableRow(DataType.getMissingCell())
                    .build(getTestExecutionCtx());

        JsonValue jsonValueRow = ContainerRowMapper.firstRowToJsonValue(simpleTable);

        JsonValue expectedJson =
                new JsonValueBuilder()
                    .withNullObject("string-column")
                    .build();

        assertEquals(expectedJson, jsonValueRow);
    }

    /**
     * Tests that a simple table containing a single string cell is mapped to its json representation.
     *
     * @throws Exception
     */
    @Test
    public void testASingleStringCellTable() throws Exception {
        BufferedDataTable simpleTable =
                new TestBufferedDataTableBuilder()
                    .withColumnNames("string-column")
                    .withColumnTypes(StringCell.TYPE)
                    .withTableRow(new StringCell("value"))
                    .build(getTestExecutionCtx());

        JsonValue jsonValueRow = ContainerRowMapper.firstRowToJsonValue(simpleTable);

        JsonValue expectedJson =
                new JsonValueBuilder()
                    .withStringObject("string-column", "value")
                    .build();

        assertEquals(expectedJson, jsonValueRow);
    }

    /**
     * Tests that a simple table containing a single integer cell is mapped to its json representation.
     *
     * @throws Exception
     */
    @Test
    public void testASingleIntegerCellTable() throws Exception {
        BufferedDataTable simpleTable =
                new TestBufferedDataTableBuilder()
                    .withColumnNames("int-column")
                    .withColumnTypes(IntCell.TYPE)
                    .withTableRow(new IntCell(42))
                    .build(getTestExecutionCtx());

        JsonValue jsonValueRow = ContainerRowMapper.firstRowToJsonValue(simpleTable);

        JsonValue expectedJson =
                new JsonValueBuilder()
                    .withIntObject("int-column", 42)
                    .build();

        assertEquals(expectedJson, jsonValueRow);
    }

    /**
     * Tests that a simple table containing a single double cell is mapped to its json representation.
     *
     * @throws Exception
     */
    @Test
    public void testASingleDoubleCellTable() throws Exception {
        BufferedDataTable simpleTable =
                new TestBufferedDataTableBuilder()
                    .withColumnNames("double-column")
                    .withColumnTypes(DoubleCell.TYPE)
                    .withTableRow(new DoubleCell(32.1))
                    .build(getTestExecutionCtx());

        JsonValue jsonValueRow = ContainerRowMapper.firstRowToJsonValue(simpleTable);

        JsonValue expectedJson =
                new JsonValueBuilder()
                    .withDoubleObject("double-column", 32.1)
                    .build();

        assertEquals(expectedJson, jsonValueRow);
    }

    /**
     * Tests that a simple table containing a single long cell is mapped to its json representation.
     *
     * @throws Exception
     */
    @Test
    public void testASingleLongCellTable() throws Exception {
        BufferedDataTable simpleTable =
                new TestBufferedDataTableBuilder()
                    .withColumnNames("long-column")
                    .withColumnTypes(LongCell.TYPE)
                    .withTableRow(new LongCell(Long.MAX_VALUE))
                    .build(getTestExecutionCtx());

        JsonValue jsonValueRow = ContainerRowMapper.firstRowToJsonValue(simpleTable);

        JsonValue expectedJson =
                new JsonValueBuilder()
                    .withLongObject("long-column", Long.MAX_VALUE)
                    .build();

        assertEquals(expectedJson, jsonValueRow);
    }

    /**
     * Tests that a simple table containing a single boolean cell is mapped to its json representation.
     *
     * @throws Exception
     */
    @Test
    public void testASingleBooleanCellTable() throws Exception {
        BufferedDataTable simpleTable =
                new TestBufferedDataTableBuilder()
                    .withColumnNames("boolean-column")
                    .withColumnTypes(BooleanCell.TYPE)
                    .withTableRow(BooleanCell.FALSE)
                    .build(getTestExecutionCtx());

        JsonValue jsonValueRow = ContainerRowMapper.firstRowToJsonValue(simpleTable);

        JsonValue expectedJson =
                new JsonValueBuilder()
                    .withBooleanObject("boolean-column", false)
                    .build();

        assertEquals(expectedJson, jsonValueRow);
    }

    /**
     * Tests that only the first row of a table containing multiple rows is mapped to its json representation.
     *
     * @throws Exception
     */
    @Test
    public void testOnlyTheFirstRowOfATableIsMapped() throws Exception {
        BufferedDataTable simpleTable =
                new TestBufferedDataTableBuilder()
                    .withColumnNames("boolean-column", "integer-column")
                    .withColumnTypes(BooleanCell.TYPE, IntCell.TYPE)
                    .withTableRow(BooleanCell.TRUE, new IntCell(123))
                    .withTableRow(BooleanCell.FALSE, new IntCell(99999))
                    .withTableRow(BooleanCell.FALSE, new IntCell(0))
                    .build(getTestExecutionCtx());

        JsonValue jsonValueRow = ContainerRowMapper.firstRowToJsonValue(simpleTable);

        JsonValue expectedJson =
                new JsonValueBuilder()
                    .withBooleanObject("boolean-column", true)
                    .withIntObject("integer-column", 123)
                    .build();

        assertEquals(expectedJson, jsonValueRow);
    }

    /**
     * Tests that a table without rows is mapped to an empty json value.
     *
     * @throws Exception
     */
    @Test
    public void testTableWithoutRowsReturnsEmptyJson() throws Exception {
        BufferedDataTable simpleTable =
                new TestBufferedDataTableBuilder()
                    .withColumnNames("boolean-column", "integer-column")
                    .withColumnTypes(BooleanCell.TYPE, IntCell.TYPE)
                    .build(getTestExecutionCtx());

        JsonValue jsonValueRow = ContainerRowMapper.firstRowToJsonValue(simpleTable);
        JsonValue expectedJson = new JsonValueBuilder().build();

        assertEquals(expectedJson, jsonValueRow);
    }

    /**
     * Tests that some non primitive types are mapped to their string representation.
     *
     * @throws Exception
     */
    @Test
    public void testNonPrimitiveTypes() throws Exception {
        JsonValue jsonCell = new JsonValueBuilder().withStringObject("hello", "test!").build();
        DataCell jsonDataCell = JSONCellFactory.create(jsonCell);

        BufferedDataTable simpleTable =
                new TestBufferedDataTableBuilder()
                    .withColumnNames("local-date", "json")
                    .withColumnTypes(LocalDateCellFactory.TYPE, JSONCell.TYPE)
                    .withTableRow(LocalDateCellFactory.create("2019-03-03"), jsonDataCell)
                    .build(getTestExecutionCtx());

        JsonValue jsonValueRow = ContainerRowMapper.firstRowToJsonValue(simpleTable);
        JsonValue expectedJson =
                new JsonValueBuilder()
                    .withStringObject("local-date", "2019-03-03")
                    .withJsonValueObject("json", jsonCell)
                    .build();

        assertEquals(expectedJson, jsonValueRow);
    }

    /**
     * Tests that json is mapped to its primitive value and not a string.
     *
     * @throws Exception
     */
    @Test
    public void testJsonIsMappedToItsValue() throws Exception {
        JsonValue jsonCell =
                new JsonValueBuilder()
                    .withStringObject("hello", "test!")
                    .withJsonPersonObject()
                    .build();
        DataCell jsonDataCell = JSONCellFactory.create(jsonCell);

        BufferedDataTable simpleTable =
                new TestBufferedDataTableBuilder()
                    .withColumnNames("json-column")
                    .withColumnTypes(JSONCell.TYPE)
                    .withTableRow(jsonDataCell)
                    .build(getTestExecutionCtx());

        JsonValue jsonValueRow = ContainerRowMapper.firstRowToJsonValue(simpleTable);
        JsonValue expectedJson = new JsonValueBuilder().withJsonValueObject("json-column", jsonCell).build();

        assertEquals(expectedJson, jsonValueRow);
    }

}
