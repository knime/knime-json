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
 *   24 April 2015 (Gabor): created
 */
package org.knime.json.node.jsonpath.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.knime.core.node.util.CheckUtils;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;

/**
 * Simple JSONPath parser which can handle {@link Option#AS_PATH_LIST} results (without escaping).
 *
 * @author Gabor Bakos
 */
public class SimplePathParser {

    /**
     * A (marker) interface for parts of {@link Path}.
     */
    public static interface PathPart extends Comparable<PathPart> {
    }

    /**
     * Represents a canonical {@link JsonPath} path. Similar to {@link com.jayway.jsonpath.internal.Path}.
     */
    public static class Path implements Comparable<Path> {
        private final List<PathPart> m_parts;

        /**
         * @param canonicalPath A canonical {@link JsonPath} in {@link String} representation.
         */
        public Path(final String canonicalPath) {
            this(parts(canonicalPath));
        }

        /**
         * @param parts The parts to be used to represent {@link Path}.
         */
        private Path(final List<PathPart> parts) {
            super();
            this.m_parts = parts;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int compareTo(final Path o) {
            List<PathPart> p = m_parts, op = o.m_parts;
            for (int i = 0; i < p.size(); ++i) {
                if (op.size() > i) {
                    PathPart p0 = p.get(i), o0 = op.get(i);
                    int comp = p0.compareTo(o0);
                    if (comp == 0) {
                        continue;
                    }
                    return comp;
                }
                return 1;
            }
            return p.size() - op.size();
        }

        /**
         * @return {@code true} if it ends with an array index part.
         */
        public boolean endsWithIndex() {
            return m_parts.size() > 0 && m_parts.get(m_parts.size() - 1) instanceof Index;
        }

        /**
         * @return A Path where the last part is replaced with a star (in case there are any).
         */
        public Path replaceLastWithStar() {
            List<PathPart> parts = new ArrayList<>(m_parts);
            if (!parts.isEmpty()) {
                parts.set(parts.size() - 1, IndexStar.INSTANCE);
            }
            return new Path(parts);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("$[");
            for (PathPart pathPart : m_parts) {
                sb.append(pathPart).append("][");
            }
            sb.append("]");
            sb.setLength(sb.length() - 2);
            return sb.toString();
        }



        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((m_parts == null) ? 0 : m_parts.hashCode());
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
            Path other = (Path)obj;
            if (m_parts == null) {
                if (other.m_parts != null) {
                    return false;
                }
            } else if (!m_parts.equals(other.m_parts)) {
                return false;
            }
            return true;
        }

        /**
         * @return A {@link Path} where the last array index was replaced with a star.
         */
        public Path lastIndexToStar() {
            ArrayList<PathPart> parts = new ArrayList<>(m_parts);
            for (int i = m_parts.size(); i-- > 0;) {
                if (parts.get(i) instanceof Index) {
                    parts.set(i, IndexStar.INSTANCE);
                    break;
                }
            }
            return new Path(parts);
        }

        /**
         * @param other Another {@link Path}.
         * @return {@code true} iff this {@link Path} starts with {@code other}.
         */
        public boolean startsWith(final Path other) {
            if (other.m_parts.size() > m_parts.size()) {
                return false;
            }
            for (int i = 0; i < other.m_parts.size(); i++) {
                if (m_parts.get(i).compareTo(other.m_parts.get(i)) != 0) {
                    return false;
                }
            }
            return true;
        }
    }

    private static class Key implements PathPart {
        private final String m_key;

        /**
         * @param key
         */
        public Key(final String key) {
            super();
            this.m_key = key;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int compareTo(final PathPart o) {
            if (o instanceof Key) {
                Key ok = (Key)o;
                return m_key.compareTo(ok.m_key);
            }
            if (o instanceof Index || o instanceof IndexStar) {
                return -1;
            }
            throw new UnsupportedOperationException(o.getClass().getName());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((m_key == null) ? 0 : m_key.hashCode());
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
            Key other = (Key)obj;
            if (m_key == null) {
                if (other.m_key != null) {
                    return false;
                }
            } else if (!m_key.equals(other.m_key)) {
                return false;
            }
            return true;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return String.format("'%s'", m_key);
        }

    }

    private static class Index implements PathPart {
        private final int m_index;

        /**
         * @param index
         */
        public Index(final int index) {
            super();
            this.m_index = index;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int compareTo(final PathPart o) {
            if (o instanceof Index) {
                Index oi = (Index)o;
                return Integer.compare(m_index, oi.m_index);
            }
            if (o instanceof Key) {
                return 1;
            }
            if (o instanceof IndexStar) {
                return 1;
            }
            throw new UnsupportedOperationException(o.getClass().getName());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + m_index;
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
            Index other = (Index)obj;
            if (m_index != other.m_index) {
                return false;
            }
            return true;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return String.format("%s", m_index);
        }
    }

    private static class IndexStar implements PathPart {
        static final IndexStar INSTANCE = new IndexStar();

        /**
         *
         */
        private IndexStar() {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int compareTo(final PathPart o) {
            if (o instanceof Key) {
                return 1;
            }
            if (o instanceof Index) {
                return -1;
            }
            if (o instanceof IndexStar) {
                return 0;
            }
            throw new UnsupportedOperationException(o.getClass().getName());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return "*";
        }
    }

    /**
     * Constructs {@link PathPart}s from a canonical {@link JsonPath}, {@code jsonPath}.
     *
     * @param jsonPath A canonical {@link JsonPath} in {@link String} representation.
     * @return The {@link PathPart}s of {@code jsonPath}.
     */
    private static List<PathPart> parts(final String jsonPath) {
        if ("$".equals(jsonPath)) {
            return Collections.emptyList();
        }
        CheckUtils.checkArgument(jsonPath.length() > 3, jsonPath + " is too short");
        String stripped = jsonPath.substring(2, jsonPath.length() - 1);
        String[] split = stripped.split("\\]\\[");
        List<PathPart> parts = new ArrayList<>(split.length);
        try {
            for (int i = 0; i < split.length; i++) {
                String s = split[i];
                if (s.startsWith("'") && s.endsWith("'")) {
                    parts.add(new Key(s.substring(1, s.length() - 1)));
                } else if ("*".equals(s)) {
                    parts.add(new IndexStar());
                } else {
                    parts.add(new Index(Integer.parseInt(s)));
                }
            }
        } catch (RuntimeException e) {
            throw new RuntimeException(e.getMessage() + " in " + jsonPath, e);
        }
        return parts;
    }

    /**
     * Just for testing.
     *
     * @param args Not used.
     */
    public static void main(final String[] args) {
        List<String> paths =
            Arrays.asList("$['expensive']", "$['store']['book'][1]['category']", "$['store']['book'][2]['isbn']",
                "$['store']['book'][*]['isbn']", "$", "$['store']['book'][1]['isbn']");
        List<Path> p = new ArrayList<>();
        for (String string : paths) {
            Path path = new Path(string);
            System.out.println(path);
            p.add(path);
        }
        Collections.sort(p);
        System.out.println(p);
    }
}
