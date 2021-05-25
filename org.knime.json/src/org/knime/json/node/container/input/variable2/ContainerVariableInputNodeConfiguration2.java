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
 *   17.05.2021 (jl): created
 */
package org.knime.json.node.container.input.variable2;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * Configuration of the Container Input (Variable) node.
 *
 * @author Jannik Löscher, KNIME GmbH, Konstanz, Germany
 * @since 4.4
 */
final class ContainerVariableInputNodeConfiguration2 {

    private boolean m_requireMatchSpecification;

    private boolean m_useSimpleJsonSpec;

    private boolean m_mergeVariables;

    /**
     * Constructs a new configuration object.
     */
    ContainerVariableInputNodeConfiguration2() {
        m_useSimpleJsonSpec = false;
    }

    /**
     * @return whether a simpler JSON specification should be used if possible
     */
    public boolean hasSimpleJsonSpec() {
        return m_useSimpleJsonSpec;
    }

    /**
     * @return whether the variables should be merged or replaced
     */
    public boolean hasMergeVariables() {
        return m_mergeVariables;
    }

    /**
     * @return whether the external input must match the specification
     */
    public boolean isRequireMatchSpecification() {
        return m_requireMatchSpecification;
    }

    /**
     * @param useSimpleJsonSpec whether a simpler JSON specification should be used if possible
     */
    public void setUseSimpleJsonSpec(final boolean useSimpleJsonSpec) {
        m_useSimpleJsonSpec = useSimpleJsonSpec;
    }

    /**
     * @param mergeVariables whether the variables should be merged or replaced
     */
    public void setMergeVariables(final boolean mergeVariables) {
        m_mergeVariables = mergeVariables;
    }

    /**
     * @param requireMatchSpecification whether the external input must match the specification
     */
    public void setRequireMatchSpecification(final boolean requireMatchSpecification) {
        m_requireMatchSpecification = requireMatchSpecification;
    }

    /**
     * Loads the settings from the given node settings object. Loading will fail if settings are missing or invalid.
     *
     * @param settings a node settings object
     * @throws InvalidSettingsException if settings are missing or invalid
     */
    void loadInModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        setUseSimpleJsonSpec(settings.getBoolean("useSimpleJsonSpec"));
        setMergeVariables(settings.getBoolean("mergeVariables"));
        setRequireMatchSpecification(settings.getBoolean("requireSpecification"));
    }

    /**
     * Loads the settings from the given node settings object. Default values will be used for missing or invalid
     * settings.
     *
     * @param settings a node settings object
     */
    void loadInDialog(final NodeSettingsRO settings) {
        try {
            setUseSimpleJsonSpec(settings.getBoolean("useSimpleJsonSpec"));
            setMergeVariables(settings.getBoolean("mergeVariables"));
            setRequireMatchSpecification(settings.getBoolean("requireSpecification"));
        } catch (InvalidSettingsException e) { // NOSONAR: use default values silently as demanded in the specification
            m_useSimpleJsonSpec = false;
            m_mergeVariables = false;
            m_requireMatchSpecification = false;
        }
    }

    /**
     * Saves the current configuration to the given settings object.
     *
     * @param settings a settings object
     */
    void save(final NodeSettingsWO settings) {
        settings.addBoolean("useSimpleJsonSpec", m_useSimpleJsonSpec);
        settings.addBoolean("mergeVariables", m_mergeVariables);
        settings.addBoolean("requireSpecification", m_requireMatchSpecification);
    }
}