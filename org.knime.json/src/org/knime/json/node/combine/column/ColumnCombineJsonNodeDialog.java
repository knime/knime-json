package org.knime.json.node.combine.column;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnSelectionComboxBox;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterConfiguration;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterPanel;
import org.knime.json.util.RootKeyType;
import org.knime.json.util.StringCellFilter;

/**
 * <code>NodeDialog</code> for the "ColumnCombineJson" Node.
 * Combines multiple JSON columns to a single.<br/>
 * Based on org.knime.xml.node.ccombine2.XMLColumnCombinerNodeDialog.
 *
 * @author Gabor Bakos
 */
class ColumnCombineJsonNodeDialog extends NodeDialogPane {
    private JTextField m_newColumn;
    private JRadioButton m_omitRootKey;
    private JRadioButton m_useDataBoundKey;
    private ColumnSelectionComboxBox m_dataBoundKeyColumn;
    private JRadioButton m_useCustomElementName;
    private JTextField m_rootKey;
    private DataColumnSpecFilterPanel m_filterPanel = new DataColumnSpecFilterPanel();
    private JCheckBox m_removeSourceColumns;

    /**
     * Creates a new dialog.
     */
    public ColumnCombineJsonNodeDialog() {
        super();

        JPanel settings = createSettingsPanel();
        addTab("Settings", settings/*, false*/);
        updateControls();
    }

    private JPanel createSettingsPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(2, 4, 2, 4);
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0;
        c.weighty = 0;

        c.insets = new Insets(8, 4, 6, 4);
        c.gridwidth = 1;
        p.add(new JLabel("New column name:"), c);
        c.gridx++;
        c.weightx = 1;
        m_newColumn = new JTextField();
        p.add(m_newColumn, c);
        c.weightx = 0;

        c.gridx = 0;
        c.gridwidth = 2;
        c.gridy++;
        c.weightx = 1;
        p.add(createRootPanel(), c);

        c.insets = new Insets(2, 4, 6, 4);
        c.gridx = 0;
        c.gridwidth = 2;
        c.gridy++;
        c.weightx = 1;
        p.add(m_filterPanel, c);
        c.weightx = 0;

        c.insets = new Insets(2, 4, 2, 4);
        c.gridx = 0;

        c.gridy++;
        c.weighty = 0;
        m_removeSourceColumns = new JCheckBox("Remove source columns.");
        p.add(m_removeSourceColumns, c);

        c.gridy++;
        c.weighty = 1;
        p.add(new JPanel(), c);
        return p;
    }

    private JPanel createRootPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(0, 0, 4, 0);
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.weightx = 0;
        c.weighty = 0;

        m_omitRootKey = new JRadioButton("Omit root key");
        final ActionListener radioButtonActionListener = new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
