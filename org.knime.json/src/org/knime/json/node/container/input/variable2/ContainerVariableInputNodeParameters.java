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

package org.knime.json.node.container.input.variable2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.webui.node.dialog.defaultdialog.internal.button.SimpleButtonWidget;
import org.knime.core.webui.node.dialog.defaultdialog.internal.persistence.ArrayPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.internal.persistence.ElementFieldPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.internal.persistence.PersistArray;
import org.knime.core.webui.node.dialog.defaultdialog.internal.persistence.PersistArrayElement;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.PersistWithin;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.StateComputationFailureException;
import org.knime.json.node.container.input.variable2.ContainerVariableInputNodeParameters.TemplateVariable.IsSimpleJsonSpec;
import org.knime.json.node.util.SettingsModelVariables;
import org.knime.json.node.util.SettingsModelVariables.Type;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.array.ArrayWidget;
import org.knime.node.parameters.layout.After;
import org.knime.node.parameters.layout.HorizontalLayout;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.migration.DefaultProvider;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.migration.Migration;
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
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.EnumChoicesProvider;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.RadioButtonsWidget;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;
import org.knime.node.parameters.widget.text.TextAreaWidget;
import org.knime.node.parameters.widget.text.TextInputWidget;
import org.knime.node.parameters.widget.text.TextInputWidgetValidation.PatternValidation;
import org.knime.node.parameters.widget.text.TextInputWidgetValidation.PatternValidation.IsNotBlankValidation;

