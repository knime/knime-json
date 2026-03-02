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
 *   24 Sept. 2014 (Gabor): created
 */
package org.knime.json.node.patch.apply;

import static org.knime.node.impl.description.PortDescription.fixedPort;

import java.util.List;
import java.util.Map;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeView;
import org.knime.core.webui.node.dialog.NodeDialogFactory;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultKaiNodeInterface;
import org.knime.core.webui.node.dialog.kai.KaiNodeInterface;
import org.knime.core.webui.node.dialog.kai.KaiNodeInterfaceFactory;
import org.knime.core.webui.node.dialog.scripting.AbstractDefaultScriptingNodeDialog;
import org.knime.core.webui.node.dialog.scripting.AbstractFallbackScriptingNodeFactory;
import org.knime.node.impl.description.PortDescription;

/**
 * <code>NodeFactory</code> for the "JSONTransformer" Node. Changes JSON values.
 *
 * @author Gabor Bakos
 * @author Jannik Eurich, KNIME GmbH, Berlin, Germany
 * @author AI Migration Pipeline v1.2
 */
@SuppressWarnings("restriction")
public final class JSONPatchApplyNodeFactory extends AbstractFallbackScriptingNodeFactory<JSONPatchApplyNodeModel>
    implements NodeDialogFactory, KaiNodeInterfaceFactory {

    @Override
    public JSONPatchApplyNodeModel createNodeModel() {
        return new JSONPatchApplyNodeModel();
    }

    @Override
    public int getNrNodeViews() {
        return 0;
    }

    @Override
    public NodeView<JSONPatchApplyNodeModel> createNodeView(final int viewIndex,
        final JSONPatchApplyNodeModel nodeModel) {
        throw new UnsupportedOperationException("No views yet.");
    }

    private static final String NODE_NAME = "JSON Transformer";

    private static final String NODE_ICON = "./jsonpatch.png";

    private static final String SHORT_DESCRIPTION = """
            Applies a patch on the input JSON column.
            """;

    private static final String FULL_DESCRIPTION =
        """
                <p>Applies a <a href="http://tools.ietf.org/html/rfc6902">patch</a> or a <a
                    href="http://tools.ietf.org/html/rfc7386">merge patch</a> on the input JSON column.</p><p> When a
                    (merge) patch cannot be applied, missing values will be generated, node execution will not fail.</p><p>
                    See also the node: <a
                    href="http://www.knime.com/files/node-documentation/org.knime.json.node.patch.create.JSONPatchCreateNodeFactory.html">JSON
                    Diff</a>.</p><p> Given <tt>{"a":"b","c":{"d":"e","f": "g"} }</tt> let us assume the target is
                    <tt>{"a":"z","c":{"d":"e"} }</tt> (changing <tt>a</tt>'s value to <tt>z</tt> and removing <tt>f</tt>).
                    To achieve this, either the following patch should be applied:
                    <tt>[{"op":"replace","path":"/a","value":"z"},{"op":"remove","path":"/c/f"}]</tt> or this merge and
                    patch: <tt>{"a":"z","c":{"f": null} }</tt></p><p> The following operators (<tt>op</tt>) are supported
                    for patch: <ul> <li><tt>add</tt> (<tt>path</tt>, <tt>value</tt>)</li> <li><tt>remove</tt>
                    (<tt>path</tt>)</li> <li><tt>replace</tt> (<tt>path</tt>, <tt>value</tt>)</li> <li><tt>move</tt>
                    (<tt>from</tt>, <tt>path</tt>)</li> <li><tt>copy</tt> (<tt>from</tt>, <tt>path</tt>)</li>
                    <li><tt>test</tt> (<tt>path</tt>, <tt>value</tt>)</li> </ul></p><p> The merge and patch format
                    reconstructs the parts that need to be changed (changes included), all else can be omitted.</p><p> It
                    uses the <a href="https://github.com/fge/json-patch">fge/json-patch</a> implementation.</p> <p>To refer
                    to flow variables, use the <tt>$${TflowVarName}$$</tt> syntax (where <tt>T</tt> is <tt>S</tt> for String
                    type, <tt>D</tt> for floating point numbers and <tt>I</tt> for integer numbers).</p> <p>To refer to
                    columns (boolean, numeric, String, JSON), use the <tt>$columnName$</tt> syntax.</p> <p>References to
                    (<tt>0</tt>-based) row index (<tt>$$ROWINDEX$$</tt>), row count (<tt>$$ROWCOUNT$$</tt>) and row keys
                    (<tt>$$ROWID$$</tt>) can also be used in values.</p>
                """;

    private static final List<PortDescription> INPUT_PORTS = List.of(fixedPort("table with JSON", """
            A table with JSON column to transform
            """));

    private static final List<PortDescription> OUTPUT_PORTS = List.of(fixedPort("table with JSON", """
            Table with the transformed JSON values
            """));

    @Override
    public AbstractDefaultScriptingNodeDialog createNodeDialog() {
        return new JSONPatchApplyScriptingNodeDialog();
    }

    @Override
    public KaiNodeInterface createKaiNodeInterface() {
        return new DefaultKaiNodeInterface(Map.of(SettingsType.MODEL, JSONPatchApplyNodeParameters.class));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeDialogPane createLegacyNodeDialogPane() {
        // TODO Auto-generated method stub
        return null;
    }
}
