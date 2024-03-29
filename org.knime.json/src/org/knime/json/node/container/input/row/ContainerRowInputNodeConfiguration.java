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
 * ---------------------------------------------------------------------
 *
 * History
 *   Apr 03, 2018 (Tobias Urhaug): created
 */
package org.knime.json.node.container.input.row;

import java.io.IOException;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.dialog.DialogNode;
import org.knime.core.node.dialog.InputNode;
import org.knime.core.node.util.CheckUtils;
import org.knime.json.node.container.mappers.row.inputhandling.ContainerRowMapperInputHandling;
import org.knime.json.node.container.mappers.row.inputhandling.MissingColumnHandling;
import org.knime.json.node.container.mappers.row.inputhandling.MissingValuesHandling;
import org.knime.json.util.JSONUtil;

import jakarta.json.JsonValue;

/**
 * Configuration for the Container Input (Row) node.
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 */
final class ContainerRowInputNodeConfiguration {

    private static final String DEFAULT_PARAMETER_NAME = "row-input";
    private static final String DEFAULT_DESCRIPTION = "";
    private static final String DEFAULT_INPUT_PATH_OR_URL = null;
    private static final JsonValue DEFAULT_TEMPLATE_ROW = ContainerRowDefaultJsonStructure.asJsonValue();
    private static final boolean DEFAULT_USE_TEMPLATE_AS_SPEC = false;
    private static final MissingValuesHandling DEFAULT_MISSING_VALUES_HANDLING = MissingValuesHandling.ACCEPT;
    private static final MissingColumnHandling DEFAULT_MISSING_COLUMN_HANDLING =
            MissingColumnHandling.FILL_WITH_MISSING_VALUE;
    private static final boolean DEFAULT_APPEND_UNKNOWN_COLUMNS = true;

    private String m_parameterName;
    private boolean m_useFQNParamName = false; // added in 4.3
    private String m_description;
    private String m_inputPathOrUrl;
    private JsonValue m_templateRow;
    private boolean m_useTemplateAsSpec;
    private MissingValuesHandling m_missingValuesHandling;
    private MissingColumnHandling m_missingColumnHandling;
    private boolean m_appendUnkownColumns;

    /**
     * Default constructor
     */
    public ContainerRowInputNodeConfiguration() {
        m_parameterName = DEFAULT_PARAMETER_NAME;
        m_description = DEFAULT_DESCRIPTION;
        m_inputPathOrUrl = DEFAULT_INPUT_PATH_OR_URL;
        m_templateRow = DEFAULT_TEMPLATE_ROW;
        m_useTemplateAsSpec = DEFAULT_USE_TEMPLATE_AS_SPEC;
        m_missingValuesHandling = DEFAULT_MISSING_VALUES_HANDLING;
        m_missingColumnHandling = DEFAULT_MISSING_COLUMN_HANDLING;
        m_appendUnkownColumns = DEFAULT_APPEND_UNKNOWN_COLUMNS;
    }

    /**
     * Returns a user-supplied description for this input node.
     *
     * @return a description, never <code>null</code>
     */
    String getDescription() {
        return m_description;
    }

    /**
     * Sets a user-supplied description for this input node.
     *
     * @param s a description, must not be <code>null</code>
     */
    void setDescription(final String s) {
        m_description = s;
    }

    /**
     * Returns the parameter name.
     *
     * @return the parameter name, never <code>null</code>
     */
    String getParameterName() {
        return m_parameterName;
    }

    /**
     * Sets the parameter name.
     *
     * @param value the new parameter name
     * @param allowLegacyFormat if true it will allow the {@link DialogNode#PARAMETER_NAME_PATTERN_LEGACY} (backward
     *            compatible)
     * @return the updated configuration
     */
    ContainerRowInputNodeConfiguration setParameterName(final String value) throws InvalidSettingsException {
        CheckUtils.checkSetting(StringUtils.isNotEmpty(value), "parameter name must not be null or empty");
        CheckUtils.checkSetting(DialogNode.PARAMETER_NAME_PATTERN.matcher(value).matches(),
            "Parameter doesn't match pattern - must start with character, followed by other characters, digits, "
                + "or single dashes or underscores:\n  Input: %s\n  Pattern: %s",
            value, DialogNode.PARAMETER_NAME_PATTERN.pattern());
        m_parameterName = value;
        return this;
    }

