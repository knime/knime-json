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
 *   27 Sept 2014 (Gabor): created
 */
package org.knime.json.node.util;

import org.knime.core.data.DataValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObjectSpec;

/**
 * Common node settings to specify whether to replace a single column or add a new one. <br/>
 * <b>Do not use the following keys:</b>
 * <ul>
 * <li>{@value SingleColumnReplaceOrAddNodeModel#APPEND}</li>
 * <li>{@value SingleColumnReplaceOrAddNodeModel#INPUT_COLUMN}</li>
 * <li>{@value SingleColumnReplaceOrAddNodeModel#NEW_COLUMN_NAME}</li>
 * </ul>
 *
 * @author Gabor Bakos
 */
public class ReplaceOrAddColumnSettings {
    /**
     * The default error message for no JSON columns.
     */
    public static final String NO_JSON_COLUMNS_USE_FOR_EXAMPLE_THE_STRING_TO_JSON_NODE_TO_CREATE_ONE =
        "No JSON columns! Use for example the \"String to JSON\" node to create one.";

    private String m_newColumnName, m_inputColumnName;

    private boolean m_append;

    private final Class<? extends DataValue> m_inputColumnType;

    /**
     * @param inputColumnType
     */
    public ReplaceOrAddColumnSettings(final Class<? extends DataValue> inputColumnType) {
        super();
        m_inputColumnType = inputColumnType;
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
     * @return the append
     */
    protected final boolean isAppend() {
        return m_append;
    }

    /**
     * @param append the append to set
     */
    protected final void setAppend(final boolean append) {
        this.m_append = append;
    }

    /**
     *
     * @param settings
     * @throws InvalidSettingsException
     */
    protected void loadSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_inputColumnName = settings.getString(SingleColumnReplaceOrAddNodeModel.INPUT_COLUMN);
        m_append = settings.getBoolean(SingleColumnReplaceOrAddNodeModel.APPEND);
        m_newColumnName = settings.getString(SingleColumnReplaceOrAddNodeModel.NEW_COLUMN_NAME);
    }

    /**
     *
     * @param settings
     * @throws InvalidSettingsException
     */
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        String inputColumnName = settings.getString(SingleColumnReplaceOrAddNodeModel.INPUT_COLUMN);
        if (inputColumnName.isEmpty()) {
            throw new InvalidSettingsException("No input column was selected!");
        }
        boolean append = settings.getBoolean(SingleColumnReplaceOrAddNodeModel.APPEND);
        String newColumnName = settings.getString(SingleColumnReplaceOrAddNodeModel.NEW_COLUMN_NAME);
        if (append && newColumnName.isEmpty()) {
            throw new InvalidSettingsException("No new column name was specified!");
        }

    }

    /**
     *
     * @param settings
     */
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addString(SingleColumnReplaceOrAddNodeModel.INPUT_COLUMN, m_inputColumnName);
        settings.addBoolean(SingleColumnReplaceOrAddNodeModel.APPEND, m_append);
        settings.addString(SingleColumnReplaceOrAddNodeModel.NEW_COLUMN_NAME, m_newColumnName);
    }

    /**
     * @param settings
     * @param specs
     */
    protected void loadSettingsForDialogs(final NodeSettingsRO settings, final PortObjectSpec[] specs) {
        m_inputColumnName = settings.getString(SingleColumnReplaceOrAddNodeModel.INPUT_COLUMN, "");
        m_append = settings.getBoolean(SingleColumnReplaceOrAddNodeModel.APPEND, false);
        m_newColumnName = settings.getString(SingleColumnReplaceOrAddNodeModel.NEW_COLUMN_NAME, "");
    }

    /**
     * @return the inputColumnType
     */
    public final Class<? extends DataValue> getInputColumnType() {
        return m_inputColumnType;
    }

    /**
     * @return The {@code 0}-based index of the input table to transform.
     */
    public int inputTableIndex() {
        return 0;
    }

    /**
     * @return The message to report when autoguessing failed, by default it is JSON-specific.
     */
    protected String autoGuessFailedMessage() {
        return NO_JSON_COLUMNS_USE_FOR_EXAMPLE_THE_STRING_TO_JSON_NODE_TO_CREATE_ONE;
    }
}
