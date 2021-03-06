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
 *   9 Nov. 2014 (Gabor): created
 */
package org.knime.json.util;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
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
public class Json2XmlTest {
    private final String m_expected, m_inputJson;
    private Json2Xml m_converter;

    @Parameters(/*name="{index}: input:{1}"*/)
    public static List<Object[]> parameters() {
        List<Object[]> ret = new ArrayList<>();
//        ret.add(new Object[] {"<root><Object/></root>", "{}"});
        ret.add(new Object[] {"<Array:root"
            //+ " xmlns:Array=\"http://www.w3.org/2001/XMLSchema/list\""
            + "/>", "[]"});
        ret.add(new Object[] {"<root><Int:item>3</Int:item></root>", "[3]"});
        ret.add(new Object[] {"<root><Text:item>3</Text:item></root>", "[\"3\"]"});
        ret.add(new Object[] {"<root><Int:item>3</Int:item><Int:item>4</Int:item></root>", "[3, 4]"});
//        ret.add(new Object[] {"<root><Object><a>4</a></Object></root>", "{\"a\":4}"});
//        ret.add(new Object[] {"<root><Object><a>4</a></Object></root>", "{\"a\":\"4\"}"});
//        ret.add(new Object[] {"<root><Object><a>34</a></Object></root>", "{\"a\":\"4\", \"a\":34}"});
        return ret;
    }
    /**
     * @param expected
     * @param inputJson
     */
    public Json2XmlTest(final String expected, final String inputJson) {
        super();
        this.m_expected = expected;
        this.m_inputJson = inputJson;
    }

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        m_converter = new Json2Xml();
    }

    /**
     * Test method for {@link org.knime.json.util.Json2Xml#toXml(com.fasterxml.jackson.databind.JsonNode)}.
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws JsonProcessingException
     * @throws DOMException
     * @throws TransformerException
     */
    @Test
    public void testToXml() throws DOMException, JsonProcessingException, ParserConfigurationException, IOException, TransformerException {
        JsonNode json = new ObjectMapper().readTree(m_inputJson);
//        System.out.println(json);
        Document doc = m_converter.toXml(json);
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));
        String output = writer.getBuffer().toString().replaceAll("\n|\r", "");
        assertEquals(m_expected, output.replaceAll("\\s+xmlns:\\w+=\".+?\"", ""));
    }

}
