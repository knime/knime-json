/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *
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
 *   20 Dec 2014 (Gabor): created
 */
package org.knime.json.node.combine.column;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterConfiguration;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterPanel;
import org.knime.json.util.RootKeyType;

/**
 * Node settings for JSON Column Combiner. <br/>
 * Based on org.knime.xml.node.ccombine2.XMLColumnCombinerNodeSettings.
 *
 * @author Gabor Bakos
 */
public class ColumnCombineJsonSettings {
    private static final String NEW_COLUMN = "newColumn";

    private static final String ROOT_KEY_NAME = "rootKeyName", ROOT_KEY_TYPE = "rootKeyType";

    private static final String KEY_NAME_COLUMN = "keyNameColumn";

    private static final String REMOVE_SOURCE_COLUMNS = "removeSourceColumns";

    private String m_newColumn = "JSON";

    private String m_rootName = "array";

    private RootKeyType m_rootKeyType = RootKeyType.Unnamed;

    private String m_keyNameColumn = null;

    private boolean m_removeSourceColumns = false;

    private DataColumnSpecFilterConfiguration m_filterConfiguration = ColumnCombineJsonNodeModel
        .createDCSFilterConfiguration();

    /**
     * @return the newColumn
     */
    String getNewColumn() {
        return m_newColumn;
    }

    /**
     * @param newColumn the newColumn to set
     */
    void setNewColumn(final String newColumn) {
        m_newColumn = newColumn;
    }

    /**
     * @return the rootKeyType
     */
    RootKeyType getRootKeyType() {
        return m_rootKeyType;
    }

    /**
     * @param rootKeyType the rootKeyType to set
     */
    void setRootKeyType(final RootKeyType rootKeyType) {
        this.m_rootKeyType = rootKeyType;
    }

    /**
     * @return the removeSourceColumns
     */
    boolean getRemoveSourceColumns() {
        return m_removeSourceColumns;
    }

    /**
     * @param removeSourceColumns the removeSourceColumns to set
     */
    void setRemoveSourceColumns(final boolean removeSourceColumns) {
        m_removeSourceColumns = removeSourceColumns;
    }

    /**
     * @return the rootKeyName
     */
    String getRootKeyName() {
        return m_rootName;
    }

    /**
     * @param rootKeyName the rootKeyName to set
     */
    void setRootKeyName(final String rootKeyName) {
        m_rootName = rootKeyName;
    }

    /**
     * @return the rootKeyNameColumn
     */
    String getRootKeyNameColumn() {
        return m_keyNameColumn;
    }

    /**
     * @param rootKeyNameColumn the rootKeyNameColumn to set
     */
    void setRootKeyNameColumn(final String rootKeyNameColumn) {
        m_keyNameColumn = rootKeyNameColumn;
    }

    /**
     * Called from dialog when settings are to be loaded.
     *
     * @param settings To load from
     * @param inSpec Input spec
     */
    void loadSettingsDialog(final NodeSettingsRO settings, final DataTableSpec inSpec) {
        m_newColumn = settings.getString(NEW_COLUMN, null);
        m_rootName = settings.getString(ROOT_KEY_NAME, null);
        m_rootKeyType = RootKeyType.valueOf(settings.getString(ROOT_KEY_TYPE, RootKeyType.Unnamed.name()));
        m_keyNameColumn = settings.getString(KEY_NAME_COLUMN, null);
        DataColumnSpecFilterConfiguration config = ColumnCombineJsonNodeModel.createDCSFilterConfiguration();
        config.loadConfigurationInDialog(settings, inSpec);
        m_filterConfiguration = config;
        m_removeSourceColumns = settings.getBoolean(REMOVE_SOURCE_COLUMNS, false);
    }

    /**
     * Called from model when settings are to be loaded.
     *
     * @param settings To load from
     * @throws InvalidSettingsException If settings are invalid.
     */
    void loadSettingsModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_newColumn = settings.getString(NEW_COLUMN);
        m_rootName = settings.getString(ROOT_KEY_NAME);
        m_rootKeyType = RootKeyType.valueOf(settings.getString(ROOT_KEY_TYPE));
        m_keyNameColumn = settings.getString(KEY_NAME_COLUMN);
        DataColumnSpecFilterConfiguration config = ColumnCombineJsonNodeModel.createDCSFilterConfiguration();
        config.loadConfigurationInModel(settings);
        m_filterConfiguration = config;
        m_removeSourceColumns = settings.getBoolean(REMOVE_SOURCE_COLUMNS);
    }

    /**
     * Called from model and dialog to save current settings.
     *
     * @param settings To save to.
     */
    void saveSettings(final NodeSettingsWO settings) {
        settings.addString(NEW_COLUMN, m_newColumn);
        settings.addString(ROOT_KEY_NAME, m_rootName);
        settings.addString(ROOT_KEY_TYPE, m_rootKeyType.name());
        settings.addString(KEY_NAME_COLUMN, m_keyNameColumn);
        m_filterConfiguration.saveConfiguration(settings);
        settings.addBoolean(REMOVE_SOURCE_COLUMNS, m_removeSourceColumns);
    }

    /**
     * @param config a filter panel configuration
     */
    public void setFilterConfiguration(final DataColumnSpecFilterConfiguration config) {
        m_filterConfiguration = config;
    }

    /**
     * @return filter configuration for {@link DataColumnSpecFilterPanel}
     */
    public DataColumnSpecFilterConfiguration getFilterConfiguration() {
        return m_filterConfiguration;
    }
}
