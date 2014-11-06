package org.knime.json.node.fromxml;

import java.awt.GridBagConstraints;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.knime.core.data.xml.XMLValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.json.node.util.GUIFactory;
import org.knime.json.node.util.ReplaceOrAddColumnDialog;

/**
 * <code>NodeDialog</code> for the "XMLToJSON" Node. Converts XML values to JSON values.
 *
 * @author Gabor Bakos
 */
public class XMLToJSONNodeDialog extends ReplaceOrAddColumnDialog<XMLToJSONSettings> {

    private JTextField m_textKey;

    /**
     * New pane for configuring the XMLToJSON node.
     */
    protected XMLToJSONNodeDialog() {
        super(XMLToJSONNodeModel.createXMLToSJONSettings(), "XML column:", XMLValue.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int addAfterInputColumn(final JPanel panel, final int afterInput) {
        GridBagConstraints gbc = createInitialConstraints();
        gbc.gridx = 0;
        gbc.gridy = afterInput;
        panel.add(new JLabel("Text body translated to JSON with key: "), gbc);
        gbc.gridx = 1;
        m_textKey = GUIFactory.createTextField("", 22);
        panel.add(m_textKey, gbc);
        gbc.gridy++;
        return gbc.gridy;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        getSettings().setTextKey(m_textKey.getText());
        super.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        super.loadSettingsFrom(settings, specs);
        m_textKey.setText(getSettings().getTextKey());
    }
}
