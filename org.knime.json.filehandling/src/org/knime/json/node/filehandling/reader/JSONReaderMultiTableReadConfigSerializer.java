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
 *   Apr 7, 2021 (Moditha): created
 */
package org.knime.json.node.filehandling.reader;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.filehandling.core.node.table.reader.config.ConfigSerializer;
import org.knime.filehandling.core.node.table.reader.config.DefaultTableReadConfig;
import org.knime.filehandling.core.node.table.reader.config.TableReadConfig;
import org.knime.filehandling.core.node.table.reader.config.tablespec.ConfigID;
import org.knime.filehandling.core.node.table.reader.config.tablespec.ConfigIDFactory;
import org.knime.filehandling.core.node.table.reader.config.tablespec.NodeSettingsConfigID;
import org.knime.filehandling.core.util.SettingsUtils;

/**
 * The {@link ConfigSerializer} for the JSON reader node.
 *
 * @author Moditha Hewasinghage, KNIME GmbH, Berlin, Germany
 */
enum JSONReaderMultiTableReadConfigSerializer
    implements ConfigSerializer<JSONMultiTableReadConfig>, ConfigIDFactory<JSONMultiTableReadConfig> {

        /**
         * Singleton instance.
         */
        INSTANCE;

    private static final String KEY = "json_reader";

    private static final String CFG_DEFAULT_COLUMN_NAME = "json";

    private static final String CFG_COLUMN_NAME = "column_name";

    /**
     * remove the internal suffix after enabling autodetect
     */
    private static final String CFG_READ_MODE = "read_mode" + SettingsModel.CFGKEY_INTERNAL;

    private static final String CFG_ALLOW_COMMENTS = "allow_comments";

    private static final String CFG_USE_PATH = "use_path";

    private static final String CFG_JSON_PATH = "json_path";

    private static final String CFG_FAIL_IF_NOT_FOUND = "fail_if_not_found";

    private static final String CFG_DEFAULT_JSON_PATH = "$";

    private static final String CFG_LIMIT_ROWS_TAB = "limit_rows";

    private static final String CFG_MAX_ROWS = "max_rows";

    private static final String CFG_LIMIT_DATA_ROWS = "limit_data_rows";

    private static final String CFG_NUMBER_OF_ROWS_TO_SKIP = "number_of_rows_to_skip";

    private static final String CFG_SKIP_DATA_ROWS = "skip_data_rows";

    private static final String CFG_APPEND_PATH_COLUMN = "append_path_column" + SettingsModel.CFGKEY_INTERNAL;

    private static final String CFG_PATH_COLUMN_NAME = "path_column_name" + SettingsModel.CFGKEY_INTERNAL;

    @Override
    public ConfigID createFromSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        return new NodeSettingsConfigID(settings.getNodeSettings(KEY));
    }

    @Override
    public ConfigID createFromConfig(final JSONMultiTableReadConfig config) {
        final NodeSettings settings = new NodeSettings(KEY);
        saveConfigIDSettingsTab(config, settings.addNodeSettings(SettingsUtils.CFG_SETTINGS_TAB));
        saveConfigIDLimitRowsTab(config, settings.addNodeSettings(CFG_LIMIT_ROWS_TAB));
        return new NodeSettingsConfigID(settings);
    }

    private static void saveConfigIDSettingsTab(final JSONMultiTableReadConfig config, final NodeSettingsWO settings) {
        final JSONReaderConfig cc = config.getReaderSpecificConfig();
        settings.addString(CFG_READ_MODE, cc.getJsonReadMode().name());
        settings.addString(CFG_COLUMN_NAME, cc.getColumnName());
        settings.addBoolean(CFG_USE_PATH, cc.useJSONPath());
        settings.addString(CFG_JSON_PATH, cc.getJSONPath());
        settings.addBoolean(CFG_FAIL_IF_NOT_FOUND, cc.failIfNotFound());
        settings.addBoolean(CFG_ALLOW_COMMENTS, cc.allowComments());
    }

    private static void saveConfigIDLimitRowsTab(final JSONMultiTableReadConfig config,
        final NodeSettingsWO limitRowsSettings) {
        limitRowsSettings.addBoolean(CFG_SKIP_DATA_ROWS, config.getTableReadConfig().skipRows());
        limitRowsSettings.addLong(CFG_NUMBER_OF_ROWS_TO_SKIP, config.getTableReadConfig().getNumRowsToSkip());
        limitRowsSettings.addBoolean(CFG_LIMIT_DATA_ROWS, config.getTableReadConfig().limitRows());
        limitRowsSettings.addLong(CFG_MAX_ROWS, config.getTableReadConfig().getMaxRows());
    }

    @Override
    public void loadInDialog(final JSONMultiTableReadConfig config, final NodeSettingsRO settings,
        final PortObjectSpec[] specs) throws NotConfigurableException {
        loadSettingsTabInDialog(config, SettingsUtils.getOrEmpty(settings, SettingsUtils.CFG_SETTINGS_TAB));
        loadLimitRowsTabInDialog(config, SettingsUtils.getOrEmpty(settings, CFG_LIMIT_ROWS_TAB));
    }

    private static void loadSettingsTabInDialog(final JSONMultiTableReadConfig config, final NodeSettingsRO settings) {
        final DefaultTableReadConfig<JSONReaderConfig> tc = config.getTableReadConfig();
        tc.setRowIDIdx(-1);
        tc.setLimitRowsForSpec(false);
        tc.setUseColumnHeaderIdx(false);
        final JSONReaderConfig jsonReaderCfg = config.getReaderSpecificConfig();
        jsonReaderCfg.setColumnName(settings.getString(CFG_COLUMN_NAME, CFG_DEFAULT_COLUMN_NAME));
        jsonReaderCfg
            .setJsonReadMode(JSONReadMode.valueOf(settings.getString(CFG_READ_MODE, JSONReadMode.LEGACY.name())));
        jsonReaderCfg.setAllowComments(settings.getBoolean(CFG_ALLOW_COMMENTS, false));
        jsonReaderCfg.setFailIfNotFound(settings.getBoolean(CFG_FAIL_IF_NOT_FOUND, false));
        jsonReaderCfg.setJSONPath(settings.getString(CFG_JSON_PATH, CFG_DEFAULT_JSON_PATH));
        jsonReaderCfg.setUseJSONPath(settings.getBoolean(CFG_USE_PATH, false));

        config.setAppendItemIdentifierColumn(
            settings.getBoolean(CFG_APPEND_PATH_COLUMN, config.appendItemIdentifierColumn()));
        config.setItemIdentifierColumnName(
            settings.getString(CFG_PATH_COLUMN_NAME, config.getItemIdentifierColumnName()));
    }

    private static void loadLimitRowsTabInDialog(final JSONMultiTableReadConfig config, final NodeSettingsRO settings) {
        final DefaultTableReadConfig<JSONReaderConfig> tc = config.getTableReadConfig();
        tc.setSkipRows(settings.getBoolean(CFG_SKIP_DATA_ROWS, false));
        tc.setNumRowsToSkip(settings.getLong(CFG_NUMBER_OF_ROWS_TO_SKIP, 1L));
        tc.setLimitRows(settings.getBoolean(CFG_LIMIT_DATA_ROWS, false));
        tc.setMaxRows(settings.getLong(CFG_MAX_ROWS, 50L));
    }

    @Override
    public void loadInModel(final JSONMultiTableReadConfig config, final NodeSettingsRO settings)
        throws InvalidSettingsException {
        loadSettingsTabInModel(config, settings.getNodeSettings(SettingsUtils.CFG_SETTINGS_TAB));
        loadLimitRowsTabInModel(config, settings.getNodeSettings(CFG_LIMIT_ROWS_TAB));
    }

    private static void loadLimitRowsTabInModel(final JSONMultiTableReadConfig config, final NodeSettingsRO settings)
        throws InvalidSettingsException {
        final DefaultTableReadConfig<JSONReaderConfig> tc = config.getTableReadConfig();

        tc.setSkipRows(settings.getBoolean(CFG_SKIP_DATA_ROWS));
        tc.setNumRowsToSkip(settings.getLong(CFG_NUMBER_OF_ROWS_TO_SKIP));
        tc.setLimitRows(settings.getBoolean(CFG_LIMIT_DATA_ROWS));
        tc.setMaxRows(settings.getLong(CFG_MAX_ROWS));
    }

    private static void loadSettingsTabInModel(final JSONMultiTableReadConfig config, final NodeSettingsRO settings)
        throws InvalidSettingsException {
        final DefaultTableReadConfig<JSONReaderConfig> tc = config.getTableReadConfig();
        tc.setUseRowIDIdx(false);
        tc.setRowIDIdx(-1);
        tc.setColumnHeaderIdx(0);
        tc.setLimitRowsForSpec(false);
        tc.setUseColumnHeaderIdx(false);
        final JSONReaderConfig jsonReaderCfg = config.getReaderSpecificConfig();
        jsonReaderCfg.setColumnName(settings.getString(CFG_COLUMN_NAME));
        jsonReaderCfg.setJsonReadMode(JSONReadMode.valueOf(settings.getString(CFG_READ_MODE)));
        jsonReaderCfg.setAllowComments(settings.getBoolean(CFG_ALLOW_COMMENTS));
        jsonReaderCfg.setFailIfNotFound(settings.getBoolean(CFG_FAIL_IF_NOT_FOUND));
        jsonReaderCfg.setJSONPath(settings.getString(CFG_JSON_PATH));
        jsonReaderCfg.setUseJSONPath(settings.getBoolean(CFG_USE_PATH));
        config.setAppendItemIdentifierColumn(settings.getBoolean(CFG_APPEND_PATH_COLUMN));
        config.setItemIdentifierColumnName(settings.getString(CFG_PATH_COLUMN_NAME));
    }

    @Override
    public void saveInModel(final JSONMultiTableReadConfig config, final NodeSettingsWO settings) {
        saveSettingsTab(config, SettingsUtils.getOrAdd(settings, SettingsUtils.CFG_SETTINGS_TAB));
        saveLimitRowsTab(config, settings.addNodeSettings(CFG_LIMIT_ROWS_TAB));

    }

    private static void saveLimitRowsTab(final JSONMultiTableReadConfig config, final NodeSettingsWO settings) {
        final TableReadConfig<JSONReaderConfig> tc = config.getTableReadConfig();

        settings.addBoolean(CFG_SKIP_DATA_ROWS, tc.skipRows());
        settings.addLong(CFG_NUMBER_OF_ROWS_TO_SKIP, tc.getNumRowsToSkip());
        settings.addBoolean(CFG_LIMIT_DATA_ROWS, tc.limitRows());
        settings.addLong(CFG_MAX_ROWS, tc.getMaxRows());
    }

    private static void saveSettingsTab(final JSONMultiTableReadConfig config, final NodeSettingsWO settings) {
        final JSONReaderConfig jsonReaderCfg = config.getReaderSpecificConfig();

        settings.addString(CFG_READ_MODE, jsonReaderCfg.getJsonReadMode().name());
        settings.addString(CFG_COLUMN_NAME, jsonReaderCfg.getColumnName());
        settings.addBoolean(CFG_USE_PATH, jsonReaderCfg.useJSONPath());
        settings.addString(CFG_JSON_PATH, jsonReaderCfg.getJSONPath());
        settings.addBoolean(CFG_FAIL_IF_NOT_FOUND, jsonReaderCfg.failIfNotFound());
        settings.addBoolean(CFG_ALLOW_COMMENTS, jsonReaderCfg.allowComments());
        settings.addBoolean(CFG_APPEND_PATH_COLUMN, config.appendItemIdentifierColumn());
        settings.addString(CFG_PATH_COLUMN_NAME, config.getItemIdentifierColumnName());
    }

    @Override
    public void saveInDialog(final JSONMultiTableReadConfig config, final NodeSettingsWO settings)
        throws InvalidSettingsException {
        saveInModel(config, settings);
    }

    @Override
    public void validate(final JSONMultiTableReadConfig config, final NodeSettingsRO settings)
        throws InvalidSettingsException {
        validateSettingsTab(settings.getNodeSettings(SettingsUtils.CFG_SETTINGS_TAB));
        validateLimitRowsTab(settings.getNodeSettings(CFG_LIMIT_ROWS_TAB));
    }

    private static void validateSettingsTab(final NodeSettingsRO settings) throws InvalidSettingsException {
        settings.getString(CFG_COLUMN_NAME);
        settings.getString(CFG_READ_MODE);
        settings.getBoolean(CFG_ALLOW_COMMENTS);
        settings.getBoolean(CFG_FAIL_IF_NOT_FOUND);
        settings.getBoolean(CFG_USE_PATH);
        settings.getString(CFG_JSON_PATH);
        settings.getBoolean(CFG_APPEND_PATH_COLUMN);
        settings.getString(CFG_PATH_COLUMN_NAME);
    }

    private static void validateLimitRowsTab(final NodeSettingsRO settings) throws InvalidSettingsException {
        settings.getBoolean(CFG_SKIP_DATA_ROWS);
        settings.getLong(CFG_NUMBER_OF_ROWS_TO_SKIP);
        settings.getBoolean(CFG_LIMIT_DATA_ROWS);
        settings.getLong(CFG_MAX_ROWS);
    }
}
