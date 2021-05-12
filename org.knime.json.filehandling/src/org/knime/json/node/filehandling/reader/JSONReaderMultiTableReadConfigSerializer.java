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
import org.knime.core.node.port.PortObjectSpec;
import org.knime.filehandling.core.node.table.reader.config.ConfigSerializer;
import org.knime.filehandling.core.node.table.reader.config.DefaultTableReadConfig;
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

    private static final String DEFAULT_COLUMN_NAME = "json";

    private static final String COLUMN_NAME = "column_name";

    private static final String READ_MODE = "read_mode";

    private static final String ALLOW_COMMENTS = "allow_comments";

    private static final String USE_PATH = "use_path";

    private static final String JSON_PATH = "json_path";

    private static final String FAIL_IF_NOT_FOUND = "fail_if_not_found";

    private static final String DEFAULT_JSON_PATH = "$";

    @Override
    public ConfigID createFromSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        return new NodeSettingsConfigID(settings.getNodeSettings(KEY));
    }

    @Override
    public ConfigID createFromConfig(final JSONMultiTableReadConfig config) {
        final NodeSettings settings = new NodeSettings(KEY);
        saveConfigIDSettingsTab(config, settings.addNodeSettings(SettingsUtils.CFG_SETTINGS_TAB));
        return new NodeSettingsConfigID(settings);
    }

    /**
     * @param config
     * @param addNodeSettings
     */
    void saveConfigIDSettingsTab(final JSONMultiTableReadConfig config, final NodeSettingsWO addNodeSettings) {

    }

    @Override
    public void loadInDialog(final JSONMultiTableReadConfig config, final NodeSettingsRO settings,
        final PortObjectSpec[] specs) throws NotConfigurableException {
        loadSettingsTabInDialog(config, SettingsUtils.getOrEmpty(settings, SettingsUtils.CFG_SETTINGS_TAB));
    }

    /**
     * @param config
     * @param orEmpty
     */
    private static void loadSettingsTabInDialog(final JSONMultiTableReadConfig config, final NodeSettingsRO settings) {
        final DefaultTableReadConfig<JSONReaderConfig> tc = config.getTableReadConfig();
        tc.setRowIDIdx(-1);
        tc.setLimitRowsForSpec(false);
        tc.setUseColumnHeaderIdx(false);
        final JSONReaderConfig jsonReaderCfg = config.getReaderSpecificConfig();
        jsonReaderCfg.setColumnName(settings.getString(COLUMN_NAME, DEFAULT_COLUMN_NAME));
        jsonReaderCfg.setJsonReadMode(JSONReadMode.valueOf(settings.getString(READ_MODE, JSONReadMode.LEGACY.name())));
        jsonReaderCfg.setAllowComments(settings.getBoolean(ALLOW_COMMENTS, false));
        jsonReaderCfg.setFailIfNotFound(settings.getBoolean(FAIL_IF_NOT_FOUND, false));
        jsonReaderCfg.setJSONPath(settings.getString(JSON_PATH, DEFAULT_JSON_PATH));
        jsonReaderCfg.setUseJSONPath(settings.getBoolean(USE_PATH, false));
    }

    @Override
    public void loadInModel(final JSONMultiTableReadConfig config, final NodeSettingsRO settings)
        throws InvalidSettingsException {
        loadSettingsTabInModel(config, settings.getNodeSettings(SettingsUtils.CFG_SETTINGS_TAB));
    }

    /**
     * @param config
     * @param nodeSettings
     */
    private static void loadSettingsTabInModel(final JSONMultiTableReadConfig config, final NodeSettingsRO settings)
        throws InvalidSettingsException {
        final DefaultTableReadConfig<JSONReaderConfig> tc = config.getTableReadConfig();
        tc.setUseRowIDIdx(false);
        tc.setRowIDIdx(-1);
        tc.setColumnHeaderIdx(0);
        tc.setLimitRowsForSpec(false);
        tc.setUseColumnHeaderIdx(false);
        final JSONReaderConfig jsonReaderCfg = config.getReaderSpecificConfig();
        jsonReaderCfg.setColumnName(settings.getString(COLUMN_NAME));
        jsonReaderCfg.setJsonReadMode(JSONReadMode.valueOf(settings.getString(READ_MODE)));
        jsonReaderCfg.setAllowComments(settings.getBoolean(ALLOW_COMMENTS));
        jsonReaderCfg.setFailIfNotFound(settings.getBoolean(FAIL_IF_NOT_FOUND));
        jsonReaderCfg.setJSONPath(settings.getString(JSON_PATH));
        jsonReaderCfg.setUseJSONPath(settings.getBoolean(USE_PATH));
    }

    @Override
    public void saveInModel(final JSONMultiTableReadConfig config, final NodeSettingsWO settings) {
        saveSettingsTab(config, SettingsUtils.getOrAdd(settings, SettingsUtils.CFG_SETTINGS_TAB));
    }

    private static void saveSettingsTab(final JSONMultiTableReadConfig config, final NodeSettingsWO settings) {
        final JSONReaderConfig jsonReaderCfg = config.getReaderSpecificConfig();

        settings.addString(READ_MODE, jsonReaderCfg.getJsonReadMode().name());
        settings.addString(COLUMN_NAME, jsonReaderCfg.getColumnName());
        settings.addBoolean(USE_PATH, jsonReaderCfg.useJSONPath());
        settings.addString(JSON_PATH, jsonReaderCfg.getJSONPath());
        settings.addBoolean(FAIL_IF_NOT_FOUND, jsonReaderCfg.failIfNotFound());
        settings.addBoolean(ALLOW_COMMENTS, jsonReaderCfg.allowComments());
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
    }

    /**
     * @param settings
     * @throws InvalidSettingsException
     */
    public static void validateSettingsTab(final NodeSettingsRO settings) throws InvalidSettingsException {
        settings.getString(COLUMN_NAME);
        settings.getString(READ_MODE);
        settings.getBoolean(ALLOW_COMMENTS);
        settings.getBoolean(FAIL_IF_NOT_FOUND);
        settings.getBoolean(USE_PATH);
        settings.getString(JSON_PATH);
    }
}
