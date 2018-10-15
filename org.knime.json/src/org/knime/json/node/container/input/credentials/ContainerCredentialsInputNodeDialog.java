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
 *   Aug 2, 2018 (Tobias Urhaug, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.json.node.container.input.credentials;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.dialog.DialogNode;

/**
 * Dialog for the Container Input (Credentials) node.
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 * @since 3.7
 */
public class ContainerCredentialsInputNodeDialog extends NodeDialogPane {

    private final JFormattedTextField m_parameterNameField;
    private final JTextArea m_descriptionArea;

    /**
     * New pane for configuring the Container Input (Table) node.
     */
    ContainerCredentialsInputNodeDialog() {
        m_parameterNameField = new JFormattedTextField();
        m_parameterNameField.setInputVerifier(DialogNode.PARAMETER_NAME_VERIFIER);

        m_descriptionArea = new JTextArea(1, 20);
        m_descriptionArea.setLineWrap(true);
        m_descriptionArea.setPreferredSize(new Dimension(100, 50));
        m_descriptionArea.setMinimumSize(new Dimension(100, 30));

        addTab("Container Input (Credentials)", createLayout(), false);
    }

    private JPanel createLayout() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(5, 5, 5, 5);
        p.add(new JLabel("Parameter Name: "), gbc);
        gbc.gridx += 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        p.add(m_parameterNameField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weightx = 0;
        p.add(new JLabel("Description: "), gbc);
        JScrollPane sp = new JScrollPane(m_descriptionArea);
        sp.setPreferredSize(m_descriptionArea.getPreferredSize());
        sp.setMinimumSize(m_descriptionArea.getMinimumSize());
        gbc.weightx = 1;
        gbc.gridx++;
        p.add(sp, gbc);

        return p;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        ContainerCredentialsInputNodeConfiguration config = new ContainerCredentialsInputNodeConfiguration();
        config.setParameterName(m_parameterNameField.getText());
        config.setDescription(m_descriptionArea.getText());
        config.save(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs) throws NotConfigurableException {
        ContainerCredentialsInputNodeConfiguration config = new ContainerCredentialsInputNodeConfiguration().loadInDialog(settings);
        m_parameterNameField.setText(config.getParameterName());
        m_descriptionArea.setText(config.getDescription());
    }
}
