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

package org.knime.json.node.fromtable;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import org.knime.json.node.fromtable.TableToJsonNodeModel.Direction;
import org.knime.json.node.fromtable.TableToJsonNodeModel.MissingValueHandling;
import org.knime.json.node.fromtable.TableToJsonNodeModel.RowKeyOption;
import org.knime.json.node.jsonpath.util.JsonPathUtils;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.migration.DefaultProvider;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.migration.Migration;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.persistence.legacy.EnumBooleanPersistor;
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
import org.knime.node.parameters.widget.choices.EnumChoicesProvider;
import org.knime.node.parameters.widget.choices.RadioButtonsWidget;
import org.knime.node.parameters.widget.choices.filter.ColumnFilter;
import org.knime.node.parameters.widget.choices.util.CompatibleColumnsProvider;
import org.knime.node.parameters.widget.text.TextInputWidget;

/**
 * Node parameters for Table to JSON.
 *
 * @author Leon Wenzler, KNIME GmbH, Konstanz
 * @author AI Migration Pipeline v1.0
 */
@LoadDefaultsForAbsentFields
class TableToJsonNodeParameters implements NodeParameters {

    static final class JSONColumnsProvider extends CompatibleColumnsProvider {
        JSONColumnsProvider() {
            super(Arrays.asList(JsonPathUtils.supportedInputDataValuesAsArray()));
        }
    }

    @SuppressWarnings("restriction")
    static final class ColumnFilterPersistor extends LegacyColumnFilterPersistor {
        ColumnFilterPersistor() {
            super(TableToJsonSettings.CFGKEY_SELECTED_COLUMNS);
        }
    }

    @Widget(title = "Input columns", description = "The selected columns will be transformed.")
    @ChoicesProvider(JSONColumnsProvider.class)
    @Persistor(ColumnFilterPersistor.class)
    ColumnFilter m_selectedColumns = new ColumnFilter();

    interface RowKeyOptionRef extends ParameterReference<RowKeyOption> {
    }

    /**
     * Choices provider that filters RowKeyOption based on the selected {@link Direction} When Direction is
     * {@link Direction#ColumnsOutside}, the `asKey` option is not available.
     */
    static final class RowKeyOptionChoicesProvider implements EnumChoicesProvider<RowKeyOption> {

        private Supplier<Direction> m_directionSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            EnumChoicesProvider.super.init(initializer);
            m_directionSupplier = initializer.computeFromValueSupplier(DirectionRef.class);
        }

