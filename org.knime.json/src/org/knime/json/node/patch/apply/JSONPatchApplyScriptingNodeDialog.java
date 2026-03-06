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
 */
package org.knime.json.node.patch.apply;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.knime.base.node.util.ManipulatorProvider;
import org.knime.base.node.util.WebUIDialogUtils;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.webui.node.dialog.scripting.AbstractDefaultScriptingNodeDialog;
import org.knime.core.webui.node.dialog.scripting.GenericInitialDataBuilder;
import org.knime.core.webui.node.dialog.scripting.WorkflowControl;

/**
 * Scripting dialog for the JSON Transformer node with code editor and autocompletion.
 *
 * @author Jannik Eurich, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction")
final class JSONPatchApplyScriptingNodeDialog extends AbstractDefaultScriptingNodeDialog {

    private static final ManipulatorProvider JSON_PATCH_MANIPULATOR_PROVIDER =
        JsonPatchManipulator.MANIPULATOR_PROVIDER;

    JSONPatchApplyScriptingNodeDialog() {
        super(JSONPatchApplyNodeParameters.class);
    }

    //This method is needed to allow correct auto completion in a json format.
    public static StaticCompletionItem[] getCompletionItemsPatch(final WorkflowControl workflowControl,
    final ManipulatorProvider manipulatorProvider, final boolean includeColumns) {
    Set<StaticCompletionItem> items = new HashSet<>();

    // For this node: insert the full JSON template on completion.
    if (manipulatorProvider != null) {
        manipulatorProvider.getCategories().forEach(c ->
            manipulatorProvider.getManipulators(c).forEach(m ->
                items.add(new StaticCompletionItem(
                    m.getDisplayName(), // inserted text
                    null,
                    m.getDescription(),
                    m.getReturnType().getSimpleName()
                ))
            )
        );
    }

    // Reuse default flow-variable + column completions.
    Collections.addAll(items, WebUIDialogUtils.getCompletionItems(workflowControl, null, includeColumns));

    return items.toArray(StaticCompletionItem[]::new);
}

    @Override
    protected GenericInitialDataBuilder getInitialData(final NodeContext context) {
        var workflowControl = new WorkflowControl(context.getNodeContainer());
        return GenericInitialDataBuilder.createDefaultInitialDataBuilder(context) //
            .addDataSupplier(WebUIDialogUtils.DATA_SUPPLIER_KEY_INPUT_OBJECTS,
                () -> WebUIDialogUtils.getFirstInputTableModel(workflowControl)) //
            .addDataSupplier(WebUIDialogUtils.DATA_SUPPLIER_KEY_FLOW_VARIABLES,
                () -> WebUIDialogUtils.getFlowVariablesInputOutputModel(workflowControl)) //
            .addDataSupplier(WebUIDialogUtils.DATA_SUPPLIER_KEY_OUTPUT_OBJECTS, Collections::emptyList) //
            .addDataSupplier(WebUIDialogUtils.DATA_SUPPLIER_KEY_LANGUAGE,
                () -> "json") //
            .addDataSupplier(WebUIDialogUtils.DATA_SUPPLIER_KEY_FILE_NAME,
                () -> "file.json") //
            .addDataSupplier(WebUIDialogUtils.DATA_SUPPLIER_KEY_MAIN_SCRIPT_CONFIG_KEY,
                () -> JSONPatchApplyNodeParameters.SCRIPT_FIELD_KEY) //
            .addDataSupplier(WebUIDialogUtils.DATA_SUPPLIER_KEY_STATIC_COMPLETION_ITEMS,
                () -> getCompletionItemsPatch(workflowControl, JSON_PATCH_MANIPULATOR_PROVIDER, true));
    }
}
