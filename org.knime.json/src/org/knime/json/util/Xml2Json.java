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

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Notation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeCreator;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

/**
 * Helper class to convert XML {@link Document} to Jackson {@link JsonNode}.
 *
 * @author Gabor Bakos
 */
public class Xml2Json implements Cloneable {
    private enum Position {
        First, Last;
    }

    private enum GroupingStrategy {
        /** No grouping is performed. */
        None,
        /** Group consecutive array elements with same name to single element. */
        OnlyIfSafe,
        /**
         * Use Object instead of array and resolve ambiguity by appending {@code _ddd} number, fail if that causes
         * ambiguity.
         */
        //usually for text, comment and processing mixed
            GroupWithNumber,
            /** Group items with same name together, even if this make the positional information get lost. */
            GroupWithoutPositionalInformation;
    }

    /**
     * Where to put the attributes within the array?
     */
    private Position m_attributePosition = Position.First;

    /**
     * Should we omit the prefix/namespace properties of attributes and make them key/values. If {@code true} (and
     * collapseSingle is {@code false}): {@code <a ns:a1="x" ns:a2="y"/>} becomes:
     * <code>{"a":[[{"a1":"x","a2":"y"}]]}</code> If {@code false} (and noNamespaces also {@code false}):
     * {@code <a ns:a1="x" ns:a2="y"/>} becomes:
     * <code>{"a":[[{"__namespaceRef__":"ns", "a1":"x"},{"__namespaceRef__":"ns", "a2":"y"}]]}</code>
     */
    private boolean m_simpleAttributes = true;

    /**
     * Omit namespaces from the output.
     */
    private boolean m_noNamespaces = false;

    /**
     * When there are multiple elements with the same name
     */
    private GroupingStrategy m_groupingStrategy = GroupingStrategy.None;

    /**
     * When {@code false}, namespace info looks like a sibling of the real content within the object, when {@code true},
     * the namespace info is added to the attributes.
     */
    private boolean m_treatNamespaceInfoAsAttribute = false;

    /** Comments should be translated or omitted. */
    private boolean m_translateComments = false;

    /** Processing instructions to be translated or omitted. */
    private boolean m_translateProcessingInstructions = false;

//    //attributes are arrays within the array of the content
//    @Deprecated
    private String m_attribute = "__attribute__";

    private String m_processing = "__processing__";

    private String m_comment = "__comment__";

    private String m_cdata = "__cdata__";

    private String m_text = "__text__";

    private String m_entity = "__entity__";

    private String m_entityRef = "__entityRef__";

    private String m_namespace = "__namespace__";

    private String m_namespaceRef = "__namespaceRef__";

    private String m_notation = "__notation__";

    private String m_attributePrefix = "@";

    private String m_processingPrefix = "?";

    /**
     *
     */
    public Xml2Json() {
    }

    /**
     * Converts an XML {@link Document} to Jackson {@link JsonNode}.
     *
     * @param doc The input XML {@link Document}.
     * @return The converted Jackson {@link JsonNode}.
     */
    public JsonNode toJson(final Document doc) {
        JsonNodeFactory fact = JsonNodeFactory.withExactBigDecimals(true);
        ObjectNode objectNode = fact.objectNode();
        ArrayNode notations = null;
        for (int i = 0; i < doc.getChildNodes().getLength(); ++i) {
            if (doc.getChildNodes().item(i).getNodeType() == Node.NOTATION_NODE) {
                if (notations == null) {
                    notations = objectNode.arrayNode();
                }
                Notation notation = (Notation)doc.getChildNodes().item(i);
                String publicId = notation.getPublicId();
                String systemId = notation.getSystemId();
                if (publicId != null || systemId != null) {
                    ObjectNode node = notations.addObject();
                    if (publicId != null) {
                        node.put("publicId", publicId);
                    }
                    if (systemId != null) {
                        node.put("systemId", systemId);
                    }
                    node.set(notation.getNodeName(), node.nullNode());
                }
            }
        }
        if (notations != null) {
            objectNode.set(m_notation, notations);
        }
        Element element = doc.getDocumentElement();
        if (element != null) {
            process(objectNode, element);
        }
        return objectNode;
    }

    /**
     * @param node
     * @param element
     */
    private ObjectNode process(final ObjectNode node, final Element element) {
        if (!m_noNamespaces) {
            String uri = element.getNamespaceURI();
            if (uri != null) {
                node.put(m_namespace, uri);
                String prefix = element.getPrefix();
                if (prefix != null) {
                    node.put(m_namespaceRef, prefix);
                }
            }
        }
        Map<String, JsonNode> group = groupingAllowed(element);
        if (group != null) {
            if (m_attributePosition == Position.First) {
                processAttributes(node, element);
            }
            processChildren(node, element.getChildNodes(), group);
            if (m_attributePosition == Position.Last) {
                processAttributes(node, element);
            }
        } else {
            ArrayNode arrayNode = node.arrayNode();
            node.set(element.getNodeName(), arrayNode);
            if (m_attributePosition == Position.First) {
                processAttributes(arrayNode, element);
            }
            processChildren(arrayNode, element.getChildNodes());
            if (m_attributePosition == Position.Last) {
                processAttributes(arrayNode, element);
            }
        }
        return node;
    }

