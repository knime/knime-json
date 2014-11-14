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
 *   14 Nov. 2014 (Gabor): created
 */
package org.knime.json.node.util;

import org.knime.core.data.DataValue;

/**
 * A {@link RemoveOrAddColumnSettings} which allows to replace the content instead of just remove and add a new column.
 * Do not use the following key in your settings:
 * {@value #REPLACE_INPUT_COLUMN}
 *
 * @author Gabor Bakos
 */
public class ReplaceColumnSettings extends RemoveOrAddColumnSettings {
//    /** The config key whether to replace column or not. */
//    protected static final String REPLACE_INPUT_COLUMN = "replace.input.column";
//    private static final boolean DEFAULT_REPLACE_INPUT_COLUMN = true;
//
//    private boolean m_replaceColumn = DEFAULT_REPLACE_INPUT_COLUMN;

    /**
     * @param inputColumnType The input column's {@link DataValue} class.
     */
    public ReplaceColumnSettings(final Class<? extends DataValue> inputColumnType) {
        super(inputColumnType);
    }
//
//    /**
//     * @return the replaceColumn
//     */
//    final boolean isReplaceColumn() {
//        return m_replaceColumn;
//    }
//
//    /**
//     * @param replaceColumn the replaceColumn to set
//     */
//    final void setReplaceColumn(final boolean replaceColumn) {
//        this.m_replaceColumn = replaceColumn;
//    }
//
//    /**
//     * {@inheritDoc}
//     */
//    @Override
//    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
//        super.validateSettings(settings);
//        settings.getBoolean(REPLACE_INPUT_COLUMN);
//    }
//
//    /**
//     * {@inheritDoc}
//     */
//    @Override
//    protected void loadSettingsForDialogs(final NodeSettingsRO settings, final PortObjectSpec[] specs) {
//        super.loadSettingsForDialogs(settings, specs);
//        m_replaceColumn = settings.getBoolean(REPLACE_INPUT_COLUMN, DEFAULT_REPLACE_INPUT_COLUMN);
//    }
//
//    /**
//     * {@inheritDoc}
//     */
//    @Override
//    protected void loadSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
//        super.loadSettingsFrom(settings);
//        m_replaceColumn = settings.getBoolean(REPLACE_INPUT_COLUMN);
//    }
//
//    /**
//     * {@inheritDoc}
//     */
//    @Override
//    protected void saveSettingsTo(final NodeSettingsWO settings) {
//        super.saveSettingsTo(settings);
//        settings.addBoolean(REPLACE_INPUT_COLUMN, m_replaceColumn);
//    }
}
