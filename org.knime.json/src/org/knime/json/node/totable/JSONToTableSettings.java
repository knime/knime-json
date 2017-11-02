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
 *   3 Febr 2015 (Gabor): created
 */
package org.knime.json.node.totable;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.json.JSONValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * Settings for the JSON to Table node.
 *
 * @author Gabor Bakos
 */
final class JSONToTableSettings {
    /**
     * Config key for remove source column.
     */
    private static final String REMOVE_SOURCE_COLUMN = "removeSourceColumn";
    /** Default number of levels to expand up to. */
    static final int DEFAULT_UP_TO_N_LEVELS = 1;
    private static final String INPUT_COLUMN = "input.column", COLUMN_NAME_STRATEGY = "column.name.strategy", ARRAY_HANDLING = "array.handling", EXPANSION = "expansion", PATH_SEGMENT_SEPARATOR = "column.segment.separator";
    private static final String UP_TO_N = "up.to.n.levels";
    private static final String DEFAULT_PATH_SEGMENT_SEPARATOR = ".";
    private static final String OMIT_NESTED_OBJECTS = "omit.nested.objects";
    /** Default value for removing source column. */
    static final boolean DEFAULT_REMOVE_SOURCE_COLUMN = true;
    /** Default value for omit nested objects */
    static final boolean DEFAULT_OMIT_NESTED_OBJECTS = true;
    private String m_inputColumn = "", m_separator = DEFAULT_PATH_SEGMENT_SEPARATOR;
    private ColumnNamePattern m_columnNameStrategy = ColumnNamePattern.UniquifiedLeafNames;
    private ArrayHandling m_arrayHandling = ArrayHandling.GenerateCollectionCells;
    private Expansion m_expansion = Expansion.OnlyLeaves;
    private int m_upToNLevel = DEFAULT_UP_TO_N_LEVELS;
    private boolean m_removeSourceColumn = DEFAULT_REMOVE_SOURCE_COLUMN, m_omitNestedObjects = DEFAULT_OMIT_NESTED_OBJECTS;

