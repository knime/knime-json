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
package org.knime.json.node.reader;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObjectSpec;

/**
 * The settings for the JSONReader node.
 *
 * @author Gabor Bakos
 */
final class JSONReaderSettings {
    static final String ALLOW_COMMENTS = "allow.comments",
            COLUMN_NAME = "column.name", DEFAULT_COLUMN_NAME = "json", LOCATION = "json.location";

    static final String SELECT_PART = "select.part", JSON_POINTER = "json.pointer", FAIL_IF_NOT_FOUND = "fail.if.not.found";
    static final String DEFAULT_JSON_POINTER = "";

    private boolean m_allowComments = false, m_selectPart = false, m_failIfNotFound;

    private String m_columnName = DEFAULT_COLUMN_NAME, m_location = System.getProperty("user.home", "/"), m_jsonPointer;

    /**
     * Constructs the object.
     */
    JSONReaderSettings() {
    }

    /**
     * Loads the settings with defaults as a failback.
     *
     * @param settings The proposed {@link NodeSettings}.
     * @param specs The input port specs.
     */
    void loadSettingsForDialogs(final NodeSettingsRO settings, final PortObjectSpec[] specs) {
        m_allowComments = settings.getBoolean(ALLOW_COMMENTS, false);
        m_columnName = settings.getString(COLUMN_NAME, DEFAULT_COLUMN_NAME);
        m_selectPart = settings.getBoolean(SELECT_PART, false);
        m_jsonPointer = settings.getString(JSON_POINTER, DEFAULT_JSON_POINTER);
        m_failIfNotFound = settings.getBoolean(FAIL_IF_NOT_FOUND, true);
        m_location = settings.getString(LOCATION, "");
    }

    /**
     * @param settings input settings.
     * @throws InvalidSettingsException Wrong input.
     * @see JSONReaderNodeModel#loadValidatedSettingsFrom(NodeSettingsRO)
     */
    void loadSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_allowComments = settings.getBoolean(ALLOW_COMMENTS);
        m_columnName = settings.getString(COLUMN_NAME);
        m_selectPart = settings.getBoolean(SELECT_PART);
        m_jsonPointer = settings.getString(JSON_POINTER);
        m_failIfNotFound = settings.getBoolean(FAIL_IF_NOT_FOUND);
        m_location = settings.getString(LOCATION);
    }

    /**
     * Saves the content of this settings object to {@code settings}.
     *
     * @param settings The output {@link NodeSettings} object.
     */
    void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addBoolean(ALLOW_COMMENTS, m_allowComments);
        settings.addString(COLUMN_NAME, m_columnName);
        settings.addBoolean(SELECT_PART, m_selectPart);
        settings.addString(JSON_POINTER, m_jsonPointer);
        settings.addBoolean(FAIL_IF_NOT_FOUND, m_failIfNotFound);
        settings.addString(LOCATION, m_location);
    }

    /**
     * Validates input and the current settings.
     *
     * @param settings The proposed {@link NodeSettings}.
     * @throws InvalidSettingsException No column was selected.
     */
    void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        if (settings.getString(COLUMN_NAME).isEmpty()) {
            throw new InvalidSettingsException("Empty column name is not allowed.");
        }
    }

    /**
     * @return the allowComments
     */
    final boolean isAllowComments() {
        return m_allowComments;
    }

    /**
     * @param allowComments the allowComments to set
     */
    final void setAllowComments(final boolean allowComments) {
        this.m_allowComments = allowComments;
    }

    /**
     * @return the columnName (output)
     */
    final String getColumnName() {
        return m_columnName;
    }

    /**
     * @param columnName the columnName to set
     */
    final void setColumnName(final String columnName) {
        this.m_columnName = columnName;
    }

    /**
     * @return the location to read the JSON from.
     */
    final String getLocation() {
        return m_location;
    }

    /**
     * @param location the location to set
     */
    final void setLocation(final String location) {
        this.m_location = location;
    }

    /**
     * @return the selectPart
     */
    final boolean isSelectPart() {
        return m_selectPart;
    }

    /**
     * @param selectPart the selectPart to set
     */
    final void setSelectPart(final boolean selectPart) {
        this.m_selectPart = selectPart;
    }

    /**
     * @return the failIfNotFound
     */
    final boolean isFailIfNotFound() {
        return m_failIfNotFound;
    }

    /**
     * @param failIfNotFound the failIfNotFound to set
     */
    final void setFailIfNotFound(final boolean failIfNotFound) {
        this.m_failIfNotFound = failIfNotFound;
    }

    /**
     * @return the jsonPointer
     */
    final String getJsonPointer() {
        return m_jsonPointer;
    }

    /**
     * @param jsonPointer the jsonPointer to set
     */
    final void setJsonPointer(final String jsonPointer) {
        this.m_jsonPointer = jsonPointer;
    }
}
