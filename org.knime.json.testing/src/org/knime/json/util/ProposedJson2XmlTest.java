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
 *   9 Nov. 2014 (Gabor): created
 */
package org.knime.json.util;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.knime.json.util.Json2Xml.Json2XmlSettings;
import org.knime.json.util.Json2Xml.Options;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Tests for {@link Json2Xml}.
 *
 * @author Gabor Bakos
 */
@RunWith(Parameterized.class)
public class ProposedJson2XmlTest {
    private final String m_expected, m_inputJson;

    private Json2Xml m_converter;

    private Options[] m_options;

    /**
     * @return The expected xml as a {@link String}, the input JSON also as a {@link String} and the {@link Options}
     *         array.
     */
    @Parameters(/*name = "{index}: input:{1}"*/)
    public static List<Object[]> parameters() {
        List<Object[]> ret = new ArrayList<>();
        ret.add(new Object[]{"<root/>", "{}", new Options[]{}});
        ret.add(new Object[]{"<Array:root xmlns:Array=\"http://www.w3.org/2001/XMLSchema/list\"/>", "[]",
            new Options[]{}});
        ret.add(new Object[]{"<root><item>3</item></root>", "[3]", new Options[]{Options.looseTypeInfo}});
        ret.add(new Object[]{"<root><item>3</item></root>", "[\"3\"]", new Options[]{Options.looseTypeInfo}});
        ret.add(new Object[]{"<root><item>3</item><item>4</item></root>", "[3, 4]",
            new Options[]{Options.looseTypeInfo}});
        ret.add(new Object[]{"<root a=\"4\"/>", "{\"a\":4}", new Options[]{Options.looseTypeInfo}});
        ret.add(new Object[]{"<root a=\"4\"/>", "{\"a\":\"4\"}", new Options[]{Options.looseTypeInfo}});
        ret.add(new Object[]{"<root a=\"34\"/>", "{\"a\":\"4\", \"a\":34}", new Options[]{Options.looseTypeInfo}});
        ret.add(new Object[]{"<root><a/></root>", "{\"a\":{}}", new Options[]{Options.looseTypeInfo}});
        ret.add(new Object[]{"<root><a x=\"4\"/></root>", "{\"a\":{\"x\":4}}", new Options[]{Options.looseTypeInfo}});
        ret.add(new Object[]{"<root><a x=\"4\" y=\"&quot;44&quot;\"/></root>",
            "{\"a\":{\"x\":4,\"y\":\"\\\"44\\\"\"}}", new Options[]{Options.looseTypeInfo}});
        //This might be fishy.<a x="4"/><a x="4"/> might be a better option, but that breaks a few other tests.
        ret.add(new Object[]{"<root><a><item x=\"4\"/><item x=\"4\"/></a></root>", "{\"a\":[{\"x\":4},{\"x\":4}]}",
            new Options[]{Options.looseTypeInfo}});
        ret.add(new Object[]{"<root><a><item x=\"4\"><y v=\"2\"/></item><item x=\"0\"><y v=\"3\"/></item></a></root>",
            "{\"a\":[{\"@x\":4, \"y\":{\"v\":2}},{\"x\":0, \"y\":{\"v\":3}}]}", new Options[]{Options.looseTypeInfo}});
        ret.add(new Object[]{"<root><a><item x=\"4\"/><item x=\"4\"/></a></root>", "{\"a\":[[{\"x\":4},{\"x\":4}]]}",
            new Options[]{Options.looseTypeInfo}});
        ret.add(new Object[]{"<root><a><item x=\"4\"/><item><item x=\"4\"/></item></a></root>",
            "{\"a\":[[{\"x\":4},[{\"x\":4}]]]}", new Options[]{Options.looseTypeInfo}});
        ret.add(new Object[]{"<root><a><item><item x=\"4\"/><item><item x=\"4\"/></item></item></a></root>",
            "{\"a\":[[[{\"x\":4},[{\"x\":4}]]]]}", new Options[]{Options.looseTypeInfo}});
        ret.add(new Object[]{"<root><a x=\"4\"><y z=\"t\"/></a></root>", "{\"a\":{\"y\":{\"z\":\"t\"},\"x\":4}}",
            new Options[]{Options.looseTypeInfo}});
        ret.add(new Object[]{"<root><a><b><item>z</item><item><w/></item></b></a></root>",
            "{\"a\":{\"b\":[\"z\",{\"w\":{}}]}}", new Options[]{Options.looseTypeInfo}});
        ret.add(new Object[]{"<root><a><b>z</b><b><w/></b></a></root>", "{\"a\":{\"b\":[\"z\",{\"w\":{}}]}}",
            new Options[]{Options.looseTypeInfo, Options.UseParentKeyWhenPossible}});
        ret.add(new Object[]{"<root><item/></root>", "[{}]", new Options[]{Options.looseTypeInfo}});
        ret.add(new Object[]{"<root><a><b c=\"2\"><d t=\"text\"/><e t=\"text2\"/></b></a></root>",
            "{\"a\":{\"b\":[{\"c\":2,\"d\":{\"t\":\"text\"},\"e\":{\"t\":\"text2\"}}]}}",
            new Options[]{Options.looseTypeInfo}});
        ret.add(new Object[]{"<root><a><c>2</c>text</a></root>", "{\"a\":[{\"c\":2,\"#text\":\"text\"}]}",
            new Options[]{Options.looseTypeInfo, Options.UseParentKeyWhenPossible}});
        ret.add(new Object[]{"<root><a><b v=\"2\"><q>p</q><r>s</r></b></a></root>",
            "{\"a\":{\"b\":[{\"v\":\"2\",\"q\":{\"#text\":\"p\"},\"r\":{\"#text\":\"s\"}}]}}",
            new Options[]{Options.looseTypeInfo}});
        ret.add(new Object[]{"<root><a><b><item v=\"1\"><q>p</q></item><item v=\"2\"><q>z</q></item></b></a></root>",
            "{\"a\":{\"b\":[{\"v\":\"1\",\"q\":{\"#text\":\"p\"}},{\"v\":\"2\",\"q\":{\"#text\":\"z\"}}]}}",
            new Options[]{Options.looseTypeInfo}});
        ret.add(new Object[]{"<root><a x=\"4\"><y v=\"2\"/></a><a x=\"0\"><y v=\"3\"/></a></root>",
            "{\"a\":[{\"@x\":4, \"y\":{\"v\":2}},{\"x\":0, \"y\":{\"v\":3}}]}",
            new Options[]{Options.looseTypeInfo, Options.UseParentKeyWhenPossible}});
        ret.add(new Object[]{"<root><a x=\"4\"/><a x=\"4\"/></root>", "{\"a\":[{\"x\":4},{\"x\":4}]}",
            new Options[]{Options.looseTypeInfo, Options.UseParentKeyWhenPossible}});
        ret.add(new Object[]{"<root><a c=\"attr\"><b/></a></root>", "{\"a\":[{\"b\":{},\"c\":\"attr\"}]}",
            new Options[]{Options.looseTypeInfo, Options.UseParentKeyWhenPossible}});
        ret.add(new Object[]{"<root xmlns:Text=\"http://www.w3.org/2001/XMLSchema/string\"><a><Text:b>z</Text:b><b><w/></b></a></root>", "{\"a\":{\"b\":[\"z\",{\"w\":{}}]}}",
            new Options[]{Options.UseParentKeyWhenPossible}});
        return ret;
    }

