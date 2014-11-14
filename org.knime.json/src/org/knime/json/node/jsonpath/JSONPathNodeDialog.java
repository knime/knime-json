/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
package org.knime.json.node.jsonpath;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.util.EnumSet;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.json.node.util.GUIFactory;
import org.knime.json.node.util.OutputType;
import org.knime.json.node.util.PathOrPointerDialog;

import com.jayway.jsonpath.JsonPath;

/**
 * <code>NodeDialog</code> for the "JSONPath" Node. Selects certain paths from the selected JSON column.
 *
 * @author Gabor Bakos
 */
public class JSONPathNodeDialog extends PathOrPointerDialog<JSONPathSettings> {
    private JTextField m_path;

    private JCheckBox m_resultIsList;

    private JCheckBox m_returnPaths;

    private JLabel m_nonDefiniteWarning, m_syntaxError;

    /**
     * New pane for configuring the JSONPath node.
     */
    protected JSONPathNodeDialog() {
        super(JSONPathNodeModel.createJSONPathProjectionSettings());
        updateEnabled();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void afterNewColumnName(final JPanel panel, final int afterInput) {
        m_nonDefiniteWarning = new JLabel("Path is non-definite");
        m_syntaxError = new JLabel();
        panel.setPreferredSize(new Dimension(800, 300));
        GridBagConstraints gbc = createInitialConstraints();
        gbc.gridy = afterInput;
        gbc.gridx = 0;
        panel.add(new JLabel("JSONPath:"), gbc);
        gbc.gridx = 1;
        m_path = GUIFactory.createTextField("", 22);
        m_path.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(final DocumentEvent e) {
                updateWarnings();
            }

            @Override
            public void removeUpdate(final DocumentEvent e) {
                updateWarnings();
            }

            @Override
            public void changedUpdate(final DocumentEvent e) {
                updateWarnings();
            }
            });
        panel.add(m_path, gbc);
        gbc.gridy++;
        panel.add(m_syntaxError, gbc);
        gbc.gridy++;
        gbc.gridy = addOutputTypePanel(panel, gbc.gridy);

        m_returnPaths = new JCheckBox("Return the paths instead of values");
        m_returnPaths.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                updateEnabled();
            }
        });
        panel.add(m_returnPaths, gbc);
        gbc.gridy++;

        gbc.gridx = 0;
        panel.add(m_nonDefiniteWarning, gbc);
        m_nonDefiniteWarning.setVisible(false);
        gbc.gridx = 1;
        m_resultIsList = new JCheckBox("Result is list");
        panel.add(m_resultIsList, gbc);
        gbc.gridy++;

        m_resultIsList.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                updateWarnings();
                updateEnabled();
            }
        });
    }

    private void updateEnabled() {
        if (m_returnPaths.isSelected() && !(OutputType.String == getOutputTypeModel().getSelectedItem())) {
            getOutputTypeModel().setSelectedItem(OutputType.String);
        }
        m_resultIsList.setEnabled(!m_returnPaths.isSelected());
        m_resultIsList.setSelected(m_returnPaths.isSelected() || m_resultIsList.isSelected());
        OutputType selectedOutputType = (OutputType)getOutputTypeModel().getSelectedItem();
        if (!m_resultIsList.isSelected()) {
            EnumSet<OutputType> supportedOutputTypes =
                EnumSet.allOf(OutputType.class);
            if (m_returnPaths.isSelected()) {
                supportedOutputTypes = EnumSet.of(OutputType.String);
            }
            getOutputTypeModel().removeAllElements();
            for (OutputType outputType : supportedOutputTypes) {
                getOutputTypeModel().addElement(outputType);
            }
            if (selectedOutputType != null) {
                getOutputTypeModel().setSelectedItem(selectedOutputType);
            }
        } else if (!m_returnPaths.isSelected()) {
            getOutputTypeModel().removeAllElements();
            for (OutputType type : OutputType.values()) {
                getOutputTypeModel().addElement(type);
            }
            getOutputTypeModel().setSelectedItem(selectedOutputType);
        }
    }

    private void updateWarnings() {
        String text = m_path.getText();
        m_nonDefiniteWarning.setVisible(false);
        m_syntaxError.setText("");
        try {
            if (!JsonPath.compile(text).isDefinite()) {
                m_nonDefiniteWarning.setVisible(!m_resultIsList.isSelected() && !m_returnPaths.isSelected());
            }
        } catch (RuntimeException e) {
            m_syntaxError.setText(e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        super.loadSettingsFrom(settings, specs);
        m_path.setText(getSettings().getJsonPath());
        m_resultIsList.setSelected(getSettings().isResultIsList());
        m_returnPaths.setSelected(getSettings().isReturnPaths());
        updateEnabled();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        try {
            JsonPath.compile(m_path.getText());
        } catch (RuntimeException e) {
            throw new InvalidSettingsException("Wrong path: " + m_path.getText() + "\n" + e.getMessage(), e);
        }
        getSettings().setJsonPath(m_path.getText());
        getSettings().setResultIsList(m_resultIsList.isSelected());
        getSettings().setReturnPaths(m_returnPaths.isSelected());
        super.saveSettingsTo(settings);
    }
}
