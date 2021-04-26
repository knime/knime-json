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
 *   7 Nov. 2014 (Gabor): created
 */
package org.knime.json.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.knime.core.node.util.CheckUtils;
import org.knime.json.node.util.ErrorHandling;
import org.w3c.dom.Comment;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ProcessingInstruction;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

/**
 * Helper class to convert Jackson {@link JsonNode} to XML {@link Document}s.
 *
 * @author Gabor Bakos
 */
public class Json2Xml {
    /**
     *
     */
    private static final String NS_ORIGINAL_KEY = "ns:originalKey";

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

        /** Whether {@code #comment} should be translated to comment ({@code true}) or element {@code false}. */
        private boolean m_translateHashCommentToComment = false;

        /**
         * Whether {@code <?something?>} should be translated to a processing instruction ({@code true}) or element
         * {@code false}.
         */
        private boolean m_translateQuestionPrefixToProcessingInstruction = false;

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

        /**
         * @param translateHashCommentToComment the translateHashCommentToComment to set
         * @since 3.2
         */
        public void setTranslateHashCommentToComment(final boolean translateHashCommentToComment) {
            m_translateHashCommentToComment = translateHashCommentToComment;
        }

        /**
         * @return the translateHashCommentToComment
         * @since 3.2
         */
        public boolean isTranslateHashCommentToComment() {
            return m_translateHashCommentToComment;
        }

        /**
         * @param translateQuestionPrefixToProcessingInstruction the translateQuestionPrefixToProcessingInstruction to
         *            set
         * @since 3.2
         */
        public void setTranslateQuestionPrefixToProcessingInstruction(
            final boolean translateQuestionPrefixToProcessingInstruction) {
            m_translateQuestionPrefixToProcessingInstruction = translateQuestionPrefixToProcessingInstruction;
        }

        /**
         * @return the translateQuestionPrefixToProcessingInstruction
         * @since 3.2
         */
        public boolean isTranslateQuestionPrefixToProcessingInstruction() {
            return m_translateQuestionPrefixToProcessingInstruction;
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

    private static final String HASH_COMMENT = "#comment";
    private static final String QUESTIONMARK_PREFIX = "?";

    private static final Pattern REPLACE_START_WITH_NON_LETTER_OR_UNDERSCORE = Pattern.compile("^[^a-zA-Z_]+");

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
        //Necessary because of {"a":[{"c":2,"#text":"text"}]} with type prefixes for item.
        //Though unfortunately it also makes tags like <4> possible.
        doc.setStrictErrorChecking(false);
        //The used types
        EnumSet<JsonPrimitiveTypes> types = EnumSet.noneOf(JsonPrimitiveTypes.class);
        if (node.isArray() && node.size() == 0 && !isLooseTypeInfo()) {
            specialCaseEmptyArrayWithTypes(doc);
            return doc;
        }
        if (node.isValueNode()) {//Special casing value nodes.
            specialCaseRootValueNode(node, doc);
            return doc;
        }
        //Add root element.
        doc.appendChild(m_settings.m_namespace == null ? createElement(doc, m_settings.m_rootName) : doc
            .createElementNS(m_settings.m_namespace, m_settings.m_rootName));
        doc.setDocumentURI(m_settings.m_namespace);
        Element root = doc.getDocumentElement();
        setRootNamespace(root);
        //With all possible primitive type prefices
        for (JsonPrimitiveTypes type : JsonPrimitiveTypes.values()) {
            root.setAttributeNS(XMLNS_URI, "xmlns:" + m_settings.prefix(type), m_settings.namespace(type));
        }
        create(null, null, node, doc.getDocumentElement(), types);
        //Remove unused primitive type prefices
        for (JsonPrimitiveTypes type : EnumSet.complementOf(types)) {
            root.removeAttribute("xmlns:" + m_settings.prefix(type));
        }
        return doc;
    }

