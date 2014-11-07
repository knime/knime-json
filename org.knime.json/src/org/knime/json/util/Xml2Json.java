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

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Notation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Helper class to convert XML {@link Document} to Jackson {@link JsonNode}.
 *
 * @author Gabor Bakos
 * @since 2.11
 */
public class Xml2Json {
    //attributes are arrays within the array of the content
    @Deprecated
    private String m_attribute = "__attribute__";
    private String m_processing = "__processing__";
    private String m_comment = "__comment__";
    //CDATA is not distinguished from text
    @Deprecated
    private String m_cdata = "__cdata__";
    //Text is not distinguished from CDATA
    @Deprecated
    private String m_text = "__text__";
    private String m_entity = "__entity__";
    private String m_entityRef = "__entityRef__";
    private String m_namespace = "__namespace__";
    private String m_namespaceRef = "__namespaceRef__";
    private String m_notation = "__notation__";

    /**
     *
     */
    public Xml2Json() {
    }

    /**
     * Converts an XML {@link Document} to Jackson {@link JsonNode}.
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
        process(objectNode, element);
        return objectNode;
    }

    /**
     * @param node
     * @param element
     */
    private void process(final ObjectNode node, final Element element) {
        String uri = element.getNamespaceURI();
        if (uri != null) {
            node.put(m_namespace, uri);
            String prefix = element.getPrefix();
            if (prefix != null) {
                node.put(m_namespaceRef, prefix);
            }
        }
        ArrayNode arrayNode = node.arrayNode();
        node.set(element.getNodeName(), arrayNode);
        processAttributes(arrayNode, element);
        processChildren(arrayNode, element.getChildNodes());
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
            o =  rootNode.addObject();
            switch (node.getNodeType()) {
                case Node.CDATA_SECTION_NODE:
                    throw new IllegalStateException("Should be handled in the if case!");
                    //o.put(m_cdata, node.getNodeValue());
                case Node.COMMENT_NODE:
                    o.put(m_comment, node.getNodeValue());
                    break;
                case Node.ENTITY_NODE:
                    o.put(m_entity, node.getNodeName());
                    break;
                case Node.ENTITY_REFERENCE_NODE:
                    o.put(m_entityRef, node.getNodeName());
                    break;
                case Node.PROCESSING_INSTRUCTION_NODE:
                    o.put(m_processing, node.getNodeName());
                    break;
                case Node.TEXT_NODE:
                    throw new IllegalStateException("Should be handled in the if case!");
                case Node.ELEMENT_NODE:
                    //ObjectNode child = o.objectNode();
                    process(o, (Element)node);
                    //o.set(node.getNodeName(), child);
                    break;
                    default:
                        throw new UnsupportedOperationException("Unknown node type: " + node.getNodeType() + "\n" + node);
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
            //arrayNode is necessary because of namespaces and prefixes
            ArrayNode arrayNode = node.arrayNode();
            node.add(arrayNode);
            for (int i = 0; i < attributes.getLength(); ++i) {
                Node attr = attributes.item(i);
                String namespace = attr.getNamespaceURI();
                String prefix = attr.getPrefix();
                ObjectNode object = arrayNode.addObject();
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
