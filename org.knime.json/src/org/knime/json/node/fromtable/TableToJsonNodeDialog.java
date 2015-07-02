package org.knime.json.node.fromtable;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterPanel;
import org.knime.json.node.fromtable.TableToJsonNodeModel.Direction;
import org.knime.json.node.fromtable.TableToJsonNodeModel.RowKeyOption;
import org.knime.json.node.util.GUIFactory;

/**
 * <code>NodeDialog</code> for the "TableToJson" Node. Converts a whole table to a single JSON cell.
 *
 * @author Gabor Bakos
 */
@SuppressWarnings("restriction")
class TableToJsonNodeDialog extends NodeDialogPane {
    private final TableToJsonSettings m_settings = new TableToJsonSettings();

    private final DataColumnSpecFilterPanel m_selectedColumns;

    //    private final JCheckBox m_includeRowKeys;

    private final JTextField m_rowKeyKey = GUIFactory.createTextField(TableToJsonSettings.DEFAULT_ROWKEY_KEY, 11);

    private final JTextField m_outputColumn = GUIFactory.createTextField(
        TableToJsonSettings.DEFAULT_OUTPUT_COLUMN_NAME, 22);

    private final JTextField m_columnNameSeparator = GUIFactory.createTextField(
        TableToJsonSettings.DEFAULT_COLUMN_NAME_SEPARATOR, 3);

    private final ButtonGroup m_directionGroup = new ButtonGroup(), m_rowKeyGroup = new ButtonGroup();

    private final JRadioButton m_rowsOutside = new JRadioButton("Row-oriented (n input rows \u2192 1 output cell)",
        Direction.RowsOutside == TableToJsonSettings.DEFAULT_DIRECTION), m_columnsOutside = new JRadioButton(
        "Column-oriented (n input rows \u2192 1 output cell)", Direction.ColumnsOutside == TableToJsonSettings.DEFAULT_DIRECTION),
            m_keepRows = new JRadioButton("Keep rows (n input rows \u2192 n output cell)", Direction.KeepRows == TableToJsonSettings.DEFAULT_DIRECTION);

    private final JRadioButton m_omitRowKey = new JRadioButton("Omit row key",
        RowKeyOption.omit == TableToJsonSettings.DEFAULT_ROW_KEY_OPTION), m_rowKeyAsKey = new JRadioButton(
        "Row key as JSON key", RowKeyOption.asKey == TableToJsonSettings.DEFAULT_ROW_KEY_OPTION),
            m_rowKeyAsValue = new JRadioButton("Row key as JSON value with key: ",
                RowKeyOption.asValue == TableToJsonSettings.DEFAULT_ROW_KEY_OPTION);

    private final JCheckBox m_columnNamesAsPath = new JCheckBox(
        "Column names as paths, where path separator in column names:",
        TableToJsonSettings.DEFAULT_COLUMN_NAMES_AS_PATH), m_removeSourceColumns = new JCheckBox(
        "Remove source columns", TableToJsonSettings.DEFAULT_REMOVE_SOURCE_COLUMNS);

    /**
     * New pane for configuring the TableToJson node.
     */
    protected TableToJsonNodeDialog() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        m_selectedColumns = new DataColumnSpecFilterPanel();
        m_selectedColumns.setRemoveButtonText("<");
        m_selectedColumns.setRemoveAllButtonText("<<");
        m_selectedColumns.setAddButtonText(">");
        m_selectedColumns.setAddAllButtonText(">>");

        panel.add(m_selectedColumns, gbc);
        gbc.gridy++;
        gbc.gridwidth = 2;
        gbc.gridx = 0;
        ActionListener rowKeyListener = new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                m_rowKeyKey.setEnabled(m_rowKeyAsValue.isSelected());
            }
        };
        m_omitRowKey.addActionListener(rowKeyListener);
        m_rowKeyAsKey.addActionListener(rowKeyListener);
        m_rowKeyAsValue.addActionListener(rowKeyListener);
        m_rowKeyKey.setEnabled(m_rowKeyAsValue.isSelected());

        JPanel directionPanel = new JPanel(), rowKeyPanel =
            new JPanel(new GridBagLayout()), keepRowsPanel = new JPanel();
        directionPanel.setLayout(new BoxLayout(directionPanel, BoxLayout.PAGE_AXIS));