    /**
     * @param node A value {@link JsonNode}.
     * @param doc The resultin {@link Document}.
     */
    private void specialCaseRootValueNode(final JsonNode node, final Document doc) {
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
        } else if (node.isNull()) {//Not supported currently at root
            type = JsonPrimitiveTypes.NULL;
        } else {
            throw new IllegalStateException("Unknown primitive type: " + node + " [" + node.getNodeType() + "]");
        }
        final String namespace;
        final String prefix;
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
    }

    /**
     * @param doc The {@link Document} to fill with the empty array with the special namespace for empty arrays.
     */
    private void specialCaseEmptyArrayWithTypes(final Document doc) {
        doc.appendChild(m_settings.m_array == null ? doc.createElement(m_settings.m_rootName) : doc.createElementNS(
            LIST_NAMESPACE, m_settings.m_array + ":" + m_settings.m_rootName));
        doc.getDocumentElement().setAttributeNS(XMLNS_URI, "xmlns:" + m_settings.m_array, LIST_NAMESPACE);
        setRootNamespace(doc.getDocumentElement());
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
                    types.add(JsonPrimitiveTypes.BINARY);
                    return copyElementWithPrefix(element, m_settings.m_binary, BINARY_NAMESPACE);
                } else if (node.isIntegralNumber()) {
                    types.add(JsonPrimitiveTypes.INT);
                    return copyElementWithPrefix(element, m_settings.m_int, INTEGER_NAMESPACE);
                } else if (node.isFloatingPointNumber()) {
                    types.add(JsonPrimitiveTypes.FLOAT);
                    return copyElementWithPrefix(element, m_settings.m_real, DECIMAL_NAMESPACE);
                } else if (node.isBoolean()) {
                    types.add(JsonPrimitiveTypes.BOOLEAN);
                    return copyElementWithPrefix(element, m_settings.m_bool, BOOLEAN_NAMESPACE);
                } else if (node.isNull()) {
                    types.add(JsonPrimitiveTypes.NULL);
                    return copyElementWithPrefix(element, m_settings.m_null, NULL_NAMESPACE);
                } else if (node.isTextual()) {
                    //Wrong heuristic
                    //                    if (Base64.decode(element.getTextContent().getBytes(Charset.forName("UTF-8"))) == null) {
                    types.add(JsonPrimitiveTypes.TEXT);
                    return copyElementWithPrefix(element, m_settings.m_text, STRING_NAMESPACE);
                    //                    } else {
                    //                        types.add(JsonPrimitiveTypes.BINARY);
                    //                        return copyElementWithPrefix(element, m_settings.m_binary, BINARY_NAMESPACE);
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
     * @param origKey The parent's key that led to this call (can be {@code null} when we are in an array).
     * @param parent The parent node (can be {@code null}).
     * @param node The current node to convert.
     * @param parentElement The current element to transform or append attributes/children (can be {@code null}).
     * @param types The types used and should be declared as prefixes.
     * @return The created element (always {@code parentElement}).
     * @throws DOMException Problem with XML DOM creation.
     * @throws IOException Problem decoding binary values.
     */
    protected Element create(final String origKey, final JsonNode parent, final JsonNode node,
        final Element parentElement, final Set<JsonPrimitiveTypes> types) throws DOMException, IOException {
        if (node.isMissingNode()) {
            return parentElement;
        }
        Document doc = parentElement.getOwnerDocument();
        if (origKey == null) {//we are in an array
            return createNoKey(parent, node, parentElement, types);
        }
        //We have parent key, so we are within an object
        if (node.isArray()) {
            return arrayWithinObject(origKey, node, parentElement, types);
        }
        if (node.isObject()) {
            final Element elem = createElement(doc, origKey);
            parentElement.appendChild(elem);
            createSubObject(elem, (ObjectNode)node, types);
            return parentElement;
        }
        if (node.isValueNode()) {
            final JsonPrimitiveTypes primitiveType = valueTypeToPrimitiveType(node);
            safeAdd(
                parentElement,
                createElementWithContent(m_settings.prefix(primitiveType), origKey, primitiveType, valueToString(node),
                    doc, types));
            return parentElement;
        }
        throw new IllegalStateException("Should not reach this! " + node);
    }

    /**
     * Creates the item when no key is present from the parent node.
     *
     * @param parent The parent {@link JsonNode}.
     * @param node The current {@link JsonNode}.
     * @param parentElement The parent result {@link Element}.
     * @param types The used types.
     * @return Usually {@code parentElement} with filled content, though in case of
     *         {@link Options#UseParentKeyWhenPossible}, when the {@code node} is a value, it should be recreated.
     * @throws IOException Problem decoding binary from Base64 encoded {@link String}.
     */
    protected Element createNoKey(final JsonNode parent, final JsonNode node, final Element parentElement,
        final Set<JsonPrimitiveTypes> types) throws IOException {
        Element item = createItem(node, parentElement, parent != null, types);
        safeAdd(parentElement, item);
        return parentElement;
    }

    /**
     * Convert the {@code node} to an {@link Element} when {@code node} is an array within an object. For example for
     * the <code>{"a":[2]}</code>, we would get {@code origKey} as {@code a} and the {@code node} as {@code [2]}.
     *
     * @param origKey The original key, though it is not used unless {@link Options#UseParentKeyWhenPossible using
     *            parent key}.
     * @param node An array {@link JsonNode}.
     * @param parentElement The parent {@link Element} (for the object).
     * @param types The used types.
     * @return {@code parentElement}.
     * @throws IOException Problem decoding Base64 encoded value.
     * @since 3.0
     */
    protected Element arrayWithinObject(final String origKey, final JsonNode node, final Element parentElement,
        final Set<JsonPrimitiveTypes> types) throws IOException {
        for (final JsonNode jsonNode : node) {
            final Element child = createItem(jsonNode, parentElement, true, types);
            safeAdd(parentElement, child);
        }
        return parentElement;
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
        Element currentElement = elem;
        final Document doc = elem.getOwnerDocument();
        for (final Iterator<Entry<String, JsonNode>> fields = objectNode.fields(); fields.hasNext();) {
            final Entry<String, JsonNode> entry = fields.next();
            final JsonNode node = entry.getValue();
            final JsonNode value = entry.getValue();
            if (m_settings.m_translateHashCommentToComment && HASH_COMMENT.equals(entry.getKey())) {
                handleComment(currentElement, doc, value);
            } else if (m_settings.m_translateQuestionPrefixToProcessingInstruction
                && entry.getKey().startsWith(QUESTIONMARK_PREFIX)) {
                handleProcessingInstruction(currentElement, doc, entry);
            } else if (node.isValueNode() && (getTextKey().equals(entry.getKey()) || entry.getKey().startsWith("@"))) {
                //primitive text or attribute
                currentElement = addValueAsAttribute(currentElement, entry, types);
            } else if (node.isValueNode() || node.isObject()) {
                //In case node is not a value, but the key is still the text key, it is handled like other keys:
                //it might get originalKey attribute for the new Element or just becomes an simple Element.
                final Element object = create(entry.getKey(), objectNode, node, currentElement, types);
                safeAdd(currentElement, object);
            } else if (node.isArray()) {
                handeArraysInSubObjects(currentElement, entry, types);
            }
        }
        return currentElement;
    }

    /**
     * @param currentElement An {@link Element}.
     * @param doc The {@link Document} of the resulting XML.
     * @param entry An entry for the processing instruction (key should start with {@value #QUESTIONMARK_PREFIX}).
     */
    private void handleProcessingInstruction(final Element currentElement, final Document doc,
        final Entry<String, JsonNode> entry) {
        final String target = entry.getKey().substring(1);
        if (target.matches("[a-zA-Z][\\w\\-]*") && !"xml".equalsIgnoreCase(target)) {
            final String string = string(entry.getValue());
            int firstQuestionMarkGreater = string.indexOf("?>");
            if (firstQuestionMarkGreater >= 0) {
                final String value = string.substring(0, firstQuestionMarkGreater);
                final String rest = string.substring(firstQuestionMarkGreater);
                safeAdd(currentElement, doc.createProcessingInstruction(entry.getKey().substring(1), value));
                safeAdd(currentElement, doc.createTextNode(rest));
            } else {
                safeAdd(currentElement, doc.createProcessingInstruction(entry.getKey().substring(1), string));
            }
        } else {
            throw new IllegalStateException("Invalid processing instruction target: " + target);
        }
    }

    /**
     * @param currentElement An {@link Element}.
     * @param doc The {@link Document} of the resulting XML.
     * @param value The value to be used as comment.
     */
    private void handleComment(final Element currentElement, final Document doc, final JsonNode value) {
        final String v = string(value);
        if (v.contains("--")) {
            throw new IllegalStateException("-- cannot be in XML comments.");
        }
        safeAdd(currentElement, doc.createComment(string(value)));
    }

    /**
     * @param value A {@link JsonNode} (might be non-value)
     * @return The {@link String} representation of it.
     */
    private String string(final JsonNode value) {
        return value instanceof TextNode ? value.asText() : value.toString();
    }

    /**
     * Fills the content of the {@code parent} with an array with the specified key. For example see the following
     * input: <code>{"a":{"b":["z"]}}</code>, in this case we have {@code b} as the key for {@code entry} and
     * {@code ["z"]} for the value. The {@code parent} is the {@link Element} created for {@code a}.
     *
     * @param parent The parent {@link Element}.
     * @param entry The key-value for the parent object within {@link #createSubObject(Element, ObjectNode, Set)}. Its
     *            {@link Entry#getValue() value} is an {@link ArrayNode}.
     * @param types The used types.
     * @throws IOException Problem with Base64 encoding.
     * @since 3.0
     */
    protected void handeArraysInSubObjects(final Element parent, final Entry<String, JsonNode> entry,
        final Set<JsonPrimitiveTypes> types) throws IOException {
        final Element elemBase = createElement(parent.getOwnerDocument(), entry.getKey());
        safeAdd(parent, elemBase);
        for (final JsonNode jsonNode : entry.getValue()) {
            safeAdd(elemBase, create(null, entry.getValue(), jsonNode, elemBase, types));
        }
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
     * @since 3.0
     */
    protected Element createObjectWithoutParent(final ObjectNode node, final Element element,
        final Set<JsonPrimitiveTypes> types) throws DOMException, IOException {
        final boolean hasTextKey = hasText(node);
        final Document doc = element.getOwnerDocument();
        Element currentElement = element;
        for (final Iterator<Entry<String, JsonNode>> it = node.fields(); it.hasNext();) {
            final Entry<String, JsonNode> entry = it.next();
            final JsonNode value = entry.getValue();
            if (m_settings.m_translateHashCommentToComment && HASH_COMMENT.equals(entry.getKey())) {
                handleComment(currentElement, doc, value);
            } else if (m_settings.m_translateQuestionPrefixToProcessingInstruction && entry.getKey().startsWith(QUESTIONMARK_PREFIX)) {
                handleProcessingInstruction(currentElement, doc, entry);
            } else if (value.isValueNode() && !hasTextKey) {
                if (entry.getKey().startsWith("@")) {
                    currentElement = addValueAsAttribute(currentElement, entry, types);
                } else {
                    final JsonPrimitiveTypes primitiveType = valueTypeToPrimitiveType(value);
                    safeAdd(
                        currentElement,
                        createElementWithContent(m_settings.prefix(primitiveType), entry.getKey(), primitiveType,
                            valueToString(value), doc, types));
                }
            } else if (hasTextKey && value.isValueNode()) {
                if (entry.getKey().equals(getTextKey())) {
                    currentElement = setTextContent(currentElement, entry, types);
                } else {
                    if (entry.getKey().startsWith("@")) {
                        currentElement = addValueAsAttribute(currentElement, entry, types);
                    } else {
                        JsonPrimitiveTypes primitiveType = valueTypeToPrimitiveType(value);
                        Element elem =
                            createElementWithContent(m_settings.prefix(primitiveType), entry.getKey(), primitiveType,
                                valueToString(value), doc, types);
                        safeAdd(currentElement, elem);
                    }
                }
            } else if (value.isArray()) {
                arrayWithoutParentKey(entry, node, currentElement, types);
            } else if (value.isObject()) {
                create(entry.getKey(), node, value, currentElement, types);
            } else {
                assert false : value;
            }
        }
        return currentElement;
    }

    /**
     * Handles the case within {@link #createObjectWithoutParent(ObjectNode, Element, Set)} when the value within the
     * object is an array.
     *
     * @param entry The entry for the array (value) node and its key.
     * @param node The parent node.
     * @param element The parent element.
     * @param types The used types.
     * @throws IOException Base64 decoding was unsuccessful.
     * @since 3.0
     */
    protected void arrayWithoutParentKey(final Entry<String, JsonNode> entry, final ObjectNode node,
        final Element element, final Set<JsonPrimitiveTypes> types) throws IOException {
        final Element elem = createElement(element.getOwnerDocument(), entry.getKey());
        create(entry.getKey(), node, entry.getValue(), elem, types);
        safeAdd(element, elem);
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
        final Document doc = element.getOwnerDocument();
        if (node.isValueNode()) {//We are inside an array, origKey should be null
            final String elementName = m_settings.m_primitiveArrayItem;
            if (node.isBoolean()) {
                return createElementWithContent(m_settings.m_bool, elementName, JsonPrimitiveTypes.BOOLEAN,
                    Boolean.toString(node.asBoolean()), doc, types);
            }
            if (node.isIntegralNumber()) {
                return createElementWithContent(m_settings.m_int, elementName, JsonPrimitiveTypes.INT, node
                    .bigIntegerValue().toString(), doc, types);
            }
            if (node.isFloatingPointNumber()) {
                return createElementWithContent(m_settings.m_real, elementName, JsonPrimitiveTypes.FLOAT,
                    Double.toString(node.asDouble()), doc, types);
            } else if (node.isBinary()) {
                return createElementWithContent(m_settings.m_binary, elementName, JsonPrimitiveTypes.BINARY,
                    new String(Base64.getEncoder().encode(node.binaryValue()), StandardCharsets.UTF_8), doc, types);
            } else if (node.isTextual()) {
                return createElementWithContent(m_settings.m_text, elementName, JsonPrimitiveTypes.TEXT,
                    node.textValue(), doc, types);
            } else if (node.isNull()) {
                return createElementWithContent(m_settings.m_null, elementName, JsonPrimitiveTypes.NULL, "", doc, types);
            } else {
                assert false : node;
            }
        } else if (node.isArray()) {
            if (forceItemElement) {
                final Element arrayItem = createElement(doc, m_settings.m_primitiveArrayItem);
                final boolean hasValue = hasValue(node);
                for (final JsonNode item : node) {
                    final Element elem;
                    if (item.isArray() || hasValue || hasAttribute(item)) {
                        elem = createItem(item, arrayItem, true, types);
                    } else {
                        elem = create(null, node, item, arrayItem, types);
                    }
                    safeAdd(arrayItem, elem);
                }
                safeAdd(element, arrayItem);
            } else {
                for (final JsonNode item : node) {
                    final Element elem = create(null, node, item, element, types);
                    safeAdd(element, elem);
                }
            }
            return element;
        } else if (node.isObject()) {
            if (forceItemElement) {
                final Element elem = createElement(doc, m_settings.m_primitiveArrayItem);
                //safeAdd(element, elem);
                return createObjectWithoutParent((ObjectNode)node, elem, types);
            }
            return createObjectWithoutParent((ObjectNode)node, element, types);
        }
        assert false : node;
        throw new IllegalStateException("Cannot reach this point: " + ErrorHandling.shorten(node.toString(), 45));
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
             * {@inheritDoc} This one is simpler than the overridden, as we do not have to create an additional element
             * in this case.
             */
            @Override
            protected void arrayWithoutParentKey(final Entry<String, JsonNode> entry, final ObjectNode node,
                final Element element, final Set<JsonPrimitiveTypes> types) throws IOException {
                create(entry.getKey(), node, entry.getValue(), element, types);
            }

            /**
             * {@inheritDoc} This is more complex than the overridden method as we do not always generate the "item"
             * element (not: for values and objects).
             */
            @Override
            protected Element arrayWithinObject(final String origKey, final JsonNode node, final Element parentElement,
                final Set<JsonPrimitiveTypes> types) throws IOException {
                assert node.isArray();
                final Document doc = parentElement.getOwnerDocument();
                for (final JsonNode jsonNode : node) {
                    final Element elem = createElement(doc, origKey);
                    if (jsonNode.isArray()) {
                        parentElement.appendChild(createItem(jsonNode, elem, true, types));
                    } else {
                        parentElement.appendChild(createNoKey(node, jsonNode, elem, types));
                    }
                }
                return parentElement;
            }

            /**
             * {@inheritDoc} The difference to the overridden method that this tries to create new elements with the
             * parent key when possible.
             */
            @Override
            protected void handeArraysInSubObjects(final Element parent, final Entry<String, JsonNode> entry,
                final Set<JsonPrimitiveTypes> types) throws IOException {
                final Document doc = parent.getOwnerDocument();
                //create elements with the key
                for (final JsonNode jsonNode : entry.getValue()) {
                    final Element element = createElement(doc, entry.getKey());
                    parent.appendChild(element);
                    if (jsonNode.isObject()) {
                        createObjectWithoutParent((ObjectNode)jsonNode, element, types);
                    } else if (jsonNode.isArray()) {
                        createItem(jsonNode, element, true, types);
                    } else {
                        //Fix values.
                        fix(jsonNode, element, types);
                    }
                }
            }

            /**
             * Creates an element without parent key. This is more complex than the overridden method, as we do not want
             * to always create "item" elements.
             *
             * @param parent The parent {@link JsonNode}, can be {@code null}.
             * @param node The {@link JsonNode} to convert.
             * @param parentElement The parent {@link Element} (cannot be {@code null}).
             * @param types The types used.
             * @return The converted {@link Element}.
             * @throws IOException Problem decoding binary values.
             */
            @Override
            protected Element createNoKey(final JsonNode parent, final JsonNode node, final Element parentElement,
                final Set<JsonPrimitiveTypes> types) throws IOException {
                final Document doc = parentElement.getOwnerDocument();
                //we are in the root, or in an array
                assert parent == null || parent.isArray() : parent;
                if (node.isValueNode()) {//parent object was already created, we have to recreate it.
                    final JsonPrimitiveTypes primitiveType = valueTypeToPrimitiveType(node);
                    final String origName = parentElement.getAttribute(NS_ORIGINAL_KEY);
                    return createElementWithContent(settings.prefix(primitiveType),
                        parentElement.hasAttribute(NS_ORIGINAL_KEY) ? origName : parentElement.getNodeName(),
                        primitiveType, valueToString(node), doc, types);
                }
                if (node.isArray()) {
                    for (final JsonNode child : node) {
                        if (child.isObject() || child.isArray()) {
                            //This is a bit tricky, as it might create unexpected results:
                            //[[],[]] becomes <item/><item/>, though
                            //[[2],[3]] becomes <item><item>2</item></item><item><item>3</item></item>
                            safeAdd(
                                parentElement,
                                createItem(child, /*createElement(doc, getPrimitiveArrayItem())*/parentElement, true,
                                    types));
                        } else {
                            final Element arrayItem = createItem(child, parentElement, true, types);
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
                    return createSubObject(parentElement, (ObjectNode)node, types);//createItem(node, parentElement, hasValue, types);
                }
                //We already handled the missing case and object.
                assert false : node;
                throw new IllegalStateException("Should not reach this! " + node);
            }
        };
    }

    /**
     * Adds {@code child} as a child to {@code parent}, unless those refer to the same {@link Element}.
     *
     * @param parent An {@link Element}.
     * @param child An {@link Element}, {@link Comment} or {@link ProcessingInstruction}.
     */
    private static void safeAdd(final Element parent, final Node child) {
        if (parent != child) {
            parent.appendChild(child);
        }
    }

    /**
     * @param key A possible key.
     * @return The invalid (non-word) characters removed.
     */
    private static String removeInvalidChars(final String key) {
        final String validChars = key.replaceAll("[^\\w\\-.]", "");
        return REPLACE_START_WITH_NON_LETTER_OR_UNDERSCORE.matcher(validChars).replaceFirst("");
    }

    /**
     * @param value A value {@link JsonNode}.
     * @return The {@link String} representation of {@code value}.
     */
    private static String valueToString(final JsonNode value) {
        assert value.isValueNode() : value + " ! " + value.getNodeType();
        return value.isNull() ? "" : value.asText();
    }

    /**
     * @param value A {@link JsonNode}, expected to be a {@link JsonNode#isValueNode() value node}.
     * @return The {@link JsonPrimitiveTypes} that belongs to {@code value}.
     * @throws IllegalStateException When it is not a value node.
     */
    private static JsonPrimitiveTypes valueTypeToPrimitiveType(final JsonNode value) {
        switch (value.getNodeType()) {
            case ARRAY: //intentional fall-through
            case OBJECT: //intentional fall-through
            case POJO: //intentional fall-through
            case MISSING:
                throw new IllegalStateException("Not a primitive value type: " + value.getNodeType() + " of " + value);
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
     * Adds value ({@code entry}) as attribute to {@code element}.
     *
     * @param element An {@link Element}.
     * @param entry The value to add.
     * @param types The types used.
     * @return The updated {@code element}.
     */
    private Element addValueAsAttribute(final Element element, final Entry<String, JsonNode> entry,
        final Set<JsonPrimitiveTypes> types) {
        assert entry.getKey().startsWith("@") || getTextKey().equals(entry.getKey());
        final JsonNode v = entry.getValue();
        if (v.isValueNode()) {
            String val = v.asText();
            String key = entry.getKey();
            if (key.equals(getTextKey())) {
                return setTextContent(element, entry, types);
            }
            key = removeInvalidChars(key);
            if (m_looseTypeInfo) {
                element.setAttribute(key, val);
            } else if (v.isIntegralNumber()) {
                types.add(JsonPrimitiveTypes.INT);
                element.setAttributeNS(INTEGER_NAMESPACE, m_settings.m_int + ":" + key, val);
            } else if (v.isFloatingPointNumber()) {
                types.add(JsonPrimitiveTypes.FLOAT);
                element.setAttributeNS(DECIMAL_NAMESPACE, m_settings.m_real + ":" + key, val);
            } else if (v.isBinary()) {
                types.add(JsonPrimitiveTypes.BINARY);
                element.setAttributeNS(BINARY_NAMESPACE, m_settings.m_binary + ":" + key, val);
            } else if (v.isTextual()) {
                //We cannot recognize as binary, as for example "text" is recognized as binary
                types.add(JsonPrimitiveTypes.TEXT);
                element.setAttributeNS(STRING_NAMESPACE, m_settings.m_text + ":" + key, val);
            } else if (v.isNull()) {
                types.add(JsonPrimitiveTypes.NULL);
                element.setAttributeNS(NULL_NAMESPACE, m_settings.m_null + ":" + key, "");
            } else if (v.isBoolean()) {
                types.add(JsonPrimitiveTypes.BOOLEAN);
                element.setAttributeNS(BOOLEAN_NAMESPACE, m_settings.m_bool + ":" + key, val);
            } else {
                assert false : entry;
            }
            return element;
        }
        assert false : v;
        throw new IllegalStateException(v.toString());
    }

    /**
     * Sets value as text of the XML.
     *
     * @param element The {@link Element} to adjust.
     * @param entry The value to add.
     * @param types The types used.
     * @return The updated {@code element}.
     */
    private Element setTextContent(final Element element, final Entry<String, JsonNode> entry,
        final Set<JsonPrimitiveTypes> types) {
        final JsonNode v = entry.getValue();
        final String val = v.asText();
        element.appendChild(element.getOwnerDocument().createTextNode(val));
        if (!m_looseTypeInfo) {
            if (v.isIntegralNumber()) {
                types.add(JsonPrimitiveTypes.INT);
                return copyElementWithPrefix(element, m_settings.m_int, INTEGER_NAMESPACE);
            } else if (v.isFloatingPointNumber()) {
                types.add(JsonPrimitiveTypes.FLOAT);
                return copyElementWithPrefix(element, m_settings.m_real, DECIMAL_NAMESPACE);
            } else if (v.isTextual()) {
                types.add(JsonPrimitiveTypes.TEXT);
                return copyElementWithPrefix(element, m_settings.m_text, STRING_NAMESPACE);
            } else if (v.isNull()) {
                types.add(JsonPrimitiveTypes.NULL);
                return copyElementWithPrefix(element, m_settings.m_null, NULL_NAMESPACE);
            } else if (v.isBinary()) {
                types.add(JsonPrimitiveTypes.BINARY);
                return copyElementWithPrefix(element, m_settings.m_binary, BINARY_NAMESPACE);
            } else if (v.isBoolean()) {
                types.add(JsonPrimitiveTypes.BOOLEAN);
                return copyElementWithPrefix(element, m_settings.m_bool, BOOLEAN_NAMESPACE);
            } else {
                assert false : entry;
                throw new IllegalStateException(v.toString());
            }
        }
        return element;
    }

    /**
     * @param element
     * @param i
     * @param integerNamespace
     * @return
     */
    private Element copyElementWithPrefix(final Element element, final String prefix, final String namespace) {
        final Document document = element.getOwnerDocument();
        final Element newElement = document.createElementNS(namespace, prefix + ":" + element.getNodeName());
        final Node parentNode = element.getParentNode();
        if (parentNode != null) {
            final Node nextSibling = element.getNextSibling();
            parentNode.removeChild(element);
            if (nextSibling != null) {
                parentNode.insertBefore(newElement, nextSibling);
            }else {
                parentNode.appendChild(newElement);
            }
        }
        final NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); ++i) {
            final Node attrib = attributes.item(i);
            newElement.setAttributeNS(attrib.getNamespaceURI(), attrib.getLocalName(), attrib.getNodeValue());
        }
        final NodeList childNodes = element.getChildNodes();
        //Important to keep a copy as the appendChild call would change the childNodes.
        List<Node> children = new ArrayList<>(childNodes.getLength());
        for (int i = 0; i < childNodes.getLength(); ++i) {
            children.add(childNodes.item(i));
        }
        for (Node node : children) {
            newElement.appendChild(node);
        }
        //newElement.setTextContent(element.getTextContent());
        return newElement;
    }

    /**
     * Creates element with text content (namespace is adjusted according to {@code type}).
     *
     * @param prefix The prefix to use.
     * @param rawElementName The raw {@link Element} name for the to be created element.
     * @param type The type of the element.
     * @param content The text content.
     * @param doc The owner document.
     * @param types The types used.
     * @return The transformed {@link Element}.
     */
    protected Element createElementWithContent(final String prefix, final String rawElementName,
        final JsonPrimitiveTypes type, final String content, final Document doc, final Set<JsonPrimitiveTypes> types) {
        final String cleanName = removeInvalidChars(rawElementName);
        final String elementName = elementName(prefix, cleanName, types, type);
        final Element elem =
            doc.createElementNS(m_looseTypeInfo ? getNamespace() : type.getDefaultNamespace(), elementName);
        if (!cleanName.equals(rawElementName)) {
            elem.setAttributeNS(ORIGINALKEY_URI, NS_ORIGINAL_KEY, rawElementName);
        }
        elem.setTextContent(content);
        return elem;
    }

    /**
     * @param jsonNode An object {@link JsonNode}, {@link ObjectNode}.
     * @return Checks whether the attributes contain key starting with {@code @}, or not.
     */
    private boolean hasAttribute(final JsonNode jsonNode) {
        assert jsonNode.isObject() : jsonNode + " " + jsonNode.getNodeType();
        final ObjectNode obj = (ObjectNode)jsonNode;
        for (final Iterator<String> it = obj.fieldNames(); it.hasNext();) {
            if (it.next().startsWith("@")) {
                return true;
            }
        }
        return false;
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
        final String cleanName = removeInvalidChars(name);
        if (cleanName.equals(name) && !name.isEmpty()) {
            return doc.createElementNS(m_settings.m_namespace, name);
        }
        final Element ret = doc.createElementNS(m_settings.m_namespace, cleanName.isEmpty() ? "_" : cleanName);
        ret.setAttributeNS(ORIGINALKEY_URI, NS_ORIGINAL_KEY, name);
        return ret;
    }

    /**
     * @param prefix The prefix to use (if we do not omit).
     * @param types The types used.
     * @param type Type of the element.
     * @return Name of the element with prefix if required.
     */
    private String elementName(final String prefix, final String rawElementName, final Set<JsonPrimitiveTypes> types,
        final JsonPrimitiveTypes type) {
        if (m_looseTypeInfo) {
            return rawElementName;
        }
        types.add(type);
        return prefix == null || prefix.isEmpty() ? rawElementName : prefix + ":" + rawElementName;
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
}
