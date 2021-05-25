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
 *   17.05.2021 (jl): created
 */
package org.knime.json.node.container.input.variable2;

import java.awt.CardLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.knime.base.node.io.variablecreator.DialogComponentVariables;
import org.knime.base.node.io.variablecreator.SettingsModelVariables;
import org.knime.base.node.io.variablecreator.SettingsModelVariables.Type;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.json.node.container.input.file.ContainerNodeSharedDialogPanel;

/**
 * Node dialog for the Container Input (Variable) node.
 *
 * @author Jannik Löscher, KNIME GmbH, Konstanz, Germany
 * @since 4.4
 */
final class ContainerVariableInputNodeDialog2 extends NodeDialogPane {

    private final ContainerNodeSharedDialogPanel m_sharedPanel;

    private final DialogComponentVariables m_variableSelection;

    private final ContainerVariableInputNodeConfiguration2 m_config;

    private final JCheckBox m_simpleJsonSpec;

    private final JRadioButton m_acceptAnyInput;

    private final JRadioButton m_requireSpecification;

    private final JButton m_loadVariables;

    private final JPanel m_loadVariablesSettings;

    private final JRadioButton m_mergeVariables;

    private final JRadioButton m_setVariables;

    private DocumentListener m_parameterListener;

    /**
     * New pane for configuring the Container Input (Variable) node.
     */
    ContainerVariableInputNodeDialog2() {
        m_sharedPanel = new ContainerNodeSharedDialogPanel("variable-input");
        m_variableSelection = new DialogComponentVariables(new SettingsModelVariables(
            ContainerVariableInputNodeModel2.SETTINGS_MODEL_CONFIG_NAME,
            ContainerVariableInputNodeModel2.SUPPORTED_VARIABLE_TYPES, getAvailableFlowVariables(Type.getAllTypes())));
        m_config = new ContainerVariableInputNodeConfiguration2();
        m_simpleJsonSpec = new JCheckBox();
        m_acceptAnyInput = new JRadioButton();
        m_requireSpecification = new JRadioButton();
        m_mergeVariables = new JRadioButton();
        m_setVariables = new JRadioButton();
        m_loadVariables = new JButton();
        m_loadVariablesSettings = new JPanel();

        addTab("Container Input (Variable)", initDialogPanel());
    }

    private JPanel initDialogPanel() {
        final var settingsPanel = new JPanel(new GridBagLayout());
        final var constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(5, 5, 5, 5);
        constraints.weightx = 1.0;
        constraints.gridx = 0;
        settingsPanel.add(setupSharedPanel(), constraints);
        settingsPanel.add(setUpSepcificationUsagePanel(), constraints);

        constraints.fill = GridBagConstraints.BOTH;
        constraints.weightx = constraints.weighty = 1.0;
        settingsPanel.add(setupSpecificationPanel(), constraints);

        return settingsPanel;
    }

    private JPanel setupSharedPanel() {
        return m_sharedPanel;
    }

    private JPanel setUpSepcificationUsagePanel() {
        final var panel = new JPanel(new GridBagLayout());
        final var constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.NORTHWEST;

        final var group = new ButtonGroup();
        group.add(m_acceptAnyInput);
        group.add(m_requireSpecification);

        m_acceptAnyInput.setText("Accept any input");
        m_acceptAnyInput.addActionListener(e -> setSpecificationPanelEnabled(false));
        m_requireSpecification.setText("Require input to match template variables specification");
        m_requireSpecification.addActionListener(e -> setSpecificationPanelEnabled(true));
        if (m_config.isRequireMatchSpecification()) {
            m_requireSpecification.setSelected(true);
        } else {
            m_acceptAnyInput.setSelected(true);
        }

        panel.add(m_acceptAnyInput, constraints);
        panel.add(m_requireSpecification, constraints);

        return panel;

    }

