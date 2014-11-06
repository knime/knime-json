package org.knime.json.node.jsonpointer;

import java.awt.Dimension;
import java.awt.GridBagConstraints;

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
 * <code>NodeDialog</code> for the "JSONPointer" Node. Selects certain pointers from the selected JSON column.
 *
 * @author Gabor Bakos
 */
public class JSONPointerNodeDialog extends ReplaceOrAddColumnDialog<JSONPointerSettings> {
    private JTextField m_path;

    /**
     * New pane for configuring the JSONPointer node.
     */
    protected JSONPointerNodeDialog() {
        super(JSONPointerNodeModel.createJSONPathProjectionSettings(), "JSON column", JSONValue.class);
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
        panel.add(new JLabel("JSONPointer:"), gbc);
        gbc.gridx = 1;
        m_path = GUIFactory.createTextField("", 33);
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
        m_path.setText(getSettings().getJsonPointer());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        getSettings().setJsonPath(m_path.getText());
        super.saveSettingsTo(settings);
    }
}
