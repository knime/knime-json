/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright by KNIME AG, Zurich, Switzerland
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.com
 * email: contact@knime.com
 * ---------------------------------------------------------------------
 *
 * History
 *   Created on Feb 15, 2015 by wiswedel
 */
package org.knime.json.node.output;

import java.io.IOException;
import java.util.regex.Pattern;

import javax.json.JsonValue;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.json.JSONValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.dialog.DialogNode;
import org.knime.core.node.dialog.InputNode;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.json.util.JSONUtil;

/**
 * Configuration for the JSON Output node.
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
final class JSONOutputConfiguration {
    private String m_parameterName;
    private boolean m_useFQNParamName = false; // added in 4.3
    private String m_jsonColumnName;
    private boolean m_keepOneRowTablesSimple;
    private JsonValue m_exampleJson;
    private String m_description = "";

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
     * Returns the name of the column containing the JSON object.
     *
     * @return a column name
     */
    String getJsonColumnName() {
        return m_jsonColumnName;
    }

    /**
     * Returns whether tables with only one row should be returned as a single JSON object instead of an array with
     * one element.
     *
     * @return <code>true</code> if single-row tables result in a JSON object, <code>false</code> if they should
     * result in an one-element array
     */
    boolean isKeepOneRowTablesSimple() {
        return m_keepOneRowTablesSimple;
    }

    /**
     * Returns the JsonValue which should be used for the "example" field for OpenAPI specification.
     *
     * @return The json value
     */
    JsonValue getExampleJson() {
        return m_exampleJson;
    }

    /**
     * Sets the parameter name.
     *
     * @param value the new parameter name
     * @param allowLegacyFormat if true it will allow the {@link DialogNode#PARAMETER_NAME_PATTERN_LEGACY} (backward
     *            compatible)
     * @return the updated configuration
     */
    JSONOutputConfiguration setParameterName(final String value, final boolean allowLegacyFormat) throws InvalidSettingsException {
        CheckUtils.checkSetting(StringUtils.isNotEmpty(value), "parameter name must not be null or empty");
        Pattern pattern = allowLegacyFormat
                ? DialogNode.PARAMETER_NAME_PATTERN_LEGACY : DialogNode.PARAMETER_NAME_PATTERN;
        CheckUtils.checkSetting(pattern.matcher(value).matches(),
            "Parameter doesn't match pattern - must start with character, followed by other characters, digits, "
                + "or single dashes or underscores:\n  Input: %s\n  Pattern: %s",
            value, pattern.pattern());
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
     * Sets the name of the column containing the JSON object.
     *
     * @param value a column name
     * @return the updated configuration
     */
    JSONOutputConfiguration setJsonColumnName(final String value) throws InvalidSettingsException {
        CheckUtils.checkSetting(StringUtils.isNotEmpty(value), "Column name must not be empty");
        m_jsonColumnName = value;
        return this;
    }

    /**
     * Sets whether tables with only one row should be returned as a single JSON object instead of an array with
     * one element.
     *
     * @param value <code>true</code> if single-row tables result in a JSON object, <code>false</code> if they should
     * result in an one-element array
     * @return the updated configuration
     */
    JSONOutputConfiguration setKeepOneRowTablesSimple(final boolean value) {
        m_keepOneRowTablesSimple = value;
        return this;
    }

    /**
     * Sets the json value for the example field, which is used for OpenAPI specification of the workflow.
     *
     * @return the updated configuration
     */
    JSONOutputConfiguration setExampleJson(final JsonValue value) {
        m_exampleJson = value;
        return this;
    }

    /**
     * Loads the settings from the given node settings object. Loading will fail if settings are missing or invalid.
     *
     * @param settings a node settings object
     * @return the updated configuration
     * @throws InvalidSettingsException if settings are missing or invalid
     */
    JSONOutputConfiguration loadInModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        setParameterName(settings.getString("parameterName"), true);
        setUseFQNParamName(settings.getBoolean("useFullyQualifiedName", true)); // added in 4.3
        setJsonColumnName(settings.getString("jsonColumnName"));
        setKeepOneRowTablesSimple(settings.getBoolean("keepOneRowTablesSimple"));

        final String jsonString = settings.getString("exampleJson", "{}");
        try {
            m_exampleJson = JSONUtil.parseJSONValue(jsonString);
        } catch (IOException e) {
            throw new InvalidSettingsException("Could not load example JSON: " + e.getMessage(), e);
        }

        setDescription(settings.getString("description", "")); // added in 3.5
        return this;
    }

    /**
     * Loads the settings from the given node settings object. Default values will be used for missing or invalid settings.
     *
     * @param settings a node settings object
     * @return the updated configuration
     */
    JSONOutputConfiguration loadInDialog(final NodeSettingsRO settings, final DataTableSpec inSpec) {
        try {
            setParameterName(settings.getString("parameterName"), true);
        } catch (InvalidSettingsException e) {
            m_parameterName = SubNodeContainer.getDialogNodeParameterNameDefault(JSONOutputNodeModel.class);
        }
        setUseFQNParamName(settings.getBoolean("useFullyQualifiedName", false)); // added in 4.3

        String firstJSONCol = null;
        for (DataColumnSpec col : inSpec) {
            if (col.getType().isCompatible(JSONValue.class)) {
                firstJSONCol = col.getName();
                break;
            }
        }
        m_jsonColumnName = settings.getString("jsonColumnName", firstJSONCol);
        DataColumnSpec col = inSpec.getColumnSpec(m_jsonColumnName);
        if (col == null || !col.getType().isCompatible(JSONValue.class)) {
            m_jsonColumnName = firstJSONCol;
        }
        setKeepOneRowTablesSimple(settings.getBoolean("keepOneRowTablesSimple", true));

        final String jsonString = settings.getString("exampleJson", "");
        try {
            m_exampleJson = JSONUtil.parseJSONValue(jsonString);
        } catch (IOException e) {
            // Should always be valid, as not saved otherwise. Therefore must be some other invalid settings.
            m_exampleJson = null;
        }

        setDescription(settings.getString("description", ""));
        return this;
    }

    /**
     * Saves the current configuration to the given settings object.
     *
     * @param settings a settings object
     * @return this object
     */
    JSONOutputConfiguration save(final NodeSettingsWO settings) {
        settings.addString("parameterName", m_parameterName);
        settings.addBoolean("useFullyQualifiedName", m_useFQNParamName); // added in 4.3
        settings.addString("jsonColumnName", m_jsonColumnName);
        settings.addBoolean("keepOneRowTablesSimple", m_keepOneRowTablesSimple);

        if (m_exampleJson != null) {
            final String jsonString = JSONUtil.toPrettyJSONString(m_exampleJson);
            settings.addString("exampleJson", jsonString);
        }

        settings.addString("description", m_description);
        return this;
    }
}
