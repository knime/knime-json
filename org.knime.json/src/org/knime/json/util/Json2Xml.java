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
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.xmlbeans.impl.util.Base64;
import org.knime.core.node.util.CheckUtils;
import org.knime.json.node.util.ErrorHandling;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Helper class to convert Jackson {@link JsonNode} to XML {@link Document}s.
 *
 * @author Gabor Bakos
 */
public class Json2Xml {
    /**
     *
     */
    private static final String XMLNS_URI = "http://www.w3.org/2000/xmlns/";
    private static final String ORIGINALKEY_URI = "http://www.knime.org/json2xml/originalKey/";

    /**
     * Some settings for {@link Json2Xml}.
     */
    public static class Json2XmlSettings {
        /** The name for the root element. */
        private String m_rootName;

        /** Name for the array items. */
        private String m_primitiveArrayItem;

        /** Base namespace. */
        private String m_namespace;

        /** Prefix for arrays. */
        private String m_array;

        /** Prefix for null values. */
        private String m_null;

        /** Prefix for binary values. */
        private String m_binary;

        /** Prefix for text values. */
        private String m_text;

        /** Prefix for decimal values. */
        private String m_real;

        /** Prefix for integral values. */
        private String m_int;

        /** Prefix for boolean/logical values. */
        private String m_bool;

        /**
         * {@code null} means we do not convert any keys to text, otherwise these keys are converted to text when
         * applicable.
         */
        private String m_textKey = "#text";

        /** The prefixes for the types. */
        private Map<JsonPrimitiveTypes, String> m_prefixes = new EnumMap<>(JsonPrimitiveTypes.class);

        /**
         * Default settings for the conversion:
         * <ul>
         * <li><tt>root</tt> - root element name</li>
         * <li><tt>item</tt> - array item name</li>
         * <li>no namespace - base namespace</li>
         * <li><tt>Array</tt> - array prefix for empty list.</li>
         * <li><tt>null</tt> - prefix for null values.</li>
         * <li><tt>Binary</tt> - prefix for binary values.</li>
         * <li><tt>Text</tt> - prefix for string/text values.</li>
         * <li><tt>Int</tt> - prefix for integral values.</li>
         * <li><tt>Real</tt> - prefix for decimal/real values.</li>
         * <li><tt>Bool</tt> - prefix for boolean/logical values.</li>
         * <li><tt>#text</tt> - key, which's value should be interpreted as text.</li>
         * </ul>
         */
        public Json2XmlSettings() {
            this("root", "item", null, "Array", "null", "Binary", "Text", "Real", "Int", "Bool", "#text");
        }

        /**
         * Constructs the settings with initial values.
         *
         * @param rootName root element name
         * @param primitiveArrayItem array item name
         * @param namespace base namespace
         * @param array array prefix for empty list
         * @param nullPrefix prefix for null values
         * @param binary prefix for binary values
         * @param text prefix for string/text values
         * @param real prefix for decimal/real values
         * @param intPrefix prefix for integral values
         * @param bool prefix for boolean/logical values
         * @param textKey key, which's value should be interpreted as text
         */
        public Json2XmlSettings(final String rootName, final String primitiveArrayItem, final String namespace,
            final String array, final String nullPrefix, final String binary, final String text, final String real,
            final String intPrefix, final String bool, final String textKey) {
            this.m_rootName = rootName;
            this.m_primitiveArrayItem = primitiveArrayItem;
            this.m_namespace = namespace;
            this.m_array = array;
            this.m_null = nullPrefix;
            this.m_binary = binary;
            this.m_text = text;
            this.m_real = real;
            this.m_int = intPrefix;
            this.m_bool = bool;
            this.m_textKey = textKey;
            m_prefixes.put(JsonPrimitiveTypes.BINARY, m_binary);
            m_prefixes.put(JsonPrimitiveTypes.BOOLEAN, m_bool);
            m_prefixes.put(JsonPrimitiveTypes.FLOAT, m_real);
            m_prefixes.put(JsonPrimitiveTypes.INT, m_int);
            m_prefixes.put(JsonPrimitiveTypes.NULL, m_null);
            m_prefixes.put(JsonPrimitiveTypes.TEXT, m_text);
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
         * @param nullName the null-valued node prefix to set
         */
        public final void setNull(final String nullName) {
            this.m_null = nullName;
            m_prefixes.put(JsonPrimitiveTypes.NULL, m_null);
        }

        /**
         * @param binary the binary value node prefix to set
         */
        public final void setBinary(final String binary) {
            this.m_binary = binary;
            m_prefixes.put(JsonPrimitiveTypes.BINARY, m_binary);
        }

        /**
         * @param text the text/string node prefix to set
         */
        public final void setText(final String text) {
            this.m_text = text;
            m_prefixes.put(JsonPrimitiveTypes.TEXT, m_text);
        }

        /**
         * @param real the real/decimal node prefix to set
         */
        public final void setReal(final String real) {
            this.m_real = real;
            m_prefixes.put(JsonPrimitiveTypes.FLOAT, m_real);
        }

        /**
         * @param integer the integral node prefix to set
         */
        public final void setInt(final String integer) {
            this.m_int = integer;
            m_prefixes.put(JsonPrimitiveTypes.INT, m_int);
        }

        /**
         * @param bool the bool/logical node prefix to set
         */
        public final void setBool(final String bool) {
            this.m_bool = bool;
            m_prefixes.put(JsonPrimitiveTypes.BOOLEAN, m_bool);
        }

        /**
         * @return the textKey for values translated as XML text.
         */
        public final String getTextKey() {
            return m_textKey;
        }

        /**
         * @param textKey the textKey to set
         */
        public final void setTextKey(final String textKey) {
            this.m_textKey = textKey;
        }

        /**
         * @return the element name for array items
         */
        public String getPrimitiveArrayItem() {
            return m_primitiveArrayItem;
        }

        /**
         * @param primitiveArrayItem the element name for array items to set
         */
        public void setPrimitiveArrayItem(final String primitiveArrayItem) {
            this.m_primitiveArrayItem = primitiveArrayItem;
        }

        /**
         * @param type The type for the prefix name.
         * @return The set prefix name.
         */
        public String prefix(final JsonPrimitiveTypes type) {
            return m_prefixes.get(type);
        }

        /**
         * @param type The type for the namespace.
         * @return The namespace (default).
         */
        public String namespace(final JsonPrimitiveTypes type) {
            return type.getDefaultNamespace();
        }
    }

