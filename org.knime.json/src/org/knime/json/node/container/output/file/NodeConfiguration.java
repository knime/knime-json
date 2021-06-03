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
 *   21.04.2021 (loescher): created
 */
package org.knime.json.node.container.output.file;

import java.util.Optional;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * Contains the “Container Output (File)” node specific settings
 *
 * @author Jannik Löscher, KNIME GmbH, Konstanz, Germany
 */
final class NodeConfiguration {

    private static final String CFG_SELECTED_FLOW_VAR_NAME_KEY = "selectedFlowVariableName";

    private String m_flowVariableName;

    /**
     * Constructs the “Container Output (File)” node specific settings
     */
    NodeConfiguration() {
        reset();
    }

    /**
     * @return the name of the flow variable to use for the file download or <code>null</code> if no such name is
     *         selected
     */
    public Optional<String> getFlowVariableName() {
        return Optional.ofNullable(m_flowVariableName);
    }

    /**
     * @param flowVariableName the name of the flow variable to use for the file download if present
     */
    public void setFlowVariableName(final String flowVariableName) {
        m_flowVariableName = flowVariableName;
    }

    /**
     * Resets the internal values to their default. This does not clean the files got by {@link #getExternalURI()} or
     * {@link #getLocalLocation()}.
     */
    void reset() {
        m_flowVariableName = null;
    }

    /**
     * Saves the settings of this configuration to the given settings object.
     *
     * @param settings the settings to save to.
     */
    void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addString(CFG_SELECTED_FLOW_VAR_NAME_KEY, m_flowVariableName);
    }

    /**
     * Validates the given settings. Please refer to the given setters.
     *
     * @param settings the settings to be validated.
     * @throws InvalidSettingsException if the settings were invalid
     */
    static void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        settings.getString(CFG_SELECTED_FLOW_VAR_NAME_KEY);
    }

    /**
     * Loads the validated settings from the given settings object.
     *
     * @param settings the settings from which to load.
     * @throws InvalidSettingsException if the needed settings are not present
     */
    void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_flowVariableName = settings.getString(CFG_SELECTED_FLOW_VAR_NAME_KEY);
    }

}
