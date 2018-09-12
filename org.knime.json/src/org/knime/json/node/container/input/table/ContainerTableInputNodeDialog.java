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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.json.JsonValue;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.knime.base.node.io.filereader.PreviewTableContentView;
import org.knime.core.data.DataTable;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.DataAwareNodeDialogPane;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.dialog.DialogNode;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.tableview.TableView;
import org.knime.json.node.container.mappers.ContainerTableMapper;

/**
 * Dialog for the Container Input (Table) node.
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 */
final class ContainerTableInputNodeDialog extends DataAwareNodeDialogPane {

    private final JFormattedTextField m_parameterNameField;
    private final JTextArea m_descriptionArea;
    private TableView m_exampleInputView;
    private JButton m_createExampleInputButton;
    private BufferedDataTable m_inputTable;
    private JsonValue m_exampleInputJson;

    /**
     * New pane for configuring the Container Input (Table) node.
     */
    ContainerTableInputNodeDialog() {
        m_parameterNameField = new JFormattedTextField();
        m_parameterNameField.setInputVerifier(DialogNode.PARAMETER_NAME_VERIFIER);

        m_descriptionArea = new JTextArea(1, 20);
        m_descriptionArea.setLineWrap(true);
        m_descriptionArea.setPreferredSize(new Dimension(100, 50));
        m_descriptionArea.setMinimumSize(new Dimension(100, 30));

        m_createExampleInputButton = new JButton("Create example input based on input table");
        m_createExampleInputButton.addActionListener(e -> createExampleInputFromInputTable());

        PreviewTableContentView ptcv = new PreviewTableContentView();
        m_exampleInputView = new TableView(ptcv);

        addTab("Container Input (Table)", createLayout(), false);
    }

    private void createExampleInputFromInputTable() {
        m_exampleInputView.setDataTable(m_inputTable);
        try {
            m_exampleInputJson = ContainerTableMapper.toContainerTableJsonValue(m_inputTable);
        } catch (InvalidSettingsException e) {
            throw new RuntimeException("Could not map input table to json", e);
        }
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

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weightx = 0;
        panel.add(new JLabel("Description: "), gbc);
        JScrollPane scrollPane = new JScrollPane(m_descriptionArea);
        scrollPane.setPreferredSize(m_descriptionArea.getPreferredSize());
        scrollPane.setMinimumSize(m_descriptionArea.getMinimumSize());
        gbc.gridx++;
        panel.add(scrollPane, gbc);

        JPanel exampleInputPanel = new JPanel(new BorderLayout());
        exampleInputPanel.setBorder(
            BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Example Input"));

        exampleInputPanel.add(m_createExampleInputButton, BorderLayout.NORTH);
        exampleInputPanel.add(m_exampleInputView, BorderLayout.CENTER);

        gbc.gridx = 0;
        gbc.weightx = 1;
        gbc.gridy++;
        gbc.gridwidth = 3;
        panel.add(exampleInputPanel, gbc);

        return panel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        ContainerTableInputNodeConfiguration config = new ContainerTableInputNodeConfiguration();
        config.setParameterName(m_parameterNameField.getText());
        config.setDescription(m_descriptionArea.getText());
        if (m_exampleInputJson != null) {
            config.setExampleInput(m_exampleInputJson);
        }
        config.save(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs) throws NotConfigurableException {
        loadSettings(settings, null);
        m_createExampleInputButton.setEnabled(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final BufferedDataTable[] input) {
        loadSettings(settings, input[0]);
        m_createExampleInputButton.setEnabled(true);
    }

    private void loadSettings(final NodeSettingsRO settings, final BufferedDataTable inputTable) {
        ContainerTableInputNodeConfiguration config = new ContainerTableInputNodeConfiguration().loadInDialog(settings);
        m_parameterNameField.setText(config.getParameterName());
        m_descriptionArea.setText(config.getDescription());
        m_inputTable = inputTable;
        DataTable[] exampleInputTable = getConfiguredExampleInput(config);
        m_exampleInputView.setDataTable(exampleInputTable[0]);
    }

    private static DataTable[] getConfiguredExampleInput(final ContainerTableInputNodeConfiguration config) {
        JsonValue exampleInput = config.getExampleInput();
        try {
            return ContainerTableMapper.toDataTable(exampleInput);
        } catch (InvalidSettingsException e) {
            throw new RuntimeException("Could not map the configrued example input to a table", e);
        }
    }

}
