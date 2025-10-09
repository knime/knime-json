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

package org.knime.json.node.combine.column;

import java.util.function.Supplier;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.StringValue;
import org.knime.core.data.json.JSONValue;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.TypedStringFilterWidgetInternal;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.StateComputationFailureException;
import org.knime.json.util.RootKeyType;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.persistence.legacy.LegacyColumnFilterPersistor;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.RadioButtonsWidget;
import org.knime.node.parameters.widget.choices.filter.ColumnFilter;
import org.knime.node.parameters.widget.choices.util.ColumnSelectionUtil;
import org.knime.node.parameters.widget.choices.util.CompatibleColumnsProvider;

/**
 * Node parameters for JSON Column Combiner.
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.1
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction")
class ColumnCombineJsonNodeParameters implements NodeParameters {

    @Persist(configKey = ColumnCombineJsonSettings.NEW_COLUMN)
    @Widget(title = "New column name", description = "Name of the new JSON column")
    String m_newColumn = ColumnCombineJsonSettings.DEFAULT_NEW_COLUMN;

    @Persist(configKey = ColumnCombineJsonSettings.REMOVE_SOURCE_COLUMNS)
    @Widget(title = "Remove source columns", description = "When checked, the included columns and the data bound "
        + "key/value columns will be removed from the resulting table")
    boolean m_removeSourceColumns;

    @Persist(configKey = ColumnCombineJsonSettings.ROOT_KEY_TYPE)
    @Widget(title = "Root key type", description = "How to create the root of the JSON value: Unnamed root elements "
        + "generate JSON without a wrapping key, Custom key uses a constant key for all rows, Data bound key uses a "
        + "column value from each row as the key")
    @RadioButtonsWidget
    @ValueReference(RootKeyTypeReference.class)
    RootKeyType m_rootKeyType = RootKeyType.Unnamed;

    @Persist(configKey = ColumnCombineJsonSettings.ROOT_KEY_NAME)
    @Widget(title = "Custom key name",
    description = "The constant key for the array of JSON values selected (Selected JSON columns)")
    @Effect(predicate = IsCustomKeySelected.class, type = Effect.EffectType.SHOW)
    String m_rootKeyName = ColumnCombineJsonSettings.DEFAULT_ROOT_NAME;

    @Persist(configKey = ColumnCombineJsonSettings.KEY_NAME_COLUMN)
    @Widget(title = "Data bound key column", description = "The column name, whose values become the key for the "
        + "array of JSON values selected (Selected JSON columns)")
    @Effect(predicate = IsDataBoundKeySelected.class, type = Effect.EffectType.SHOW)
    @ChoicesProvider(StringColumnsProvider.class)
    @ValueReference(DataBoundKeyColumnReference.class)
    @ValueProvider(value = DataBoundKeyColumnProvider.class)
    String m_keyNameColumn;

    @Persistor(ColumnFilterPersistor.class)
    @Widget(title = "Selected JSON columns",
        description = "The included columns will be combined to a new JSON value for each row")
    @TypedStringFilterWidgetInternal(hideTypeFilter = true)
    @ChoicesProvider(value = JSONColumnsProvider.class)
    ColumnFilter m_selectedColumns = new ColumnFilter();

    static final class RootKeyTypeReference implements ParameterReference<RootKeyType> {
    }

    static final class DataBoundKeyColumnReference implements ParameterReference<String> {
    }

    static final class IsCustomKeySelected implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(RootKeyTypeReference.class).isOneOf(RootKeyType.Constant);
        }

    }

    static final class IsDataBoundKeySelected implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(RootKeyTypeReference.class).isOneOf(RootKeyType.DataBound);
        }

    }

    static final class StringColumnsProvider extends CompatibleColumnsProvider {

        StringColumnsProvider() {
            super(StringValue.class);
        }

    }

    static final class JSONColumnsProvider extends CompatibleColumnsProvider {

        JSONColumnsProvider() {
            super(JSONValue.class);
        }

    }

    static final class ColumnFilterPersistor extends LegacyColumnFilterPersistor {

        ColumnFilterPersistor() {
            super(ColumnCombineJsonNodeModel.COLUMN_FILTER_CONFIG_KEY);
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

}