    /**
     * Namespace for arrays. (Used only for empty root array.)
     */
    public static final String LIST_NAMESPACE = "http://www.w3.org/2001/XMLSchema/list";

    /**
     * Namespace for textual content.
     */
    public static final String STRING_NAMESPACE = "http://www.w3.org/2001/XMLSchema/string";

    /**
     * Namespace for null.
     */
    public static final String NULL_NAMESPACE = "http://www.w3.org/2001/XMLSchema";

    /**
     * Namespace for integral content.
     */
    public static final String INTEGER_NAMESPACE = "http://www.w3.org/2001/XMLSchema/integer";

    /**
     * Namespace for decimal (floating point/real) content.
     */
    public static final String DECIMAL_NAMESPACE = "http://www.w3.org/2001/XMLSchema/decimal";

    /**
     * Namespace for boolean/logical content.
     */
    public static final String BOOLEAN_NAMESPACE = "http://www.w3.org/2001/XMLSchema/boolean";

    /**
     * Namespace for binary content.
     */
    public static final String BINARY_NAMESPACE = "http://www.w3.org/2001/XMLSchema/binary";

    /** Loose/omit type information? */
    private boolean m_looseTypeInfo = false;

    private Json2XmlSettings m_settings = new Json2XmlSettings("root", "item", null, "Array", "null", "Binary", "Text",
        "Real", "Int", "Bool", "#text");

    /**
     * Creates the {@link Json2Xml} converter with default settings. By default it keeps the type information.
     *
     * @see Json2XmlSettings#Json2XmlSettings()
     * @see #Json2Xml(Json2XmlSettings)
     */
    public Json2Xml() {
    }

    /**
     * Creates the {@link Json2Xml} with the specified {@link Json2XmlSettings} and keeping the type information. The
     * default implementation will not use the parent's key to create object when there is an array, instead it creates
     * array items. For example: a content like <code>{"a":[{"b":2},{"c":3}]}</code> will be translated to
     * {@code <a><item b="2"/><item c="3"/></a>}.
     *
     * @param settings The settings to use.
     * @see #createWithUseParentKeyWhenPossible(Json2XmlSettings)
     */
    public Json2Xml(final Json2XmlSettings settings) {
        this();
        m_settings = settings;
    }

    /**
     * Attributes of conversion.
     */
    public enum Options {
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
     * @throws ParserConfigurationException Failed to create {@link DocumentBuilder}.
     * @throws DOMException Problem with XML DOM creation.
     * @throws IOException Problem decoding binary values.
     */
    public Document toXml(final JsonNode node) throws ParserConfigurationException, DOMException, IOException {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        Document doc = documentBuilder.newDocument();
        doc.setStrictErrorChecking(false);
        EnumSet<JsonPrimitiveTypes> types = EnumSet.noneOf(JsonPrimitiveTypes.class);
        if (node.isArray() && node.size() == 0 && !isLooseTypeInfo()) {
            doc.appendChild(m_settings.m_array == null ? doc.createElement(m_settings.m_rootName) : doc
                .createElementNS(LIST_NAMESPACE, m_settings.m_array + ":" + m_settings.m_rootName));
            doc.getDocumentElement().setAttributeNS(XMLNS_URI, "xmlns:" + m_settings.m_array,
                LIST_NAMESPACE);
            setRootNamespace(doc.getDocumentElement());
            return doc;
        }
        if (node.isValueNode()) {//Special casing value nodes.
            CheckUtils.checkState(m_settings.getNamespace() == null || isLooseTypeInfo(),
                "Type information selected to be kept, but also a default namespace was specified."
                    + " Primitive values does not support this combination. Value: " + node);
            final JsonPrimitiveTypes type;
            if (node.isBoolean()) {
                type = JsonPrimitiveTypes.BOOLEAN;
            } else if (node.isIntegralNumber()) {
                type = JsonPrimitiveTypes.INT;
            } else if (node.isFloatingPointNumber()) {
                type = JsonPrimitiveTypes.FLOAT;
            } else if (node.isTextual()) {
                type = JsonPrimitiveTypes.TEXT;
            } else if (node.isBinary()) {//Not preserved information
                type = JsonPrimitiveTypes.BINARY;
            } else if (node.isNull()) {//Not supported currently
                type = JsonPrimitiveTypes.NULL;
            } else {
                throw new IllegalStateException("Unknown primitive type: " + node + " [" + node.getNodeType() + "]");
            }
            final String namespace, prefix;
            if (m_settings.getNamespace() == null) {
                if (m_looseTypeInfo) {
                    namespace = null;
                    prefix = "";
                } else {
                    namespace = m_settings.namespace(type);
                    prefix = m_settings.prefix(type) + ":";
                }
            } else {
                namespace = m_settings.getNamespace();
                prefix = "";
            }

            Element element;
            if (namespace == null) {
                element = doc.createElement(m_settings.m_rootName);
            } else {
                element = doc.createElementNS(namespace, prefix + m_settings.m_rootName);
            }
            if (!node.isNull()) {
                element.setTextContent(node.asText());
            }
            doc.appendChild(element);
            return doc;
        }
        doc.appendChild(m_settings.m_namespace == null ? createElement(doc, m_settings.m_rootName) : doc
            .createElementNS(m_settings.m_namespace, m_settings.m_rootName));
        doc.setDocumentURI(m_settings.m_namespace);
        Element root = doc.getDocumentElement();
        setRootNamespace(root);
        for (JsonPrimitiveTypes type : JsonPrimitiveTypes.values()) {
            root.setAttributeNS(XMLNS_URI, "xmlns:" + m_settings.prefix(type),
                m_settings.namespace(type));
        }
        create(null, null, node, doc.getDocumentElement(), types);
        for (JsonPrimitiveTypes type : EnumSet.complementOf(types)) {
            root.removeAttribute("xmlns:" + m_settings.prefix(type));
        }
        return doc;
    }

    /**
     * Sets the namespace attribute.
     *
     * @param root The root element.
     */
    protected void setRootNamespace(final Element root) {
        if (m_settings.m_namespace != null) {
            root.setAttributeNS(XMLNS_URI, "xmlns:", m_settings.m_namespace);
        }
    }

