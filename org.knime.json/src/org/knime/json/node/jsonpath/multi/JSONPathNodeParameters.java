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

package org.knime.json.node.jsonpath.multi;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.json.JSONCell;
import org.knime.core.data.json.JSONValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.custom.CustomValidation;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.custom.CustomValidationProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.custom.ValidationCallback;
import org.knime.json.node.jsonpath.util.JsonPathUtils;
import org.knime.json.util.OutputType;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.array.ArrayWidget;
import org.knime.node.parameters.layout.After;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.updates.legacy.ColumnNameAutoGuessValueProvider;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.DataTypeChoicesProvider;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;
import org.knime.node.parameters.widget.choices.util.ColumnSelectionUtil;
import org.knime.node.parameters.widget.choices.util.CompatibleColumnsProvider;
import org.knime.node.parameters.widget.text.TextInputWidget;
import org.knime.node.parameters.widget.text.util.ColumnNameValidationUtils.ColumnNameValidation;

import com.jayway.jsonpath.JsonPath;

/**
 * Node parameters for JSON Path.
 *
 * @author Paul Baernreuther, KNIME GmbH, Germany
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction")
class JSONPathNodeParameters implements NodeParameters {

    JSONPathNodeParameters() {
    }

    JSONPathNodeParameters(final NodeParametersInput input) {
        // Auto-select the first JSON column if available
        var tableSpec = input.getInTableSpec(0);
        if (tableSpec.isPresent()) {
            m_inputColumn = ColumnSelectionUtil.getFirstCompatibleColumn(tableSpec.get(), JSONValue.class)
                .map(DataColumnSpec::getName).orElse(null);
        }
    }

    @Section(title = "Input")
    interface InputSection {

    }

    @Section(title = "Outputs")
    @After(InputSection.class)
    interface OutputSection {
    }

    interface InputColumnRef extends ParameterReference<String> {
    }

    @Widget(title = "Source column", description = "The JSON column to select the paths from.")
    @ChoicesProvider(JSONColumnsProvider.class)
    @ValueReference(InputColumnRef.class)
    @ValueProvider(LoadFirstJsonColumnIfNoneSelected.class)
    @Layout(InputSection.class)
    @Persist(configKey = JSONPathSettings.INPUT_COLUMN)
    String m_inputColumn;

    static final class LoadFirstJsonColumnIfNoneSelected extends ColumnNameAutoGuessValueProvider {

        LoadFirstJsonColumnIfNoneSelected() {
            super(InputColumnRef.class);
        }

        @Override
        protected Optional<DataColumnSpec> autoGuessColumn(final NodeParametersInput parametersInput) {
            return ColumnSelectionUtil.getFirstCompatibleColumnOfFirstPort(parametersInput, JSONValue.class);
        }

    }

    static final class JSONColumnsProvider extends CompatibleColumnsProvider {
        JSONColumnsProvider() {
            super(JSONValue.class);
        }
    }

    @Widget(title = "Remove source column", description = "When checked, the source column will be removed.")
    @Persist(configKey = JSONPathSettings.REMOVE_SOURCE)
    @Layout(InputSection.class)
    boolean m_removeSourceColumn;

    @Layout(OutputSection.class)
    @Widget(title = "Output columns", description = """
            Configure the JSON paths to extract from the input column. Each element defines one output
            column from one JSONPath expression.
            """)
    @ArrayWidget(addButtonText = "Add JSONPath", elementTitle = "JSONPath")
    @Persistor(OutputSettingsPersistor.class)
    JSONPathOutputSetting[] m_outputSettings = new JSONPathOutputSetting[0];

    /**
     * Settings for a single JSON path output configuration.
     */
    static final class JSONPathOutputSetting implements NodeParameters {

        JSONPathOutputSetting() {
            // called by the framework
        }

        private JSONPathOutputSetting(final SingleSetting singleSetting) throws InvalidSettingsException {
            m_jsonPath = singleSetting.getJsonPath();
            m_outputColumnName = singleSetting.getNewColumnName();
            m_resultIsList = singleSetting.isResultIsList();
            m_outputMode = singleSetting.isReturnPaths() ? ValueOrPath.PATH : ValueOrPath.VALUE;
            m_outputType = singleSetting.getReturnType().getDataType();
        }

        @Widget(title = "Output column name", description = "The name for the output column.")
        @TextInputWidget(patternValidation = ColumnNameValidation.class)
        String m_outputColumnName;

        @Widget(title = "JSONPath", description = """
                The JSONPath expression to extract data from the JSON input. Examples:
                <ul>
                <li>$.book[0] - Select the first book</li>
                <li>$.book[*].title - Select all book titles</li>
                <li>$.book[?(@.year>2000)] - Select books published after 2000</li>
                </ul>
                """)
        @CustomValidation(JSONPathValidatorProvider.class)
        String m_jsonPath = SingleSetting.DEFAULT_JSON_PATH;

        static final class JSONPathValidatorProvider implements CustomValidationProvider<String> {

            private Supplier<ValueOrPath> m_valueOrPathSupplier;

            private Supplier<Boolean> m_resultIsListSupplier;

            @Override
            public void init(final StateProviderInitializer initializer) {
                initializer.computeAfterOpenDialog();
                m_valueOrPathSupplier = initializer.computeFromValueSupplier(ValueOrPath.Ref.class);
                m_resultIsListSupplier = initializer.computeFromValueSupplier(ResultIsListRef.class);

            }

            @Override
            public ValidationCallback<String> computeValidationCallback(final NodeParametersInput parametersInput) {
                if (resultIsList()) {
                    return JSONPathValidatorProvider::parseToJsonPath;
                } else {
                    return jsonPath -> {
                        final var parsed = parseToJsonPath(jsonPath);
                        if (!parsed.isDefinite()) {
                            throw new InvalidSettingsException("Path is non-definite.");
                        }
                    };
                }

            }

            static JsonPath parseToJsonPath(final String jsonPath) throws InvalidSettingsException {
                // currently, a bug in the JsonPath library requires commas in quotes to be (un)escaped manually, see
                // - AP-10014
                // - https://github.com/json-path/JsonPath/issues/400
                // - https://github.com/json-path/JsonPath/issues/487
                final var escaped = JsonPathUtils.escapeCommas(jsonPath);
                try {
                    return JsonPath.compile(escaped);
                } catch (RuntimeException e) {
                    throw new InvalidSettingsException(e.getMessage());
                }
            }

            /**
             * Path always output result as list. If value is selected, the result is a list if the user selected so.
             *
             * @return true if the result is a list
             */
            boolean resultIsList() {
                return m_valueOrPathSupplier.get() == ValueOrPath.PATH || m_resultIsListSupplier.get();
            }

        }

        @Widget(title = "Output mode", description = """
                Define what the output should contain.
                """)
        @ValueSwitchWidget
        @ValueReference(ValueOrPath.Ref.class)
        ValueOrPath m_outputMode = ValueOrPath.VALUE; // See SingleSetting.DEFAULT_RETURN_PATHS

        enum ValueOrPath {

                @Label(value = "Value", description = "Return the values found at the JSON paths.")
                VALUE, //
                @Label(value = "Path", description = "Return the JSONPath expressions that lead to the "
                    + "values instead of the values themselves.")
                PATH;

            interface Ref extends ParameterReference<ValueOrPath> {
            }

            static final class IsValue implements EffectPredicateProvider {

                @Override
                public EffectPredicate init(final PredicateInitializer i) {
                    return i.getEnum(Ref.class).isOneOf(VALUE);
                }

            }
        }

        @Widget(title = "Output type", description = """
                The data type for the output column.
                """)
        @ChoicesProvider(SupportedDataTypeChoicesProvider.class)
        @Effect(predicate = ValueOrPath.IsValue.class, type = EffectType.SHOW)
        DataType m_outputType = JSONCell.TYPE;

        static final class SupportedDataTypeChoicesProvider implements DataTypeChoicesProvider {

            @Override
            public List<DataType> choices(final NodeParametersInput context) {
                return Arrays.stream(OutputType.values())//
                    .map(OutputType::getDataType)//
                    .collect(Collectors.toList());
            }

        }

        interface ResultIsListRef extends ParameterReference<Boolean> {
        }

        @Widget(title = "Result is list", description = """
                When enabled, the result will be returned as a list value.
                """)
        @Effect(predicate = ValueOrPath.IsValue.class, type = EffectType.SHOW)
        @ValueReference(ResultIsListRef.class)
        boolean m_resultIsList = SingleSetting.DEFAULT_RESULT_IS_LIST;

    }

    private static final class OutputSettingsPersistor implements NodeParametersPersistor<JSONPathOutputSetting[]> {
        @Override
        public JSONPathOutputSetting[] load(final NodeSettingsRO settings) throws InvalidSettingsException {
            int n = settings.getStringArray(SingleSetting.RETURN_TYPE, new String[0]).length;
            JSONPathOutputSetting[] outputSettings = new JSONPathOutputSetting[n];
            for (int i = 0; i < n; i++) {
                final var singleSetting = new SingleSetting();
                singleSetting.loadSettingsFrom(settings, i);
                outputSettings[i] = new JSONPathOutputSetting(singleSetting);
            }
            return outputSettings;
        }

        @Override
        public void save(final JSONPathOutputSetting[] obj, final NodeSettingsWO settings) {
            String[] paths = new String[obj.length], returnTypes = new String[obj.length],
                    colNames = new String[obj.length];
            boolean[] resultIsListArray = new boolean[obj.length], resultPaths = new boolean[obj.length];
            for (int i = 0; i < obj.length; ++i) {
                JSONPathOutputSetting setting = obj[i];
                colNames[i] = setting.m_outputColumnName;
                paths[i] = setting.m_jsonPath;
                resultIsListArray[i] = setting.m_resultIsList;
                resultPaths[i] = setting.m_outputMode == JSONPathOutputSetting.ValueOrPath.PATH;
                returnTypes[i] = toOutputType(setting.m_outputType).name();
            }
            settings.addStringArray(SingleSetting.NEW_COLUMN_NAMES, colNames);
            settings.addStringArray(SingleSetting.JSON_PATH, paths);
            settings.addBooleanArray(SingleSetting.RESULT_IS_LIST, resultIsListArray);
            settings.addBooleanArray(SingleSetting.RETURN_PATHS, resultPaths);
            settings.addStringArray(SingleSetting.RETURN_TYPE, returnTypes);
        }

        static OutputType toOutputType(final DataType dataType) {
            for (OutputType type : OutputType.values()) {
                if (type.getDataType().equals(dataType)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Unsupported data type: " + dataType);
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{//
                {SingleSetting.NEW_COLUMN_NAMES}, //
                {SingleSetting.JSON_PATH}, //
                {SingleSetting.RESULT_IS_LIST}, //
                {SingleSetting.RETURN_PATHS}, //
                {SingleSetting.RETURN_TYPE} //
            };
        }
    }
}
