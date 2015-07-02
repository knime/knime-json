package org.knime.json.node.tojson;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.EventObject;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnSelectionComboxBox;
import org.knime.core.node.util.KeyValuePanel;
import org.knime.core.node.util.VerticalCollapsablePanels;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterConfiguration;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterPanel;
import org.knime.json.util.KeyValueTablePanelWithCopyFromRight;
import org.knime.json.util.RootKeyType;
import org.knime.json.util.StringCellFilter;

/**
 * <code>NodeDialog</code> for the "ColumnsToJson" Node. Converts contents of columns to JSON values rowwise.
 * <p/>
 * Based on org.knime.xml.node.columntoxml.ColumnToXMLNodeDialog
 *
 * @author Gabor Bakos
 */
public class ColumnsToJsonNodeDialog extends NodeDialogPane {
    private JTextField m_outputColumn;

    private JRadioButton m_useDataBoundKeyName;

    private JRadioButton m_useCustomKeyName;

    private JRadioButton m_omitRoot;

    private ColumnSelectionComboxBox m_keyNameColumn;

    private JTextField m_elementName;

//    private final SelectableAccordion m_dataBoundSelection = new SelectableAccordion(DataBoundValuesConfigType.CustomKeys.ordinal());

    private org.knime.json.util.KeyValuePanel m_dataBoundKeys;

    private DataColumnSpecFilterPanel m_dataBoundAutomatic;

    private KeyValuePanel m_keyValues;

    private JCheckBox m_removeSourceColumns;

    /**
     * Creates a new dialog for configuring the ColumnsToJson node.
     */
    public ColumnsToJsonNodeDialog() {
        super();
        JPanel settings = createSettingsPanel();
        settings.setPreferredSize(new Dimension(600, 500));
        addTab("Settings", settings);
        m_dataBoundAutomatic.setRemoveButtonText("<");
        m_dataBoundAutomatic.setRemoveAllButtonText("<<");
        m_dataBoundAutomatic.setAddButtonText(">");
        m_dataBoundAutomatic.setAddAllButtonText(">>");
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
        m_outputColumn = new JTextField();
        p.add(m_outputColumn, c);
        c.weightx = 0;

        c.insets = new Insets(2, 4, 2, 4);
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 2;
        JPanel elementName = createReturnTypePanel();
        elementName.setBorder(BorderFactory.createTitledBorder("Root key name"));
        p.add(elementName, c);

        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 2;
        c.weighty = 1;
        VerticalCollapsablePanels verticalPanel = new VerticalCollapsablePanels();
        m_dataBoundKeys = new org.knime.json.util.KeyValueTablePanelWithCopyFromRight();
//        m_dataBoundKeys.getTable().setPreferredScrollableViewportSize(null);
        m_dataBoundKeys.setBorder(BorderFactory.createTitledBorder("Key/data bound value pairs"));
        m_dataBoundKeys.setKeyColumnLabel("Key");
        m_dataBoundKeys.setValueColumnLabel("Value");
        m_dataBoundKeys.setPreferredSize(new Dimension(300, 200));
//        m_dataBoundSelection.addTab("Manual", m_dataBoundKeys);
        m_dataBoundAutomatic = new DataColumnSpecFilterPanel(true);
//        m_dataBoundAutomatic.setPreferredSize(new Dimension(300, 250));
//        m_dataBoundSelection.addTab("Automatic", m_dataBoundAutomatic);
//        p.add(m_dataBoundSelection, c);
        verticalPanel.addPanel(m_dataBoundKeys, true, "Manual Selection");
        verticalPanel.addPanel(m_dataBoundAutomatic, false, "Automatic Selection");

//        c.gridy++;
//        c.weighty = 1;
        m_keyValues = new KeyValuePanel();
        m_keyValues.setBorder(BorderFactory.createTitledBorder("Custom key/value pairs"));
        m_keyValues.setKeyColumnLabel("Key");
        m_keyValues.setValueColumnLabel("Value");
        m_keyValues.setPreferredSize(new Dimension(300, 200));
        verticalPanel.addPanel(m_keyValues, true, "Custom Key-Value Pairs");
        //verticalPanel.add(new Box.Filler(new Dimension(300, 0), new Dimension(300, 270), new Dimension(1000, 1000)), -1);
//        verticalPanel.add(Box.createVerticalGlue(), -1);
//        p.add(m_keyValues, c);
//        p.add(verticalPanel, c);
        p.add(new JScrollPane(verticalPanel), c);

        c.gridy++;
        c.weighty = 0;
        m_removeSourceColumns = new JCheckBox("Remove source columns.");
        p.add(m_removeSourceColumns, c);
        return p;
    }

