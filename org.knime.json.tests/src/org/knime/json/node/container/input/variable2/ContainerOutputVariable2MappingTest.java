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
 *   31.05.2021 (jl): created
 */
package org.knime.json.node.container.input.variable2;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.knime.base.node.io.variablecreator.DialogComponentVariables;
import org.knime.base.node.io.variablecreator.SettingsModelVariables;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.dialog.ExternalNodeData;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.VariableType;
import org.knime.json.node.container.input.file.ContainerNodeSharedConfiguration;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * Tests for the variable mapping of “Container Input (Variable)” 2 and pushing of the variables.
 *
 * For more extensive tests see the <a href=
 * "https://bitbucket.org/KNIME/server-integration-tests/src/master/com.knime.enterprise.server.integration.tests/src/com/knime/enterprise/server/ittests/v4/nodes/containerinput/ContainerVariableInput2Test.java">
 * server integration tests</a>.
 *
 * @author Jannik Löscher, KNIME GmbH, Konstanz, Germany
 * @since 4.4
 */
public class ContainerOutputVariable2MappingTest {

    private static final JsonBuilderFactory FACTORY = Json.createBuilderFactory(null);

    private static final ContainerVariableInputNodeModel2 MODEL_COMPLEX =
        new ContainerVariableInputNodeFactory2().createNodeModel();

    private static final Map<String, FlowVariable> VARIABLES;

    private static final Map<String, Object> VALUES;

    private static final String KEY_STRING = "string";

    private static final String KEY_INT = "int";

    private static final String KEY_DOUBLE = "double";

    private static final String KEY_BOOLEAN = "bool";

    private static final String KEY_ANY = "any";

    private static final String VAL_STRING = "value";

    private static final Integer VAL_INT = 1234;

    private static final Double VAL_DOUBLE = 123.25;

    private static final Boolean VAL_BOOLEAN = false;

    static {
        VARIABLES = Map.of(KEY_STRING, new FlowVariable(KEY_STRING, VAL_STRING), KEY_INT,
            new FlowVariable(KEY_INT, VAL_INT), KEY_DOUBLE, new FlowVariable(KEY_DOUBLE, VAL_DOUBLE), KEY_BOOLEAN,
            new FlowVariable(KEY_BOOLEAN, VariableType.BooleanType.INSTANCE, VAL_BOOLEAN));
        VALUES = Map.of(KEY_STRING, VAL_STRING, KEY_INT, VAL_INT, KEY_DOUBLE, VAL_DOUBLE, KEY_BOOLEAN, VAL_BOOLEAN);

    }

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    /**
     * Test serialization of flow variables to JSON
     *
     * @throws InvalidSettingsException if the values could not be mapped to a {@link ContainerVariableJsonSchema2}.
     * @throws JsonProcessingException if the values could not transformed in to a Json string
     */
    @Test
    public void testSerialization() throws InvalidSettingsException, JsonProcessingException {

        // test complex format
        final var expectedJsonAll = FACTORY.createObjectBuilder().add(KEY_STRING, VAL_STRING).add(KEY_INT, VAL_INT)
            .add(KEY_DOUBLE, VAL_DOUBLE).add(KEY_BOOLEAN, VAL_BOOLEAN).build();
        final var actualJsonAll = ContainerVariableMapper2.toContainerVariableJsonValue(VARIABLES, true);

        assertThat("Expected values to be correctly parsed into JSON", actualJsonAll, equalTo(expectedJsonAll));

        // test simple format
        for (final var entry : VARIABLES.entrySet()) {
            final var flowVar = entry.getValue();
            final var expectedJson = toJsonValue(flowVar.getValue(flowVar.getVariableType()));
            final var actualJson = ContainerVariableMapper2
                .toContainerVariableJsonValue(Collections.singletonMap(entry.getKey(), flowVar), true);
            assertThat("Expected simple format", actualJson, equalTo(expectedJson));
        }

        // test simple format in complex form
        for (final var entry : VARIABLES.entrySet()) {
            final var flowVar = entry.getValue();
            final var expectedJson = toJsonObject(entry);
            final var actualJson = ContainerVariableMapper2
                .toContainerVariableJsonValue(Collections.singletonMap(entry.getKey(), flowVar), false);
            assertThat("Expected no simple format", actualJson, equalTo(expectedJson));
        }
    }