        @Override
        public List<RowKeyOption> choices(final NodeParametersInput input) {
            final var direction = m_directionSupplier.get();
            if (direction == Direction.ColumnsOutside) {
                // when column-oriented is selected, exclude the `asKey` option
                return List.of(RowKeyOption.omit, RowKeyOption.asValue);
            } else {
                // for other directions, all options are available
                return List.of(RowKeyOption.omit, RowKeyOption.asValue, RowKeyOption.asKey);
            }
        }
    }

    /**
     * Value provider that sets a valid {@link RowKeyOption} when {@link Direction} changes. If it is
     * {@link Direction#ColumnsOutside} and current value is `asKey`, change to `omit`.
     */
    static final class RowKeyOptionValueProvider implements StateProvider<RowKeyOption> {

        private Supplier<Direction> m_directionSupplier;

        private Supplier<RowKeyOption> m_rowKeyOptionSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            m_directionSupplier = initializer.computeFromValueSupplier(DirectionRef.class);
            m_rowKeyOptionSupplier = initializer.getValueSupplier(RowKeyOptionRef.class);
        }

        @Override
        public RowKeyOption computeState(final NodeParametersInput context) {
            final var direction = m_directionSupplier.get();
            final var rowKeyOption = m_rowKeyOptionSupplier.get();

            // in case the user switched to the only unsupported combination of settings
            // via the direction, switch the RowKey option to the default value
            if (direction == Direction.ColumnsOutside && rowKeyOption == RowKeyOption.asKey) {
                return TableToJsonSettings.DEFAULT_ROW_KEY_OPTION;
            }
            return rowKeyOption;
        }
    }

    @Widget(title = "Row keys", description = "Configure how row keys should be handled in the JSON output.")
    @ChoicesProvider(RowKeyOptionChoicesProvider.class)
    @ValueProvider(RowKeyOptionValueProvider.class)
    @ValueReference(RowKeyOptionRef.class)
    @Persist(configKey = TableToJsonSettings.CFGKEY_ROW_KEY_OPTION)
    RowKeyOption m_rowKeyOption = TableToJsonSettings.DEFAULT_ROW_KEY_OPTION;

    static final class RowKeyIsAsValue implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            // `asValue` is the option to set a custom value as "RowKey"
            return i.getEnum(RowKeyOptionRef.class).isOneOf(RowKeyOption.asValue);
        }
    }

    @Widget(title = "Row key name", description = "The key name to use when row keys are included as JSON values.")
    @TextInputWidget
    @Persist(configKey = TableToJsonSettings.CFGKEY_ROWKEY_KEY)
    @Effect(type = EffectType.SHOW, predicate = RowKeyIsAsValue.class)
    String m_rowKeyKey = TableToJsonSettings.DEFAULT_ROWKEY_KEY;

    interface ColumnNamesAsPathRef extends ParameterReference<Boolean> {
    }

    @Widget(title = "Column names as paths",
        description = "When enabled, column names are treated as hierarchical paths using the specified separator.")
    @ValueReference(ColumnNamesAsPathRef.class)
    @Persist(configKey = TableToJsonSettings.CFGKEY_COLUMN_NAMES_AS_PATH)
    boolean m_columnNamesAsPath = TableToJsonSettings.DEFAULT_COLUMN_NAMES_AS_PATH;

    static final class ColumnNamesAsPathIsTrue implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getBoolean(ColumnNamesAsPathRef.class).isTrue();
        }
    }

    @Widget(title = "Path separator",
        description = "When column names as paths is enabled, "
            + "this value will be used to find the keys for JSON columns based on the column names.")
    @TextInputWidget
    @Persist(configKey = TableToJsonSettings.CFGKEY_COLUMN_NAME_SEPARATOR)
    @Effect(type = EffectType.SHOW, predicate = ColumnNamesAsPathIsTrue.class)
    String m_columnNameSeparator = TableToJsonSettings.DEFAULT_COLUMN_NAME_SEPARATOR;

    interface DirectionRef extends ParameterReference<Direction> {
    }

    @Widget(title = "Aggregation direction", description = "Configure how to aggregate the selected columns into JSON.")
    @RadioButtonsWidget
    @ValueReference(DirectionRef.class)
    @Persist(configKey = TableToJsonSettings.CFGKEY_DIRECTION)
    Direction m_direction = TableToJsonSettings.DEFAULT_DIRECTION;

    static final class DirectionIsKeepRows implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(DirectionRef.class).isOneOf(Direction.KeepRows);
        }
    }

    @Widget(title = "Remove source columns",
        description = "When checked, the source columns are removed when the rows are kept. "
            + "It has no effect for the other aggregation direction options, when all columns are removed.")
    @Persist(configKey = TableToJsonSettings.CFGKEY_REMOVE_SOURCE_COLUMNS)
    @Effect(type = EffectType.SHOW, predicate = DirectionIsKeepRows.class)
    boolean m_removeSourceColumns = TableToJsonSettings.DEFAULT_REMOVE_SOURCE_COLUMNS;

    interface MissingValueHandlingRef extends ParameterReference<MissingValueHandling> {
    }

    @SuppressWarnings("restriction")
    static final class MissingValueHandlingPersistor extends EnumBooleanPersistor<MissingValueHandling> {
        MissingValueHandlingPersistor() {
            super(TableToJsonSettings.CFGKEY_MISSINGS_ARE_OMITTED, MissingValueHandling.class,
                MissingValueHandling.OMITTED);
        }
    }

    @Widget(title = "Missing values",
        description = "Configure how missing values should be handled in the JSON output.")
    @RadioButtonsWidget
    @ValueReference(MissingValueHandlingRef.class)
    @Persistor(MissingValueHandlingPersistor.class)
    @Migration(LoadAsNullForOldNodes.class)
    MissingValueHandling m_missingValueHandling = MissingValueHandling.OMITTED;

    static class LoadAsNullForOldNodes implements DefaultProvider<MissingValueHandling> {
        @Override
        public MissingValueHandling getDefault() {
            return MissingValueHandling.AS_NULL;
        }
    }

    @Widget(title = "Output column name", description = "Name of the resulting JSON column.")
    @TextInputWidget
    @Persist(configKey = TableToJsonSettings.CFGKEY_OUTPUT_COLUMN_NAME)
    String m_outputColumnName = TableToJsonSettings.DEFAULT_OUTPUT_COLUMN_NAME;

    // this one is only an internal value for backwards compatibility,
    // the field does not expose any widget in the WebUI dialog
    @Persist(configKey = TableToJsonSettings.CFGKEY_BOOLEANS_AS_NUMBERS)
    boolean m_booleansAsNumbers; // false
}
