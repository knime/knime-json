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
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Helper class to convert Jackson {@link JsonNode} to XML {@link Document}s.
 *
 * @author Gabor Bakos
 * @since 2.11
 */
public final class Json2Xml {
    /**
     *
     */
    public static final String LIST_NAMESPACE = "http://www.w3.org/2001/XMLSchema/list";

    /**
     *
     */
    public static final String STRING_NAMESPACE = "http://www.w3.org/2001/XMLSchema/string";

    /**
     *
     */
    public static final String NULL_NAMESPACE = "http://www.w3.org/2001/XMLSchema";

    /**
     *
     */
    public static final String INTEGER_NAMESPACE = "http://www.w3.org/2001/XMLSchema/integer";

    /**
     *
     */
    public static final String DECIMAL_NAMESPACE = "http://www.w3.org/2001/XMLSchema/decimal";

    /**
     *
     */
    public static final String BOOLEAN_NAMESPACE = "http://www.w3.org/2001/XMLSchema/boolean";

    /**
     *
     */
    public static final String BINARY_NAMESPACE = "http://www.w3.org/2001/XMLSchema/binary";

    private String m_rootName = "root";

    private String m_primitiveArrayItem = "item";

    private String m_namespace = null;

    private boolean m_looseTypeInfo = false, m_useParentKey = true;

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
        /**
         * Collapse the attributes, this is the default behaviour, cannot be changed.
         */
        @Deprecated
        CollapseToAttributes,
        /**
         * Loose the type information when we just use text for all types, else we use namespaces.
         */
        looseTypeInfo,
        /** Create object with parent key for each array element if possible without loosing ordering information. */
        UseParentKeyWhenPossible;
    }

    /**
     * Converts a {@link JsonNode} {@code node} to an XML {@link Document}.
     *
     * @param node A Jackson {@link JsonNode}.
     * @return The converted XML {@link Document}.
     * @throws ParserConfigurationException
     * @throws DOMException
     * @throws IOException
     */
    //TODO Exception normalization
    public Document toXml(final JsonNode node) throws ParserConfigurationException, DOMException, IOException {
        DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = documentBuilder.newDocument();
        Set<JsonPrimitiveTypes> types = EnumSet.noneOf(JsonPrimitiveTypes.class);
        if (node.isArray() && node.size() == 0 && !m_looseTypeInfo) {
            doc.appendChild(doc.createElement(m_array == null ? m_rootName : m_array + ":" + m_rootName));
            doc.getDocumentElement().setAttribute("xmlns:" + m_array, LIST_NAMESPACE);
            //            if (m_namespace != null) {
            //                doc.getDocumentElement().setAttribute("xmlns", m_namespace);
            //            }
            return doc;
        }
        doc.appendChild(m_namespace == null ? doc.createElement(m_rootName) : doc.createElementNS(m_namespace,
            m_rootName));
        doc.setDocumentURI(m_namespace);
        //        if (m_namespace != null) {
        //            doc.getDocumentElement().setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:", m_namespace);
        //        }
        if (m_useParentKey) {
            createWithParentKey(null, null, node, doc.getDocumentElement(), types);
        } else {
            create(null, null, node, doc.getDocumentElement(), types);
        }
        for (JsonPrimitiveTypes jsonPrimitiveTypes : types) {
            switch (jsonPrimitiveTypes) {
                case BINARY:
                    doc.getDocumentElement().setAttribute("xmlns:" + m_binary, BINARY_NAMESPACE);
                    break;
                case BOOLEAN:
                    doc.getDocumentElement().setAttribute("xmlns:" + m_bool, BOOLEAN_NAMESPACE);
                    break;
                case FLOAT:
                    doc.getDocumentElement().setAttribute("xmlns:" + m_real, DECIMAL_NAMESPACE);
                    break;
                case INT:
                    doc.getDocumentElement().setAttribute("xmlns:" + m_int, INTEGER_NAMESPACE);
                    break;
                case NULL:
                    doc.getDocumentElement().setAttribute("xmlns:" + m_null, NULL_NAMESPACE);
                    break;
                case TEXT:
                    doc.getDocumentElement().setAttribute("xmlns:" + m_text, STRING_NAMESPACE);
                    break;

                default:
                    break;
            }
        }
        return doc;
    }

    /**
     * @param origKey
     * @param parent
     * @param node
     * @param parentElement
     * @param types
     */
    private Element createWithParentKey(final String origKey, final JsonNode parent, final JsonNode node, final Element parentElement,
        final Set<JsonPrimitiveTypes> types) throws DOMException, IOException {
        if (node.isMissingNode()) {
            return parentElement;
        }
        Document doc = parentElement.getOwnerDocument();
        if (origKey == null) {
            return createNoKey(parent, node, parentElement, types);
        }
        //We have parent key, so we are within an object
        if (node.isArray()) {
            for (JsonNode jsonNode : node) {
                Element elem = doc.createElement(origKey);
                if (jsonNode.isObject()) {
                    parentElement.appendChild(createNoKey(node, jsonNode, elem, types));
                } else {
                    parentElement.appendChild(elem);
                    fix(jsonNode, elem, types);
                }
            }
            return parentElement;
        }
        if (node.isObject()) {
            Element elem = doc.createElement(origKey);
            parentElement.appendChild(elem);
            createSubObject(elem, (ObjectNode)node, types);
            return parentElement;
        }
        assert m_useParentKey : node;
        return fixValueNode(node, parentElement, types);
    }

    /**
     * @param node
     * @param element
     * @param types
     * @return
     */
    protected Element
        fixValueNode(final JsonNode node, final Element element, final Set<JsonPrimitiveTypes> types) {
        if (node.isValueNode()) {
            element.setTextContent(node.asText());
            if (!m_looseTypeInfo) {
                if (node.isBinary()) {
                    element.setPrefix(m_binary);
                    types.add(JsonPrimitiveTypes.BINARY);
                } else if (node.isIntegralNumber()) {
                    element.setPrefix(m_int);
                    types.add(JsonPrimitiveTypes.INT);
                } else if (node.isFloatingPointNumber()) {
                    element.setPrefix(m_real);
                    types.add(JsonPrimitiveTypes.FLOAT);
                } else if (node.isBoolean()) {
                    element.setPrefix(m_bool);
                    types.add(JsonPrimitiveTypes.BOOLEAN);
                } else if (node.isNull()) {
                    element.setPrefix(m_null);
                    types.add(JsonPrimitiveTypes.NULL);
                } else if (node.isTextual()) {
                    element.setPrefix(m_text);
                    types.add(JsonPrimitiveTypes.TEXT);
                }
            }
            return element;
        }
        throw new IllegalStateException("Should not reach this! " + node);
    }

    /**
     * @param node
     * @param jsonNode
     * @param elem
     * @param types
     */
    private void fix(final JsonNode child, final Element elem, final Set<JsonPrimitiveTypes> types) {
        if (child.isMissingNode()) {
            return;
        }
        fixValueNode(child, elem, types);
    }

    /**
     * @param origKey The parent's key that led to this call.
     * @param parent The parent node.
     * @param node The current node to convert.
     * @param parentElement The current element to transform or append attributes/children.
     * @return The created element.
     * @throws IOException
     * @throws DOMException
     */
    private Element create(final String origKey, final JsonNode parent, final JsonNode node,
        final Element parentElement, final Set<JsonPrimitiveTypes> types) throws DOMException, IOException {
        if (node.isMissingNode()) {
            return parentElement;
        }
        Document doc = parentElement.getOwnerDocument();
        if (origKey == null) {
            return createNoKey(parent, node, parentElement, types);
        }
        //We have parent key, so we are within an object
        if (node.isArray()) {
            for (JsonNode jsonNode : node) {
                Element elem = doc.createElement(origKey);
                if (jsonNode.isObject()) {
                    parentElement.appendChild(createNoKey(node, jsonNode, elem, types));
                } else {
                    if (m_useParentKey) {
                        parentElement.appendChild(createNoKey(node, jsonNode, elem, types));
                    } else {
                        parentElement.appendChild(createItem(jsonNode, elem, types));
                    }
                }
            }
            return parentElement;
        }
        if (node.isObject()) {
            Element elem = doc.createElement(origKey);
            parentElement.appendChild(elem);
            createSubObject(elem, (ObjectNode)node, types);
            return parentElement;
        }
        assert m_useParentKey : node;
        if (node.isValueNode()) {
            parentElement.setTextContent(node.asText());
            return parentElement;
        }
        throw new IllegalStateException("Should not reach this! " + node);
    }

    /**
     * @param parent
     * @param node
     * @param parentElement
     * @param types
     * @param doc
     * @return
     * @throws IOException
     */
    protected Element createNoKey(final JsonNode parent, final JsonNode node, final Element parentElement,
        final Set<JsonPrimitiveTypes> types) throws IOException {
        Document doc = parentElement.getOwnerDocument();
        //we are in the root, or in an array
        assert parent == null || parent.isArray() : parent;
        if (node.isValueNode()) {
            Element element = createItem(node, parentElement, types);
            parentElement.appendChild(element);
            return element;
        }
        if (node.isArray()) {
            for (JsonNode child : node) {
                if (child.isObject() || child.isArray()) {
                    parentElement.appendChild(createItem(child, doc.createElement(m_primitiveArrayItem), types));
                } else {
                    Element arrayItem = createItem(child, parentElement, types);
                    parentElement.appendChild(arrayItem);
                }
            }
            return parentElement;
        }
        if (node.isObject()) {
            if (parent == null) {
                //First object
                return createObjectWithoutParent((ObjectNode)node, parentElement, types);
            }
            //object within array
            return createItem(node, parentElement, types);
        }
        //We already handled the missing case and object.
        assert false : node;
        throw new IllegalStateException("Should not reach this! " + node);
    }

    /**
     * @param elem
     * @param fields
     * @param types
     * @return
     * @throws IOException
     * @throws DOMException
     */
    private Element
        createSubObject(final Element elem, final ObjectNode objectNode, final Set<JsonPrimitiveTypes> types)
            throws DOMException, IOException {
        for (Iterator<Entry<String, JsonNode>> fields = objectNode.fields(); fields.hasNext();) {
            Entry<String, JsonNode> entry = fields.next();
            JsonNode node = entry.getValue();
            if (node.isValueNode()) {
                addValueAsAttribute(elem, entry, types);
            } else if (node.isObject()) {
                Element object = create(entry.getKey(), objectNode, node, elem, types);
                if (object == elem) {
                    continue;
                }
                elem.appendChild(object);
            } else if (node.isArray()) {
                Document document = elem.getOwnerDocument();
                if (m_useParentKey) {
                    for (JsonNode jsonNode : node) {
                        Element element = document.createElement(entry.getKey());
                        elem.appendChild(element);
                        if (jsonNode.isObject()) {
                        Element created = createObjectWithoutParent((ObjectNode)jsonNode, element, types);
                        if (created == element) {
                            continue;
                        }} else if (jsonNode.isArray()) {
                            create(null, node, jsonNode,element, types);
                        } else {
                            fix(jsonNode, element, types);
                        }
                    }
                } else {
                    Element element = document.createElement(entry.getKey());
                    elem.appendChild(element);
                    for (JsonNode jsonNode : node) {
                        final Element created = create(null, node, jsonNode, element, types);
                        if (created == element) {
                            continue;
                        }
                        element.appendChild(created);
                    }
                }
            }
        }
        return elem;
    }

    /**
     * @param node
     * @param element
     * @param types
     * @return
     * @throws IOException
     * @throws DOMException
     */
    private Element createObjectWithoutParent(final ObjectNode node, final Element element,
        final Set<JsonPrimitiveTypes> types) throws DOMException, IOException {
        for (Iterator<Entry<String, JsonNode>> it = node.fields(); it.hasNext();) {
            Entry<String, JsonNode> entry = it.next();
            if (entry.getValue().isValueNode()) {
                addValueAsAttribute(element, entry, types);
            } else if (entry.getValue().isObject()) {
                return create(entry.getKey(), node, entry.getValue(), element, types);
            } else if (entry.getValue().isArray()) {
                return create(entry.getKey(), node, entry.getValue(), element, types);
            } else {
                assert false : entry.getValue();
            }
        }
        return element;
    }

    /**
     * @param element
     * @param entry
     */
    private void addValueAsAttribute(final Element element, final Entry<String, JsonNode> entry,
        final Set<JsonPrimitiveTypes> types) {
        JsonNode v = entry.getValue();
        if (v.isValueNode()) {
            String val = v.asText();
            String key = entry.getKey();
            key = key.replaceAll("[^\\w]", "");
            if (m_looseTypeInfo) {
                element.setAttribute(key, val);
            } else if (v.isIntegralNumber()) {
                types.add(JsonPrimitiveTypes.INT);
                element.setAttribute(m_int + ":" + key, val);
            } else if (v.isFloatingPointNumber()) {
                types.add(JsonPrimitiveTypes.FLOAT);
                element.setAttribute(m_real + ":" + key, val);
            } else if (v.isTextual()) {
                types.add(JsonPrimitiveTypes.TEXT);
                element.setAttribute(m_text + ":" + key, val);
            } else if (v.isNull()) {
                types.add(JsonPrimitiveTypes.NULL);
                element.setAttribute(m_null + ":" + key, "");
            } else if (v.isBinary()) {
                types.add(JsonPrimitiveTypes.BINARY);
                //TODO should we encode?
                element.setAttribute(m_binary + ":" + key, val);
            } else if (v.isBoolean()) {
                types.add(JsonPrimitiveTypes.BOOLEAN);
                element.setAttribute(m_bool + ":" + key, val);
            } else {
                assert false : entry;
            }
        } else {
            assert false : v;
        }
    }

    /**
     * @param node
     * @param element
     * @param types
     * @return
     * @throws IOException
     * @throws DOMException
     */
    private Element createItem(final JsonNode node, final Element element, final Set<JsonPrimitiveTypes> types)
        throws DOMException, IOException {
        Document doc = element.getOwnerDocument();
        if (node.isValueNode()) {//We are inside an array, origKey should be null
            if (node.isBoolean()) {
                return createElementWithContent(m_bool, JsonPrimitiveTypes.BOOLEAN, Boolean.toString(node.asBoolean()),
                    doc, types);
            }
            if (node.isIntegralNumber()) {
                return createElementWithContent(m_int, JsonPrimitiveTypes.INT, node.bigIntegerValue().toString(), doc,
                    types);
            }
            if (node.isFloatingPointNumber()) {
                return createElementWithContent(m_real, JsonPrimitiveTypes.FLOAT, Double.toString(node.asDouble()),
                    doc, types);
            } else if (node.isTextual()) {
                return createElementWithContent(m_text, JsonPrimitiveTypes.TEXT, node.textValue(), doc, types);
            } else if (node.isBinary()) {
                return createElementWithContent(m_binary, JsonPrimitiveTypes.BINARY,
                    new String(Base64.encode(node.binaryValue()), Charset.forName("UTF-8")), doc, types);
            } else if (node.isNull()) {
                return createElementWithContent(m_null, JsonPrimitiveTypes.NULL, "", doc, types);
            } else {
                assert false : node;
            }
        } else if (node.isArray()) {
            for (JsonNode item : node) {
                Element arrayItem = doc.createElement(m_primitiveArrayItem);
                Element elem = create(null, node, item, arrayItem, types);
                element.appendChild(elem);
            }
            return element;
        } else if (node.isObject()) {
            //Element elem = doc.createElement(m_primitiveArrayItem);
            return createObjectWithoutParent((ObjectNode)node, element, types);
            //            Element arrayItem = doc.createElement(m_primitiveArrayItem);
            //            for (final Iterator<Entry<String, JsonNode>>it = node.fields(); it.hasNext();) {
            //                Entry<String, JsonNode> entry = it.next();
            //                arrayItem.appendChild(create(entry.getKey(), node, entry.getValue(), arrayItem, types));
            //            }
        }
        assert false : node;
        return null;
    }

    /**
     * @param prefix
     * @param type
     * @param content
     * @param doc
     * @param types
     * @return
     */
    private Element createElementWithContent(final String prefix, final JsonPrimitiveTypes type, final String content,
        final Document doc, final Set<JsonPrimitiveTypes> types) {
        String elementName = elementName(prefix, types, type);
        Element elem = doc.createElement(elementName);
        elem.setTextContent(content);
        return elem;
    }

    /**
     * @param prefix
     * @param ret
     * @param type
     * @return
     */
    private String elementName(final String prefix, final Set<JsonPrimitiveTypes> ret, final JsonPrimitiveTypes type) {
        if (m_looseTypeInfo) {
            return m_primitiveArrayItem;
        }
        ret.add(type);
        return prefix == null || prefix.isEmpty() ? m_primitiveArrayItem : prefix + ":" + m_primitiveArrayItem;
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
     * @return the useParentKey
     */
    public final boolean isUseParentKey() {
        return m_useParentKey;
    }

    /**
     * @param useParentKey the useParentKey to set
     */
    public final void setUseParentKey(final boolean useParentKey) {
        this.m_useParentKey = useParentKey;
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
     * @param array the array prefix name to set
     */
    public final void setArrayPrefix(final String array) {
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

    /**
     * Sets the boolean options in a single step.
     *
     * @param os The {@link Options}.
     */
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

    /**
     * Sets the boolean options in a single step.
     *
     * @param os The {@link Options}.
     */
    public void setOptions(final Set<Options> os) {
        for (Options o : Options.values()) {
            switch (o) {
                case CollapseToAttributes:
                    break;
                case looseTypeInfo:
                    setLooseTypeInfo(os.contains(o));
                case UseParentKeyWhenPossible:
                    setUseParentKey(os.contains(o));
                default:
                    break;
            }
        }
    }
}