    /**
     * @param n
     * @param childNodes
     * @param group
     */
    private void processChildren(final ObjectNode o, final NodeList childNodes, final Map<String, JsonNode> group) {
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            switch (node.getNodeType()) {
                case Node.CDATA_SECTION_NODE:
                    put(group, m_cdata, node.getNodeValue(), o);
                    break;
                case Node.COMMENT_NODE:
                    if (m_translateComments) {
                        put(group, m_comment, node.getNodeValue(), o);
                    }
                    break;
                case Node.ENTITY_NODE:
                    put(group, m_entity, node.getNodeName(), o);
                    break;
                case Node.ENTITY_REFERENCE_NODE:
                    put(group, m_entityRef, node.getNodeName(), o);
                    break;
                case Node.PROCESSING_INSTRUCTION_NODE:
                    if (m_translateProcessingInstructions) {
                        if (m_processingPrefix != null) {
                            put(group, m_processingPrefix + node.getNodeName(), node.getNodeValue(), o);
                        } else {
                            ObjectNode proc = o.objectNode();
                            proc.put(node.getNodeName(), node.getNodeValue());
                            put(group, m_processing, proc, o);
                        }
                    }
                    break;
                case Node.TEXT_NODE:
                    put(group, m_text, node.getNodeValue(), o);
                    break;
                case Node.ELEMENT_NODE:
                    put(group, node.getNodeName(),
                        process(o
                            .objectNode()
                            , (Element)node)
                        , o)
                        ;
                    break;
                default:
                    throw new UnsupportedOperationException("Unknown node type: " + node.getNodeType() + "\n" + node);
            }
        }
//        if (group.size() == 1) {
//            Entry<String, JsonNode> entry = group.entrySet().iterator().next();
//            if (entry.getValue().get(entry.getKey()) != null && entry.getValue() instanceof ObjectNode) {
//                o.setAll((ObjectNode)entry.getValue());
//                return;
//            }
//        }
        o.setAll(group);
    }

    /**
     * @param group
     * @param key
     * @param obj
     */
    private void put(final Map<String, JsonNode> group, final String key, final JsonNode obj, final JsonNodeCreator creator) {
        JsonNode old = group.get(key);
        if (old == null) {
            if (obj instanceof ObjectNode) {
                ObjectNode objNode = (ObjectNode)obj;
                Iterator<Entry<String, JsonNode>> it = objNode.fields();
                if (it.hasNext() && it.next().getKey().equals(key) && !it.hasNext() && objNode.get(key) instanceof ArrayNode) {
                    //We have to correct the #processAttributes(ArrayNode, Element) extra key.
                    group.put(key, objNode.get(key));
                    return;
                }
            }
            group.put(key, obj);
        } else if (old instanceof ArrayNode) {
            ArrayNode array = (ArrayNode)old;
            array.add(obj);
        } else {
            ArrayNode array = creator.arrayNode();
            array.add(old);
            array.add(obj);
            group.put(key, array);
        }
    }

    /**
     * @param group
     * @param key
     * @param nodeValue
     * @param creator
     */
    private void put(final Map<String, JsonNode> group, final String key, final String nodeValue, final JsonNodeCreator creator) {
        JsonNode old = group.get(key);
        if (old == null) {
            group.put(key, new TextNode(nodeValue));
        } else if (old instanceof ArrayNode) {
            ArrayNode array = (ArrayNode)old;
            array.add(nodeValue);
        } else {
            ArrayNode array = creator.arrayNode();
            array.add(old);
            array.add(new TextNode(nodeValue));
            group.put(key, array);
        }
    }

    /**
     * @param element
     * @return
     */
    private Map<String, JsonNode> groupingAllowed(final Element element) {
        switch (m_groupingStrategy) {
            case None:
                return null;
            case OnlyIfSafe: {
                String lastKey = null;
                Set<String> keys = new LinkedHashSet<>();
                if (m_attributePosition == Position.First) {
                    Set<? extends String> attributeKeys = attributeKeys(element, keys);
                    if (attributeKeys == null) {
                        return null;
                    }
                    keys.addAll(attributeKeys);
                }
                for (int i = 0; i < element.getChildNodes().getLength(); ++i) {
                    Node node = element.getChildNodes().item(i);
                    String key;
                    switch (node.getNodeType()) {
                        case Node.CDATA_SECTION_NODE:
                            key = m_cdata;
                            break;
                        case Node.TEXT_NODE:
                            key = m_text;
                            break;
                        case Node.COMMENT_NODE:
                            if (m_translateComments) {
                                key = m_comment;
                            } else {
                                continue;
                            }
                            break;
                        case Node.NOTATION_NODE:
                            key = m_notation;
                            break;
                        case Node.PROCESSING_INSTRUCTION_NODE:
                            if (m_translateProcessingInstructions) {
                                key = m_processingPrefix == null ? m_processing : m_processingPrefix + node.getNodeName();
                            } else {
                                continue;
                            }
                            break;
                        case Node.ENTITY_NODE:
                            key = m_entity;
                            break;
                        case Node.ENTITY_REFERENCE_NODE:
                            key = m_entityRef;
                            break;
                        case Node.ELEMENT_NODE:
                            key = node.getNodeName();
                            break;
                        default:
                            throw new IllegalStateException("Unknown type: " + node.getNodeType() + "\n" + node);
                    }
                    if (lastKey == null) {
                        lastKey = key;
                    }
                    if (!keys.add(key) && lastKey != key) {
                        return null;
                    }
                    lastKey = key;
                }
                if (m_attributePosition == Position.Last) {
                    Set<? extends String> attributeKeys = attributeKeys(element, keys);
                    if (attributeKeys == null) {
                        return null;
                    }
                    keys.addAll(attributeKeys);
                }
                keys.removeAll(attributeKeys(element, new HashSet<String>()));
                Map<String, JsonNode> ret = new LinkedHashMap<>();
                for (String string : keys) {
                    ret.put(string, null);
                }
                return ret;
            }
            case GroupWithoutPositionalInformation: {
                Map<String, JsonNode> ret = new LinkedHashMap<>();
                return ret;
            }
            case GroupWithNumber: {
                Map<String, JsonNode> ret = new LinkedHashMap<>();
                return ret;
            }
            default:
                throw new UnsupportedOperationException("Not supported grouping strategy: " + m_groupingStrategy);
        }
    }

    /**
     * @param element
     * @param keys
     * @return
     */
    private Set<? extends String> attributeKeys(final Element element, final Set<String> keys) {
        Set<String> ret = new LinkedHashSet<>();
        if (m_treatNamespaceInfoAsAttribute && !m_noNamespaces) {
            String namespace = element.getNamespaceURI();
            String prefix = element.getPrefix();
            if (namespace != null) {
                ret.add(m_namespace);
                if (prefix != null) {
                    ret.add(m_namespaceRef);
                }
            }
        }
        String prefix;
        if ((m_simpleAttributes || m_noNamespaces) && m_attributePrefix != null) {
            prefix = m_attributePrefix;
        } else {
            prefix = "";
        }
        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); ++i) {
            String name = prefix + attributes.item(i).getNodeName();
            ret.add(name);
            if (keys.contains(name)) {
                return null;
            }
        }
        return ret;
    }

    /**
     * @param rootNode
     * @param childNodes
     */
    private void processChildren(final ArrayNode rootNode, final NodeList childNodes) {
        ObjectNode o;
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            if (node.getNodeType() == Node.CDATA_SECTION_NODE || node.getNodeType() == Node.TEXT_NODE) {
                rootNode.add(node.getTextContent());
            } else {
                o = rootNode.addObject();
                switch (node.getNodeType()) {
                    case Node.CDATA_SECTION_NODE:
                        throw new IllegalStateException("Should be handled in the if case!");
                    case Node.COMMENT_NODE:
                        if (m_translateComments) {
                            o.put(m_comment, node.getNodeValue());
                        }
                        break;
                    case Node.ENTITY_NODE:
                        o.put(m_entity, node.getNodeName());
                        break;
                    case Node.ENTITY_REFERENCE_NODE:
                        o.put(m_entityRef, node.getNodeName());
                        break;
                    case Node.PROCESSING_INSTRUCTION_NODE:
                        if (m_translateProcessingInstructions) {
                            if (m_processingPrefix != null) {
                                o.put(m_processingPrefix + node.getNodeName(), node.getNodeValue());
                            } else {
                                ObjectNode proc = o.objectNode();
                                proc.put(node.getNodeName(), node.getNodeValue());
                                o.set(m_processing, proc);
                            }
                        }
                        break;
                    case Node.TEXT_NODE:
                        throw new IllegalStateException("Should be handled in the if case!");
                    case Node.ELEMENT_NODE:
                        o.set(node.getNodeName(), process(o.objectNode(), (Element)node));
                        break;
                    default:
                        throw new UnsupportedOperationException("Unknown node type: " + node.getNodeType() + "\n"
                            + node);
                }
            }
        }
    }

    /**
     * @param node
     * @param element
     */
    private void processAttributes(final ArrayNode node, final Element element) {
        NamedNodeMap attributes = element.getAttributes();
        if (attributes.getLength() > 0) {
            if (m_simpleAttributes || m_noNamespaces) {
                ObjectNode objectNode = node.objectNode();
                node.add(objectNode);
                for (int i = 0; i < attributes.getLength(); ++i) {
                    Node attr = attributes.item(i);
                    objectNode.put(attr.getNodeName(), attr.getNodeValue());
                }
            } else {
                //arrayNode is necessary because of namespaces and prefixes
                ArrayNode arrayNode = node.arrayNode();
                node.add(arrayNode);
                for (int i = 0; i < attributes.getLength(); ++i) {
                    Node attr = attributes.item(i);
                    ObjectNode object = arrayNode.addObject();
                    String namespace = attr.getNamespaceURI();
                    String prefix = attr.getPrefix();
                    if (namespace != null) {
                        object.put(m_namespace, namespace);
                        if (prefix != null) {
                            object.put(m_namespaceRef, prefix);
                        }
                    }
                    object.put(attr.getNodeName(), attr.getNodeValue());
                }
            }
        }
    }

    /**
     * @param node
     * @param element
     */
    private void processAttributes(final ObjectNode node, final Element element) {
        NamedNodeMap attributes = element.getAttributes();
        if (attributes.getLength() > 0) {
            if (m_simpleAttributes || m_noNamespaces) {
                for (int i = 0; i < attributes.getLength(); ++i) {
                    Node attr = attributes.item(i);
                    node.put(m_attributePrefix + attr.getNodeName(), attr.getNodeValue());
                }
            } else {
                //arrayNode is necessary because of namespaces and prefixes
                ArrayNode arrayNode = node.arrayNode();
                node.set(m_attribute, arrayNode);
                for (int i = 0; i < attributes.getLength(); ++i) {
                    Node attr = attributes.item(i);
                    ObjectNode object = arrayNode.addObject();
                    String namespace = attr.getNamespaceURI();
                    String prefix = attr.getPrefix();
                    if (namespace != null) {
                        object.put(m_namespace, namespace);
                        if (prefix != null) {
                            object.put(m_namespaceRef, prefix);
                        }
                    }
                    object.put(attr.getNodeName(), attr.getNodeValue());
                }
            }
        }
    }

    /**
     * @param simpleAttributes the simpleAttributes to set
     */
    public void setSimpleAttributes(final boolean simpleAttributes) {
        this.m_simpleAttributes = simpleAttributes;
    }

    /**
     * @param translateComments the translateComments to set
     * @return The new {@link Xml2Json} node with {@code translateComments} for the option translate comments.
     * @since 3.2
     */
    public Xml2Json setTranslateComments(final boolean translateComments) {
        return setInClone(clone -> clone.m_translateComments = translateComments);
    }

    /**
     * @param translateProcessingInstructions the translateProcessingInstructions to set
     * @return The new {@link Xml2Json} node with {@code translateProcessingInstructions} for the option translate
     *         processing instructions.
     * @since 3.2
     */
    public Xml2Json setTranslateProcessingInstructions(final boolean translateProcessingInstructions) {
        return setInClone(clone -> clone.m_translateProcessingInstructions = translateProcessingInstructions);
    }

    /**
     * @return The preconfigured {@link Xml2Json} object with proposed settings.
     */
    public static Xml2Json proposedSettings() {
        Xml2Json ret = new Xml2Json();
        ret.m_attributePrefix = "@";
        ret.m_attributePosition = Position.First;
        ret.m_cdata = "#cdata-section";
        ret.m_comment = "#comment";
        ret.m_entity = null;
        ret.m_entityRef = null;
        ret.m_groupingStrategy = GroupingStrategy.OnlyIfSafe;
        //ret.m_namespace =
        //ret.m_namespaceRef =
        ret.m_translateComments = false;
        ret.m_translateProcessingInstructions = false;
        ret.m_noNamespaces = true;
        ret.m_processingPrefix = "?";
        ret.m_simpleAttributes = true;
        ret.m_text = "#text";
        ret.m_treatNamespaceInfoAsAttribute = true;
        return ret;
    }

    /**
     * @param textKey The key for text entries.
     * @return The new {@link Xml2Json} node with {@code textKey} for text.
     */
    public Xml2Json setTextKey(final String textKey) {
        return setInClone((clone) -> clone.m_text = textKey);
    }

    /**
     * @param change The lambda changing the clone.
     * @return The cloned and updated {@link Xml2Json} object.
     */
    private Xml2Json setInClone(final Consumer<Xml2Json> change) {
        Xml2Json clone;
        try {
            clone = (Xml2Json)clone();
            change.accept(clone);
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException(e);
        }
        return clone;
    }
}
