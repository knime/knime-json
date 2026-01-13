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

package org.knime.json.node.combine.row;

import static org.knime.core.webui.node.dialog.defaultdialog.setting.singleselection.RowIDChoice.ROW_ID;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.NominalValue;
import org.knime.core.data.json.JSONValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.setting.singleselection.RowIDChoice;
import org.knime.core.webui.node.dialog.defaultdialog.setting.singleselection.StringOrEnum;
import org.knime.json.node.combine.row.RowCombineSettings.JsonStructure;
import org.knime.json.node.combine.row.RowCombineSettings.ObjectOrArray;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.array.ArrayWidget;
import org.knime.node.parameters.array.ArrayWidget.ElementLayout;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.persistence.legacy.EnumBooleanPersistor;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;
import org.knime.node.parameters.widget.choices.util.ColumnSelectionUtil;
import org.knime.node.parameters.widget.choices.util.CompatibleColumnsProvider;
import org.knime.node.parameters.widget.text.TextInputWidget;
import org.knime.node.parameters.widget.text.TextInputWidgetValidation.PatternValidation.IsNotBlankValidation;

/**
 * Node parameters for JSON Row Combiner.
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.1
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction")
final class RowCombineJsonNodeParameters implements NodeParameters {

    // ====== Constructor

    RowCombineJsonNodeParameters() {
        // Default constructor
    }

    RowCombineJsonNodeParameters(final NodeParametersInput input) {
        m_inputColumn = ColumnSelectionUtil.getCompatibleColumnsOfFirstPort(input, JSONValue.class).stream()
            .map(DataColumnSpec::getName).findFirst().orElse(null);
    }

    // ====== Layout sections

    @Section(title = "JSON Structure")
    interface JsonStructureSection {
    }

    // ====== Parameters

    @Persist(configKey = RowCombineSettings.INPUT_COLUMN)
    @Widget(title = "JSON column", description = "The JSON column providing the values for the array.")
    @ChoicesProvider(JSONValueProvider.class)
    String m_inputColumn;

    @Persist(configKey = RowCombineJsonSettings.NEW_COLUMN)
    @Widget(title = "New column name", description = "Name of the new (single, JSON) column in the output table.")
    @TextInputWidget(patternValidation = IsNotBlankValidation.class)
    String m_newColumn = "JSON"; // see RowCombineJsonSettings.m_newColumn

    @Layout(JsonStructureSection.class)
    @Persist(configKey = RowCombineSettings.OBJECT_OR_ARRAY)
    @Widget(title = "Combine rows as", description = "Configure how to collect the JSON values.")
    @ValueSwitchWidget
    @ValueReference(CollectionTypeRef.class)
    ObjectOrArray m_collectType = RowCombineSettings.DEFAULT_OBJECT_OR_ARRAY;

    @Layout(JsonStructureSection.class)
    @Persistor(ObjectKeyColumnPersistor.class)
    @Widget(title = "Object key column",
        description = "The column providing the keys for the object when collecting into object. "
            + "The rows become an object within another object with the keys specified by "
            + "the selected column's values (will fail if there are duplicates). "
            + "Select \"RowID\" to use the Row IDs as object keys.")
    @ChoicesProvider(NominalColumnsProvider.class)
    @Effect(predicate = ObjectCollectionTypeEffect.class, type = Effect.EffectType.SHOW)
    StringOrEnum<RowIDChoice> m_objectKeyColumn = new StringOrEnum<>(ROW_ID);

    @Layout(JsonStructureSection.class)
    @Persistor(value = JsonStructurePersistor.class)
    @Widget(title = "Place resulting JSON", description = "How to structure the resulting JSON output.")
    @ValueSwitchWidget
    @ValueReference(JsonStructureRef.class)
    JsonStructure m_jsonStructure = JsonStructure.NESTED;

    @Layout(JsonStructureSection.class)
    @Persist(configKey = RowCombineSettings.ROOT_KEY)
    @Widget(title = "Root object key", description = "Key under which to place the resulting JSON.")
    @Effect(predicate = JsonStructureEffect.class, type = Effect.EffectType.SHOW)
    String m_rootKey = RowCombineSettings.DEFAULT_ROOT_KEY;

    @Layout(JsonStructureSection.class)
    @Persistor(KeyValuePairPersistor.class)
    @Widget(title = "Additional properties", description = "Additional key/value pairs to add to the root object.")
    @ArrayWidget(elementLayout = ElementLayout.HORIZONTAL_SINGLE_LINE, addButtonText = "Add property",
        showSortButtons = false)
    @Effect(predicate = JsonStructureEffect.class, type = Effect.EffectType.SHOW)
    KeyValuePairSettings[] m_keyValuePairs = new KeyValuePairSettings[0];

    // ====== References for effects

    interface JsonStructureRef extends ParameterReference<JsonStructure> {
    }

    interface CollectionTypeRef extends ParameterReference<ObjectOrArray> {
    }

    interface ObjectKeyIsRowIDRef extends ParameterReference<Boolean> {
    }

    // ====== State Providers

    static final class JsonStructureEffect implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(JsonStructureRef.class).isOneOf(JsonStructure.NESTED);
        }
    }

    static final class ObjectCollectionTypeEffect implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(CollectionTypeRef.class).isOneOf(ObjectOrArray.Object);
        }
    }

    static final class NominalColumnsProvider extends CompatibleColumnsProvider {
        NominalColumnsProvider() {
            super(NominalValue.class);
        }
    }

    static final class JSONValueProvider extends CompatibleColumnsProvider {
        JSONValueProvider() {
            super(JSONValue.class);
        }
    }

    // ====== Custom Persistors

    static final class JsonStructurePersistor extends EnumBooleanPersistor<JsonStructure> {
        JsonStructurePersistor() {
            super(RowCombineSettings.ADD_ROOT_KEY, JsonStructure.class, JsonStructure.NESTED);
        }
    }

    static final class ObjectKeyColumnPersistor implements NodeParametersPersistor<StringOrEnum<RowIDChoice>> {

        @Override
        public StringOrEnum<RowIDChoice> load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return settings.getBoolean(RowCombineSettings.OBJECT_KEY_ROW_ID) ? new StringOrEnum<>(ROW_ID)
                : new StringOrEnum<>(settings.getString(RowCombineSettings.OBJECT_KEY_COLUMN));
        }

        @Override
        public void save(final StringOrEnum<RowIDChoice> param, final NodeSettingsWO settings) {
            settings.addBoolean(RowCombineSettings.OBJECT_KEY_ROW_ID, param.getEnumChoice().isPresent());
            settings.addString(RowCombineSettings.OBJECT_KEY_COLUMN, param.getStringChoice());
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{RowCombineSettings.OBJECT_KEY_ROW_ID}, {RowCombineSettings.OBJECT_KEY_COLUMN}};
        }
    }
}
