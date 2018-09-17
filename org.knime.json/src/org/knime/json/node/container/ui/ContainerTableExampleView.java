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
 *   Sep 12, 2018 (Tobias Urhaug, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.json.node.container.ui;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;

import javax.json.JsonValue;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.knime.base.node.io.filereader.PreviewTableContentView;
import org.knime.core.data.DataTable;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.tableview.TableView;
import org.knime.json.node.container.mappers.ContainerTableMapper;

/**
 * A view that displays an example table and holds an internal template table that can be set as a
 * new example by clicking a button.
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 */
public final class ContainerTableExampleView extends JPanel {

    private static final long serialVersionUID = 6012599572331463639L;

    private TableView m_templateTableView;
    private JButton m_createTemplateButton;
    private JLabel m_warningLabel;
    private BufferedDataTable m_inputTable;
    private JsonValue m_inputTableJson;
    private JsonValue m_templateTableJson;

    /**
     * Constructs a new table example view.
     * @param borderTitle the title of the border
     */
    public ContainerTableExampleView(final String borderTitle) {
        setLayout(new GridLayout());

        PreviewTableContentView ptcv = new PreviewTableContentView();
        m_templateTableView = new TableView(ptcv);

        JPanel internalPanel = new JPanel(new GridBagLayout());
        internalPanel.setBorder(
            BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), borderTitle));
        GridBagConstraints gbc = new GridBagConstraints();
        m_createTemplateButton = new JButton("Set input table as template");
        m_createTemplateButton.addActionListener(e -> setInputTableAsTemplate());
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.gridx = gbc.gridy = 0;
        gbc.weightx = 0;
        gbc.weighty = 0;
        internalPanel.add(m_createTemplateButton, gbc);

        m_warningLabel = new JLabel();
        m_warningLabel.setForeground(Color.RED.darker());
        gbc.ipadx = 100;
        gbc.insets = new Insets(5,5,5,5);
        gbc.weightx = 1;
        gbc.gridx++;
        internalPanel.add(m_warningLabel, gbc);

        gbc.insets = new Insets(5,1,1,1);
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        internalPanel.add(m_templateTableView, gbc);

        add(internalPanel);
    }

    private void setInputTableAsTemplate() {
        m_templateTableView.setDataTable(m_inputTable);
        m_templateTableJson = m_inputTableJson;
        setInputAndTemplateEqualState();
    }

    /**
     * Initializes the internal state based on an input table and a configured template table.
     *
     * @param inputTable the input table
     * @param configuredTemplate the configured table
     */
    public void initialize(final BufferedDataTable inputTable, final JsonValue configuredTemplate) {
        if (inputTable != null) {
            m_inputTable = inputTable;
            m_inputTableJson = mapToJson(inputTable);
            setButtonEnabledStateBasedOnEquality(m_inputTableJson, configuredTemplate);
        } else {
            m_warningLabel.setForeground(Color.BLACK);
            m_warningLabel.setText("No input table connected.");
            m_createTemplateButton.setEnabled(false);
        }

        m_templateTableView.setDataTable(mapToTable(configuredTemplate));
        m_templateTableJson = configuredTemplate;
    }

    private static JsonValue mapToJson(final BufferedDataTable table) {
        try {
            return ContainerTableMapper.toContainerTableJsonValue(table);
        } catch (InvalidSettingsException e) {
            throw new RuntimeException("Could not map input table to json", e);
        }
    }

    private void setButtonEnabledStateBasedOnEquality(final JsonValue inputTable, final JsonValue configuredTemplate) {
        if (inputTable.equals(configuredTemplate)) {
            setInputAndTemplateEqualState();
        } else {
            m_warningLabel.setForeground(Color.RED.darker());
            m_warningLabel.setText("The input table is different from the configured template table, "
                + "you might want to update the template");
            m_createTemplateButton.setEnabled(true);
        }
    }

    private void setInputAndTemplateEqualState() {
        m_warningLabel.setForeground(Color.BLACK);
        m_warningLabel.setText("The input table is equal to the configured template table.");
        m_createTemplateButton.setEnabled(false);
    }

    private static DataTable mapToTable(final JsonValue configuredTemplate) {
        try {
            DataTable[] configuredTable = ContainerTableMapper.toDataTable(configuredTemplate);
            return configuredTable[0];
        } catch (InvalidSettingsException e) {
            throw new RuntimeException("Could not map configured template to table", e);
        }
    }

    /**
     * Returns the json representation of the table set as a new template. Null if no new template has been set.
     *
     * @return a json representation of the new template table, null if no new template table has been set
     */
    public JsonValue getTemplateTableJson() {
        return m_templateTableJson;
    }

}
