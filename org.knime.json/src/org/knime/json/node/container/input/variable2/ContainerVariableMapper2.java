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
 *   21.05.2021 (jl): created
 */
package org.knime.json.node.container.input.variable2;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.VariableType;
import org.knime.json.node.container.input.variable2.ContainerVariableJsonSchema2.SimpleSchema;
import org.knime.json.util.JSONUtil;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

import jakarta.json.JsonValue;

/**
 * Class that converts flow variables to a JsonValue conforming to {@link ContainerVariableJsonSchema2}.
 *
 * @author Jannik Löscher, KNIME GmbH, Konstanz, Germany
 * @since 4.4
 * @noreference This class is not intended to be referenced by clients (made public s.t. it can be referenced by other
 *              internal code such as node implementations).
 */
public final class ContainerVariableMapper2 {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private ContainerVariableMapper2() {
    }

    /**
     * Converts a collection of flow variables to a JsonValue conforming to {@link ContainerVariableJsonSchema2}.
     *
     * @param flowVariables the flow variables
     * @return a JsonValue representing the flow variables
     * @param trySimplifiedJson output the json in simplified form if only one variable is defined
     * @throws InvalidSettingsException if variables cannot be mapped to {@link ContainerVariableJsonSchema2}
     * @since 5.1
     */
    public static JsonValue toContainerVariableJsonValue(final Map<String, FlowVariable> flowVariables,
        final boolean trySimplifiedJson) throws InvalidSettingsException {
        final var containerVariableInput = new ContainerVariableJsonSchema2();
        for (final var entry : flowVariables.entrySet()) {
            final var flowVariableName = entry.getKey();
            final var flowVariable = entry.getValue();
            if (!flowVariable.isGlobalConstant()) {
                containerVariableInput.addVariable(flowVariableName, parseFlowVariable(flowVariable));
            }
        }

        try {
            String containerVariableJson;
            if (trySimplifiedJson && containerVariableInput.getVariables().size() == 1) {
                // encode only the *value* of the single present flow variable, omit the name
                containerVariableJson = new ObjectMapper()
                    .writeValueAsString(containerVariableInput.getVariables().values().iterator().next());
            } else {
                // encode according to schema given by `ContainerVariableJsonSchema2`.
                containerVariableJson = new ObjectMapper().writeValueAsString(containerVariableInput);
            }
            return JSONUtil.parseJSONValue(containerVariableJson);
        } catch (IOException e) {
            throw new InvalidSettingsException("Could not parse the variables to JsonValue", e);
        }
    }

    private static Object parseFlowVariable(final FlowVariable variable) throws InvalidSettingsException {
        if (variable.getVariableType().equals(VariableType.StringType.INSTANCE)) {
            return variable.getStringValue();
        } else if (variable.getVariableType().equals(VariableType.IntType.INSTANCE)) {
            return variable.getValue(VariableType.IntType.INSTANCE);
        } else if (variable.getVariableType().equals(VariableType.DoubleType.INSTANCE)) {
            return variable.getValue(VariableType.DoubleType.INSTANCE);
        } else if (variable.getVariableType().equals(VariableType.BooleanType.INSTANCE)) {
            return variable.getValue(VariableType.BooleanType.INSTANCE);
        } else {
            throw new InvalidSettingsException("Type not supported! " + variable.getVariableType().getIdentifier());
        }
    }

    /**
     * Maps a json string to {@link ContainerVariableJsonSchema2}, if conforms to the schema.
     *
     * @param json json string that should be mapped to {@link ContainerVariableJsonSchema2}
     * @param simplifiedSchemaName the name of the variable to use if the simplified json schema is expected
     * @return {@link ContainerVariableJsonSchema2} of the given input string
     * @throws InvalidSettingsException if the input string does not conform to {@link ContainerVariableJsonSchema2}
     */
    public static ContainerVariableJsonSchema2 toContainerVariableJsonSchema(final String json,
        final String simplifiedSchemaName) throws InvalidSettingsException {
        // case of simplified schema
        if (simplifiedSchemaName != null) {
            try {
                return Optional
                    .ofNullable(OBJECT_MAPPER.readValue(json, ContainerVariableJsonSchema2.SimpleSchema.class))
                    .orElseThrow().build(simplifiedSchemaName);
            } catch (MismatchedInputException | NoSuchElementException e) {
                throw new InvalidSettingsException(
                    "Error while parsing simplified JSON: expected a value of one of the following types "
                        + Arrays.toString(ContainerVariableInputNodeModel2.SUPPORTED_VARIABLE_TYPES)
                        + " and not a JSON object, array or null.",
                    e);
            } catch (IOException e) {
                throw checkNestedInvalidSettingsException(e, "simplified JSON");
            }
        } else {  // case of full schema
            try {
                return Optional.ofNullable(OBJECT_MAPPER.readValue(json, ContainerVariableJsonSchema2.class))
                    .orElseThrow();
            } catch (MismatchedInputException | NoSuchElementException e) {
                throw new InvalidSettingsException(
                    "Error while parsing JSON: expected a JSON object with the variable names as "
                        + "properties (keys) and their values being one of the following types "
                        + Arrays.toString(ContainerVariableInputNodeModel2.SUPPORTED_VARIABLE_TYPES)
                        + " (and not JSON objects, arrays or null).",
                    e);
            } catch (IOException e) {
                throw checkNestedInvalidSettingsException(e, "JSON");
            }
        }
    }

    /**
     * @param json
     * @return <code>true</code> if the json string can be read in as {@link SimpleSchema}, otherwise
     *         <code>false</code>
     */
    public static boolean hasSimpleSchema(final String json) {
        try {
            OBJECT_MAPPER.readValue(json, ContainerVariableJsonSchema2.SimpleSchema.class);
            return true;
        } catch (IOException e) { // NOSONAR
            return false;
        }
    }

    private static InvalidSettingsException checkNestedInvalidSettingsException(final IOException e,
        final String type) {
        if (e.getCause() instanceof InvalidSettingsException) {
            return new InvalidSettingsException("Error while parsing " + type + ": " + e.getCause().getMessage(), e);
        } else {
            return new InvalidSettingsException("Error while parsing " + type + ": " + e.getMessage(), e);
        }
    }

}
