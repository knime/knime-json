package org.knime.json.node.reader;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.knime.core.data.json.JSONValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentFileChooser;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.workflow.FlowVariable.Type;
import org.knime.json.node.util.GUIFactory;

/**
 * <code>NodeDialog</code> for the "JSONReader" node. Reads {@code .json} files to {@link JSONValue}s.
 *
 * @author Gabor Bakos
 */
final class JSONReaderNodeDialog extends NodeDialogPane {
    private JSONReaderSettings m_settings = JSONReaderNodeModel.createSettings();

    private DialogComponentFileChooser m_location;

    private JTextField m_columnName;

    private JCheckBox m_processOnlyJson;

    private JCheckBox m_allowComments;

    /**
     * New pane for configuring the JSONReader node.
     */
    protected JSONReaderNodeDialog() {
        JPanel panel = new JPanel(new GridBagLayout());
        addTab("Settings", panel);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 0;

        gbc.gridx = 0;
        gbc.gridy = 0;

        gbc.gridwidth = 2;
        m_location =
            new DialogComponentFileChooser(new SettingsModelString(JSONReaderSettings.LOCATION, ""), "JSONReader",
                JFileChooser.OPEN_DIALOG, false, createFlowVariableModel(JSONReaderSettings.LOCATION, Type.STRING),
                "json|json.gz|zip;");
        panel.add(m_location.getComponentPanel(), gbc);
        gbc.gridy++;
        gbc.gridwidth = 1;

        gbc.gridx = 0;
        panel.add(new JLabel("Output column name"), gbc);
        gbc.gridx = 1;
        m_columnName = GUIFactory.createTextField("", 22);
        panel.add(m_columnName, gbc);

        gbc.gridy++;

        gbc.gridx = 1;
        m_processOnlyJson = new JCheckBox("Process only files with .json extension");
        panel.add(m_processOnlyJson, gbc);
        m_processOnlyJson
            .setToolTipText("If checked, other files are skipped, if unchecked, error will reported for non-json zip entries.");

        gbc.gridy++;
        m_allowComments = new JCheckBox("Allow comments in JSON files");
        panel.add(m_allowComments, gbc);

        gbc.gridy++;
        gbc.weighty = 1;
        panel.add(new JPanel(), gbc);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_settings.setLocation(((SettingsModelString)m_location.getModel()).getStringValue());
        m_settings.setColumnName(m_columnName.getText());
        m_settings.setProcessOnlyJson(m_processOnlyJson.isSelected());
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
        ((SettingsModelString)m_location.getModel()).setStringValue(m_settings.getLocation());
        m_columnName.setText(m_settings.getColumnName());
        m_processOnlyJson.setSelected(m_settings.isProcessOnlyJson());
        m_allowComments.setSelected(m_settings.isAllowComments());
    }
}
