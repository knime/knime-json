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
 * ------------------------------------------------------------------------
 */

package org.knime.json.node.input;

import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.widget.text.TextAreaWidget;

/**
 * Node parameters for Container Input (JSON).
 *
 * @author Paul Baernreuther, KNIME GmbH, Germany
 * @author AI Migration Pipeline v1.1
 */
@LoadDefaultsForAbsentFields
class JSONInputNodeParameters implements NodeParameters {

    @Widget(title = "Parameter name",
        description = "A unique name for the input parameter. This name is exposed in the "
            + "REST interface and in the Call Workflow node.")
    String m_parameterName = SubNodeContainer.getDialogNodeParameterNameDefault(JSONInputNodeModel.class);

    @Widget(title = "Append unique ID to parameter name",
        description = "If checked, the name set above will be amended by the node's ID to "
            + "guarantee unique parameter names. Usually it's a good idea to have this box <i>not</i> "
            + "checked and instead make sure to use meaningful and unique names in all container "
            + "nodes present in a workflow.")
    boolean m_useFullyQualifiedName;

    @Widget(title = "Description",
        description = "A description for the input parameter. The description is shown in the "
            + "API specification of the REST interface.")
    @TextAreaWidget(rows = 5)
    String m_description = "";

    @Widget(title = "Default JSON value",
        description = "The text representing a default JSON value. It might contain comments "
            + "between <pre><code>/* */</code></pre> or after <tt>#</tt> or <tt>//</tt> until the end of line.")
    @TextAreaWidget(rows = 10)
    String m_json = "{}";
}