//        keepRowsPanel.setLayout(new BoxLayout(keepRowsPanel, BoxLayout.PAGE_AXIS));
        keepRowsPanel.setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));
        keepRowsPanel.setBorder(null);
        directionPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        GridBagConstraints gbc2 = new GridBagConstraints();
        gbc2.anchor = GridBagConstraints.LINE_START;
        gbc2.fill = GridBagConstraints.HORIZONTAL;
        gbc2.weightx = 1;
        gbc2.gridwidth = 2;
        gbc2.gridy = 0;
        gbc2.gridx = 0;
        rowKeyPanel.add(m_omitRowKey, gbc2);
        gbc2.gridy++;
        rowKeyPanel.add(m_rowKeyAsKey, gbc2);
        gbc2.gridy++;
        gbc2.gridwidth = 1;
        gbc2.weightx = 0;
        rowKeyPanel.add(m_rowKeyAsValue, gbc2);
        gbc2.gridx++;
        gbc2.weightx = 1;
        rowKeyPanel.add(m_rowKeyKey, gbc2);
        rowKeyPanel.setBorder(new TitledBorder("Row keys"));

        m_directionGroup.add(m_rowsOutside);
        m_directionGroup.add(m_columnsOutside);
        m_directionGroup.add(m_keepRows);

        m_rowKeyGroup.add(m_omitRowKey);
        m_rowKeyGroup.add(m_rowKeyAsKey);
        m_rowKeyGroup.add(m_rowKeyAsValue);

        m_rowsOutside.setAlignmentX(Component.LEFT_ALIGNMENT);
        directionPanel.add(m_rowsOutside);
        directionPanel.add(Box.createHorizontalGlue());
        m_columnsOutside.setAlignmentX(Component.LEFT_ALIGNMENT);
        directionPanel.add(m_columnsOutside);
        directionPanel.add(Box.createHorizontalGlue());
        m_keepRows.setAlignmentX(Component.LEFT_ALIGNMENT);
        directionPanel.add(keepRowsPanel);
        keepRowsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        keepRowsPanel.add(m_keepRows);
        keepRowsPanel.add(Box.createHorizontalGlue());
        ActionListener directionListener = new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                m_removeSourceColumns.setEnabled(m_keepRows.isSelected());
                m_rowKeyAsKey.setEnabled(!m_columnsOutside.isSelected());
                if (m_columnsOutside.isSelected() && m_rowKeyAsKey.isSelected()) {
                    m_omitRowKey.setSelected(true);
                }
            }};
        m_rowsOutside.addActionListener(directionListener);
        m_columnsOutside.addActionListener(directionListener);
        m_keepRows.addActionListener(directionListener);
//        Dimension fillDim = new Dimension(50, 20);
//        directionPanel.add(new Box.Filler(fillDim, fillDim, fillDim));
//        directionPanel.add(new JLabel("    "));
        m_removeSourceColumns.setAlignmentY(Component.TOP_ALIGNMENT);
//        m_removeSourceColumns.setAlignmentX(Component.CENTER_ALIGNMENT);
//        keepRowsPanel.add(Box.createHorizontalGlue());
        keepRowsPanel.add(Box.createHorizontalStrut(20));
        keepRowsPanel.add(m_removeSourceColumns);
        //keepRowsPanel.add(Box.createHorizontalGlue());
//        directionPanel.add(Box.createHorizontalGlue());
        directionPanel.setBorder(new TitledBorder("Aggregation direction"));
        gbc.gridheight = 3;
        gbc.weightx = 1;
        panel.add(rowKeyPanel, gbc);
        gbc.gridheight = 1;
        gbc.gridy++;
        gbc.gridy++;
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        JPanel fromColNamesWithSeparator = new JPanel(new FlowLayout(FlowLayout.LEADING));
        fromColNamesWithSeparator.add(m_columnNamesAsPath);
        fromColNamesWithSeparator.add(m_columnNameSeparator);
        m_columnNameSeparator.setEnabled(m_columnNamesAsPath.isSelected());
