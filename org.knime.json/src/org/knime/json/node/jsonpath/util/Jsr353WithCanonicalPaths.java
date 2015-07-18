/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
 *   24 Febr 2015 (Gabor): created
 */
package org.knime.json.node.jsonpath.util;

import java.io.StringReader;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;
import java.util.TreeMap;

import javax.json.Json;
import javax.json.stream.JsonLocation;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;

import org.knime.core.util.Pair;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.TextNode;

/**
 * The JSR-353-based JSONPath to/from positions ({@link JsonLocation}) within documents.
 *
 * @author Gabor Bakos
 */
public class Jsr353WithCanonicalPaths {
    /**
     * The guard to represent the non-array path array position on the stack.
     * @see #visit(JsonParser)
     */
    private static final int GUARD = -2;

    private static final class JsonLocationImplementation implements JsonLocation {
        private int m_offset;

        private int m_line;

        private int m_col;

        /**
         * @param offset Offset within document ({@code 0}-based).
         * @param line Line number ({@code 1}-based).
         * @param col Column number ({@code 1}-based).
         */
        private JsonLocationImplementation(final int offset, final int line, final int col) {
            this.m_offset = offset;
            this.m_line = line;
            this.m_col = col;
        }

        @Override
        public long getStreamOffset() {
            return m_offset;
        }

        @Override
        public long getLineNumber() {
            return m_line;
        }

        @Override
        public long getColumnNumber() {
            return m_col;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + m_col;
            result = prime * result + m_line;
            result = prime * result + m_offset;
            return result;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            JsonLocationImplementation other = (JsonLocationImplementation)obj;
            if (m_col != other.m_col) {
                return false;
            }
            if (m_line != other.m_line) {
                return false;
            }
            if (m_offset != other.m_offset) {
                return false;
            }
            return true;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return String.format("JsonLocationImplementation [m_line=%s, m_col=%s, m_offset=%s]", m_line, m_col,
                m_offset);
        }
    }

    private static final JsonLocation UNKNOWN = new JsonLocationImplementation(-1, -1, -1);

    private Map<String, JsonLocation> m_pathToStartPosition = new TreeMap<>(), m_pathToEndPosition = new TreeMap<>();

