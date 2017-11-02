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
 *   11 Febr 2015 (Gabor): created
 */
package org.knime.json.node.jsonpath.multi;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.json.util.OutputType;

/**
 * Single setting for the JSONPath query.
 *
 * @author Gabor Bakos
 */
class SingleSetting {
    /** Key for JSONPath */
    static final String JSON_PATH = "jsonpath";

    /** Default value for JSONPath (all except the root) */
    static final String DEFAULT_JSON_PATH = "$..*";

    /** Key to generate list as the result */
    static final String RESULT_IS_LIST = "result.is.list";

    /** Default value to generate list as a result. */
    static final boolean DEFAULT_RESULT_IS_LIST = true;

    /** Key to return the JSONPaths instead of the values. */
    static final String RETURN_PATHS = "return.paths.instead.of.values";

    /** Default value to return JSONPaths (no, return the values by default). */
    static final boolean DEFAULT_RETURN_PATHS = false;

    /** Default value for the return type settings. */
    static final OutputType DEFAULT_RETURN_TYPE = OutputType.Json;

    /** Key for the String type return type settings (encoded as enum by its {@link Enum#name()}). */
    static final String RETURN_TYPE = "returnType";

    /** Key for the new column name */
    static final String NEW_COLUMN_NAMES = "newColumnName";

    private OutputType m_returnType = DEFAULT_RETURN_TYPE;

    private boolean m_resultIsList = DEFAULT_RESULT_IS_LIST;

    private boolean m_returnPaths = DEFAULT_RETURN_PATHS;

    private String m_jsonPath = DEFAULT_JSON_PATH;

    private String m_newColumnName;

    /**
     * @return the jsonPath
     */
    final String getJsonPath() {
        return m_jsonPath;
    }

    /**
     * @param jsonPath the jsonPath to set
     */
    final void setJsonPath(final String jsonPath) {
        this.m_jsonPath = jsonPath;
    }

    /**
     * @return the resultIsList
     */
    final boolean isResultIsList() {
        return m_resultIsList;
    }

    /**
     * @param resultIsList the resultIsList to set
     */
    final void setResultIsList(final boolean resultIsList) {
        this.m_resultIsList = resultIsList;
    }

    /**
     * @return the returnPaths
     */
    final boolean isReturnPaths() {
        return m_returnPaths;
    }

    /**
     * @param returnPaths the returnPaths to set
     */
    final void setReturnPaths(final boolean returnPaths) {
        this.m_returnPaths = returnPaths;
    }

    /**
     * @return the returnType
     */
    public final OutputType getReturnType() {
        return m_returnType;
    }

    /**
     * @param returnType the returnType to set
     */
    public final void setReturnType(final OutputType returnType) {
        this.m_returnType = returnType;
    }

    /**
     * @return the newColumnName
     */
    protected final String getNewColumnName() {
        return m_newColumnName;
    }

    /**
     * @param newColumnName the newColumnName to set
     */
    protected final void setNewColumnName(final String newColumnName) {
        this.m_newColumnName = newColumnName;
    }

    /**
     * @param outputType {@link OutputType}'s {@link OutputType#name()}.
     * @return The parsed {@link OutputType}.
     * @throws InvalidSettingsException Wrong format, or {@code null}.
     */
    private OutputType toOutputType(final String outputType) throws InvalidSettingsException {
        try {
            return OutputType.valueOf(outputType);
        } catch (RuntimeException e) {
            throw new InvalidSettingsException("Invalid return type: " + outputType, e);
        }
    }

    /**
     * Loads the settings to this object to be used by the dialog.
     *
     * @param settings The node settings to read from.
     * @param index The {@code 0}-based index of this object.
     * @throws IndexOutOfBoundsException when the {@code index} is negative, otherwise defaults are used.
     */
    protected void loadSettingsForDialogs(final NodeSettingsRO settings, final int index) {
        String[] paths = settings.getStringArray(JSON_PATH, new String[0]);
        String[] colNames = settings.getStringArray(NEW_COLUMN_NAMES, new String[0]);
        m_jsonPath = paths.length > index ? paths[index] : DEFAULT_JSON_PATH;
        m_newColumnName = colNames.length > index ? colNames[index] : "";
        String[] outputTypes = settings.getStringArray(RETURN_TYPE, new String[0]);
        try {
            m_returnType = outputTypes.length > index ? toOutputType(outputTypes[index]) : DEFAULT_RETURN_TYPE;
        } catch (InvalidSettingsException e) {
            throw new IllegalArgumentException("Unknown output type: " + outputTypes[index]);
        }
        boolean[] resultIsListArray = settings.getBooleanArray(RESULT_IS_LIST, new boolean[0]);
        m_resultIsList = resultIsListArray.length > index ? resultIsListArray[index] : DEFAULT_RESULT_IS_LIST;
        boolean[] returnPaths = settings.getBooleanArray(RETURN_PATHS, new boolean[0]);
        m_returnPaths = returnPaths.length > index ? returnPaths[index] : DEFAULT_RETURN_PATHS;
    }

    /**
     * Loads the settings to be used in the model.
     *
     * @param settings The node settings to read from.
     * @param index The {@code 0}-based index of this object.
     * @throws InvalidSettingsException When could not be loaded or invalid value was loaded.
     * @throws IndexOutOfBoundsException when {@code index} is less than {@code 0}.
     */
    protected void loadSettingsFrom(final NodeSettingsRO settings, final int index) throws InvalidSettingsException {
        String[] paths = settings.getStringArray(JSON_PATH), colNames = settings.getStringArray(NEW_COLUMN_NAMES);
        m_jsonPath = paths.length > index ? paths[index] : SingleSetting.<String> notEnoughArguments("JSONPath");
        m_newColumnName =
            colNames.length > index ? colNames[index] : SingleSetting.<String> notEnoughArguments("output column name");
        String[] outputTypes = settings.getStringArray(RETURN_TYPE);
        m_returnType =
            outputTypes.length > index ? toOutputType(outputTypes[index]) : SingleSetting
                .<OutputType> notEnoughArguments("result type");
        boolean[] resultIsListArray = settings.getBooleanArray(RESULT_IS_LIST);
        m_resultIsList =
            resultIsListArray.length > index ? resultIsListArray[index] : SingleSetting
                .<Boolean> notEnoughArguments("result is array");
        boolean[] returnPaths = settings.getBooleanArray(RETURN_PATHS);
        m_returnPaths =
            returnPaths.length > index ? returnPaths[index] : SingleSetting
                .<Boolean> notEnoughArguments("return paths");
    }

    private static <T> T notEnoughArguments(final String text) throws InvalidSettingsException {
        throw new InvalidSettingsException("Not enough arguments for the " + text + " parameter.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return String.format("SingleSetting [%s, %s, %s, returnPaths=%s, resultIsList=%s]", m_newColumnName,
            m_jsonPath, m_returnType, m_returnPaths, m_resultIsList);
    }
}