/**
 * Node parameters for Container Input (Variable).
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction")
class ContainerVariableInputNodeParameters implements NodeParameters {

    @Section(title = "Template Variables")
    @Effect(predicate = RequireSpecificationPredicate.class, type = EffectType.SHOW)
    interface TemplateVariableSection {
    }

    @Widget(title = "Parameter name", description = """
            A name for the variable input parameter (preferably unique). This name is exposed in the REST interface.
            """)
    @TextInputWidget(patternValidation = IsNotBlankValidation.class)
    @Persist(configKey = "parameterName")
    @ValueReference(ParameterNameRef.class)
    String m_parameterName = "variable-input";

    interface ParameterNameRef extends ParameterReference<String> {
    }

    @Widget(title = "Append unique ID to parameter name", description = """
            If checked, the name set above will be amended by the node's ID to guarantee unique parameter names. Usually
             it's a good idea to have this box <i>not</i> checked and instead make sure to use meaningful and unique
             names in all container nodes present in a workflow.
            """)
    @Persist(configKey = "useFullyQualifiedName")
    boolean m_useFullyQualifiedName;

    @Widget(title = "Description", description = """
            A description for the variable input parameter. The description is shown in the API specification of the
            REST interface.
            """)
    @TextAreaWidget(rows = 3)
    @Persist(configKey = "description")
    String m_description = "";

    @Widget(title = "Specification mode",
        description = "Choose whether to accept any input or require input to match template variables specification.")
    @RadioButtonsWidget
    @ValueReference(SpecificationModeRef.class)
    @Persistor(SpecificationModePersistor.class)
    SpecificationMode m_specificationMode = SpecificationMode.ACCEPT_ANY;

    static final class SpecificationModeRef implements ParameterReference<SpecificationMode> {
    }

    @Layout(TemplateVariableSection.class)
    @Widget(title = "Variable loading mode", description = """
            Choose how to load the flow variables from the connected input.
            """)
    @ValueSwitchWidget
    @Effect(predicate = RequireSpecificationAndHasVariableInputPredicate.class, type = EffectType.SHOW)
    @Persistor(MergeVariablesPersistor.class)
    @ValueReference(VariableLoadModeRef.class)
    VariableLoadMode m_variableLoadMode = VariableLoadMode.REPLACE;

    static final class VariableLoadModeRef implements ParameterReference<VariableLoadMode> {
    }

    @Layout(TemplateVariableSection.class)
    @Widget(title = "Set input variables as template", description = """
            Loads variables (that are not global constants) at the variable inports as the new template variables. Only
            supported types will be loaded.
            """)
    @SimpleButtonWidget(ref = LoadVariablesButtonRef.class)
    @Effect(predicate = RequireSpecificationAndHasVariableInputPredicate.class, type = EffectType.SHOW)
    Void m_loadVariables;

    static final class LoadVariablesButtonRef implements ButtonReference {
    }

    @Layout(TemplateVariableSection.class)
    @Widget(title = "Use simplified JSON format", description = """
            This option can only be used if exactly one variable is defined. As mentioned above this will allow
            the external input format to be simpler by using the value directly instead of an object that defines the
            variables as properties.
            E.g. if this option is enabled, the following format is expected in the &#8220;InputParameters&#8221;:
            <pre>
            {
                ...
                "parameter-name": &lt;value&gt;,
                ...
            }
            </pre>
            instead of the object notation
            <pre>
            {
                ...
                "parameter-name": {
                  "variable-name": &lt;value&gt;
                },
                ...
            }
            </pre>
            <i>Note:</i>
            If this option is enabled, the variable will always have the same name as the parameter name without the
            unique ID appended (and thus the same naming restrictions).
            """)
    @Effect(predicate = RequireSpecificationAndOneTemplateVariablePredicate.class, type = EffectType.ENABLE)
    @Persist(configKey = "useSimpleJsonSpec")
    @ValueReference(UseSimpleJsonSpecRef.class)
    @ValueProvider(UseSimpleJsonSpecProvider.class)
    boolean m_useSimpleJsonSpec;

    static final class UseSimpleJsonSpecRef implements ParameterReference<Boolean> {
    }

    @Layout(TemplateVariableSection.class)
    @Widget(title = "Template Variables", description = """
            <p>
            The value of the template variable. It must comply with the type-requirements as described above.
            </p>
            <p>
            <i>Note:</i>
            This value will be used as an output if no external value is present.
            </p>
            """)
    @ArrayWidget(addButtonText = "Add variable", elementTitle = "Variable",
        elementDefaultValueProvider = NewTemplateVariableProvider.class)
    @Effect(predicate = RequireSpecificationPredicate.class, type = EffectType.SHOW)
    @PersistWithin({"variables"})
    @PersistArray(TemplateVariablesArrayPersistor.class)
    @ValueReference(TemplateVariablesRef.class)
    @ValueProvider(LoadVariablesButtonEffectProvider.class)
    TemplateVariable[] m_templateVariables = new TemplateVariable[0];

    static final class TemplateVariablesRef implements ParameterReference<TemplateVariable[]> {
    }

    // Hidden fields for unused legacy settings
    @Persist(configKey = "inputPathOrUrl")
    String m_inputPathOrUrl;

    @Migration(LoadFalseIfAbsent.class)
    @ValueProvider(LoadTrueOnOpenDialog.class)
    boolean m_performNewValidation = true;

    static final class LoadFalseIfAbsent implements DefaultProvider<Boolean> {

        @Override
        public Boolean getDefault() {
            return false;
        }

    }

    static final class LoadTrueOnOpenDialog implements StateProvider<Boolean> {

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeBeforeOpenDialog();
        }

        @Override
        public Boolean computeState(final NodeParametersInput parametersInput) throws StateComputationFailureException {
            return true;
        }

    }

    static final class HasInputFlowVariable implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getConstant(pi -> pi.getInPortSpec(0).isPresent());
        }

    }

    static final class RequireSpecificationPredicate implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(SpecificationModeRef.class).isOneOf(SpecificationMode.REQUIRE_SPECIFICATION);
        }

    }

    static final class RequireSpecificationAndHasVariableInputPredicate implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(SpecificationModeRef.class).isOneOf(SpecificationMode.REQUIRE_SPECIFICATION)
                .and(i.getPredicate(HasInputFlowVariable.class));
        }

    }

    static final class IsExactlyOneTemplateVariable implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return not(i.getArray(TemplateVariablesRef.class).hasMultipleItems()).and(
                i.getArray(TemplateVariablesRef.class).containsElementSatisfying(el -> el.getConstant(npi -> true)));
        }

    }

    static final class RequireSpecificationAndOneTemplateVariablePredicate implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(SpecificationModeRef.class).isOneOf(SpecificationMode.REQUIRE_SPECIFICATION)
                .and(i.getPredicate(IsExactlyOneTemplateVariable.class));
        }

    }

    static final class UseSimpleJsonSpecProvider implements StateProvider<Boolean> {

        Supplier<Boolean> m_useSimpleJsonSpecSupplier;

        Supplier<TemplateVariable[]> m_templateVariableSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            m_templateVariableSupplier = initializer.computeFromValueSupplier(TemplateVariablesRef.class);
            m_useSimpleJsonSpecSupplier = initializer.computeFromValueSupplier(UseSimpleJsonSpecRef.class);
        }

        @Override
        public Boolean computeState(final NodeParametersInput parametersInput) throws StateComputationFailureException {
            final var templateVariables = m_templateVariableSupplier.get();
            if (templateVariables != null && templateVariables.length == 1) {
                throw new StateComputationFailureException();
            }
            if (m_useSimpleJsonSpecSupplier.get()) {
                return false;
            }
            throw new StateComputationFailureException();
        }

    }

    static final class NewTemplateVariableProvider implements StateProvider<TemplateVariable> {

        Supplier<TemplateVariable[]> m_existingVariablesSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            m_existingVariablesSupplier = initializer.computeFromValueSupplier(TemplateVariablesRef.class);
        }

        @Override
        public TemplateVariable computeState(final NodeParametersInput parametersInput)
            throws StateComputationFailureException {
            final var numberOfExistingVariables =
                m_existingVariablesSupplier.get() != null ? m_existingVariablesSupplier.get().length : 0;
            return new TemplateVariable("variable_" + (numberOfExistingVariables + 1),
                SettingsModelVariables.Type.STRING, "");
        }

    }

    static final class LoadVariablesButtonEffectProvider implements StateProvider<TemplateVariable[]> {

        Supplier<VariableLoadMode> m_variableLoadModeSupplier;

        Supplier<TemplateVariable[]> m_templateVariablesSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeOnButtonClick(LoadVariablesButtonRef.class);
            m_variableLoadModeSupplier = initializer.getValueSupplier(VariableLoadModeRef.class);
            m_templateVariablesSupplier = initializer.getValueSupplier(TemplateVariablesRef.class);
        }

        @Override
        public TemplateVariable[] computeState(final NodeParametersInput parametersInput)
            throws StateComputationFailureException {
            // we do not want to load global variables like “knime.workflow” because adding that variable is most likely
            // not wanted and can lead to errors or unexpected behavior down the line
            final var inputVariables = parametersInput
                .getAvailableInputFlowVariables(SettingsModelVariables.Type
                    .toVariableTypes(ContainerVariableInputNodeModel2.SUPPORTED_VARIABLE_TYPES))
                .entrySet().stream().filter(e -> !e.getValue().isGlobalConstant())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            final var loadMode = m_variableLoadModeSupplier.get();
            if (loadMode == VariableLoadMode.REPLACE) {
                return inputVariables.values().stream()
                    .map(variable -> new TemplateVariable(variable.getName(),
                        SettingsModelVariables.Type.getTypeFromVariableType(variable.getVariableType()),
                        variable.getValueAsString()))
                    .toArray(TemplateVariable[]::new);
            }

            // merge existing variables with input variables.
            final var existingVariables = Arrays.asList(m_templateVariablesSupplier.get());
            var newVariables = new ArrayList<TemplateVariable>();
            var encounteredVariableNames = new HashSet<String>();

            for (TemplateVariable existingVariable : existingVariables) {
                if (inputVariables.containsKey(existingVariable.m_name)) {
                    final var inputVar = inputVariables.get(existingVariable.m_name);
                    addInputVarWithNameTo(existingVariable.m_name, inputVar, newVariables);
                    encounteredVariableNames.add(existingVariable.m_name);
                } else {
                    newVariables.add(existingVariable);
                }
            }

            for (var inputVarEntry : inputVariables.entrySet()) {
                if (encounteredVariableNames.contains(inputVarEntry.getKey())) {
                    continue;
                }
                addInputVarWithNameTo(inputVarEntry.getKey(), inputVarEntry.getValue(), newVariables);
            }

            return newVariables.toArray(TemplateVariable[]::new);
        }

        private static void addInputVarWithNameTo(final String name,
            final FlowVariable inputVar, final ArrayList<TemplateVariable> newVariables) {
            final var type = Type.getTypeFromVariableType(inputVar.getVariableType());
            if (type == null) {
                throw new IllegalStateException(
                    "Encountered unknown type while merging variables: " + inputVar.getVariableType().getIdentifier());
            }
            final var valueStr = inputVar.getValueAsString();
            newVariables.add(new TemplateVariable(name, type, valueStr));
        }

    }

    static final class IsSimpleJsonSpecProvider implements StateProvider<Boolean> {

        Supplier<Boolean> m_useSimpleJsonSpecSupplier;

        Supplier<Boolean> m_currentValue;

        @Override
        public void init(final StateProviderInitializer initializer) {
            m_useSimpleJsonSpecSupplier = initializer.computeFromValueSupplier(UseSimpleJsonSpecRef.class);
            m_currentValue = initializer.getValueSupplier(IsSimpleJsonSpec.class);
        }

        @Override
        public Boolean computeState(final NodeParametersInput parametersInput) throws StateComputationFailureException {
            final var nextValue = m_useSimpleJsonSpecSupplier.get();
            final var currentValue = m_currentValue.get();
            if (currentValue == nextValue) {
                throw new StateComputationFailureException();
            }
            return nextValue;
        }

    }

    static final class SpecificationModePersistor extends EnumBooleanPersistor<SpecificationMode> {

        protected SpecificationModePersistor() {
            super("requireSpecification", SpecificationMode.class, SpecificationMode.REQUIRE_SPECIFICATION);
        }

    }

    static final class MergeVariablesPersistor extends EnumBooleanPersistor<VariableLoadMode> {

        protected MergeVariablesPersistor() {
            super("mergeVariables", VariableLoadMode.class, VariableLoadMode.REPLACE);
        }

    }

    static final class TemplateVariablesArrayPersistor implements ArrayPersistor<Integer, TemplateVariable> {

        @Override
        public int getArrayLength(final NodeSettingsRO nodeSettings) {
            final String[] nameStrings = nodeSettings.getStringArray("names", new String[0]);
            return nameStrings.length;
        }

        @Override
        public Integer createElementLoadContext(final int index) {
            return index;
        }

        @Override
        public TemplateVariable createElementSaveDTO(final int index) {
            return new TemplateVariable();
        }

        @Override
        public void save(final List<TemplateVariable> savedElements, final NodeSettingsWO nodeSettings) {
            nodeSettings.addStringArray("supportedTypes",
                Arrays.stream(ContainerVariableInputNodeModel2.SUPPORTED_VARIABLE_TYPES)
                    .map(SettingsModelVariables.Type::getStrRepresentation).toArray(String[]::new));

            String[] typeStrings = new String[savedElements.size()];
            String[] nameStrings = new String[savedElements.size()];
            String[] valStrings = new String[savedElements.size()];

            for (int i = 0; i < savedElements.size(); i++) {
                TemplateVariable element = savedElements.get(i);
                typeStrings[i] = element.m_type.getStrRepresentation();
                nameStrings[i] = element.m_name;
                valStrings[i] = element.m_value;
            }

            nodeSettings.addStringArray("types", typeStrings);
            nodeSettings.addStringArray("names", nameStrings);
            nodeSettings.addStringArray("values", valStrings);
        }

    }

    static final class TemplateVariable implements NodeParameters {

        TemplateVariable() {
            this("variable_1", SettingsModelVariables.Type.STRING, "");
        }

        TemplateVariable(final String name, final SettingsModelVariables.Type type, final String value) {
            m_name = name;
            m_type = type;
            m_value = value;
        }

        @HorizontalLayout
        interface NameTypeLayout {
        }

        @After(NameTypeLayout.class)
        interface ValueLayout {
        }

        @ValueReference(IsSimpleJsonSpec.class)
        @ValueProvider(IsSimpleJsonSpecProvider.class)
        @PersistArrayElement(NoOPPersistor.class)
        boolean m_isSimpleJsonSpec;

        static final class IsSimpleJsonSpec implements BooleanReference {
        }

        @Layout(NameTypeLayout.class)
        @Widget(title = "Variable Name", description = """
                Merges the variables from the inport and variables that are already defined in the template. If a name
                is not already defined, the variable will be appended to the end. Otherwise, the type and value of the
                matching variable will be updated to the ones of the loaded variable.
                """)
        @TextInputWidget(patternValidationProvider = IsUniqueVariableNameValidationProvider.class)
        @ValueReference(TemplateVariableNameRef.class)
        @PersistArrayElement(VariableNamePersistor.class)
        @Effect(predicate = IsSimpleJsonSpec.class, type = EffectType.DISABLE)
        @ValueProvider(UseSimpleJsonProvider.class)
        String m_name = "";

        static final class TemplateVariableNameRef implements ParameterReference<String> {
        }

        @Layout(NameTypeLayout.class)
        @Widget(title = "Type", description = """
                This is the type of the template variable to create. Depending on the type the requirements for the
                variable value may change.
                <br />
                The following four basic data types are supported:
                <br />
                <ul>
                    <li>
                        <i>String:</i>
                        A string of characters. This is the default if a new template variable is created.<br/>
                        The default value is an empty string.
                        <br/><br/>
                        To use this type in an external variable you have to use a JSON string.
                        <br/><br/>
                        <i>Note:</i>
                        The node will inform about a string that is empty or does only contain spaces because this
                        is probably unwanted.
                    </li>
                    <li>
                        <i>Integer:</i>
                        An integer number with possible values from 2&#179;&#185;-1 to -2&#179;&#185;. The value must be
                         a valid number (consisting only of an optional sign (&#8220;+&#8221;/&#8220;-&#8221;) and
                        &#8220;0&#8221;-&#8220;9&#8221;) and be in the range above.
                        <br />
                        If the size of your value exceeds the limits above, you can try to use a <i>Double</i> value
                        instead.
                        <br/>
                        The default value is &#8220;0&#8221;.
                        <br/>
                        <br/>
                        To use this type in an external variable you have to use a JSON number without a decimal point
                        (e.g. &#8220;123&#8221; instead of &#8220;123.0&#8221;).
                        <br/>
                        <i>Note:</i>
                        Using too many digits in the JSON causes the number to be interpreted as a Long instead
                        which is not supported by this node.
                    </li>
                    <li>
                        <i>Double:</i>
                        A floating point decimal number with possible values from around
                        4.9&#183;10&#8315;&#179;&#178;&#8308; to 1.8&#183;10&#179;&#8304;&#8312; in both the positive
                        and negative range.
                        <br />
                        The value must be a valid number (consisting only of an optional sign
                        (&#8220;+&#8221;/&#8220;-&#8221;) and &#8220;0&#8221;-&#8220;9&#8221;). You can specify an
                        exponent    by appending &#8220;e&#8221; followed by the exponent. Apart from a numeric value
                        you can also specify one of the following three (case-sensitive) special values:
                        <i>Infinity</i> for positive infinity, <i>-Infinity</i> for negative infinity and <i>NaN</i> for
                         &#8220;Not a Number&#8221;.
                        <br />
                        If the number is too big or too small, it may be converted into one of the these special values.
                        (You will be warned if this happens).
                        <br />
                        You should keep in mind that you may loose some precision for big values or values that are very
                         close to zero.
                        <br/>
                        The default value is &#8220;0.0&#8221;.
                        <br/>
                        <br/>
                        To use this type in an external variable you have to use a JSON number. Numbers with and without
                        a decimal point are supported.
                    </li>
                    <li>
                        <i>Boolean:</i>
                        A truth value that can be either &#8220;true&#8221; or &#8220;false&#8221;.<br/>
                        The default value is &#8220;false&#8221;.
                        <br/>
                        <br/>
                        To use this type in an external variable you have to use a JSON boolean.
                        <br/>
                        <br/>
                        <i>Note:</i>
                        Any other value will be interpreted as &#8220;false&#8221;.
                    </li>
                </ul>
                """)
        @PersistArrayElement(VariableTypePersistor.class)
        @ChoicesProvider(SupportedFlowVariableTypes.class)
        SettingsModelVariables.Type m_type = SettingsModelVariables.Type.STRING;

        @Layout(ValueLayout.class)
        @Widget(title = "Value", description = """
                <p>
                The value of the template variable. It must comply with the type-requirements as described above.
                </p>
                <p>
                <i>Note:</i>
                This value will be used as an output if no external value is present.
                </p>
                """)
        @PersistArrayElement(VariableValuePersistor.class)
        String m_value = "";

        static final class IsUniqueVariableNameValidationProvider implements StateProvider<PatternValidation> {

            private static final String MATCHES_NOTING_PATTERN = "^(?!.*).*$";

            Supplier<TemplateVariable[]> m_existingVariablesSupplier;

            Supplier<String> m_variableNameSupplier;

            @Override
            public void init(final StateProviderInitializer initializer) {
                m_existingVariablesSupplier = initializer.computeFromValueSupplier(TemplateVariablesRef.class);
                m_variableNameSupplier = initializer.getValueSupplier(TemplateVariableNameRef.class);
            }

            @Override
            public PatternValidation computeState(final NodeParametersInput parametersInput)
                throws StateComputationFailureException {
                final String currentName = m_variableNameSupplier.get();
                final var existingVariables = m_existingVariablesSupplier.get();
                var isDuplicate = false;
                if (existingVariables != null) {
                    final var matchingVariables = Arrays.stream(existingVariables)
                        .filter(variable -> variable.m_name.equals(currentName)).toList();
                    if (matchingVariables.size() > 1) {
                        isDuplicate = true;
                    }
                }
                if (isDuplicate) {
                    return visibleErrorMessage("Name conflict");
                }
                return null;

            }

            private static PatternValidation visibleErrorMessage(final String message) {
                return new PatternValidation() {

                    @Override
                    protected String getPattern() {
                        return MATCHES_NOTING_PATTERN;
                    }

                    @Override
                    public String getErrorMessage() {
                        return message;
                    }

                };
            }

        }

        static final class UseSimpleJsonProvider implements StateProvider<String> {

            Supplier<Boolean> m_isSimpleJsonSpecSupplier;

            Supplier<String> m_variableNameSupplier;

            Supplier<String> m_parameterNameSupplier;

            @Override
            public void init(final StateProviderInitializer initializer) {
                m_isSimpleJsonSpecSupplier = initializer.computeFromValueSupplier(UseSimpleJsonSpecRef.class);
                m_variableNameSupplier = initializer.getValueSupplier(TemplateVariableNameRef.class);
                m_parameterNameSupplier = initializer.computeFromValueSupplier(ParameterNameRef.class);
            }

            @Override
            public String computeState(final NodeParametersInput parametersInput)
                throws StateComputationFailureException {
                final var parameterName = m_parameterNameSupplier.get();
                if (m_isSimpleJsonSpecSupplier.get().equals(Boolean.TRUE)
                    && !parameterName.equals(m_variableNameSupplier.get())) {
                    return parameterName;
                } else {
                    throw new StateComputationFailureException();
                }
            }

        }

        static final class SupportedFlowVariableTypes implements EnumChoicesProvider<SettingsModelVariables.Type> {

            @Override
            public List<Type> choices(final NodeParametersInput context) {
                return Arrays.asList(ContainerVariableInputNodeModel2.SUPPORTED_VARIABLE_TYPES);
            }

        }

        static final class NoOPPersistor implements ElementFieldPersistor<Boolean, Integer, TemplateVariable> {

            @Override
            public Boolean load(final NodeSettingsRO nodeSettings, final Integer loadContext)
                throws InvalidSettingsException {
                return false;
            }

            @Override
            public void save(final Boolean param, final TemplateVariable saveDTO) {
                // no-op
            }

            @Override
            public String[][] getConfigPaths() {
                return new String[][]{{}};
            }

        }

        static final class VariableTypePersistor
            implements ElementFieldPersistor<SettingsModelVariables.Type, Integer, TemplateVariable> {

            @Override
            public SettingsModelVariables.Type load(final NodeSettingsRO nodeSettings, final Integer loadContext)
                throws InvalidSettingsException {
                final String[] typeStrings = nodeSettings.getStringArray("types", new String[0]);

                if (loadContext < typeStrings.length) {
                    return SettingsModelVariables.Type.getFromStringRepresentation(typeStrings[loadContext]);
                }
                return SettingsModelVariables.Type.STRING;
            }

            @Override
            public void save(final SettingsModelVariables.Type param, final TemplateVariable saveDTO) {
                saveDTO.m_type = param;
            }

            @Override
            public String[][] getConfigPaths() {
                return new String[][]{{"types"}};
            }

        }

        static final class VariableNamePersistor implements ElementFieldPersistor<String, Integer, TemplateVariable> {

            @Override
            public String load(final NodeSettingsRO nodeSettings, final Integer loadContext)
                throws InvalidSettingsException {
                final String[] nameStrings = nodeSettings.getStringArray("names", new String[0]);

                if (loadContext < nameStrings.length) {
                    return nameStrings[loadContext];
                }
                return "";
            }

            @Override
            public void save(final String param, final TemplateVariable saveDTO) {
                saveDTO.m_name = param;
            }

            @Override
            public String[][] getConfigPaths() {
                return new String[][]{{"names"}};
            }

        }

        static final class VariableValuePersistor implements ElementFieldPersistor<String, Integer, TemplateVariable> {

            @Override
            public String load(final NodeSettingsRO nodeSettings, final Integer loadContext)
                throws InvalidSettingsException {
                final String[] valStrings = nodeSettings.getStringArray("values", new String[0]);

                if (loadContext < valStrings.length) {
                    return valStrings[loadContext];
                }
                return "";
            }

            @Override
            public void save(final String param, final TemplateVariable saveDTO) {
                saveDTO.m_value = param;
            }

            @Override
            public String[][] getConfigPaths() {
                return new String[][]{{"values"}};
            }

        }

    }

    enum SpecificationMode {

            @Label(value = "Accept any input",
                description = """
                        Accepts any well formed input with an arbitrary amount of variables. The input has to be an object.
                        Each property (key/value) pair represents a variable. The property key defines the variable name
                        (which has to be a valid name; see <i>Name</i>). The property value defines the variable value. The type
                         of the variable will be determined using the JSON type of the property value. For a list of all
                        supported types see <i>Type</i>.
                        """)
            ACCEPT_ANY, //
            @Label(value = "Require input to match template variables specification",
                description = """
                        <p>
                        Only accepts input with that matches the template variables specification.
                        The input has to be an object or a value with a supported type (see <i>Use simplified JSON format</i>).
                        </p>
                        <p>
                        If an object is required, each property (key/value) has to match exactly one of
                        the variables in the specification.
                        The property key has to match the variable name and the JSON type of the property has to
                        match the variable type.
                        </p>
                        <p>
                        If <i>Use simplified JSON format</i> is enabled, a value has to be used instead of an object.
                        The type of the JSON value has to match the type defined by the only variable in the variable
                        specification.
                        </p>
                        """)
            REQUIRE_SPECIFICATION;

    }

    enum VariableLoadMode {

            @Label(value = "Replace", description = """
                    Replaces all variables that are already defined by the variables from the inports.
                    """)
            REPLACE, //
            @Label(value = "Merge",
                description = """
                        Merges the variables from the inport and variables that are already defined in the template. If a name
                        is not already defined, the variable will be appended to the end. Otherwise, the type and value of the
                        matching variable will be updated to the ones of the loaded variable.
                        """)
            MERGE;

    }

}
