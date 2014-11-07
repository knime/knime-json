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

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.xmlbeans.impl.util.Base64;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.fasterxml.jackson.databind.JsonNode;

/**
 *
 * @author Gabor Bakos
 */
public final class Json2Xml {
    /**
     *
     */
    private String m_object = "Object";

    /**
     *
     */
    private String m_array = "Array";

    /**
     *
     */
    private String m_null = "null";

    /**
     *
     */
    private String m_binary = "Binary";

    /**
     *
     */
    private String m_text = "Text";

    /**
     *
     */
    private String m_real = "Real";

    /**
     *
     */
    private String m_int = "Int";

    /**
     *
     */
    private String m_bool = "Bool";

    /**
     *
     */
    public Json2Xml() {
    }

    /**
     * Converts a {@link JsonNode} {@code node} to an XML {@link Document}.
     * TODO Exception normalization
     *
     * @param node A Jackson {@link JsonNode}.
     * @return The converted XML {@link Document}.
     * @throws ParserConfigurationException
     * @throws DOMException
     * @throws IOException
     */
    public Document toXml(final JsonNode node) throws ParserConfigurationException, DOMException, IOException {
        DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = documentBuilder.newDocument();
        doc.appendChild(doc.createElement("xml"));
        create(node, doc.getDocumentElement());
        Document ret = documentBuilder.newDocument();
        ret.appendChild(ret.adoptNode(doc.getDocumentElement().getFirstChild()));
        return ret;
    }

    /**
     * @param node
     * @param element
     * @throws IOException
     * @throws DOMException
     */
    private void create(final JsonNode node, final Element element) throws DOMException, IOException {
        if (node.isMissingNode()) {
            return;
        }
        Document doc = element.getOwnerDocument();
        if (node.isValueNode()) {
            if (node.isBoolean()) {
                element.appendChild(doc.createElement(m_bool)).setTextContent(Boolean.toString(node.asBoolean()));
            } else if (node.isIntegralNumber()) {
                element.appendChild(doc.createElement(m_int)).setTextContent(node.bigIntegerValue().toString());
            } else if (node.isFloatingPointNumber()) {
                element.appendChild(doc.createElement(m_real)).setTextContent(node.decimalValue().toString());
            } else if (node.isTextual()) {
                element.appendChild(doc.createElement(m_text)).setTextContent(node.textValue());
            } else if (node.isBinary()) {
                element.appendChild(doc.createElement(m_binary)).setTextContent(
                    new String(Base64.encode(node.binaryValue()), Charset.forName("UTF-8")));
            } else if (node.isNull()) {
                element.appendChild(doc.createElement(m_null));
            }
        } else if (node.isArray()) {
            Element array = doc.createElement(m_array);
            element.appendChild(array);
            for (JsonNode jsonNode : node) {
                createValue(jsonNode, array);
            }
        } else if (node.isObject()) {
            Element obj = doc.createElement(m_object);
            element.appendChild(obj);
            for (final Iterator<Entry<String, JsonNode>> it = node.fields(); it.hasNext();) {
                Entry<String, JsonNode> entry = it.next();
                Element newRoot = doc.createElement(entry.getKey());
                obj.appendChild(newRoot);
                createValue(entry.getValue(), newRoot);
            }
        }
    }

    private void createValue(final JsonNode node, final Element element) throws DOMException, IOException {
        if (node.isMissingNode()) {
            return;
        }
        Document doc = element.getOwnerDocument();
        if (node.isValueNode()) {
            if (node.isBoolean()) {
                element.setTextContent(Boolean.toString(node.asBoolean()));
            } else if (node.isIntegralNumber()) {
                element.setTextContent(node.bigIntegerValue().toString());
            } else if (node.isFloatingPointNumber()) {
                element.setTextContent(node.decimalValue().toString());
            } else if (node.isTextual()) {
                element.setTextContent(node.textValue());
            } else if (node.isBinary()) {
                element.setTextContent(new String(Base64.encode(node.binaryValue()), Charset.forName("UTF-8")));
            } else if (node.isNull()) {
                element.appendChild(doc.createElement(m_null));
            }
        } else if (node.isArray()) {
            for (JsonNode jsonNode : node) {
                create(jsonNode, element);
            }
        } else if (node.isObject()) {
            for (final Iterator<Entry<String, JsonNode>> it = node.fields(); it.hasNext();) {
                Entry<String, JsonNode> entry = it.next();
                Element newRoot = doc.createElement(entry.getKey());
                element.appendChild(newRoot);
                createValue(entry.getValue(), newRoot);
            }
        }
    }

    /**
     * @param object the object root name to set
     */
    public final void setObjectRoot(final String object) {
        this.m_object = object;
    }

    /**
     * @param array the array root name to set
     */
    public final void setArrayRoot(final String array) {
        this.m_array = array;
    }

    /**
     * @param nullName the null element name to set
     */
    public final void setNull(final String nullName) {
        this.m_null = nullName;
    }

    /**
     * @param binary the binary element name to set
     */
    public final void setBinary(final String binary) {
        this.m_binary = binary;
    }

    /**
     * @param text the text element name to set
     */
    public final void setText(final String text) {
        this.m_text = text;
    }

    /**
     * @param real the real element name to set
     */
    public final void setReal(final String real) {
        this.m_real = real;
    }

    /**
     * @param integer the integral element name to set
     */
    public final void setInt(final String integer) {
        this.m_int = integer;
    }

    /**
     * @param bool the bool element name to set
     */
    public final void setBool(final String bool) {
        this.m_bool = bool;
    }
}
