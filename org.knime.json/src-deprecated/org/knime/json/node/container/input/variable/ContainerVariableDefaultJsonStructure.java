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
package org.knime.json.node.container.input.variable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.knime.core.util.JsonUtil;
import org.knime.json.node.container.input.variable2.ContainerVariableInputNodeFactory2;

import jakarta.json.JsonArray;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonValue;

/**
 * Class that holds a hard coded prototype JSON structure for the Container Input (Variable) node.
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 * @since 3.6
 * @deprecated superseded by {@link ContainerVariableInputNodeFactory2}
 */
@Deprecated(since = "4.4")
public final class ContainerVariableDefaultJsonStructure {

    private ContainerVariableDefaultJsonStructure() {
        // do not initialize me
    }

    /**
     * Default string variable name.
     */
    public static final String STRING_VARIABLE_NAME = "variable-string";

    /**
     * Default string variable value.
     */
    public static final String STRING_VARIABLE_VALUE = "somevariable";

    /**
     * Default double variable name.
     */
    public static final String DOUBLE_VARIABLE_NAME = "variable-double";

    /**
     * Default double variable value.
     */
    public static final Double DOUBLE_VARIABLE_VALUE = 42.0;

    /**
     * Default int variable name.
     */
    public static final String INT_VARIABLE_NAME = "variable-int";

    /**
     * Default int variable value.
     */
    public static final Integer INT_VARIABLE_VALUE = 100;

    /**
     * Returns the default variables as a list of singleton maps.
     *
     * @return the list of default variables
     */
    public static List<Map<String, Object>> asVariableList() {
        Map<String, Object> variable1 = Collections.singletonMap(STRING_VARIABLE_NAME, STRING_VARIABLE_VALUE);
        Map<String, Object> variable2 = Collections.singletonMap(DOUBLE_VARIABLE_NAME, DOUBLE_VARIABLE_VALUE);
        Map<String, Object> variable3 = Collections.singletonMap(INT_VARIABLE_NAME, INT_VARIABLE_VALUE);

        return Arrays.asList(variable1, variable2, variable3);
    }

    /**
     * Returns a string representation of the default structure.
     *
     * @return the default variable input as string
     */
    public static String asString() {
        return asJsonValue().toString();
    }

    /**
     * Returns a JsonValue of the default structure.
     *
     * @return the default variable input as JsonValue
     */
    public static JsonValue asJsonValue() {
        JsonBuilderFactory factory = JsonUtil.getProvider().createBuilderFactory(null);
        return factory.createObjectBuilder().add("variables", createVariables(factory)).build();
    }

    private static JsonArray createVariables(final JsonBuilderFactory factory) {
        return factory.createArrayBuilder() //
            .add(factory.createObjectBuilder().add(STRING_VARIABLE_NAME, STRING_VARIABLE_VALUE)) //
            .add(factory.createObjectBuilder().add(DOUBLE_VARIABLE_NAME, DOUBLE_VARIABLE_VALUE)) //
            .add(factory.createObjectBuilder().add(INT_VARIABLE_NAME, INT_VARIABLE_VALUE)) //
            .build(); //
    }

}
