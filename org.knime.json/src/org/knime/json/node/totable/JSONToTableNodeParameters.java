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

package org.knime.json.node.totable;

import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.After;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.RadioButtonsWidget;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsPositiveIntegerValidation;
import org.knime.node.parameters.widget.text.TextInputWidget;

/**
 * Node parameters for JSON to Table.
 *
 * @author Marc Lehner, KNIME GmbH, Zurich, Switzerland
 * @author AI Migration Pipeline v1.1
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction")
final class JSONToTableNodeParameters implements NodeParameters {

    @Widget(title = "JSON column", description = "Name of the JSON column to expand.")
    @Persist(configKey = "input.column")
    String m_inputColumn = "";

    @Widget(title = "Remove source column", description = "When checked, the input JSON column is removed.")
    @Persist(configKey = "removeSourceColumn")
    boolean m_removeSourceColumn = JSONToTableSettings.DEFAULT_REMOVE_SOURCE_COLUMN;

    @Section(title = "Output Column Names")
    interface OutputColumnNamesSection {
    }

    @Widget(title = "Column naming strategy", description = """
            Select how to generate output column names from the JSON structure.
            """)
    @RadioButtonsWidget
    @Layout(OutputColumnNamesSection.class)
    @Persist(configKey = "column.name.strategy")
    @ValueReference(ColumnNamePattern.ValueRef.class)
    ColumnNamePattern m_columnNameStrategy = ColumnNamePattern.UniquifiedLeafNames;

    @Widget(title = "Path separator", description = """
            The output column name will be created from the JSONPaths found, separating the parts of the path
            with this value.
            """)
    @TextInputWidget
    @Layout(OutputColumnNamesSection.class)
    @Persist(configKey = "column.segment.separator")
    @Effect(predicate = ColumnNamePattern.IsPathWithSeparator.class, type = EffectType.SHOW)
    String m_separator = JSONToTableSettings.DEFAULT_PATH_SEGMENT_SEPARATOR;

    @Section(title = "Array Handling")
    @After(OutputColumnNamesSection.class)
    interface ArrayHandlingSection {
    }

    @Widget(title = "Array processing", description = """
            Choose how to handle JSON arrays in the input data.
            """)
    @RadioButtonsWidget
    @Layout(ArrayHandlingSection.class)
    @Persist(configKey = "array.handling")
    ArrayHandling m_arrayHandling = ArrayHandling.GenerateCollectionCells;

    @Section(title = "Children Expansion")
    @After(ArrayHandlingSection.class)
    interface ChildrenExpansionSection {
    }

    @Widget(title = "Expansion mode", description = """
            Select which parts of the JSON structure to extract.
            """)
    @RadioButtonsWidget
    @Layout(ChildrenExpansionSection.class)
    @Persist(configKey = "expansion")
    @ValueReference(Expansion.ValueRef.class)
    Expansion m_expansion = Expansion.OnlyLeaves;

    @Widget(title = "Maximum depth level", description = """
            The columns are generated only for paths with length up to this value (inclusive, starting from 1).
            """)
    @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class)
    @Layout(ChildrenExpansionSection.class)
    @Persist(configKey = "up.to.n.levels")
    @Effect(predicate = Expansion.IsOnlyUpTo.class, type = EffectType.SHOW)
    int m_upToNLevel = JSONToTableSettings.DEFAULT_UP_TO_N_LEVELS;

    @Widget(title = "Omit nested objects", description = """
            The nested objects are not included in the output when checked (except when the output column is
            a JSON column). This is sometimes desirable as sub-objects are extracted into separate levels.
            """)
    @Layout(ChildrenExpansionSection.class)
    @Persist(configKey = "omit.nested.objects")
    boolean m_omitNestedObjects = JSONToTableSettings.DEFAULT_OMIT_NESTED_OBJECTS;
}