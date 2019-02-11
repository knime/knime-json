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
package org.knime.json.node.container.input.row;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.json.JsonValue;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;

import org.knime.base.node.io.filereader.PreviewTableContentView;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.container.DataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.DataAwareNodeDialogPane;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NodeView;
import org.knime.core.node.dialog.DialogNode;
import org.knime.core.node.dialog.ValueControlledDialogPane;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.tableview.TableView;
import org.knime.json.node.container.mappers.ContainerTableMapper;

/**
 * Dialog for the Container Input (Row) node.
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 */
final class ContainerRowInputNodeDialog extends DataAwareNodeDialogPane implements ValueControlledDialogPane {

    private final JFormattedTextField m_parameterNameField;
    private final JTextArea m_descriptionArea;
    private final JButton m_createTemplateRowButton;
    private final JLabel m_warningLabel;
    private final JSpinner m_useRowNumber;
    private final JCheckBox m_useTemplateAsSpec;
    private final TableView m_templateTableView;
    private final JLabel m_statusBarLabel;

    private JsonValue m_inputTableJson;
    private JsonValue m_templateRowJson;

    /**
     * New pane for configuring the Container Input (Row) node.
     */
    ContainerRowInputNodeDialog() {
        m_parameterNameField = new JFormattedTextField();
        m_parameterNameField.setInputVerifier(DialogNode.PARAMETER_NAME_VERIFIER);

        m_descriptionArea = new JTextArea(1, 20);
        m_descriptionArea.setLineWrap(true);
        m_descriptionArea.setPreferredSize(new Dimension(100, 50));
        m_descriptionArea.setMinimumSize(new Dimension(100, 30));

        m_createTemplateRowButton = new JButton("Set row as template");
        m_createTemplateRowButton.addActionListener(e -> setRowAsTemplate());

        m_warningLabel = new JLabel();
        m_warningLabel.setForeground(Color.RED.darker());

        SpinnerNumberModel numberModel = new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 1);
        m_useRowNumber = new JSpinner(numberModel);

        m_useTemplateAsSpec = new JCheckBox("Use template row as input specification");

        m_templateTableView =  new TableView(new PreviewTableContentView());

        m_statusBarLabel = new JLabel("", NodeView.WARNING_ICON, SwingConstants.LEFT);
        m_statusBarLabel.setVisible(false);

        addTab("Container Input (Row)", createLayout(), false);
    }

    private JPanel createLayout() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(5, 5, 5, 5);

        gbc.gridx = gbc.gridy = 0;
        panel.add(new JLabel("Parameter Name: "), gbc);

        gbc.gridx++;
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

        JPanel templateRowPanel = createTemplateRowPanel();
        gbc.gridwidth = 2;
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weighty = 1;
        panel.add(templateRowPanel, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.gridwidth = 1;
        panel.add(m_statusBarLabel, gbc);

        return panel;
    }

    private JPanel createTemplateRowPanel() {
        JPanel templateRowPanel = new JPanel(new GridBagLayout());
        templateRowPanel.setBorder(
            BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Template row"));
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridx = gbc.gridy = 0;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.gridwidth = 1;
        templateRowPanel.add(m_createTemplateRowButton, gbc);

        gbc.gridx++;
        gbc.insets = new Insets(5, 12, 5, 5);
        templateRowPanel.add(m_warningLabel, gbc);

        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.insets = new Insets(5, 5, 5, 5);
        templateRowPanel.add(new JLabel("Use row number:"), gbc);

        gbc.gridx++;
        templateRowPanel.add(m_useRowNumber, gbc);

        gbc.gridwidth = 2;
        gbc.gridx = 0;
        gbc.gridy++;
        templateRowPanel.add(m_useTemplateAsSpec, gbc);

        gbc.insets = new Insets(5,1,1,1);
        gbc.gridy++;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        templateRowPanel.add(m_templateTableView, gbc);

        return templateRowPanel;
    }

    private void setRowAsTemplate() {
        DataTable templateRow = extractSelectedRowAsTable();
        m_templateTableView.setDataTable(templateRow);
        m_templateRowJson = mapToJson(templateRow);
    }

    private DataTable extractSelectedRowAsTable() {
        DataTable inputDataTable = getInputDataTable();
        DataContainer dataContainer = new DataContainer(inputDataTable.getDataTableSpec());
        int targetRowIndex = (int) m_useRowNumber.getValue();
        int i = 0;
        for (DataRow dataRow : inputDataTable) {
            if (i == targetRowIndex) {
                dataContainer.addRowToTable(dataRow);
                break;
            }
            i++;
        }
        dataContainer.close();
        return dataContainer.getTable();
    }

    private DataTable getInputDataTable() {
        try {
            return ContainerTableMapper.toDataTable(m_inputTableJson)[0];
        } catch (InvalidSettingsException e) {
            throw new RuntimeException("Error while parsing table json to data table", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        ContainerRowInputNodeConfiguration config = new ContainerRowInputNodeConfiguration();
        config.setParameterName(m_parameterNameField.getText());
        config.setDescription(m_descriptionArea.getText());
        config.setUseTemplateAsSpec(m_useTemplateAsSpec.isSelected());
        config.setTemplateRow(m_templateRowJson);
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
        ContainerRowInputNodeConfiguration config = new ContainerRowInputNodeConfiguration().loadInDialog(settings);
        m_parameterNameField.setText(config.getParameterName());
        m_descriptionArea.setText(config.getDescription());
        m_useTemplateAsSpec.setSelected(config.getUseTemplateAsSpec());

        JsonValue configuredTemplateRow = config.getTemplateRow();
        m_templateTableView.setDataTable(mapToTable(configuredTemplateRow));
        m_templateRowJson = configuredTemplateRow;

        if (inputTable == null) {
            setNoInputTableState();
        } else {
            m_inputTableJson = mapToJson(inputTable);
            setInputTablePresentState(inputTable);
        }
    }

    private void setNoInputTableState() {
        m_warningLabel.setForeground(Color.BLACK);
        m_warningLabel.setText("No input table connected.");
        m_createTemplateRowButton.setEnabled(false);
        m_useRowNumber.setEnabled(false);
    }

    private void setInputTablePresentState(final BufferedDataTable inputTable) {
        m_warningLabel.setText("");
        m_createTemplateRowButton.setEnabled(true);

        int size = (int) inputTable.size();
        SpinnerNumberModel numberModel = new SpinnerNumberModel(0, 0, size - 1, 1);
        m_useRowNumber.setModel(numberModel);
        m_useRowNumber.setPreferredSize(new Dimension(100, 20));
        m_useRowNumber.setEnabled(true);
    }

    private static DataTable mapToTable(final JsonValue configuredTemplateRow) {
        try {
            DataTable[] configuredTable = ContainerTableMapper.toDataTable(configuredTemplateRow);
            return configuredTable[0];
        } catch (InvalidSettingsException e) {
            throw new RuntimeException("Could not map configured template to table", e);
        }
    }

    private static JsonValue mapToJson(final DataTable dataTable) {
        try {
            return ContainerTableMapper.toContainerTableJsonValueFromDataTable(dataTable);
        } catch (InvalidSettingsException e) {
            throw new RuntimeException("Could not map input table to json", e);
        }
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
