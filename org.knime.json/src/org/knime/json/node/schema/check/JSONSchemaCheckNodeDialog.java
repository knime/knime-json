package org.knime.json.node.schema.check;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.fife.rsyntaxarea.internal.RSyntaxAreaActivator;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.json.JSONValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.ColumnSelectionComboxBox;

/**
 * <code>NodeDialog</code> for the "JSONSchemaCheck" node. Checks a JSON column's values against a Schema and fails if
 * it do not match.
 *
 * @author Gabor Bakos
 */
public final class JSONSchemaCheckNodeDialog extends NodeDialogPane {
    static {
        RSyntaxAreaActivator.ensureWorkaroundBug3692Applied();
    }

    @SuppressWarnings("unchecked")
    private ColumnSelectionComboxBox m_input = new ColumnSelectionComboxBox(JSONValue.class);

    private RSyntaxTextArea m_schema = new RSyntaxTextArea(20, 100);

    private JSONSchemaCheckSettings m_settings;

    /**
     * New pane for configuring the JSONSchemaCheck node.
     */
    protected JSONSchemaCheckNodeDialog() {
        m_input.setBorder(null);
        m_settings = JSONSchemaCheckNodeModel.createJSONSchemaSettings();
        m_schema.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
        JPanel panel = new JPanel(new GridBagLayout());
        addTab("Settings", panel);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.gridy = 0;
        gbc.gridx = 0;
        panel.add(new JLabel("JSON column:"), gbc);
        gbc.gridx = 1;
        panel.add(m_input);
        gbc.gridy++;

        gbc.gridx = 0;
        panel.add(new JLabel("Schema:"), gbc);
        gbc.gridx = 1;
        panel.add(new RTextScrollPane(m_schema), gbc);
        gbc.gridy++;
        gbc.weighty = 1;
        panel.add(new JPanel(), gbc);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_settings.setInputColumn(m_input.getSelectedColumn());
        m_settings.setInputSchema(m_schema.getText());
        m_settings.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        m_settings.loadSettingsForDialogs(settings, specs);
        m_input.setSelectedColumn(m_settings.getInputColumn());
        m_input.update((DataTableSpec)specs[0], m_settings.getInputColumn());
        m_schema.setText(m_settings.getInputSchema());
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
