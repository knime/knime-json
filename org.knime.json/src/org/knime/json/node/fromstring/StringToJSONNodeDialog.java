package org.knime.json.node.fromstring;

import java.awt.GridBagConstraints;

import javax.swing.JCheckBox;
import javax.swing.JPanel;

import org.knime.core.data.StringValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.json.node.util.ReplaceOrAddColumnDialog;

/**
 * <code>NodeDialog</code> for the "StringToJSON" Node. Converts String values to JSON values.
 *
 * @author Gabor Bakos
 */
final class StringToJSONNodeDialog extends ReplaceOrAddColumnDialog<StringToJSONSettings> {

    private JCheckBox m_allowComments;

    private JCheckBox m_failOnError;

    /**
     * New pane for configuring the StringToJSON node.
     */
    protected StringToJSONNodeDialog() {
        super(StringToJSONNodeModel.createStringToJSONSettings(), "String column:", StringValue.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int addAfterInputColumn(final JPanel panel, final int afterInput) {
        GridBagConstraints gbc = createInitialConstraints();
        gbc.gridy = afterInput;
        gbc.gridx = 1;
        m_allowComments = new JCheckBox("Allow comments", getSettings().isAllowComments());
        panel.add(m_allowComments, gbc);
        gbc.gridy++;
        m_failOnError = new JCheckBox("Fail on error", getSettings().isFailOnError());
        panel.add(m_failOnError, gbc);
        gbc.gridy++;
        return gbc.gridy;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        getSettings().setAllowComments(m_allowComments.isSelected());
        getSettings().setFailOnError(m_failOnError.isSelected());
        super.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        super.loadSettingsFrom(settings, specs);
        m_allowComments.setSelected(getSettings().isAllowComments());
        m_failOnError.setSelected(getSettings().isFailOnError());
    }
}
