package org.knime.json.node.toxml;

import java.awt.GridBagConstraints;

import javax.swing.JCheckBox;
import javax.swing.JPanel;

import org.knime.core.data.json.JSONValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.json.node.util.ReplaceOrAddColumnDialog;

/**
 * <code>NodeDialog</code> for the "JSONToXML" Node.
 *
 * @author Gabor Bakos
 */
public class JSONToXMLNodeDialog extends ReplaceOrAddColumnDialog<JSONToXMLSettings> {
    private JCheckBox m_arrays;

    /**
     * New pane for configuring the JSONToXML node.
     */
    protected JSONToXMLNodeDialog() {
        super(JSONToXMLNodeModel.createJSONToXMLSettings(), "JSON column", JSONValue.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int addAfterInputColumn(final JPanel panel, final int afterInput) {
        GridBagConstraints gbc = createInitialConstraints();
        gbc.gridx = 1;
        gbc.gridy = afterInput;
        m_arrays = new JCheckBox("Arrays in the result");
        m_arrays.setToolTipText("XMLs can only have a single root. If this option is checked, "
            + "the node will create list of XMLs regardless of the input, else it will fail for JSON arrays.");
        panel.add(m_arrays, gbc);
        gbc.gridy++;
        return gbc.gridy;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        getSettings().setCreateArrays(m_arrays.isSelected());
        super.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        super.loadSettingsFrom(settings, specs);
        m_arrays.setSelected(getSettings().isCreateArrays());
    }
}
