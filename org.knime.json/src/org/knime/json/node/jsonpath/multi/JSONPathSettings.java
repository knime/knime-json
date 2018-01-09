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
 *   28 Sept 2014 (Gabor): created
 */
package org.knime.json.node.jsonpath.multi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.json.JSONValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.json.node.util.PathOrPointerSettings;

/**
 * The settings object for the JSONPath node.
 *
 * @author Gabor Bakos
 */
final class JSONPathSettings {
    /**
     * The key for the remove or not the source/input column boolean value ({@code true} means remove).
     */
    static final String REMOVE_SOURCE = "remove.input.column";

    /**
     * The key for the input column.
     */
    static final String INPUT_COLUMN = "input.column";

    private static final boolean DEFAULT_REMOVE_SOURCE = false;

    private String m_inputColumnName;

    private boolean m_removeInputColumn = DEFAULT_REMOVE_SOURCE;

    private NodeLogger m_logger;

    private final List<SingleSetting> m_outputSettings = new ArrayList<>();

    /**
     * Constructs the {@link JSONPathSettings} object.
     *
     * @param logger The logger to log warnings and errors.
     */
    JSONPathSettings(final NodeLogger logger) {
        m_logger = logger;
    }

    /**
     * @return the inputColumnName
     */
    protected final String getInputColumnName() {
        return m_inputColumnName;
    }

    /**
     * @param inputColumnName the inputColumnName to set
     */
    protected final void setInputColumnName(final String inputColumnName) {
        this.m_inputColumnName = inputColumnName;
    }

    /**
     * @return the removeInputColumn
     */
    protected final boolean isRemoveInputColumn() {
        return m_removeInputColumn;
    }

    /**
     * @param removeInputColumn the removeInputColumn to set
     */
    protected final void setRemoveInputColumn(final boolean removeInputColumn) {
        this.m_removeInputColumn = removeInputColumn;
    }

    /**
     * Loads the settings with defaults if those are not available. It might use the input table {@code specs}.
     *
     * @param settings The settings containing the parameter values.
     * @param specs The input table specs.
     */
    protected void loadSettingsForDialogs(final NodeSettingsRO settings, final PortObjectSpec... specs) {
        DataTableSpec spec = (DataTableSpec)specs[0];
        m_inputColumnName = settings.getString(INPUT_COLUMN, defaultColumnName(spec));
        m_removeInputColumn = settings.getBoolean(REMOVE_SOURCE, DEFAULT_REMOVE_SOURCE);
        int n = settings.getStringArray(SingleSetting.RETURN_TYPE, new String[0]).length;
        m_outputSettings.clear();
        for (int i = 0; i < n; i++) {
            SingleSetting ss = new SingleSetting();
            m_outputSettings.add(ss);
            ss.loadSettingsForDialogs(settings, i);
        }
    }

    /**
     * @param spec The input table {@link DataTableSpec spec}.
     * @return The selectable JSON column or {@code ""}.
     */
    private String defaultColumnName(final DataTableSpec spec) {
        if (spec != null && spec.containsCompatibleType(JSONValue.class)) {
            int found = 0;
            String selectedColumnName = "";
            for (DataColumnSpec col : spec) {
                if (col.getType().isCompatible(JSONValue.class)) {
                    found++;
                    selectedColumnName = col.getName();
                }
            }
            if (found > 1) {
                m_logger.warn("There were multiple JSON columns, selected " + selectedColumnName);
            }
            m_logger.assertLog(found > 0, "Not found JSON column: " + spec);
            return selectedColumnName;
        } else {
            m_logger.warn("No JSON column in the input table: " + spec);
            return "";
        }
    }

    /**
     * Loads the settings without defaults, used by the node model.
     *
     * @param settings The node settings.
     * @throws InvalidSettingsException Wrong settings were loaded.
     */
    protected void loadSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_inputColumnName = settings.getString(INPUT_COLUMN);
        m_removeInputColumn = settings.getBoolean(REMOVE_SOURCE);
        int n = settings.getStringArray(SingleSetting.RETURN_TYPE).length;
        m_outputSettings.clear();
        for (int i = 0; i < n; i++) {
            SingleSetting ss = new SingleSetting();
            m_outputSettings.add(ss);
            ss.loadSettingsFrom(settings, i);
        }
    }

    /**
     * Persists the settings to {@code settings}.
     *
     * @param settings The node settings to write the current settings (within the model or the dialog).
     */
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addString(INPUT_COLUMN, m_inputColumnName);
        settings.addBoolean(REMOVE_SOURCE, m_removeInputColumn);
        String[] paths = new String[m_outputSettings.size()], returnTypes = new String[m_outputSettings.size()], colNames =
            new String[m_outputSettings.size()];
        boolean[] resultIsListArray = new boolean[m_outputSettings.size()], resultPaths =
            new boolean[m_outputSettings.size()];
        for (int i = 0; i < m_outputSettings.size(); ++i) {
            SingleSetting singleSetting = m_outputSettings.get(i);
            colNames[i] = singleSetting.getNewColumnName();
            paths[i] = singleSetting.getJsonPath();
            resultIsListArray[i] = singleSetting.isResultIsList();
            resultPaths[i] = singleSetting.isReturnPaths();
            returnTypes[i] = (singleSetting.getReturnType() == null ? PathOrPointerSettings.DEFAULT_RETURN_TYPE :
                singleSetting.getReturnType()).name();
        }
        settings.addStringArray(SingleSetting.NEW_COLUMN_NAMES, colNames);
        settings.addStringArray(SingleSetting.JSON_PATH, paths);
        settings.addBooleanArray(SingleSetting.RESULT_IS_LIST, resultIsListArray);
        settings.addBooleanArray(SingleSetting.RETURN_PATHS, resultPaths);
        settings.addStringArray(SingleSetting.RETURN_TYPE, returnTypes);
    }

    /**
     * Selects the {@link SingleSetting} representing the {@code index}th of the new columns.
     *
     * @param index The ({@code 0}-based) index of the new column.
     * @return The {@link SingleSetting}.
     * @throws IndexOutOfBoundsException when cannot be selected.
     */
    SingleSetting getOutputSetting(final int index) {
        return m_outputSettings.get(index);
    }

    /**
     * @return A view of the {@link SingleSetting}s.
     */
    Iterable<SingleSetting> getOutputSettings() {
        return Collections.unmodifiableList(m_outputSettings);
    }

    /**
     * Removes the {@code index}th output column definition setting.
     *
     * @param index A valid index ({@code 0-based}).
     * @throws IndexOutOfBoundsException When the index is invalid.
     */
    void removeOutputSetting(final int index) {
        m_outputSettings.remove(index);
    }

    /**
     * Adds a new {@link SingleSetting} to the end of the new column generator settings.
     *
     * @param setting The new {@link SingleSetting}.
     */
    void addOutputSetting(final SingleSetting setting) {
        m_outputSettings.add(setting);
    }

    /**
     * Removes all new column generation settings.
     */
    void clearOutputSettings() {
        m_outputSettings.clear();
    }
}
