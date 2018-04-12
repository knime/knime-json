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
package org.knime.json.node.servicein;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 *
 * @author Tobias Urhaug
 */
public class ServiceInputTest {



    /**
     * @throws JsonProcessingException
     *
     */
    @Test
    public void testSerialization() throws JsonProcessingException {
        ServiceInput input =
            new ServiceInputBuilder()
                .withColumnSpec("column-string", "string")
                .withTableRow("value1")
                .build();

        String json = new ObjectMapper().writeValueAsString(input);

        assertEquals("{\"table-spec\":[{\"column-string\":\"string\"}],\"table-data\":[[\"value1\"]]}", json);
    }

    /**
     * Checks that a null value is
     *
     * @throws JsonProcessingException
     */
    @Test
    public void testSerializingNullValues() throws JsonProcessingException {
        ServiceInput input =
            new ServiceInputBuilder()
                .withColumnSpec("column-string", "string")
                .withTableRow((Object)null)
                .build();

        String json = new ObjectMapper().writeValueAsString(input);

        assertEquals("{\"table-spec\":[{\"column-string\":\"string\"}],\"table-data\":[[null]]}", json);
    }

    /**
     * @throws IOException
     *
     */
    @Test
    public void testDeserializingNullValues() throws IOException {
        String json = "{\"table-spec\":[{\"column-string\":\"string\"}],\"table-data\":[[null]]}";

        ServiceInput serviceInput =  new ObjectMapper().readValue(json, ServiceInput.class);
        ServiceInputTableData serviceInputTableData = serviceInput.getServiceInputTableData();
        Object deserializedValue = serviceInputTableData.getServiceInputTableRows().get(0).getDataCellObjects().get(0);

        assertNull(deserializedValue);
    }

    /**
     * Checks that the table spec of a json input is correctly deserialized.
     *
     * @throws JsonParseException
     * @throws JsonMappingException
     * @throws IOException
     */
    @Test
    public void testDeserializeDefaultJsonStructureTableSpec() throws JsonParseException, JsonMappingException, IOException {
        String defaultJsonStructure = ServiceInputDefaultJsonStructure.asString();

        ServiceInput serviceInput =  new ObjectMapper().readValue(defaultJsonStructure, ServiceInput.class);

        ServiceInputTableSpec tableSpec = serviceInput.getServiceInputTableSpec();
        assertTrue(tableSpec.contains("column-string", "string"));
        assertTrue(tableSpec.contains("column-int", "int"));
        assertTrue(tableSpec.contains("column-double", "double"));
    }

    /**
     * @throws IOException
     * @throws JsonMappingException
     * @throws JsonParseException
     *
     */
    @Test
    public void testDeserializeDefaultJsonStructureTableData() throws JsonParseException, JsonMappingException, IOException {
        String defaultJsonStructure = ServiceInputDefaultJsonStructure.asString();

        ObjectMapper objectMapper = new ObjectMapper();
        ServiceInput serviceInput = objectMapper.readValue(defaultJsonStructure, ServiceInput.class);

        ServiceInputTableRow firstExpectedRow = new ServiceInputTableRow(Arrays.asList("value1", 1, 1.5, "2018-03-27"));
        ServiceInputTableRow secondExpectedRow = new ServiceInputTableRow(Arrays.asList("value2", 2, 2.5, "2018-03-28"));

        ServiceInputTableData tableData = serviceInput.getServiceInputTableData();
        assertEquals(firstExpectedRow, tableData.getServiceInputTableRows().get(0));
        assertEquals(secondExpectedRow, tableData.getServiceInputTableRows().get(1));
    }

}
