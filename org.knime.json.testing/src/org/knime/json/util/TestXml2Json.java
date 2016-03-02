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

/**
 *
 * @author Gabor Bakos
 */
@RunWith(Parameterized.class)
public class TestXml2Json {
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
                list.add(new Object[]{"{\"x\":[{\"__comment__\":\"comment\"}]}", doc});
            }
            {
                Document doc = documentBuilder.newDocument();
                Element x = doc.createElement("x");
                x.setTextContent("TextContent");
                doc.appendChild(x);
                list.add(new Object[]{"{\"x\":[\"TextContent\"]}", doc});
            }
            {
                Document doc = documentBuilder.newDocument();
                Element x = doc.createElement("x");
                x.setAttribute("attr", "val");
                doc.appendChild(x);
                list.add(new Object[]{"{\"x\":[[{\"attr\":\"val\"}]]}", doc});
            }
            {
                Document doc = documentBuilder.newDocument();
                Element x = doc.createElement("x");
                x.setAttribute("attr", "val");
                x.setAttribute("attr2", "val2");
                doc.appendChild(x);
                list.add(new Object[]{"{\"x\":[[{\"attr\":\"val\"},{\"attr2\":\"val2\"}]]}", doc});
            }
            {
                Document doc = documentBuilder.newDocument();
                Element x = doc.createElementNS("http://example.org", "ex:x");
                x.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:ex", "http://example.org");
                x.setTextContent("TextContent");
                doc.appendChild(x);
                list.add(new Object[]{
                    "{\"__namespace__\":\"http://example.org\",\"__namespaceRef__\":\"ex\",\"ex:x\":[["
                        + "{\"__namespace__\":\"http://www.w3.org/2000/xmlns/\",\"__namespaceRef__\":\"xmlns\",\"xmlns:ex\":\"http://example.org\"}"
                        + "],\"TextContent\"]}", doc});
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
    public TestXml2Json(final String expectedToString, final Document doc) {
        this.m_expectedToString = expectedToString;
        this.m_doc = doc;
    }

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        m_converter = new Xml2Json().setTranslateComments(true);
        m_converter.setSimpleAttributes(false);
    }

    /**
     * Test method for {@link org.knime.json.util.Xml2Json#toJson(org.w3c.dom.Document)}.
     */
    @Test
    public void testToJson() {
        assertEquals("input: "
            + (m_doc.getDocumentElement() == null ? null : m_doc.getDocumentElement().getTextContent()),
            m_expectedToString, m_converter.toJson(m_doc).toString().replaceAll("\\s", ""));
    }
}