    /**
     * The {@code element}'s prefix becomes the type of the {@code node}'s (unless type information can be omitted). The
     * {@code types} will be updated.
     *
     * @param node A {@link JsonNode}.
     * @param element The {@link Element} to update.
     * @param types The used {@link JsonPrimitiveTypes}.
     * @return The updated {@link Element} ({@code element}).
     */
    protected Element fixValueNode(final JsonNode node, final Element element, final Set<JsonPrimitiveTypes> types) {
        if (node.isValueNode()) {
            element.setTextContent(node.asText());
            if (!m_looseTypeInfo) {
                if (node.isBinary()) {
                    element.setPrefix(m_settings.m_binary);
                    types.add(JsonPrimitiveTypes.BINARY);
                } else if (node.isIntegralNumber()) {
                    element.setPrefix(m_settings.m_int);
                    types.add(JsonPrimitiveTypes.INT);
                } else if (node.isFloatingPointNumber()) {
                    element.setPrefix(m_settings.m_real);
                    types.add(JsonPrimitiveTypes.FLOAT);
                } else if (node.isBoolean()) {
                    element.setPrefix(m_settings.m_bool);
                    types.add(JsonPrimitiveTypes.BOOLEAN);
                } else if (node.isNull()) {
                    element.setPrefix(m_settings.m_null);
                    types.add(JsonPrimitiveTypes.NULL);
                } else if (node.isTextual()) {
                    //Wrong heuristic
                    //                    if (Base64.decode(element.getTextContent().getBytes(Charset.forName("UTF-8"))) == null) {
                    element.setPrefix(m_settings.m_text);
                    types.add(JsonPrimitiveTypes.TEXT);
                    //                    } else {
                    //                        element.setPrefix(m_settings.m_binary);
                    //                        types.add(JsonPrimitiveTypes.BINARY);
                    //                    }
                }
            }
            return element;
        }
        throw new IllegalStateException("Should not reach this! " + node);
    }

    /**
     * Fixes the value {@code elem}'s prefix information.
     *
     * @param child The {@link JsonNode} to convert.
     * @param elem The result {@link Element}.
     * @param types The types used.
     */
    protected void fix(final JsonNode child, final Element elem, final Set<JsonPrimitiveTypes> types) {
        if (child.isMissingNode()) {
            return;
        }
        fixValueNode(child, elem, types);
    }

    /**
     * Creates the converted {@link Element}.
     *
     * @param origKey The parent's key that led to this call (can be {@code null}).
     * @param parent The parent node (can be {@code null}).
     * @param node The current node to convert.
     * @param parentElement The current element to transform or append attributes/children (can be {@code null}).
     * @param types The types used and should be declared as prefixes.
     * @return The created element.
     * @throws DOMException Problem with XML DOM creation.
     * @throws IOException Problem decoding binary values.
     */
    @Deprecated
    protected Element createWrong(final String origKey, final JsonNode parent, final JsonNode node,
        final Element parentElement, final Set<JsonPrimitiveTypes> types) throws DOMException, IOException {
        if (node.isMissingNode()) {
            return parentElement;
        }
        Document doc = parentElement.getOwnerDocument();
        if (origKey == null) {
//            if (parent != null && parent.isArray() && !node.isArray()) {
//            Element item = createItem(node, parentElement, true, types);
//            parentElement.appendChild(item);
//            return parentElement;
//            }
            return createNoKey(parent, node, parentElement, types);
        }
        //We have parent key, so we are within an object
        if (node.isArray()) {
            final boolean hasValue = true;//hasValue(node)/* || conflictInAttributes((ArrayNode)node)*/;
            final Element elem = createElement(doc, origKey);
            safeAdd(parentElement, elem);
            for (JsonNode jsonNode : node) {
                if (jsonNode.isObject()) {
                    if (hasValue) {
                        final Element child = createItem(jsonNode, elem, hasValue, types);
//                        if (!parentElement.hasChildNodes()) {
//                            parentElement.appendChild(elem);
//                        }
                        safeAdd(elem, child);
                        //parentElement.getFirstChild().appendChild(child);
                    } else {
                        final Element child = createNoKey(node, jsonNode, elem, types);
                        safeAdd(parentElement, elem);
//                        if (!parentElement.hasChildNodes()) {
//                            parentElement.appendChild(elem);
//                        }
//                        if (child != parentElement.getFirstChild()) {
//                            parentElement.getFirstChild().appendChild(child);
//                        }
                        safeAdd(elem, child);
                    }
                } else {
                    //Element outerItem = createElement(doc, m_settings.m_primitiveArrayItem);
                    Element item = createItem(jsonNode, /*outerItem*/elem, hasValue, types);
//                    outerItem.appendChild(item);
//                    elem.appendChild(outerItem);
                    safeAdd(elem, item);
//                    elem.appendChild(item);
                }
            }
            return parentElement;
        }
        if (node.isObject()) {
            final Element elem = createElement(doc, origKey);
            safeAdd(parentElement, elem);
//            parentElement.appendChild(elem);
            createSubObject(elem, (ObjectNode)node, types);
            return parentElement;
        }
        if (node.isValueNode()) {
            final JsonPrimitiveTypes primitiveType = valueTypeToPrimitiveType(node);
            safeAdd(parentElement, createElementWithContent(m_settings.prefix(primitiveType), origKey, primitiveType, toString(node), doc, types));
//            parentElement.setTextContent(node.asText());
            return parentElement;
        }
        throw new IllegalStateException("Should not reach this! " + node);
    }

