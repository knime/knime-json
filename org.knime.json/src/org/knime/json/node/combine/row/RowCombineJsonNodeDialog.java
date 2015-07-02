package org.knime.json.node.combine.row;

import java.awt.GridBagConstraints;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.knime.core.data.DataTableSpec;
import org.knime.json.node.util.GUIFactory;

/**
 * <code>NodeDialog</code> for the "RowCombineJson" Node.
 * Appends JSON values in the rows to a single JSON value.
 * <br/>
 * Based on org.knime.xml.node.rcombine.XMLRowCombinerNodeDialog.
 *
 * @author Gabor Bakos
 */
class RowCombineJsonNodeDialog extends RowCombineDialog<RowCombineJsonSettings> {
    JTextField m_newColumn;
    /**
     * Creates a new dialog.
     */
    public RowCombineJsonNodeDialog() {
        super();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createOutput(final JPanel p, final GridBagConstraints c) {
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        p.add(new JLabel("New column name:"), c);
        c.gridx++;
        c.weightx = 1;
        m_newColumn = GUIFactory.createTextField("JSON", 22);
        p.add(m_newColumn, c);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadOutput(final RowCombineJsonSettings s, final DataTableSpec dataTableSpec) {
        m_newColumn.setText(s.getNewColumn());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveOutput(final RowCombineJsonSettings s) {
        s.setNewColumn(m_newColumn.getText());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected RowCombineJsonSettings initSettings() {
        return new RowCombineJsonSettings();
    }
}

