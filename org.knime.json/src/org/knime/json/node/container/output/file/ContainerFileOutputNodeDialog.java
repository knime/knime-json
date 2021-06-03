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
 *   27.05.2021 (jl): created
 */
package org.knime.json.node.container.output.file;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Optional;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.FlowVariableListCellRenderer;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.json.node.container.input.file.ContainerNodeSharedDialogPanel;

/**
 * The dialog for the “Container Output (File)” node.
 *
 * @author Jannik Löscher, KNIME GmbH, Konstanz, Germany
 */
final class ContainerFileOutputNodeDialog extends NodeDialogPane {

    private final ContainerNodeSharedDialogPanel m_sharedPanel =
        new ContainerNodeSharedDialogPanel(ContainerFileOutputNodeModel.CFG_PARAMETER_DEFAULT);

    private final NodeConfiguration m_config = new NodeConfiguration();

    private final JComboBox<FlowVariable> m_flowVariable;

    /**
     * Constructs a new dialog for the “Container Input (File)” node
     */
    public ContainerFileOutputNodeDialog() {

        m_flowVariable = new JComboBox<>(new DefaultComboBoxModel<>());

        final var settingsPanel = new JPanel(new GridBagLayout());
        final var constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1.0;
        constraints.gridx = 0;

        settingsPanel.add(setupSharedPanel(), constraints);

        constraints.fill = GridBagConstraints.BOTH;
        constraints.weighty = 1.0;
        settingsPanel.add(setupOutputPanel(), constraints);

        addTab("Container Output (File)", settingsPanel);
    }

    private JPanel setupSharedPanel() {
        return m_sharedPanel;
    }

    private JPanel setupOutputPanel() {
        final var outputPanel = new JPanel(new GridBagLayout());
        outputPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "File"));

        final var variableSelectionDescription = new JLabel("File Path Variable:");
        m_flowVariable.setRenderer(new FlowVariableListCellRenderer());

        // setup layout
        final var constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.WEST;
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.insets = new Insets(5, 5, 5, 5);
        outputPanel.add(variableSelectionDescription, constraints);

        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx++;
        constraints.weightx = 1.0;
        outputPanel.add(m_flowVariable, constraints);

        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.gridwidth = 2;
        constraints.gridy++;
        constraints.weighty = 1.0;

        outputPanel.add(Box.createGlue(), constraints);

        return outputPanel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_sharedPanel.saveSettingsTo(settings);

        try {
            m_config.setFlowVariableName(Optional.ofNullable((FlowVariable)m_flowVariable.getSelectedItem())
                .map(FlowVariable::getName).orElse(null));
        } catch (final IllegalArgumentException e) {
            throw new InvalidSettingsException("Please correct the following error: " + e.getMessage(), e);
        }
        m_config.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        m_sharedPanel.loadSettingsFrom(settings);

        try {
            NodeConfiguration.validateSettings(settings);
            m_config.loadValidatedSettingsFrom(settings);
        } catch (final InvalidSettingsException e) { // NOSONAR: the specification demands that we use load the default values in this case
            m_config.reset();
        }

        final var cbModel = (DefaultComboBoxModel<FlowVariable>)m_flowVariable.getModel();
        final var flowVariables = getAvailableFlowVariables(ContainerFileOutputNodeModel.SUPPORTED_VAR_TYPES);

        cbModel.removeAllElements();
        cbModel.addAll(flowVariables.values());

        m_config.getFlowVariableName().ifPresent(name -> cbModel.setSelectedItem(flowVariables.get(name)));
    }
}
