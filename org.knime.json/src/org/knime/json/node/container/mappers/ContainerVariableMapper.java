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
 *   Jun 13, 2018 (Tobias Urhaug, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.json.node.container.mappers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.knime.core.data.json.container.variables.ContainerVariableJsonSchema;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.json.node.container.input.variable2.ContainerVariableInputNodeFactory2;
import org.knime.json.util.JSONUtil;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.json.JsonValue;

/**
 * Class that converts flow variables to a JsonValue conforming to {@link ContainerVariableJsonSchema}.
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 * @since 3.6
 * @deprecated superseded by {@link ContainerVariableInputNodeFactory2}
 */
@Deprecated(since = "4.4")
public class ContainerVariableMapper {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Converts a collection of flow variables to a JsonValue conforming to {@link ContainerVariableJsonSchema}.
     *
     * @param flowVariables the flow variables
     * @return a JsonValue representing the flow variables
     * @throws InvalidSettingsException if variables cannot be mapped to {@link ContainerVariableJsonSchema}
     * @since 5.1
     */
    public static JsonValue toContainerVariableJsonValue(final Collection<FlowVariable> flowVariables)
        throws InvalidSettingsException {
        ContainerVariableJsonSchema containerVariableInput =
            new ContainerVariableJsonSchema(createVariables(flowVariables));
        try {
            String containerVariableJson = new ObjectMapper().writeValueAsString(containerVariableInput);
            return JSONUtil.parseJSONValue(containerVariableJson);
        } catch (IOException e) {
            throw new InvalidSettingsException("Could not parse the variables to JsonValue", e);
        }
    }

    private static List<Map<String, Object>> createVariables(final Collection<FlowVariable> flowVariables) {
        List<Map<String, Object>> variables = new ArrayList<>();
        for (FlowVariable flowVariable : flowVariables) {
            String flowVariableName = flowVariable.getName();
            if (!flowVariable.isGlobalConstant()) {
                variables.add(Collections.singletonMap(flowVariableName, parseFlowVariable(flowVariable)));
            }
        }
        return variables;
    }

    private static Object parseFlowVariable(final FlowVariable variable) {
        switch (variable.getType()) {
            case INTEGER:
                return variable.getIntValue();
            case STRING:
                return variable.getStringValue();
            case DOUBLE:
                return variable.getDoubleValue();
            default:
                return variable.getValueAsString();
        }
    }

    /**
     * Maps a json string to {@link ContainerVariableJsonSchema}, if conforms to the schema.
     *
     * @param json json string that should be mapped to {@link ContainerVariableJsonSchema}
     * @return {@link ContainerVariableJsonSchema} of the given input string
     * @throws InvalidSettingsException if the input string does not conform to {@link ContainerVariableJsonSchema}
     */
    public static ContainerVariableJsonSchema toContainerVariableJsonSchema(final String json)
        throws InvalidSettingsException {
        try {
            return OBJECT_MAPPER.readValue(json, ContainerVariableJsonSchema.class);
        } catch (IOException e) {
            throw new InvalidSettingsException("Error while parsing json: " + e.getMessage(), e);
        }
    }

}