    /**
     * Tests whether longs cannot be mapped.
     *
     * @throws InvalidSettingsException if the values could not be mapped to a {@link ContainerVariableJsonSchema2}
     *             (expected)
     */
    @Test(expected = InvalidSettingsException.class)
    public void testSerializationErrorLong() throws InvalidSettingsException {
        ContainerVariableMapper2.toContainerVariableJsonValue(Collections.singletonMap(KEY_ANY,
            new FlowVariable(KEY_ANY, VariableType.LongType.INSTANCE, 123456789012345L)), true);
    }

    /**
     * Tests whether arrays cannot be mapped.
     */
    @Test
    public void testSerializationErrorArray() {
        try {
            ContainerVariableMapper2.toContainerVariableJsonValue(Collections.singletonMap(KEY_ANY,
                new FlowVariable(KEY_ANY, VariableType.StringArrayType.INSTANCE, new String[]{VAL_STRING})), true);
            fail("Did not expect to be able to create string array");
        } catch (InvalidSettingsException e) { // NOSONAR: is ignored because expected
        }

        try {
            ContainerVariableMapper2.toContainerVariableJsonValue(Collections.singletonMap(KEY_ANY,
                new FlowVariable(KEY_ANY, VariableType.IntArrayType.INSTANCE, new Integer[]{VAL_INT})), true);
            fail("Did not expect to be able to create int array");
        } catch (InvalidSettingsException e) { // NOSONAR: is ignored because expected
        }

        try {
            ContainerVariableMapper2.toContainerVariableJsonValue(Collections.singletonMap(KEY_ANY,
                new FlowVariable(KEY_ANY, VariableType.DoubleArrayType.INSTANCE, new Double[]{VAL_DOUBLE})), true);
            fail("Did not expect to be able to create double array");
        } catch (InvalidSettingsException e) { // NOSONAR: is ignored because expected
        }

        try {
            ContainerVariableMapper2.toContainerVariableJsonValue(
                Collections.singletonMap(KEY_ANY,
                    new FlowVariable(KEY_ANY, VariableType.BooleanArrayType.INSTANCE, new Boolean[]{VAL_BOOLEAN})),
                true);
            fail("Did not expect to be able to create boolean array");
        } catch (InvalidSettingsException e) { // NOSONAR: is ignored because expected
        }
    }

    /**
     * Test correct deserialization of JSON to {@link ContainerVariableJsonSchema2}
     *
     * @throws InvalidSettingsException if the values could not be mapped to a {@link ContainerVariableJsonSchema2}.
     * @throws JsonProcessingException if the values could not transformed in to a Json string
     */
    @Test
    public void testDeserilization() throws InvalidSettingsException, JsonProcessingException {

        // test complex format
        final var stringJsonAll = FACTORY.createObjectBuilder().add(KEY_STRING, VAL_STRING).add(KEY_INT, VAL_INT)
            .add(KEY_DOUBLE, VAL_DOUBLE).add(KEY_BOOLEAN, VAL_BOOLEAN).build().toString();
        final var variablesAll =
            ContainerVariableMapper2.toContainerVariableJsonSchema(stringJsonAll, null).getVariables();
        assertThat("Expected JSON string to be correctly parsed into schema", variablesAll, equalTo(VALUES));
        assertMapsEqualContent(variablesAll, VALUES);

        // test simple format
        for (final var entry : VALUES.entrySet()) {
            final var expectedMap = Collections.singletonMap(KEY_ANY, entry.getValue());
            final var stringJson = toJsonValue(entry.getValue()).toString();
            final var variables =
                ContainerVariableMapper2.toContainerVariableJsonSchema(stringJson, KEY_ANY).getVariables();
            assertMapsEqualContent(variables, expectedMap);
        }

        // test simple format in complex form
        for (final var entry : VARIABLES.entrySet()) {
            final var expectedMap = Collections.singletonMap(entry.getKey(), VALUES.get(entry.getKey()));
            final var stringJson = toJsonObject(entry).toString();
            final var variables =
                ContainerVariableMapper2.toContainerVariableJsonSchema(stringJson, null).getVariables();
            assertMapsEqualContent(variables, expectedMap);
        }
    }

