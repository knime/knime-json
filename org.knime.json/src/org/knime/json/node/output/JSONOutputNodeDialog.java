/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright by KNIME AG, Zurich, Switzerland
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

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;

import javax.json.JsonValue;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.Border;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.json.JSONValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.DataAwareNodeDialogPane;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.dialog.DialogNode;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.ColumnSelectionPanel;
import org.knime.json.util.JSONUtil;

/**
 * This is the dialog for the JSON output node.
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
final class JSONOutputNodeDialog extends DataAwareNodeDialogPane {
    private final ColumnSelectionPanel m_columnSelectionPanel;

    private final JFormattedTextField m_parameterNameField;

    private final JTextArea m_descriptionArea;

    private final JCheckBox m_keepOneRowTablesSimpleChecker;

    private final JButton m_fillFromInput = new JButton("Fill example JSON from input data");

    private final RSyntaxTextArea m_input;

    private JsonValue m_exampleJsonFromInput = null;

    @SuppressWarnings("unchecked")
    JSONOutputNodeDialog() {
        m_columnSelectionPanel = new ColumnSelectionPanel((Border)null, JSONValue.class);

        m_parameterNameField = new JFormattedTextField();
        m_parameterNameField.setInputVerifier(DialogNode.PARAMETER_NAME_VERIFIER);

        m_keepOneRowTablesSimpleChecker = new JCheckBox("Keep single-row tables simple");

        m_descriptionArea = new JTextArea(1, 20);
        m_descriptionArea.setLineWrap(true);
        m_descriptionArea.setPreferredSize(new Dimension(100, 50));
        m_descriptionArea.setMinimumSize(new Dimension(100, 30));

        m_input = new RSyntaxTextArea();
        m_input.setCodeFoldingEnabled(true);
        m_input.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);

        m_fillFromInput.setEnabled(false);
        m_fillFromInput.setToolTipText("This function requires input data to be present.");
        m_fillFromInput.addActionListener(e -> {
            m_input.setText(JSONUtil.toPrettyJSONString(m_exampleJsonFromInput));
        });

        addTab("JSON Output", createLayout());
    }

    private JPanel createLayout() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.gridx = gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        panel.add(new JLabel("Parameter Name (JSON Key)"), gbc);
        gbc.gridx += 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(m_parameterNameField, gbc);

        gbc.gridx = 0;
        gbc.gridy += 1;
        panel.add(new JLabel("JSON Column"), gbc);
        gbc.gridx++;
        panel.add(m_columnSelectionPanel, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weightx = 0;
        panel.add(new JLabel("Description: "), gbc);
        JScrollPane sp = new JScrollPane(m_descriptionArea);
        sp.setPreferredSize(m_descriptionArea.getPreferredSize());
        sp.setMinimumSize(m_descriptionArea.getMinimumSize());
        gbc.weightx = 1;
        gbc.gridx++;
        panel.add(sp, gbc);


        gbc.gridy += 1;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        panel.add(m_keepOneRowTablesSimpleChecker, gbc);

        gbc.gridx = 0;
        gbc.gridy += 1;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        panel.add(new JLabel("Example:"), gbc);

        gbc.gridx = 0;
        gbc.gridy += 1;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        panel.add(m_fillFromInput, gbc);

        gbc.gridx = 0;
        gbc.gridy += 1;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = gbc.weighty = 1.0;
        panel.add(new RTextScrollPane(m_input, true), gbc);

        return panel;
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        JSONOutputConfiguration config = new JSONOutputConfiguration();
        config.setJsonColumnName(m_columnSelectionPanel.getSelectedColumn());
        config.setParameterName(m_parameterNameField.getText(), false);
        config.setKeepOneRowTablesSimple(m_keepOneRowTablesSimpleChecker.isSelected());

        final String exampleJsonString = m_input.getText();
        if (!exampleJsonString.isEmpty()) {
            try {
                config.setExampleJson(JSONUtil.parseJSONValue(exampleJsonString));
            } catch (IOException e) {
                throw new InvalidSettingsException("Invalid JSON value.", e);
            }
        }

        config.setDescription(m_descriptionArea.getText());
        config.save(settings);
    }

    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs) throws NotConfigurableException {
        loadConfig(settings, specs[0]);
    }

    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs) throws NotConfigurableException {
        loadSettingsFrom(settings, new DataTableSpec[]{(DataTableSpec)specs[0]});
    }

    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final BufferedDataTable[] input)
        throws NotConfigurableException {
        final JSONOutputConfiguration config = loadConfig(settings, input[0].getSpec());

        m_exampleJsonFromInput =
            JSONOutputNodeModel.readIntoJsonValue(input[0], false, config.isKeepOneRowTablesSimple());
        m_fillFromInput.setEnabled(true);
        m_fillFromInput.setToolTipText("Set the example json from input data.");
        m_descriptionArea.setText(config.getDescription());
    }

    JSONOutputConfiguration loadConfig(final NodeSettingsRO settings, final DataTableSpec spec) throws NotConfigurableException {
        final JSONOutputConfiguration config = new JSONOutputConfiguration().loadInDialog(settings, spec);

        m_columnSelectionPanel.update(spec, config.getJsonColumnName(), false, true);
        m_parameterNameField.setText(config.getParameterName());
        m_keepOneRowTablesSimpleChecker.setSelected(config.isKeepOneRowTablesSimple());

        final JsonValue exampleJson = config.getExampleJson();
        final String exampleJsonString = (exampleJson == null) ? "" : JSONUtil.toPrettyJSONString(exampleJson);
        m_input.setText(exampleJsonString);
        m_descriptionArea.setText(config.getDescription());

        return config;
    }
}
