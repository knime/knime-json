/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright by KNIME.com, Zurich, Switzerland
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

import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.json.JSONValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.dialog.DialogNode;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.SubNodeContainer;

/**
 * Configuration for the JSON Output node.
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
final class JSONOutputConfiguration {
    private String m_parameterName;
    private String m_jsonColumnName;
    private boolean m_keepOneRowTablesSimple;

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
     * Loads the settings from the given node settings object. Loading will fail if settings are missing or invalid.
     *
     * @param settings a node settings object
     * @return the updated configuration
     * @throws InvalidSettingsException if settings are missing or invalid
     */
    JSONOutputConfiguration loadInModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        setParameterName(settings.getString("parameterName"), true);
        setJsonColumnName(settings.getString("jsonColumnName"));
        setKeepOneRowTablesSimple(settings.getBoolean("keepOneRowTablesSimple"));
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
        settings.addString("jsonColumnName", m_jsonColumnName);
        settings.addBoolean("keepOneRowTablesSimple", m_keepOneRowTablesSimple);
        return this;
    }
}
