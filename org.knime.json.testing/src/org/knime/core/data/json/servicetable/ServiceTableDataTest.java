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
 *   Apr 9, 2018 (Tobias Urhaug): created
 */
package org.knime.core.data.json.servicetable;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Test suite for serializing/deserializing {@link ServiceTableData}.
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 */
public class ServiceTableDataTest {

    /**
     * Checks that a table containing a single row is correctly serialized to json.
     *
     * @throws JsonProcessingException
     */
    @Test
    public void testSerializingASingleRowTable() throws JsonProcessingException {
        ServiceTableRow tableRow = new ServiceTableRow(Arrays.asList("value1", "value2"));
        ServiceTableData tableData = new ServiceTableData(Arrays.asList(tableRow));

        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writeValueAsString(tableData);

        assertEquals("[[\"value1\",\"value2\"]]", json);
    }

    /**
     * Checks that a table containing multiple rows is correctly serialized to json.
     *
     * @throws JsonProcessingException
     */
    @Test
    public void testSerializingMultipleRowsTable() throws JsonProcessingException {
        ServiceTableRow tableRow1 = new ServiceTableRow(Arrays.asList("value1", 1));
        ServiceTableRow tableRow2 = new ServiceTableRow(Arrays.asList(12, 3.5));
        ServiceTableData tableData = new ServiceTableData(Arrays.asList(tableRow1, tableRow2));

        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writeValueAsString(tableData);

        assertEquals("[[\"value1\",1],[12,3.5]]", json);
    }

    /**
     * Checks that a json representing a table with multiple rows with multiple values
     * is correctly deserialized.
     *
     * @throws JsonParseException
     * @throws JsonMappingException
     * @throws IOException
     */
    @Test
    public void testDeserializeJson() throws JsonParseException, JsonMappingException, IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        ServiceTableData table = objectMapper.readValue("[[\"value1\",1],[12,3.5]]", ServiceTableData.class);

        List<ServiceTableRow> tableRows = table.getServiceTableRows();
        assertEquals("value1", tableRows.get(0).getDataCellObjects().get(0));
        assertEquals(1, tableRows.get(0).getDataCellObjects().get(1));
        assertEquals(12, tableRows.get(1).getDataCellObjects().get(0));
        assertEquals(3.5, tableRows.get(1).getDataCellObjects().get(1));
    }

    /**
     * Checks that a json remains the same after having been deserialized and then serialized.
     *
     * @throws JsonParseException
     * @throws JsonMappingException
     * @throws IOException
     */
    @Test
    public void testDeserializingAndThenSerializing() throws JsonParseException, JsonMappingException, IOException {
        String json = "[[\"value1\",1],[12,3.5]]";
        ObjectMapper objectMapper = new ObjectMapper();
        ServiceTableData table = objectMapper.readValue(json, ServiceTableData.class);

        String serializedJson = objectMapper.writeValueAsString(table);

        assertEquals(json, serializedJson);
    }

}