    private JPanel createReturnTypePanel() {
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

        m_useDataBoundKeyName = new JRadioButton("Data bound key:");
        m_useDataBoundKeyName.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                m_keyNameColumn.setEnabled(m_useDataBoundKeyName.isSelected());
                m_elementName.setEnabled(!m_useDataBoundKeyName.isSelected());
                enableKeyValueSettings();
            }
        });
        m_useCustomKeyName = new JRadioButton("Custom key:");
        m_useCustomKeyName.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                m_keyNameColumn.setEnabled(!m_useCustomKeyName.isSelected());
                m_elementName.setEnabled(m_useCustomKeyName.isSelected());
                enableKeyValueSettings();
            }
        });

        m_omitRoot = new JRadioButton("Unnamed root element");
        m_omitRoot.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                if (m_omitRoot.isSelected()) {
                    m_keyNameColumn.setEnabled(false);
                    m_elementName.setEnabled(false);
                }
                enableKeyValueSettings();
            }
        });
        ButtonGroup group = new ButtonGroup();
        group.add(m_omitRoot);
        group.add(m_useDataBoundKeyName);
        group.add(m_useCustomKeyName);

        m_omitRoot.setSelected(true);

        p.add(m_omitRoot, c);

        c.gridy++;
        p.add(m_useCustomKeyName, c);
        c.gridx++;
        c.weightx = 1;
        m_elementName = new JTextField();
        p.add(m_elementName, c);
        c.gridx = 0;
        c.weightx = 0;

        c.gridy++;
        p.add(m_useDataBoundKeyName, c);
        c.gridx++;
        c.weightx = 1;
        m_keyNameColumn = new ColumnSelectionComboxBox(null, new StringCellFilter());
        //m_keyNameColumn.setBorder(null);
        p.add(m_keyNameColumn, c);

        return p;
    }

    /**
     * Enables/disables the {@link KeyValuePanel}s.
     */
    private void enableKeyValueSettings() {
//        boolean enable = !m_omitRoot.isSelected();
//        m_keyValues.setEnabled(enable);
//        m_dataBoundKeys.setEnabled(enable);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        ColumnsToJsonSettings s = new ColumnsToJsonSettings();

        s.setOutputColumnName(m_outputColumn.getText().trim());
        s.setRootKeyType(m_useDataBoundKeyName.isSelected() ? RootKeyType.DataBound : m_useCustomKeyName.isSelected()
            ? RootKeyType.Constant : RootKeyType.Unnamed);
        s.setKeyNameColumn(m_keyNameColumn.getSelectedColumn());
        s.setKeyName(m_elementName.getText().trim());
        s.setDataBoundKeyNames(m_dataBoundKeys.getKeys());
        s.setDataBoundKeyColumns(m_dataBoundKeys.getValues());
//        s.setConfigType(DataBoundValuesConfigType.values()[m_dataBoundSelection.getSelectedIndex()]);
        DataColumnSpecFilterConfiguration tmpConfiguration = ColumnsToJsonSettings.createConfiguration(ColumnsToJsonSettings.createFilter());
        m_dataBoundAutomatic.saveConfiguration(tmpConfiguration);
        s.setDataBoundColumnsAutoConfiguration(tmpConfiguration);
        s.setKeyNames(m_keyValues.getKeys());
        s.setKeyValues(m_keyValues.getValues());
        s.setRemoveSourceColumns(m_removeSourceColumns.isSelected());
        s.saveSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
        throws NotConfigurableException {
        ColumnsToJsonSettings s = new ColumnsToJsonSettings();
        s.loadSettingsDialog(settings, specs[0]);

        m_outputColumn.setText(s.getOutputColumnName());
        m_omitRoot.setSelected(s.getRootKeyType() == RootKeyType.Unnamed);
        m_useDataBoundKeyName.setSelected(s.getRootKeyType() == RootKeyType.DataBound);
        m_useCustomKeyName.setSelected(s.getRootKeyType() == RootKeyType.Constant);
        m_keyNameColumn.setEnabled(s.getRootKeyType() == RootKeyType.DataBound);
        try {
            m_keyNameColumn.update(specs[0], s.getKeyNameColumn());
        } catch (NotConfigurableException e) {
            if (s.getRootKeyType() == RootKeyType.DataBound) {
                m_omitRoot.setSelected(true);
            }//else ignore
        }
        m_elementName.setText(s.getKeyName());
        m_elementName.setEnabled(s.getRootKeyType() == RootKeyType.Constant);
        m_dataBoundKeys.setTableData(s.getDataBoundKeyNames(), s.getDataBoundKeyColumns());

        ((KeyValueTablePanelWithCopyFromRight)m_dataBoundKeys).setEditors();
        TableColumn valueColumn = m_dataBoundKeys.getTable().getColumnModel().getColumn(1);
        ColumnSelectionComboxBox valueEditor =
            new ColumnSelectionComboxBox(ColumnsToJsonSettings.supportedValueClasses());
        valueEditor.setBorder(null);
        valueEditor.update(specs[0], null);
        valueColumn.setCellEditor(new ColumnSelectionCellEditor(valueEditor));
        valueColumn.setCellRenderer(new ColumnSelectionCellRenderer(specs[0]));

        m_keyValues.setTableData(s.getKeyNames(), s.getKeyValues());

//        m_dataBoundSelection.setSelectedIndex(s.getConfigType().ordinal());
        m_dataBoundAutomatic.loadConfiguration(s.getDataBoundColumnsAutoConfiguration(), specs[0]);
        m_removeSourceColumns.setSelected(s.isRemoveSourceColumns());
    }

    @SuppressWarnings("serial")
    private class ColumnSelectionCellEditor extends DefaultCellEditor {
        /**
         * Constructs a <code>DefaultCellEditor</code> object that uses a combo box.
         *
         * @param comboBox a <code>JComboBox</code> object
         */
        public ColumnSelectionCellEditor(final ColumnSelectionComboxBox comboBox) {
            super(comboBox);
            editorComponent = comboBox;
            comboBox.putClientProperty("JComboBox.isTableCellEditor", Boolean.TRUE);
            delegate = new EditorDelegate() {
                @Override
                public void setValue(final Object value1) {
                    // call twice to avoid a strange behavior where the
                    // last editor call, maybe in another row, influences
                    // the default value depicted by the combobox
                    comboBox.setSelectedColumn((String)value1);
                    comboBox.setSelectedColumn((String)value1);
                }

                @Override
                public Object getCellEditorValue() {
                    return comboBox.getSelectedColumn();
                }

                @Override
                public boolean shouldSelectCell(final EventObject anEvent) {
                    if (anEvent instanceof MouseEvent) {
                        MouseEvent e = (MouseEvent)anEvent;
                        return e.getID() != MouseEvent.MOUSE_DRAGGED;
                    }
                    return true;
                }

                @Override
                public boolean stopCellEditing() {
                    if (comboBox.isEditable()) {
                        // Commit edited value.
                        comboBox.actionPerformed(new ActionEvent(ColumnSelectionCellEditor.this, 0, ""));
                    }
                    return super.stopCellEditing();
                }
            };
            comboBox.addActionListener(delegate);
        }

    }

    @SuppressWarnings("serial")
    private class ColumnSelectionCellRenderer extends DefaultTableCellRenderer {
        private final DataTableSpec m_spec;

        private final int m_defaultFontStyle;

        /**
         * Create a new instance.
         *
         * @param spec the spec of the input table
         */
        public ColumnSelectionCellRenderer(final DataTableSpec spec) {
            m_spec = spec;
            m_defaultFontStyle = getFont().getStyle();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Component getTableCellRendererComponent(final JTable table, final Object value,
            final boolean isSelected, final boolean hasFocus, final int row, final int column) {
            setFont(getFont().deriveFont(Font.BOLD));
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            int col = m_spec.findColumnIndex((String)value);
            if (col >= 0) {
                DataColumnSpec colSpec = m_spec.getColumnSpec(col);
                setIcon(colSpec.getType().getIcon());
                setFont(getFont().deriveFont(Font.BOLD));
            } else {
                setIcon(null);
                setFont(getFont().deriveFont(m_defaultFontStyle));
            }
            return c;
        }
    }
}
