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
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import org.knime.base.node.io.filereader.PreviewTableContentView;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.RowIterator;
import org.knime.core.data.container.DataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.tableview.TableView;
import org.knime.json.node.container.mappers.ContainerTableMapper;

/**
 * A view that displays a configured template table and holds an internal table that can be set as a
 * the new template by clicking a button.
 *
 * The view offers the possibility to use the entire internal table, or only parts of it, as the new template.
 * When the "Use only first rows" radio button is selected, the number spinner gets enabled and allows for a number of
 * rows to be set.
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 */
public final class ContainerTemplateTablePanel extends JPanel {

    private static final long serialVersionUID = 6012599572331463639L;

    private final TableView m_templateTableView;
    private final JButton m_createTemplateButton;
    private final JLabel m_warningLabel;

    private final JRadioButton m_useEntireTable;
    private final JRadioButton m_usePartsOfTable;
    private final JSpinner m_numberOfRows;
    private final JCheckBox m_omitTableSpec;


    private JsonValue m_inputTableJson;
    private JsonValue m_templateTableJson;

    /**
     * Constructs a new table example view.
     * @param borderTitle the title of the border
     */
    public ContainerTemplateTablePanel(final String borderTitle) {
        setLayout(new GridLayout());

        JPanel internalPanel = new JPanel(new GridBagLayout());
        internalPanel.setBorder(
            BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), borderTitle));
        GridBagConstraints gbc = new GridBagConstraints();
        m_createTemplateButton = new JButton("Set input table as template");
        m_createTemplateButton.addActionListener(e -> setInputTableAsTemplate());
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridx = gbc.gridy = 0;
        gbc.weightx = 0;
        gbc.weighty = 0;
        internalPanel.add(m_createTemplateButton, gbc);

        m_warningLabel = new JLabel();
        m_warningLabel.setForeground(Color.RED.darker());
        gbc.insets = new Insets(5,5,5,5);
        gbc.gridx++;
        internalPanel.add(m_warningLabel, gbc);

        m_useEntireTable = new JRadioButton("Use entire input table");
        m_usePartsOfTable = new JRadioButton("Use only first rows");
        m_usePartsOfTable.addActionListener(l -> m_createTemplateButton.setEnabled(true));

        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(m_useEntireTable);
        buttonGroup.add(m_usePartsOfTable);
        m_useEntireTable.setSelected(true);

        SpinnerNumberModel numberModel = new SpinnerNumberModel(10, 0, Integer.MAX_VALUE, 1);
        m_numberOfRows = new JSpinner(numberModel);
        m_useEntireTable.addActionListener(l -> m_numberOfRows.setEnabled(false));
        m_usePartsOfTable.addActionListener(l -> m_numberOfRows.setEnabled(true));
        m_numberOfRows.setEnabled(false);

        gbc.gridx = 0;
        gbc.gridy++;
        internalPanel.add(m_useEntireTable, gbc);

        gbc.gridy++;
        internalPanel.add(m_usePartsOfTable, gbc);

        gbc.gridx++;
        internalPanel.add(m_numberOfRows, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        m_omitTableSpec = new JCheckBox("Omit table spec in API definition");
        internalPanel.add(m_omitTableSpec, gbc);

        m_templateTableView = new TableView(new PreviewTableContentView());
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
        DataTable dataTable;
        if (m_usePartsOfTable.isSelected()) {
            dataTable = getTrimmedDataTable();
        } else {
            dataTable = getDataTable();
        }
        m_templateTableView.setDataTable(dataTable);
        m_templateTableJson = mapToJson(dataTable);
        setInputAndTemplateEqualState();
    }

    private DataTable getDataTable() {
        try {
            return ContainerTableMapper.toDataTable(m_inputTableJson)[0];
        } catch (InvalidSettingsException e) {
            throw new RuntimeException("Error while parsing table json to data table", e);
        }
    }

    private DataTable getTrimmedDataTable() {
        DataTable dataTable = getDataTable();
        Integer nRows = (Integer) m_numberOfRows.getValue();
        return getFirstRowsOfTable(nRows, dataTable);
    }

    private static DataTable getFirstRowsOfTable(final int nRows, final DataTable table) {
        DataContainer container = new DataContainer(table.getDataTableSpec());
        RowIterator iterator = table.iterator();
        for (int i = 0; i < nRows; i++) {
            if (iterator.hasNext()) {
                DataRow next = iterator.next();
                container.addRowToTable(next);
            }
        }
        container.close();
        return container.getTable();
    }

    /**
     * Initializes the internal state based on an input table and a configured template table.
     *
     * @param inputTable the input table
     * @param configuredTemplate the configured table
     * @param useEntireTable flag if the entire table should be used as a template
     * @param useNumberOfRows the number of rows used as template
     */
    public void initialize(
            final BufferedDataTable inputTable,
            final JsonValue configuredTemplate,
            final boolean useEntireTable,
            final int useNumberOfRows) {
        if (inputTable != null) {
            m_createTemplateButton.setEnabled(true);
            m_useEntireTable.setEnabled(true);
            m_usePartsOfTable.setEnabled(true);
            m_numberOfRows.setEnabled(!useEntireTable);
            m_omitTableSpec.setEnabled(true);
            m_inputTableJson = mapToJson(inputTable);
            setButtonEnabledStateBasedOnEquality(m_inputTableJson, configuredTemplate);
        } else {
            m_warningLabel.setForeground(Color.BLACK);
            m_warningLabel.setText("No input table connected.");
            m_createTemplateButton.setEnabled(false);
            m_useEntireTable.setEnabled(false);
            m_usePartsOfTable.setEnabled(false);
            m_numberOfRows.setEnabled(false);
            m_omitTableSpec.setEnabled(false);
        }

        m_templateTableView.setDataTable(mapToTable(configuredTemplate));
        m_templateTableJson = configuredTemplate;
        if (useEntireTable) {
            m_useEntireTable.setSelected(true);
        } else {
            m_usePartsOfTable.setSelected(true);
        }
        m_numberOfRows.setValue(useNumberOfRows);
    }

    private static JsonValue mapToJson(final DataTable dataTable) {
        try {
            return ContainerTableMapper.toContainerTableJsonValueFromDataTable(dataTable);
        } catch (InvalidSettingsException e) {
            throw new RuntimeException("Could not map input table to json", e);
        }
    }

    private void setButtonEnabledStateBasedOnEquality(final JsonValue inputTable, final JsonValue configuredTemplate) {
        JsonValue input = inputTable;
        if (m_usePartsOfTable.isSelected()) {
            DataTable trimmedDataTable = getTrimmedDataTable();
            input = mapToJson(trimmedDataTable);
        }

        if (input.equals(configuredTemplate)) {
            setInputAndTemplateEqualState();
        } else {
            m_warningLabel.setForeground(Color.RED.darker());
            m_warningLabel.setText("The input table is different from the configured template table, "
                    + "you might want to update the template");
            m_createTemplateButton.setEnabled(true);
        }
    }

    private void setInputAndTemplateEqualState() {
        String infoMessage = "The input table is equal to the configured template table.";
        if (m_usePartsOfTable.isSelected()) {
            Integer nRows = (Integer) m_numberOfRows.getValue();
            infoMessage = "The trimmed input table (first " + nRows + " rows) is equal to the configured template table";
        }
        m_warningLabel.setForeground(Color.BLACK);
        m_warningLabel.setText(infoMessage);
        if (m_useEntireTable.isSelected()) {
            m_createTemplateButton.setEnabled(false);
        }
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

    /**
     * Returns a flag telling if the entire table should be used as a template or not.
     *
     * @return a flag telling if the entire table should be used as a template or not
     */
    public boolean getUseEntireTable() {
        return m_useEntireTable.isSelected();
    }

    /**
     * Returns the number of rows that should be used of only parts of the table should be used as template.
     *
     * @return the number of rows to be used as template
     */
    public int getNumberOfRows() {
        return (Integer) m_numberOfRows.getValue();
    }
}