    private JPanel setupSpecificationPanel() {
        final var specificationWrapper = new JPanel(new GridBagLayout());

        final var externalVariablesPresent = !getLoadableVariables().isEmpty();

        m_loadVariables.setText("Set input variables as template");
        m_loadVariables.addActionListener(e -> loadVariables());

        final var cardLayout = new CardLayout();
        m_loadVariablesSettings.setLayout(cardLayout);
        final var noVariables = new JPanel(new GridBagLayout());
        final var noVarConstraints = new GridBagConstraints();
        noVarConstraints.anchor = GridBagConstraints.NORTHWEST;
        noVarConstraints.fill = GridBagConstraints.HORIZONTAL;
        noVarConstraints.gridx = 0;
        noVarConstraints.weightx = 1.0;
        noVariables.add(new JLabel("No compatible input variables connected."), noVarConstraints);
        m_setVariables.setText("replace");
        m_mergeVariables.setText("merge");
        final var group = new ButtonGroup();
        group.add(m_setVariables);
        group.add(m_mergeVariables);

        final var loadSettingsPanel = new JPanel(new GridBagLayout());
        final var loadConstraints = new GridBagConstraints();
        loadConstraints.anchor = GridBagConstraints.NORTHWEST;
        loadConstraints.gridx = loadConstraints.gridy = 0;
        loadSettingsPanel.add(m_setVariables, loadConstraints);
        loadConstraints.fill = GridBagConstraints.HORIZONTAL;
        loadConstraints.weightx = 1.0;
        loadConstraints.gridx = 1;
        loadSettingsPanel.add(m_mergeVariables, loadConstraints);

        m_loadVariablesSettings.add(noVariables, "noVars");
        m_loadVariablesSettings.add(loadSettingsPanel, "loadSettings");

        final var buttonConstraints = new GridBagConstraints();
        buttonConstraints.anchor = GridBagConstraints.NORTHWEST;
        buttonConstraints.insets = new Insets(5, 5, 5, 5);
        buttonConstraints.gridx = loadConstraints.gridy = 0;
        specificationWrapper.add(m_loadVariables, buttonConstraints);
        buttonConstraints.fill = GridBagConstraints.HORIZONTAL;
        buttonConstraints.weightx = 1.0;
        buttonConstraints.gridx = 1;
        specificationWrapper.add(m_loadVariablesSettings, buttonConstraints);
        if (m_config.hasMergeVariables()) {
            m_mergeVariables.setSelected(true);
        } else {
            m_setVariables.setSelected(true);
        }

        m_loadVariables.setEnabled(externalVariablesPresent);
        cardLayout.show(m_loadVariablesSettings, externalVariablesPresent ? "loadSettings" : "noVars");

        m_simpleJsonSpec.setText("Use simplified JSON format");
        m_simpleJsonSpec.setSelected(m_config.hasSimpleJsonSpec());
        m_simpleJsonSpec.addActionListener(e -> {
            m_config.setUseSimpleJsonSpec(m_simpleJsonSpec.isSelected());
            updateSimpleJsonSpec();
        });

        m_variableSelection.addRowChangeListener(e -> updateSimpleJsonSpec());
        updateSimpleJsonSpec();

        final var constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(5, 5, 5, 5);
        constraints.weightx = 1.0;
        constraints.gridx = 0;
        constraints.gridwidth = 2;
        specificationWrapper.add(m_simpleJsonSpec, constraints);
        specificationWrapper.add(m_variableSelection.getComponentPanel(), constraints);

        constraints.fill = GridBagConstraints.BOTH;
        constraints.weightx = constraints.weighty = 1.0;
        specificationWrapper.add(Box.createGlue(), constraints);

        specificationWrapper
            .setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Template Variables"));
        return specificationWrapper;
    }