    /**
     * Tests whether longs cannot be deserialized.
     *
     * @throws Exception if the mock {@link ContainerVariableInputNodeModel2} couldn't be created.
     */
    @Test
    public void testDesirializationErrorLong() throws Exception {
        try {
            sendAndProcessJson(FACTORY.createObjectBuilder().add(KEY_ANY, 1233456789987654321L).build());
            fail("Did not expect to be able to use a long value in JSON format");
        } catch (InvalidSettingsException e) { // NOSONAR: is ignored because expected
            System.err.println(e.getMessage());
        }
        try {
            sendAndProcessSimpleJson(toJsonValue(1233456789987654321L));
            fail("Did not expect to be able to use a long value in simple JSON format");
        } catch (InvalidSettingsException e) { // NOSONAR: is ignored because expected
            System.err.println(e.getMessage());
        }
    }

    /**
     * Tests whether objects cannot be deserialized.
     *
     * @throws Exception if the mock {@link ContainerVariableInputNodeModel2} couldn't be created.
     */
    @Test
    public void testDesirializationErrorObject() throws Exception {
        try {
            sendAndProcessJson(
                FACTORY.createObjectBuilder().add(KEY_ANY, FACTORY.createObjectBuilder().build()).build());
            fail("Did not expect to be able to use an object value in JSON format");
        } catch (InvalidSettingsException e) { // NOSONAR: is ignored because expected
            System.err.println(e.getMessage());
        }
        try {
            sendAndProcessSimpleJson(FACTORY.createObjectBuilder().build());
            fail("Did not expect to be able to use an object value in simple JSON format");
        } catch (InvalidSettingsException e) { // NOSONAR: is ignored because expected
            System.err.println(e.getMessage());
        }
    }

    /**
     * Tests whether arrays cannot be deserialized.
     *
     * @throws Exception if the mock {@link ContainerVariableInputNodeModel2} couldn't be created.
     */
    @Test
    public void testDesirializationErrorArray() throws Exception {
        try {
            sendAndProcessJson(
                FACTORY.createObjectBuilder().add(KEY_ANY, FACTORY.createArrayBuilder().build()).build());
            fail("Did not expect to be able to use an object value in JSON format");
        } catch (InvalidSettingsException e) { // NOSONAR: is ignored because expected
            System.err.println(e.getMessage());
        }
        try {
            sendAndProcessSimpleJson(FACTORY.createArrayBuilder().build());
            fail("Did not expect to be able to use an object value in simple JSON format");
        } catch (InvalidSettingsException e) { // NOSONAR: is ignored because expected
            System.err.println(e.getMessage());
        }
    }

    /**
     * Tests whether wrong types cannot be deserialized if the simple format is enabled.
     *
     * @throws Exception if the mock {@link ContainerVariableInputNodeModel2} couldn't be created.
     */
    @Test
    public void testDesirializationErrorWrongTypeSimple() throws Exception {
        try {
            sendAndProcessSimpleJson(toJsonValue(VAL_STRING), new FlowVariable(KEY_ANY, VAL_INT));
            fail(
                "Did not expect to be able to use a string value in simple JSON format if specification demands string");
        } catch (InvalidSettingsException e) { // NOSONAR: is ignored because expected
            System.err.println(e.getMessage());
        }
        try {
            sendAndProcessSimpleJson(toJsonValue(VAL_INT));
            fail("Did not expect to be able to use an int value in simple JSON format if specification demands string");
        } catch (InvalidSettingsException e) { // NOSONAR: is ignored because expected
            System.err.println(e.getMessage());
        }
        try {
            sendAndProcessSimpleJson(toJsonValue(VAL_DOUBLE));
            fail(
                "Did not expect to be able to use a double value in simple JSON format if specification demands string");
        } catch (InvalidSettingsException e) { // NOSONAR: is ignored because expected
            System.err.println(e.getMessage());
        }
        try {
            sendAndProcessSimpleJson(toJsonValue(VAL_BOOLEAN));
            fail(
                "Did not expect to be able to use a boolean value in simple JSON format if specification demands string");
        } catch (InvalidSettingsException e) { // NOSONAR: is ignored because expected
            System.err.println(e.getMessage());
        }
    }

