/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *   28 Sept 2014 (Gabor): created
 */
package org.knime.json.node.schema.check;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObjectSpec;

/**
 * The node settings for the JSON schema checker node.
 *
 * @author Gabor Bakos
 */
public class JSONSchemaCheckSettings {
    /** config key for input schema */
    static final String INPUT_SCHEMA = "json.schema",
    /** Config key for the input column. */
    INPUT_COLUMN = "input.column",
    /** Config key for the error messages column. */
    ERROR_MESSAGES_COLUMN = "error.messages.column",
    /** Config key for fail on invalid json or append error messages. */
    FAIL_ON_INVALID_JSON = "fail.on.invalid.json";

    /** The default schema. */
    static final String DEFAULT_SCHEMA = "{}",
    /** The default error message column */
    DEFAULT_ERROR_MESSAGES_COLUMN = "invalid schema message";

    /** Should it fail on invalid json, or append error message to a new column? */
    static final boolean DEFAULT_FAIL_ON_INVALID_JSON = true;

    private String m_inputSchema = DEFAULT_SCHEMA, m_inputColumn = null,
            m_errorMessageColumn = DEFAULT_ERROR_MESSAGES_COLUMN;

    private boolean m_failOnInvalidJson = DEFAULT_FAIL_ON_INVALID_JSON;

    /**
     * Constructs the settings object.
     */
    public JSONSchemaCheckSettings() {
    }

    /**
     * Loads the settings with defaults as a failback.
     *
     * @param settings The proposed {@link NodeSettings}.
     * @param specs The input port specs.
     */
    void loadSettingsForDialogs(final NodeSettingsRO settings, final PortObjectSpec[] specs) {
        m_inputColumn = settings.getString(INPUT_COLUMN, "");
        m_inputSchema = settings.getString(INPUT_SCHEMA, DEFAULT_SCHEMA);
        m_errorMessageColumn = settings.getString(ERROR_MESSAGES_COLUMN, DataTableSpec.getUniqueColumnName((DataTableSpec)specs[0], DEFAULT_ERROR_MESSAGES_COLUMN));
        m_failOnInvalidJson = settings.getBoolean(FAIL_ON_INVALID_JSON, DEFAULT_FAIL_ON_INVALID_JSON);

    }

    /**
     * @param settings input settings.
     * @throws InvalidSettingsException Wrong input.
     * @see JSONSchemaCheckNodeModel#loadValidatedSettingsFrom(NodeSettingsRO)
     */
    void loadSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_inputColumn = settings.getString(INPUT_COLUMN);
        m_inputSchema = settings.getString(INPUT_SCHEMA);
        m_errorMessageColumn = settings.getString(ERROR_MESSAGES_COLUMN);
        m_failOnInvalidJson = settings.getBoolean(FAIL_ON_INVALID_JSON);
    }

    /**
     * Saves the content of this settings object to {@code settings}.
     *
     * @param settings The output {@link NodeSettings} object.
     */
    void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addString(INPUT_COLUMN, m_inputColumn);
        settings.addString(INPUT_SCHEMA, m_inputSchema);
        settings.addString(ERROR_MESSAGES_COLUMN, m_errorMessageColumn);
        settings.addBoolean(FAIL_ON_INVALID_JSON, m_failOnInvalidJson);
    }

    /**
     * Validates input and the current settings.
     *
     * @param settings The proposed {@link NodeSettings}.
     * @throws InvalidSettingsException No column was selected.
     */
    void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        if (settings.getString(INPUT_COLUMN).isEmpty()) {
            throw new InvalidSettingsException("No input column was selected.");
        }
        checkErrorColumn(settings.getBoolean(FAIL_ON_INVALID_JSON), settings.getString(ERROR_MESSAGES_COLUMN));
    }

    /**
     * Checks the error column validity.
     * @throws InvalidSettingsException
     */
    void checkSettings() throws InvalidSettingsException {
        checkErrorColumn(m_failOnInvalidJson, m_errorMessageColumn);
    }

    /**
     * @param failOnInvalidJson
     * @param errorMessageColumn
     * @throws InvalidSettingsException
     */
    private void checkErrorColumn(final boolean failOnInvalidJson, final String errorMessageColumn) throws InvalidSettingsException {
        if (!failOnInvalidJson && (errorMessageColumn == null || errorMessageColumn.trim().isEmpty())) {
            throw new InvalidSettingsException("No error message column was specified.");
        }
    }

    /**
     * @return the inputSchema
     */
    final String getInputSchema() {
        return m_inputSchema;
    }

    /**
     * @param inputSchema the inputSchema to set
     */
    final void setInputSchema(final String inputSchema) {
        this.m_inputSchema = inputSchema;
    }

    /**
     * @return the inputColumn
     */
    final String getInputColumn() {
        return m_inputColumn;
    }

    /**
     * @param inputColumn the inputColumn to set
     */
    final void setInputColumn(final String inputColumn) {
        this.m_inputColumn = inputColumn;
    }

    /**
     * @return the errorMessageColumn
     */
    final String getErrorMessageColumn() {
        return m_errorMessageColumn;
    }

    /**
     * @param errorMessageColumn the errorMessageColumn to set
     */
    final void setErrorMessageColumn(final String errorMessageColumn) {
        this.m_errorMessageColumn = errorMessageColumn;
    }

    /**
     * @return the failOnInvalidJson
     */
    final boolean isFailOnInvalidJson() {
        return m_failOnInvalidJson;
    }

    /**
     * @param failOnInvalidJson the failOnInvalidJson to set
     */
    final void setFailOnInvalidJson(final boolean failOnInvalidJson) {
        this.m_failOnInvalidJson = failOnInvalidJson;
    }

}
