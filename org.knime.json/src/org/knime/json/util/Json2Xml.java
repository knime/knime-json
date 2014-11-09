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
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.xmlbeans.impl.util.Base64;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Helper class to convert Jackson {@link JsonNode} to XML {@link Document}s.
 *
 * @author Gabor Bakos
 * @since 2.11
 */
public final class Json2Xml {
    private String m_rootName = "root";

    private String m_primitiveArrayItem = "item";

    private String m_namespace = null;

    private boolean m_collapseToAttributes = false;

    private boolean m_looseTypeInfo = false;

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
     * Attributes of
     */
    public enum Options {
        CollapseToAttributes, looseTypeInfo;
    }

    /**
     * Converts a {@link JsonNode} {@code node} to an XML {@link Document}. TODO Exception normalization
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
        if (node.isArray() && node.size() == 0 && !m_looseTypeInfo) {
            doc.appendChild(doc.createElement(m_array == null ? m_rootName : m_array + ":" + m_rootName));
            doc.getDocumentElement().setAttribute("xmlns:" + m_array, "http://www.w3.org/2001/XMLSchema/list");
            return doc;
        }
        doc.appendChild(doc.createElement(m_rootName));
        doc.setDocumentURI(m_namespace);
        Set<JsonPrimitiveTypes> usedAttributes = create(null, node, doc.getDocumentElement());
        for (JsonPrimitiveTypes jsonPrimitiveTypes : usedAttributes) {
            switch (jsonPrimitiveTypes) {
                case BINARY:
                    doc.getDocumentElement().setAttribute("xmlns:" + m_binary,
                        "http://www.w3.org/2001/XMLSchema/binary");
                    break;
                case BOOLEAN:
                    doc.getDocumentElement()
                        .setAttribute("xmlns:" + m_bool, "http://www.w3.org/2001/XMLSchema/boolean");
                    break;
                case FLOAT:
                    doc.getDocumentElement()
                        .setAttribute("xmlns:" + m_real, "http://www.w3.org/2001/XMLSchema/decimal");
                    break;
                case INT:
                    doc.getDocumentElement().setAttribute("xmlns:" + m_int, "http://www.w3.org/2001/XMLSchema/integer");
                    break;
                case NULL:
                    doc.getDocumentElement().setAttribute("xmlns:" + m_null, "http://www.w3.org/2001/XMLSchema");
                    break;
                case TEXT:
                    doc.getDocumentElement().setAttribute("xmlns:" + m_text, "http://www.w3.org/2001/XMLSchema/string");
                    break;

                default:
                    break;
            }
        }
        //        Document ret = documentBuilder.newDocument();
        //        ret.appendChild(ret.adoptNode(doc.getDocumentElement().getFirstChild()));
        //        return ret;
        return doc;
    }

    /**
     * @param node
     * @param element
     * @return
     * @throws IOException
     * @throws DOMException
     */
    private Set<JsonPrimitiveTypes> create(final String origKey, final JsonNode node, final Element element)
        throws DOMException, IOException {
        if (node.isMissingNode()) {
            return EnumSet.noneOf(JsonPrimitiveTypes.class);
        }
        EnumSet<JsonPrimitiveTypes> ret = EnumSet.noneOf(JsonPrimitiveTypes.class);
        Document doc = element.getOwnerDocument();
        if (node.isValueNode()) {//We are inside an array, origKey should be null
            assert origKey == null : origKey;
            if (node.isBoolean()) {
                String elementName = elementName(m_bool, ret, JsonPrimitiveTypes.BOOLEAN);
                element.appendChild(doc.createElement(elementName)).setTextContent(Boolean.toString(node.asBoolean()));
            } else if (node.isIntegralNumber()) {
                String elementName = elementName(m_int, ret, JsonPrimitiveTypes.INT);
                element.appendChild(doc.createElement(elementName)).setTextContent(node.bigIntegerValue().toString());
            } else if (node.isFloatingPointNumber()) {
                String elementName = elementName(m_text, ret, JsonPrimitiveTypes.TEXT);
                element.appendChild(doc.createElement(elementName)).setTextContent(node.decimalValue().toString());
            } else if (node.isTextual()) {
                String elementName = elementName(m_text, ret, JsonPrimitiveTypes.TEXT);
                element.appendChild(doc.createElement(elementName)).setTextContent(node.textValue());
            } else if (node.isBinary()) {
                String elementName = elementName(m_binary, ret, JsonPrimitiveTypes.BINARY);
                element.appendChild(doc.createElement(elementName)).setTextContent(
                    new String(Base64.encode(node.binaryValue()), Charset.forName("UTF-8")));
            } else if (node.isNull()) {
                String elementName = elementName(m_null, ret, JsonPrimitiveTypes.NULL);
                element.appendChild(doc.createElement(elementName));
            }
        } else if (node.isArray()) {
            if (origKey == null) {
                //            Element array = doc.createElement(m_array);
                //            element.appendChild(array);
                for (JsonNode jsonNode : node) {
                    if (jsonNode.isValueNode()) {
                        ret.addAll(create(null, jsonNode, element));
                    } else {
                        Element item = doc.createElement(m_primitiveArrayItem);
                        element.appendChild(item);
                        //                ret.addAll(create(jsonNode, array));
                        ret.addAll(create(null, jsonNode, item));
                    }
                }
            } else {
                for (JsonNode jsonNode : node) {
                    if (jsonNode.isValueNode()) {
                        ret.addAll(create(null, jsonNode, element));
                    } else {
                        Element item = doc.createElement(origKey);
                        element.appendChild(item);
                        //                ret.addAll(create(jsonNode, array));
                        ret.addAll(create(null, jsonNode, item));
                    }
                }
            }
        } else if (node.isObject()) {
            if (m_collapseToAttributes) {
                if (origKey != null) {
                    Element obj = doc.createElement(origKey);
                    element.appendChild(obj);
                    for (final Iterator<Entry<String, JsonNode>> it = node.fields(); it.hasNext();) {
                        Entry<String, JsonNode> entry = it.next();
                        Element newRoot = doc.createElement(entry.getKey());
                        obj.appendChild(newRoot);
                        createValue(entry.getValue(), newRoot);
                    }
                } else {
                    handleCollapsedObject(origKey, node, element, ret);
                }
            } else {
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
        return ret;
    }

    /**
     * @param node
     * @param element
     * @param ret
     * @param doc
     * @throws IOException
     */
    private void handleCollapsedObject(final String origKey, final JsonNode node, final Element element,
        final EnumSet<JsonPrimitiveTypes> ret) throws IOException {
        Document doc = element.getOwnerDocument();
        for (final Iterator<Entry<String, JsonNode>> it = node.fields(); it.hasNext();) {
            Entry<String, JsonNode> entry = it.next();
            JsonNode v = entry.getValue();
            if (v.isValueNode()) {
                String val = v.asText();
                if (m_looseTypeInfo) {
                    element.setAttribute(entry.getKey(), val);
                } else if (v.isIntegralNumber()) {
                    ret.add(JsonPrimitiveTypes.INT);
                    element.setAttribute(m_int + ":" + entry.getKey(), val);
                } else if (v.isFloatingPointNumber()) {
                    ret.add(JsonPrimitiveTypes.FLOAT);
                    element.setAttribute(m_real + ":" + entry.getKey(), val);
                } else if (v.isTextual()) {
                    ret.add(JsonPrimitiveTypes.TEXT);
                    element.setAttribute(m_text + ":" + entry.getKey(), val);
                } else if (v.isNull()) {
                    ret.add(JsonPrimitiveTypes.NULL);
                    element.setAttribute(m_null + ":" + entry.getKey(), "");
                } else if (v.isBinary()) {
                    ret.add(JsonPrimitiveTypes.BINARY);
                    element.setAttribute(m_binary + ":" + entry.getKey(), val);
                } else if (v.isBoolean()) {
                    ret.add(JsonPrimitiveTypes.BOOLEAN);
                    element.setAttribute(m_bool + ":" + entry.getKey(), val);
                } else {
                    assert false : entry;
                }
                //            } else if (v.isArray()) {
                //                Element newRoot = doc.createElement(m_primitiveArrayItem);
                //                element.appendChild(newRoot);
                //                ret.addAll(create(v, newRoot));
            } else {
                if (origKey == null) {
                    if (entry.getValue().isArray()) {
                        ret.addAll(create(entry.getKey(), entry.getValue(), element));
                    } else {
                        Element newRoot = doc.createElement(entry.getKey());
                        element.appendChild(newRoot);
                        createValue(v, newRoot);
                    }
//                    handleCollapsedObject(entry.getKey(), entry.getValue(), element, ret);
                } else {
                    ret.addAll(create(entry.getKey(), v, element));
                }
            }
        }
    }

    /**
     * @param prefix
     * @param ret
     * @param type
     * @return
     */
    private String
        elementName(final String prefix, final EnumSet<JsonPrimitiveTypes> ret, final JsonPrimitiveTypes type) {
        if (m_looseTypeInfo) {
            return m_primitiveArrayItem;
        }
        ret.add(type);
        return prefix == null || prefix.isEmpty() ? m_primitiveArrayItem : prefix + ":" + m_primitiveArrayItem;
    }

    private Set<JsonPrimitiveTypes> createValue(final JsonNode node, final Element element) throws DOMException,
        IOException {
        if (node.isMissingNode()) {
            return EnumSet.noneOf(JsonPrimitiveTypes.class);
        }
        EnumSet<JsonPrimitiveTypes> ret = EnumSet.noneOf(JsonPrimitiveTypes.class);
        Document doc = element.getOwnerDocument();
        if (node.isValueNode()) {//Never reached when Options.CollapseToAttributes
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
                if (jsonNode.isValueNode()) {
                    ret.addAll(create(null, jsonNode, element));
                } else {
                    Element item = doc.createElement(m_primitiveArrayItem);
                    element.appendChild(item);
                    ret.addAll(create(null, jsonNode, item));
                }
            }
        } else if (node.isObject()) {
            if (m_collapseToAttributes) {
                handleCollapsedObject(null, node, element, ret);
            } else {
                for (final Iterator<Entry<String, JsonNode>> it = node.fields(); it.hasNext();) {
                    Entry<String, JsonNode> entry = it.next();
                    Element newRoot = doc.createElement(entry.getKey());
                    element.appendChild(newRoot);
                    createValue(entry.getValue(), newRoot);
                }
            }
        }
        return ret;
    }

    /**
     * @return the collapseToAttributes
     */
    public final boolean isCollapseToAttributes() {
        return m_collapseToAttributes;
    }

    /**
     * @param collapseToAttributes the collapseToAttributes to set
     */
    public final void setCollapseToAttributes(final boolean collapseToAttributes) {
        this.m_collapseToAttributes = collapseToAttributes;
    }

    /**
     * @return the looseTypeInfo
     */
    public final boolean isLooseTypeInfo() {
        return m_looseTypeInfo;
    }

    /**
     * @param looseTypeInfo the looseTypeInfo to set
     */
    public final void setLooseTypeInfo(final boolean looseTypeInfo) {
        this.m_looseTypeInfo = looseTypeInfo;
    }

    /**
     * @return the rootName
     */
    public final String getRootName() {
        return m_rootName;
    }

    /**
     * @param rootName the rootName to set
     */
    public final void setRootName(final String rootName) {
        this.m_rootName = rootName;
    }

    /**
     * @return the namespace
     */
    public final String getNamespace() {
        return m_namespace;
    }

    /**
     * @param namespace the namespace to set
     */
    public final void setNamespace(final String namespace) {
        this.m_namespace = namespace;
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

    public void setOptions(final Options... os) {
        switch (os.length) {
            case 0:
                setOptions(EnumSet.noneOf(Options.class));
                break;
            case 1:
                setOptions(EnumSet.of(os[0]));
                break;
            default:
                Options[] rest = new Options[os.length - 1];
                System.arraycopy(os, 1, rest, 0, os.length - 1);
                setOptions(EnumSet.of(os[0], rest));
                break;
        }
    }

    public void setOptions(final Set<Options> os) {
        for (Options o : Options.values()) {
            switch (o) {
                case CollapseToAttributes:
                    setCollapseToAttributes(os.contains(o));
                    break;
                case looseTypeInfo:
                    setLooseTypeInfo(os.contains(o));
                default:
                    break;
            }
        }
    }
}
