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

import java.util.LinkedHashMap;
import java.util.Map;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

/**
 * Json schema for flow variables sent to a Container Input (Variable) node. Is serializable/deserializable with
 * Jackson.
 *
 * Full schema, has a key-value pair for each flow variable.
 *
 * @see SimpleSchema
 * @author Jannik Löscher, KNIME GmbH, Konstanz, Germany
 * @since 4.5
 */
public final class ContainerVariableJsonSchema2 {

    private final Map<String, Object> m_variables = new LinkedHashMap<>();

    /**
     * Add a variable to this schema.
     *
     * @param name the name of the variable
     * @param value the value of the variable
     * @throws InvalidSettingsException if the name is blank or already contained
     */
    @JsonAnySetter
    void addVariable(final String name, final Object value) throws InvalidSettingsException {
        final var cleanName = name.trim();
        if (cleanName.isEmpty()) {
            throw new InvalidSettingsException("Name is empty");
        }
        if (m_variables.containsKey(cleanName)) {
            throw new InvalidSettingsException("Name is already in use: " + cleanName);
        }
        m_variables.put(name, value);
    }

    /**
     * Returns the variables in this input.
     *
     * @return the variables
     */
    @JsonAnyGetter
    public Map<String, Object> getVariables() {
        return m_variables;
    }

    /**
     * Simple schema containing only the flow variable value in JSON encoding.
     */
    public static class SimpleSchema {
        final Object m_value;

        SimpleSchema(final String value) {
            m_value = value;
        }

        SimpleSchema(final Integer value) {
            m_value = value;
        }

        SimpleSchema(final Double value) {
            m_value = value;
        }

        SimpleSchema(final Boolean value) {
            m_value = value;
        }

        ContainerVariableJsonSchema2 build(final String name) {
            final var result = new ContainerVariableJsonSchema2();
            try {
                result.addVariable(name, m_value);
            } catch (InvalidSettingsException e) { // should not happen
                NodeLogger.getLogger(ContainerVariableInputNodeModel2.class).coding(e);
            }
            return result;
        }
    }
}
