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
 *   May 2, 2018 (Tobias Urhaug, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.json.node.servicevariableinput;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonBuilderFactory;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Test suite for the serialization/deserialization of a {@link ServiceVariableInput} via Jackson.
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 */
public class ServiceVariableInputTest {

    /**
     * Checks that a ServiceVariableInput with a map of variables is correctly serialized to JSON.
     *
     * @throws Exception
     */
    @Test
    public void testSerialize() throws Exception {
        ServiceVariableInput serviceVariableInput = new ServiceVariableInput(createVariablesAsList());

        ObjectMapper objectMapper = new ObjectMapper();
        String actualJson = objectMapper.writeValueAsString(serviceVariableInput);

        String expectedJson = createVariablesAsJson();
        assertEquals(expectedJson, actualJson);
    }

    /**
     * Checks that a JSON representing the variables is correctly deserialized to ServiceVariableInput.
     *
     * @throws Exception
     */
    @Test
    public void testDeserialize() throws  Exception {
        String inputJson = createVariablesAsJson();

        ObjectMapper objectMapper = new ObjectMapper();
        ServiceVariableInput deserializedInput = objectMapper.readValue(inputJson, ServiceVariableInput.class);

        List<Map<String, Object>> deserializedVariables = deserializedInput.getVariables();
        assertTrue(deserializedVariables.get(0).get("variable-string").equals("somevariable"));
        assertTrue(deserializedVariables.get(1).get("variable-double").equals(42.0));
        assertTrue(deserializedVariables.get(2).get("variable-int").equals(100));
    }

    private List<Map<String, Object>> createVariablesAsList() {
        Map<String, Object> variable1 = Collections.singletonMap("variable-string", "somevariable");
        Map<String, Object> variable2 = Collections.singletonMap("variable-double", 42.0);
        Map<String, Object> variable3 = Collections.singletonMap("variable-int", 100);

        return Arrays.asList(variable1, variable2, variable3);
    }

    private String createVariablesAsJson() {
        JsonBuilderFactory factory = Json.createBuilderFactory(null);
        return factory.createObjectBuilder().add("variables", createVariables(factory)).build().toString();
    }

    private JsonArray createVariables(final JsonBuilderFactory factory) {
        return factory.createArrayBuilder() //
                .add(factory.createObjectBuilder().add("variable-string", "somevariable")) //
                .add(factory.createObjectBuilder().add("variable-double", 42.0)) //
                .add(factory.createObjectBuilder().add("variable-int", 100)) //
                .build(); //
    }

}