    /**
     * Creates the converted {@link Element}.
     *
     * @param origKey The parent's key that led to this call (can be {@code null}).
     * @param parent The parent node (can be {@code null}).
     * @param node The current node to convert.
     * @param parentElement The current element to transform or append attributes/children (can be {@code null}).
     * @param types The types used and should be declared as prefixes.
     * @return The created element.
     * @throws DOMException Problem with XML DOM creation.
     * @throws IOException Problem decoding binary values.
     */
    protected Element create(final String origKey, final JsonNode parent, final JsonNode node,
        final Element parentElement, final Set<JsonPrimitiveTypes> types) throws DOMException, IOException {
        if (node.isMissingNode()) {
            return parentElement;
        }
        Document doc = parentElement.getOwnerDocument();
        if (origKey == null) {
//            if (parent != null && parent.isArray() && !node.isArray()) {
            Element item = createItem(node, parentElement, parent != null, types);
            safeAdd(parentElement, item);
            return parentElement;
//            }
//            return createNoKey(parent, node, parentElement, types);
        }
        //We have parent key, so we are within an object
        if (node.isArray()) {
            final boolean hasValue = true;//hasValue(node)/* || conflictInAttributes((ArrayNode)node)*/;
//            final Element elem = createElement(doc, origKey);
//            parentElement.appendChild(elem);
            for (JsonNode jsonNode : node) {
                if (jsonNode.isObject()) {
                    if (hasValue) {
                        final Element child = createItem(jsonNode, parentElement, hasValue, types);
//                        if (!parentElement.hasChildNodes()) {
//                            parentElement.appendChild(elem);
//                        }
                        safeAdd(parentElement, child);
                        //parentElement.getFirstChild().appendChild(child);
                    } else {
                        final Element child = createNoKey(node, jsonNode, parentElement, types);
//                        if (!parentElement.hasChildNodes()) {
//                            parentElement.appendChild(child);
//                        }
//                        if (child != parentElement.getFirstChild()) {
//                            parentElement.getFirstChild().appendChild(child);
//                        }
                        safeAdd(parentElement, child);
                    }
                } else {
                    //Element outerItem = createElement(doc, m_settings.m_primitiveArrayItem);
                    Element item = createItem(jsonNode, /*outerItem*/parentElement, hasValue, types);
//                    outerItem.appendChild(item);
//                    elem.appendChild(outerItem);
                    safeAdd(parentElement, item);
                }
            }
            return parentElement;
        }
        if (node.isObject()) {
            final Element elem = createElement(doc, origKey);
            parentElement.appendChild(elem);
            createSubObject(elem, (ObjectNode)node, types);
//            createSubObject(parentElement, (ObjectNode)node, types);
            return parentElement;
        }
        if (node.isValueNode()) {
            final JsonPrimitiveTypes primitiveType = valueTypeToPrimitiveType(node);
            safeAdd(parentElement, createElementWithContent(m_settings.prefix(primitiveType), origKey, primitiveType, toString(node), doc, types));
//            parentElement.setTextContent(node.asText());
            return parentElement;
        }
        throw new IllegalStateException("Should not reach this! " + node);
    }

    /**
     * Creates an element without parent key.
     *
     * @param parent The parent {@link JsonNode}, can be {@code null}.
     * @param node The {@link JsonNode} to convert.
     * @param parentElement The parent {@link Element} (cannot be {@code null}).
     * @param types The types used.
     * @return The converted {@link Element}.
     * @throws IOException Problem decoding binary values.
     */
    @Deprecated
    protected Element createNoKey(final JsonNode parent, final JsonNode node, final Element parentElement,
        final Set<JsonPrimitiveTypes> types) throws IOException {
        Document doc = parentElement.getOwnerDocument();
        //we are in the root, or in an array
        assert parent == null || parent.isArray() : parent;
        if (node.isValueNode()) {
            Element element = createItem(node, parentElement, false, types);
            safeAdd(parentElement, element);
            return element;
        }
        if (node.isArray()) {
            Element elem = createElement(doc, m_settings.m_primitiveArrayItem);
            safeAdd(parentElement, elem);
            boolean hasValue = hasValue(node);
            for (JsonNode child : node) {
//                if (child.isObject() || child.isArray()) {
//                    parentElement.appendChild(createItem(child, createElement(doc, m_settings.m_primitiveArrayItem),
//                        hasValue, types));
//                } else {
                    Element arrayItem = createItem(child, elem, hasValue, types);
                    safeAdd(elem, arrayItem);
//                }
            }
            return parentElement;
        }
        if (node.isObject()) {
            if (parent == null) {
                //First object
                return createObjectWithoutParent((ObjectNode)node, parentElement, types);
            }
            boolean hasValue = hasValue(parent);
            boolean conflictWithinAttributes =true; //hasValue;//parent.isArray() && !node.isArray(); //TODO hasValue || conflictInAttributes((ArrayNode)parent);
            //object within array
            return createItem(node, parentElement, conflictWithinAttributes, types);
        }
        //We already handled the missing case and object.
        assert false : node;
        throw new IllegalStateException("Should not reach this! " + node);
    }

    /**
     * Checks whether there would be a conflict (not all the same) within attributes if the attributes were moved
     * upwards.
     *
     * @param parent The paren {@link ArrayNode}.
     * @return Conflict or not.
     * @deprecated we are now explicit about the attributes.
     */
    @Deprecated
    private boolean conflictInAttributes(final ArrayNode parent) {
        assert !hasValue(parent) : parent;
        boolean ret = false;
        if (parent.size() == 0) {
            return false;
        }
        Map<String, JsonNode> values;
        Iterator<JsonNode> it = parent.iterator();
        JsonNode obj = it.next();
        assert obj.isObject();
        if (obj instanceof ObjectNode) {
            ObjectNode on = (ObjectNode)obj;
            values = possibleAttributesAndValues(on);
        } else {
            return true;
        }
        for (; it.hasNext();) {
            JsonNode next = it.next();
            if (!(next instanceof ObjectNode)) {
                return true;
            }
            Map<String, JsonNode> attributeTypes = possibleAttributesAndValues((ObjectNode)next);
            Iterator<Entry<String, JsonNode>> it1 = values.entrySet().iterator();
            Iterator<Entry<String, JsonNode>> it2 = attributeTypes.entrySet().iterator();
            for (; it1.hasNext() && it2.hasNext();) {
                Entry<String, JsonNode> next1 = it1.next(), next2 = it2.next();
                if (!Objects.equals(next1.getKey(), next2.getKey())) {
                    return true;
                }
                if (!Objects.equals(next1.getValue(), next2.getValue())) {
                    return true;
                }
            }
            if (it1.hasNext() != it2.hasNext()) {
                return true;
            }
        }
        return ret;
    }

    /**
     * @param on An {@link ObjectNode}.
     * @return The possible attributes with its values.
     */
    protected Map<String, JsonNode> possibleAttributesAndValues(final ObjectNode on) {
        Map<String, JsonNode> values = new LinkedHashMap<>();
        for (Iterator<Entry<String, JsonNode>> objIt = on.fields(); objIt.hasNext();) {
            Entry<String, JsonNode> keyValue = objIt.next();
            if (keyValue.getValue().isValueNode()) {
                values.put(keyValue.getKey(), keyValue.getValue());
            }
        }
        return values;
    }

    /**
     * @param parent A non-{@code null} {@link JsonNode} an array.
     * @return <code>true</code> iff it has at least one {@link JsonNode#isValueNode() value}.
     */
    private static boolean hasValue(final JsonNode parent) {
        boolean ret = false;
        assert parent.isArray() : parent;
        for (JsonNode child : parent) {
            ret |= child.isValueNode();
        }
        return ret;
    }

