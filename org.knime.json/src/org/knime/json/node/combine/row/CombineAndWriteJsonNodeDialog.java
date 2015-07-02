package org.knime.json.node.combine.row;

import java.awt.GridBagConstraints;
import java.awt.Insets;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.util.FilesHistoryPanel;
import org.knime.core.node.util.FilesHistoryPanel.LocationValidation;
import org.knime.core.node.workflow.FlowVariable.Type;

/**
 * <code>NodeDialog</code> for the "CombineAndWriteJson" Node.
 * Combines the values from a JSON column to a single JSON file.
 *
 * @author Gabor Bakos
 */
class CombineAndWriteJsonNodeDialog extends RowCombineDialog<CombineAndWriteJsonSettings> {
    private JCheckBox m_overwrite, m_prettyPrint;
    private FilesHistoryPanel m_outputFile;

    /**
     * New pane for configuring the CombineAndWriteJson node.
     */
    protected CombineAndWriteJsonNodeDialog() {
        super();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void createOutput(final JPanel p, final GridBagConstraints c) {
        int origAnchor = c.anchor;
        Insets origInsets = c.insets, insets= (Insets)c.insets.clone();
        insets.top += 9;
        c.insets = insets;
        c.gridy++;
        c.anchor = GridBagConstraints.FIRST_LINE_START;
        c.gridx = 0;
        c.weightx = 0;
        final JLabel label = new JLabel("Output file");
        label.setVerticalAlignment(SwingConstants.TOP);
        p.add(label, c);
        c.weightx = 1;
        c.gridx++;
        c.insets = origInsets;
        m_outputFile = new FilesHistoryPanel(createFlowVariableModel(CombineAndWriteJsonSettings.OUTPUT_FILE, Type.STRING) ,"combine.and.write.json", LocationValidation.FileOutput);
        m_outputFile.setBorder(null);
        p.add(m_outputFile, c);
        c.anchor = origAnchor;

        c.weightx = 0;
        c.gridwidth = 2;
        c.gridx = 0;
        c.gridy++;
        m_overwrite = new JCheckBox("Overwrite existing file");
        p.add(m_overwrite, c);

        c.gridy++;
        m_prettyPrint = new JCheckBox("Pretty print");
        p.add(m_prettyPrint, c);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveOutput(final CombineAndWriteJsonSettings s) {
        s.setOverwrite(m_overwrite.isSelected());
        s.setOutputFile(m_outputFile.getSelectedFile());
        s.setPrettyPrint(m_prettyPrint.isSelected());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected CombineAndWriteJsonSettings initSettings() {
        return new CombineAndWriteJsonSettings();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadOutput(final CombineAndWriteJsonSettings s, final DataTableSpec dataTableSpec) {
        m_overwrite.setSelected(s.isOverwrite());
        m_outputFile.setSelectedFile(s.getOutputFile());
        m_prettyPrint.setSelected(s.isPrettyPrint());
    }
}