    /**
     * @param json The {@link JsonNode} to wrap.
     */
    public Jsr353WithCanonicalPaths(final String json) {
        String trimmed = json.trim();
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            visit(Json.createParser(new StringReader(json)));
        } else {
            m_pathToStartPosition.put("$", new JsonLocationImplementation(0, 1, 1));
            String[] lines = json.split("\\n", -1);
            m_pathToEndPosition.put("$", new JsonLocationImplementation(json.length(), lines.length,
                lines[lines.length - 1].length() + 1));
        }
    }

    /**
     * Fills the start and end positions.
     *
     * @param json {@link JsonParser} to use.
     */
    private void visit(final JsonParser json) {
        Stack<Pair<String, Integer>> stack = new Stack<>();
        stack.push(Pair.create("$", GUARD));
        JsonLocation lastLocation = json.getLocation();
        m_pathToStartPosition.put(stack.peek().getFirst(), json.getLocation());
        for (; json.hasNext();) {
            Event token = json.next();
            switch (token) {
                case START_ARRAY:
                    m_pathToStartPosition.put(toPath(stack.peek()), lastLocation);
                    stack.push(Pair.create(toPath(stack.peek()), 0));
                    lastLocation = json.getLocation();
                    break;
                case START_OBJECT:
                    stack.push(Pair.create(toPath(stack.peek()), GUARD));
                    m_pathToStartPosition.put(toPath(stack.peek()), lastLocation);
                    lastLocation = json.getLocation();
                    break;
                case END_ARRAY: {
                    stack.pop();
                    m_pathToEndPosition.put(toPath(stack.peek()), json.getLocation());
                    //after a value, we have to increase the index when we are in an array
                    Pair<String, Integer> pop2 = stack.pop();
                    if (pop2.getSecond() != GUARD) {
                        stack.push(Pair.create(pop2.getFirst(), pop2.getSecond() + 1));
                    }
                }
                    break;
                case END_OBJECT: {
                    stack.pop();
                    m_pathToEndPosition.put(toPath(stack.peek()), json.getLocation());
                    lastLocation = json.getLocation();
                    //after a value, we have to increase the index when we are in an array
                    if (stack.peek().getSecond() != GUARD) {
                        Pair<String, Integer> pop2 = stack.pop();
                        stack.push(Pair.create(pop2.getFirst(), pop2.getSecond() + 1));
                    } else {
                    	stack.pop();
                    }
                }
                    break;
                case KEY_NAME:
                    lastLocation = json.getLocation();
                    stack.push(Pair.create(toPath(stack.peek()) + "['" + json.getString() + "']", GUARD));
                    m_pathToStartPosition.put(toPath(stack.peek()), json.getLocation());
                    break;
                case VALUE_FALSE:
                case VALUE_NULL:
                case VALUE_NUMBER:
                case VALUE_STRING:
                case VALUE_TRUE: {
                    //after a value, we have to increase the index when we are in an array
                    Pair<String, Integer> pop = stack.pop();
                    m_pathToStartPosition.put(toPath(pop), lastLocation);
                    m_pathToEndPosition.put(toPath(pop), json.getLocation());
                    lastLocation = json.getLocation();
                    if (pop.getSecond() != GUARD) {
                        stack.push(Pair.create(pop.getFirst(), pop.getSecond() + 1));
                    }
                }
                    break;
            }
        }
    }

    /**
     * @param pair A pair of path and current position within array.
     * @return The canonical JSONPath.
     */
    private String toPath(final Pair<String, Integer> pair) {
        return pair.getSecond() == GUARD ? pair.getFirst() : pair.getFirst() + "[" + Math.max(0, pair.getSecond()) + "]";
    }

    /**
     * @param jsonPath A JSONPath.
     * @return The start and end of the value pointed by that {@code jsonPath}.
     */
    public Pair<JsonLocation, JsonLocation> get(final String jsonPath) {
        if (m_pathToEndPosition.containsKey(jsonPath)) {
            JsonLocation ret = m_pathToStartPosition.get(jsonPath);
            return Pair.create(ret == null ? UNKNOWN : ret, m_pathToEndPosition.get(jsonPath));
        }
        return Pair.create(UNKNOWN, UNKNOWN);
    }

    /**
     * Some tests.
     *
     * @param args Not used.
     */
    public static void main(final String[] args) {
        ObjectMapper mapper = new ObjectMapper();
        for (JsonNode json : new JsonNode[]{
            new IntNode(22),
            new TextNode("Hello world"),
            mapper.createArrayNode().add(3).add(mapper.createArrayNode())
                .add(mapper.createObjectNode().set("v", mapper.createArrayNode().add(33))), mapper.createArrayNode(),
            mapper.createArrayNode().add(mapper.createArrayNode().add("bb")), mapper.createArrayNode().add(3).add("v"),
            mapper.createArrayNode().add(mapper.createObjectNode().put("y", 4).set("v", mapper.createArrayNode())),
            mapper.createArrayNode().add(mapper.createObjectNode()), mapper.createObjectNode(),
            mapper.createObjectNode().put("test", "tt").put("set", "s"),}) {
            System.out.println(json);
            Jsr353WithCanonicalPaths paths = new Jsr353WithCanonicalPaths(json.toString());
            System.out.println(paths.m_pathToStartPosition);
            System.out.println(paths.m_pathToEndPosition);
            System.out.println("-----------------");
        }
        String json = "\"HEllo \nworld\"";
        System.out.println(json);
        Jsr353WithCanonicalPaths paths = new Jsr353WithCanonicalPaths(json);
        System.out.println(paths.m_pathToStartPosition);
        System.out.println(paths.m_pathToEndPosition);
        System.out.println("-----------------");
    }

    /**
     * @param caretPosition The character position of caret within the editor.
     * @return The path specified by {@code caretPosition} as {@link JsonLocation#getStreamOffset()}.
     */
    public String findPath(final int caretPosition) {
        Entry<String, JsonLocation> bestPath = null;
        for (Entry<String, JsonLocation> entry : m_pathToStartPosition.entrySet()) {
            long start = entry.getValue().getStreamOffset();
            long end = m_pathToEndPosition.get(entry.getKey()).getStreamOffset();
            if (start <= caretPosition && end > caretPosition) {
                if (bestPath == null || bestPath.getValue().getStreamOffset() < start
                    || m_pathToEndPosition.get(bestPath.getKey()).getStreamOffset() > end) {
                    bestPath = entry;
                }
            }
        }
        return bestPath == null ? null : bestPath.getKey();
    }
}
