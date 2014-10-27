package org.knime.json.node.patch.apply;

import java.awt.GridBagConstraints;
import java.util.Vector;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.fife.rsyntaxarea.internal.RSyntaxAreaActivator;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.knime.core.data.json.JSONValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.json.node.util.ReplaceOrAddColumnDialog;

/**
 * <code>NodeDialog</code> for the "JSONTransformer" Node. Changes JSON values.
 *
 * @author Gabor Bakos
 */
public final class JSONPatchApplyNodeDialog extends ReplaceOrAddColumnDialog<JSONPatchApplySettings> {
    static {
        RSyntaxAreaActivator.ensureWorkaroundBug3692Applied();
    }

    private JComboBox<String> m_patchType;

    private RSyntaxTextArea m_patch;

    /**
     * New pane for configuring the JSONTransformer node.
     */
    protected JSONPatchApplyNodeDialog() {
        super(JSONPatchApplyNodeModel.createJSONPatchApplySetting(), "JSON column", JSONValue.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int addAfterInputColumn(final JPanel panel, final int afterInput) {
        GridBagConstraints gbc = createInitialConstraints();
        gbc.gridx = 0;
        gbc.gridy = afterInput;
        panel.add(new JLabel("Patch type:"), gbc);
        gbc.gridx = 1;
        m_patchType = new JComboBox<>(new Vector<>(JSONPatchApplySettings.PATCH_TYPES));
        panel.add(m_patchType, gbc);
        gbc.gridy++;

        gbc.gridx = 0;
        panel.add(new JLabel("Patch:"), gbc);
        gbc.gridx = 1;
        m_patch = new RSyntaxTextArea(22, 100);
        m_patch.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
        panel.add(new RTextScrollPane(m_patch), gbc);
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
        m_patchType.setSelectedItem(getSettings().getPatchType());
        m_patch.setText(getSettings().getJsonPatch());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        getSettings().setPatchType((String)m_patchType.getSelectedItem());
        getSettings().setJsonPatch(m_patch.getText());
        super.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean closeOnESC() {
        //@see org.knime.base.node.jsnippet.JavaSnippetNodeDialog.closeOnESC()
        return false;
    }
}
