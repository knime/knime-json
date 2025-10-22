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
 *   14 Sept. 2014 (Gabor): created
 */
package org.knime.json.node.fromxml;

import static org.knime.node.impl.description.PortDescription.fixedPort;

import java.util.List;
import java.util.Map;

import org.knime.core.node.NodeDescription;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;
import org.knime.core.webui.node.dialog.NodeDialog;
import org.knime.core.webui.node.dialog.NodeDialogFactory;
import org.knime.core.webui.node.dialog.NodeDialogManager;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultKaiNodeInterface;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeDialog;
import org.knime.core.webui.node.dialog.kai.KaiNodeInterface;
import org.knime.core.webui.node.dialog.kai.KaiNodeInterfaceFactory;
import org.knime.node.impl.description.DefaultNodeDescriptionUtil;
import org.knime.node.impl.description.PortDescription;

/**
 * <code>NodeFactory</code> for the "XMLToJSON" Node. Converts XML values to JSON values.
 *
 * @author Gabor Bakos
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@SuppressWarnings("restriction")
public class XMLToJSONNodeFactory extends NodeFactory<XMLToJSONNodeModel>
    implements NodeDialogFactory, KaiNodeInterfaceFactory {

    @Override
    public XMLToJSONNodeModel createNodeModel() {
        return new XMLToJSONNodeModel();
    }

    @Override
    public int getNrNodeViews() {
        return 0;
    }

    @Override
    public NodeView<XMLToJSONNodeModel> createNodeView(final int viewIndex, final XMLToJSONNodeModel nodeModel) {
        throw new IllegalStateException("No views: " + viewIndex);
    }

    @Override
    public boolean hasDialog() {
        return true;
    }

    private static final String NODE_NAME = "XML to JSON";
    private static final String NODE_ICON = "./xml2json.png";
    private static final String SHORT_DESCRIPTION = """
            Converts XML values to JSON values.
            """;
    private static final String FULL_DESCRIPTION = """
            Reads values from an XML column and converts them into JSON format.
            <ul>
                <li>If there are <b>multiple pieces of text</b> around inner elements, they are translated into
                    elements of a JSON array.</li>
                <li>Text content inside XML elements is stored under a specified key (default key is <code>text</code>).
                </li>
                <li>If enabled, XML <b>attributes</b> are converted to JSON keys with an <code>@</code> prefix.</li>
                <li>If enabled, XML <b>processing instructions</b> are converted to JSON keys with a <code>?</code>
                prefix.</li>
            </ul>
            <b>Example</b>:<br/><br/>
            The XML
            <pre><code>&lt;a&gt;Hello &lt;b&gt;world&lt;/b&gt;!&lt;/a&gt;</code></pre>
            becomes:
            <pre><code>{
               "a": [
                 "Hello ",
                 {
                   "b": {
                     "text": "world"
                   }
                 },
                 "!"
               ]
            }</code></pre>
            """;
    private static final List<PortDescription> INPUT_PORTS = List.of(
            fixedPort("table with XML", """
                Table with an XML column.
                """)
    );
    private static final List<PortDescription> OUTPUT_PORTS = List.of(
            fixedPort("table with JSON", """
                Table containing the converted JSON column.
                """)
    );

    /**
     * @since 5.8
     */
    @Override
    public NodeDialogPane createNodeDialogPane() {
        return NodeDialogManager.createLegacyFlowVariableNodeDialog(createNodeDialog());
    }

    /**
     * @since 5.8
     */
    @Override
    public NodeDialog createNodeDialog() {
        return new DefaultNodeDialog(SettingsType.MODEL, XMLToJSONNodeParameters.class);
    }

    @Override
    public NodeDescription createNodeDescription() {
        return DefaultNodeDescriptionUtil.createNodeDescription(
            NODE_NAME,
            NODE_ICON,
            INPUT_PORTS,
            OUTPUT_PORTS,
            SHORT_DESCRIPTION,
            FULL_DESCRIPTION,
            List.of(),
            XMLToJSONNodeParameters.class,
            null,
            NodeType.Manipulator,
            List.of(),
            null
        );
    }

    /**
     * @since 5.8
     */
    @Override
    public KaiNodeInterface createKaiNodeInterface() {
        return new DefaultKaiNodeInterface(Map.of(SettingsType.MODEL, XMLToJSONNodeParameters.class));
    }

}
