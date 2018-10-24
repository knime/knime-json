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
import org.knime.json.util.JSONUtil;

/**
 * Configuration for the Container Input (Table) node.
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 */
final class ContainerTableInputNodeConfiguration {

    private static final String DEFAULT_PARAMETER_NAME = "input";
    private static final String DEFAULT_DESCRIPTION = "";
    private static final String DEFAULT_INPUT_PATH_OR_URL = null;
    private static final JsonValue DEFAULT_EXAMPLE_INPUT = ContainerTableDefaultJsonStructure.asJsonValue();
    private static final boolean DEFAULT_USE_ENTIRE_TABLE = true;
    private static final int DEFAULT_NUMBER_OF_ROWS = 10;

    private String m_parameterName;
    private String m_description;
    private String m_inputPathOrUrl;
    private JsonValue m_exampleInput;
    private boolean m_useEntireTable;
    private int m_numberOfRows;

    public ContainerTableInputNodeConfiguration() {
        m_parameterName = DEFAULT_PARAMETER_NAME;
        m_description = DEFAULT_DESCRIPTION;
        m_inputPathOrUrl = DEFAULT_INPUT_PATH_OR_URL;
        m_exampleInput = DEFAULT_EXAMPLE_INPUT;
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
     * Gets the example input.
     * @return the example input
     */
    JsonValue getExampleInput() {
        return m_exampleInput;
    }

    /**
     * Sets the example input.
     * @param exampleInput the example input to set
     * @throws InvalidSettingsException if the input does not comply with {@link ContainerTableJsonSchema}
     */
    void setExampleInput(final JsonValue exampleInput) throws InvalidSettingsException {
        if (ContainerTableJsonSchema.hasContainerTableJsonSchema(exampleInput)) {
            m_exampleInput = exampleInput;
        } else {
            throw new InvalidSettingsException("Example input has wrong format.");
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
    ContainerTableInputNodeConfiguration loadInModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        setParameterName(settings.getString("parameterName"));
        setDescription(settings.getString("description"));
        setInputPathOrUrl(settings.getString("inputPathOrUrl"));
        setUseEntireTable(settings.getBoolean("useEntireTable", DEFAULT_USE_ENTIRE_TABLE));
        setNumberOfRows(settings.getInt("numberOfRows", DEFAULT_NUMBER_OF_ROWS));
        String jsonString = settings.getString("exampleInput");
        try {
            JsonValue jsonValue = JSONUtil.parseJSONValue(jsonString);
            setExampleInput(jsonValue);
        } catch (IOException e) {
            throw new InvalidSettingsException("Example input has wrong format.", e);
        }

        return this;
    }

    /**
     * Loads the settings from the given node settings object. Default values will be used for missing or invalid settings.
     *
     * @param settings a node settings object
     * @return the updated configuration
     */
    ContainerTableInputNodeConfiguration loadInDialog(final NodeSettingsRO settings) {
        try {
            setParameterName(settings.getString("parameterName", DEFAULT_PARAMETER_NAME));
            setInputPathOrUrl(settings.getString("inputPathOrUrl", DEFAULT_INPUT_PATH_OR_URL));
        } catch (InvalidSettingsException e) {
            m_parameterName = DEFAULT_PARAMETER_NAME;
            m_inputPathOrUrl = DEFAULT_INPUT_PATH_OR_URL;
        }
        setDescription(settings.getString("description", DEFAULT_DESCRIPTION));
        String jsonString = settings.getString("exampleInput", ContainerTableDefaultJsonStructure.asString());
        setUseEntireTable(settings.getBoolean("useEntireTable", DEFAULT_USE_ENTIRE_TABLE));
        setNumberOfRows(settings.getInt("numberOfRows", DEFAULT_NUMBER_OF_ROWS));
        try {
            JsonValue jsonValue = JSONUtil.parseJSONValue(jsonString);
            setExampleInput(jsonValue);
        } catch (IOException | InvalidSettingsException e) {
            m_exampleInput = DEFAULT_EXAMPLE_INPUT;
        }
        return this;
    }

    /**
     * Saves the current configuration to the given settings object.
     *
     * @param settings a settings object
     * @return this object
     */
    ContainerTableInputNodeConfiguration save(final NodeSettingsWO settings) {
        settings.addString("parameterName", m_parameterName);
        settings.addString("description", m_description);
        settings.addString("inputPathOrUrl", m_inputPathOrUrl);
        settings.addBoolean("useEntireTable", m_useEntireTable);
        settings.addInt("numberOfRows", m_numberOfRows);
        if (m_exampleInput != null ) {
            settings.addString("exampleInput", JSONUtil.toPrettyJSONString(m_exampleInput));
        }
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "\"" + m_parameterName + "\"";
    }

}
