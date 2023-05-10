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
 *   21.07.2015 (thor): created
 */
package org.knime.json.util;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collections;

import org.knime.core.data.json.JacksonConversions;
import org.knime.core.util.JsonUtil;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsonp.JSONPModule;

import jakarta.json.JsonStructure;
import jakarta.json.JsonValue;
import jakarta.json.JsonWriter;
import jakarta.json.JsonWriterFactory;
import jakarta.json.stream.JsonGenerator;

/**
 * Various utility function for processing JSON.
 *
 * @author Thorsten Meinl, KNIME AG, Zurich, Switzerland
 * @since 2.12
 */
public final class JSONUtil {
    private static final ObjectMapper MAPPER = JacksonConversions.getInstance().newMapper()
        .registerModule(new JSONPModule(JsonUtil.getProvider()));

    /**
     * Returns a pretty-printed string representation of the given JSON object.
     *
     * @param json a JSON structure
     * @return a JSON string
     * @since 5.1
     */
    public static String toPrettyJSONString(final JsonValue json) {
        if (json instanceof JsonStructure) {
            StringWriter stringWriter = new StringWriter();
            JsonWriterFactory writerFactory = JsonUtil.getProvider()
                .createWriterFactory(Collections.singletonMap(JsonGenerator.PRETTY_PRINTING, true));
            try (JsonWriter jsonWriter = writerFactory.createWriter(stringWriter)) {
                jsonWriter.write((JsonStructure) json);
            }
            return stringWriter.toString();
        } else {
            return json.toString();
        }
    }


    /**
     * Parses the given string into a JSON value.
     *
     * @param s a JSON string
     * @return a new JSON value
     * @throws IOException if parsing the presumed JSON string fails (usually no I/O error but an invalid string)
     * @since 5.1
     */
    public static JsonValue parseJSONValue(final String s) throws IOException {
        final Thread currentThread = Thread.currentThread();
        ClassLoader contextClassLoader = currentThread.getContextClassLoader();
        currentThread.setContextClassLoader(JsonValue.class.getClassLoader());

        try {
            final JsonFactory jsonFactory = MAPPER.getFactory();
            JsonParser parser = jsonFactory.createParser(new StringReader(s));
            return MAPPER.readValue(parser, JsonValue.class);
        } finally {
            currentThread.setContextClassLoader(contextClassLoader);
        }
    }
}
