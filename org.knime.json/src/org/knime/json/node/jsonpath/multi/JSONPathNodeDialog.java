/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   14 Sept. 2014 (Gabor): created
 */
package org.knime.json.node.jsonpath.multi;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicReference;

import javax.json.stream.JsonLocation;
import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.text.BadLocationException;
import javax.swing.text.Highlighter;
import javax.swing.text.TextAction;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextAreaHighlighter;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.fife.ui.rtextarea.SmartHighlightPainter;
import org.knime.base.data.aggregation.dialogutil.BooleanCellRenderer;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.json.JSONValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.DataAwareNodeDialogPane;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.ColumnSelectionComboxBox;
import org.knime.core.util.Pair;
import org.knime.json.node.jsonpath.JsonPathUtil;
import org.knime.json.node.jsonpath.util.JsonPathUtils;
import org.knime.json.node.jsonpath.util.Jsr353WithCanonicalPaths;
import org.knime.json.node.jsonpath.util.OutputKind;
import org.knime.json.node.util.GUIFactory;
import org.knime.json.util.OutputType;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.ReadContext;

/**
 * <code>NodeDialog</code> for the "JSONPath" Node. Selects certain paths from the selected JSON column.
 *
 * @author Gabor Bakos
 */
class JSONPathNodeDialog extends DataAwareNodeDialogPane {
    /**
     *
     */
    private static final String INVALID_PATH_EXCEPTION = "InvalidPathException";

    /**
     *
     */
    private final class AddRow extends AbstractAction {
        private static final long serialVersionUID = 3462402745035997468L;

        private boolean m_multiple;

        /**
         * @param name
         * @param multiple
         */
        private AddRow(final String name, final boolean multiple) {
            super(name);
            this.m_multiple = multiple;
        }

        @Override
        public void actionPerformed(final ActionEvent e) {
            int caretPosition = m_preview.getCaretPosition();
            String path = m_paths.findPath(caretPosition);
            if (path != null) {
                path = m_multiple ? path.replaceAll("\\[-?\\d+\\]", "[*]") : path;
                SingleSetting setting = new SingleSetting();
                setting.setJsonPath(path);
                setting.setNewColumnName(suggestColumnName(path));
                setting.setResultIsList(m_multiple);
                Object read = JsonPath.compile(path).read(m_preview.getText());
                final AtomicReference<String> warning = new AtomicReference<>();
                OutputKind kind = JsonPathUtils.kindOfJackson(JsonPathUtil.toJackson(JsonNodeFactory.instance, read), warning);
                setting.setReturnType(kind.getType());
                m_tableModel.addRow(setting);
                int lastRow = m_tableModel.getRowCount() - 1;
                m_table.getSelectionModel().setSelectionInterval(lastRow, lastRow);
            }
        }

        /**
         * @param path The path used to suggest a column name.
         * @return The last key (with an {@code s} suffix if {@code path} contains {@code *}) in the path, else the
         *         whole {@code path}.
         */
        private String suggestColumnName(final String path) {
            if (path.length() <= 2 || path.indexOf(']') < 0) {
                return path;
            }
            int end = path.lastIndexOf("']");
            int start = path.lastIndexOf("['");
            if (end > 0 && start >= 0) {
                String resPath = path.substring(start + 2, end);
                if (path.contains("*")) {
                    resPath += "s";
                }
                return resPath;
            }
            return path;
        }
    }

    @SuppressWarnings("unchecked")
    private final ColumnSelectionComboxBox m_inputColumn = new ColumnSelectionComboxBox(JSONValue.class);

    private final JCheckBox m_removeSourceColumn = new JCheckBox("Remove source column");

    private JTextField m_path, m_outputColumn;

    private JCheckBox m_resultIsList;

    private JCheckBox m_returnPaths;

    private JLabel m_nonDefiniteWarning, m_syntaxError;

    private final JSONPathTableModel m_tableModel = new JSONPathTableModel();

    private final JTable m_table = new JTable(m_tableModel);

    //    private JPanel m_settingPanel = new JPanel(new GridBagLayout());

    private JSONPathSettings m_settings = new JSONPathSettings(getLogger());

    private DefaultComboBoxModel<OutputType> m_outputTypeModel;

    private JComboBox<OutputType> m_outputTypes;

    private RSyntaxTextArea m_preview = new RSyntaxTextArea(8, 80);

    private RTextScrollPane m_previewContainer = new RTextScrollPane(m_preview, true);

    private Jsr353WithCanonicalPaths m_paths;

    private BufferedDataTable m_inputTable;

    private final JButton m_addSinglePreviewSelection = new JButton(new AddRow("Add single query", false));