    /**
     * @param parent A non-{@code null} {@link JsonNode} (an array).
     * @return <code>true</code> iff it has at least one {@link JsonNode#isValueNode() value} or
     *         {@link JsonNode#isObject() object} child.
     */
    private static boolean hasValueOrObject(final JsonNode parent) {
        boolean ret = false;
        assert parent.isArray() : parent;
        for (JsonNode child : parent) {
            ret |= child.isValueNode() || child.isObject();
        }
        return ret;
    }

    /**
     * @param parent An {@link ObjectNode}.
     * @return whether {@code parent} contains key with {@link #getTextKey()}.
     */
    protected boolean hasText(final ObjectNode parent) {
        String textKey = getTextKey();
        boolean ret = false;
        for (Iterator<String> it = parent.fieldNames(); it.hasNext();) {
            ret |= it.next().equals(textKey);
        }
        return ret;
    }

    /**
     * Creates a sub-object.
     *
     * @param elem An {@link Element} to adjust (ex. add children or attributes).
     * @param objectNode The {@link ObjectNode} to transform.
     * @param types The types used.
     * @return {@code elem} adjusted.
     * @throws DOMException Problem with XML DOM creation.
     * @throws IOException Problem decoding binary values.
     */
    protected Element createSubObject(final Element elem, final ObjectNode objectNode,
        final Set<JsonPrimitiveTypes> types) throws DOMException, IOException {
        for (Iterator<Entry<String, JsonNode>> fields = objectNode.fields(); fields.hasNext();) {
            Entry<String, JsonNode> entry = fields.next();
            JsonNode node = entry.getValue();
//            if (node.isValueNode()) {
//                addValueAsAttribute(elem, entry, types);
//            } else if (node.isObject()) {
            if (node.isValueNode() || node.isObject()) {
                if (node.isValueNode() && (getTextKey().equals(entry.getKey()) || entry.getKey().startsWith("@"))) {
                    addValueAsAttribute(elem, entry, types);
                    continue;
                }
                final Element object = create(entry.getKey(), objectNode, node, elem, types);
                if (object == elem) {
                    continue;
                }
                safeAdd(elem, object);
            } else if (node.isArray()) {
                Document document = elem.getOwnerDocument();
                Element elemBase = document.createElement(entry.getKey());
                safeAdd(elem, elemBase);
//                elem.appendChild(elemBase);
                for (JsonNode jsonNode : node) {
                    //                    Element element = document.createElement(m_settings.m_primitiveArrayItem);
                    //                    elemBase.appendChild(element);
                    final Element created = create(null, node, jsonNode, elemBase, types);
                    if (created == elemBase) {
                        continue;
                    }
                    safeAdd(elemBase, created);
                }
            }
        }
        return elem;
    }

    /**
     * Creates the {@code node}'s {@link Element} when we have no parent.
     *
     * @param node An {@link ObjectNode} to transform.
     * @param element The {@link Element} to adjust.
     * @param types The used types.
     * @return The transformed {@code element} {@link Element}.
     * @throws DOMException Problem with XML DOM creation.
     * @throws IOException Problem decoding binary values.
     */
    protected Element createObjectWithoutParent(final ObjectNode node, final Element element,
        final Set<JsonPrimitiveTypes> types) throws DOMException, IOException {
        boolean hasTextKey = hasText(node);
        for (Iterator<Entry<String, JsonNode>> it = node.fields(); it.hasNext();) {
            Entry<String, JsonNode> entry = it.next();
            JsonNode value = entry.getValue();
            if (value.isValueNode() && !hasTextKey) {
                if (entry.getKey().startsWith("@")) {
                    addValueAsAttribute(element, entry, types);
                } else {
                    JsonPrimitiveTypes primitiveType = valueTypeToPrimitiveType(value);
                    safeAdd(element, createElementWithContent(m_settings.prefix(primitiveType), entry.getKey(), primitiveType, toString(value), element.getOwnerDocument(), types));
                }
                //create(entry.getKey(), node, value, element, types);
            } else if (hasTextKey && value.isValueNode()) {
                if (entry.getKey().equals(getTextKey())) {
                    setTextContent(element, entry, types);
                } else {
                    if (!entry.getKey().startsWith("@")) {
                        Document doc = element.getOwnerDocument();
                        JsonPrimitiveTypes primitiveType = valueTypeToPrimitiveType(value);
                        Element elem = createElementWithContent(m_settings.prefix(primitiveType), entry.getKey(), primitiveType, toString(value), doc, types);
                        safeAdd(element, elem);
                    } else {
                        addValueAsAttribute(element, entry, types);
                    }
                    //                    Document doc = element.getOwnerDocument();
                    //                    Element elem = createElement(doc, entry.getKey());
                    //                    element.appendChild(elem);
                    //                    create(entry.getKey(), node, value, elem, types);
                }
            } else if (value.isObject() || value.isArray()) {
                if (value.isArray()) {
                    Element elem = createElement(element.getOwnerDocument(), entry.getKey());
                    create(entry.getKey(), node, value, elem, types);
                    safeAdd(element, elem);
                } else {
                    create(entry.getKey(), node, value, element, types);
                }
            } else {
                assert false : value;
            }
        }
        return element;
    }

    /**
     * @param value
     * @return
     */
    private static JsonPrimitiveTypes valueTypeToPrimitiveType(final JsonNode value) {
        switch(value.getNodeType()) {
            case ARRAY: //intentional fall-through
            case OBJECT: //intentional fall-through
            case POJO: //intentional fall-through
            case MISSING:
                throw new IllegalStateException("Not a primitive value type: " + value.getNodeType() + " of "+ value);
            case NULL:
                return JsonPrimitiveTypes.NULL;
            case NUMBER:
                return value.isIntegralNumber() ? JsonPrimitiveTypes.INT : JsonPrimitiveTypes.FLOAT;
            case BINARY:
                return JsonPrimitiveTypes.BINARY;
            case BOOLEAN:
                return JsonPrimitiveTypes.BOOLEAN;
            case STRING:
                return JsonPrimitiveTypes.TEXT;
                default:
                    throw new UnsupportedOperationException("Not supported type: " + value.getNodeType() + " of " + value);
        }
    }

    /**
     * @param value
     * @return
     */
    private static String toString(final JsonNode value) {
        assert value.isValueNode(): value + " ! " + value.getNodeType();
        return value.isNull() ? "" : value.asText();
    }

