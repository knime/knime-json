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
package org.knime.core.data.json.container.variables;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.knime.core.node.NodeLogger;
import org.knime.json.node.container.input.variable2.ContainerVariableInputNodeFactory2;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.json.JsonValue;

/**
 * Json schema for flow variables sent to a Container Input (Variable) node.
 * Is serializable/deserializable with Jackson.
 *
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 * @since 3.6
 * @deprecated superseded by {@link ContainerVariableInputNodeFactory2}
 */
@Deprecated(since="4.4")
public class ContainerVariableJsonSchema {

    private final List<Map<String, Object>> m_variables;

    /**
     * Constructor for the Container Variable Json Schema.
     *
     * @param variables the variables in this input
     */
    @JsonCreator
    public ContainerVariableJsonSchema(@JsonProperty("variables") final List<Map<String, Object>> variables) {
        m_variables = variables;
    }

    /**
     * Returns the variables in this input.
     *
     * @return the variables
     */
    @JsonProperty("variables")
    public List<Map<String, Object>> getVariables() {
        return m_variables;
    }

    /**
     * Checks if a json value conforms to the structure of {@link ContainerVariableJsonSchema}.
     *
     * @param jsonValue the json value under question
     * @return true if the supplied Json value conforms to {@link ContainerVariableJsonSchema}
     * @since 5.1
     */
    public static boolean hasContainerVariablesJsonSchema(final JsonValue jsonValue) {
        if (jsonValue.toString().equals("{}")) {
            return false;
        }
        try {
            new ObjectMapper().readValue(jsonValue.toString(), ContainerVariableJsonSchema.class);
            return true;
        } catch (IOException e) {
            NodeLogger.getLogger(ContainerVariableJsonSchema.class).error(e);
            return false;
        }
    }

}
