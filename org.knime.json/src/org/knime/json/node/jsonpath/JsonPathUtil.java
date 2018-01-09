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
 *   5 Nov. 2014 (Gabor): created
 */
package org.knime.json.node.jsonpath;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.knime.json.internal.Activator;
import org.knime.json.node.util.ErrorHandling;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.jayway.jsonpath.JsonPath;

/**
 * Some utility methods to ease JsonPath handling.
 *
 * @author Gabor Bakos
 */
public class JsonPathUtil {
    private JsonPathUtil() {
        //Hide constructor
    }

    /**
     * Converts the result from {@link JsonPath#read(Object, com.jayway.jsonpath.Configuration)} to a {@link List} of {@link String}s.
     *
     * @param o Result from {@link JsonPath#read(Object, com.jayway.jsonpath.Configuration)}.
     * @return A (potentially unmodifiable) {@link List} of {@link String}s. Never {@code null}.
     */
    public static List<String> asList(final Object o) {
        List<String> ret;
        if (o instanceof List<?>) {
            List<?> oldList = (List<?>)o;
            ret = new ArrayList<>(oldList.size());
            for (Object object : oldList) {
                String str = object instanceof String ? (String)object : object.toString();
                ret.add(str);
            }
            return ret;
        }
        if (o instanceof Iterable<?>) {
            Iterable<?> itr = (Iterable<?>)o;
            ret = new ArrayList<>();
            for (Object object : itr) {
                String str = object instanceof String ? (String)object : object.toString();
                ret.add(str);
            }
            return ret;
        }
        if (o instanceof String) {
            String str = (String)o;
            return Collections.singletonList(str);
        }
        if (o == null) {
            return Collections.emptyList();
        }
        try {
            return asList(Activator.getInstance().getJsonPathConfiguration().jsonProvider().toIterable(o));
        } catch (RuntimeException e) {
            return Collections.singletonList(o.toString());
        }
    }

    /**
     * Converts a value to Jackson classes.
     *
     * @param factory The {@link JsonNodeFactory} for Jackson classes.
     * @param value The value to convert.
     * @return Converted object.
     * @throws IllegalStateException When in a map the keys are not all {@link String}s.
     * @throws IllegalArgumentException When unsupported type is in the {@code object}.
     */
    public static JsonNode toJackson(final JsonNodeFactory factory, final Object value) {
        if (value instanceof Map<?, ?>) {
            Map<?, ?> map = (Map<?, ?>)value;
            Map<String, JsonNode> objectNodeContent = new LinkedHashMap<>(map.size());
            for (Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() instanceof String) {
                    String key = (String)entry.getKey();
                    if (entry.getValue() instanceof JsonNode) {
                        JsonNode v = (JsonNode)entry.getValue();
                        objectNodeContent.put(key, v);
                    } else {
                        objectNodeContent.put(key, toJackson(factory, entry.getValue()));
                    }
                } else {
                    throw new IllegalStateException("The key for the JSON object is not a String: "
                        + entry.getKey());
                }
            }
            return factory.objectNode().setAll(objectNodeContent);
        }
        if (value instanceof Integer) {
            Integer i = (Integer)value;
            return factory.numberNode(i);
        }
        if (value instanceof Long) {
            Long l = (Long)value;
            return factory.numberNode(l);
        }
        if (value instanceof BigInteger) {
            BigInteger bi = (BigInteger)value;
            return factory.numberNode(bi);
        }
        if (value instanceof BigDecimal) {
            BigDecimal bd = (BigDecimal)value;
            return factory.numberNode(bd);
        }
        if (value instanceof Double) {
            Double d = (Double)value;
            return factory.numberNode(d);
        }
        if (value instanceof Float) {
            Float f = (Float)value;
            return factory.numberNode(f);
        }
        if (value instanceof Short) {
            Short s = (Short)value;
            return factory.numberNode(s);
        }
        if (value instanceof Byte) {
            Byte b = (Byte)value;
            return factory.numberNode(b);
        }
        if (value instanceof byte[]) {
            byte[] bs = (byte[])value;
            return factory.binaryNode(bs);
        }
        if (value instanceof Boolean) {
            Boolean b = (Boolean)value;
            return factory.booleanNode(b.booleanValue());
        }
        if (value instanceof String) {
            String s = (String)value;
            return factory.textNode(s);
        }
        if (value instanceof Object[]) {
            Object[] os = (Object[])value;
            ArrayNode array = factory.arrayNode();
            for (int i = 0; i < os.length; ++i) {
                array.add(toJackson(factory, os[i]));
            }
            return array;
        }
        if (value instanceof JsonNode) {
            JsonNode node = (JsonNode)value;
            return node;
        }
        if (value instanceof Iterable<?>) {
            Iterable<?> os = (Iterable<?>)value;
            ArrayNode array = factory.arrayNode();
            for (Object o : os) {
                array.add(toJackson(factory, o));
            }
            return array;
        }
        if (value == null) {
            return factory.nullNode();
        }
        throw new IllegalArgumentException("Not supported content: "
            + ErrorHandling.shorten(value.toString(), 77) + "\nType: " + value.getClass());
    }

}
