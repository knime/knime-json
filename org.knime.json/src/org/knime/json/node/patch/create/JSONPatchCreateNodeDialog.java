package org.knime.json.node.patch.create;

import java.awt.GridBagConstraints;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.json.JSONValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.ColumnSelectionComboxBox;
import org.knime.json.node.util.ReplaceOrAddColumnDialog;

/**
 * <code>NodeDialog</code> for the "JSONDiff" Node. Compares JSON values.
 *
 * @author Gabor Bakos
 */
public final class JSONPatchCreateNodeDialog extends ReplaceOrAddColumnDialog<JSONPatchCreateSettings> {

    private ColumnSelectionComboxBox m_targetColumn;

    /**
     * New pane for configuring the JSONDiff node.
     */
    protected JSONPatchCreateNodeDialog() {
        super(JSONPatchCreateNodeModel.createJSONPatchCreateSettings(), "Source (JSON) column", JSONValue.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int addAfterInputColumn(final JPanel panel, final int afterInput) {
        GridBagConstraints gbc = createInitialConstraints();
        gbc.gridy = afterInput;
        gbc.gridx = 0;
        panel.add(new JLabel("Target (JSON) column"), gbc);
        gbc.gridx = 1;
        @SuppressWarnings("unchecked")
        ColumnSelectionComboxBox columnSelectionComboxBox = new ColumnSelectionComboxBox(JSONValue.class);
        m_targetColumn = columnSelectionComboxBox;
        m_targetColumn.setBorder(null);
        panel.add(m_targetColumn, gbc);

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
        m_targetColumn.setSelectedColumn(getSettings().getInputColumnRight());
        m_targetColumn.update((DataTableSpec)specs[0], m_targetColumn.getSelectedColumn());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        getSettings().setInputColumnRight(m_targetColumn.getSelectedColumn());
        if (getSettings().getInputColumnRight().isEmpty()) {
            throw new InvalidSettingsException("No target column was selected");
        }
        super.saveSettingsTo(settings);
    }
}