    /**
     * Adds value ({@code entry}) as attribute to {@code element}.
     *
     * @param element An {@link Element}.
     * @param entry The value to add.
     * @param types The types used.
     */
    protected void addValueAsAttribute(final Element element, final Entry<String, JsonNode> entry,
        final Set<JsonPrimitiveTypes> types) {
        assert entry.getKey().startsWith("@") || getTextKey().equals(entry.getKey());
        //final String attributeName = entry.getKey().substring(1);
        final JsonNode v = entry.getValue();
        if (v.isValueNode()) {
            String val = v.asText();
            String key = entry.getKey();
            if (key.equals(getTextKey())) {
                setTextContent(element, entry, types);
                return;
            }
            key = removeInvalidChars(key);
            if (m_looseTypeInfo) {
                element.setAttribute(key, val);
            } else if (v.isIntegralNumber()) {
                types.add(JsonPrimitiveTypes.INT);
                element.setAttribute(m_settings.m_int + ":" + key, val);
            } else if (v.isFloatingPointNumber()) {
                types.add(JsonPrimitiveTypes.FLOAT);
                element.setAttribute(m_settings.m_real + ":" + key, val);
            } else if (v.isBinary()) {
                types.add(JsonPrimitiveTypes.BINARY);
                element.setAttribute(m_settings.m_binary + ":" + key, val);
            } else if (v.isTextual()) {
                //This is a wrong heuristic, for example "text" is recognized as binary
                //                if (Base64.decode(val.getBytes(Charset.forName("UTF-8"))) == null) {
                types.add(JsonPrimitiveTypes.TEXT);
                element.setAttribute(m_settings.m_text + ":" + key, val);
                //                } else {
                //                types.add(JsonPrimitiveTypes.BINARY);
                //                element.setAttribute(m_settings.m_binary + ":" + key, val);
                //                }
            } else if (v.isNull()) {
                types.add(JsonPrimitiveTypes.NULL);
                element.setAttribute(m_settings.m_null + ":" + key, "");
            } else if (v.isBoolean()) {
                types.add(JsonPrimitiveTypes.BOOLEAN);
                element.setAttribute(m_settings.m_bool + ":" + key, val);
            } else {
                assert false : entry;
            }
        } else {
            assert false : v;
        }
    }

    /**
     * Sets value as text of the XML.
     *
     * @param element The {@link Element} to adjust.
     * @param entry The value to add.
     * @param types The types used.
     */
    protected void setTextContent(final Element element, final Entry<String, JsonNode> entry,
        final Set<JsonPrimitiveTypes> types) {
        JsonNode v = entry.getValue();
        String val = v.asText();
        element.appendChild(element.getOwnerDocument().createTextNode(val));
        if (!m_looseTypeInfo) {
            if (v.isIntegralNumber()) {
                types.add(JsonPrimitiveTypes.INT);
                element.setPrefix(m_settings.m_int);
            } else if (v.isFloatingPointNumber()) {
                types.add(JsonPrimitiveTypes.FLOAT);
                element.setPrefix(m_settings.m_real);
            } else if (v.isTextual()) {
                types.add(JsonPrimitiveTypes.TEXT);
                element.setPrefix(m_settings.m_text);
            } else if (v.isNull()) {
                types.add(JsonPrimitiveTypes.NULL);
                element.setPrefix(m_settings.m_null);
            } else if (v.isBinary()) {
                types.add(JsonPrimitiveTypes.BINARY);
                element.setPrefix(m_settings.m_binary);
            } else if (v.isBoolean()) {
                types.add(JsonPrimitiveTypes.BOOLEAN);
                element.setPrefix(m_settings.m_bool);
            } else {
                assert false : entry;
            }
        }
    }

    /**
     * @param key A possible key.
     * @return The invalid (non-word) characters removed.
     */
    private static String removeInvalidChars(final String key) {
        return key.replaceAll("[^\\w]", "");
    }

    /**
     * Creates an item for an array value/object ({@code node}).
     *
     * @param node The array item to transform.
     * @param element The {@link Element} to adjust.
     * @param forceItemElement If {@code true}, an {@link #getPrimitiveArrayItem()} element will be created, else
     *            element with different key can be created.
     * @param types The types used.
     * @return The transformed {@link Element}.
     * @throws DOMException Problem with XML DOM creation.
     * @throws IOException Problem decoding binary values.
     */
    protected Element createItem(final JsonNode node, final Element element, final boolean forceItemElement,
        final Set<JsonPrimitiveTypes> types) throws DOMException, IOException {
        Document doc = element.getOwnerDocument();
        if (node.isValueNode()) {//We are inside an array, origKey should be null
            String elementName = m_settings.m_primitiveArrayItem;
            if (node.isBoolean()) {
                return createElementWithContent(m_settings.m_bool, elementName, JsonPrimitiveTypes.BOOLEAN,
                    Boolean.toString(node.asBoolean()), doc, types);
            }
            if (node.isIntegralNumber()) {
                return createElementWithContent(m_settings.m_int, elementName, JsonPrimitiveTypes.INT, node.bigIntegerValue()
                    .toString(), doc, types);
            }
            if (node.isFloatingPointNumber()) {
                return createElementWithContent(m_settings.m_real, elementName, JsonPrimitiveTypes.FLOAT,
                    Double.toString(node.asDouble()), doc, types);
            } else if (node.isBinary()) {
                return createElementWithContent(m_settings.m_binary, elementName, JsonPrimitiveTypes.BINARY,
                    new String(Base64.encode(node.binaryValue()), Charset.forName("UTF-8")), doc, types);
            } else if (node.isTextual()) {
                return createElementWithContent(m_settings.m_text, elementName, JsonPrimitiveTypes.TEXT, node.textValue(), doc,
                    types);
            } else if (node.isNull()) {
                return createElementWithContent(m_settings.m_null, elementName, JsonPrimitiveTypes.NULL, "", doc, types);
            } else {
                assert false : node;
            }
        } else if (node.isArray()) {
            if (forceItemElement) {
            Element arrayItem = createElement(doc, m_settings.m_primitiveArrayItem);
            for (JsonNode item : node) {
                Element elem;
                if (item.isArray()) {
                    elem = createItem(item, arrayItem, true, types);
                } else {
                    elem = create(null, node, item, arrayItem, types);
                }
                safeAdd(arrayItem, elem);
            }
            safeAdd(element, arrayItem);
            } else {
                for (JsonNode item : node) {
                    Element elem = create(null, node, item, element, types);
                    safeAdd(element, elem);
                }
            }
            return element;
        } else if (node.isObject()) {
            if (forceItemElement) {
                Element elem = createElement(doc, m_settings.m_primitiveArrayItem);
                return createObjectWithoutParent((ObjectNode)node, elem, types);
            }
            return createObjectWithoutParent((ObjectNode)node, element, types);
            //            Element arrayItem = doc.createElement(m_primitiveArrayItem);
            //            for (final Iterator<Entry<String, JsonNode>>it = node.fields(); it.hasNext();) {
            //                Entry<String, JsonNode> entry = it.next();
            //                arrayItem.appendChild(create(entry.getKey(), node, entry.getValue(), arrayItem, types));
            //            }
        }
        assert false : node;
        throw new IllegalStateException("Cannot reach this point: " + ErrorHandling.shorten(node.toString(), 45));
    }

