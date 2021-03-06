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
 *   14 Sept. 2014 (Gabor): created
 */
package org.knime.json.node.reader;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.knime.core.data.json.JSONValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.FilesHistoryPanel;
import org.knime.core.node.util.FilesHistoryPanel.LocationValidation;
import org.knime.core.node.workflow.FlowVariable.Type;
import org.knime.json.node.util.GUIFactory;

import com.jayway.jsonpath.JsonPath;

/**
 * <code>NodeDialog</code> for the "JSONReader" node. Reads {@code .json} files to {@link JSONValue}s.
 *
 * @author Gabor Bakos
 * @deprecated
 */
@Deprecated
final class JSONReaderNodeDialog extends NodeDialogPane {
    /**
     * The key for the history of previous locations.
     */
    static final String HISTORY_ID = "JSONReader";

    private JSONReaderSettings m_settings = JSONReaderNodeModel.createSettings();

    private final FilesHistoryPanel m_location;

    private JTextField m_columnName;

    private final JCheckBox m_selectPart;

    private final JTextField m_jsonPath;

    private final JCheckBox m_failIfNotFound;

    private JCheckBox m_allowComments;

    private final JLabel m_warningLabel;

    /**
     * New pane for configuring the JSONReader node.
     */
    protected JSONReaderNodeDialog() {
        m_location = new FilesHistoryPanel(createFlowVariableModel(JSONReaderSettings.LOCATION, Type.STRING),
            HISTORY_ID, LocationValidation.FileInput, "json|json.gz", "");
        m_columnName = GUIFactory.createTextField("", 22);
        m_columnName.getDocument().addDocumentListener(new DocumentListener() {
            private void reportError() {
                if (m_columnName.getText().trim().isEmpty()) {
                    error();
                } else {
                    noError();
                }
            }

            private void noError() {
                m_columnName.setBackground(Color.WHITE);
                m_columnName.setToolTipText(null);
            }

            private void error() {
                m_columnName.setBackground(Color.RED);
                m_columnName.setToolTipText("Empty column names are not allowed");
            }

            @Override
            public void insertUpdate(final DocumentEvent e) {
                reportError();
            }

            @Override
            public void removeUpdate(final DocumentEvent e) {
                reportError();
            }

            @Override
            public void changedUpdate(final DocumentEvent e) {
                reportError();
            }
        });
        m_selectPart = new JCheckBox("Select with JSONPath");
        m_jsonPath = GUIFactory.createTextField("", 22);
        m_jsonPath.setToolTipText("Hint: Use the annotations to explain it.");
        m_warningLabel = new JLabel();
        m_warningLabel.setForeground(Color.RED);
        final DocumentListener docListener = new DocumentListener() {
            @Override
            public void removeUpdate(final DocumentEvent e) {
                reportError();
            }

            @Override
            public void insertUpdate(final DocumentEvent e) {
                reportError();
            }

            @Override
            public void changedUpdate(final DocumentEvent e) {
                reportError();
            }

            private void reportError() {
                if (m_selectPart.isSelected()) {
                    try {
                        JsonPath.compile(m_jsonPath.getText());
                        noError();
                    } catch (RuntimeException e) {
                        error(e.getMessage());
                    }
                } else {
                    noError();
                }
            }

            private void error(final String message) {
                m_warningLabel.setText(message);
            }

            private void noError() {
                m_warningLabel.setText("");
            }
        };
        m_jsonPath.getDocument().addDocumentListener(docListener);
        m_failIfNotFound = new JCheckBox("Fail if path not found");
        m_failIfNotFound.setToolTipText("When unchecked and path do not match input, "
            + "missing value will be generated.");
        m_allowComments = new JCheckBox("Allow comments in JSON files");
        m_allowComments.setToolTipText("/*...*/, // or #");
        m_selectPart.getModel().addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent e) {
                boolean select = m_selectPart.isSelected();
                m_jsonPath.setEnabled(select);
                m_failIfNotFound.setEnabled(select);
                docListener.changedUpdate(null);
            }
        });
        m_selectPart.setSelected(true);
        m_selectPart.setSelected(false);



        addTab("Settings", initLayout());
    }

    private JPanel initLayout(){
        final JPanel filePanel = new JPanel();
        filePanel.setLayout(new BoxLayout(filePanel, BoxLayout.X_AXIS));
        filePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Input location:"));
        filePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, m_location.getPreferredSize().height));
        filePanel.add(m_location);

        JPanel optionsPanel = new JPanel(new GridBagLayout());
        optionsPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Reader options:"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.gridwidth = 1;

        optionsPanel.add(new JLabel("Output column name"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        optionsPanel.add(m_columnName, gbc);

        gbc.gridy++;

        gbc.gridx = 1;
        optionsPanel.add(m_selectPart, gbc);
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.weightx = 0;
        optionsPanel.add(new JLabel("JSONPath"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        optionsPanel.add(m_jsonPath, gbc);
        gbc.gridy++;
        optionsPanel.add(m_warningLabel, gbc);
        gbc.gridy++;

        gbc.weightx = 1;
        optionsPanel.add(m_failIfNotFound, gbc);
        gbc.gridy++;

        gbc.gridy++;
        optionsPanel.add(m_allowComments, gbc);

        //Filling remaining space
        gbc.gridy++;
        gbc.weighty = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.BOTH;
        optionsPanel.add(new JPanel(), gbc);

        final JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(filePanel);
        panel.add(Box.createVerticalStrut(5));
        panel.add(optionsPanel);
        return panel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        //We add the location to history using dummy save
        m_location.addToHistory();
        m_settings.setLocation(m_location.getSelectedFile());
        m_settings.setColumnName(m_columnName.getText());
        m_settings.setSelectPart(m_selectPart.isSelected());
        m_settings.setJsonPath(m_jsonPath.getText());
        m_settings.setFailIfNotFound(m_failIfNotFound.isSelected());
        m_settings.setAllowComments(m_allowComments.isSelected());
        m_settings.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        m_settings.loadSettingsForDialogs(settings, specs);
        m_location.setSelectedFile(m_settings.getLocation());
        m_selectPart.setSelected(m_settings.isSelectPart());
        m_jsonPath.setText(m_settings.getJsonPath());
        m_failIfNotFound.setSelected(m_settings.isFailIfNotFound());
        m_columnName.setText(m_settings.getColumnName());
        m_allowComments.setSelected(m_settings.isAllowComments());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCancel() {
        super.onCancel();
    }
}
