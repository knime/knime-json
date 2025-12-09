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

package org.knime.json.node.container.input.table;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.RowIterator;
import org.knime.core.data.container.DataContainer;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.internal.button.SimpleButtonWidget;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.WidgetInternal;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.booleanhelpers.DoNotPersistBoolean;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.StateComputationFailureException;
import org.knime.json.node.container.mappers.ContainerTableMapper;
import org.knime.json.node.container.ui.ContainerTemplateTableConfiguration;
import org.knime.json.util.JSONUtil;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.migration.DefaultProvider;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.migration.Migration;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.persistence.legacy.EnumBooleanPersistor;
import org.knime.node.parameters.updates.ButtonReference;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.updates.util.BooleanReference;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.RadioButtonsWidget;
import org.knime.node.parameters.widget.message.TextMessage;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsPositiveIntegerValidation;
import org.knime.node.parameters.widget.text.TextAreaWidget;
import org.knime.node.parameters.widget.text.TextInputWidget;
import org.knime.node.parameters.widget.text.TextInputWidgetValidation.PatternValidation.IsNotBlankValidation;

import jakarta.json.JsonValue;

/**
 * Node parameters for Container Input (Table).
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@SuppressWarnings("restriction")
@LoadDefaultsForAbsentFields
class ContainerTableInputNodeParameters implements NodeParameters {

    @Widget(title = "Parameter name", description = """
            A name for the input parameter (preferably unique). This name is exposed in the REST interface and
            in the Call Workflow (Table Based) node.
            """)
    @TextInputWidget(patternValidation = IsNotBlankValidation.class)
    @Persist(configKey = ContainerTableInputNodeConfiguration.CFG_PARAMETER_NAME)
    String m_parameterName = "table-input";

    @Widget(title = "Append unique ID to parameter name", description = """
            If checked, the name set above will be amended by the node's ID to guarantee unique parameter names.
            Usually it's a good idea to have this box not checked and instead make sure to use meaningful and unique
            names in all container nodes present in a workflow.
            """)
    @Persist(configKey = ContainerTableInputNodeConfiguration.CFG_USE_FULLY_QUALIFIED_NAME)
    @Migration(UseFullyQualifiedNameMigration.class)
    boolean m_useFullyQualifiedName;

    static final class UseFullyQualifiedNameMigration implements DefaultProvider<Boolean> {

        @Override
        public Boolean getDefault() {
            return true;
        }

    }

    @Widget(title = "Description", description = """
            A description for the input parameter. The description is shown in the API specification of the
            REST interface.
            """)
    @TextAreaWidget(rows = 3)
    @Persist(configKey = ContainerTableInputNodeConfiguration.CFG_DESCRIPTION)
    String m_description = "";

    @TextMessage(SetInputTableButtonMessageProvider.class)
    Void m_setInputTableButtonSummary;

    @Widget(title = "Set input table as template", description = """
            By selecting this button, the input table, given one is provided, will be set as the new template table.
            <br/><br/>
            A template table can be used to define a table structure which allows the workflow to execute properly.
            The main purpose of the template table is to populate the "InputParameters" field of the generated OpenAPI
            specification, which is presented to end users for documentation purposes. When a node is executed without
            receiving any external input (over REST or from the <i>Call Workflow (Table Based)</i> node) and it has no
            table connected to its optional input port, the template is output. This allows downstream nodes
            to be configured when no external input is present and simplifies making adjustments to the workflow.<br/>
            <br/>
            The template table also serves as a table spec when a simplified external input without table spec is
            received over the REST API. When receiving an input without table spec, the input will
            be parsed according to the spec defined by the template table, i.e. each input row must contain as many
            cells as columns in the template table and each cell must contain a type that is compatible to the column
            spec, throwing an error in case of any inconsistencies.
            """)
    @SimpleButtonWidget(ref = SetInputTableButtonRef.class)
    @Effect(predicate = TemplateMatchesInputTableRef.class, type = EffectType.DISABLE)
    Void m_setInputTable;

    static final class SetInputTableButtonRef implements ButtonReference {
    }

    @Widget(title = "Save new template", description = """
            This option is automatically set when the user clicks on the \"Set input table as template\" button
            requiring the node to be reconfigured. Manual changes to this setting are ignored.
            """)
    @WidgetInternal(hideControlInNodeDescription = "This is a helper setting to make the dialog dirty.")
    @Persistor(DoNotPersistBoolean.class)
    @Effect(predicate = MakeDialogDirtyInitialValue.class, type = EffectType.SHOW)
    @ValueReference(MakeDialogDirty.class)
    @ValueProvider(MakeDialogDirtyProvider.class)
    boolean m_makeDialogDirty;

    static final class MakeDialogDirty implements BooleanReference {
    }

    @Widget(title = "Template table rows to use", description = """
            Choose how many rows of the template table to use.
            """)
    @RadioButtonsWidget
    @Persistor(TemplateTableOptionsPersistor.class)
    @ValueReference(TemplateTableRowsOptionRef.class)
    @Effect(predicate = HasInputTable.class, type = EffectType.ENABLE)
    TemplateTableRowsOption m_templateTableRowsOption = TemplateTableRowsOption.USE_ENTIRE_TABLE;

    static final class TemplateTableRowsOptionRef implements ParameterReference<TemplateTableRowsOption> {
    }

    @Widget(title = "Number of rows", description = """
            When selected, only the first n rows are used as the template table. Can be especially useful
            when the input table serves as an example in the OpenAPI specification and you want to avoid over
            specifying the example with too many rows.
            """)
    @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class)
    @Effect(predicate = UseOnlyFirstRows.class, type = EffectType.SHOW)
    @Persist(configKey = ContainerTemplateTableConfiguration.CFG_NUMBER_OF_ROWS)
    @ValueReference(NumberOfRowsRef.class)
    int m_numberOfRows = ContainerTemplateTableConfiguration.DEFAULT_NUMBER_OF_ROWS;

    static final class NumberOfRowsRef implements ParameterReference<Integer> {
    }

    @Widget(title = "Omit table spec in API definition", description = """
            When selected, only the data part of the template table is exposed to the OpenAPI definition,
            showing a well formed simplified example input. Select this if the workflow is expected to be
            consumed over REST and the caller of the workflow prefers the simplified input format.
            """)
    @Persist(configKey = ContainerTemplateTableConfiguration.CFG_OMIT_TABLE_SPEC)
    boolean m_omitTableSpec = ContainerTemplateTableConfiguration.DEFAULT_OMIT_TABLE_SPEC;

    // Hidden field for now until a table preview is implemented for the web-ui.
    @Persistor(TemplateTablePersistor.class)
    @ValueProvider(SetAsInputTableProvider.class)
    @ValueReference(TemplateTableRef.class)
    String m_templateTable = ContainerTemplateTableConfiguration.DEFAULT_TEMPLATE_STRING;

    static final class TemplateTableRef implements ParameterReference<String> {
    }

    @Persistor(DoNotPersistString.class)
    @ValueProvider(TrimmedInputTableProvider.class)
    @ValueReference(TrimmedInputTableRef.class)
    String m_trimmedInputTable;

    static final class TrimmedInputTableRef implements ParameterReference<String> {
    }

    @Persistor(DoNotPersistBoolean.class)
    @ValueProvider(TemplateMatchesInputTableProvider.class)
    @ValueReference(TemplateMatchesInputTableRef.class)
    boolean m_templateMatchesInputTable;

    static final class TemplateMatchesInputTableRef implements BooleanReference {
    }

    // Hidden field for unused legacy settings
    @Persist(configKey = ContainerTableInputNodeConfiguration.CFG_INPUT_PATH_OR_URL)
    String m_inputPathOrUrl;

    /**
     * The initial computed value for the change internal dialog state. The effect of m_changeInternalDialogState is
     * based on this setting to reduce the flickering when the user clicks the checkbox. Otherwise, the checkbox would
     * dissapear and reappear on each click. Also, it is needed to detect whether the user clicked the checkbox or not
     * to check the checkbox again.
     */
    @Persistor(DoNotPersistBoolean.class)
    @ValueReference(MakeDialogDirtyInitialValue.class)
    @ValueProvider(MakeDialogDirtyInitialValueProvider.class)
    boolean m_makeDialogDirtyInitialValue;

    static final class MakeDialogDirtyInitialValue implements BooleanReference {
    }

    /**
     * Indicates whether the initialization of the "change internal dialog state" has been done. Needed to detect
     * whether the used clicked the "change internal dialog state" checkbox (as the value provider listens to
     * beforeOpenDialog and to a user change, but we cannot detect that in the provider).
     */
    @Persistor(DoNotPersistBoolean.class)
    @ValueProvider(InitializationDoneProvider.class)
    @ValueReference(InitializationDoneReference.class)
    boolean m_initializationDone;

    static final class InitializationDoneReference implements BooleanReference {
    }

    static final class HasInputTable implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getConstant(pi -> pi.getInPortObject(0).isPresent());
        }

    }

    static final class UseOnlyFirstRows implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(TemplateTableRowsOptionRef.class).isOneOf(TemplateTableRowsOption.USE_ONLY_FIRST_ROWS)
                    .and(i.getPredicate(HasInputTable.class));
        }

    }

    static final class SetInputTableButtonMessageProvider implements StateProvider<Optional<TextMessage.Message>> {

        Supplier<TemplateTableRowsOption> m_templateTableRowsOptionSupplier;

        Supplier<Integer> m_numberOfRowsSupplier;

        Supplier<Boolean> m_templateTableMatchesInputTableSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeBeforeOpenDialog();
            m_templateTableMatchesInputTableSupplier = initializer
                .computeFromProvidedState(TemplateMatchesInputTableProvider.class);
            m_templateTableRowsOptionSupplier = initializer.getValueSupplier(TemplateTableRowsOptionRef.class);
            m_numberOfRowsSupplier = initializer.getValueSupplier(NumberOfRowsRef.class);
        }

        @Override
        public Optional<TextMessage.Message> computeState(final NodeParametersInput parametersInput)
            throws StateComputationFailureException {
            final var inputPortSpecs = parametersInput.getInPortSpecs();
            if (inputPortSpecs.length == 1 && inputPortSpecs[0] == null) {
                return Optional.of(new TextMessage.Message("No input table connected.",
                    "Connect an input table to set it as table template.", TextMessage.MessageType.INFO));
            }

            final var inputPortObjects = parametersInput.getInPortObjects();
            if (inputPortObjects.length == 1 && inputPortObjects[0] == null) {
                return Optional.of(new TextMessage.Message("Execute upstream node first.",
                    "Execute the upstream node first to set it as table template.", TextMessage.MessageType.INFO));
            }

            final var templateTableRowsOpt = m_templateTableRowsOptionSupplier.get();
            final var nrOfRows = m_numberOfRowsSupplier.get();

            if (!m_templateTableMatchesInputTableSupplier.get()) {
                return Optional.of(new TextMessage.Message("Input table is different form configured template table.",
                    "You might want to update the template", TextMessage.MessageType.ERROR));
            }

            if (templateTableRowsOpt == TemplateTableRowsOption.USE_ENTIRE_TABLE) {
                return Optional.of(new TextMessage.Message("The input table is equal to the configured template table.",
                    null, TextMessage.MessageType.INFO));
            }

            return Optional.of(new TextMessage.Message(
                "The trimmed input table (first %s rows) is equal to the configured template table".formatted(nrOfRows),
                null, TextMessage.MessageType.INFO));
        }

    }

    static final class SetAsInputTableProvider implements StateProvider<String> {

        Supplier<String> m_trimmedInputTableSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeOnButtonClick(SetInputTableButtonRef.class);
            m_trimmedInputTableSupplier = initializer.getValueSupplier(TrimmedInputTableRef.class);
        }

        @Override
        public String computeState(final NodeParametersInput parametersInput)
            throws StateComputationFailureException {
            return m_trimmedInputTableSupplier.get();
        }

    }

    static final class MakeDialogDirtyProvider implements StateProvider<Boolean> {

        private Supplier<Boolean> m_makeDialogDirty;

        private Supplier<Boolean> m_makeDialogDirtyInitialValue;

        private Supplier<Boolean> m_initializationDone;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeOnButtonClick(SetInputTableButtonRef.class);
            m_initializationDone = initializer.getValueSupplier(InitializationDoneReference.class);
            m_makeDialogDirty = initializer.computeFromValueSupplier(MakeDialogDirty.class);
            m_makeDialogDirtyInitialValue = initializer.getValueSupplier(MakeDialogDirtyInitialValue.class);
        }

        @Override
        public Boolean computeState(final NodeParametersInput parametersInput) throws StateComputationFailureException {
            if (m_initializationDone.get()) {
                final var changeInternalDialogStateInitialValue = m_makeDialogDirtyInitialValue.get();
                if (m_makeDialogDirty.get() != changeInternalDialogStateInitialValue) {
                    return changeInternalDialogStateInitialValue;
                }
                throw new StateComputationFailureException();
            }

            return true;
        }

    }

    static final class TrimmedInputTableProvider implements StateProvider<String> {

        Supplier<TemplateTableRowsOption> m_templateTableRowsOptionSupplier;

        Supplier<Integer> m_numberOfRowsSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeAfterOpenDialog();
            m_templateTableRowsOptionSupplier = initializer.computeFromValueSupplier(TemplateTableRowsOptionRef.class);
            m_numberOfRowsSupplier = initializer.computeFromValueSupplier(NumberOfRowsRef.class);
        }

        @Override
        public String computeState(final NodeParametersInput parametersInput)
            throws StateComputationFailureException {
            final var inputTableOpt = parametersInput.getInTable(0);
            if (inputTableOpt.isEmpty()) {
                return ContainerTemplateTableConfiguration.DEFAULT_TEMPLATE_STRING;
            }
            final var inputTable = inputTableOpt.get();

            final var templateTableRowsOpt = m_templateTableRowsOptionSupplier.get();
            final var numberOfRows = m_numberOfRowsSupplier.get();

            DataTable dataTable;
            if (templateTableRowsOpt == TemplateTableRowsOption.USE_ONLY_FIRST_ROWS) {
                dataTable = getTrimmedDataTable(inputTable, numberOfRows);
            } else {
                dataTable = inputTable;
            }

            return JSONUtil.toPrettyJSONString(mapToJson(dataTable));
        }

    }

    static final class TemplateMatchesInputTableProvider implements StateProvider<Boolean> {

        Supplier<String> m_templateTableSupplier;

        Supplier<String> m_trimmedInputTableSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            m_templateTableSupplier = initializer.computeFromValueSupplier(TemplateTableRef.class);
            m_trimmedInputTableSupplier = initializer.computeFromValueSupplier(TrimmedInputTableRef.class);
        }

        @Override
        public Boolean computeState(final NodeParametersInput parametersInput) throws StateComputationFailureException {
            if (parametersInput.getInPortObject(0).isEmpty()) {
                return true;
            }
            return Objects.equals(m_templateTableSupplier.get(), m_trimmedInputTableSupplier.get());
        }

    }

    static final class MakeDialogDirtyInitialValueProvider implements StateProvider<Boolean> {

        private Supplier<Boolean> m_changeInternalDialogState;

        @Override
        public void init(final StateProviderInitializer initializer) {
            m_changeInternalDialogState = initializer.computeFromProvidedState(MakeDialogDirtyProvider.class);
        }

        @Override
        public Boolean computeState(final NodeParametersInput parametersInput) throws StateComputationFailureException {
            return m_changeInternalDialogState.get();
        }

    }

    static final class InitializationDoneProvider implements StateProvider<Boolean> {

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeFromProvidedState(MakeDialogDirtyProvider.class);
        }

        @Override
        public Boolean computeState(final NodeParametersInput parametersInput) throws StateComputationFailureException {
            return true;
        }

    }

    static final class TemplateTableOptionsPersistor extends EnumBooleanPersistor<TemplateTableRowsOption> {

        protected TemplateTableOptionsPersistor() {
            super(ContainerTemplateTableConfiguration.CFG_USE_ENTIRE_TABLE, TemplateTableRowsOption.class,
                TemplateTableRowsOption.USE_ENTIRE_TABLE);
        }

    }

    static final class TemplateTablePersistor implements NodeParametersPersistor<String> {

        @Override
        public String load(final NodeSettingsRO nodeSettings) throws InvalidSettingsException {
            String jsonString = nodeSettings.getString(
                ContainerTableInputNodeConfiguration.CFG_CONTAINER_INPUT_TABLE_TEMPLATE,
                ContainerTableDefaultJsonStructure.asString());
            return jsonString != null ? jsonString : ContainerTemplateTableConfiguration.DEFAULT_TEMPLATE_STRING;
        }

        @Override
        public void save(final String param, final NodeSettingsWO nodeSettings) {
            nodeSettings.addString(ContainerTableInputNodeConfiguration.CFG_CONTAINER_INPUT_TABLE_TEMPLATE, param);
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{ContainerTableInputNodeConfiguration.CFG_CONTAINER_INPUT_TABLE_TEMPLATE}};
        }

    }

    static final class DoNotPersistString implements NodeParametersPersistor<String> {

        @Override
        public String load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return null;
        }

        @Override
        public void save(final String obj, final NodeSettingsWO settings) {
            // do nothing
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[0][0];
        }

    }

    enum TemplateTableRowsOption {

        @Label(value = "Use entire input table", description = """
                When selected, the entire input table will be set as the template.
                """)
        USE_ENTIRE_TABLE, //

        @Label(value = "Use only first rows", description = """
                When selected, only the first n rows are used as the template table. Can be especially useful
                when the input table serves as an example in the OpenAPI specification and you want to avoid over
                specifying the example with too many rows.
                """)
        USE_ONLY_FIRST_ROWS;

    }

    private static JsonValue mapToJson(final DataTable dataTable) {
        try {
            return ContainerTableMapper.toContainerTableJsonValueFromDataTable(dataTable);
        } catch (InvalidSettingsException e) {
            throw new RuntimeException("Could not map input table to json", e);
        }
    }

    private static DataTable getTrimmedDataTable(final DataTable dataTable, final int nRows) {
        return getFirstRowsOfTable(nRows, dataTable);
    }

    private static DataTable getFirstRowsOfTable(final int nRows, final DataTable table) {
        DataContainer container = new DataContainer(table.getDataTableSpec());
        RowIterator iterator = table.iterator();
        for (int i = 0; i < nRows; i++) {
            if (iterator.hasNext()) {
                DataRow next = iterator.next();
                container.addRowToTable(next);
            }
        }
        container.close();
        return container.getTable();
    }

}