    /**
     * @param parent
     * @param child
     */
    private static void safeAdd(final Element parent, final Element child) {
        if (parent!= child) {
            parent.appendChild(child);
        }
    }

    /**
     * Creates element with text content.
     *
     * @param prefix The prefix to use.
     * @param type The type of the element.
     * @param content The text content.
     * @param doc The owner document.
     * @param types The types used.
     * @return The transformed {@link Element}.
     */
    protected Element createElementWithContent(final String prefix, final String rawElementName, final JsonPrimitiveTypes type, final String content,
        final Document doc, final Set<JsonPrimitiveTypes> types) {
        String cleanName = removeInvalidChars(rawElementName);
        String elementName = elementName(prefix, cleanName, types, type);
        Element elem = doc.createElementNS(m_looseTypeInfo ? getNamespace() : type.getDefaultNamespace(), elementName);
        if (!cleanName.equals(rawElementName)) {
            elem.setAttributeNS(ORIGINALKEY_URI, "ns:originalKey", rawElementName);
        }
        elem.setTextContent(content);
        return elem;
    }

    /**
     * @param prefix The prefix to use (if we do not omit).
     * @param types The types used.
     * @param type Type of the element.
     * @return Name of the element with prefix if required.
     */
    private String elementName(final String prefix, final String rawElementName, final Set<JsonPrimitiveTypes> types, final JsonPrimitiveTypes type) {
        if (m_looseTypeInfo) {
            return rawElementName;
        }
        types.add(type);
        return prefix == null || prefix.isEmpty() ? rawElementName : prefix + ":"
            + rawElementName;
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
        return m_settings.m_rootName;
    }

    /**
     * @param rootName the rootName to set
     */
    public final void setRootName(final String rootName) {
        this.m_settings.m_rootName = rootName;
    }

    /**
     * @return the namespace
     */
    public final String getNamespace() {
        return m_settings.m_namespace;
    }

    /**
     * @param namespace the namespace to set
     */
    public final void setNamespace(final String namespace) {
        this.m_settings.m_namespace = namespace;
    }

    /**
     * @param array the array prefix name to set
     */
    public final void setArrayPrefix(final String array) {
        this.m_settings.m_array = array;
    }

    /**
     * @param nullName the null-value node prefix to set
     */
    public final void setNull(final String nullName) {
        this.m_settings.m_null = nullName;
    }

    /**
     * @param binary the binary node prefix to set
     */
    public final void setBinary(final String binary) {
        this.m_settings.m_binary = binary;
    }

    /**
     * @param text the text/string node prefix to set
     */
    public final void setText(final String text) {
        this.m_settings.m_text = text;
    }

    /**
     * @param real the real (decimal/floating point) node prefix to set
     */
    public final void setReal(final String real) {
        this.m_settings.m_real = real;
    }

    /**
     * @param integer the integral node prefix to set
     */
    public final void setInt(final String integer) {
        this.m_settings.m_int = integer;
    }

    /**
     * @param bool the bool/logical node prefix to set
     */
    public final void setBool(final String bool) {
        this.m_settings.m_bool = bool;
    }

    /**
     * Sets the boolean options in a single step.
     *
     * @param os The {@link Options}.
     */
    public void setOptions(final Options... os) {
        EnumSet<Options> options = EnumSet.noneOf(Options.class);
        for (Options option : os) {
            options.add(option);
        }
        setOptions(options);
    }

    /**
     * Sets the boolean options in a single step.
     *
     * @param os The {@link Options}.
     */
    public void setOptions(final Set<Options> os) {
        for (Options o : Options.values()) {
            switch (o) {
                case looseTypeInfo:
                    setLooseTypeInfo(os.contains(o));
                    break;
                case UseParentKeyWhenPossible:
                    if (os.contains(o)) {
                        throw new UnsupportedOperationException("Use the factory method to create such an object.");
                    }
                default:
                    break;
            }
        }
    }

