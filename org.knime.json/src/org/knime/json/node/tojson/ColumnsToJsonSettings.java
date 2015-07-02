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
 *   18 Dec 2014 (Gabor): created
 */
package org.knime.json.node.tojson;

import org.knime.core.data.BooleanValue;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.LongValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.blob.BinaryObjectDataValue;
import org.knime.core.data.collection.CollectionDataValue;
import org.knime.core.data.json.JSONValue;
import org.knime.core.data.vector.bytevector.ByteVectorValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.filter.NameFilterConfiguration;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterConfiguration;
import org.knime.core.node.util.filter.column.DataTypeColumnFilter;
import org.knime.json.util.DataBoundValuesConfigType;
import org.knime.json.util.RootKeyType;

/**
 * Node settings for the columns to json node.
 *
 * @author Gabor Bakos
 */
final class ColumnsToJsonSettings {
    private static String NEW_COLUMN = "newColumn", ROOT_KEY_TYPE = "rootKeyType", KEY_NAME = "keyName",
            KEY_NAME_COLUMN = "keyNameColumn", REMOVE_SOURCE_COLUMNS = "removeSourceColumns",
            DATA_BOUND_KEY_NAMES = "dataBoundKeyNames", DATA_BOUND_KEY_COLUMNS = "dataBoundKeyColumns",
            KEY_NAMES = "keyNames", KEY_VALUES = "keyValues", DATA_BOUND_VALUE_TYPE = "dataBoundValueType",
            DATA_BOUND_VALUE_COLUMNS = "dataBoundValueColumns";

    private String m_outputColumnName = "JSON", m_keyNameColumn = "", m_keyName = "cell";

    private boolean m_removeSourceColumns = false, m_notConfigured = true;

    private String[] m_dataBoundKeyNames = new String[0], m_dataBoundKeyColumns = new String[0],
            m_keyNames = new String[0], m_keyValues = new String[0];

    private RootKeyType m_rootKeyType = RootKeyType.Unnamed;

    private DataBoundValuesConfigType m_configType = DataBoundValuesConfigType.CustomKeys;

    private DataColumnSpecFilterConfiguration m_dataBoundColumnsAutoConfiguration;

    /**
     * Constructs settings.
     */
    ColumnsToJsonSettings() {
        m_dataBoundColumnsAutoConfiguration = createConfiguration(createFilter());
    }

    /**
     * @return
     */
    @SuppressWarnings("unchecked")
    static Class<? extends DataValue>[] supportedValueClasses() {
        return (Class<? extends DataValue>[])new Class<?>[]{LongValue.class, DoubleValue.class, JSONValue.class,
            BinaryObjectDataValue.class, ByteVectorValue.class, StringValue.class, BooleanValue.class, CollectionDataValue.class/*, DateAndTimeValue.class*/};
    }

    /**
     * @return
     */
    static DataTypeColumnFilter createFilter() {
        return new DataTypeColumnFilter(supportedValueClasses());
    }

    /**
     * @param filter
     * @return
     */
    static DataColumnSpecFilterConfiguration createConfiguration(final DataTypeColumnFilter filter) {
        return new DataColumnSpecFilterConfiguration(DATA_BOUND_VALUE_COLUMNS, filter,
            DataColumnSpecFilterConfiguration.FILTER_BY_DATATYPE | NameFilterConfiguration.FILTER_BY_NAMEPATTERN);
    }

    /**
     * @return the rootKeyType
     */
    final RootKeyType getRootKeyType() {
        return m_rootKeyType;
    }

    /**
     * @param rootKeyType the rootKeyType to set
     */
    final void setRootKeyType(final RootKeyType rootKeyType) {
        this.m_rootKeyType = rootKeyType;
    }

    /**
     * @return the outputColumnName
     */
    final String getOutputColumnName() {
        return m_outputColumnName;
    }

    /**
     * @param outputColumnName the outputColumnName to set
     */
    final void setOutputColumnName(final String outputColumnName) {
        this.m_outputColumnName = outputColumnName;
    }

    /**
     * @return the keyNameColumn
     */
    final String getKeyNameColumn() {
        return m_keyNameColumn;
    }

    /**
     * @param keyNameColumn the keyNameColumn to set
     */
    final void setKeyNameColumn(final String keyNameColumn) {
        this.m_keyNameColumn = keyNameColumn;
    }

    /**
     * @return the keyName
     */
    final String getKeyName() {
        return m_keyName;
    }

    /**
     * @param keyName the keyName to set
     */
    final void setKeyName(final String keyName) {
        this.m_keyName = keyName;
    }

    /**
     * @return the removeSourceColumns
     */
    final boolean isRemoveSourceColumns() {
        return m_removeSourceColumns;
    }

    /**
     * @param removeSourceColumns the removeSourceColumns to set
     */
    final void setRemoveSourceColumns(final boolean removeSourceColumns) {
        this.m_removeSourceColumns = removeSourceColumns;
    }

    /**
     * @return the dataBoundKeyNames
     */
    final String[] getDataBoundKeyNames() {
        return m_dataBoundKeyNames;
    }

    /**
     * @param dataBoundKeyNames the dataBoundKeyNames to set
     */
    final void setDataBoundKeyNames(final String[] dataBoundKeyNames) {
        this.m_dataBoundKeyNames = dataBoundKeyNames;
    }

    /**
     * @return the dataBoundKeyColumns
     */
    final String[] getDataBoundKeyColumns() {
        return m_dataBoundKeyColumns;
    }

