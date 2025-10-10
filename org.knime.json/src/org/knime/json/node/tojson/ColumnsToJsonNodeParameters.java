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

package org.knime.json.node.tojson;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.StringValue;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.json.JSONCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.StateComputationFailureException;
import org.knime.json.util.RootKeyType;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.array.ArrayWidget;
import org.knime.node.parameters.array.ArrayWidget.ElementLayout;
import org.knime.node.parameters.layout.After;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.migration.ConfigMigration;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.migration.Migration;
import org.knime.node.parameters.migration.NodeParametersMigration;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.persistence.legacy.LegacyColumnFilterPersistor;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.ColumnChoicesProvider;
import org.knime.node.parameters.widget.choices.RadioButtonsWidget;
import org.knime.node.parameters.widget.choices.filter.ColumnFilter;
import org.knime.node.parameters.widget.choices.util.ColumnSelectionUtil;
import org.knime.node.parameters.widget.choices.util.CompatibleColumnsProvider;

/**
 * Node parameters for Columns to JSON.
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.1
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction")
class ColumnsToJsonNodeParameters implements NodeParameters {

    @Section(title = "Root Key Configuration")
    interface RootKeySection {
    }

    @Section(title = "Manual Selection")
    @After(RootKeySection.class)
    interface ManualSelectionSection {
    }

    @Section(title = "Automatic Selection")
    @After(ManualSelectionSection.class)
    interface AutomaticSelectionSection {
    }

    @Section(title = "Custom Key-Value Pairs")
    @After(AutomaticSelectionSection.class)
    interface CustomKeyValueSection {
    }

    @Persist(configKey = ColumnsToJsonSettings.NEW_COLUMN)
    @Widget(title = "New column name", description = "Name of the new JSON column")
    String m_outputColumnName = ColumnsToJsonSettings.DEFAULT_OUTPUT_COLUMN_NAME;

    @Persist(configKey = ColumnsToJsonSettings.REMOVE_SOURCE_COLUMNS)
    @Widget(title = "Remove source columns",
        description = "The source columns include the Data bound key column too (when that is selected)")
    boolean m_removeSourceColumns = ColumnsToJsonSettings.DEFAULT_REMOVE_SOURCE_COLUMNS;

    @Persist(configKey = ColumnsToJsonSettings.ROOT_KEY_TYPE)
    @Layout(RootKeySection.class)
    @Widget(title = "Root key type", description = "How to create the root of the JSON value")
    @RadioButtonsWidget
    @ValueReference(RootKeyTypeReference.class)
    RootKeyType m_rootKeyType = RootKeyType.Unnamed;

    @Persist(configKey = ColumnsToJsonSettings.KEY_NAME)
    @Layout(RootKeySection.class)
    @Widget(title = "Custom key name", description = "Specify the constant key to use for the root object.")
    @Effect(predicate = IsConstantRootKey.class, type = EffectType.SHOW)
    String m_customKey = ColumnsToJsonSettings.DEFAULT_KEY_NAME;

    @Persist(configKey = ColumnsToJsonSettings.KEY_NAME_COLUMN)
    @Layout(RootKeySection.class)
    @Widget(title = "Data bound key column",
        description = "Select the column whose values will be used as the root keys.")
    @ChoicesProvider(StringColumnsProvider.class)
    @Effect(predicate = IsDataBoundRootKey.class, type = EffectType.SHOW)
    @ValueReference(DataBoundKeyColumnReference.class)
    @ValueProvider(value = DataBoundKeyColumnProvider.class)
    String m_dataBoundKeyColumn;

    @Persistor(value = ManualSelectionPersistor.class)
    @Layout(ManualSelectionSection.class)
    @Widget(title = "Manual column selection",
        description = "The predefined key and the actual value in the selected "
            + "columns will provide the keys and values in the object")
    @ArrayWidget(elementLayout = ElementLayout.HORIZONTAL_SINGLE_LINE,
        elementDefaultValueProvider = ManualSelectionDefaultValueProvider.class,
        addButtonText = "Add key/data bound value pair", showSortButtons = false)
    @ValueReference(value = ManualSelectionRef.class)
    KeyColumnPairSettings[] m_manualSelection = new KeyColumnPairSettings[0];

    @Persistor(AutomaticSelectionPersitor.class)
    @Layout(AutomaticSelectionSection.class)
    @Widget(title = "Automatic column selection",
        description = "The keys are the column names and the actual value in"
            + " the selected columns will provide the keys and values in the object")
    @ChoicesProvider(SupportedValueClassesProvider.class)
    ColumnFilter m_automaticSelection = new ColumnFilter();

    @Persistor(KeyValuePairPersistor.class)
    @Layout(CustomKeyValueSection.class)
    @Widget(title = "Custom key/value pairs", description = "Additional key/value pairs within the object")
    @ArrayWidget(elementLayout = ElementLayout.HORIZONTAL_SINGLE_LINE, addButtonText = "Add custom key/value pair",
        showSortButtons = false)
    KeyValuePairSettings[] m_keyValuePairs = new KeyValuePairSettings[0];

    // This configuration key is neither used in the legacy dialog nor in the node model but can be set as flow variable
    @Persist(configKey = ColumnsToJsonSettings.DATA_BOUND_VALUE_TYPE)
    @Migration(value = DataBoundValuesConfigTypeMigration.class)
    int m_dataBoundValueType;

    static final class RootKeyTypeReference implements ParameterReference<RootKeyType> {
    }

    static final class ManualSelectionRef implements ParameterReference<KeyColumnPairSettings[]> {
    }

    static final class DataBoundKeyColumnReference implements ParameterReference<String> {
    }

    static final class IsConstantRootKey implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(RootKeyTypeReference.class).isOneOf(RootKeyType.Constant);
        }

    }

    static final class IsDataBoundRootKey implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(RootKeyTypeReference.class).isOneOf(RootKeyType.DataBound);
        }

    }

    public static class StringColumnsProvider implements ColumnChoicesProvider {

        @Override
        public List<DataColumnSpec> columnChoices(final NodeParametersInput context) {
            final var tableSpec = context.getInTableSpec(0);
            if (tableSpec.isEmpty()) {
                return List.of();
            }
            return tableSpec.get().stream().filter(colSpec ->
                StringCell.TYPE.isASuperTypeOf(colSpec.getType())
                && !JSONCell.TYPE.isASuperTypeOf(colSpec.getType())).toList();
        }

    }

    static final class SupportedValueClassesProvider extends CompatibleColumnsProvider {

        protected SupportedValueClassesProvider() {
            super(Arrays.asList(ColumnsToJsonSettings.supportedValueClasses()));
        }

    }

    static final class DataBoundKeyColumnProvider implements StateProvider<String> {

        private Supplier<String> m_keyColumnSupplier;

        private Supplier<RootKeyType> m_rootKeyTypeSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeAfterOpenDialog();
            m_rootKeyTypeSupplier = initializer.computeFromValueSupplier(RootKeyTypeReference.class);
            m_keyColumnSupplier = initializer.getValueSupplier(DataBoundKeyColumnReference.class);
        }

        @Override
        public String computeState(final NodeParametersInput parametersInput) throws StateComputationFailureException {
            if ((m_keyColumnSupplier.get() != null && !m_keyColumnSupplier.get().isEmpty())
                || m_rootKeyTypeSupplier.get() != RootKeyType.DataBound) {
                return m_keyColumnSupplier.get();
            }

            final var specOpt = parametersInput.getInTableSpec(0);
            if (specOpt.isEmpty()) {
                return null;
            }
            final var spec = specOpt.get();
            return ColumnSelectionUtil.getCompatibleColumns(spec, StringValue.class).stream()
                .map(DataColumnSpec::getName).findFirst().orElse(null);
        }

    }

    static final class ManualSelectionDefaultValueProvider implements StateProvider<KeyColumnPairSettings> {

        private Supplier<KeyColumnPairSettings[]> m_manualSelectionSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeBeforeOpenDialog();
            m_manualSelectionSupplier = initializer.computeFromValueSupplier(ManualSelectionRef.class);
        }

        @Override
        public KeyColumnPairSettings computeState(final NodeParametersInput context)
            throws StateComputationFailureException {
            final var spec = context.getInTableSpec(0);
            if (spec.isEmpty()) {
                return new KeyColumnPairSettings();
            }
            final var alreadySelectedColumns = Arrays.stream(m_manualSelectionSupplier.get()).map(s -> s.m_column)
                .filter(StringUtils::isNotBlank).collect(Collectors.toSet());
            final var firstAvailableCol = ColumnSelectionUtil //
                .getCompatibleColumns(spec.get(), Arrays.asList(ColumnsToJsonSettings.supportedValueClasses())) //
                .stream() //
                .filter(colSpec -> !alreadySelectedColumns.contains(colSpec.getName())) //
                .findFirst();
            if (firstAvailableCol.isEmpty()) {
                return new KeyColumnPairSettings();
            }
            final var firstColName = firstAvailableCol.get().getName();
            return new KeyColumnPairSettings(firstColName, firstColName);
        }

    }

    static final class ManualSelectionPersistor implements NodeParametersPersistor<KeyColumnPairSettings[]> {

        @Override
        public KeyColumnPairSettings[] load(final NodeSettingsRO settings) throws InvalidSettingsException {
            String[] keys = settings.getStringArray(ColumnsToJsonSettings.DATA_BOUND_KEY_NAMES, new String[0]);
            String[] columns = settings.getStringArray(ColumnsToJsonSettings.DATA_BOUND_KEY_COLUMNS, new String[0]);
            if (keys.length != columns.length) {
                throw new InvalidSettingsException("The number of keys (" + keys.length
                    + ") does not match the number of columns (" + columns.length + ").");
            }
            return IntStream.range(0, keys.length) //
                .mapToObj(i -> new KeyColumnPairSettings(keys[i] == null ? columns[i] : keys[i], columns[i])) //
                .toArray(KeyColumnPairSettings[]::new);
        }

        @Override
        public void save(final KeyColumnPairSettings[] obj, final NodeSettingsWO settings) {
            if (obj != null) {
                String[] keys = Arrays.stream(obj).map(kv -> kv.m_key).toArray(String[]::new);
                String[] columns = Arrays.stream(obj).map(kv -> kv.m_column).toArray(String[]::new);
                settings.addStringArray(ColumnsToJsonSettings.DATA_BOUND_KEY_NAMES, keys);
                settings.addStringArray(ColumnsToJsonSettings.DATA_BOUND_KEY_COLUMNS, columns);
            } else {
                settings.addStringArray(ColumnsToJsonSettings.DATA_BOUND_KEY_NAMES, new String[0]);
                settings.addStringArray(ColumnsToJsonSettings.DATA_BOUND_KEY_COLUMNS, new String[0]);
            }
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{ColumnsToJsonSettings.DATA_BOUND_KEY_NAMES},
                {ColumnsToJsonSettings.DATA_BOUND_KEY_COLUMNS}};
        }

    }

    static final class AutomaticSelectionPersitor extends LegacyColumnFilterPersistor {

        AutomaticSelectionPersitor() {
            super(ColumnsToJsonSettings.DATA_BOUND_VALUE_COLUMNS);
        }

    }

    static class KeyValuePairPersistor implements NodeParametersPersistor<KeyValuePairSettings[]> {

        @Override
        public KeyValuePairSettings[] load(final NodeSettingsRO settings) throws InvalidSettingsException {
            String[] keys = settings.getStringArray(ColumnsToJsonSettings.KEY_NAMES, new String[0]);
            String[] values = settings.getStringArray(ColumnsToJsonSettings.KEY_VALUES, new String[0]);
            if (keys.length != values.length) {
                throw new InvalidSettingsException("The number of keys (" + keys.length
                    + ") does not match the number of values (" + values.length + ").");
            }
            var keyValuePairs = new KeyValuePairSettings[keys.length];
            for (int i = 0; i < keys.length; i++) {
                keyValuePairs[i] = new KeyValuePairSettings(keys[i], values[i]);
            }
            return keyValuePairs;
        }

        @Override
        public void save(final KeyValuePairSettings[] obj, final NodeSettingsWO settings) {
            if (obj != null) {
                String[] keys = Arrays.stream(obj).map(kv -> kv.m_key).toArray(String[]::new);
                String[] values = Arrays.stream(obj).map(kv -> kv.m_value).toArray(String[]::new);
                settings.addStringArray(ColumnsToJsonSettings.KEY_NAMES, keys);
                settings.addStringArray(ColumnsToJsonSettings.KEY_VALUES, values);
            } else {
                settings.addStringArray(ColumnsToJsonSettings.KEY_NAMES, new String[0]);
                settings.addStringArray(ColumnsToJsonSettings.KEY_VALUES, new String[0]);
            }
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{ColumnsToJsonSettings.KEY_NAMES}, {ColumnsToJsonSettings.KEY_VALUES}};
        }

    }

    private static final class DataBoundValuesConfigTypeMigration implements NodeParametersMigration<Integer> {

        @Override
        public List<ConfigMigration<Integer>> getConfigMigrations() {
            return List.of(ConfigMigration.builder(DataBoundValuesConfigTypeMigration::loadLegacy)
                .withDeprecatedConfigPath(ColumnsToJsonSettings.DATA_BOUND_VALUE_TYPE).build());
        }

        private static Integer loadLegacy(final NodeSettingsRO settings) {
            return settings.getInt(ColumnsToJsonSettings.DATA_BOUND_VALUE_TYPE, 0);
        }

    }

}
