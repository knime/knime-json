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
 *   Mar 29, 2018 (Tobias Urhaug): created
 */
package org.knime.json.node.container.input.table;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;

import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.DataAwareNodeDialogPane;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NodeView;
import org.knime.core.node.dialog.DialogNode;
import org.knime.core.node.dialog.ValueControlledDialogPane;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.json.node.container.ui.ContainerTemplateTablePanel;

/**
 * Dialog for the Container Input (Table) node.
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 */
final class ContainerTableInputNodeDialog extends DataAwareNodeDialogPane implements ValueControlledDialogPane {

    private final JFormattedTextField m_parameterNameField;
    private final JCheckBox m_useFQParamNameChecker;
    private final JTextArea m_descriptionArea;
    private final ContainerTemplateTablePanel m_templateInputPanel;
    private final JLabel m_statusBarLabel;

    /**
     * New pane for configuring the Container Input (Table) node.
     */
    ContainerTableInputNodeDialog() {
        m_parameterNameField = new JFormattedTextField();
        m_parameterNameField.setInputVerifier(DialogNode.PARAMETER_NAME_VERIFIER);

        m_useFQParamNameChecker = new JCheckBox("Append unique ID to parameter name");
        m_useFQParamNameChecker.setToolTipText(
            "If checked, the name set above will be amended by the node's ID to guarantee unique parameter names.");

        m_descriptionArea = new JTextArea(1, 20);
        m_descriptionArea.setLineWrap(true);
        m_descriptionArea.setPreferredSize(new Dimension(100, 50));
        m_descriptionArea.setMinimumSize(new Dimension(100, 30));

        m_templateInputPanel = new ContainerTemplateTablePanel("Template table");

        m_statusBarLabel = new JLabel("", NodeView.WARNING_ICON, SwingConstants.LEFT);
        m_statusBarLabel.setVisible(false);

        addTab("Container Input (Table)", createLayout(), false);
    }

    private JPanel createLayout() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(5, 5, 5, 5);
        panel.add(new JLabel("Parameter Name: "), gbc);
        gbc.gridx += 1;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 0;
        panel.add(m_parameterNameField, gbc);

        gbc.gridx = 1;
        gbc.gridy++;
        gbc.weightx = 0;
        panel.add(m_useFQParamNameChecker, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weightx = 0;
        panel.add(new JLabel("Description: "), gbc);
        JScrollPane scrollPane = new JScrollPane(m_descriptionArea);
        scrollPane.setPreferredSize(m_descriptionArea.getPreferredSize());
        scrollPane.setMinimumSize(m_descriptionArea.getMinimumSize());
        gbc.gridx++;
        panel.add(scrollPane, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.gridwidth = 3;
        panel.add(m_templateInputPanel, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.gridwidth = 1;
        panel.add(m_statusBarLabel, gbc);

        return panel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        ContainerTableInputNodeConfiguration config = new ContainerTableInputNodeConfiguration();
        config.setParameterName(m_parameterNameField.getText());
        config.setUseFQNParamName(m_useFQParamNameChecker.isSelected());
        config.setDescription(m_descriptionArea.getText());
        config.setTemplateConfiguration(m_templateInputPanel);
        config.save(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs) {
        loadSettings(settings, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final BufferedDataTable[] input) {
        loadSettings(settings, input[0]);
    }

    private void loadSettings(final NodeSettingsRO settings, final BufferedDataTable inputTable) {
        ContainerTableInputNodeConfiguration config = new ContainerTableInputNodeConfiguration().loadInDialog(settings);
        m_parameterNameField.setText(config.getParameterName());
        m_useFQParamNameChecker.setSelected(config.isUseFQNParamName());
        m_descriptionArea.setText(config.getDescription());
        m_templateInputPanel.initialize(inputTable, config.getTemplateConfiguration());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadCurrentValue(final NodeSettingsRO value) throws InvalidSettingsException {
        String warningMessage = value.getString("infoMessage", null);
        if (warningMessage != null) {
            m_statusBarLabel.setText(warningMessage);
            m_statusBarLabel.setVisible(true);
        } else {
            m_statusBarLabel.setText("");
            m_statusBarLabel.setVisible(false);
        }
    }

}
