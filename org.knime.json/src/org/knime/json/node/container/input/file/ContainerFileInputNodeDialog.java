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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Arrays;
import java.util.stream.Stream;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.SharedIcons;
import org.knime.filehandling.core.data.location.variable.FSLocationVariableType;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.DialogComponentReaderFileChooser;

/**
 * The dialog for the “Container Input (File)” node.
 *
 * @author Jannik Löscher, KNIME GmbH, Konstanz, Germany
 */
final class ContainerFileInputNodeDialog extends NodeDialogPane {

    private final ContainerNodeSharedDialogPanel m_sharedPanel = new ContainerNodeSharedDialogPanel("input-file");

    private final NodeConfiguration m_config = new NodeConfiguration();

    private DialogComponentReaderFileChooser m_fileChooser;

    private JCheckBox m_useDefaultFileBox;

    private JTextField m_outVarNameField;

    /**
     * Constructs a new dialog for the “Container Input (File)” node
     */
    public ContainerFileInputNodeDialog() {

        final var settingsPanel = new JPanel(new GridBagLayout());
        final var constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1.0;
        constraints.gridx = 0;

        settingsPanel.add(setupSharedPanel(), constraints);
        settingsPanel.add(setupOuputPanel(), constraints);
        settingsPanel.add(setupDefaultFilePanel(), constraints);

        addTab("Container Input (File)", settingsPanel);
    }

    private JPanel setupSharedPanel() {
        return m_sharedPanel;
    }

    private JPanel setupOuputPanel() {
        final var outputPanel = new JPanel(new GridBagLayout());
        outputPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Output"));

        final var outVarNameDesc = new JLabel("Variable name:");
        m_outVarNameField = new JTextField(m_config.getOutputVariableName());
        final var hint = new JLabel(" ");
        m_outVarNameField.getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void removeUpdate(final DocumentEvent e) {
                checkOutputVarName(m_outVarNameField, hint);
            }

            @Override
            public void insertUpdate(final DocumentEvent e) {
                checkOutputVarName(m_outVarNameField, hint);
            }

            @Override
            public void changedUpdate(final DocumentEvent e) {
                checkOutputVarName(m_outVarNameField, hint);
            }
        });

        // setup layout
        final var constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 0;
        constraints.insets = new Insets(5, 5, 5, 5);

        outputPanel.add(outVarNameDesc, constraints);

        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1;
        constraints.gridx++;
        constraints.insets = new Insets(5, 5, 0, 5);
        outputPanel.add(m_outVarNameField, constraints);
        constraints.gridy++;
        constraints.insets = new Insets(0, 5, 5, 5);
        outputPanel.add(hint, constraints);

        return outputPanel;
    }

    private static void checkOutputVarName(final JTextField text, final JLabel hint) {
        if (text.getText().isBlank()) {
            hint.setText("Please choose a non-empty name");
            hint.setIcon(SharedIcons.ERROR.get());
        } else {
            hint.setText(" ");
            hint.setIcon(null);

        }
    }

    private JPanel setupDefaultFilePanel() {
        // setup file chooser
        final String[] keyChain =
            Stream
                .concat(Stream.of(NodeConfiguration.CFG_DEFAULT_FILE_KEY),
                    Arrays.stream(m_config.getFileChooserSettingsModel().getKeysForFSLocation()))
                .toArray(String[]::new);
        final var fvm = createFlowVariableModel(keyChain, FSLocationVariableType.INSTANCE);
        m_fileChooser =
            new DialogComponentReaderFileChooser(m_config.getFileChooserSettingsModel(), "container_input_file", fvm);

        final var filePanelWrapper = new JPanel(new GridBagLayout());
        m_useDefaultFileBox = new JCheckBox("Use a default file", m_config.isUsingDefaultFile());
        m_useDefaultFileBox.setToolTipText("Use a default file if an external file is not available.");
        m_useDefaultFileBox.addActionListener(
            l -> m_config.getFileChooserSettingsModel().setEnabled(m_useDefaultFileBox.isSelected()));
        m_config.getFileChooserSettingsModel().setEnabled(m_config.isUsingDefaultFile());
        final var constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.NORTHWEST;

        filePanelWrapper.add(m_useDefaultFileBox, constraints);
        constraints.gridx++;
        filePanelWrapper.add(m_fileChooser.getComponentPanel(), constraints);

        filePanelWrapper
            .setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Default File"));
        return filePanelWrapper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_sharedPanel.saveSettingsTo(settings);
        m_fileChooser.saveSettingsTo(settings);

        try {
            m_config.setUseDefaultFile(m_useDefaultFileBox.isSelected());
            m_config.setOutputVarianleName(m_outVarNameField.getText().strip());
        } catch (final IllegalArgumentException e) {
            throw new InvalidSettingsException("Please correct the following error: " + e.getMessage(), e);
        }
        m_config.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        m_sharedPanel.loadSettingsFrom(settings);
        m_fileChooser.loadSettingsFrom(settings, specs);

        try {
            m_config.validateSettings(settings);
            m_config.loadValidatedSettingsFrom(settings);
        } catch (final InvalidSettingsException e) { // NOSONAR: the specification demands that we use load the default values in this case
            m_config.reset();
        }
        m_useDefaultFileBox.setSelected(m_config.isUsingDefaultFile());
        m_outVarNameField.setText(m_config.getOutputVariableName());
    }
}
