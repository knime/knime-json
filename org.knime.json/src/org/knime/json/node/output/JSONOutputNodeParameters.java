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

package org.knime.json.node.output;

import java.util.function.Supplier;

import org.knime.core.data.json.JSONValue;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.webui.node.dialog.defaultdialog.internal.button.SimpleButtonWidget;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.StateComputationFailureException;
import org.knime.core.webui.node.dialog.defaultdialog.widget.handler.WidgetHandlerException;
import org.knime.json.util.JSONUtil;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.updates.ButtonReference;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.util.ColumnSelectionUtil;
import org.knime.node.parameters.widget.choices.util.CompatibleColumnsProvider;
import org.knime.node.parameters.widget.text.TextAreaWidget;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Node parameters for Container Output (JSON).
 *
 * @author Paul Baernreuther, KNIME GmbH, Germany
 * @author AI Migration Pipeline v1.1
 */
@LoadDefaultsForAbsentFields
class JSONOutputNodeParameters implements NodeParameters {

    @Widget(title = "Parameter Name",
        description = "A name, which serves as key in the resulting JSON object. This is used to assert "
            + "uniqueness in case multiple output results (nodes) are present in workflow.")
    String m_parameterName = SubNodeContainer.getDialogNodeParameterNameDefault(JSONOutputNodeModel.class);

    @Widget(title = "Append unique ID to parameter name",
        description = "If checked, the name set above will be amended by the node's ID to "
            + "guarantee unique parameter names. Usually it's a good idea to have this box not "
            + "checked and instead make sure to use meaningful and unique names in all container "
            + "nodes present in a workflow.")
    boolean m_useFullyQualifiedName = false;

    interface ColumnRef extends ParameterReference<String> {
    }

    @Widget(title = "JSON Column",
        description = "The column containing the JSON result. All other columns are ignored.")
    @ChoicesProvider(JSONValueColumnChoicesProvider.class)
    @ValueReference(ColumnRef.class)
    @JsonInclude(JsonInclude.Include.ALWAYS) // for effect below.
    @ValueProvider(LoadFirstJsonColumnIfNoneSelected.class)
    String m_jsonColumnName;

    static final class LoadFirstJsonColumnIfNoneSelected implements StateProvider<String> {

        private Supplier<String> m_currentColumnNameSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeAfterOpenDialog();
            m_currentColumnNameSupplier = initializer.getValueSupplier(ColumnRef.class);
        }

        @Override
        public String computeState(final NodeParametersInput parametersInput) throws StateComputationFailureException {
            final var currentColumnName = m_currentColumnNameSupplier.get();
            if (currentColumnName != null && !currentColumnName.isEmpty()) {
                return currentColumnName;
            }
            final var firstJsonColumn = ColumnSelectionUtil
                .getFirstCompatibleColumn(parametersInput.getInTableSpec(0).orElse(null), JSONValue.class);
            if (firstJsonColumn.isPresent()) {
                return firstJsonColumn.get().getName();
            }
            return currentColumnName;
        }

    }

    static final class JSONValueColumnChoicesProvider extends CompatibleColumnsProvider {
        JSONValueColumnChoicesProvider() {
            super(JSONValue.class);
        }
    }

    @Widget(title = "Description",
        description = "A description for the output parameter. The description is shown in the "
            + "API specification of the REST interface.")
    @TextAreaWidget(rows = 5)
    String m_description = "";

    @Widget(title = "Keep single-row tables simple",
        description = "For the special case that the input table contains one row with a "
            + "non-missing JSON value this value is taken as result and not wrapped into a JSON array.")
    @ValueReference(KeepOneRowTablesSimpleRef.class)
    boolean m_keepOneRowTablesSimple = true;

    interface KeepOneRowTablesSimpleRef extends ParameterReference<Boolean> {
    }

    static final class InputTableIsPresentAndColumnIsChosen implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            final var inputCol = i.getString(ColumnRef.class);
            final var columnIsEmpty = or(inputCol.isEqualTo(""), inputCol.isEqualTo(null));
            return i.getConstant(input -> input.getInTable(0).isPresent()).and(not(columnIsEmpty));
        }
    }

    @SimpleButtonWidget(ref = FillExampleRef.class)
    @Effect(predicate = InputTableIsPresentAndColumnIsChosen.class, type = EffectType.ENABLE)
    @Widget(title = "Fill example JSON from input data",
        description = "Fill the example JSON field below with data from the input table. "
            + "This requires that preceding nodes have been executed and that a JSON column is selected above.")
    Void m_fillExampleButton;

    interface FillExampleRef extends ButtonReference {
    }

    static final class FillExampleProvider implements StateProvider<String> {

        private Supplier<String> m_columnNameSupplier;

        private Supplier<Boolean> m_keepOneRowTablesSimpleSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeOnButtonClick(FillExampleRef.class);
            m_columnNameSupplier = initializer.getValueSupplier(ColumnRef.class);
            m_keepOneRowTablesSimpleSupplier = initializer.getValueSupplier(KeepOneRowTablesSimpleRef.class);
        }

        @Override
        public String computeState(final NodeParametersInput parametersInput) throws StateComputationFailureException {
            try {
                final var inputTable = parametersInput.getInTable(0);
                if (inputTable.isEmpty()) {
                    throw new IllegalStateException("No input data available. Execute preceding nodes first.");
                }
                final var table = inputTable.get();
                final var selectedColumnName = m_columnNameSupplier.get();
                final var keepOneRowTablesSimple = m_keepOneRowTablesSimpleSupplier.get();

                if (selectedColumnName == null || selectedColumnName.isEmpty()) {
                    throw new IllegalStateException("No JSON column selected.");
                }
                final var spec = table.getDataTableSpec();
                final var columnIndex = spec.findColumnIndex(selectedColumnName);
                if (columnIndex == -1) {
                    throw new WidgetHandlerException("The selected JSON column is missing.");
                }
                final var jsonValue =
                    JSONOutputNodeModel.readIntoJsonValue(table, false, keepOneRowTablesSimple, columnIndex);
                return JSONUtil.toPrettyJSONString(jsonValue);
            } catch (Exception e) {
                throw new WidgetHandlerException("Failed to extract JSON from input data: " + e.getMessage());
            }
        }

    }

    @Widget(title = "Example",
        description = "A JSON representing an example of what output received by this node may look like. "
            + "Mainly used for populating the \"example\" field of the generated OpenAPI output parameter "
            + "specification, which is presented to end users for documentation purposes. The content of "
            + "this node can be filled from input data (if preceding nodes are executed.)")
    @TextAreaWidget(rows = 10)
    @ValueProvider(FillExampleProvider.class)
    @Persist(configKey = "exampleJson")
    String m_exampleJson = "{}";
}
