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
 *   7 Nov. 2014 (Gabor): created
 */
package org.knime.json.util;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 *
 * @author Gabor Bakos
 */
@RunWith(Parameterized.class)
public class ProposedXml2JsonTest {
    private Xml2Json m_converter;

    private String m_expectedToString;

    private Document m_doc;

    @Parameters(/*name = "{index}, {0}"*/)
    public static Iterable<Object[]> testCases() {
        try {
            DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            ArrayList<Object[]> list = new ArrayList<>();
            {
                Document doc = documentBuilder.newDocument();
                Element x = doc.createElement("x");
                x.appendChild(doc.createComment("comment"));
                doc.appendChild(x);
                list.add(new Object[]{"{\"x\":{\"#comment\":\"comment\"}}", doc});
            }
            {
                Document doc = documentBuilder.newDocument();
                Element x = doc.createElement("x");
                x.setTextContent("TextContent");
                doc.appendChild(x);
                list.add(new Object[]{"{\"x\":{\"#text\":\"TextContent\"}}", doc});
            }
            {
                Document doc = documentBuilder.newDocument();
                Element x = doc.createElement("x");
                x.setAttribute("attr", "val");
                doc.appendChild(x);
                list.add(new Object[]{"{\"x\":{\"@attr\":\"val\"}}", doc});
            }
            {
                Document doc = documentBuilder.newDocument();
                Element x = doc.createElement("x");
                x.setAttribute("attr", "val");
                x.setAttribute("attr2", "val2");
                doc.appendChild(x);
                list.add(new Object[]{"{\"x\":{\"@attr\":\"val\",\"@attr2\":\"val2\"}}", doc});
            }
            {
                Document doc = documentBuilder.newDocument();
                Element x = doc.createElement("is");
                x.appendChild(doc.createElement("item"));
                x.appendChild(doc.createElement("item"));
                doc.appendChild(x);
                list.add(new Object[]{"{\"is\":{\"item\":[{},{}]}}", doc});
            }
            {
                Document doc = documentBuilder.newDocument();
                Element x = doc.createElement("is");
                x.appendChild(doc.createElement("item"));
                x.appendChild(doc.createElement("item"));
                x.appendChild(doc.createElement("separator"));
                x.appendChild(doc.createElement("item"));
                doc.appendChild(x);
                list.add(new Object[]{"{\"is\":[{\"item\":{}},{\"item\":{}},{\"separator\":{}},{\"item\":{}}]}", doc});
            }
            {
                Document doc = documentBuilder.newDocument();
                Element x = doc.createElement("is");
                Element item = doc.createElement("item");
                x.appendChild(item);
                item.appendChild(doc.createElement("x"));
                x.appendChild(doc.createElement("item"));
                x.appendChild(doc.createElement("separator"));
                x.appendChild(doc.createElement("item"));
                doc.appendChild(x);
                list.add(new Object[]{"{\"is\":[{\"item\":{\"x\":{}}},{\"item\":{}},{\"separator\":{}},{\"item\":{}}]}", doc});
            }
            {
                Document doc = documentBuilder.newDocument();
                Element x = doc.createElement("is");
                Element item = doc.createElement("item");
                x.appendChild(item);
                item.appendChild(doc.createElement("x"));
                item.appendChild(doc.createElement("y"));
                x.appendChild(doc.createElement("item"));
                x.appendChild(doc.createElement("separator"));
                x.appendChild(doc.createElement("item"));
                doc.appendChild(x);
                list.add(new Object[]{"{\"is\":[{\"item\":{\"x\":{},\"y\":{}}},{\"item\":{}},{\"separator\":{}},{\"item\":{}}]}", doc});
            }
            {
//<a a="2">q<b>r<c></c><d dd="v">x</d>y</b></a>
                Document doc = documentBuilder.newDocument();
                Element a = doc.createElement("a");
                a.setAttribute("a", "2");
                a.appendChild(doc.createTextNode("q"));
                Element b = doc.createElement("b");
                a.appendChild(b);
                b.appendChild(doc.createTextNode("r"));
                b.appendChild(doc.createElement("c"));
                Element d = doc.createElement("d");
                b.appendChild(d);
                d.setAttribute("dd", "v");
                d.appendChild(doc.createTextNode("x"));
                b.appendChild(doc.createTextNode("y"));
                doc.appendChild(a);
                list.add(new Object[]{"{\"a\":"
                    + "{\"@a\":\"2\","
                        +"\"#text\":\"q\","
                    + "\"b\":["
                    + "\"r\","
                    + "{\"c\":{}},"
                    + "{\"d\":{\"@dd\":\"v\",\"#text\":\"x\"}},"
                    + "\"y\"]}}", doc});
            }
            {
                Document doc = documentBuilder.newDocument();
                Element x = doc.createElementNS("http://example.org", "ex:x");
                x.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:ex", "http://example.org");
                x.setTextContent("TextContent");
                doc.appendChild(x);
                list.add(new Object[]{
                    "{\"ex:x\":{\"@xmlns:ex\":\"http://example.org\",\"#text\":\"TextContent\"}}", doc});
            }
            return list;
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * @param expectedToString
     * @param doc
     *
     */
    public ProposedXml2JsonTest(final String expectedToString, final Document doc) {
        this.m_expectedToString = expectedToString;
        this.m_doc = doc;
    }

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        m_converter = Xml2Json.proposedSettings().setTranslateComments(true);
    }

    /**
     * Test method for {@link org.knime.json.util.Xml2Json#toJson(org.w3c.dom.Document)}.
     */
    @Test
    public void testToJson() {
        Document doc;
        try {
            doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            if (m_doc.getDocumentElement() != null) {
                Node oldRoot = doc.adoptNode(m_doc.getDocumentElement());
                Element root = doc.createElement("root");
                root.appendChild(oldRoot);
                doc.appendChild(root);
            }
            assertEquals("input: "
                + (m_doc.getDocumentElement() == null ? null : m_doc.getDocumentElement().getTextContent()),
                m_expectedToString, m_converter.toJson(doc).toString().replaceAll("\\s", ""));
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException(e);
        }
    }
}
