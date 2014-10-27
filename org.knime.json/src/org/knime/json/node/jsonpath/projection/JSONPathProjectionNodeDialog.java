package org.knime.json.node.jsonpath.projection;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.util.Vector;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.knime.core.data.json.JSONValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.json.node.util.GUIFactory;
import org.knime.json.node.util.ReplaceOrAddColumnDialog;

/**
 * <code>NodeDialog</code> for the "JSONPathProjection" Node. Selects certain paths from the selected JSON column.
 *
 * @author Gabor Bakos
 */
public class JSONPathProjectionNodeDialog extends ReplaceOrAddColumnDialog<JSONPathProjectionSettings> {

    private JComboBox<String> m_pathFormatOptions;

    private JTextField m_path;

    /**
     * New pane for configuring the JSONPathProjection node.
     */
    protected JSONPathProjectionNodeDialog() {
        super(JSONPathProjectionNodeModel.createJSONPathProjectionSettings(), "JSON column", JSONValue.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int addAfterInputColumn(final JPanel panel, final int afterInput) {
        panel.setPreferredSize(new Dimension(800, 300));
        GridBagConstraints gbc = createInitialConstraints();
        gbc.gridy = afterInput;
        gbc.gridx = 0;
        panel.add(new JLabel("Path format:"), gbc);
        gbc.gridx = 1;
        m_pathFormatOptions = new JComboBox<>(new Vector<>(JSONPathProjectionSettings.pathTypes()));
        panel.add(m_pathFormatOptions, gbc);
        gbc.gridy++;
        gbc.gridx = 0;
        panel.add(new JLabel("JSONPath:"), gbc);
        gbc.gridx = 1;
        m_path = GUIFactory.createTextField("", 44);
        panel.add(m_path, gbc);
        gbc.gridy++;
        return gbc.gridy;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        super.loadSettingsFrom(settings, specs);
        m_pathFormatOptions.setSelectedItem(getSettings().getPathType());
        m_path.setText(getSettings().getJsonPath());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        getSettings().setPathType(m_pathFormatOptions.getModel().getElementAt(m_pathFormatOptions.getSelectedIndex()));
        getSettings().setJsonPath(m_path.getText());
        super.saveSettingsTo(settings);
    }
}
