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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
 *   30 Jan 2015 (Gabor): created
 */
package org.knime.json.util;

import java.awt.Component;
import java.awt.event.MouseEvent;
import java.util.EventObject;

import javax.swing.DefaultCellEditor;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import org.knime.core.node.util.WatermarkTextField;

/**
 * A {@link KeyValuePanel} with copy from second column on any change of it.
 *
 * @author Gabor Bakos
 */
@SuppressWarnings("serial")
public class KeyValueTablePanelWithCopyFromRight extends KeyValuePanel {

    /**
     * @param model
     */
    protected KeyValueTablePanelWithCopyFromRight(final KeyValueTableModel model) {
        super(model);
        model.addModelListenerCopyFromSecondColumn();
        setEditors();
    }

    /**
     *
     */
    public void setEditors() {
        getTable().getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(new WatermarkTextField()) {
            {
//                editorComponent = new WatermarkTextField();
                editorComponent.putClientProperty("JTextField.isTableCellEditor",
                    Boolean.TRUE);
                delegate = new EditorDelegate() {
                    /**
                     * {@inheritDoc}
                     */
                    @Override
                    public void setValue(final Object v) {
                        ((WatermarkTextField)editorComponent).setText(v == null || v.toString().isEmpty() ? null : v.toString());
                        super.setValue(v);
                    }
                    @Override
                    public Object getCellEditorValue() {
                        String text = ((WatermarkTextField)editorComponent).getText();
                        return text == null || text.isEmpty() ? null : text;
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
//                        if (editorComponent.isEditable()) {
//                            // Commit edited value.
//                            ((WatermarkTextField)editorComponent).actionPerformed(new ActionEvent(
//                                    ColumnSelectionCellEditor.this, 0, ""));
//                        }
                        return super.stopCellEditing();
                    }

                };
            }
            /**
             * {@inheritDoc}
             */
            @Override
            public Component getTableCellEditorComponent(final JTable table, final Object value, final boolean isSelected, final int row,
                final int column) {
//                WatermarkTextField ret = new WatermarkTextField();//(WatermarkTextField)super.getTableCellEditorComponent(table, value, isSelected, row, column);
//                Object key = getTable().getValueAt(row, 0);
//                ret.setText(key == null ? null : key.toString());
//                ret.setWatermark(getTable().getValueAt(row, 1).toString());
//                return ret;
                WatermarkTextField ret = (WatermarkTextField)super.getTableCellEditorComponent(table, value, isSelected, row, column);
                Object v = getTable().getValueAt(row, 1);
                ret.setWatermark(v == null ? "" : v.toString());
                return ret;
            }
        });
        getTable().getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            /**
             * {@inheritDoc}
             */
            @Override
            public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected,
                final boolean hasFocus, final int row, final int column) {
                if (value == null) {
                    Component ret = super.getTableCellRendererComponent(table, getTable().getValueAt(row, 1), isSelected, hasFocus, row, column);
                    ret.setEnabled(false);
                    return ret;
                }
                Component ret = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                ret.setEnabled(table != null && table.isEnabled());
                return ret;
            }
        });
    }

    /**
     *
     */
    public KeyValueTablePanelWithCopyFromRight() {
        this(initModel());
    }

    /**
     * @return
     */
    private static KeyValueTableModel initModel() {
        return new KeyValueTableModel();
    }

}
