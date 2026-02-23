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

package org.knime.json.node.patch.apply;

import java.util.List;
import java.util.Optional;

import org.knime.base.node.util.WebUIDialogUtils;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.json.JSONValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.json.node.util.SingleColumnReplaceOrAddNodeModel;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.updates.legacy.ColumnNameAutoGuessValueProvider;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;
import org.knime.node.parameters.widget.choices.util.ColumnSelectionUtil;
import org.knime.node.parameters.widget.choices.util.CompatibleColumnsProvider;
import org.knime.node.parameters.widget.message.TextMessage;

/**
 * Node parameters for JSON Transformer.
 *
 * @author Jannik Eurich, KNIME GmbH, Berlin, Germany
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
class JSONPatchApplyNodeParameters implements NodeParameters {

    static final String SCRIPT_FIELD_KEY = "jsonPatch";

    @Widget(title = "JSON column", description = "The column containing the JSON values to transform.")
    @ChoicesProvider(JSONColumnsProvider.class)
    @ValueReference(InputColumnRef.class)
    @ValueProvider(InputColumnProvider.class)
    @Persist(configKey = SingleColumnReplaceOrAddNodeModel.INPUT_COLUMN)
    String m_inputColumn;

    @Widget(title = "Remove source column",
        description = "If selected, the source column is removed and only the transformed output column remains.")
    @Persist(configKey = SingleColumnReplaceOrAddNodeModel.REMOVE_SOURCE)
    boolean m_removeSourceColumn = true;

    @Widget(title = "New column", description = "Name of the output JSON column that contains the transformed values.")
    @Persist(configKey = SingleColumnReplaceOrAddNodeModel.NEW_COLUMN_NAME)
    String m_newColumn = "";

    @Widget(title = "Patch type",
        description = "Choose whether to apply a JSON Patch (RFC 6902) or a JSON Merge Patch (RFC 7386).")
    @ValueSwitchWidget
    @Persistor(PatchTypePersistor.class)
    @ValueReference(PatchTypeRef.class)
    PatchType m_patchType = PatchType.PATCH;

    @Widget(title = "Keep original value when 'test' operation fails",
        description = "When enabled, failed 'test' operations keep the original JSON value instead of inserting a "
            + "missing value.")
    @Effect(predicate = IsPatchType.class, type = EffectType.SHOW)
    @Persist(configKey = JSONPatchApplySettings.KEEP_ORIGINAL_WHEN_TEST_FAILS)
    boolean m_keepOriginalWhenTestFails = JSONPatchApplySettings.DEFAULT_KEEP_ORIGINAL_WHEN_TEST_FAILS;

    @Persistor(JsonPatchPersistor.class)
    String m_jsonPatch = JSONPatchApplySettings.DEFAULT_JSON_PATCH;

    @TextMessage(WebUIDialogUtils.FunctionAutoCompletionShortcutInfoMessageProvider.class)
    Void m_textMessage;

    static final class InputColumnRef implements ParameterReference<String> {
    }

    static final class InputColumnProvider extends ColumnNameAutoGuessValueProvider {

        InputColumnProvider() {
            super(InputColumnRef.class);
        }

        @Override
        protected Optional<DataColumnSpec> autoGuessColumn(final NodeParametersInput parametersInput) {
            var compatibleColumns =
                ColumnSelectionUtil.getCompatibleColumnsOfFirstPort(parametersInput, JSONValue.class);
            return compatibleColumns.isEmpty() ? Optional.empty() : Optional.of(compatibleColumns.get(0));
        }
    }

    static final class JSONColumnsProvider extends CompatibleColumnsProvider {
        JSONColumnsProvider() {
            super(List.of(JSONValue.class));
        }
    }

    enum PatchType {
            @Label(value = "Patch",
                description = "Apply a JSON Patch (RFC 6902) with operations like add, remove, replace, move, copy, "
                    + "and test")
            PATCH, @Label(value = "Merge and patch",
                description = "Apply a JSON Merge Patch (RFC 7386) using a partial JSON document")
            MERGE_AND_PATCH;
    }

    static final class PatchTypeRef implements ParameterReference<PatchType> {
    }

    static final class IsPatchType implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(PatchTypeRef.class).isOneOf(PatchType.PATCH);
        }
    }

    static final class PatchTypePersistor implements NodeParametersPersistor<PatchType> {

        @Override
        public PatchType load(final NodeSettingsRO settings) throws InvalidSettingsException {
            var value = settings.getString(JSONPatchApplySettings.PATCH_TYPE, JSONPatchApplySettings.PATCH_OPTION);
            return JSONPatchApplySettings.MERGE_PATCH_OPTION.equals(value) ? PatchType.MERGE_AND_PATCH
                : PatchType.PATCH;
        }

        @Override
        public void save(final PatchType obj, final NodeSettingsWO settings) {
            settings.addString(JSONPatchApplySettings.PATCH_TYPE, obj == PatchType.MERGE_AND_PATCH
                ? JSONPatchApplySettings.MERGE_PATCH_OPTION : JSONPatchApplySettings.PATCH_OPTION);
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{JSONPatchApplySettings.PATCH_TYPE}};
        }
    }

    static final class JsonPatchPersistor implements NodeParametersPersistor<String> {

        @Override
        public String load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return settings.getString(JSONPatchApplySettings.JSON_PATCH, JSONPatchApplySettings.DEFAULT_JSON_PATCH);
        }

        @Override
        public void save(final String param, final NodeSettingsWO settings) {
            settings.addString(JSONPatchApplySettings.JSON_PATCH, param);
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{JSONPatchApplySettings.JSON_PATCH}};
        }
    }
}