    private void updateSimpleJsonSpec() {
        var removeListener = false;

        if (m_variableSelection.getRowCount() == 1) {
            m_simpleJsonSpec.setEnabled(true);
            m_simpleJsonSpec.setSelected(m_config.hasSimpleJsonSpec());
            removeListener = !m_config.hasSimpleJsonSpec();
        } else {
            if (m_simpleJsonSpec.isEnabled()) {
                m_config.setUseSimpleJsonSpec(m_simpleJsonSpec.isSelected());
                m_simpleJsonSpec.setEnabled(false);
            }
            m_simpleJsonSpec.setSelected(false);
            removeListener = true;
        }

        if (removeListener && m_parameterListener != null) {
            // only remove the listener if it is there
            if (m_variableSelection.getRowCount() != 0) {
                m_variableSelection.removeNameBinding(0);
            }

            m_sharedPanel.getParameterTextField().getDocument().removeDocumentListener(m_parameterListener);
            m_parameterListener = null;
        } else if (!removeListener && m_parameterListener == null) {
            // only add the listener if it is not there
            final var nameSetter = m_variableSelection.setNameBinding(0);
            m_parameterListener = new DocumentListener() {
                @Override
                public void removeUpdate(final DocumentEvent e) {
                    nameSetter.accept(m_sharedPanel.getParameterTextField().getText());
                }

                @Override
                public void insertUpdate(final DocumentEvent e) {
                    nameSetter.accept(m_sharedPanel.getParameterTextField().getText());
                }

                @Override
                public void changedUpdate(final DocumentEvent e) {
                    nameSetter.accept(m_sharedPanel.getParameterTextField().getText());
                }
            };
            m_sharedPanel.getParameterTextField().getDocument().addDocumentListener(m_parameterListener);
            // do first update
            m_parameterListener.changedUpdate(null);
        }
    }

    private final void setSpecificationPanelEnabled(final boolean enabled) {
        if (enabled) {
            final var externalVariablesPresent = !getLoadableVariables().isEmpty();
            m_loadVariables.setEnabled(externalVariablesPresent);
            m_variableSelection.getModel().setEnabled(m_requireSpecification.isSelected());
            m_simpleJsonSpec.setEnabled(m_variableSelection.getRowCount() == 1);
        } else {
            m_loadVariables.setEnabled(false);
            m_variableSelection.getModel().setEnabled(false);
            m_simpleJsonSpec.setEnabled(false);
        }

        synchronized (m_loadVariables.getTreeLock()) {
            for (final var card : m_loadVariablesSettings.getComponents()) {
                card.setEnabled(enabled);
                synchronized (card.getTreeLock()) {
                    for (final var component : ((JPanel)card).getComponents()) {
                        component.setEnabled(enabled);
                    }
                }
            }
        }
    }

    private void loadVariables() {
        final var variables = getLoadableVariables();

        if (m_mergeVariables.isSelected()) {
            m_variableSelection.mergeVariables(variables);
        } else {
            m_variableSelection.setVariables(variables);
        }
    }

    private Map<String, FlowVariable> getLoadableVariables() {
        return getAvailableFlowVariables(
            Type.toVariableTypes(m_variableSelection.getVariableTable().getSupportedTypes())).entrySet().stream()
                // we do not want to load global variables like “knime.workflow” because adding that variable is most likely not
                // wanted and can lead to errors or unexpected behavior down the line
                .filter(e -> !e.getValue().isGlobalConstant())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_config.setRequireMatchSpecification(m_requireSpecification.isSelected());
        m_config.setMergeVariables(m_mergeVariables.isSelected());

        m_sharedPanel.saveSettingsTo(settings);
        m_variableSelection.saveSettingsTo(settings);
        m_config.save(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        m_sharedPanel.loadSettingsFrom(settings);
        m_variableSelection.loadSettingsFrom(settings, specs);
        m_variableSelection.getVariableTable().setExternalVariables(getAvailableFlowVariables(Type.getAllTypes()));
        m_config.loadInDialog(settings);

        final var externalVariablesPresent = !getLoadableVariables().isEmpty();
        m_loadVariables.setEnabled(externalVariablesPresent);
        ((CardLayout)m_loadVariablesSettings.getLayout()).show(m_loadVariablesSettings,
            externalVariablesPresent ? "loadSettings" : "noVars");

        updateSimpleJsonSpec();
        if (m_config.isRequireMatchSpecification()) {
            m_requireSpecification.setSelected(true);
            setSpecificationPanelEnabled(true);
        } else {
            m_acceptAnyInput.setSelected(true);
            setSpecificationPanelEnabled(false);
        }
        if (m_config.hasMergeVariables()) {
            m_mergeVariables.setSelected(true);
        } else {
            m_setVariables.setSelected(true);
        }
    }

}