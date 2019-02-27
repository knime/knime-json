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
 *   01.07.2016 (Jonathan Hale): created
 */
package org.knime.core.data.json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Optional;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.junit.Test;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataValue;
import org.knime.core.data.convert.datacell.JavaToDataCellConverter;
import org.knime.core.data.convert.datacell.JavaToDataCellConverterFactory;
import org.knime.core.data.convert.datacell.JavaToDataCellConverterRegistry;
import org.knime.core.data.convert.java.DataCellToJavaConverter;
import org.knime.core.data.convert.java.DataCellToJavaConverterFactory;
import org.knime.core.data.convert.java.DataCellToJavaConverterRegistry;
import org.knime.core.node.ExecutionContext;

/**
 * Test whether the {@link DataCellToJavaConverterFactory} and {@link JavaToDataCellConverterFactory} have been created
 * correctly.
 *
 * @author Jonathan Hale
 */
public class TestConverters {

    /**
     * Test Java -> DataCell conversion.
     *
     * @throws Exception
     */
    @Test
    public void testJavaToDataCellConversion() throws Exception {
        final JsonObject obj = Json.createObjectBuilder().add("name", "KNIME").build();

        final Optional<JavaToDataCellConverterFactory<JsonObject>> factory = JavaToDataCellConverterRegistry
            .getInstance().getConverterFactories(JsonObject.class, JSONCell.TYPE).stream().findFirst();
        assertTrue(factory.isPresent());

        final JavaToDataCellConverter<JsonObject> converter = factory.get().create((ExecutionContext)null);
        assertNotNull(converter);

        final DataCell cell = converter.convert(obj);
        assertEquals(cell.getType(), JSONCell.TYPE);
        assertEquals(((JSONValue)converter.convert(obj)).getJsonValue(), obj);
    }

    /**
     * Test DataCell -> Java conversion.
     *
     * @throws Exception
     */
    @Test
    public void testDataCellToJavaConversion() throws Exception {
        final JsonObject obj = Json.createObjectBuilder().add("name", "KNIME").build();
        final DataCell cell = JSONCellFactory.create(obj);

        final Optional<DataCellToJavaConverterFactory<? extends DataValue, JsonValue>> factory =
            DataCellToJavaConverterRegistry.getInstance().getPreferredConverterFactory(JSONCell.TYPE, JsonValue.class);
        assertTrue(factory.isPresent());

        final DataCellToJavaConverter<? extends DataValue, JsonValue> converter = factory.get().create();
        assertNotNull(converter);

        assertEquals(converter.convertUnsafe(cell), obj);
    }

}
