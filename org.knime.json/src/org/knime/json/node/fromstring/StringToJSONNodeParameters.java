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

package org.knime.json.node.fromstring;

import org.knime.base.node.util.EnumBooleanPersistor;
import org.knime.core.data.DataColumnSpec;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.RadioButtonsWidget;
import org.knime.node.parameters.widget.choices.util.ColumnSelectionUtil;
import org.knime.node.parameters.widget.choices.util.CompatibleColumnsProvider.StringColumnsProvider;

/**
 * Node parameters for String to JSON.
 *
 * @author Ali Asghar Marvi, KNIME Gmbh, Konstanz, Germany
 */
@LoadDefaultsForAbsentFields
final class StringToJSONNodeParameters implements NodeParameters {

    StringToJSONNodeParameters() {
    }

    StringToJSONNodeParameters(final NodeParametersInput input) {
        m_inputColumn = input.getInTableSpec(0).flatMap(ColumnSelectionUtil::getFirstStringColumn)
            .map(DataColumnSpec::getName).orElse("");
    }

    /**
     * Enum representing whether to replace input column or append new column
     */
    enum OutputColumnMode {
            @Label(value = "Replace input column",
                description = "Replace the input column with the JSON column, keeping its name.")
            REPLACE,

            @Label(value = "Append new column", description = "Add a new column with the JSON values.")
            APPEND
    }

    static final class OutputColumnModePersistor extends EnumBooleanPersistor<OutputColumnMode> {
        OutputColumnModePersistor() {
            super("remove.input.column", OutputColumnMode.class, OutputColumnMode.REPLACE);
        }
    }

    @Widget(title = "Input column", description = "Select the String column containing JSON content.")
    @ChoicesProvider(StringColumnsProvider.class)
    @Persist(configKey = "input.column")
    String m_inputColumn = "";

    @Widget(title = "Output column", description = "Choose whether to replace the input column or append a new column.")
    @RadioButtonsWidget
    @Persistor(OutputColumnModePersistor.class)
    @ValueReference(OutputColumnModeRef.class)
    OutputColumnMode m_outputColumnMode = OutputColumnMode.REPLACE;

    @Widget(title = "New column name", description = "Name of the new JSON column.")
    @Persist(configKey = "new.column.name")
    @Effect(predicate = IsColumnAppend.class, type = EffectType.SHOW)
    String m_newColumnName = "JSON";

    @Widget(title = "Allow comments",
        description = "Allow comments (/* */, // and #) within JSON strings. "
            + "Note: Comments are not part of the JSON specification but are sometimes used.")
    @Persist(configKey = "allow.comments")
    boolean m_allowComments = true;

    @Widget(title = "Fail on error",
        description = "If checked, the node fails when encountering invalid JSON. "
            + "Otherwise, missing values are generated for invalid JSON strings.")
    @Persist(configKey = "fail.on.error")
    boolean m_failOnError = true;

    /**
     * Reference for the output column mode
     */
    interface OutputColumnModeRef extends ParameterReference<OutputColumnMode> {
    }

    /**
     * Predicate to show new column name field when in APPEND mode
     */
    static final class IsColumnAppend implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(OutputColumnModeRef.class).isOneOf(OutputColumnMode.APPEND);
        }
    }
}
