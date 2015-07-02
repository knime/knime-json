package org.knime.json.node.totable;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.TitledBorder;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.json.JSONValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnSelectionPanel;

/**
 * <code>NodeDialog</code> for the "JSONToTable" Node. Converts JSON values to new columns.
 *
 * @author Gabor Bakos
 */
class JSONToTableNodeDialog extends NodeDialogPane {
    @SuppressWarnings("unchecked")
    private final ColumnSelectionPanel m_inputColumn = new ColumnSelectionPanel("Input JSON column",
        JSONValue.class);

    private final JSONToTableSettings m_settings = new JSONToTableSettings();

    private final JRadioButton m_pathWithSeparator = new JRadioButton("Use path with separator: "),
            m_leafName = new JRadioButton("Use leaf name (uniquified with (#1)/(#2)/...))");

    private final JRadioButton m_jsonArray = new JRadioButton("Keep as JSON array"), m_collection = new JRadioButton(
        "Keep as collection elements"), m_expandToColumn = new JRadioButton("Expand to columns (may generate many)");

    private final JRadioButton m_onlyLeaves = new JRadioButton("Only leaves"), m_onlyUpTo = new JRadioButton(
        "Only up to level");
    private final JSpinner m_upToN = new JSpinner(new SpinnerNumberModel(1, 1, Integer.MAX_VALUE, 1));

    private final ButtonGroup m_columnNameGenerator = new ButtonGroup(), m_arrayHandling = new ButtonGroup(),
            m_expansion = new ButtonGroup();

    private final JTextField m_pathSeparator = new JTextField(".", 4);

    private final JCheckBox m_removeSourceChecker = new JCheckBox("Remove source column");

    private JCheckBox m_omitNestedObjects = new JCheckBox("Omit nested objects");

    /**
     * New pane for configuring the JSONToTable node.
     */
    protected JSONToTableNodeDialog() {
        m_columnNameGenerator.add(m_pathWithSeparator);
        m_columnNameGenerator.add(m_leafName);
        m_arrayHandling.add(m_jsonArray);
        m_arrayHandling.add(m_collection);
        m_arrayHandling.add(m_expandToColumn);
        m_expansion.add(m_onlyLeaves);
        m_expansion.add(m_onlyUpTo);
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.weightx = 1;
        panel.add(m_inputColumn, c);
        c.gridy++;
        panel.add(m_removeSourceChecker, c);
        c.gridy++;
        JPanel output = outputs();
        panel.add(output, c);
        JPanel arrayHandling = arrayHandling();
        c.gridy++;
        panel.add(arrayHandling, c);
        JPanel expansion = expansion();
        c.gridy++;
        panel.add(expansion, c);
        c.gridy++;
        c.fill = GridBagConstraints.BOTH;
        panel.add(new JPanel(), c);
        addTab("Settings", panel);
    }

