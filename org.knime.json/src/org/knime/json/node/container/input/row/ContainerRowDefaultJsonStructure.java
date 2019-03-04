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
 *   Apr 3, 2018 (Tobias Urhaug): created
 */
package org.knime.json.node.container.input.row;

import java.io.IOException;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonValue;

import org.knime.json.util.JSONUtil;

/**
 * Class that holds a hard coded prototype JSON structure for the Container input/output nodes.
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 * @since 3.8
 */
public final class ContainerRowDefaultJsonStructure {

    /**
     * Creates a JsonValue object of the hard coded structure.
     *
     * @return a JsonValue of the structure
     */
    public static JsonValue asJsonValue() {
        try {
            return JSONUtil.parseJSONValue(asString());
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Returns the string representation of the JSON structure.
     * @return the string representation of the JSON structure
     */
    public static String asString() {
        JsonBuilderFactory factory = Json.createBuilderFactory(null);
        return
            factory.createObjectBuilder()
                .add("table-spec",  createTableSpec(factory))
                .add("table-data", createDataTable(factory))
                .build()
                .toString();
    }

    private static JsonArray createTableSpec(final JsonBuilderFactory factory) {
        return
            factory.createArrayBuilder()
                .add(factory.createObjectBuilder().add("column-string", "string"))
                .add(factory.createObjectBuilder().add("column-int", "int"))
                .add(factory.createObjectBuilder().add("column-double", "double"))
                .add(factory.createObjectBuilder().add("column-long", "long"))
                .add(factory.createObjectBuilder().add("column-boolean", "boolean"))
                .add(factory.createObjectBuilder().add("column-localdate", "localdate"))
                .add(factory.createObjectBuilder().add("column-localdatetime", "localdatetime"))
                .add(factory.createObjectBuilder().add("column-zoneddatetime", "zoneddatetime"))
                .build();
    }

    private static JsonArray createDataTable(final JsonBuilderFactory factory) {
        return
            factory.createArrayBuilder()
                .add(
                    createRow(
                        factory,
                        "value1",
                        1,
                        1.5,
                        1000,
                        true,
                        "2018-03-27",
                        "2018-03-27T08:30:45.111",
                        "2018-03-27T08:30:45.111+01:00[Europe/Paris]")
                    )
                .build();
    }

    private static JsonArray createRow(final JsonBuilderFactory factory, final Object... cells) {
        JsonArrayBuilder arrayBuilder = factory.createArrayBuilder();
        for (Object cell : cells) {
            if (cell instanceof String) {
                arrayBuilder.add((String) cell);
            } else if (cell instanceof Integer) {
                arrayBuilder.add((Integer) cell);
            } else if (cell instanceof Double) {
                arrayBuilder.add((Double) cell);
            } else if (cell instanceof Long) {
                arrayBuilder.add((Long) cell);
            } else if (cell instanceof Boolean) {
                arrayBuilder.add((Boolean) cell);
            }
        }

        return arrayBuilder.build();
    }

}