    /**
     * @param expected
     * @param inputJson
     * @param os The options to be used.
     */
    public ProposedJson2XmlTest(final String expected, final String inputJson, final Options... os) {
        super();
        this.m_expected = expected;
        this.m_inputJson = inputJson;
        this.m_options = os;
    }

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        EnumSet<Options> set = EnumSet.noneOf(Options.class);
        set.addAll(Arrays.asList(m_options));
        if (set.contains(Options.UseParentKeyWhenPossible)) {
            m_converter = Json2Xml.createWithUseParentKeyWhenPossible(new Json2XmlSettings());
            m_converter.setLooseTypeInfo(set.contains(Options.looseTypeInfo));
        } else {
            m_converter = new Json2Xml();
            m_converter.setOptions(m_options);
        }
    }

    /**
     * Test method for {@link org.knime.json.util.Json2Xml#toXml(com.fasterxml.jackson.databind.JsonNode)}.
     *
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws JsonProcessingException
     * @throws DOMException
     * @throws TransformerException
     */
    @Test
    public void testToXml() throws DOMException, JsonProcessingException, ParserConfigurationException, IOException,
        TransformerException {
        JsonNode json = new ObjectMapper().readTree(m_inputJson);
        //System.out.println(json);
        Document doc = m_converter.toXml(json);
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));
        String output = writer.getBuffer().toString().replaceAll("\n|\r", "");
        assertEquals(m_expected, output.replaceAll("#34", "quot"));
    }

}
