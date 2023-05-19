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
 *   May 19, 2023 (wiswedel): created
 */
package org.knime.core.data.json;

import org.assertj.core.api.Assertions;
import org.assertj.core.util.IterableUtil;
import org.junit.jupiter.api.Test;

import jakarta.json.Json;

/**
 * Tests {@link JsonValueToJavaConverterFactory}, especially backward compatibility of identifiers.
 * @author wiswedel
 */
@SuppressWarnings("static-method")
final class JsonValueToJavaConverterFactoryTest {

    /**
     * Test method for {@link org.knime.core.data.json.JsonValueToJavaConverterFactory#getIdentifierAliases()}.
     */
    @Test
    final void testGetIdentifierAliases() {
        var instance = new JsonValueToJavaConverterFactory();
        Assertions.assertThat(IterableUtil.toArray(instance.getIdentifierAliases(), String.class)) //
            .as("Number of aliases").hasSize(1) //
            .as("Old alias before moving to jakarta.json").contains(
                // eh, why copy it from JsonValueToJavaConverterFactory? Because the name/identifier is stored in
                // old workflows and is therefore a constant
                "org.knime.core.data.convert.java.SimpleDataCellToJavaConverterFactory("
                    + "JSONValue,interface javax.json.JsonValue,JsonValue)");
    }

    /**
     * Test method for {@link org.knime.core.data.convert.java.SimpleDataCellToJavaConverterFactory#getIdentifier()}.
     */
    @Test
    final void testGetIdentifier() {
        var instance = new JsonValueToJavaConverterFactory();
        Assertions.assertThat(instance.getIdentifier()).as("Backward compatible identifier")
            .isEqualTo("org.knime.core.data.json.JsonValueToJavaConverterFactory("
                + "JSONValue,interface jakarta.json.JsonValue,JsonValue)");
    }

    /** Tests basic conversion logic. */
    @Test
    final void testConversion() throws Exception {
        var instance = new JsonValueToJavaConverterFactory();
        var converter = instance.create();
        Assertions.assertThat(converter.convertUnsafe(JSONCellFactory.create("{\"foo\":\"bar\"}"))) //
        .as("simple converted json").isEqualTo(Json.createObjectBuilder().add("foo", "bar").build());
    }

}