    private final static JsonObject toJsonObject(final Map.Entry<String, FlowVariable> entry) {
        final var type = entry.getValue().getVariableType();
        final var builder = FACTORY.createObjectBuilder();
        if (type == VariableType.StringType.INSTANCE) {
            builder.add(entry.getKey(), entry.getValue().getStringValue());
        } else if (type == VariableType.IntType.INSTANCE) {
            builder.add(entry.getKey(), entry.getValue().getIntValue());
        } else if (type == VariableType.LongType.INSTANCE) {
            builder.add(entry.getKey(), entry.getValue().getValue(VariableType.LongType.INSTANCE));
        } else if (type == VariableType.DoubleType.INSTANCE) {
            builder.add(entry.getKey(), entry.getValue().getDoubleValue());
        } else if (type == VariableType.BooleanType.INSTANCE) {
            builder.add(entry.getKey(), entry.getValue().getValue(VariableType.BooleanType.INSTANCE));
        } else {
            fail("Could not convert type: Unknown type.");
        }

        return builder.build();
    }

    private final static JsonValue toJsonValue(final Object value) {
        final var clazz = value.getClass();
        if (String.class.equals(clazz)) {
            return Json.createValue((String)value);
        } else if (Integer.class.equals(clazz)) {
            return Json.createValue((Integer)value);
        } else if (Long.class.equals(clazz)) {
            return Json.createValue((Long)value);
        } else if (Double.class.equals(clazz)) {
            return Json.createValue((Double)value);
        } else if (Boolean.class.equals(clazz)) {
            return ((Boolean)value).booleanValue() ? JsonValue.TRUE : JsonValue.FALSE;
        } else {
            fail("Could not convert type: Unknown type.");
            return null;
        }
    }

    private static <K, V> void assertMapsEqualContent(final Map<K, V> actualMap, final Map<K, V> expectedMap) {
        assertThat("Expected maps to have same size", actualMap.size(), equalTo(expectedMap.size()));
        for (final var entry : actualMap.entrySet()) {
            assertThat("Expected entry to be equal (" + entry.getKey() + ")", entry.getValue(),
                equalTo(expectedMap.get(entry.getKey())));
        }
    }

    private static void sendAndProcessJson(final JsonValue json) throws Exception {
        MODEL_COMPLEX.setInputData(ExternalNodeData.builder(KEY_ANY).jsonValue(json).build());
        MODEL_COMPLEX.reset();
        MODEL_COMPLEX.configure(null);
        MODEL_COMPLEX.execute(null, null);
    }

    private static void sendAndProcessSimpleJson(final JsonValue json) throws Exception {
        sendAndProcessSimpleJson(json, new FlowVariable(KEY_ANY, VAL_STRING));
    }

    private static void sendAndProcessSimpleJson(final JsonValue json, final FlowVariable flow) throws Exception {
        final var model = new ContainerVariableInputNodeFactory2().createNodeModel();
        final var config = new ContainerVariableInputNodeConfiguration2();
        final var sharedConfig = new ContainerNodeSharedConfiguration(KEY_ANY);
        config.setRequireMatchSpecification(true);
        config.setUseSimpleJsonSpec(true);
        final var variables = new SettingsModelVariables(KEY_ANY,
            ContainerVariableInputNodeModel2.SUPPORTED_VARIABLE_TYPES, Collections.emptyMap());
        final var dialog = new DialogComponentVariables(variables);
        dialog.setVariables(Collections.singletonMap(flow.getName(), flow));

        final var settings = new NodeSettings(KEY_ANY);
        dialog.saveSettingsTo(settings);
        variables.saveSettingsTo(settings);
        config.save(settings);
        sharedConfig.saveSettingsTo(settings);

        try {
            model.validateSettings(settings);
            model.loadValidatedSettingsFrom(settings);
        } catch (InvalidSettingsException e) {
            throw new IllegalStateException(e);
        }

        model.setInputData(ExternalNodeData.builder(KEY_ANY).jsonValue(json).build());
        model.reset();
        model.configure(null);
        model.execute(null, null);
    }
}
