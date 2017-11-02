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
 *   20 Dec 2014 (Gabor): created
 */
package org.knime.json.node.combine.row;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.json.JSONValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.util.ColumnSelectionComboxBox;
import org.knime.core.node.util.ColumnSelectionPanel;
import org.knime.core.node.util.KeyValuePanel;
import org.knime.json.node.combine.row.RowCombineSettings.ObjectOrArray;
import org.knime.json.node.util.GUIFactory;
import org.knime.json.util.StringCellFilter;

/**
 * Common base class for the row combining JSON node dialogs.
 *
 * @author Gabor Bakos
 * @param <Settings> The node specific {@link RowCombineSettings} type.
 */
public abstract class RowCombineDialog<Settings extends RowCombineSettings> extends NodeDialogPane {

    private ColumnSelectionComboxBox m_inputColumn;

    private JTextField m_rootKey;

    private KeyValuePanel m_keyValuePanel;

    private JRadioButton m_addRoot, m_omitRoot, m_collectToArray, m_collectToObject;

    private ColumnSelectionPanel m_objectKeyColumn;

    /**
     *
     */
    public RowCombineDialog() {
        super();
        m_collectToArray = new JRadioButton("Collect into array");
        m_collectToObject = new JRadioButton("Collect into object with key:");
        ButtonGroup collectGroup = new ButtonGroup();
        collectGroup.add(m_collectToArray);
        collectGroup.add(m_collectToObject);
        m_objectKeyColumn = new ColumnSelectionPanel(null, new StringCellFilter(), false, true);
        ActionListener collectListener = new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                m_objectKeyColumn.setEnabled(m_collectToObject.isSelected());
            }
        };
        m_objectKeyColumn.setEnabled(false);
        m_collectToArray.getModel().addActionListener(collectListener);
        m_collectToObject.getModel().addActionListener(collectListener);
        m_collectToArray.setSelected(true);
        JPanel settings = createSettingsPanel();
        settings.setPreferredSize(new Dimension(600, 400));
        addTab("Settings", settings);

    }

    /**
     * @return The "Settings" panel.
     */
    private JPanel createSettingsPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(4, 4, 4, 4);
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.weightx = 0;
        c.weighty = 0;

        p.add(new JLabel("JSON column:"), c);
        c.gridx++;
        c.weightx = 1;
        @SuppressWarnings("unchecked")
        final ColumnSelectionComboxBox inputColumn = new ColumnSelectionComboxBox(JSONValue.class);
        m_inputColumn = inputColumn;
        m_inputColumn.setBorder(null);
        p.add(m_inputColumn, c);

        createOutput(p, c);

        JPanel rootPanel = new JPanel(new GridBagLayout()), collectPanel = new JPanel(new GridBagLayout());
        GridBagConstraints cr = new GridBagConstraints(), cc = new GridBagConstraints();
        cr.anchor = GridBagConstraints.LINE_START;
        cr.gridx = 0;
        cr.gridy = 0;
        cr.weightx = 1;
        cr.gridwidth = 2;
        m_omitRoot = new JRadioButton("Omit root");
        rootPanel.add(m_omitRoot, cr);
        cr.gridy++;
        cr.weightx = 0;
        cr.gridwidth = 1;
        m_addRoot = new JRadioButton("Add root object with key:");
        cr.fill = GridBagConstraints.HORIZONTAL;
        rootPanel.add(m_addRoot, cr);
        ButtonGroup bg = new ButtonGroup();
        bg.add(m_omitRoot);
        bg.add(m_addRoot);
        //        c.gridy++;
        //        c.weightx = 0;
        //        p.add(new JLabel("JSON key:"), c);
        cr.gridx++;
        cr.weightx = 1;
        m_rootKey = GUIFactory.createTextField("root", 22);
        rootPanel.add(m_rootKey, cr);
        cr.gridx = 0;
        cr.gridwidth = 2;
        cr.gridy++;
        cr.fill = GridBagConstraints.BOTH;
        cr.weighty = 1;
        m_keyValuePanel = new KeyValuePanel();
        m_keyValuePanel.getTable().setPreferredScrollableViewportSize(null);
        m_keyValuePanel.setKeyColumnLabel("Key");
        m_keyValuePanel.setValueColumnLabel("Value");
        m_keyValuePanel.setBorder(BorderFactory.createTitledBorder("Custom key/value pairs"));
        rootPanel.add(m_keyValuePanel, cr);

        cc.anchor = GridBagConstraints.LINE_START;
        cc.gridx = 0;
        cc.gridy = 0;
        cc.gridwidth = 2;
        cc.weightx = 0;

        collectPanel.add(m_collectToArray, cc);
        cc.gridy++;
        cc.gridwidth = 1;
        collectPanel.add(m_collectToObject, cc);
        cc.gridx++;
        cc.weightx = 1;
        collectPanel.add(m_objectKeyColumn, cc);

        rootPanel.setBorder(new TitledBorder("Root"));
        collectPanel.setBorder(new TitledBorder("Object or array"));

        c.gridx = 0;
        c.gridwidth = 2;
        c.gridy++;
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        p.add(rootPanel, c);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weighty = 0;
        c.gridy++;
        p.add(collectPanel, c);

        m_addRoot.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent e) {
                m_rootKey.setEnabled(m_addRoot.isSelected());
                m_keyValuePanel.setEnabled(m_addRoot.isSelected());
            }
        });
        m_addRoot.setSelected(RowCombineSettings.DEFAULT_ADD_ROOT_KEY);
        return p;
    }

    /**
     * Creates the controls for the output in the dialog.
     *
     * @param p A {@link JPanel}.
     * @param c The previous {@link GridBagConstraints}. (Update it to point to the (visually) last component added.)
     */
    protected abstract void createOutput(JPanel p, GridBagConstraints c);

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        Settings s = initSettings();
        s.setInputColumn(m_inputColumn.getSelectedColumn());
        saveOutput(s);
        s.setAddRootKey(m_addRoot.isSelected());
        s.setRootKey(m_rootKey.getText());
        s.setKeys(m_keyValuePanel.getKeys());
        s.setValues(m_keyValuePanel.getValues());
        s.setObjectOrArray(m_collectToArray.isSelected() ? ObjectOrArray.Array : m_collectToObject.isSelected()
            ? ObjectOrArray.Object : CheckUtils.checkSettingNotNull((ObjectOrArray)null,
                "Nor object nor array was selected."));
        s.setObjectKeyColumn(m_objectKeyColumn.getSelectedColumn());
        s.setObjectKeyIsRowID(m_objectKeyColumn.rowIDSelected());
        s.saveSettings(settings);
    }

    /**
     * Saves the internal output related settings to the node-specific {@code s}.
     *
     * @param s The {@code <Settings>} object to save the settings from the dialog.
     * @see #saveSettingsTo(NodeSettingsWO)
     */
    protected abstract void saveOutput(Settings s);

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
        throws NotConfigurableException {
        Settings s = initSettings();
        s.loadSettingsDialog(settings, null);
        m_inputColumn.update(specs[0], s.getInputColumn());
        loadOutput(s, specs[0]);
        m_addRoot.setSelected(s.isAddRootKey());
        m_omitRoot.setSelected(!s.isAddRootKey());
        m_rootKey.setText(s.getRootKey());
        m_keyValuePanel.setTableData(s.getKeys(), s.getValues());
        switch (s.getObjectOrArray()) {
            case Array:
                m_collectToArray.setSelected(true);
                m_objectKeyColumn.setEnabled(false);
                break;
            case Object:
                m_collectToObject.setSelected(true);
                m_objectKeyColumn.setEnabled(true);
               break;
            default:
                CheckUtils.checkState(false, "Unknown object or array option: " + s.getObjectOrArray());
        }
        m_objectKeyColumn.update(specs[0], s.getObjectKeyColumn(), s.isObjectKeyIsRowID(), false);
    }

    /**
     * @return a new {@code <Settings>}.
     */
    protected abstract Settings initSettings();

    /**
     * Loads the settings to the dialog from the internal represention of {@code s}.
     *
     * @param s The {@code <Settings>} object.
     * @param dataTableSpec The {@link DataTableSpec}.
     * @see #loadSettingsFrom(NodeSettingsRO, DataTableSpec[])
     */
    protected abstract void loadOutput(Settings s, DataTableSpec dataTableSpec);

}