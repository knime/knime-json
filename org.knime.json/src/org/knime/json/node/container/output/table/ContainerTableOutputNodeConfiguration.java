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

import java.io.IOException;
import java.util.Optional;

import javax.json.JsonValue;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.data.json.container.table.ContainerTableJsonSchema;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.dialog.DialogNode;
import org.knime.core.node.util.CheckUtils;
import org.knime.json.node.container.input.table.ContainerTableDefaultJsonStructure;
import org.knime.json.util.JSONUtil;

/**
 * Configuration for ContainerTableOutputNodeModel.
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 * @since 3.6
 */
final class ContainerTableOutputNodeConfiguration {

    private static final String DEFAULT_PARAMETER_NAME = "output";
    private static final String DEFAULT_DESCRIPTION = "";
    private static final String DEFAULT_OUTPUT_PATH_OR_URL = null;
    private static final JsonValue DEFAULT_EXAMPLE_OUTPUT = ContainerTableDefaultJsonStructure.asJsonValue();
    private static final boolean DEFAULT_USE_ENTIRE_TABLE = true;
    private static final int DEFAULT_NUMBER_OF_ROWS = 10;

    private String m_parameterName;
    private String m_description;
    private String m_outputPathOrUrl;
    private JsonValue m_exampleOutput;
    private boolean m_useEntireTable;
    private int m_numberOfRows;

    /**
     * Constructs a new configuration.
     */
    public ContainerTableOutputNodeConfiguration() {
        m_parameterName = DEFAULT_PARAMETER_NAME;
        m_description = DEFAULT_DESCRIPTION;
        m_outputPathOrUrl = DEFAULT_OUTPUT_PATH_OR_URL;
        m_exampleOutput = DEFAULT_EXAMPLE_OUTPUT;
        m_useEntireTable = DEFAULT_USE_ENTIRE_TABLE;
        m_numberOfRows = DEFAULT_NUMBER_OF_ROWS;
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
        return m_exampleOutput;
    }

    /**
     * Sets the example output.
     * @param exampleOutput the example output to set
     * @throws InvalidSettingsException if the output does not comply with {@link ContainerTableJsonSchema}
     */
    void setTemplateOutput(final JsonValue exampleOutput) throws InvalidSettingsException {
        if (ContainerTableJsonSchema.hasContainerTableJsonSchema(exampleOutput)) {
            m_exampleOutput = exampleOutput;
        } else {
            throw new InvalidSettingsException("Example output has wrong format.");
        }
    }

    /**
     * Gets the use entire table flag.
     * @return the use entire table flag
     */
    boolean getUseEntireTable() {
        return m_useEntireTable;
    }

    /**
     * Sets the use entire table flag.
     * @param useEntireTable the flag to be set
     * @throws InvalidSettingsException if the input does not comply with {@link ContainerTableJsonSchema}
     */
    void setUseEntireTable(final boolean useEntireTable) {
        m_useEntireTable = useEntireTable;
    }

    /**
     * Gets the number of rows the template table uses.
     * @return the number of rows the template table uses
     */
    int getNumberOfRows() {
        return m_numberOfRows;
    }

    /**
     * Sets the number of rows the template table uses.
     * @param numberOfRows the number of rows the template table uses
     */
    void setNumberOfRows(final int numberOfRows) {
        m_numberOfRows = numberOfRows;
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
        setDescription(settings.getString("description"));
        setOutputPathOrUrl(settings.getString("outputPathOrUrl"));
        setUseEntireTable(settings.getBoolean("useEntireTable", DEFAULT_USE_ENTIRE_TABLE));
        setNumberOfRows(settings.getInt("numberOfRows", DEFAULT_NUMBER_OF_ROWS));
        String jsonString = settings.getString("exampleOutput");
        try {
            JsonValue jsonValue = JSONUtil.parseJSONValue(jsonString);
            setTemplateOutput(jsonValue);
        } catch (IOException e) {
            throw new InvalidSettingsException("Example output has wrong format.", e);
        }
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
        setDescription(settings.getString("description", DEFAULT_DESCRIPTION));
        setUseEntireTable(settings.getBoolean("useEntireTable", DEFAULT_USE_ENTIRE_TABLE));
        setNumberOfRows(settings.getInt("numberOfRows", DEFAULT_NUMBER_OF_ROWS));
        String jsonString = settings.getString("exampleOutput", ContainerTableDefaultJsonStructure.asString());
        try {
            JsonValue jsonValue = JSONUtil.parseJSONValue(jsonString);
            setTemplateOutput(jsonValue);
        } catch (IOException | InvalidSettingsException e) {
            m_exampleOutput = DEFAULT_EXAMPLE_OUTPUT;
        }
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
        settings.addString("description", m_description);
        settings.addString("outputPathOrUrl", m_outputPathOrUrl);
        settings.addBoolean("useEntireTable", m_useEntireTable);
        settings.addInt("numberOfRows", m_numberOfRows);
        if (m_exampleOutput != null ) {
            settings.addString("exampleOutput", JSONUtil.toPrettyJSONString(m_exampleOutput));
        }
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "\"" + m_parameterName + "\"";
    }

}
