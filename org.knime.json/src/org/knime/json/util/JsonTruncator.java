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
 *   12 Sept 2024 (jasper): created
 */
package org.knime.json.util;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.apache.commons.io.output.CountingOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.knime.core.node.NodeLogger;

import jakarta.json.Json;
import jakarta.json.stream.JsonGenerationException;
import jakarta.json.stream.JsonGenerator;
import jakarta.json.stream.JsonGeneratorFactory;
import jakarta.json.stream.JsonParser;
import jakarta.json.stream.JsonParser.Event;
import jakarta.json.stream.JsonParsingException;

/**
 * Utility for truncating long JSON strings to e.g. load them into a dialog
 *
 * @author Leonard Wörteler, KNIME GmbH, Konstanz, Germany
 * @author Jasper Krauter, KNIME GmbH, Konstanz, Germany
 * @since 5.4
 */
public final class JsonTruncator {

    private static final String ELLIPSIS = "[…]";

    private final JsonGeneratorFactory m_generatorFactory;

    private final int m_maxStringLength;

    private final int m_lengthLimit;

    private JsonTruncator(final int lengthLimit, final int maxStringLength, final boolean prettyPrint) {
        m_lengthLimit = lengthLimit;
        m_maxStringLength = maxStringLength;
        m_generatorFactory = Json.createGeneratorFactory(Map.of(JsonGenerator.PRETTY_PRINTING, prettyPrint));
    }

    /**
     * Abbreviates a JSON string to a given length limit by truncating string literals and cutting off objects and
     * arrays after a certain maximum length has been reached.
     *
     * @param input An input stream of JSON data
     * @param lengthLimit the length limit of the output JSON string in bytes
     * @param maxStringLength the maximum length of string literals in the output JSON string
     * @param prettyPrint whether to pretty print the output JSON string
     * @return the abbreviated JSON string
     * @throws IOException
     * @throws JsonParsingException if the input JSON is invalid
     * @since 5.4
     */
    public static String abbreviate(final InputStream input, final int lengthLimit, final int maxStringLength,
        final boolean prettyPrint) throws IOException {
        final var abbreviator = new JsonTruncator(lengthLimit, maxStringLength, prettyPrint);
        try (final var parser =
            Json.createParser(new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8)))) {
            final var output = new ByteArrayOutputStream();
            abbreviator.process(parser, output);
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        } catch (JsonGenerationException e) {
            NodeLogger.getLogger(JsonTruncator.class).error("Failed to generate truncated JSON", e);
            return "{}"; // output still valid (but very truncated) JSON
        }
    }

    @SuppressWarnings("resource") // JsonGenerator is properly closed in try-with-resources
    private void process(final JsonParser parser, final OutputStream output) throws IOException { // NOSONAR: complexity accepted
        try (final var countingOut = new CountingOutputStream(output);
                final var generator = m_generatorFactory.createGenerator(countingOut, StandardCharsets.UTF_8)) {
            var depth = 0; // one depth = one array or object context
            while (parser.hasNext()) { // NOSONAR: break; and continue; are needed for loop logic (and not confusing)
                final var event = parser.next();
                switch (event) {
                    case START_ARRAY:
                        generator.writeStartArray(); // "["
                        depth++; // push array context
                        break;
                    case START_OBJECT:
                        generator.writeStartObject(); // "{"
                        depth++; // push object context
                        break;
                    case KEY_NAME:
                        generator.writeKey(parser.getString());
                        break;
                    case VALUE_FALSE, VALUE_TRUE:
                        generator.write(event == JsonParser.Event.VALUE_TRUE);
                        break;
                    case VALUE_NULL:
                        generator.writeNull();
                        break;
                    case VALUE_NUMBER:
                        generator.write(parser.getValue());
                        break;
                    case VALUE_STRING:
                        // abbreviate string literals
                        generator.write(StringUtils.abbreviate(parser.getString(), ELLIPSIS, m_maxStringLength));
                        break;
                    case END_ARRAY, END_OBJECT:
                        generator.writeEnd(); // "]" or "}"
                        depth--;
                        break;
                    default:
                        throw new IllegalStateException("Unexpected JSON event type: " + event.name());
                }

                // we cannot abbreviate after a key name without an associated value
                if (event == Event.KEY_NAME) {
                    continue;
                }

                // flush the generator so the byte count is accurate
                generator.flush();

                // check whether the already written JSON exceeds the length limit
                if (countingOut.getByteCount() > m_lengthLimit) {
                    // do not read more JSON, only finish up output
                    break;
                }
            } // end of event loop

            // clean up by closing brackets, if necessary
            while (depth > 0) {
                generator.writeEnd();
                depth--;
            }
        }
    }
}