    /**
     * @return
     */
    private JPanel expansion() {
        JPanel ret = new JPanel();
        ret.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.gridx = 0;
        gbc.weightx = 1;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        ret.add(m_onlyLeaves, gbc);
        gbc.weightx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        ret.add(m_onlyUpTo, gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        ret.add(m_upToN, gbc);
        m_upToN.setEnabled(m_onlyUpTo.isSelected());
        ActionListener expansionListener = new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                m_upToN.setEnabled(m_onlyUpTo.isSelected());
            }};
        m_onlyUpTo.addActionListener(expansionListener);
        m_onlyLeaves.addActionListener(expansionListener);
        m_omitNestedObjects.setSelected(JSONToTableSettings.DEFAULT_OMIT_NESTED_OBJECTS);
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        ret.add(m_omitNestedObjects, gbc);
        ret.setBorder(new TitledBorder("Children Expansion"));
        return ret;
    }

    /**
     * @return
     */
    private JPanel arrayHandling() {
        JPanel ret = new JPanel();
        ret.setLayout(new BoxLayout(ret, BoxLayout.PAGE_AXIS));
        ret.add(m_jsonArray);
        ret.add(m_collection);
        ret.add(m_expandToColumn);
        ret.setBorder(new TitledBorder("Arrays"));
        return ret;
    }

    /**
     * @return
     */
    private JPanel outputs() {
        JPanel output = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        c.gridx = 0;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.FIRST_LINE_START;
        output.add(m_pathWithSeparator, c);
        c.gridx = 1;
        output.add(m_pathSeparator, c);
        ActionListener pathListener = new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                m_pathSeparator.setEnabled(m_pathWithSeparator.isSelected());
            }
        };
        m_pathWithSeparator.addActionListener(pathListener);
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 2;
        output.add(m_leafName, c);
        m_leafName.addActionListener(pathListener);
        output.setBorder(new TitledBorder("Output column names"));
        return output;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_settings.setInputColumn(m_inputColumn.getSelectedColumn());
        m_settings.setArrayHandling(selectedArrayHandling());
        m_settings.setExpansion(selectedExpansion());
        m_settings.setUpToNLevel(((Number)m_upToN.getValue()).intValue());
        m_settings.setColumnNameStrategy(selectedColumnNameStrategy());
        m_settings.setSeparator(m_pathSeparator.getText());
        m_settings.setRemoveSourceColumn(m_removeSourceChecker.isSelected());
        m_settings.setOmitNestedObjects(m_omitNestedObjects.isSelected());
        m_settings.saveSettings(settings);
    }

    /**
     * @return
     */
    private ColumnNamePattern selectedColumnNameStrategy() {
        if (m_leafName.isSelected()) {
            return ColumnNamePattern.UniquifiedLeafNames;
        }
        assert m_pathWithSeparator.isSelected();
        return ColumnNamePattern.JsonPathWithCustomSeparator;
    }

    /**
     * @return
     */
    private Expansion selectedExpansion() {
        if (m_onlyUpTo.isSelected()) {
            return Expansion.OnlyUpTo;
        }
        assert m_onlyLeaves.isSelected();
        return Expansion.OnlyLeaves;
    }

    /**
     * @return
     */
    private ArrayHandling selectedArrayHandling() {
        if (m_jsonArray.isSelected()) {
            return ArrayHandling.KeepAllArrayAsJsonArray;
        }
        if (m_collection.isSelected()) {
            return ArrayHandling.GenerateCollectionCells;
        }
        assert m_expandToColumn.isSelected();
        return ArrayHandling.GenerateColumns;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
        throws NotConfigurableException {
        m_settings.loadSettingsDialog(settings, specs[0]);
        m_inputColumn.update(specs[0], m_settings.getInputColumn(), false);
        m_pathSeparator.setText(m_settings.getSeparator());
        switch (m_settings.getColumnNameStrategy()) {
            case JsonPathWithCustomSeparator:
                m_pathWithSeparator.setSelected(true);
                break;
            case UniquifiedLeafNames:
                m_leafName.setSelected(true);
                break;
            default:
                throw new UnsupportedOperationException("Unknown column name generating strategy: "
                    + m_settings.getColumnNameStrategy());
        }
        m_pathSeparator.setEnabled(m_pathWithSeparator.isSelected());
        switch (m_settings.getArrayHandling()) {
            case KeepAllArrayAsJsonArray:
                m_jsonArray.setSelected(true);
                break;
            case GenerateCollectionCells:
                m_collection.setSelected(true);
                break;
            case GenerateColumns:
                m_expandToColumn.setSelected(true);
                break;
            default:
                throw new UnsupportedOperationException("Unknown array handling: " + m_settings.getArrayHandling());
        }
        switch (m_settings.getExpansion()) {
            case OnlyUpTo:
                m_onlyUpTo.setSelected(true);
                m_onlyLeaves.setSelected(false);
                break;
            case OnlyLeaves:
                m_onlyLeaves.setSelected(true);
                m_onlyUpTo.setSelected(false);
                break;
            default:
                throw new UnsupportedOperationException("Unknown expansion strategy: " + m_settings.getExpansion());
        }
        m_upToN.setValue(m_settings.getUpToNLevel());
        m_upToN.setEnabled(m_onlyUpTo.isSelected());
        m_removeSourceChecker.setSelected(m_settings.isRemoveSourceColumn());
        m_omitNestedObjects.setSelected(m_settings.isOmitNestedObjects());
    }
}