//        m_columnNamesAsPath.addChangeListener(new ChangeListener() {
//            @Override
//            public void stateChanged(final ChangeEvent e) {
//                m_columnNameSeparator.setEnabled(m_columnNamesAsPath.isSelected());
//            }
//        });
        m_columnNamesAsPath.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                m_columnNameSeparator.setEnabled(m_columnNamesAsPath.isSelected());
            }
        });
        panel.add(fromColNamesWithSeparator, gbc);
        gbc.gridy++;

        gbc.gridwidth = 2;
        gbc.gridx = 0;
        gbc.gridheight = 3;
        panel.add(directionPanel, gbc);
        gbc.gridheight = 1;
        gbc.gridy++;
        gbc.gridy++;
        gbc.gridy++;

        JPanel outputPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
        outputPanel.add(new JLabel("Output column name:"));
        outputPanel.add(m_outputColumn);
        panel.add(outputPanel, gbc);
        addTab("Settings", panel);
        //        addDialogComponent(new DialogComponentColumnFilter2(TableToJsonNodeModel.createSelectedColumns(), 0));
        //        addDialogComponent(new DialogComponentBoolean(TableToJsonNodeModel.createIncludeRowKey(), "Include the row keys in the JSON"));
        //        addDialogComponent(new DialogComponentString(TableToJsonNodeModel.createRowKeyKey(), "RowKey keys in JSON"));
        //        addDialogComponent(new DialogComponentStringSelection(TableToJsonNodeModel.createDirection(), "Direction", Arrays.asList(Direction.RowsOutside.name(), Direction.KeepRows.name(), Direction.ColumnsOutside.name(), Direction.FromColumnNamesKeepRows.name())));
        //        addDialogComponent(new DialogComponentString(TableToJsonNodeModel.createColumnNameSeparator(), "Path separator in column names", true, 3));
        //        addDialogComponent(new DialogComponentString(TableToJsonNodeModel.createOutputColumnName(), "Output column name", true, 11));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_selectedColumns.saveConfiguration(m_settings.getSelectedColumns());
        m_settings.setRowKeyKey(m_rowKeyKey.getText());
        m_settings.setOutputColumnName(m_outputColumn.getText());
        m_settings.setColumnNameSeparator(m_columnNameSeparator.getText());
        m_settings.setDirection(m_rowsOutside.isSelected() ? Direction.RowsOutside : m_columnsOutside.isSelected()
            ? Direction.ColumnsOutside : m_keepRows.isSelected() ? Direction.KeepRows : CheckUtils.checkNotNull(
                (Direction)null, "No direction options were selected!"));
        m_settings.setColumnNamesAsPath(m_columnNamesAsPath.isSelected());
        m_settings.setRowKey(m_omitRowKey.isSelected() ? RowKeyOption.omit : m_rowKeyAsKey.isSelected()
            ? RowKeyOption.asKey : m_rowKeyAsValue.isSelected() ? RowKeyOption.asValue : CheckUtils.checkNotNull(
                (RowKeyOption)null, "No row key options were selected!"));
        m_settings.setRemoveSourceColumns(m_removeSourceColumns.isSelected());
        m_settings.saveSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
        throws NotConfigurableException {
        m_settings.loadSettingsDialog(settings, specs[0]);
        m_selectedColumns.loadConfiguration(m_settings.getSelectedColumns(), specs[0]);
        m_rowKeyKey.setText(m_settings.getRowKeyKey());
        m_columnNameSeparator.setText(m_settings.getColumnNameSeparator());
        switch (m_settings.getDirection()) {
            case RowsOutside:
                m_rowsOutside.setSelected(true);
                m_removeSourceColumns.setEnabled(false);
                break;
            case ColumnsOutside:
                m_columnsOutside.setSelected(true);
                m_removeSourceColumns.setEnabled(false);
                break;
            case KeepRows:
                m_keepRows.setSelected(true);
                m_removeSourceColumns.setEnabled(true);
                break;
            default:
                throw new UnsupportedOperationException("Unknown direction: " + m_settings.getDirection().name());
        }
        m_columnNamesAsPath.setSelected(m_settings.isColumnNamesAsPath());
        m_columnNameSeparator.setEnabled(m_settings.isColumnNamesAsPath());
        switch (m_settings.getRowKey()) {
            case omit:
                m_omitRowKey.setSelected(true);
                m_rowKeyKey.setEnabled(false);
                break;
            case asKey:
                m_rowKeyAsKey.setSelected(true);
                m_rowKeyKey.setEnabled(false);
                break;
            case asValue:
                m_rowKeyAsValue.setSelected(true);
                m_rowKeyKey.setEnabled(true);
                break;
        }
        m_removeSourceColumns.setSelected(m_settings.isRemoveSourceColumns());
        m_rowKeyAsKey.setEnabled(!m_columnsOutside.isSelected());
    }
}
