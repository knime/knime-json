/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright by KNIME.com, Zurich, Switzerland
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.com
 * email: contact@knime.com
 * ---------------------------------------------------------------------
 *
 * History
 *   Created on Feb 15, 2015 by wiswedel
 */
package org.knime.json.node.output;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.json.JSONValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.dialog.DialogNode;
import org.knime.core.node.util.ColumnSelectionPanel;

/**
 * This is the dialog for the JSON output node.
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
final class JSONOutputNodeDialog extends NodeDialogPane {
    private final ColumnSelectionPanel m_columnSelectionPanel;
    private final JFormattedTextField m_parameterNameField;
    private final JCheckBox m_keepOneRowTablesSimpleChecker;

    @SuppressWarnings("unchecked")
    JSONOutputNodeDialog() {
        m_columnSelectionPanel = new ColumnSelectionPanel((Border)null, JSONValue.class);
        m_parameterNameField = new JFormattedTextField();
        m_parameterNameField.setInputVerifier(DialogNode.PARAMETER_NAME_VERIFIER);
        m_keepOneRowTablesSimpleChecker = new JCheckBox("Keep single-row tables simple");
        addTab("JSON Output", createLayout());
    }

    private JPanel createLayout() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.gridx = gbc.gridy = 0;
        panel.add(new JLabel("Parameter Name (JSON Key)"), gbc);
        gbc.gridy += 1;
        panel.add(new JLabel("JSON Column"), gbc);
        gbc.gridy = 0;
        gbc.gridx += 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(m_parameterNameField, gbc);
        gbc.gridy += 1;
        panel.add(m_columnSelectionPanel, gbc);
        gbc.gridy += 1;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        panel.add(m_keepOneRowTablesSimpleChecker, gbc);
        return panel;
    }

    /** {@inheritDoc} */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        JSONOutputConfiguration config = new JSONOutputConfiguration();
        config.setJsonColumnName(m_columnSelectionPanel.getSelectedColumn());
        config.setParameterName(m_parameterNameField.getText());
        config.setKeepOneRowTablesSimple(m_keepOneRowTablesSimpleChecker.isSelected());
        config.save(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs) throws NotConfigurableException {
        JSONOutputConfiguration config = new JSONOutputConfiguration().loadInDialog(settings, specs[0]);
        m_columnSelectionPanel.update(specs[0], config.getJsonColumnName(), false, true);
        m_parameterNameField.setText(config.getParameterName());
        m_keepOneRowTablesSimpleChecker.setSelected(config.isKeepOneRowTablesSimple());
    }
}
