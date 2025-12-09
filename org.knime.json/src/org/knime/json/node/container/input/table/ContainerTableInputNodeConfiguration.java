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
package org.knime.json.node.container.input.table;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.dialog.DialogNode;
import org.knime.core.node.dialog.InputNode;
import org.knime.core.node.util.CheckUtils;
import org.knime.json.node.container.ui.ContainerTemplateTableConfiguration;
import org.knime.json.node.container.ui.ContainerTemplateTablePanel;

import jakarta.json.JsonValue;

/**
 * Configuration for the Container Input (Table) node.
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 */
final class ContainerTableInputNodeConfiguration {

    static final String CFG_CONTAINER_INPUT_TABLE_TEMPLATE = "exampleInput";
    static final String CFG_PARAMETER_NAME = "parameterName";
    static final String CFG_USE_FULLY_QUALIFIED_NAME = "useFullyQualifiedName";
    static final String CFG_DESCRIPTION = "description";
    static final String CFG_INPUT_PATH_OR_URL = "inputPathOrUrl";

    private static final String DEFAULT_PARAMETER_NAME = "table-input";
    private static final String DEFAULT_DESCRIPTION = "";
    private static final String DEFAULT_INPUT_PATH_OR_URL = null;

    private String m_parameterName;
    private boolean m_useFQNParamName = false; // added in 4.3
    private String m_description;
    private String m_inputPathOrUrl;
    private ContainerTemplateTableConfiguration m_templateConfiguration;

    public ContainerTableInputNodeConfiguration() {
        m_parameterName = DEFAULT_PARAMETER_NAME;
        m_description = DEFAULT_DESCRIPTION;
        m_inputPathOrUrl = DEFAULT_INPUT_PATH_OR_URL;
        m_templateConfiguration = new ContainerTemplateTableConfiguration(CFG_CONTAINER_INPUT_TABLE_TEMPLATE);
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
    ContainerTableInputNodeConfiguration setParameterName(final String value) throws InvalidSettingsException {
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
     * Gets the template configuration.
     *
     * @return the template configuration
     */
    ContainerTemplateTableConfiguration getTemplateConfiguration() {
        return m_templateConfiguration;
    }

    /**
     * Sets the template configuration.
     *
     * @param templateConfiguration the template configuration to be set
     */
    void setTemplateConfiguration(final ContainerTemplateTableConfiguration templateConfiguration) {
        m_templateConfiguration = templateConfiguration;
    }

    /**
     * Sets the template configuration based on a {@link ContainerTemplateTablePanel}.
     *
     * @param templateInputPanel the panel from which the configuration should be set
     * @throws InvalidSettingsException if input panel has invalid settings
     */
    void setTemplateConfiguration(final ContainerTemplateTablePanel templateInputPanel)
            throws InvalidSettingsException {
        ContainerTemplateTableConfiguration templateConfig =
                new ContainerTemplateTableConfiguration(CFG_CONTAINER_INPUT_TABLE_TEMPLATE);
        templateConfig.setTemplate(templateInputPanel.getTemplateTableJson());
        templateConfig.setUseEntireTable(templateInputPanel.getUseEntireTable());
        templateConfig.setNumberOfRows(templateInputPanel.getNumberOfRows());
        templateConfig.setOmitTableSpec(templateInputPanel.getOmitTableSpec());
        m_templateConfiguration = templateConfig;
    }

    /**
     * Gets the configured template input.
     *
     * @return the configured template input
     */
    JsonValue getExampleInput() {
        return m_templateConfiguration.getExampleTemplate();
    }

    /**
     * Gets the entire configured template table.
     * @return the configured template table
     */
    public JsonValue getTemplateTable() {
        return m_templateConfiguration.getTemplateTable();
    }

    /**
     * Loads the settings from the given node settings object. Loading will fail if settings are missing or invalid.
     *
     * @param settings a node settings object
     * @return the updated configuration
     * @throws InvalidSettingsException if settings are missing or invalid
     */
    ContainerTableInputNodeConfiguration loadInModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        setParameterName(settings.getString(CFG_PARAMETER_NAME));
        setUseFQNParamName(settings.getBoolean(CFG_USE_FULLY_QUALIFIED_NAME, true)); // added in 4.3
        setDescription(settings.getString(CFG_DESCRIPTION));
        setInputPathOrUrl(settings.getString(CFG_INPUT_PATH_OR_URL));
        setTemplateConfiguration(
            new ContainerTemplateTableConfiguration(CFG_CONTAINER_INPUT_TABLE_TEMPLATE).loadInModel(settings));
        return this;
    }

    /**
     * Loads the settings from the given node settings object.
     * Default values will be used for missing or invalid settings.
     *
     * @param settings a node settings object
     * @return the updated configuration
     */
    ContainerTableInputNodeConfiguration loadInDialog(final NodeSettingsRO settings) {
        try {
            setParameterName(settings.getString(CFG_PARAMETER_NAME, DEFAULT_PARAMETER_NAME));
            setInputPathOrUrl(settings.getString(CFG_INPUT_PATH_OR_URL, DEFAULT_INPUT_PATH_OR_URL));
        } catch (InvalidSettingsException e) {
            m_parameterName = DEFAULT_PARAMETER_NAME;
            m_inputPathOrUrl = DEFAULT_INPUT_PATH_OR_URL;
        }
        setUseFQNParamName(settings.getBoolean(CFG_USE_FULLY_QUALIFIED_NAME, false)); // added in 4.3
        setDescription(settings.getString(CFG_DESCRIPTION, DEFAULT_DESCRIPTION));
        setTemplateConfiguration(
            new ContainerTemplateTableConfiguration(CFG_CONTAINER_INPUT_TABLE_TEMPLATE).loadInDialog(settings));
        return this;
    }

    /**
     * Saves the current configuration to the given settings object.
     *
     * @param settings a settings object
     * @return this object
     */
    ContainerTableInputNodeConfiguration save(final NodeSettingsWO settings) {
        settings.addString(CFG_PARAMETER_NAME, m_parameterName);
        settings.addBoolean(CFG_USE_FULLY_QUALIFIED_NAME, m_useFQNParamName); // added in 4.3
        settings.addString(CFG_DESCRIPTION, m_description);
        settings.addString(CFG_INPUT_PATH_OR_URL, m_inputPathOrUrl);
        m_templateConfiguration.save(settings);
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "\"" + m_parameterName + "\"";
    }

}
