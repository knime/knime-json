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

import org.knime.json.node.fromtable.TableToJsonNodeModel.Direction;
import org.knime.json.node.fromtable.TableToJsonNodeModel.RowKeyOption;
import org.knime.json.node.jsonpath.util.JsonPathUtils;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.migration.DefaultProvider;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.migration.Migration;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.persistence.legacy.LegacyColumnFilterPersistor;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.RadioButtonsWidget;
import org.knime.node.parameters.widget.choices.filter.ColumnFilter;
import org.knime.node.parameters.widget.choices.filter.TwinlistWidget;
import org.knime.node.parameters.widget.choices.util.CompatibleColumnsProvider;
import org.knime.node.parameters.widget.text.TextInputWidget;

/**
 * Node parameters for Table to JSON.
 *
 * @author GitHub Copilot, KNIME AG, AI Migration
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
    @TwinlistWidget(includedLabel = "Selected", excludedLabel = "Available")
    @ChoicesProvider(JSONColumnsProvider.class)
    @Persistor(ColumnFilterPersistor.class)
    ColumnFilter m_selectedColumns = new ColumnFilter();

    @Widget(title = "Row keys", description = "Configure how row keys should be handled in the JSON output.")
    @RadioButtonsWidget
    @ValueReference(RowKeyOptionRef.class)
    @Persist(configKey = TableToJsonSettings.CFGKEY_ROW_KEY_OPTION)
    RowKeyOption m_rowKeyOption = TableToJsonSettings.DEFAULT_ROW_KEY_OPTION;

    interface RowKeyOptionRef extends ParameterReference<RowKeyOption> {
    }

    static final class RowKeyIsAsValue implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(RowKeyOptionRef.class).isOneOf(RowKeyOption.asValue);
        }
    }

    @Widget(title = "Row key name", description = "The key name to use when row keys are included as JSON values.")
    @TextInputWidget
    @Persist(configKey = TableToJsonSettings.CFGKEY_ROWKEY_KEY)
    @Effect(type = EffectType.SHOW, predicate = RowKeyIsAsValue.class)
    String m_rowKeyKey = TableToJsonSettings.DEFAULT_ROWKEY_KEY;

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

    /* ============================= Output Section ================================ */

    @Section(title = "Output")
    interface OutputSection {
    }

    @Widget(title = "Output column name", description = "Name of the resulting JSON column.")
    @TextInputWidget
    @Persist(configKey = TableToJsonSettings.CFGKEY_OUTPUT_COLUMN_NAME)
    @Layout(OutputSection.class)
    String m_outputColumnName = TableToJsonSettings.DEFAULT_OUTPUT_COLUMN_NAME;

    /* ============================= Advanced Settings ============================= */

    interface ColumnNamesAsPathRef extends ParameterReference<Boolean> {
    }

    @Widget(title = "Column names as paths",
        description = "When enabled, column names are treated as hierarchical paths using the specified separator.",
        advanced = true)
    @ValueReference(ColumnNamesAsPathRef.class)
    @Persist(configKey = TableToJsonSettings.CFGKEY_COLUMN_NAMES_AS_PATH)
    boolean m_columnNamesAsPath = TableToJsonSettings.DEFAULT_COLUMN_NAMES_AS_PATH;

    static final class ColumnNamesAsPathIsTrue implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getBoolean(ColumnNamesAsPathRef.class).isTrue();
        }
    }

    @Widget(title = "Path separator", description = "The character used to separate path components in column names.",
        advanced = true)
    @TextInputWidget
    @Persist(configKey = TableToJsonSettings.CFGKEY_COLUMN_NAME_SEPARATOR)
    @Effect(type = EffectType.SHOW, predicate = ColumnNamesAsPathIsTrue.class)
    String m_columnNameSeparator = TableToJsonSettings.DEFAULT_COLUMN_NAME_SEPARATOR;

    @Widget(title = "Missing values are omitted",
        description = "Missing values from the input table do not generate a key in the resulting JSON structure, "
            + "they are omitted completely. Note that in a column-oriented transformation missing cells will still "
            + "be inserted as null values in the column's array to maintain row alignment.",
        advanced = true)
    @Persist(configKey = TableToJsonSettings.CFGKEY_MISSINGS_ARE_OMITTED)
    boolean m_missingsAreOmitted = TableToJsonSettings.DEFAULT_MISSINGS_ARE_OMITTED;

    static final class BooleansAsNumbersMigration implements DefaultProvider<Boolean> {
        @Override
        public Boolean getDefault() {
            // for existing nodes this value is true, for new nodes it is false (AP-5685)
            return false;
        }
    }

    @Migration(BooleansAsNumbersMigration.class)
    boolean m_booleansAsNumbers = true;
}