    /** Get value as per {@link #setUseFQNParamName(boolean)}.
     * @return the useFQNParamName
     */
    boolean isUseFQNParamName() {
        return m_useFQNParamName;
    }

    /** Sets property as per {@link InputNode#isUseAlwaysFullyQualifiedParameterName()}.
     * @param useFQNParamName the useFQNParamName to set
     */
    void setUseFQNParamName(final boolean useFQNParamName) {
        m_useFQNParamName = useFQNParamName;
    }

    /**
     * @return the input path or url
     */
    Optional<String> getInputPathOrUrl() {
        return Optional.ofNullable(m_inputPathOrUrl);
    }

    /**
     * @param inputPathOrUrl the fileName to set
     * @throws InvalidSettingsException on empty ("") argument
     */
    void setInputPathOrUrl(final String inputPathOrUrl) throws InvalidSettingsException {
        CheckUtils.checkSetting(inputPathOrUrl == null || StringUtils.isNotEmpty(inputPathOrUrl),
                "Input path \"%s\" must be null or not empty", inputPathOrUrl);
        m_inputPathOrUrl = inputPathOrUrl;
    }

    /**
     * Returns the configured template row.
     *
     * @return the configured template row
     */
    JsonValue getTemplateRow() {
        return m_templateRow;
    }

    /**
     * Sets the configured template row.
     *
     * @param templateRowJson the configured template
     */
    void setTemplateRow(final JsonValue templateRowJson) {
        m_templateRow = templateRowJson;
    }

    /**
     * Returns the use template row as spec flag.
     *
     * @return the use template row as spec flag
     */
    boolean getUseTemplateAsSpec() {
        return m_useTemplateAsSpec;
    }

    /**
     * Sets the use template row as spec flag.
     *
     * @param useTemplateAsSpec the value of the flag
     */
    void setUseTemplateAsSpec(final boolean useTemplateAsSpec) {
        m_useTemplateAsSpec = useTemplateAsSpec;
    }

    /**
     * Loads the settings from the given node settings object. Loading will fail if settings are missing or invalid.
     *
     * @param settings a node settings object
     * @return the updated configuration
     * @throws InvalidSettingsException if settings are missing or invalid
     */
    ContainerRowInputNodeConfiguration loadInModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        setParameterName(settings.getString("parameterName"));
        setUseFQNParamName(settings.getBoolean("useFullyQualifiedName", true)); // added in 4.3
        setDescription(settings.getString("description"));
        setInputPathOrUrl(settings.getString("inputPathOrUrl"));
        setUseTemplateAsSpec(settings.getBoolean("useTemplateAsSpec"));


        String templateRowJsonString = settings.getString("templateRow");
        try {
            JsonValue templateRowJson = JSONUtil.parseJSONValue(templateRowJsonString);
            setTemplateRow(templateRowJson);
        } catch (IOException e) {
            throw new InvalidSettingsException("Configured template row has wrong format.", e);
        }

        String missingValuesHandling = settings.getString("missingValuesHandling");
        setMissingValuesHandling(missingValuesHandling);

        String missingColumnHandling = settings.getString("missingColumnHandling");
        setMissingColumnHandling(missingColumnHandling);

        setAppendUnknownColumns(settings.getBoolean("appendSuperfluousColumns"));

