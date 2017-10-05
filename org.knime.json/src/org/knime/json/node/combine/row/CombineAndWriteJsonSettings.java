/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *   20 Dec 2014 (Gabor): created
 */
package org.knime.json.node.combine.row;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * The JSON Combine and Write node specific settings class.
 *
 * @author Gabor Bakos
 */
final class CombineAndWriteJsonSettings extends RowCombineSettings {
    static final boolean DEFAULT_OVERWRITE = false, DEFAULT_PRETTY_PRINT = false;
    static final String OUTPUT_FILE = "file";
    static final String OVERWRITE_EXISTING_FILE = "overwriteExistingFile";
    static final String PRETTY_PRINT = "prettyPrint";
    static final String ROOT_KEY_TYPE = "rootKeyType";
    private String m_outputFile;
    private boolean m_overwrite = DEFAULT_OVERWRITE;
    private boolean m_prettyPrint = DEFAULT_PRETTY_PRINT;

    /**
     *
     */
    public CombineAndWriteJsonSettings() {
    }

    /**
     * @return the outputFile
     */
    final String getOutputFile() {
        return m_outputFile;
    }

    /**
     * @param outputFile the outputFile to set
     */
    final void setOutputFile(final String outputFile) {
        this.m_outputFile = outputFile;
    }

    /**
     * @return the overwrite
     */
    final boolean isOverwrite() {
        return m_overwrite;
    }

    /**
     * @param overwrite the overwrite to set
     */
    final void setOverwrite(final boolean overwrite) {
        this.m_overwrite = overwrite;
    }

    /**
     * @return the prettyPrint
     */
    final boolean isPrettyPrint() {
        return m_prettyPrint;
    }

    /**
     * @param prettyPrint the prettyPrint to set
     */
    final void setPrettyPrint(final boolean prettyPrint) {
        this.m_prettyPrint = prettyPrint;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsDialog(final NodeSettingsRO settings, final DataTableSpec inSpec) {
        super.loadSettingsDialog(settings, inSpec);
        m_outputFile = settings.getString(OUTPUT_FILE, null);
        m_overwrite = settings.getBoolean(OVERWRITE_EXISTING_FILE, DEFAULT_OVERWRITE);
        m_prettyPrint = settings.getBoolean(PRETTY_PRINT, DEFAULT_PRETTY_PRINT);
    }
    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.loadSettingsModel(settings);
        m_outputFile = settings.getString(OUTPUT_FILE);
        m_overwrite = settings.getBoolean(OVERWRITE_EXISTING_FILE);
        m_prettyPrint = settings.getBoolean(PRETTY_PRINT);
    }
    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettings(final NodeSettingsWO settings) {
        settings.addString(OUTPUT_FILE, m_outputFile);
        settings.addBoolean(OVERWRITE_EXISTING_FILE, m_overwrite);
        settings.addBoolean(PRETTY_PRINT, m_prettyPrint);
        super.saveSettings(settings);
    }
}