    private final JButton m_addMultiplePreviewSelection = new JButton(new AddRow("Add collection query", true));

    private Object m_caretHighlight;

    private JButton m_edit;

    private JButton m_remove;

    /**
     * New pane for configuring the JSONPath node.
     */
    protected JSONPathNodeDialog() {
        super();
        JPanel panel = new JPanel(new GridBagLayout());
        int afterInputControls = addInputControls(panel);
        GridBagConstraints gbc = createInitialConstraints();
        gbc.gridy = addTable(panel, afterInputControls);
        gbc.weightx = 1;
        //        gbc.weighty = 0;
        gbc.gridwidth = 1;
        //addSingleSettingControls(m_settingPanel);
        //panel.add(m_settingPanel, gbc);
        //        gbc.gridy++;
        gbc.weighty = 1;
        //        m_previewContainer.getViewport().setPreferredSize(new Dimension(400, 100));
        //        m_previewContainer.setPreferredSize(new Dimension(400, 100));
        m_preview.setHighlighter(new RSyntaxTextAreaHighlighter());
        m_preview.setEditable(false);
        m_preview.addCaretListener(new CaretListener() {
            @Override
            public void caretUpdate(final CaretEvent e) {
                if (m_paths == null || m_preview == null) {
                    return;
                }
                String path = m_paths.findPath(m_preview.getCaretPosition());
                m_addSinglePreviewSelection.setToolTipText(path);
                if (path != null) {
                    m_addSinglePreviewSelection.setEnabled(true);
                    String starPath = path.replaceAll("\\[-?\\d+\\]", "[*]");
                    m_addMultiplePreviewSelection.setEnabled(!starPath.equals(path));
                    m_addMultiplePreviewSelection.setToolTipText(starPath);
                    Pair<JsonLocation, JsonLocation> pair = m_paths.get(path);
                    try {
                        if (m_caretHighlight == null
                            || !Arrays.asList(m_preview.getHighlighter().getHighlights()).contains(m_caretHighlight)) {
                            m_caretHighlight =
                                m_preview.getHighlighter().addHighlight((int)pair.getFirst().getStreamOffset(),
                                    (int)pair.getSecond().getStreamOffset(),
                                    new SmartHighlightPainter(Color.LIGHT_GRAY));
                        } else {
                            m_preview.getHighlighter().changeHighlight(m_caretHighlight,
                                (int)pair.getFirst().getStreamOffset(), (int)pair.getSecond().getStreamOffset());
                        }
                    } catch (BadLocationException e1) {
                        //ignore
                    }
                } else {
                    m_preview.getHighlighter().removeHighlight(m_caretHighlight);
                    m_addSinglePreviewSelection.setEnabled(false);
                    m_addMultiplePreviewSelection.setEnabled(false);
                }
            }
        });
        // add "Add JSONPath" option to popup menu
        JPopupMenu popup = m_preview.getPopupMenu();
        popup.addSeparator();
        @SuppressWarnings("serial")
        final JMenuItem menuItem = new JMenuItem(new TextAction("Add JSONPath") {
            private AddRow m_addRow = new AddRow("Add JSONPath", false);

            @Override
            public void actionPerformed(final ActionEvent e) {
                m_addRow.actionPerformed(e);
            }
        });
        popup.add(menuItem);
        JPanel preview = new JPanel(new BorderLayout());
        preview.setBorder(new TitledBorder("JSON-Cell Preview"));
        preview.add(m_previewContainer, BorderLayout.CENTER);
        panel.add(preview, gbc);
        addTab("Settings", panel, true);
        //updateEnabled();
    }