        return this;
    }

    /**
     * Loads the settings from the given node settings object.
     * Default values will be used for missing or invalid settings.
     *
     * @param settings a node settings object
     * @return the updated configuration
     */
    ContainerRowInputNodeConfiguration loadInDialog(final NodeSettingsRO settings) {

        try {
            setParameterName(settings.getString("parameterName", DEFAULT_PARAMETER_NAME));
            setUseFQNParamName(settings.getBoolean("useFullyQualifiedName", false)); // added in 4.3
            setDescription(settings.getString("description", DEFAULT_DESCRIPTION));
            setInputPathOrUrl(settings.getString("inputPathOrUrl", DEFAULT_INPUT_PATH_OR_URL));
            setUseTemplateAsSpec(settings.getBoolean("useTemplateAsSpec", DEFAULT_USE_TEMPLATE_AS_SPEC));

            String missingValuesHandling = settings.getString(
                "missingValuesHandling", DEFAULT_MISSING_VALUES_HANDLING.getName());
            setMissingValuesHandling(missingValuesHandling);

            String missingColumnHandling = settings.getString(
                "missingColumnHandling", DEFAULT_MISSING_COLUMN_HANDLING.getName());
            setMissingColumnHandling(missingColumnHandling);

            setAppendUnknownColumns(
                settings.getBoolean("appendSuperfluousColumns", DEFAULT_APPEND_UNKNOWN_COLUMNS));

        } catch (InvalidSettingsException e) {
            m_parameterName = DEFAULT_PARAMETER_NAME;
            m_inputPathOrUrl = DEFAULT_INPUT_PATH_OR_URL;
        }

        String jsonString = settings.getString("templateRow", DEFAULT_TEMPLATE_ROW.toString());
        try {
            JsonValue jsonValue = JSONUtil.parseJSONValue(jsonString);
            setTemplateRow(jsonValue);
        } catch (IOException  e) {
            setTemplateRow(DEFAULT_TEMPLATE_ROW);
        }

        return this;
    }

    /**
     * Saves the current configuration to the given settings object.
     *
     * @param settings a settings object
     * @return this object
     */
    ContainerRowInputNodeConfiguration save(final NodeSettingsWO settings) {
        settings.addString("parameterName", m_parameterName);
        settings.addBoolean("useFullyQualifiedName", m_useFQNParamName); // added in 4.3
        settings.addString("description", m_description);
        settings.addString("inputPathOrUrl", m_inputPathOrUrl);
        settings.addBoolean("useTemplateAsSpec", m_useTemplateAsSpec);

        if (m_templateRow != null) {
            settings.addString("templateRow", JSONUtil.toPrettyJSONString(m_templateRow));
        }

        settings.addString("missingValuesHandling", m_missingValuesHandling.getName());
        settings.addString("missingColumnHandling", m_missingColumnHandling.getName());
        settings.addBoolean("appendSuperfluousColumns", m_appendUnkownColumns);

        return this;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "\"" + m_parameterName + "\"";
    }

    MissingColumnHandling getMissingColumnHandling() {
        return m_missingColumnHandling;
    }

    void setMissingColumnHandling(final MissingColumnHandling missingColumnHandling) throws InvalidSettingsException {
        m_missingColumnHandling = missingColumnHandling;
    }

    void setMissingColumnHandling(final String missingColumnHandling) throws InvalidSettingsException {
        MissingColumnHandling result = null;
        for (MissingColumnHandling handling : MissingColumnHandling.values()) {
            if (handling.getName().equals(missingColumnHandling)) {
                result = handling;
            }
        }

        if (result == null) {
            throw new InvalidSettingsException("'" + missingColumnHandling +"' is not a missing columns handling.");
        }
        m_missingColumnHandling = result;
    }

    MissingValuesHandling getMissingValuesHandling() {
        return m_missingValuesHandling;
    }

    void setMissingValuesHandling(final MissingValuesHandling missingValuesHandling) throws InvalidSettingsException {
        m_missingValuesHandling = missingValuesHandling;
    }

    void setMissingValuesHandling(final String missingValuesHandling) throws InvalidSettingsException {
        MissingValuesHandling result = null;
        for (MissingValuesHandling handling : MissingValuesHandling.values()) {
            if (handling.getName().equals(missingValuesHandling)) {
                result = handling;
            }
        }

        if (result == null) {
            throw new InvalidSettingsException("'" + missingValuesHandling +"' is not a missing values handling.");
        }
        m_missingValuesHandling = result;
    }

    boolean getAppendUnknownColumns() {
        return m_appendUnkownColumns;
    }


    void setAppendUnknownColumns(final boolean appendSuperfluousColumns) {
        m_appendUnkownColumns = appendSuperfluousColumns;
    }

    /**
     * Creates an input handling object from the relevant properties of this configuration object.
     *
     * @return an input handling object
     */
    public ContainerRowMapperInputHandling createMapperInputHandling() {
        return
            new ContainerRowMapperInputHandling(
                m_missingColumnHandling,
                m_appendUnkownColumns,
                m_missingValuesHandling
            );
    }

}
