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
 *   24 Sept. 2014 (Gabor): created
 */
package org.knime.json.node.schema.check;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.json.JSONValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.ColumnSelectionComboxBox;
import org.knime.json.node.util.GUIFactory;

/**
 * <code>NodeDialog</code> for the "JSONSchemaCheck" node. Checks a JSON column's values against a Schema and fails if
 * it do not match.
 *
 * @author Gabor Bakos
 */
public final class JSONSchemaCheckNodeDialog extends NodeDialogPane {
    @SuppressWarnings("unchecked")
    private ColumnSelectionComboxBox m_input = new ColumnSelectionComboxBox(JSONValue.class);

    private RSyntaxTextArea m_schema = new RSyntaxTextArea(20, 100);

    private JCheckBox m_failOnError = new JCheckBox("Fail on invalid JSON value");

    private JTextField m_errorMessageColumnName = GUIFactory.createTextField(JSONSchemaCheckSettings.DEFAULT_ERROR_MESSAGES_COLUMN, 22);

    private JSONSchemaCheckSettings m_settings;

    /**
     * New pane for configuring the JSONSchemaCheck node.
     */
    protected JSONSchemaCheckNodeDialog() {
        m_input.setBorder(null);
        m_settings = JSONSchemaCheckNodeModel.createJSONSchemaSettings();
        m_schema.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
        JPanel panel = new JPanel(new GridBagLayout());
        addTab("Settings", panel);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.gridy = 0;
        gbc.gridx = 0;
        panel.add(new JLabel("JSON column:"), gbc);
        gbc.gridx = 1;
        panel.add(m_input);
        gbc.gridy++;

        gbc.gridx = 0;
        panel.add(new JLabel("Schema:"), gbc);
        gbc.gridx = 1;
        panel.add(new RTextScrollPane(m_schema), gbc);
        gbc.gridy++;

        gbc.gridx = 0;
        gbc.gridwidth = 2;
        panel.add(m_failOnError, gbc);
        m_failOnError.setSelected(JSONSchemaCheckSettings.DEFAULT_FAIL_ON_INVALID_JSON);
        gbc.gridy++;
        gbc.gridwidth = 1;
        panel.add(new JLabel("Error message column:"), gbc);
        gbc.gridx = 1;
        panel.add(m_errorMessageColumnName, gbc);
        ChangeListener failOnErrorListener = new ChangeListener() {

            @Override
            public void stateChanged(final ChangeEvent e) {
                m_errorMessageColumnName.setEnabled(!m_failOnError.isSelected());
            }
        };
        m_failOnError.addChangeListener(failOnErrorListener);
        failOnErrorListener.stateChanged(null);
        gbc.gridy++;

        gbc.weighty = 1;
        panel.add(new JPanel(), gbc);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_settings.setInputColumn(m_input.getSelectedColumn());
        m_settings.setInputSchema(m_schema.getText());
        m_settings.setFailOnInvalidJson(m_failOnError.isSelected());
        m_settings.setErrorMessageColumn(m_errorMessageColumnName.getText());
        m_settings.checkSettings();
        m_settings.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        m_settings.loadSettingsForDialogs(settings, specs);
        m_input.setSelectedColumn(m_settings.getInputColumn());
        m_input.update((DataTableSpec)specs[0], m_settings.getInputColumn());
        m_schema.setText(m_settings.getInputSchema());
        m_failOnError.setSelected(m_settings.isFailOnInvalidJson());
        m_errorMessageColumnName.setText(m_settings.getErrorMessageColumn());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean closeOnESC() {
        //@see org.knime.base.node.jsnippet.JavaSnippetNodeDialog.closeOnESC()
        return false;
    }
}
