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
package org.knime.json.node.jsonpath.multi;

import static org.knime.node.impl.description.PortDescription.fixedPort;

import java.io.IOException;
import java.util.List;

import org.apache.xmlbeans.XmlException;
import org.knime.core.node.NodeDescription;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;
import org.knime.core.webui.node.dialog.NodeDialog;
import org.knime.core.webui.node.dialog.NodeDialogFactory;
import org.knime.core.webui.node.dialog.NodeDialogManager;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeDialog;
import org.knime.node.impl.description.DefaultNodeDescriptionUtil;
import org.knime.node.impl.description.PortDescription;
import org.xml.sax.SAXException;

/**
 * <code>NodeFactory</code> for the "JSONPath" Node. Selects certain paths from the selected JSON column.
 *
 * @author Gabor Bakos
 * @author Paul Baernreuther, KNIME GmbH, Germany
 * @author AI Migration Pipeline v1.2
 */
@SuppressWarnings("restriction")
public class JSONPathNodeFactory extends NodeFactory<JSONPathNodeModel> implements NodeDialogFactory {

    /**
     * Feature flag for webUI configuration dialogs in local AP.
     */
    private static final boolean JSON_PATH_WEBUI_DIALOG =
        !"js".equals(System.getProperty("org.knime.jsonpath.webuidialog"));

    /**
     * {@inheritDoc}
     */
    @Override
    public JSONPathNodeModel createNodeModel() {
        return new JSONPathNodeModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNrNodeViews() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeView<JSONPathNodeModel> createNodeView(final int viewIndex, final JSONPathNodeModel nodeModel) {
        throw new IllegalArgumentException("No views: " + viewIndex);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasDialog() {
        return true;
    }

    private static final String NODE_NAME = "JSON Path";

    private static final String NODE_ICON = "./jsonpath.png";

    private static final String SHORT_DESCRIPTION = """
            Selects the defined paths from the selected JSON column.
            """;

    /**
     * TODO: This documentation is lying about the possibility to auto-generate JSONPath queries in the dialog. (Since
     * this is v1...)
     */
    private static final String FULL_DESCRIPTION = """
            <p> <a href="http://goessner.net/articles/JsonPath/">JSONPath</a> is a query language for JSON, similar
                to XPath for XML. </p> <p> The result of a simple query (also called definite JSONPath) is a single
                value. The result of a collection query (also called indefinite JSONPath) is a list of multiple values.
                Results of JSONPath queries are converted to the selected KNIME type. If the result is a list and the
                selected KNIME type is not compatible, the execution will fail. If the result cannot be converted to the
                selected KNIME type, a missing value will be returned. </p> <p> JSONPath queries can be automatically
                generated via the node configuration dialog.
                Alternatively, you can write your own JSONPath query by clicking the "Add JSONPath" button. </p> Example
                input: <pre><code>
                {"book": [
                    {"year": 1999,
                    "title": "Timeline",
                    "author": "Michael Crichton"
                    }, {"year": 2000,
                     "title": "Plain Truth",
                     "author": "Jodi Picoult"}
                ]}
                </code></pre> <p> Example JSONPath queries and
                evaluation results:<br /> <b>$.book[0]</b><br /><tt>{"year": 1999, "title": "Timeline", "author":
                "Michael Crichton"}</tt> (<i>JSON</i> or <i>String</i> single value)<br /> <b>$.book[*].year</b><br
                /><tt>[1999,2000]</tt> (<i>JSON</i>, <i>Int</i> or <i>Real</i> list)<br /> <b>$.book[2].year</b><br
                /><tt>?</tt> (no such part)<br /> <b>$.book[?(@.year==1999)].title</b><br /><tt>Timeline</tt>
                (<i>String</i>) or <tt>"Timeline"</tt> (<i>JSON</i>) </p><p> The default path (<tt>$..*</tt>) will
                select all possible subparts (excluding the whole JSON value). </p><p>When you request the paths instead
                of values for the <tt>$.book[0].*</tt> JSONPath, you will get the paths -in bracket notation- as a list
                of Strings:<ul> <li>$['book'][0]['year']</li> <li>$['book'][0]['title']</li>
                <li>$['book'][0]['author']</li> </ul> which are valid JSONPaths for the input JSON value. </p><p>The
                filters <tt>?(expr)</tt>can be used to select contents with specific properties, for example
                <tt>$..book[?(@.publisher)]</tt> selects the books that specify their publisher (<tt>@</tt> refers to
                the actual element). </p><p> The JSON Path node uses the <a
                href="https://github.com/json-path/JsonPath">Jayway JSONPath</a> implementation.</p>
            """;

    private static final List<PortDescription> INPUT_PORTS = List.of(fixedPort("Table with JSON", """
            A table with JSON column
            """));

    private static final List<PortDescription> OUTPUT_PORTS = List.of(fixedPort("Table", """
            Table with the found parts
            """));

    @Override
    public NodeDialogPane createNodeDialogPane() {
        if (JSON_PATH_WEBUI_DIALOG) {
            return NodeDialogManager.createLegacyFlowVariableNodeDialog(createNodeDialog());
        }
        return new JSONPathNodeDialog();
    }

    @Override
    public boolean hasNodeDialog() {
        return JSON_PATH_WEBUI_DIALOG;
    }

    @Override
    public NodeDialog createNodeDialog() {
        return new DefaultNodeDialog(SettingsType.MODEL, JSONPathNodeParameters.class);
    }

    @Override
    public NodeDescription createNodeDescription() throws SAXException, IOException, XmlException {
        if (JSON_PATH_WEBUI_DIALOG) {
            return DefaultNodeDescriptionUtil.createNodeDescription(NODE_NAME, NODE_ICON, INPUT_PORTS, OUTPUT_PORTS,
                SHORT_DESCRIPTION, FULL_DESCRIPTION, List.of(), JSONPathNodeParameters.class, null,
                NodeType.Manipulator, List.of(), null);
        }
        return super.createNodeDescription();
    }

    // TODO: Implement KaiNodeInterfaceFactory
    //  @Override
    // public KaiNodeInterface createKaiNodeInterface() {
    //     return new DefaultKaiNodeInterface(Map.of(SettingsType.MODEL, JSONPathNodeParameters.class));
    // }
}
