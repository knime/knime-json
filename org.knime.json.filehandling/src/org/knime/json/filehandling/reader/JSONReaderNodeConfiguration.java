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
 *   Feb 13, 2021 (Moditha): created
 */
package org.knime.json.filehandling.reader;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.SettingsModelReaderFileChooser;

/**
 *
 * @author Moditha Hewasinghage
 */
public class JSONReaderNodeConfiguration {

    static final String ALLOW_COMMENTS = "allow.comments", //
            COLUMN_NAME = "column.name", //
            DEFAULT_COLUMN_NAME = "json", //
            INFER_COLUMNS = "infer.columns";

    private SettingsModelBoolean m_inferColumns = new SettingsModelBoolean(ALLOW_COMMENTS, false);

    private SettingsModelBoolean m_allowComments = new SettingsModelBoolean(ALLOW_COMMENTS, false);

    private SettingsModelString m_columnName = new SettingsModelString(COLUMN_NAME, DEFAULT_COLUMN_NAME);

    private final SettingsModelReaderFileChooser m_fileChooserSettings;

    JSONReaderNodeConfiguration(final SettingsModelReaderFileChooser fileChooserSettings) {
        m_fileChooserSettings = fileChooserSettings;
        m_inferColumns.setBooleanValue(false);
        m_allowComments.setBooleanValue(false);

        m_inferColumns.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(final ChangeEvent e) {
                m_columnName.setEnabled(!m_inferColumns.getBooleanValue());
            }
        });
    }

    SettingsModelReaderFileChooser getFileChooserSettings() {
        return m_fileChooserSettings;
    }

    void loadSettingsForDialog(final NodeSettingsRO settings) throws InvalidSettingsException {
        loadSettings(settings);
    }

    void loadSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_fileChooserSettings.loadSettingsFrom(settings);
        loadSettings(settings);
    }

    /**
     * @param settings
     * @throws InvalidSettingsException
     */
    private void loadSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_allowComments.loadSettingsFrom(settings);
        m_columnName.loadSettingsFrom(settings);
        m_inferColumns.loadSettingsFrom(settings);
    }

    void saveSettingsForDialog(final NodeSettingsWO settings) {
        saveSettings(settings);
    }

    void saveSettingsForModel(final NodeSettingsWO settings) {
        saveSettings(settings);
        m_fileChooserSettings.saveSettingsTo(settings);
    }

    private void saveSettings(final NodeSettingsWO settings) {
        m_allowComments.saveSettingsTo(settings);
        m_columnName.saveSettingsTo(settings);
        m_inferColumns.saveSettingsTo(settings);
    }

    void validateSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_fileChooserSettings.validateSettings(settings);
        loadSettings(settings);
        checkColumnName();
    }

    /**
     * Checks whether the set column name is empty or not if there is no column inferencing.
     *
     * @throws InvalidSettingsException The column name contains only white spaces .
     */
    void checkColumnName() throws InvalidSettingsException {
        checkColName(m_columnName.getStringValue());
    }

    /**
     * @param colName The expected column name if no column inferencing.
     * @throws InvalidSettingsException Only whitespaces.
     */
    private void checkColName(final String colName) throws InvalidSettingsException {
        if (!m_inferColumns.getBooleanValue() && colName.trim().isEmpty()) {
            throw new InvalidSettingsException("Empty column name is not allowed.");
        }
    }

    /**
     * @return the inferColumns
     */
    public SettingsModelBoolean getInferColumns() {
        return m_inferColumns;
    }

    /**
     * @param inferColumns the inferColumns to set
     */
    public void setInferColumns(final SettingsModelBoolean inferColumns) {
        m_inferColumns = inferColumns;
    }

    /**
     * @return the allowComments
     */
    public SettingsModelBoolean getAllowComments() {
        return m_allowComments;
    }

    /**
     * @param allowComments the allowComments to set
     */
    public void setAllowComments(final SettingsModelBoolean allowComments) {
        m_allowComments = allowComments;
    }

    /**
     * @return the columnName
     */
    public SettingsModelString getColumnName() {
        return m_columnName;
    }

    /**
     * @param columnName the columnName to set
     */
    public void setColumnName(final SettingsModelString columnName) {
        m_columnName = columnName;
    }

}
