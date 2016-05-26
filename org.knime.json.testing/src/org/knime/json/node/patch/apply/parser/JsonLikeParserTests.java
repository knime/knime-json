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
 *   15 May 2016 (Gabor Bakos): created
 */
package org.knime.json.node.patch.apply.parser;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.knime.core.node.workflow.FlowVariable.Type;
import org.knime.core.util.Pair;
import org.knime.json.node.patch.apply.parser.JsonLikeParser.TableReferences;

import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

/**
 * Tests for {@link JsonLikeParser} (the good cases).
 *
 * @author Gabor Bakos
 */
@RunWith(Parameterized.class)
public class JsonLikeParserTests {
    private String m_input;

    private TreeNode m_jsonLike;

    /**
     * @return Test parameters. First value is the JSON as {@link String}, second is the expected value as
     *         {@link JsonNode} when {@code a} is both a String type flow variable and a column.
     */
    @Parameters(name = "{index} - {0} expected: {1}")
    public static Object[][] inputs() {
        JsonNodeFactory fact = JsonNodeFactory.instance;
        return new Object[][]{new Object[]{"{}", new ObjectNode(fact)}, //
            new Object[]{"[{}]", new ArrayNode(fact, Arrays.asList(new ObjectNode(fact)))}, //
            new Object[]{"{\"a\":\"b\"}", fact.objectNode().put("a", "b")}, //
            new Object[]{"{\"a\":$${Sa}$$}", fact.objectNode().set("a", fact.pojoNode(Pair.create("a", Type.STRING)))}, //
            new Object[]{"{\"a\":$a$}", fact.objectNode().set("a", fact.pojoNode("a"))},//
            new Object[]{"{\"a\":$$ROWINDEX$$}", fact.objectNode().set("a", fact.pojoNode(TableReferences.RowIndex))},//
            new Object[]{"{\"a\":$$ROWID$$}", fact.objectNode().set("a", fact.pojoNode(TableReferences.RowId))},//
            new Object[]{"{\"a\":$$ROWCOUNT$$}", fact.objectNode().set("a", fact.pojoNode(TableReferences.RowCount))},//
            new Object[]{"[\"a\",$a$, null]", new ArrayNode(fact, Arrays.asList(new TextNode("a"), fact.pojoNode("a"), NullNode.getInstance()))},//
            new Object[]{"[\"a\",[$a$]]", new ArrayNode(fact, Arrays.asList(new TextNode("a"), new ArrayNode(fact, Arrays.asList(fact.pojoNode("a")))))},//
            new Object[]{"{\"a\": [$a$]}", fact.objectNode().set("a", new ArrayNode(fact, Arrays.asList(fact.pojoNode("a"))))},//
            new Object[]{"$a$", fact.pojoNode("a")},//
        };
    }

    /**
     * Constructor for the test cases.
     *
     * @param input The input content.
     * @param jsonLike The expected output content.
     */
    public JsonLikeParserTests(final String input, final TreeNode jsonLike) {
        m_input = input;
        m_jsonLike = jsonLike;

    }

    /**
     * Test method for {@link com.fasterxml.jackson.core.JsonParser#readValueAsTree()}.
     *
     * @throws IOException
     */
    @Test()
    public void testReadValueAsTree() throws IOException {
        try (Reader reader = new StringReader(m_input);
                JsonLikeParser parser = new JsonLikeParser(reader, Feature.collectDefaults(),
                    Collections.singletonMap("a", Type.STRING), Collections.singleton("a"))) {
            assertEquals(m_jsonLike, parser.readValueAsTree());
        }
    }
}