    /**
     * @param panel
     * @param afterInputControls
     * @return
     */
    private int addTable(final JPanel panel, final int afterInputControls) {
        m_table.setAutoCreateColumnsFromModel(false);
        for (int i = m_table.getColumnCount(); i-- > 0;) {
            m_table.getColumnModel().removeColumn(m_table.getColumnModel().getColumn(i));
        }
        JCheckBox checkBox = new JCheckBox("");
        checkBox.setHorizontalAlignment(SwingConstants.CENTER);
        @SuppressWarnings("serial")
        JComboBox<OutputType> outputComboBox = new JComboBox<OutputType>() {

            /**
             * {@inheritDoc}
             */
            @Override
            public Object getSelectedItem() {
                Object selectedItem = super.getSelectedItem();
                if (selectedItem instanceof Pair<?, ?>) {
                    Pair<?, ?> pair = (Pair<?, ?>)selectedItem;
                    return pair.getFirst();
                }
                return selectedItem;
            }
        };
        outputComboBox.setEditable(false);
        for (OutputType outputType : OutputType.values()) {
            outputComboBox.addItem(outputType);
        }
        final TableColumn colName = new TableColumn(0, 20, new TableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(final JTable table, final Object value,
                final boolean isSelected, final boolean hasFocus, final int row, final int column) {
                String text = (String)((Pair<?, ?>)value).getFirst();
                OutputType outputType = (OutputType)((Pair<?, ?>)value).getSecond();
                JLabel ret =
                    new JLabel(text, outputType == null ? DataType.getMissingCell().getType().getIcon() :
                        outputType.getIcon(), SwingConstants.HORIZONTAL);
                //                ret.setOpaque(true);
                JPanel res = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
                res.add(ret);
                if (isSelected) {
                    res.setForeground(table.getSelectionForeground());
                    res.setBackground(table.getSelectionBackground());
                } else {
                    res.setForeground(table.getForeground());
                    res.setBackground(table.getBackground());
                }

                return res;
            }
        }, new DefaultCellEditor(outputComboBox)), jsonPath = new TableColumn(1, 25), isPaths =
            new TableColumn(3, 40, new BooleanCellRenderer("Return paths instead of value"), new DefaultCellEditor(
                checkBox)), resultIsList =
            new TableColumn(2, 30, new BooleanCellRenderer("Result is list"), new DefaultCellEditor(checkBox));
        isPaths.setMaxWidth(40);
        isPaths.setResizable(false);
        resultIsList.setMaxWidth(30);
        resultIsList.setResizable(false);
        jsonPath.setMinWidth(100);
        colName.setMinWidth(100);
        colName.setHeaderValue("Output column");
        m_table.setVisible(true);
        m_table.addColumn(colName);
        //m_tableModel.addColumn(colName.getHeaderValue());
        jsonPath.setHeaderValue("JSONPath");
        m_table.addColumn(jsonPath);
        //m_tableModel.addColumn(jsonPath.getHeaderValue());
        //        outputType.setHeaderValue("Type");
        //        outputType.setResizable(false);
        //        m_table.addColumn(outputType);
        //m_tableModel.addColumn(outputType.getHeaderValue());
        resultIsList.setHeaderValue("List");
        m_table.addColumn(resultIsList);
        //m_tableModel.addColumn(resultIsList.getHeaderValue());
        isPaths.setHeaderValue("Paths");
        m_table.addColumn(isPaths);
        m_table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                if (e.getClickCount() == 2) {
                    edit(m_table.getSelectedRow());
                }
            }
        });
        //m_tableModel.addColumn(isPaths.getHeaderValue());
        GridBagConstraints gbc = createInitialConstraints();
        gbc.gridy = afterInputControls;
        JPanel p = new JPanel(), controls = new JPanel();
        p.setLayout(new BorderLayout());
        controls.setLayout(new BoxLayout(controls, BoxLayout.LINE_AXIS));
        JScrollPane tableScrollPane = new JScrollPane(m_table);
        tableScrollPane.setPreferredSize(new Dimension(350, 150));
        tableScrollPane.getViewport().setPreferredSize(new Dimension(300, 150));
        p.add(tableScrollPane, BorderLayout.CENTER);
        controls.add(m_addSinglePreviewSelection);
        controls.add(m_addMultiplePreviewSelection);
        controls.add(Box.createHorizontalGlue());
        controls.add(new JButton(new AbstractAction("Add JSONPath") {
            private static final long serialVersionUID = 2789893122362849755L;

            @Override
            public void actionPerformed(final ActionEvent e) {
                SingleSetting setting = new SingleSetting();
                setting.setNewColumnName("column");
                m_tableModel.addRow(setting);
            }
        }));
        controls.add(Box.createHorizontalStrut(11));
        m_edit = new JButton(new AbstractAction("Edit JSONPath") {
            private static final long serialVersionUID = -6654836901873863707L;

            @Override
            public void actionPerformed(final ActionEvent e) {
                int selectedRow = m_table.getSelectedRow();
                if (selectedRow >= 0) {
                    edit(selectedRow);
                }
            }
        });
        m_edit.setEnabled(false);
        controls.add(m_edit);
        controls.add(Box.createHorizontalStrut(11));
        m_remove = new JButton(new AbstractAction("Remove JSONPath") {
            private static final long serialVersionUID = 3305786869021888749L;

            @Override
            public void actionPerformed(final ActionEvent e) {
                int selectedRow = m_table.getSelectedRow();
                if (selectedRow >= 0) {
                    m_tableModel.removeRow(selectedRow);
                }
            }
        });
        controls.add(m_remove);
        controls.add(Box.createHorizontalStrut(11));
        final JButton moveUpButton = new JButton("\u2B06"); // up arrow
        moveUpButton.addActionListener(e -> {
            int selectedRow = m_table.getSelectedRow();
            if (selectedRow > 0 && m_tableModel.moveUp(selectedRow)) {
                m_table.getSelectionModel().setSelectionInterval(selectedRow - 1, selectedRow - 1);
            }
        });
        controls.add(moveUpButton);
        controls.add(Box.createHorizontalStrut(11));
        final JButton moveDownButton = new JButton("\u2B07"); // down arrow
        moveDownButton.addActionListener(e -> {
            int selectedRow = m_table.getSelectedRow();
            if (selectedRow >= 0 && m_tableModel.moveDown(selectedRow)) {
                m_table.getSelectionModel().setSelectionInterval(selectedRow + 1, selectedRow + 1);
            }
        });
        controls.add(moveDownButton);
        controls.add(Box.createHorizontalStrut(11));
        //        controls.add(new JButton(new AbstractAction("Edit") {
        //            private static final long serialVersionUID = -2065702331538862108L;
        //
        //            @Override
        //            public void actionPerformed(final ActionEvent e) {
        //                updateSingleSetting();
        //            }
        //        }));
        m_table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(final ListSelectionEvent e) {
                updateSingleSetting();
                //updateEnabled();
                ListSelectionModel selectionModel = m_table.getSelectionModel();
                boolean isSingleSelection = selectionModel.getMinSelectionIndex() >= 0
                        && selectionModel.getMinSelectionIndex() == selectionModel.getMaxSelectionIndex();
                moveUpButton.setEnabled(selectionModel.getMinSelectionIndex() > 0 && isSingleSelection);
                moveDownButton.setEnabled(selectionModel.getMaxSelectionIndex() < m_table.getRowCount() - 1
                    && isSingleSelection);
                m_edit.setEnabled(!selectionModel.isSelectionEmpty());
                m_remove.setEnabled(!selectionModel.isSelectionEmpty());
            }
        });
        m_tableModel.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(final TableModelEvent e) {
                if (e.getFirstRow() == e.getLastRow() && e.getFirstRow() == m_table.getSelectedRow()) {
                    updateSingleSetting();
//                    switch (e.getColumn()) {
//                        case TableModelEvent.ALL_COLUMNS:
//                            //skip:
//                            break;
//                        case 0:
//                            Pair<?, ?> pair = (Pair<?, ?>)m_tableModel.getValueAt(e.getFirstRow(), 0);
//                            Object outputColName = pair.getFirst();
//                            if (!Objects.equals(outputColName, m_outputColumn.getText())) {
//                                m_outputColumn.setText((String)outputColName);
//                            }
//                            if (!Objects.equals(pair.getSecond(), m_outputTypes.getSelectedItem())) {
//                                m_outputTypes.setSelectedItem(pair.getSecond());
//                            }
//                            break;
//                        case 1:
//                            if (!Objects.equals(m_tableModel.getValueAt(e.getFirstRow(), 1), m_path.getText())) {
//                                m_path.setText((String)m_tableModel.getValueAt(e.getFirstRow(), 1));
//                            }
//                            break;
//                        case 2:
//                            if (((Boolean)m_table.getValueAt(e.getFirstRow(), 2)).booleanValue() != m_resultIsList
//                                .isSelected()) {
//                                m_resultIsList.setSelected(!m_resultIsList.isSelected());
//                            }
//                            break;
//                        case 3:
//                            if (((Boolean)m_table.getValueAt(e.getFirstRow(), 3)).booleanValue() != m_returnPaths
//                                .isSelected()) {
//                                m_returnPaths.setSelected(!m_returnPaths.isSelected());
//                            }
//                        default:
//                            break;
//                    }
                }
            }
        });
        p.add(controls, BorderLayout.SOUTH);
        p.setBorder(new TitledBorder("Outputs"));
        panel.add(p, gbc);
        return ++gbc.gridy;
    }

    /**
     * @param selectedRow
     */
    protected void edit(final int selectedRow) {
        final JDialog dialog = new JDialog(getFrame(), "Edit", true);
        dialog.setPreferredSize(new Dimension(550, 200));
        Container cp = dialog.getContentPane();
        JPanel outer = new JPanel(new BorderLayout());
        JPanel panel = new JPanel(new GridBagLayout());
        addSingleSettingControls(panel);
        m_path.setText((String)m_tableModel.getValueAt(selectedRow, 1));
        m_resultIsList.setSelected((boolean)m_tableModel.getValueAt(selectedRow, 2));
        m_returnPaths.setSelected((boolean)m_tableModel.getValueAt(selectedRow, 3));
        m_outputTypeModel.setSelectedItem(((Pair<?, ?>)m_tableModel.getValueAt(selectedRow, 0)).getSecond());
        m_outputColumn.setText((String)((Pair<?, ?>)m_tableModel.getValueAt(selectedRow, 0)).getFirst());
        outer.add(panel, BorderLayout.CENTER);
        JPanel controls = new JPanel();
        outer.add(controls, BorderLayout.SOUTH);
        cp.add(outer);
        controls.setLayout(new BoxLayout(controls, BoxLayout.LINE_AXIS));
        controls.add(Box.createHorizontalGlue());
        controls.add(new JButton(new AbstractAction("OK") {
            private static final long serialVersionUID = -7407212263971824342L;

            @Override
            public void actionPerformed(final ActionEvent e) {
                m_tableModel.setValueAt(Pair.create(m_outputColumn.getText(), m_outputTypeModel.getSelectedItem()),
                    selectedRow, 0);
                m_tableModel.setValueAt(m_path.getText(), selectedRow, 1);
                m_tableModel.setValueAt(m_resultIsList.isSelected(), selectedRow, 2);
                m_tableModel.setValueAt(m_returnPaths.isSelected(), selectedRow, 3);
                dialog.dispose();
            }
        }));
        AbstractAction cancel = new AbstractAction("Cancel") {
            private static final long serialVersionUID = 7941634258650889859L;

            @Override
            public void actionPerformed(final ActionEvent e) {
                dialog.dispose();
            }
        };
        controls.add(new JButton(cancel));
        dialog.getRootPane().registerKeyboardAction(cancel, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW);
        dialog.pack();
        m_outputColumn.requestFocusInWindow();
        dialog.setVisible(true);
    }

    /**
     * @param panel
     */
    private int addInputControls(final JPanel panel) {
        GridBagConstraints gbc = createInitialConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = 2;
        JPanel inputColumnWrapper = new JPanel(new BorderLayout());
        inputColumnWrapper.setBorder(new TitledBorder("Input"));
        inputColumnWrapper.add(m_inputColumn, BorderLayout.CENTER);

        panel.add(inputColumnWrapper, gbc);
        m_inputColumn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                updatePreview();
            }
        });
        m_inputColumn.setBorder(null);
        gbc.gridy++;
        panel.add(m_removeSourceColumn, gbc);
        return ++gbc.gridy;
    }

    /**
     * Override only if you want to change the layout of the default controls. Else just change the returned value.
     *
     * @return The basic {@link GridBagConstraints}.
     */
    protected GridBagConstraints createInitialConstraints() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        gbc.weighty = 0;
        return gbc;
    }

    /**
     * @param panel
     */
    protected void addSingleSettingControls(final JPanel panel) {
        m_nonDefiniteWarning = new JLabel("Path is non-definite");
        m_syntaxError = new JLabel();
        m_syntaxError.setForeground(Color.RED);
        //panel.setPreferredSize(new Dimension(800, 300));
        GridBagConstraints gbc = createInitialConstraints();
        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.weightx = 0;
        panel.add(new JLabel("JSONPath"), gbc);
        gbc.gridx = 1;
        m_path = GUIFactory.createTextField("", 22);
        m_path.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(final DocumentEvent e) {
                handleEdit();
            }

            @Override
            public void removeUpdate(final DocumentEvent e) {
                handleEdit();
            }

            @Override
            public void changedUpdate(final DocumentEvent e) {
                handleEdit();
            }

            private void handleEdit() {
                updateWarnings();
//                int selectedRow = m_table.getSelectedRow();
//                if (selectedRow >= 0) {
//                    m_tableModel.setValueAt(m_path.getText(), selectedRow, 1);
//                }
            }
        });
        gbc.weightx = 1;
        panel.add(m_path, gbc);
        gbc.gridy++;

        m_outputColumn = GUIFactory.createTextField("", 22);
        m_outputColumn.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(final DocumentEvent e) {
                handleEdit();
            }

            @Override
            public void removeUpdate(final DocumentEvent e) {
                handleEdit();
            }

            @Override
            public void changedUpdate(final DocumentEvent e) {
                handleEdit();
            }

            private void handleEdit() {
//                int selectedRow = m_table.getSelectedRow();
//                if (selectedRow >= 0) {
//                    m_tableModel.setValueAt(m_outputColumn.getText(), selectedRow, 0);
//                }
            }
        });
        gbc.gridx = 0;
        gbc.weightx = 0;
        panel.add(new JLabel("Column name: "), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(m_outputColumn, gbc);
        gbc.gridy++;

        gbc.weightx = 1;
        gbc.gridy = addOutputTypePanel(panel, gbc.gridy);

        gbc.gridx = 1;
        panel.add(m_nonDefiniteWarning, gbc);
        m_nonDefiniteWarning.setForeground(Color.RED);//PINK?
        m_nonDefiniteWarning.setVisible(false);
        gbc.gridx = 1;
        panel.add(m_syntaxError, gbc);
        gbc.gridy++;

        m_returnPaths = new JCheckBox("Return the paths instead of values");
        m_returnPaths.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                updateEnabled();
//                int selectedRow = m_table.getSelectedRow();
//                if (selectedRow >= 0) {
//                    m_tableModel.setValueAt(m_returnPaths.isSelected(), selectedRow, 3);
//                }
            }
        });
        gbc.gridx = 0;
        m_resultIsList = new JCheckBox("Result is list");
        panel.add(m_resultIsList, gbc);
        gbc.gridx = 1;
        panel.add(m_returnPaths, gbc);
        gbc.gridy++;

        //        gbc.gridx = 0;
        //        panel.add(m_addSinglePreviewSelection, gbc);
        //        gbc.gridx++;
        //        panel.add(m_addMultiplePreviewSelection, gbc);

        m_resultIsList.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                updateWarnings();
                updateEnabled();
