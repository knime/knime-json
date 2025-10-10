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

import java.util.function.Supplier;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.json.JSONValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.StateComputationFailureException;
import org.knime.json.util.OutputType;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.array.ArrayWidget;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.util.ColumnSelectionUtil;
import org.knime.node.parameters.widget.choices.util.CompatibleColumnsProvider;
import org.knime.node.parameters.widget.text.TextInputWidget;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Node parameters for JSON Path.
 *
 * @author Paul Baernreuther, KNIME GmbH, Germany
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
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

    @Section(title = "Output")
    interface OutputSection {
    }

    interface ColumnRef extends ParameterReference<String> {
    }

    @Widget(title = "Source column", description = "The JSON column to select the paths from.")
    @ChoicesProvider(JSONColumnsProvider.class)
    @ValueReference(ColumnRef.class)
    @ValueProvider(LoadFirstJsonColumnIfNoneSelected.class)
    @JsonInclude(JsonInclude.Include.ALWAYS) // for effect below.
    @Persist(configKey = JSONPathSettings.INPUT_COLUMN)
    String m_inputColumn;

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

    static final class JSONColumnsProvider extends CompatibleColumnsProvider {
        JSONColumnsProvider() {
            super(JSONValue.class);
        }
    }

    @Widget(title = "Remove source column", description = "When checked, the source column will be removed.")
    @Persist(configKey = JSONPathSettings.REMOVE_SOURCE)
    boolean m_removeSourceColumn = false;

    @Layout(OutputSection.class)
    @Widget(title = "Output columns", description = """
            Configure the JSON paths to extract from the input column. Each row defines one output column with its
            JSONPath expression, output data type, and whether to return a list of values or paths instead of values.
            Note: The Modern UI does not yet support an interactive JSON path chooser component - you will need to
            manually enter JSONPath expressions.
            """)
    @ArrayWidget(addButtonText = "Add JSONPath", elementTitle = "JSONPath Configuration")
    @Persistor(OutputSettingsPersistor.class)
    JSONPathOutputSetting[] m_outputSettings = new JSONPathOutputSetting[0];

    /**
     * Settings for a single JSON path output configuration.
     */
    static final class JSONPathOutputSetting implements NodeParameters {

        @Widget(title = "Output column name", description = "The name for the output column.")
        @TextInputWidget
        String m_outputColumnName;

        @Widget(title = "JSONPath", description = """
                The JSONPath expression to extract data from the JSON input. Examples:
                • $.book[0] - Select the first book
                • $.book[*].title - Select all book titles
                • $.book[?(@.year>2000)] - Select books published after 2000
                """)
        String m_jsonPath = SingleSetting.DEFAULT_JSON_PATH;

        @Widget(title = "Output type",
            description = """
                    The data type for the output column. When 'Return paths' is enabled, this is automatically set to String.
                    """)
        OutputType m_outputType = SingleSetting.DEFAULT_RETURN_TYPE;

        @Widget(title = "Result is list", description = """
                When enabled, the result will be returned as a list even if only one value is found.
                This is automatically enabled when 'Return paths' is selected.
                """)
        boolean m_resultIsList = SingleSetting.DEFAULT_RESULT_IS_LIST;

        @Widget(title = "Return paths", description = """
                When enabled, returns the JSONPath expressions that lead to the values instead of the values themselves.
                This automatically enables 'Result is list' and sets the output type to String.
                """)
        boolean m_returnPaths = SingleSetting.DEFAULT_RETURN_PATHS;

        void loadSettingsFrom(final NodeSettingsRO settings, final int index) throws InvalidSettingsException {
            String[] paths = settings.getStringArray(SingleSetting.JSON_PATH);
            String[] colNames = settings.getStringArray(SingleSetting.NEW_COLUMN_NAMES);
            String[] outputTypes = settings.getStringArray(SingleSetting.RETURN_TYPE);
            boolean[] resultIsListArray = settings.getBooleanArray(SingleSetting.RESULT_IS_LIST);
            boolean[] returnPaths = settings.getBooleanArray(SingleSetting.RETURN_PATHS);

            m_jsonPath = paths.length > index ? paths[index] : SingleSetting.DEFAULT_JSON_PATH;
            m_outputColumnName = colNames.length > index ? colNames[index] : "result";
            m_resultIsList =
                resultIsListArray.length > index ? resultIsListArray[index] : SingleSetting.DEFAULT_RESULT_IS_LIST;
            m_returnPaths = returnPaths.length > index ? returnPaths[index] : SingleSetting.DEFAULT_RETURN_PATHS;

            if (outputTypes.length > index) {
                try {
                    m_outputType = OutputType.valueOf(outputTypes[index]);
                } catch (IllegalArgumentException e) {
                    m_outputType = SingleSetting.DEFAULT_RETURN_TYPE;
                }
            } else {
                m_outputType = SingleSetting.DEFAULT_RETURN_TYPE;
            }
        }
    }

    private static final class OutputSettingsPersistor implements NodeParametersPersistor<JSONPathOutputSetting[]> {
        @Override
        public JSONPathOutputSetting[] load(final org.knime.core.node.NodeSettingsRO settings)
            throws InvalidSettingsException {
            int n = settings.getStringArray(SingleSetting.RETURN_TYPE, new String[0]).length;
            JSONPathOutputSetting[] outputSettings = new JSONPathOutputSetting[n];
            for (int i = 0; i < n; i++) {
                outputSettings[i] = new JSONPathOutputSetting();
                outputSettings[i].loadSettingsFrom(settings, i);
            }
            return outputSettings;
        }

        @Override
        public void save(final JSONPathOutputSetting[] obj, final org.knime.core.node.NodeSettingsWO settings) {
            String[] paths = new String[obj.length], returnTypes = new String[obj.length],
                    colNames = new String[obj.length];
            boolean[] resultIsListArray = new boolean[obj.length], resultPaths = new boolean[obj.length];
            for (int i = 0; i < obj.length; ++i) {
                JSONPathOutputSetting setting = obj[i];
                colNames[i] = setting.m_outputColumnName;
                paths[i] = setting.m_jsonPath;
                resultIsListArray[i] = setting.m_resultIsList;
                resultPaths[i] = setting.m_returnPaths;
                returnTypes[i] = setting.m_outputType.name();
            }
            settings.addStringArray(SingleSetting.NEW_COLUMN_NAMES, colNames);
            settings.addStringArray(SingleSetting.JSON_PATH, paths);
            settings.addBooleanArray(SingleSetting.RESULT_IS_LIST, resultIsListArray);
            settings.addBooleanArray(SingleSetting.RETURN_PATHS, resultPaths);
            settings.addStringArray(SingleSetting.RETURN_TYPE, returnTypes);
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
