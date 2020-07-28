package org.knime.json.node.container.output.row;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JCheckBox;
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
import org.knime.core.node.dialog.DialogNode;

/**
 * <code>NodeDialog</code> for the "ContainerRowOutput" Node.
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 */
final class ContainerRowOutputNodeDialog extends NodeDialogPane {

    private final JFormattedTextField m_parameterNameField;
    private final JCheckBox m_useFQParamNameChecker;
    private final JTextArea m_descriptionArea;

    /**
     * New pane for configuring the ContainerRowOutput node.
     */
    protected ContainerRowOutputNodeDialog() {
        m_parameterNameField = new JFormattedTextField();
        m_parameterNameField.setInputVerifier(DialogNode.PARAMETER_NAME_VERIFIER);

        m_useFQParamNameChecker = new JCheckBox("Append unique ID to parameter name");
        m_useFQParamNameChecker.setToolTipText(
            "If checked, the name set above will be amended by the node's ID to guarantee unique parameter names.");

        m_descriptionArea = new JTextArea(1, 20);
        m_descriptionArea.setLineWrap(true);
        m_descriptionArea.setPreferredSize(new Dimension(100, 50));
        m_descriptionArea.setMinimumSize(new Dimension(100, 30));

        addTab("Container Output (Row)", createLayout(), false);
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

        gbc.gridx = 1;
        gbc.gridy++;
        gbc.weightx = 0;
        p.add(m_useFQParamNameChecker, gbc);

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
        ContainerRowOutputNodeConfiguration config = new ContainerRowOutputNodeConfiguration();
        config.setParameterName(m_parameterNameField.getText());
        config.setUseFQNParamName(m_useFQParamNameChecker.isSelected());
        config.setDescription(m_descriptionArea.getText());
        config.save(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs) {
        ContainerRowOutputNodeConfiguration config =
                new ContainerRowOutputNodeConfiguration().loadInDialog(settings);
        m_parameterNameField.setText(config.getParameterName());
        m_useFQParamNameChecker.setSelected(config.isUseFQNParamName());
        m_descriptionArea.setText(config.getDescription());
    }

}

