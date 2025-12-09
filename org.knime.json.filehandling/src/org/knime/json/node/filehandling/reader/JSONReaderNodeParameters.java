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
 * ------------------------------------------------------------------------
 */

package org.knime.json.node.filehandling.reader;

import java.util.Optional;

import org.knime.base.node.io.filehandling.webui.reader2.AppendFilePathColumnParameters;
import org.knime.base.node.io.filehandling.webui.reader2.MaxNumberOfRowsParameters;
import org.knime.base.node.io.filehandling.webui.reader2.ReaderLayout;
import org.knime.base.node.io.filehandling.webui.reader2.SkipFirstDataRowsParameters;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileReaderWidget;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.MultiFileSelectionMode;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.MultiFileSelectionWidget;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.PersistWithin;
import org.knime.filehandling.core.util.SettingsUtils;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.Before;
import org.knime.node.parameters.layout.Inside;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.persistence.legacy.LegacyMultiFileSelection;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.updates.util.BooleanReference;
import org.knime.node.parameters.widget.text.TextInputWidget;
import org.knime.node.parameters.widget.text.util.ColumnNameValidationUtils.ColumnNameValidation;

/**
 * Node parameters for JSON Reader.
 *
 * @author Paul Baernreuther, KNIME GmbH, Germany
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction")
@Layout(ReaderLayout.class)
class JSONReaderNodeParameters implements NodeParameters {

    // Legacy option kept for compatibility
    @Persist(configKey = JSONReaderMultiTableReadConfigSerializer.CFG_READ_MODE)
    @PersistWithin(SettingsUtils.CFG_SETTINGS_TAB)
    JSONReadMode m_readMode = JSONReadMode.LEGACY;

    @PersistWithin(SettingsUtils.CFG_SETTINGS_TAB)
    @Persist(configKey = "file_selection")
    @MultiFileSelectionWidget({MultiFileSelectionMode.FILE, MultiFileSelectionMode.FILES_IN_FOLDERS})
    @FileReaderWidget(fileExtensions = {"json", "json.gz"})
    LegacyMultiFileSelection m_fileSelection = new LegacyMultiFileSelection(MultiFileSelectionMode.FILE);

    @PersistWithin(SettingsUtils.CFG_SETTINGS_TAB)
    @Persist(configKey = JSONReaderMultiTableReadConfigSerializer.CFG_COLUMN_NAME)
    @Widget(title = "Output column name", description = "Name of the output column.")
    @TextInputWidget(patternValidation = ColumnNameValidation.class)
    String m_outputColumnName = JSONReaderMultiTableReadConfigSerializer.DEFAULT_COLUMN_NAME;

    @Section(title = "JSON Options")
    @Inside(ReaderLayout.class)
    @Before(ReaderLayout.MultipleFileHandling.class)
    @Before(ReaderLayout.DataArea.class)
    interface JSONOptionsSection {
    }

    @PersistWithin(SettingsUtils.CFG_SETTINGS_TAB)
    @Persist(configKey = JSONReaderMultiTableReadConfigSerializer.CFG_USE_PATH)
    @Widget(title = "Select with JSONPath",
        description = "Enable JSONPath filtering to extract a specific part of the read JSON.")
    @ValueReference(UseJSONPath.class)
    @Layout(JSONOptionsSection.class)
    boolean m_useJSONPath;

    static final class UseJSONPath implements BooleanReference {
    }

    @PersistWithin(SettingsUtils.CFG_SETTINGS_TAB)
    @Persist(configKey = JSONReaderMultiTableReadConfigSerializer.CFG_ALLOW_COMMENTS)
    @Widget(title = "Allow comments in JSON files", description = """
            When selected, <tt>/*</tt> ... <tt>*/</tt> and the line comments <tt>//</tt>, <tt>#</tt>
            are interpreted as comments and get ignored instead of causing errors.
            """)
    @Effect(predicate = UseJSONPath.class, type = EffectType.DISABLE)
    @Layout(JSONOptionsSection.class)
    boolean m_allowComments;

    @PersistWithin(SettingsUtils.CFG_SETTINGS_TAB)
    @Persist(configKey = JSONReaderMultiTableReadConfigSerializer.CFG_JSON_PATH)
    @Widget(title = "JSONPath", description = """
            The part to select from the input JSON. Using
            <a href="http://goessner.net/articles/JsonPath/">JSONPath</a> preferably with a single result.
            (For multiple results new rows will be created from them.)
            """)
    @Effect(predicate = UseJSONPath.class, type = EffectType.SHOW)
    @Layout(JSONOptionsSection.class)
    String m_jsonPath = JSONReaderMultiTableReadConfigSerializer.DEFAULT_JSON_PATH;

    @PersistWithin(SettingsUtils.CFG_SETTINGS_TAB)
    @Persist(configKey = JSONReaderMultiTableReadConfigSerializer.CFG_FAIL_IF_NOT_FOUND)
    @Widget(title = "Fail if path not found", description = """
            If checked, execution will fail if no such part found.
            If unchecked and not found, the result will be an empty file.
            """)
    @Effect(predicate = UseJSONPath.class, type = EffectType.SHOW)
    @Layout(JSONOptionsSection.class)
    boolean m_failIfNotFound;

    @PersistWithin(SettingsUtils.CFG_SETTINGS_TAB)
    @Persistor(AppendFilePathColumnPersistor.class)
    AppendFilePathColumnParameters m_appendFilePathColumnParameters = new AppendFilePathColumnParameters();

    @PersistWithin(JSONReaderMultiTableReadConfigSerializer.CFG_LIMIT_ROWS_TAB)
    @Persistor(SkipFirstDataRowsPersistor.class)
    SkipFirstDataRowsParameters m_skipFirstRowsParameters = new SkipFirstDataRowsParameters();

    @PersistWithin(JSONReaderMultiTableReadConfigSerializer.CFG_LIMIT_ROWS_TAB)
    @Persistor(MaxNumberOfRowsPersistor.class)
    MaxNumberOfRowsParameters m_maxNumberOfRowsParameters = new MaxNumberOfRowsParameters();

    // --- PERSISTORS ---

    static final class AppendFilePathColumnPersistor
        implements NodeParametersPersistor<AppendFilePathColumnParameters> {

        @Override
        public AppendFilePathColumnParameters load(final NodeSettingsRO nodeSettings) throws InvalidSettingsException {
            final boolean appendColumn =
                nodeSettings.getBoolean(JSONReaderMultiTableReadConfigSerializer.CFG_APPEND_PATH_COLUMN);
            if (appendColumn) {
                final String columnName =
                    nodeSettings.getString(JSONReaderMultiTableReadConfigSerializer.CFG_PATH_COLUMN_NAME);
                return new AppendFilePathColumnParameters(columnName);
            } else {
                return new AppendFilePathColumnParameters();
            }
        }

        @Override
        public void save(final AppendFilePathColumnParameters param, final NodeSettingsWO nodeSettings) {
            final Optional<String> appendPathColumn = param.getAppendPathColumn();
            nodeSettings.addBoolean(JSONReaderMultiTableReadConfigSerializer.CFG_APPEND_PATH_COLUMN,
                appendPathColumn.isPresent());
            nodeSettings.addString(JSONReaderMultiTableReadConfigSerializer.CFG_PATH_COLUMN_NAME,
                appendPathColumn.orElse(""));
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{JSONReaderMultiTableReadConfigSerializer.CFG_APPEND_PATH_COLUMN},
                {JSONReaderMultiTableReadConfigSerializer.CFG_PATH_COLUMN_NAME}};
        }
    }

    static final class SkipFirstDataRowsPersistor implements NodeParametersPersistor<SkipFirstDataRowsParameters> {

        @Override
        public SkipFirstDataRowsParameters load(final NodeSettingsRO nodeSettings) throws InvalidSettingsException {
            final boolean skipRows =
                nodeSettings.getBoolean(JSONReaderMultiTableReadConfigSerializer.CFG_SKIP_DATA_ROWS);
            if (skipRows) {
                final long numRowsToSkip =
                    nodeSettings.getLong(JSONReaderMultiTableReadConfigSerializer.CFG_NUMBER_OF_ROWS_TO_SKIP);
                return new SkipFirstDataRowsParameters(numRowsToSkip);
            } else {
                return new SkipFirstDataRowsParameters();
            }
        }

        @Override
        public void save(final SkipFirstDataRowsParameters param, final NodeSettingsWO nodeSettings) {
            nodeSettings.addBoolean(JSONReaderMultiTableReadConfigSerializer.CFG_SKIP_DATA_ROWS,
                param.getSkipFirstDataRows() > 0);
            nodeSettings.addLong(JSONReaderMultiTableReadConfigSerializer.CFG_NUMBER_OF_ROWS_TO_SKIP,
                param.getSkipFirstDataRows());
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{JSONReaderMultiTableReadConfigSerializer.CFG_SKIP_DATA_ROWS},
                {JSONReaderMultiTableReadConfigSerializer.CFG_NUMBER_OF_ROWS_TO_SKIP}};
        }
    }

    static final class MaxNumberOfRowsPersistor implements NodeParametersPersistor<MaxNumberOfRowsParameters> {

        @Override
        public MaxNumberOfRowsParameters load(final NodeSettingsRO nodeSettings) throws InvalidSettingsException {
            final boolean limitRows =
                nodeSettings.getBoolean(JSONReaderMultiTableReadConfigSerializer.CFG_LIMIT_DATA_ROWS);
            if (limitRows) {
                final long maxRows = nodeSettings.getLong(JSONReaderMultiTableReadConfigSerializer.CFG_MAX_ROWS);
                return new MaxNumberOfRowsParameters(maxRows);
            } else {
                return new MaxNumberOfRowsParameters();
            }
        }

        @Override
        public void save(final MaxNumberOfRowsParameters param, final NodeSettingsWO nodeSettings) {
            nodeSettings.addBoolean(JSONReaderMultiTableReadConfigSerializer.CFG_LIMIT_DATA_ROWS,
                param.getMaximumNumberOfRows().isPresent());
            nodeSettings.addLong(JSONReaderMultiTableReadConfigSerializer.CFG_MAX_ROWS,
                param.getMaximumNumberOfRows().orElse(0L));
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{JSONReaderMultiTableReadConfigSerializer.CFG_LIMIT_DATA_ROWS},
                {JSONReaderMultiTableReadConfigSerializer.CFG_MAX_ROWS}};
        }
    }

}
