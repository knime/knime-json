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
 *   May 4, 2018 (Tobias Urhaug, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.json.node.container.output.table;

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
 * Configuration for ContainerTableOutputNodeModel.
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 * @since 3.6
 */
final class ContainerTableOutputNodeConfiguration {

    private static final String DEFAULT_PARAMETER_NAME = "table-output";
    private static final String DEFAULT_DESCRIPTION = "";
    private static final String DEFAULT_OUTPUT_PATH_OR_URL = null;

    private String m_parameterName;
    private boolean m_useFQNParamName = false; // added in 4.3
    private String m_description;
    private String m_outputPathOrUrl;
    private ContainerTemplateTableConfiguration m_templateConfiguration;

    /**
     * Constructs a new configuration.
     */
    public ContainerTableOutputNodeConfiguration() {
        m_parameterName = DEFAULT_PARAMETER_NAME;
        m_description = DEFAULT_DESCRIPTION;
        m_outputPathOrUrl = DEFAULT_OUTPUT_PATH_OR_URL;
        m_templateConfiguration = new ContainerTemplateTableConfiguration("exampleOutput");
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
     * @param description a description, must not be <code>null</code>
     */
    ContainerTableOutputNodeConfiguration setDescription(final String description) {
        m_description = description;
        return this;
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
    ContainerTableOutputNodeConfiguration setParameterName(final String value) throws InvalidSettingsException {
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
     * Returns the outputFilePath.
     *
     * @return the outputFilePath
     */
    Optional<String> getOutputPathOrUrl() {
        return Optional.ofNullable(m_outputPathOrUrl);
    }

    /**
     * Sets the output file path. This settings isn't exposed in the dialog but can be controlled by a flow variable
     * from an external caller. In case the value is empty, no file will be written.
     *
     * @param outputPathOrUrl the outputFilePath to set
     * @throws InvalidSettingsException if argument is non-null but empty
     */
    ContainerTableOutputNodeConfiguration setOutputPathOrUrl(final String outputPathOrUrl)
        throws InvalidSettingsException {
        CheckUtils.checkSetting(outputPathOrUrl == null || StringUtils.isNotEmpty(outputPathOrUrl),
                "Output path \"%s\" must be null or not empty", outputPathOrUrl);
        m_outputPathOrUrl = outputPathOrUrl;
        return this;
    }

    /**
     * Gets the example output.
     * @return the example output
     */
    JsonValue getTemplateOutput() {
        return m_templateConfiguration.getExampleTemplate();
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
     * @param templateOutputPanel the panel from which the configuration should be set
     * @throws InvalidSettingsException if input panel has invalid settings
     */
    void setTemplateConfiguration(final ContainerTemplateTablePanel templateOutputPanel)
            throws InvalidSettingsException {
        ContainerTemplateTableConfiguration templateConfig = new ContainerTemplateTableConfiguration("exampleOutput");
        templateConfig.setTemplate(templateOutputPanel.getTemplateTableJson());
        templateConfig.setUseEntireTable(templateOutputPanel.getUseEntireTable());
        templateConfig.setNumberOfRows(templateOutputPanel.getNumberOfRows());
        templateConfig.setOmitTableSpec(templateOutputPanel.getOmitTableSpec());
        m_templateConfiguration = templateConfig;
    }

    /**
     * Loads the settings from the given node settings object. Loading will fail if settings are missing or invalid.
     *
     * @param settings a node settings object
     * @return the updated configuration
     * @throws InvalidSettingsException if settings are missing or invalid
     */
    ContainerTableOutputNodeConfiguration loadInModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        setParameterName(settings.getString("parameterName"));
        setUseFQNParamName(settings.getBoolean("useFullyQualifiedName", true)); // added in 4.3
        setDescription(settings.getString("description"));
        setOutputPathOrUrl(settings.getString("outputPathOrUrl"));
        setTemplateConfiguration(new ContainerTemplateTableConfiguration("exampleOutput").loadInModel(settings));
        return this;
    }

    /**
     * Loads the settings from the given node settings object. Default values will be used for missing or
     * invalid settings.
     *
     * @param settings a node settings object
     * @return the updated configuration
     */
    ContainerTableOutputNodeConfiguration loadInDialog(final NodeSettingsRO settings) {
        try {
            setParameterName(settings.getString("parameterName", DEFAULT_PARAMETER_NAME));
            setOutputPathOrUrl(settings.getString("outputPathOrUrl", DEFAULT_OUTPUT_PATH_OR_URL));
        } catch (InvalidSettingsException e) {
            m_parameterName = DEFAULT_PARAMETER_NAME;
            m_outputPathOrUrl = DEFAULT_OUTPUT_PATH_OR_URL;
        }
        setUseFQNParamName(settings.getBoolean("useFullyQualifiedName", false)); // added in 4.3
        setDescription(settings.getString("description", DEFAULT_DESCRIPTION));
        setTemplateConfiguration(new ContainerTemplateTableConfiguration("exampleOutput").loadInDialog(settings));
        return this;
    }

    /**
     * Saves the current configuration to the given settings object.
     *
     * @param settings a settings object
     * @return this object
     */
    ContainerTableOutputNodeConfiguration save(final NodeSettingsWO settings) {
        settings.addString("parameterName", m_parameterName);
        settings.addBoolean("useFullyQualifiedName", m_useFQNParamName); // added in 4.3
        settings.addString("description", m_description);
        settings.addString("outputPathOrUrl", m_outputPathOrUrl);
        m_templateConfiguration.save(settings);
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "\"" + m_parameterName + "\"";
    }

}
