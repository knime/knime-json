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
 *   4 Mar 2015 (Gabor): created
 */
package org.knime.json.node.fromtable;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterConfiguration;
import org.knime.core.node.util.filter.column.DataTypeColumnFilter;
import org.knime.json.node.fromtable.TableToJsonNodeModel.Direction;
import org.knime.json.node.fromtable.TableToJsonNodeModel.RowKeyOption;
import org.knime.json.node.jsonpath.util.JsonPathUtils;

/**
 * Settings object for the Table To JSON node.
 *
 * @author Gabor Bakos
 */
class TableToJsonSettings {
    private static final String CFGKEY_SELECTED_COLUMNS = "selectedColumns";

    private static final String CFGKEY_ROWKEY_KEY = "rowkey.key";

    private static final String CFGKEY_DIRECTION = "direction";

    private static final String CFGKEY_COLUMN_NAMES_AS_PATH = "column.names.as.path";

    private static final String CFGKEY_ROW_KEY_OPTION = "row.key.option";

    private static final String CFGKEY_COLUMN_NAME_SEPARATOR = "column.name.separator";

    private static final String CFGKEY_OUTPUT_COLUMN_NAME = "output.column.name";

    private static final String CFGKEY_REMOVE_SOURCE_COLUMNS = "remove.source.columns";

    private static final String CFGKEY_BOOLEANS_AS_NUMBERS = "output.boolean.asNumbers";

    private static final String CFGKEY_MISSINGS_ARE_OMITTED = "missing.values.are.omitted";

    static final String DEFAULT_ROWKEY_KEY = "key";

    static final Direction DEFAULT_DIRECTION = Direction.RowsOutside;

    static final boolean DEFAULT_COLUMN_NAMES_AS_PATH = false;

    static final String DEFAULT_COLUMN_NAME_SEPARATOR = ".";

    static final String DEFAULT_OUTPUT_COLUMN_NAME = "JSON";

    static final RowKeyOption DEFAULT_ROW_KEY_OPTION = RowKeyOption.omit;

    static final boolean DEFAULT_REMOVE_SOURCE_COLUMNS = false;

    static final boolean COMPAT_MISSINGS_ARE_OMITTED = false, DEFAULT_MISSINGS_ARE_OMITTED = true;

    private final DataColumnSpecFilterConfiguration m_selectedColumns = new DataColumnSpecFilterConfiguration(
        CFGKEY_SELECTED_COLUMNS, new DataTypeColumnFilter(JsonPathUtils.supportedInputDataValuesAsArray()));

    private String m_rowKeyKey = DEFAULT_ROWKEY_KEY;

    private Direction m_direction = DEFAULT_DIRECTION;

    private boolean m_columnNamesAsPath = DEFAULT_COLUMN_NAMES_AS_PATH, m_removeSourceColumns = DEFAULT_REMOVE_SOURCE_COLUMNS;

    private RowKeyOption m_rowKey = DEFAULT_ROW_KEY_OPTION;

    private String m_columnNameSeparator = DEFAULT_COLUMN_NAME_SEPARATOR;

    private String m_outputColumnName = DEFAULT_OUTPUT_COLUMN_NAME;

    private boolean m_booleansAsNumbers;

    private boolean m_missingsAreOmitted = DEFAULT_MISSINGS_ARE_OMITTED;

    TableToJsonSettings() {
    }

    /**
     * Called from dialog when settings are to be loaded.
     *
     * @param settings To load from
     * @param inSpec Input spec
     */
    protected void loadSettingsDialog(final NodeSettingsRO settings, final DataTableSpec inSpec) {
        m_selectedColumns.loadConfigurationInDialog(settings, inSpec);
        m_rowKeyKey = settings.getString(CFGKEY_ROWKEY_KEY, DEFAULT_ROWKEY_KEY);
        m_direction = Direction.valueOf(settings.getString(CFGKEY_DIRECTION, DEFAULT_DIRECTION.name()));
        m_columnNameSeparator = settings.getString(CFGKEY_COLUMN_NAME_SEPARATOR, DEFAULT_COLUMN_NAME_SEPARATOR);
        m_outputColumnName = settings.getString(CFGKEY_OUTPUT_COLUMN_NAME, DEFAULT_OUTPUT_COLUMN_NAME);
        m_rowKey = RowKeyOption.valueOf(settings.getString(CFGKEY_ROW_KEY_OPTION, DEFAULT_ROW_KEY_OPTION.name()));
        m_columnNamesAsPath = settings.getBoolean(CFGKEY_COLUMN_NAMES_AS_PATH, DEFAULT_COLUMN_NAMES_AS_PATH);
        m_removeSourceColumns = settings.getBoolean(CFGKEY_REMOVE_SOURCE_COLUMNS, DEFAULT_REMOVE_SOURCE_COLUMNS);

        // for existing nodes this value should be true; for new nodes it should default to false (see AP-5685)
        m_booleansAsNumbers =
            !settings.containsKey(CFGKEY_DIRECTION) || settings.getBoolean(CFGKEY_BOOLEANS_AS_NUMBERS, false);
        //Since 3.3
        m_missingsAreOmitted = settings.getBoolean(CFGKEY_MISSINGS_ARE_OMITTED, DEFAULT_MISSINGS_ARE_OMITTED);
    }

