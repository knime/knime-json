/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *   13 Mar 2015 (Gabor): created
 */
package org.knime.json.node.jsonpath.multi;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import javax.swing.table.AbstractTableModel;

import org.knime.core.util.Pair;
import org.knime.json.util.OutputType;

/**
 *
 * @author Gabor
 */
final class JSONPathTableModel extends AbstractTableModel implements Iterable<SingleSetting> {
    private static enum Columns {
        outputColNameAndType, path, list, paths;
    }

    private final List<SingleSetting> content = new ArrayList<>();

    /**
     *
     */
    private static final long serialVersionUID = 4397800184937700474L;

    /**
     *
     */
    public JSONPathTableModel() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getRowCount() {
        return content.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getColumnCount() {
        return 4;//colname+type, path, asList, paths
    }

    /**
     * {@inheritDoc}
     *
     * @throws IndexOutOfBoundsException when {@code rowIndex} or {@code columnIndex} is wrong.
     */
    @Override
    public Object getValueAt(final int rowIndex, final int columnIndex) {
        SingleSetting setting = content.get(rowIndex);
        Columns col = Columns.values()[columnIndex];
        switch (col) {
            case outputColNameAndType://output+
                return Pair.create(setting.getNewColumnName(), setting.getReturnType());
            case path:
                return setting.getJsonPath();
            case list:
                return setting.isResultIsList();
            case paths:
                return setting.isReturnPaths();
            default:
                throw new IllegalStateException("Unknown column: ");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setValueAt(final Object aValue, final int rowIndex, final int columnIndex) {
        Columns col = Columns.values()[columnIndex];
        if (rowIndex >= content.size()) {
            return;
        }
        SingleSetting setting = content.get(rowIndex);
        switch (col) {
            case outputColNameAndType: {
                if (aValue instanceof Pair<?, ?>) {
                    Pair<?, ?> pair = (Pair<?, ?>)aValue;
                    Object first = pair.getFirst();
                    Object second = pair.getSecond();
                    setValueAt(first, rowIndex, columnIndex);
                    setValueAt(second, rowIndex, columnIndex);
                } else if (aValue instanceof String) {
                    String outputName = (String)aValue;
                    String colName = setting.getNewColumnName();
                    setting.setNewColumnName(outputName);
                    if (!Objects.equals(colName, outputName)) {
                        fireTableCellUpdated(rowIndex, columnIndex);
                    }
                } else if (aValue instanceof OutputType) {
                    OutputType outputType = (OutputType)aValue;
                    OutputType returnType = setting.getReturnType();
                    setting.setReturnType(outputType);
                    if (!Objects.equals(returnType, outputType)) {
                        fireTableCellUpdated(rowIndex, columnIndex);
                    }
                }
                break;
            }
            case path:
            {
                if (aValue instanceof String) {
                    String path = (String)aValue;
                    String jsonPath = setting.getJsonPath();
                    setting.setJsonPath(path);
                    if (!Objects.equals(path, jsonPath)) {
                        fireTableCellUpdated(rowIndex, columnIndex);
                    }
                }
                break;
            }
            case list:
            {
                if (aValue instanceof Boolean) {
                    Boolean isList = (Boolean)aValue;
                    boolean resultIsList = setting.isResultIsList();
                    setting.setResultIsList(isList.booleanValue());
                    if (isList.booleanValue() != resultIsList) {
                        fireTableCellUpdated(rowIndex, columnIndex);
                    }
                }
                break;
            }
            case paths: {
                if (aValue instanceof Boolean) {
                    Boolean paths = (Boolean)aValue;
                    boolean returnPaths = setting.isReturnPaths();
                    setting.setReturnPaths(paths.booleanValue());
                    if (paths.booleanValue() != returnPaths) {
                        fireTableCellUpdated(rowIndex, columnIndex);
                    }
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<SingleSetting> iterator() {
        return content.iterator();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCellEditable(final int rowIndex, final int columnIndex) {
        return columnIndex > 0 && (columnIndex != 2 || !content.get(rowIndex).isReturnPaths());
    }

    void addRow(final SingleSetting setting) {
        content.add(setting);
        fireTableRowsInserted(content.size() - 1, content.size() -1);
    }
    SingleSetting newRow() {
        SingleSetting ret = new SingleSetting();
        addRow(ret);
        return ret;
    }
    void insertRow(final int rowIndex, final SingleSetting setting) {
        content.add(rowIndex, setting);
        fireTableRowsInserted(rowIndex, rowIndex);
    }
    void removeRow(final int rowIndex) {
        content.remove(rowIndex);
        fireTableRowsDeleted(rowIndex, rowIndex);
    }
    boolean moveUp(final int rowIndex) {
        if (rowIndex > 0 && rowIndex < content.size()) {
            content.add(rowIndex - 1, content.remove(rowIndex));
            fireTableRowsUpdated(rowIndex - 1, rowIndex);
            return true;
        }
        return false;
    }
    boolean moveDown(final int rowIndex) {
        if (rowIndex >= 0 && rowIndex < content.size() - 1) {
            content.add(rowIndex + 1, content.remove(rowIndex));
            fireTableRowsUpdated(rowIndex, rowIndex + 1);
            return true;
        }
        return false;
    }

    /**
     *
     */
    void clear() {
        int origSize = content.size();
        content.clear();
        if (origSize > 0) {
            fireTableRowsDeleted(0, origSize - 1);
        }
    }
}