//                m_dataBoundKeyColumn.setEnabled(
//                        m_useDataBoundKey.isSelected());
//                m_rootKey.setEnabled(
//                        !m_useDataBoundKey.isSelected());
                updateControls();
            }
        };
        m_omitRootKey.addActionListener(radioButtonActionListener
//            new ActionListener() {
//            @Override
//            public void actionPerformed(final ActionEvent e) {
//                updateControls();
//            }
//        }
            );

        m_useDataBoundKey = new JRadioButton("Data bound key:");
        m_useDataBoundKey.addActionListener(radioButtonActionListener);
        m_useCustomElementName = new JRadioButton("Custom key:");
        m_useCustomElementName.addActionListener(radioButtonActionListener
//            new ActionListener() {
//            @Override
//            public void actionPerformed(final ActionEvent e) {
////                m_dataBoundKeyColumn.setEnabled(
////                        !m_useCustomElementName.isSelected());
////                m_rootKey.setEnabled(
////                        m_useCustomElementName.isSelected());
//                updateControls();
//            }
//        }
            );
        ButtonGroup group = new ButtonGroup();
        group.add(m_omitRootKey);
        group.add(m_useDataBoundKey);
        group.add(m_useCustomElementName);

        p.add(m_omitRootKey, c);
        c.gridy++;

        p.add(m_useCustomElementName, c);
        c.gridx++;
        c.weightx = 1;
        m_rootKey = new JTextField();
        m_rootKey.setEnabled(false);
        p.add(m_rootKey, c);
        c.gridx = 0;
        c.weightx = 0;

        c.gridy++;
        p.add(m_useDataBoundKey, c);
        c.gridx++;
        c.weightx = 1;
        m_dataBoundKeyColumn = new ColumnSelectionComboxBox(null, new StringCellFilter());
        m_dataBoundKeyColumn.setBorder(null);
        m_dataBoundKeyColumn.setEnabled(false);
        p.add(m_dataBoundKeyColumn, c);

        return p;
    }

    /**
     *
     */
    protected void updateControls() {
        if (m_omitRootKey.isSelected()) {
            m_dataBoundKeyColumn.setEnabled(false);
            m_rootKey.setEnabled(false);
        } else if (m_useDataBoundKey.isSelected()) {
            m_dataBoundKeyColumn.setEnabled(true);
            m_rootKey.setEnabled(false);
        } else if (m_useCustomElementName.isSelected()) {
            m_dataBoundKeyColumn.setEnabled(false);
            m_rootKey.setEnabled(true);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        ColumnCombineJsonSettings s = new ColumnCombineJsonSettings();

        s.setNewColumn(m_newColumn.getText().trim());
        RootKeyType rootKeyType;
        if (m_omitRootKey.isSelected()) {
            rootKeyType = RootKeyType.Unnamed;
        } else if (m_useDataBoundKey.isSelected()) {
            rootKeyType = RootKeyType.DataBound;
        } else if (m_useCustomElementName.isSelected()) {
            rootKeyType = RootKeyType.Constant;
        } else {
            throw new IllegalStateException("No selection of root key type");
        }
        s.setRootKeyType(rootKeyType);
//        s.setUseDataBoundKeyName(m_useDataBoundKey.isSelected());
        s.setRootKeyNameColumn(m_dataBoundKeyColumn.getSelectedColumn());
        s.setRootKeyName(m_rootKey.getText().trim());
        DataColumnSpecFilterConfiguration config = ColumnCombineJsonNodeModel.createDCSFilterConfiguration();
        m_filterPanel.saveConfiguration(config);
        s.setFilterConfiguration(config);
//        s.setDataBoundKeyNames(m_dataBoundValues.getKeys());
//        s.setDataBoundValues(m_dataBoundValues.getValues());
//        s.setKeyNames(m_keyValues.getKeys());
//        s.setValues(m_keyValues.getValues());
        s.setRemoveSourceColumns(m_removeSourceColumns.isSelected());
        s.saveSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        ColumnCombineJsonSettings s = new ColumnCombineJsonSettings();
        s.loadSettingsDialog(settings, specs[0]);

        m_newColumn.setText(s.getNewColumn());
        final RootKeyType rootKeyType = s.getRootKeyType();
        updateButtons(rootKeyType);
//        m_useDataBoundKey.setSelected(s.getUseDataBoundKeyName());
//        m_useCustomElementName.setSelected(!s.getUseDataBoundKeyName());
        try {
            m_dataBoundKeyColumn.update(specs[0], s.getRootKeyNameColumn());
        }catch (NotConfigurableException e) {
            if (m_useDataBoundKey.isSelected()) {
                m_omitRootKey.setSelected(true);
            }//else ignore
        }
//        m_dataBoundKeyColumn.setEnabled(s.getUseDataBoundKeyName());
        m_rootKey.setText(s.getRootKeyName());
//        m_rootKey.setEnabled(!s.getUseDataBoundKeyName());
        DataColumnSpecFilterConfiguration config = s.getFilterConfiguration();
        m_filterPanel.loadConfiguration(config, specs[0]);
//        m_dataBoundValues.setTableData(s.getDataBoundKeyNames(),
//                s.getDataBoundValues());

//        TableColumn valueColumn = m_dataBoundValues.getTable()
//            .getColumnModel().getColumn(1);
//        @SuppressWarnings("unchecked")
//        ColumnSelectionComboxBox valueEditor =
//            new ColumnSelectionComboxBox(JsonPathUtils.supportedInputDataValues().toArray(new Class[0]));
//        valueEditor.setBorder(null);
//        valueEditor.update(specs[0], null);
//        valueColumn.setCellEditor(
//                new ColumnSelectionCellEditor(valueEditor));
//        valueColumn.setCellRenderer(
//                new ColumnSelectionCellRenderer(specs[0]));

//        m_keyValues.setTableData(s.getKeyNames(),
//                s.getValues());
        m_removeSourceColumns.setSelected(s.getRemoveSourceColumns());
        updateControls();
    }

    /**
     * Updates the radiobuttons' selection based on {@code rootKeyType}.
     *
     * @param rootKeyType {@link RootKeyType}.
     */
    protected void updateButtons(final RootKeyType rootKeyType) {
        for (JRadioButton button : new JRadioButton[] {m_omitRootKey, m_useDataBoundKey, m_useCustomElementName}) {
            button.setSelected(false);
        }
        switch (rootKeyType) {
            case Unnamed:
                m_omitRootKey.setSelected(true);
                break;
            case Constant:
                m_useCustomElementName.setSelected(true);
                break;
            case DataBound:
                m_useDataBoundKey.setSelected(true);
                break;
            default:
                throw new UnsupportedOperationException("Not supported root type: " + rootKeyType);
        }
    }

//    @SuppressWarnings("serial")
//    private class  ColumnSelectionCellEditor extends DefaultCellEditor {
//        /**
//         * Constructs a <code>DefaultCellEditor</code> object that uses a
//         * combo box.
//         *
//         * @param comboBox  a <code>JComboBox</code> object
//         */
//        public ColumnSelectionCellEditor(
//                final ColumnSelectionComboxBox comboBox) {
//            super(comboBox);
//            editorComponent = comboBox;
//            comboBox.putClientProperty("JComboBox.isTableCellEditor",
//                    Boolean.TRUE);
//            delegate = new EditorDelegate() {
//                @Override
//                public void setValue(final Object value1) {
//                    // call twice to avoid a strange behavior where the
//                    // last editor call, maybe in another row, influences
//                    // the default value depicted by the combobox
//                    comboBox.setSelectedColumn((String)value1);
//                    comboBox.setSelectedColumn((String)value1);
//                }
//
//                @Override
//                public Object getCellEditorValue() {
//                    return comboBox.getSelectedColumn();
//                }
//
//                @Override
//                public boolean shouldSelectCell(final EventObject anEvent) {
//                    if (anEvent instanceof MouseEvent) {
//                        MouseEvent e = (MouseEvent)anEvent;
//                        return e.getID() != MouseEvent.MOUSE_DRAGGED;
//                    }
//                    return true;
//                }
//                @Override
//                public boolean stopCellEditing() {
//                    if (comboBox.isEditable()) {
//                        // Commit edited value.
//                        comboBox.actionPerformed(new ActionEvent(
//                                ColumnSelectionCellEditor.this, 0, ""));
//                    }
//                    return super.stopCellEditing();
//                }
//            };
//            comboBox.addActionListener(delegate);
//        }
//
//    }
//
//    @SuppressWarnings("serial")
//    private class ColumnSelectionCellRenderer extends DefaultTableCellRenderer {
//        private final DataTableSpec m_spec;
//        private final int m_defaultFontStyle;
//
//        /**
//         * Create a mew instance.
//         *
//         * @param spec the table spec of the input table
//         */
//        public ColumnSelectionCellRenderer(final DataTableSpec spec) {
//            m_spec = spec;
//            m_defaultFontStyle = getFont().getStyle();
//        }
//
//
//        /**
//         * {@inheritDoc}
//         */
//        @Override
//        public Component getTableCellRendererComponent(final JTable table,
//                final Object value, final boolean isSelected,
//                final boolean hasFocus, final int row,
//                final int column) {
//            setFont(getFont().deriveFont(Font.BOLD));
//            Component c = super.getTableCellRendererComponent(table, value,
//                    isSelected,
//                    hasFocus, row, column);
//
//            int col = m_spec.findColumnIndex((String)value);
//            if (col >= 0) {
//                DataColumnSpec colSpec = m_spec.getColumnSpec(col);
//                setIcon(colSpec.getType().getIcon());
//                setFont(getFont().deriveFont(Font.BOLD));
//            } else {
//                setIcon(null);
//                setFont(getFont().deriveFont(m_defaultFontStyle));
//            }
//            return c;
//        }
//    }
}