    /**
     * Called from model when settings are to be loaded.
     *
     * @param settings To load from
     * @throws InvalidSettingsException If settings are invalid.
     */
    protected void loadSettingsModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_selectedColumns.loadConfigurationInModel(settings);
        m_rowKeyKey = settings.getString(CFGKEY_ROWKEY_KEY);
        m_direction = Direction.valueOf(settings.getString(CFGKEY_DIRECTION));
        m_columnNameSeparator = settings.getString(CFGKEY_COLUMN_NAME_SEPARATOR);
        m_outputColumnName = settings.getString(CFGKEY_OUTPUT_COLUMN_NAME);
        m_rowKey = RowKeyOption.valueOf(settings.getString(CFGKEY_ROW_KEY_OPTION));
        m_columnNamesAsPath = settings.getBoolean(CFGKEY_COLUMN_NAMES_AS_PATH);
        m_removeSourceColumns = settings.getBoolean(CFGKEY_REMOVE_SOURCE_COLUMNS);
        m_booleansAsNumbers = settings.getBoolean(CFGKEY_BOOLEANS_AS_NUMBERS, true);
        //Since 3.3
        m_missingsAreOmitted = settings.getBoolean(CFGKEY_MISSINGS_ARE_OMITTED, COMPAT_MISSINGS_ARE_OMITTED);
    }

    /**
     * Called from model and dialog to save current settings.
     *
     * @param settings To save to.
     */
    protected void saveSettings(final NodeSettingsWO settings) {
        m_selectedColumns.saveConfiguration(settings);
        settings.addString(CFGKEY_ROWKEY_KEY, m_rowKeyKey);
        settings.addString(CFGKEY_DIRECTION, m_direction.name());
        settings.addString(CFGKEY_COLUMN_NAME_SEPARATOR, m_columnNameSeparator);
        settings.addString(CFGKEY_OUTPUT_COLUMN_NAME, m_outputColumnName);
        settings.addString(CFGKEY_ROW_KEY_OPTION, m_rowKey.name());
        settings.addBoolean(CFGKEY_COLUMN_NAMES_AS_PATH, m_columnNamesAsPath);
        settings.addBoolean(CFGKEY_REMOVE_SOURCE_COLUMNS, m_removeSourceColumns);
        settings.addBoolean(CFGKEY_BOOLEANS_AS_NUMBERS, m_booleansAsNumbers);
        //Since 3.3
        settings.addBoolean(CFGKEY_MISSINGS_ARE_OMITTED, m_missingsAreOmitted);
    }

    /**
     * Returns whether boolean values should be translated to JSON numbers or if they should translate to booleans.
     *
     * @return <code>true</code> if booleans should be translated as numbers, <code>false</code> otherwise
     */
    boolean isBooleansAsNumbers() {
        return m_booleansAsNumbers;
    }

    /**
     * @return the rowKeyKey
     */
    final String getRowKeyKey() {
        return m_rowKeyKey;
    }

    /**
     * @param rowKeyKey the rowKeyKey to set
     */
    final void setRowKeyKey(final String rowKeyKey) {
        this.m_rowKeyKey = rowKeyKey;
    }

    /**
     * @return the direction
     */
    final Direction getDirection() {
        return m_direction;
    }

    /**
     * @param direction the direction to set
     */
    final void setDirection(final Direction direction) {
        this.m_direction = direction;
    }

    /**
     * @return the columnNameSeparator
     */
    final String getColumnNameSeparator() {
        return m_columnNameSeparator;
    }

    /**
     * @param columnNameSeparator the columnNameSeparator to set
     */
    final void setColumnNameSeparator(final String columnNameSeparator) {
        this.m_columnNameSeparator = columnNameSeparator;
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
     * @return the selectedColumns
     */
    final DataColumnSpecFilterConfiguration getSelectedColumns() {
        return m_selectedColumns;
    }

    /**
     * @return the columnNamesAsPath
     */
    final boolean isColumnNamesAsPath() {
        return m_columnNamesAsPath;
    }

    /**
     * @param columnNamesAsPath the columnNamesAsPath to set
     */
    final void setColumnNamesAsPath(final boolean columnNamesAsPath) {
        this.m_columnNamesAsPath = columnNamesAsPath;
    }

    /**
     * @return the rowKey
     */
    final RowKeyOption getRowKey() {
        return m_rowKey;
    }

    /**
     * @param rowKey the rowKey to set
     */
    final void setRowKey(final RowKeyOption rowKey) {
        this.m_rowKey = rowKey;
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
     * @return the missingsAreOmitted
     */
    final boolean isMissingsAreOmitted() {
        return m_missingsAreOmitted;
    }

    /**
     * @param missingsAreOmitted the missingsAreOmitted to set
     */
    final void setMissingsAreOmitted(final boolean missingsAreOmitted) {
        m_missingsAreOmitted = missingsAreOmitted;
    }



//    /**
//     * Autoconfigures the input column based on the input {@code dataTableSpec}.
//     *
//     * @param dataTableSpec The input {@link DataTableSpec}.
//     * @return The warning or an empty {@link String}.
//     * @throws InvalidSettingsException When there are no JSON columns.
//     */
//    protected String autoConfigure(final DataTableSpec dataTableSpec) throws InvalidSettingsException {
//        if (null == getInputColumn()) {
//            List<String> compatibleCols = new ArrayList<String>();
//            for (DataColumnSpec c : dataTableSpec) {
//                if (c.getType().isCompatible(JSONValue.class)) {
//                    compatibleCols.add(c.getName());
//                }
//            }
//            if (compatibleCols.size() == 1) {
//                // auto-configure
//                setInputColumn(compatibleCols.get(0));
//            } else if (compatibleCols.size() > 1) {
//                // auto-guessing
//                setInputColumn(compatibleCols.get(0));
//                return "Auto guessing: using column \"" + compatibleCols.get(0) + "\".";
//            } else {
//                throw new InvalidSettingsException("No JSON " + "column in input table."
//                    + " Try using the Columns to JSON node before this node.");
//            }
//        }
//        return "";
//    }
}
