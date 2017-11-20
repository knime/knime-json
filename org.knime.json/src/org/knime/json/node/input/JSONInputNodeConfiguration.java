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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
 *   Created on Feb 15, 2015 by wiswedel
 */
package org.knime.json.node.input;

import java.io.IOException;
import java.util.regex.Pattern;

import javax.json.JsonValue;
import javax.json.spi.JsonProvider;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.dialog.DialogNode;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.json.util.JSONUtil;

/**
 * Configuration for the JSON Input node.
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
final class JSONInputNodeConfiguration {
    private JsonValue m_value = JsonProvider.provider().createObjectBuilder().build();
    private String m_parameterName = SubNodeContainer.getDialogNodeParameterNameDefault(JSONInputNodeModel.class);
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
     * Return the current JSON value.
     *
     * @return a JSON value, never <code>null</code>
     */
    JsonValue getValue() {
        return m_value;
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
     * Sets the JSON value.
     *
     * @param value the new JSON value as string
     * @return the updated configuration
     */
    JSONInputNodeConfiguration setValue(final String json) throws InvalidSettingsException {
        try {
            m_value = JSONUtil.parseJSONValue(json);
        } catch (IOException e) {
            throw new InvalidSettingsException("Unable to parse JSON: " + e.getMessage(), e);
        }
        return this;
    }

    /**
     * Sets the parameter name.
     *
     * @param value the new parameter name
     * @param allowLegacyFormat if true it will allow the {@link DialogNode#PARAMETER_NAME_PATTERN_LEGACY} (backward
     *            compatible)
     * @return the updated configuration
     */
    JSONInputNodeConfiguration setParameterName(final String value, final boolean allowLegacyFormat) throws InvalidSettingsException {
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
     * Loads the settings from the given node settings object. Loading will fail if settings are missing or invalid.
     *
     * @param settings a node settings object
     * @return the updated configuration
     * @throws InvalidSettingsException if settings are missing or invalid
     */
    JSONInputNodeConfiguration loadInModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        setParameterName(settings.getString("parameterName"), true);
        setValue(settings.getString("json"));
        setDescription(settings.getString("description", "")); // added in 3.5
        return this;
    }

    /**
     * Loads the settings from the given node settings object. Default values will be used for missing or invalid settings.
     *
     * @param settings a node settings object
     * @return the updated configuration
     */
    JSONInputNodeConfiguration loadInDialog(final NodeSettingsRO settings) {
        try {
            setParameterName(settings.getString("parameterName"), true);
        } catch (InvalidSettingsException e) {
            m_parameterName = SubNodeContainer.getDialogNodeParameterNameDefault(JSONInputNodeModel.class);
        }

        try {
            setValue(settings.getString("json"));
        } catch (InvalidSettingsException e) {
            m_value = JsonProvider.provider().createObjectBuilder().build();
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
    JSONInputNodeConfiguration save(final NodeSettingsWO settings) {
        settings.addString("parameterName", m_parameterName);
        settings.addString("json", m_value.toString());
        settings.addString("description", m_description);
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "\"" + m_parameterName + "\": " + m_value.toString();
    }
}