    /**
     * Constructs the default settings.
     */
    JSONToTableSettings() {
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
     * @return the columnNameStrategy
     */
    final ColumnNamePattern getColumnNameStrategy() {
        return m_columnNameStrategy;
    }

    /**
     * @param columnNameStrategy the columnNameStrategy to set
     */
    final void setColumnNameStrategy(final ColumnNamePattern columnNameStrategy) {
        this.m_columnNameStrategy = columnNameStrategy;
    }

    /**
     * @return the separator
     */
    final String getSeparator() {
        return m_separator;
    }

    /**
     * @param separator the separator to set
     */
    final void setSeparator(final String separator) {
        this.m_separator = separator;
    }

    /**
     * @return the arrayHandling
     */
    final ArrayHandling getArrayHandling() {
        return m_arrayHandling;
    }

    /**
     * @param arrayHandling the arrayHandling to set
     */
    final void setArrayHandling(final ArrayHandling arrayHandling) {
        this.m_arrayHandling = arrayHandling;
    }

    /**
     * @return the expansion
     */
    final Expansion getExpansion() {
        return m_expansion;
    }

    /**
     * @param expansion the expansion to set
     */
    final void setExpansion(final Expansion expansion) {
        this.m_expansion = expansion;
    }

    /**
     * @return the upToNLevel
     */
    public int getUpToNLevel() {
        return m_upToNLevel;
    }

    /**
     * @param upToNLevel the upToNLevel to set
     */
    public void setUpToNLevel(final int upToNLevel) {
        this.m_upToNLevel = upToNLevel;
    }

    /**
     * @param removeSourceColumn the removeSourceColumn to set
     */
    public void setRemoveSourceColumn(final boolean removeSourceColumn) {
        m_removeSourceColumn = removeSourceColumn;
    }

    /**
     * @return the remove source column value.
     */
    public boolean isRemoveSourceColumn() {
        return m_removeSourceColumn;
    }

    /**
     * @return the omitNestedObjects
     */
    final boolean isOmitNestedObjects() {
        return m_omitNestedObjects;
    }

    /**
     * @param omitNestedObjects the omitNestedObjects to set
     */
    final void setOmitNestedObjects(final boolean omitNestedObjects) {
        this.m_omitNestedObjects = omitNestedObjects;
    }

    /**
     * Called from dialog when settings are to be loaded.
     *
     * @param settings To load from
     * @param inSpec Input spec
     */
    void loadSettingsDialog(final NodeSettingsRO settings, final DataTableSpec inSpec) {
        String inputColumn = inSpec == null ? null : jsonColumnNameOrNull(inSpec);
        m_inputColumn = settings.getString(INPUT_COLUMN, inputColumn);
        m_columnNameStrategy = ColumnNamePattern.valueOf(settings.getString(COLUMN_NAME_STRATEGY, ColumnNamePattern.JsonPathWithCustomSeparator.name()));
        m_separator = settings.getString(PATH_SEGMENT_SEPARATOR, DEFAULT_PATH_SEGMENT_SEPARATOR);
        m_expansion = Expansion.valueOf(settings.getString(EXPANSION, Expansion.OnlyLeaves.name()));
        m_upToNLevel = settings.getInt(UP_TO_N, DEFAULT_UP_TO_N_LEVELS);
        m_arrayHandling = ArrayHandling.valueOf(settings.getString(ARRAY_HANDLING, ArrayHandling.KeepAllArrayAsJsonArray.name()));
        m_removeSourceColumn = settings.getBoolean(REMOVE_SOURCE_COLUMN, DEFAULT_REMOVE_SOURCE_COLUMN);
        m_omitNestedObjects = settings.getBoolean(OMIT_NESTED_OBJECTS, DEFAULT_OMIT_NESTED_OBJECTS);
    }

    /**
     * @param inSpec
     * @return
     */
    private String jsonColumnNameOrNull(final DataTableSpec inSpec) {
        if (!inSpec.containsCompatibleType(JSONValue.class)) {
            return null;
        }
        for (DataColumnSpec dataColumnSpec : inSpec) {
            if (dataColumnSpec.getType().isCompatible(JSONValue.class)) {
                return dataColumnSpec.getName();
            }
        }
        assert false: inSpec;
        return null;
    }

    /**
     * Called from model when settings are to be loaded.
     *
     * @param settings To load from
     * @throws InvalidSettingsException If settings are invalid.
     */
    void loadSettingsModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_inputColumn = settings.getString(INPUT_COLUMN);
        m_columnNameStrategy = ColumnNamePattern.valueOf(settings.getString(COLUMN_NAME_STRATEGY));
        m_separator = settings.getString(PATH_SEGMENT_SEPARATOR);
        m_expansion = Expansion.valueOf(settings.getString(EXPANSION));
        m_upToNLevel = settings.getInt(UP_TO_N);
        m_arrayHandling = ArrayHandling.valueOf(settings.getString(ARRAY_HANDLING));
        m_removeSourceColumn = settings.getBoolean(REMOVE_SOURCE_COLUMN);
        m_omitNestedObjects = settings.getBoolean(OMIT_NESTED_OBJECTS);
    }

    /**
     * Called from model and dialog to save current settings.
     *
     * @param settings To save to.
     */
    void saveSettings(final NodeSettingsWO settings) {
        settings.addString(INPUT_COLUMN, m_inputColumn);
        settings.addString(COLUMN_NAME_STRATEGY, m_columnNameStrategy.name());
        settings.addString(PATH_SEGMENT_SEPARATOR, m_separator);
        settings.addString(EXPANSION, m_expansion.name());
        settings.addInt(UP_TO_N, m_upToNLevel);
        settings.addString(ARRAY_HANDLING, m_arrayHandling.name());
        settings.addBoolean(REMOVE_SOURCE_COLUMN, m_removeSourceColumn);
        settings.addBoolean(OMIT_NESTED_OBJECTS, m_omitNestedObjects);
    }
}