//                int selectedRow = m_table.getSelectedRow();
//                if (selectedRow >= 0) {
//                    m_tableModel.setValueAt(m_resultIsList.isSelected(), selectedRow, 2);
//                }
            }
        });
    }

    /**
     * Adds the output and the output-specific controls to the {@code panel}.
     *
     * @param panel The (main) configuration panel.
     * @param gridy The current position with {@link GridBagConstraints}.
     * @return The new y position of {@link GridBagConstraints}.
     */
    protected int addOutputTypePanel(final JPanel panel, final int gridy) {
        final GridBagConstraints gbc = createInitialConstraints();
        gbc.gridy = gridy;

        panel.add(new JLabel("Result type"), gbc);
        gbc.gridx = 1;
        m_outputTypeModel = new DefaultComboBoxModel<>(OutputType.values());
        m_outputTypes = new JComboBox<>(m_outputTypeModel);
        m_outputTypes.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(final ItemEvent e) {
//                int selectedRow = m_table.getSelectedRow();
//                if (selectedRow >= 0 && m_listenToChanges) {
//                    if (e.getStateChange() == ItemEvent.SELECTED
//                        && !Objects.equals(m_outputTypeModel.getSelectedItem(),
//                            ((Pair<?, ?>)m_tableModel.getValueAt(selectedRow, 0)).getSecond())) {
//                        m_tableModel.setValueAt(m_outputTypeModel.getSelectedItem(), selectedRow, 0);
//                    }
//                }
            }
        });
        m_outputTypes.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {

                OutputType type = (OutputType)m_outputTypeModel.getSelectedItem();
                int selectedRow = m_table.getSelectedRow();
                if (selectedRow >= 0) {
                    m_tableModel.setValueAt(type, selectedRow, 2);
                }
            }
        });
        gbc.weightx = 0;
        panel.add(m_outputTypes, gbc);
        gbc.gridy++;

        return gbc.gridy;
    }

    private void updateEnabled() {
        boolean rowSelected = m_table.getSelectedRow() >= 0;
        if (m_returnPaths.isSelected() && !(OutputType.String == m_outputTypeModel.getSelectedItem())) {
            m_outputTypeModel.setSelectedItem(OutputType.String);
        }
        m_resultIsList.setEnabled(rowSelected && !m_returnPaths.isSelected());
        m_returnPaths.setEnabled(rowSelected);
        m_outputTypes.setEnabled(rowSelected && !m_returnPaths.isSelected());
        m_resultIsList.setSelected(rowSelected && (m_returnPaths.isSelected() || m_resultIsList.isSelected()));
        OutputType selectedOutputType = (OutputType)m_outputTypeModel.getSelectedItem();
        EnumSet<OutputType> supportedOutputTypes = EnumSet.allOf(OutputType.class);
        if (m_returnPaths.isSelected()) {
            supportedOutputTypes = EnumSet.of(OutputType.String);
        }
        m_outputTypeModel.removeAllElements();
        for (OutputType outputType : supportedOutputTypes) {
            m_outputTypeModel.addElement(outputType);
        }
        if (selectedOutputType != null && supportedOutputTypes.contains(selectedOutputType)) {
            m_outputTypeModel.setSelectedItem(selectedOutputType);
        } else {
            m_outputTypeModel.setSelectedItem(supportedOutputTypes.iterator().next());
        }
        m_path.setEnabled(rowSelected);
        m_outputColumn.setEnabled(rowSelected);
        //        m_outputTypes.setEnabled(rowSelected);
        //        m_returnPaths.setEnabled(rowSelected);
        //        m_resultIsList.setEnabled(rowSelected);
    }

    private void updateWarnings() {
        String text = m_path.getText();

        // currently, a bug in the JsonPath library requires commas in quotes to be (un)escaped manually, see
        // - AP-10014
        // - https://github.com/json-path/JsonPath/issues/400
        // - https://github.com/json-path/JsonPath/issues/487
        text = JsonPathUtils.escapeCommas(text);

        m_nonDefiniteWarning.setVisible(false);
        m_syntaxError.setText("");
        try {
            if (!JsonPath.compile(text).isDefinite()) {
                m_nonDefiniteWarning.setVisible(!m_resultIsList.isSelected() && !m_returnPaths.isSelected());
            }
        } catch (RuntimeException e) {
            String message = e.getMessage();
            if (message.contains(INVALID_PATH_EXCEPTION)) {
                message =
                    message
                        .substring(message.lastIndexOf(INVALID_PATH_EXCEPTION) + INVALID_PATH_EXCEPTION.length() + 1);
            } else if (e.getCause() instanceof StringIndexOutOfBoundsException) {
                message = "Incomplete path";
            }
            m_syntaxError.setText(message);
        }
    }

    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        loadSettingsFrom(settings, new DataTableSpec[]{(DataTableSpec)specs[0]});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
        throws NotConfigurableException {
        m_settings.loadSettingsForDialogs(settings, specs);
        //m_inputColumn.setSelectedColumn(m_settings.getInputColumnName());
        m_inputColumn.update(specs[0], m_settings.getInputColumnName());
        m_removeSourceColumn.setSelected(m_settings.isRemoveInputColumn());
        m_tableModel.clear();
        boolean enableEditAndRemove = false;
        for (SingleSetting single : m_settings.getOutputSettings()) {
            m_tableModel.addRow(single);
            enableEditAndRemove = true;
        }
        m_edit.setEnabled(enableEditAndRemove);
        m_remove.setEnabled(enableEditAndRemove);
        m_previewContainer.setVisible(false);
        //        m_path.setText(m_settings.getJsonPath());
        //        m_resultIsList.setSelected(getSettings().isResultIsList());
        //        m_returnPaths.setSelected(getSettings().isReturnPaths());
        //updateEnabled();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final BufferedDataTable[] inputs)
        throws NotConfigurableException {
        loadSettingsFrom(settings, new DataTableSpec[]{inputs[0].getSpec()});
        m_inputTable = inputs[0];

        updatePreview();
    }

    /**
     *
     */
    public void updatePreview() {
        m_previewContainer.setVisible(m_inputTable != null);
        if (m_inputTable == null || m_inputTable.size() == 0L) {
            m_preview.setText("No input");
            m_paths = new Jsr353WithCanonicalPaths("{}"/*new JsonNodeFactory(true).nullNode()*/);
            return;
        }
        int idx = m_inputTable.getDataTableSpec().findColumnIndex(m_inputColumn.getSelectedColumn());
        if (idx >= 0) {
            m_paths = new Jsr353WithCanonicalPaths("{}"/*new JsonNodeFactory(true).nullNode()*/);
            try (CloseableRowIterator it = m_inputTable.iteratorFailProve()) {
                JSONValue value = null;
                while (it.hasNext() && value == null) {
                    DataRow row = it.next();
                    DataCell cell = row.getCell(idx);
                    if (cell.isMissing()) {
                        continue;
                    }
                    if (cell instanceof JSONValue) {
                        JSONValue json = (JSONValue)cell;
                        value = json;
                    }
                }
                if (value == null) {
                    m_preview.setText("?");
                    m_paths = new Jsr353WithCanonicalPaths("{}"/*NullNode.getInstance()*/);
                } else {
                    m_preview.setText(value.toString());
                    m_paths = new Jsr353WithCanonicalPaths(value.toString());
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        //        try {
        //            JsonPath.compile(m_path.getText());
        //        } catch (RuntimeException e) {
        //            throw new InvalidSettingsException("Wrong path: " + m_path.getText() + "\n" + e.getMessage(), e);
        //        }
        m_settings.setInputColumnName(m_inputColumn.getSelectedColumn());
        m_settings.setRemoveInputColumn(m_removeSourceColumn.isSelected());
        m_settings.clearOutputSettings();
        for (int i = 0; i < m_tableModel.getRowCount(); ++i) {
            SingleSetting setting = new SingleSetting();
            setting.setNewColumnName((String)((Pair<?, ?>)m_tableModel.getValueAt(i, 0)).getFirst());
            setting.setJsonPath((String)m_tableModel.getValueAt(i, 1));
            setting.setReturnType((OutputType)((Pair<?, ?>)m_tableModel.getValueAt(i, 0)).getSecond());
            setting.setReturnPaths((Boolean)m_tableModel.getValueAt(i, 3));
            setting.setResultIsList((Boolean)m_tableModel.getValueAt(i, 2));
            m_settings.addOutputSetting(setting);
        }
        //        m_settings.setJsonPath(m_path.getText());
        //        m_settings.setResultIsList(m_resultIsList.isSelected());
        //        m_settings.setReturnPaths(m_returnPaths.isSelected());
        m_settings.saveSettingsTo(settings);
    }

    /**
     *
     */
    private void updateSingleSetting() {
        int selectedRow = m_table.getSelectedRow();
        Highlighter h = m_preview.getHighlighter();
        if (selectedRow >= 0 && selectedRow < m_table.getRowCount()) {

            // currently, a bug in the JsonPath library requires commas in quotes to be (un)escaped manually, see
            // - AP-10014
            // - https://github.com/json-path/JsonPath/issues/400
            // - https://github.com/json-path/JsonPath/issues/487
            String origPath = JsonPathUtils.escapeCommas((String)m_tableModel.getValueAt(selectedRow, 1));

            Boolean path = (Boolean)m_tableModel.getValueAt(selectedRow, 3);
            if (path != null && path.booleanValue()) {
                Pair<?, ?> pair = (Pair<?, ?>)m_tableModel.getValueAt(selectedRow, 0);
                m_tableModel.setValueAt(Pair.create(pair.getFirst(), OutputType.String), selectedRow, 0);
                m_tableModel.setValueAt(Boolean.TRUE, selectedRow, 2);
            }
            JsonPath jsonPath = JsonPath.compile(origPath);
            ReadContext parsed =
                JsonPath.using(Configuration.builder().options(Option.AS_PATH_LIST).build()).parse(m_preview.getText());
            try {
                Object read = parsed.read(jsonPath);
                Iterable<?> paths = JsonPathUtil.asList(read);
                h.removeAllHighlights();
                for (Object object : paths) {
                    Pair<javax.json.stream.JsonLocation, javax.json.stream.JsonLocation> context =
                        positionsFromPath(object);
                    //                    m_preview.setSelectionStart((int)context.getFirst().getByteOffset());
                    //                    m_preview.setSelectionEnd((int)context.getSecond().getByteOffset());
//                    m_preview.setSelectionStart((int)context.getSecond().getStreamOffset());
//                    m_preview.setSelectionEnd((int)context.getFirst().getStreamOffset());
                    try {
                        //                        h.addHighlight((int)context.getFirst().getByteOffset(), (int)context.getSecond().getByteOffset(),
                        //                            new SmartHighlightPainter());
                        h.addHighlight((int)context.getFirst().getStreamOffset(), (int)context.getSecond()
                            .getStreamOffset(), new SmartHighlightPainter());
                    } catch (BadLocationException e) {
                        //ignore
                    }
                }
                if (paths.iterator().hasNext()) {
                    m_preview.setCaretPosition((int)positionsFromPath(paths.iterator().next()).getFirst().getStreamOffset());
                }
            } catch (RuntimeException e) {
                h.removeAllHighlights();
            }
//            m_outputColumn.setText((String)((Pair<?, ?>)m_tableModel.getValueAt(selectedRow, 0)).getFirst());
//            m_listenToChanges = false;
//            m_outputTypeModel.setSelectedItem(((Pair<?, ?>)m_tableModel.getValueAt(selectedRow, 0)).getSecond());
//            m_listenToChanges = true;
//            m_returnPaths.setSelected((Boolean)m_tableModel.getValueAt(selectedRow, 3));
//            m_resultIsList.setSelected((boolean)m_tableModel.getValueAt(selectedRow, 2));
        } else {
            h.removeAllHighlights();
        }
//        updateEnabled();
    }

    /**
     * @param object
     * @return
     */
    private Pair<javax.json.stream.JsonLocation, javax.json.stream.JsonLocation> positionsFromPath(final Object object) {
        String normalizedPath = (String)object;
        Pair<javax.json.stream.JsonLocation, javax.json.stream.JsonLocation> context =
            m_paths.get(normalizedPath);
        return context;
    }

    /**
     * @return the parent frame
     */
    protected Frame getFrame() {
        Frame f = null;
        Container c = getPanel().getParent();
        while (c != null) {
            if (c instanceof Frame) {
                f = (Frame)c;
                break;
            }
            c = c.getParent();
        }
        return f;
    }
}
