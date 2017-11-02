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

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.knime.core.util.Pair;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonStreamContext;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Jackson-based map to/from JSONPaths.
 *
 * @deprecated because it was very off for the values. See: https://github.com/FasterXML/jackson-core/issues/37
 *
 * @author Gabor Bakos
 */
@Deprecated
public class JsonWithCanonicalPaths {
    private Map<String, JsonLocation> m_pathToStartPosition = new TreeMap<>(), m_pathToEndPosition = new TreeMap<>();

    /**
     * @param json The {@link JsonNode} to wrap.
     *
     */
    public JsonWithCanonicalPaths(final String json) {
        StringBuilder sb = new StringBuilder("$");
        try {
            //visit(sb, new JsonFactory().createParser(json.getBytes("UTF-8")));
            visit(sb, new JsonFactory().createParser(json.toCharArray()));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String parsingContextToPath(final JsonStreamContext ctx) {
        StringBuilder ret = new StringBuilder();
        parsingContextToPath(ret, ctx);
        return ret.toString();
    }

    /**
     * @param sb
     * @param ctx
     */
    private static void parsingContextToPath(final StringBuilder sb, final JsonStreamContext ctx) {
        if (ctx.inRoot()) {
            sb.append("$");
        } else {
            parsingContextToPath(sb, ctx.getParent());
            String string = ctx.toString();
            if (!"{?}".equals(string)) {
                if (string.startsWith("{\"") && string.endsWith("\"}")) {
                    sb.append("['").append(string.substring(2, string.length() - 2)).append("']");
                } else {
                    sb.append(string);
                }
            }
        }
    }

    /**
     * @param path
     * @param json
     */
    private void visit(final StringBuilder path, final JsonParser json) {
        m_pathToStartPosition.put(path.toString(), json.getTokenLocation());
        JsonToken token;
        try {
            while ((token = json.nextToken()) != null) {
                String currentPath = parsingContextToPath(json.getParsingContext());
                if (token.isStructStart()) {
                    String p = currentPath;
                    m_pathToStartPosition.put(currentPath, json.getTokenLocation());
                    if (p.endsWith("[0]") && !m_pathToStartPosition.containsKey(p.substring(0, p.length() - 3))) {
                        m_pathToStartPosition.put(p.substring(0, p.length() - 3), json.getTokenLocation());
                    }
                } else if (token.isStructEnd()) {
                    m_pathToEndPosition.put(currentPath, json.getCurrentLocation());
                } else if (token == JsonToken.FIELD_NAME) {
                    m_pathToStartPosition.put(currentPath, json.getTokenLocation());
                    m_pathToEndPosition.put(currentPath, json.getCurrentLocation());
                } else {
                    m_pathToStartPosition.put(currentPath, json.getTokenLocation());

                    m_pathToEndPosition.put(currentPath, json.getCurrentLocation());
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * @param jsonPath The JSONPath to be queried.
     * @return The (approximate) start and end {@link JsonLocation} of the value selected.
     */
    public Pair<JsonLocation, JsonLocation> get(final String jsonPath) {
        if (m_pathToEndPosition.containsKey(jsonPath)) {
            JsonLocation ret = m_pathToStartPosition.get(jsonPath);
            return Pair.create(ret == null ? JsonLocation.NA : ret, m_pathToEndPosition.get(jsonPath));
        }
        return Pair.create(JsonLocation.NA, JsonLocation.NA);
    }

    /**
     * Example test cases.
     *
     * @param args Not used.
     */
    public static void main(final String[] args) {
        ObjectMapper mapper = new ObjectMapper();
        for (JsonNode json : new JsonNode[]{
            mapper.createArrayNode().add(3).add(mapper.createArrayNode())
                .add(mapper.createObjectNode().set("v", mapper.createArrayNode().add(33))),
            mapper.createArrayNode().add(mapper.createObjectNode().put("y", 4).set("v", mapper.createArrayNode())),
            mapper.createArrayNode().add(mapper.createObjectNode()), mapper.createArrayNode(),
            mapper.createObjectNode(), mapper.createObjectNode().put("test", "tt").put("set", "s"),
            mapper.createArrayNode().add(3).add("v"), mapper.createArrayNode().add(mapper.createArrayNode().add("bb")),}) {
            System.out.println(json);
            JsonWithCanonicalPaths paths = new JsonWithCanonicalPaths(json.toString());
            System.out.println(paths.m_pathToStartPosition);
            System.out.println(paths.m_pathToEndPosition);
            System.out.println("-----------------");
        }
    }

    /**
     * @param caretPosition The character position of caret within the editor.
     * @return The path specified by {@code caretPosition} as {@link JsonLocation#getByteOffset()}.
     */
    public String findPath(final int caretPosition) {
        Entry<String, JsonLocation> bestPath = null;
        for (Entry<String, JsonLocation> entry : m_pathToStartPosition.entrySet()) {
            //            long start = entry.getValue().getByteOffset();
            long start = entry.getValue().getCharOffset();
            //            long end = m_pathToEndPosition.get(entry.getKey()).getByteOffset();
            long end = m_pathToEndPosition.get(entry.getKey()).getCharOffset();
            if (start <= caretPosition && end > caretPosition) {
                //                if (bestPath == null || bestPath.getValue().getByteOffset() < start || m_pathToEndPosition.get(bestPath.getKey()).getByteOffset() > end) {
                if (bestPath == null || bestPath.getValue().getCharOffset() < start
                    || m_pathToEndPosition.get(bestPath.getKey()).getCharOffset() > end) {
                    bestPath = entry;
                }
            }
        }
        return bestPath == null ? null : bestPath.getKey();
    }
}