    /**
     * Creates a {@link Json2Xml} object that transforms the content like <code>{"a":[{"b":2},{"c":3}]}</code> to
     * {@code <a b="2"/><a c="3"/>}. By default it keeps the type information.
     *
     * @param settings The settings to use.
     * @return The {@link Json2Xml} object with using parent's key when possible.
     */
    public static Json2Xml createWithUseParentKeyWhenPossible(final Json2XmlSettings settings) {
        return new Json2Xml(settings) {
            /**
             * {@inheritDoc}
             */
            @Override
            protected Element create(final String origKey, final JsonNode parent, final JsonNode node,
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
                    boolean hasValue = hasValueOrObject(node);
                    for (JsonNode jsonNode : node) {
                        Element elem = createElement(doc, origKey);
                        if (jsonNode.isObject()) {
                            parentElement.appendChild(//hasValue ? createItem(jsonNode, elem, hasValue, types):
                                createNoKey(node, jsonNode, elem, types));
                        } else if (jsonNode.isArray()){
                            parentElement.appendChild(hasValue || node.isArray() ? createItem(jsonNode, elem, hasValue || node.isArray(), types):
                                createNoKey(node, jsonNode, elem, types));
                        }else {
                            parentElement.appendChild(createNoKey(node, jsonNode, elem, types));
                        }
                    }
                    return parentElement;
                }
                if (node.isObject()) {
                    Element elem = createElement(doc, origKey);
                    parentElement.appendChild(elem);
                    createSubObject(elem, (ObjectNode)node, types);
                    return parentElement;
                }
                if (node.isValueNode()) {
                    parentElement.appendChild(doc.createTextNode(node.asText()));
                    return parentElement;
                }
                throw new IllegalStateException("Should not reach this! " + node);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            protected Element createSubObject(final Element elem, final ObjectNode objectNode,
                final Set<JsonPrimitiveTypes> types) throws DOMException, IOException {
                boolean hasTextKey = false;
                String textKey = getTextKey();
                for (Iterator<String> nameIt = objectNode.fieldNames(); nameIt.hasNext();) {
                    String name = nameIt.next();
                    hasTextKey |= name.equals(textKey);
                }
                for (Iterator<Entry<String, JsonNode>> fields = objectNode.fields(); fields.hasNext();) {
                    Entry<String, JsonNode> entry = fields.next();
                    JsonNode node = entry.getValue();
                    if (node.isValueNode()) {
                        if (node.isValueNode() && (getTextKey().equals(entry.getKey()) || entry.getKey().startsWith("@"))) {
                            addValueAsAttribute(elem, entry, types);
                        } else {
                            if (entry.getKey().equals(textKey)) {
                                elem.appendChild(elem.getOwnerDocument().createTextNode(node.asText()));
                            } else {
                                Element obj = createElement(elem.getOwnerDocument(), entry.getKey());
                                safeAdd(elem, obj);
                                elem.appendChild(obj);
                                Element object = create(entry.getKey(), /*elem*/objectNode, node, obj, types);
                                safeAdd(obj, object);
//                                if (object == elem) {
//                                    continue;
//                                }
//                                elem.appendChild(object);
                            }
                        }
                    } else if (node.isObject()) {
                        Element object = create(entry.getKey(), objectNode, node, elem, types);
                        if (object == elem) {
                            continue;
                        }
                        elem.appendChild(object);
                    } else if (node.isArray()) {
                        Document document = elem.getOwnerDocument();
                        for (JsonNode jsonNode : node) {
                            Element element = createElement(document, entry.getKey());
                            elem.appendChild(element);
                            if (jsonNode.isObject()) {
                                Element created = createObjectWithoutParent((ObjectNode)jsonNode, element, types);
                                if (created == element) {
                                    continue;
                                }
                            } else if (jsonNode.isArray()) {
                                //create(null, node, jsonNode, element, types);
                                createItem(jsonNode, element, true, types);
                            } else {
                                fix(jsonNode, element, types);
                            }
                        }
                    }
                }
                return elem;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            protected Element createNoKey(final JsonNode parent, final JsonNode node, final Element parentElement,
                final Set<JsonPrimitiveTypes> types) throws IOException {
                Document doc = parentElement.getOwnerDocument();
                //we are in the root, or in an array
                assert parent == null || parent.isArray() : parent;
                if (node.isValueNode()) {
                    parentElement.setTextContent(toString(node));
//                    Element element = createItem(node, parentElement, false, types);
//                    parentElement.appendChild(element);
//                    return element;
                    return parentElement;
                }
                if (node.isArray()) {
                    boolean hasValue = hasValueOrObject(node);
                    for (JsonNode child : node) {
                        if (/*child.isObject() || */child.isArray()) {
                            parentElement.appendChild(createItem(child, createElement(doc, getPrimitiveArrayItem()),
                                hasValue, types));
                        } if (child.isObject()) {
                            safeAdd(parentElement, createSubObject(parentElement, (ObjectNode)child, types));
                        } else {
                            Element arrayItem = createItem(child, parentElement, hasValue, types);
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
                    boolean hasValue = hasValueOrObject(parent);
                    //object within array
                    return createSubObject(parentElement, (ObjectNode)node, types);//createItem(node, parentElement, hasValue, types);
                }
                //We already handled the missing case and object.
                assert false : node;
                throw new IllegalStateException("Should not reach this! " + node);
            }

            @Override
            protected Element createObjectWithoutParent(final ObjectNode node, final Element element,
                final Set<JsonPrimitiveTypes> types) throws DOMException, IOException {
                boolean hasTextKey = hasText(node);
                for (Iterator<Entry<String, JsonNode>> it = node.fields(); it.hasNext();) {
                    Entry<String, JsonNode> entry = it.next();
                    JsonNode value = entry.getValue();
                    if (value.isValueNode() && !hasTextKey) {
                        if (entry.getKey().startsWith("@")) {
                            addValueAsAttribute(element, entry, types);
                        } else {
                            JsonPrimitiveTypes primitiveType = valueTypeToPrimitiveType(value);
                            safeAdd(element, createElementWithContent(settings.prefix(primitiveType), entry.getKey(), primitiveType, toString(value), element.getOwnerDocument(), types));
                        }
                        //create(entry.getKey(), node, value, element, types);
                    } else if (hasTextKey && value.isValueNode()) {
                        if (entry.getKey().equals(getTextKey())) {
                            setTextContent(element, entry, types);
                        } else {
                            if (!entry.getKey().startsWith("@")) {
                                Document doc = element.getOwnerDocument();
                                JsonPrimitiveTypes primitiveType = valueTypeToPrimitiveType(value);
                                Element elem = createElementWithContent(settings.prefix(primitiveType), entry.getKey(), primitiveType, toString(value), doc, types);
                                safeAdd(element, elem);
                            } else {
                                addValueAsAttribute(element, entry, types);
                            }
                            //                    Document doc = element.getOwnerDocument();
                            //                    Element elem = createElement(doc, entry.getKey());
                            //                    element.appendChild(elem);
                            //                    create(entry.getKey(), node, value, elem, types);
                        }
                    } else if (value.isObject() || value.isArray()) {
                        create(entry.getKey(), node, value, element, types);
                    } else {
                        assert false : value;
                    }
                }
                return element;
            }

        };
    }

    /**
     * @return The key values to be translated as text.
     */
    protected String getTextKey() {
        return m_settings.getTextKey();
    }

    /**
     * @return The element name returned for array items.
     */
    protected String getPrimitiveArrayItem() {
        return m_settings.getPrimitiveArrayItem();
    }

    /**
     * @param doc The owner document.
     * @param name The name of the {@link Element}.
     * @return The {@link Element} with proper namespace.
     */
    protected Element createElement(final Document doc, final String name) {
        String cleanName = removeInvalidChars(name);
        if (cleanName.equals(name)) {
            return doc.createElementNS(m_settings.m_namespace, name);
        }
        Element ret = doc.createElementNS(m_settings.m_namespace, cleanName);
        ret.setAttributeNS(ORIGINALKEY_URI, "ns:originalKey", name);
        return ret;
    }
}
