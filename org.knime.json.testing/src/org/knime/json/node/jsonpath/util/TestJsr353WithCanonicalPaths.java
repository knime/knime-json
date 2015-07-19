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
 *   18 July 2015 (Gabor): created
 */
package org.knime.json.node.jsonpath.util;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.json.stream.JsonLocation;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.knime.core.util.Pair;

/**
 * Tests for {@link Jsr353WithCanonicalPaths}.
 *
 * @author Gabor Bakos
 */
@RunWith(Parameterized.class)
public class TestJsr353WithCanonicalPaths {
    private final String m_input;

    private Jsr353WithCanonicalPaths m_paths;

    private Map<String, Pair<Integer, Integer>> m_positions;

    /**
     * Parameters for the tests.
     *
     * @return A {@link List} of {@link String} and a {@link Map} object array. The {@link String} contains the input
     *         JSON, while the {@link Map} provides the offset values for the paths (key: path, value: {@link Pair} of
     *         {@code 0}-based offset positions of start and the position following the last character).
     */
    @Parameters
    public static List<Object[]> testCases() {
        return Arrays
            .asList(new Object[][]{
                new Object[]{"22", toMap(Arrays.asList(Pair.create("$", Pair.create(0, 2))))},
                new Object[]{"Hello world", toMap(Arrays.asList(Pair.create("$", Pair.create(0, 11))))},
                new Object[]{
                    //[3,[],{"v":[33]}]
                    "[3,[],{\"v\":[33]}]",
                    toMap(Arrays.asList(Pair.create("$", Pair.create(0, 17)), Pair.create("$[0]", Pair.create(1, 2)),
                        Pair.create("$[1]", Pair.create(2, 5)), Pair.create("$[2]", Pair.create(5, 16)),
                        Pair.create("$[2]['v']", Pair.create(10, 15)), Pair.create("$[2]['v'][0]", Pair.create(12, 14))))},
                new Object[]{"[]", toMap(Arrays.asList(Pair.create("$", Pair.create(0, 2))))},
                new Object[]{
                    //[["bb"]]
                    "[[\"bb\"]]",
                    toMap(Arrays.asList(Pair.create("$", Pair.create(0, 8)), Pair.create("$[0]", Pair.create(1, 7)),
                        Pair.create("$[0][0]", Pair.create(2, 6))))},
                new Object[]{
                    //[3,"v"]
                    "[3,\"v\"]",
                    toMap(Arrays.asList(Pair.create("$", Pair.create(0, 7)), Pair.create("$[0]", Pair.create(1, 2)),
                        Pair.create("$[1]", Pair.create(2, 6))))},
                new Object[]{
                    //[{"y":4,"v":[]}]
                    "[{\"y\":4,\"v\":[]}]",
                    toMap(Arrays.asList(Pair.create("$", Pair.create(0, 16)), Pair.create("$[0]", Pair.create(1, 15)),
                        Pair.create("$[0]['v']", Pair.create(11, 14)), Pair.create("$[0]['y']", Pair.create(5, 7))))},
                new Object[]{"[{}]",
                    toMap(Arrays.asList(Pair.create("$", Pair.create(0, 4)), Pair.create("$[0]", Pair.create(1, 3))))},
                new Object[]{"{}", toMap(Arrays.asList(Pair.create("$", Pair.create(0, 2))))},
                new Object[]{
                    //{"test":"tt","set":"s"}
                    "{\"test\":\"tt\",\"set\":\"s\"}",
                    toMap(Arrays.asList(Pair.create("$", Pair.create(0, 23)),
                        Pair.create("$['test']", Pair.create(7, 12)), Pair.create("$['set']", Pair.create(18, 22))))},
                new Object[]{
                    //{"a0":{"b":{},"b1":{}}}
                    "{\"a0\":{\"b\":{},\"b1\":{}}}",
                    toMap(Arrays.asList(Pair.create("$", Pair.create(0, 23)),
                        Pair.create("$['a0']", Pair.create(5, 22)), Pair.create("$['a0']['b']", Pair.create(10, 13)),
                        Pair.create("$['a0']['b1']", Pair.create(18, 21))))},});
    }

    private static final Map<String, Pair<Integer, Integer>>
        toMap(final List<Pair<String, Pair<Integer, Integer>>> list) {
        Map<String, Pair<Integer, Integer>> ret = new LinkedHashMap<String, Pair<Integer, Integer>>(list.size() * 2);
        for (Pair<String, Pair<Integer, Integer>> pairO : list) {
            ret.put(pairO.getFirst(), pairO.getSecond());
        }
        return ret;
    }

    /**
     * @param input The input JSON.
     * @param positions The paths to the start/end positions. ({@code 0}-based, end points after the last character.)
     */
    public TestJsr353WithCanonicalPaths(final String input, final Map<String, Pair<Integer, Integer>> positions) {
        m_input = input;
        m_positions = positions;
    }

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        m_paths = new Jsr353WithCanonicalPaths(m_input);
    }

    /**
     * Test method for {@link org.knime.json.node.jsonpath.util.Jsr353WithCanonicalPaths#get(java.lang.String)}.
     */
    @Test
    public void testGet() {
        for (Entry<String, Pair<Integer, Integer>> entry : m_positions.entrySet()) {
            Pair<JsonLocation, JsonLocation> pair = m_paths.get(entry.getKey());
            assertEquals(entry.getKey(), entry.getValue().getFirst().intValue(), pair.getFirst().getStreamOffset());
            assertEquals(entry.getKey(), entry.getValue().getSecond().intValue(), pair.getSecond().getStreamOffset());
        }
    }

    /**
     * Test method for {@link org.knime.json.node.jsonpath.util.Jsr353WithCanonicalPaths#findPath(int)}.
     */
    @Test
    public void testFindPath() {
        for (Entry<String, Pair<Integer, Integer>> entry : m_positions.entrySet()) {
            assertEquals(entry.getKey(), entry.getKey(), m_paths.findPath(entry.getValue().getFirst()));
            assertEquals(entry.getKey(), entry.getKey(), m_paths.findPath(entry.getValue().getSecond() - 1));
        }
    }

}
