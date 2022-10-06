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
 *   24 Jun 2022 (alexander): created
 */
package org.knime.json.node.container.input.raw;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

/**
 * A simple table for editing key-value pairs.
 *
 * @author Alexander Fillbrunn, KNIME GmbH, Konstanz, Germany
 */
final class InteractiveKeyValueTable extends JPanel {

    private static final long serialVersionUID = 771164411205219615L;

    private JTable m_tablePanel;

    private DefaultTableModel m_keyValueTblModel;

    /**
     * Constructor for a JPanl representing and editable table of key-value pairs.
     *
     * @param keyLabel Label for the key column
     * @param valueLabel Label for the value column
     */
    public InteractiveKeyValueTable(final String keyLabel, final String valueLabel) {
        createLayout(keyLabel, valueLabel);
    }

    private void createLayout(final String keyLabel, final String valueLabel) {
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createLineBorder(Color.gray));

        var gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 3, 3, 3);
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridwidth = 2;

        m_tablePanel = new JTable();
        // stores the key-value pairs of the UI Component
        m_keyValueTblModel = new DefaultTableModel(0, 2);
        m_keyValueTblModel.setColumnIdentifiers(new String[]{keyLabel, valueLabel});
        m_tablePanel.setModel(m_keyValueTblModel);
        this.add(m_tablePanel, gbc);

        gbc.fill = GridBagConstraints.NONE;
        gbc.weighty = 0.0;
        gbc.weightx = 0.0;
        gbc.gridwidth = 1;
        gbc.gridy = 1;

        var addRowBtn = new JButton("Add");
        addRowBtn.addActionListener((e -> m_keyValueTblModel.addRow(new Object[]{"key", "value"})));
        this.add(addRowBtn, gbc);

        gbc.gridx = 1;
        var removeRowBtn = new JButton("Remove");
        removeRowBtn.addActionListener((final ActionEvent e) -> {
            ListSelectionModel sm = m_tablePanel.getSelectionModel();
            int min = sm.getMinSelectionIndex();
            int max = sm.getMaxSelectionIndex();
            if (sm.getMinSelectionIndex() != -1) {
                for (int i = max; i >= min; i--) {
                    m_keyValueTblModel.removeRow(i);
                }
            }
        });
        this.add(removeRowBtn, gbc);
    }

    /**
     * Retrieves the JPanels table entries as a HashMap.
     *
     * @return Table of key-value pairs
     */
    @SuppressWarnings("unchecked")
    public Map<String, String> getTable() {
        fireTableDataConfirm();
        HashMap<String, String> table = new HashMap<>();
        for (Vector<String> row : m_keyValueTblModel.getDataVector()) {
            table.put(row.elementAt(0), row.elementAt(1));
        }
        return table;
    }

    /**
     * Sets the JPanels table entries from a given HashMap.
     *
     * @param table Table of key-value pairs
     */
    public void setTable(final Map<String, String> table) {
        while (m_keyValueTblModel.getRowCount() > 0) {
            m_keyValueTblModel.removeRow(0);
        }
        for (Entry<String, String> row : table.entrySet()) {
            m_keyValueTblModel.addRow(new Object[]{row.getKey(), row.getValue()});
        }
    }

    /**
     * Confirms the data still being edited in the interactive table by firing an Enter key event on the component. The
     * "edit-mode" of the table cells is then exited and the values are written to the model.
     * Note: only firing {@link DefaultTableModel#fireTableDataChanged()} is not enough.
     */
    private void fireTableDataConfirm() {
        if (m_tablePanel != null) {
            var ke = new KeyEvent(m_tablePanel, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0,
                KeyEvent.VK_ENTER, KeyEvent.CHAR_UNDEFINED);
            m_tablePanel.dispatchEvent(ke);
            m_keyValueTblModel.fireTableDataChanged();
        }
    }
}