    /**
     * @param dataBoundKeyColumns the dataBoundKeyColumns to set
     */
    final void setDataBoundKeyColumns(final String[] dataBoundKeyColumns) {
        this.m_dataBoundKeyColumns = dataBoundKeyColumns;
    }

    /**
     * @return the configType
     */
    final DataBoundValuesConfigType getConfigType() {
        return m_configType;
    }

    /**
     * @param configType the configType to set
     */
    final void setConfigType(final DataBoundValuesConfigType configType) {
        this.m_configType = configType;
    }

    /**
     * @return the m_dataBoundColumnsAutoConfiguration
     */
    final DataColumnSpecFilterConfiguration getDataBoundColumnsAutoConfiguration() {
        return m_dataBoundColumnsAutoConfiguration;
    }

    /**
     * @param dataBoundColumnsAutoConfiguration the dataBoundColumnsAutoConfiguration to set
     */
    public void setDataBoundColumnsAutoConfiguration(
        final DataColumnSpecFilterConfiguration dataBoundColumnsAutoConfiguration) {
        this.m_dataBoundColumnsAutoConfiguration = dataBoundColumnsAutoConfiguration;
    }

    /**
     * @return the keyNames
     */
    final String[] getKeyNames() {
        return m_keyNames;
    }

    /**
     * @param keyNames the keyNames to set
     */
    final void setKeyNames(final String[] keyNames) {
        this.m_keyNames = keyNames;
    }

    /**
     * @return the keyValues
     */
    final String[] getKeyValues() {
        return m_keyValues;
    }

    /**
     * @param keyValues the keyValues to set
     */
    final void setKeyValues(final String[] keyValues) {
        this.m_keyValues = keyValues;
    }

    /**
     * @return the notConfigured
     */
    boolean isNotConfigured() {
        return m_notConfigured;
    }

    /**
     * Called from dialog when settings are to be loaded.
     *
     * @param settings To load from
     * @param inSpec Input spec
     */
    void loadSettingsDialog(final NodeSettingsRO settings, final DataTableSpec inSpec) {
        m_outputColumnName = settings.getString(NEW_COLUMN, null);
        m_keyName = settings.getString(KEY_NAME, null);
        m_rootKeyType = RootKeyType.valueOf(settings.getString(ROOT_KEY_TYPE, RootKeyType.Unnamed.name()));
        m_keyNameColumn = settings.getString(KEY_NAME_COLUMN, null);
        m_removeSourceColumns = settings.getBoolean(REMOVE_SOURCE_COLUMNS, false);
        m_dataBoundKeyNames = settings.getStringArray(DATA_BOUND_KEY_NAMES, new String[0]);
        m_dataBoundKeyColumns = settings.getStringArray(DATA_BOUND_KEY_COLUMNS, new String[0]);
        m_configType = DataBoundValuesConfigType.values()[settings.getInt(DATA_BOUND_VALUE_TYPE, 0)];
        m_dataBoundColumnsAutoConfiguration.loadConfigurationInDialog(settings, inSpec);
        m_keyNames = settings.getStringArray(KEY_NAMES, new String[0]);
        m_keyValues = settings.getStringArray(KEY_VALUES, new String[0]);
    }

    /**
     * Called from model when settings are to be loaded.
     *
     * @param settings To load from
     * @throws InvalidSettingsException If settings are invalid.
     */
    void loadSettingsModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_notConfigured = false;
        m_outputColumnName = settings.getString(NEW_COLUMN);
        m_keyName = settings.getString(KEY_NAME);
        m_rootKeyType = RootKeyType.valueOf(settings.getString(ROOT_KEY_TYPE));
        m_keyNameColumn = settings.getString(KEY_NAME_COLUMN);
        m_removeSourceColumns = settings.getBoolean(REMOVE_SOURCE_COLUMNS);
        m_dataBoundKeyNames = settings.getStringArray(DATA_BOUND_KEY_NAMES);
        m_dataBoundKeyColumns = settings.getStringArray(DATA_BOUND_KEY_COLUMNS);
        m_configType = DataBoundValuesConfigType.values()[settings.getInt(DATA_BOUND_VALUE_TYPE)];
        m_dataBoundColumnsAutoConfiguration.loadConfigurationInModel(settings);
        m_keyNames = settings.getStringArray(KEY_NAMES);
        m_keyValues = settings.getStringArray(KEY_VALUES);
    }

    /**
     * Called from model and dialog to save current settings.
     *
     * @param settings To save to.
     */
    void saveSettings(final NodeSettingsWO settings) {
        m_notConfigured = false;
        settings.addString(NEW_COLUMN, m_outputColumnName);
        settings.addString(KEY_NAME, m_keyName);
        settings.addString(ROOT_KEY_TYPE, m_rootKeyType.name());
        settings.addString(KEY_NAME_COLUMN, m_keyNameColumn);
        settings.addBoolean(REMOVE_SOURCE_COLUMNS, m_removeSourceColumns);
        settings.addInt(DATA_BOUND_VALUE_TYPE, m_configType.ordinal());
        settings.addStringArray(DATA_BOUND_KEY_NAMES, m_dataBoundKeyNames);
        settings.addStringArray(DATA_BOUND_KEY_COLUMNS, m_dataBoundKeyColumns);
        m_dataBoundColumnsAutoConfiguration.saveConfiguration(settings);
        settings.addStringArray(KEY_NAMES, m_keyNames);
        settings.addStringArray(KEY_VALUES, m_keyValues);
    }
}
