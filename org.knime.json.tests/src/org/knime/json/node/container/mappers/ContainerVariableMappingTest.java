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
package org.knime.json.node.container.mappers;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.knime.core.data.json.container.variables.ContainerVariableJsonSchema;
import org.knime.json.node.container.input.variable.ContainerVariableDefaultJsonStructure;
import org.knime.json.node.container.input.variable2.ContainerInputVariable2MappingTest;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Test suite for the serialization/deserialization of a {@link ContainerVariableJsonSchema} via Jackson.
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 * @deprecated superseded by {@link ContainerInputVariable2MappingTest}
 */
@Deprecated(since = "4.4")
public class ContainerVariableMappingTest {

    /**
     * Checks that a ContainerVariableJsonSchema with a map of variables is correctly serialized to JSON.
     *
     * @throws Exception
     */
    @Test
    public void testSerialize() throws Exception {
        List<Map<String, Object>> variableList = ContainerVariableDefaultJsonStructure.asVariableList();
        ContainerVariableJsonSchema containerVariableJsonSchema = new ContainerVariableJsonSchema(variableList);

        ObjectMapper objectMapper = new ObjectMapper();
        String actualJson = objectMapper.writeValueAsString(containerVariableJsonSchema);

        String expectedJson = ContainerVariableDefaultJsonStructure.asString();
        assertEquals(expectedJson, actualJson);
    }

    /**
     * Checks that a JSON representing the variables is correctly deserialized to ContainerVariableJsonSchema.
     *
     * @throws Exception
     */
    @Test
    public void testDeserialize() throws Exception {
        String inputJson = ContainerVariableDefaultJsonStructure.asString();

        ObjectMapper objectMapper = new ObjectMapper();
        ContainerVariableJsonSchema deserializedInput =
            objectMapper.readValue(inputJson, ContainerVariableJsonSchema.class);

        List<Map<String, Object>> deserializedVariables = deserializedInput.getVariables();

        Map<String, Object> stringVariable = deserializedVariables.get(0);
        assertThat(stringVariable.entrySet(), hasSize(1));
        assertThat(stringVariable, hasEntry(//
            ContainerVariableDefaultJsonStructure.STRING_VARIABLE_NAME, //
            ContainerVariableDefaultJsonStructure.STRING_VARIABLE_VALUE)); //

        Map<String, Object> doubleVariable = deserializedVariables.get(1);
        assertThat(doubleVariable.entrySet(), hasSize(1));
        assertThat(doubleVariable, hasEntry(//
            ContainerVariableDefaultJsonStructure.DOUBLE_VARIABLE_NAME, //
            ContainerVariableDefaultJsonStructure.DOUBLE_VARIABLE_VALUE)); //

        Map<String, Object> intVariable = deserializedVariables.get(2);
        assertThat(intVariable.entrySet(), hasSize(1));
        assertThat(intVariable, hasEntry(//
            ContainerVariableDefaultJsonStructure.INT_VARIABLE_NAME, //
            ContainerVariableDefaultJsonStructure.INT_VARIABLE_VALUE)); //
    }

}
