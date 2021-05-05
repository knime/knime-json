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
 *   20.04.2021 (jl): created
 */
package org.knime.json.node.container.input.file;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.SharedIcons;

/**
 * Contains the dialog components that are shared by all Container nodes.
 *
 * @author Jannik Löscher, KNIME GmbH, Konstanz, Germany
 * @since 4.4
 */
public final class ContainerNodeSharedDialogPanel extends JPanel {
    private static final long serialVersionUID = 8005938399476092828L;

    private final JTextField m_parameterNameField;

    private final JLabel m_parameterNameHint;

    private final JCheckBox m_useFQParamNameChecker;

    private final JTextArea m_descriptionArea;

    private final ContainerNodeSharedConfiguration m_config;

    /**
     * New pane for configuring the Container node.
     */
    ContainerNodeSharedDialogPanel(final String defaultParamameter) {
        m_config = new ContainerNodeSharedConfiguration(defaultParamameter);

        m_parameterNameField = new JTextField();
        m_parameterNameHint = new JLabel(" ");
        m_parameterNameField.getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void removeUpdate(final DocumentEvent e) {
                checkParameterNameField();
            }

            @Override
            public void insertUpdate(final DocumentEvent e) {
                checkParameterNameField();
            }

            @Override
            public void changedUpdate(final DocumentEvent e) {
                checkParameterNameField();
            }
        });

        m_useFQParamNameChecker = new JCheckBox("Append unique ID to parameter name");
        m_useFQParamNameChecker.setToolTipText(
            "If checked, the name set above will be amended by the node's ID to guarantee unique parameter names.");

        m_descriptionArea = new JTextArea(1, 20);
        m_descriptionArea.setLineWrap(true);
        m_descriptionArea.setPreferredSize(new Dimension(100, 50));
        m_descriptionArea.setMinimumSize(new Dimension(100, 30));

        initLayout();
    }

    private void checkParameterNameField() {
        final var text = m_parameterNameField.getText().strip();
        if (text.isEmpty()) {
            m_parameterNameHint.setText("Please choose a non-empty name");
            m_parameterNameHint.setToolTipText(null);
            m_parameterNameHint.setIcon(SharedIcons.ERROR.get());
        } else if (!ContainerNodeSharedConfiguration.CFG_PARAMETER_VERIFIER.test(text)) {
            m_parameterNameHint.setText("Please choose a valid name (see tooltip)");
            m_parameterNameHint.setToolTipText(ContainerNodeSharedConfiguration.MSG_PARAMETER_FORMAT_DESC);
            m_parameterNameHint.setIcon(SharedIcons.ERROR.get());
        } else {
            m_parameterNameHint.setText(" ");
            m_parameterNameHint.setToolTipText(null);
            m_parameterNameHint.setIcon(null);
        }
    }

    private void initLayout() {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(5, 5, 5, 5);
        add(new JLabel("Parameter Name: "), gbc);
        gbc.gridx += 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(5, 5, 0, 5);
        add(m_parameterNameField, gbc);
        gbc.gridy++;
        gbc.insets = new Insets(0, 5, 5, 5);
        add(m_parameterNameHint, gbc);

        gbc.gridx = 1;
        gbc.gridy++;
        gbc.weightx = 0;
        gbc.insets = new Insets(5, 5, 5, 5);
        add(m_useFQParamNameChecker, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weightx = 0;
        add(new JLabel("Description: "), gbc);
        JScrollPane sp = new JScrollPane(m_descriptionArea);
        sp.setPreferredSize(m_descriptionArea.getPreferredSize());
        sp.setMinimumSize(m_descriptionArea.getMinimumSize());
        gbc.weightx = 1;
        gbc.gridx++;
        add(sp, gbc);
    }

    /**
     * Saves the settings of this dialog to the underlying {@link ContainerNodeSharedConfiguration}.
     *
     * @param settings the settings to save to.
     * @throws InvalidSettingsException if the settings are invalid
     */
    public void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        try {
            m_config.setParameter(m_parameterNameField.getText().strip());
            m_config.setFullyQualifiedName(m_useFQParamNameChecker.isSelected());
            m_config.setDescription(m_descriptionArea.getText());
        } catch (IllegalArgumentException e) {
            throw new InvalidSettingsException("Please correct the following error: " + e.getMessage(), e);
        }
        m_config.saveSettingsTo(settings);
    }

    /**
     * Loads from settings to this dialog's fields and the underlying {@link ContainerNodeSharedConfiguration}.
     *
     * @param settings the settings to load from to.
     */
    public void loadSettingsFrom(final NodeSettingsRO settings) {
        try {
            ContainerNodeSharedConfiguration.validateSettings(settings);
            m_config.loadValidatedSettingsFrom(settings);
        } catch (InvalidSettingsException e) { // NOSONAR: the method that calls this demands that errors are handled with default values instead
            m_config.reset();
        }
        m_parameterNameField.setText(m_config.getParameter());
        m_useFQParamNameChecker.setSelected(m_config.hasFullyQualifiedName());
        m_descriptionArea.setText(m_config.getDescription());
    }

}
