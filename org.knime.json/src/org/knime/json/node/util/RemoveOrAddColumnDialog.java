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
 *   28 Sept 2014 (Gabor): created
 */
package org.knime.json.node.util;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.ColumnSelectionComboxBox;

/**
 * The common superclass for the nodes that can have an input column that can be replaced by an output, or a new custom
 * output could be specified.
 *
 * @author Gabor Bakos
 * @param <S> The type of {@link RemoveOrAddColumnSettings} to set.
 */
public class RemoveOrAddColumnDialog<S extends RemoveOrAddColumnSettings> extends NodeDialogPane {
    private S m_settings;

    private ColumnSelectionComboxBox m_inputColumn;

    private final JTextField m_newColumnName = GUIFactory.createTextField("", 22);

    private final JCheckBox m_removeSourceColumn = new JCheckBox("Remove source column");

    private final int m_inputTable;

    /**
     * Constructs the dialog for the first input table with {@link S#getInputColumnType() the data value type} from the
     * {@code settings} object..
     *
     * @param settings The node specific settings.
     * @param inputColumnLabel The label of the input column.
     */
    public RemoveOrAddColumnDialog(final S settings, final String inputColumnLabel) {
        this(settings, inputColumnLabel, 0, settings.getInputColumnType());
    }

    /**
     * Constructs the dialog for the first input table.
     *
     * @param settings The node specific settings.
     * @param inputColumnLabel The label of the input column.
     * @param inputValueClass The possible input value class.
     */
    public RemoveOrAddColumnDialog(final S settings, final String inputColumnLabel,
        final Class<? extends DataValue> inputValueClass) {
        this(settings, inputColumnLabel, 0, inputValueClass);
    }

    /**
     * Constructs the dialog.
     *
     * @param settings The node specific settings.
     * @param inputColumnLabel The label of the input column.
     * @param inputTable The {@code 0}-based index of the input table.
     * @param inputValueClass The possible input value class.
     */
    public RemoveOrAddColumnDialog(final S settings, final String inputColumnLabel, final int inputTable,
        final Class<? extends DataValue> inputValueClass) {
        m_settings = settings;
        this.m_inputTable = inputTable;
        JPanel panel = new JPanel(new GridBagLayout());
        addTab("Settings", panel);
        addContentsToSettings(inputColumnLabel, inputValueClass, panel);
    }

    /**
     * This method fills the contents of the "Settings" panel.
     *
     * @param inputColumnLabel The suggested label for the input column.
     * @param inputValueClass The type of the input column.
     * @param panel The "Settings" panel.
     */
    protected void addContentsToSettings(final String inputColumnLabel,
        final Class<? extends DataValue> inputValueClass, final JPanel panel) {
        GridBagConstraints gbc = createInitialConstraints();
        int gridY = addBeforeInputColumn(panel);
        addInputColumn(inputColumnLabel, inputValueClass, panel, gbc, gridY);
        gridY = addAfterInputColumn(panel, gbc.gridy + 1);
        gbc.gridy = gridY;
        addRemoveAndAddNewColumn(panel, gbc);
        afterNewColumnName(panel, gbc.gridy);
    }

    /**
     * Adds the input column control to {@code panel}. As a side effect it might change the {@code gbc}.
     *
     * @param inputColumnLabel The suggested input column label.
     * @param inputValueClass The type of the expected column.
     * @param panel The panel to use.
     * @param gbc The {@link GridBagConstraints} to use.
     * @param gridY The suggested gridy value of {@code gbc}.
     */
    protected void addInputColumn(final String inputColumnLabel, final Class<? extends DataValue> inputValueClass,
        final JPanel panel, final GridBagConstraints gbc, final int gridY) {
        gbc.gridy = gridY;
        panel.add(new JLabel(inputColumnLabel), gbc);
        gbc.gridx++;
        gbc.weightx = 1;
        {
            @SuppressWarnings("unchecked")
            ColumnSelectionComboxBox columnSelectionComboxBox = new ColumnSelectionComboxBox(inputValueClass);
            m_inputColumn = columnSelectionComboxBox;
            m_inputColumn.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    onSelectedInputColumnChanged(m_inputColumn);
                }
            });
        }
        m_inputColumn.setBorder(null);
        panel.add(m_inputColumn, gbc);
    }

    /**
     * Adds the remove original and the new column controls to {@code panel}
     *
     * @param panel A {@link JPanel} with {@link GridBagConstraints}.
     * @param gbc The suggested {@link GridBagConstraints}.
     */
    protected void addRemoveAndAddNewColumn(final JPanel panel, final GridBagConstraints gbc) {
        getRemoveSourceColumn().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                onRemoveSourceColumnChanged(getRemoveSourceColumn());
            }
        });
        panel.add(getRemoveSourceColumn(), gbc);
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 0;
        panel.add(new JLabel("New column"), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 1;
        getNewColumnName().getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(final DocumentEvent e) {
                onNewColumnTextChanged(getNewColumnName());
            }

            @Override
            public void removeUpdate(final DocumentEvent e) {
                onNewColumnTextChanged(getNewColumnName());
            }

            @Override
            public void changedUpdate(final DocumentEvent e) {
                onNewColumnTextChanged(getNewColumnName());
            }
        });
        JPanel newNamePanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
        newNamePanel.add(new JLabel(DataType.getUtilityFor(m_settings.getInputColumnType()).getIcon()));
        newNamePanel.add(getNewColumnName());
        panel.add(newNamePanel, gbc);
        gbc.gridy++;
    }

    /**
     * This method is called when we have added the last control for the new column name. <br/>
     * Override it if you want to add other controls to the settings tab.
     *
     * @param panel The {@link JPanel} for the settings tab.
     * @param afterNewColumn This is the {@link GridBagConstraints#gridy} value for the next control.
     */
    protected void afterNewColumnName(final JPanel panel, final int afterNewColumn) {
    }

    /**
     * @param panel The panel of the settings panel. <b>Do not remove components!</b>
     * @param afterInput The gridy value for the next control after input column control.
     * @return The next gridy value to use after the custom controls after input column.
     */
    protected int addAfterInputColumn(final JPanel panel, final int afterInput) {
        return afterInput;
    }

    /**
     * @param panel The panel of the settings panel. <b>Do not remove components!</b>
     * @return The next gridy value to use for the input column.
     */
    protected int addBeforeInputColumn(final JPanel panel) {
        return 0;
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
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_settings.setInputColumnName(m_inputColumn.getSelectedColumn());
        if (m_inputColumn.getSelectedColumn().isEmpty()) {
            throw new InvalidSettingsException("No input column selected!");
        }
        m_settings.setRemoveInputColumn(getRemoveSourceColumn().isSelected());
        m_settings.setNewColumnName(getNewColumnName().getText());
        if (getNewColumnName().getText().trim().isEmpty()) {
            throw new InvalidSettingsException("No name specified for the new column.");
        }
        m_settings.saveSettingsTo(settings);
    }

    /**
     * @return the settings
     */
    public S getSettings() {
        return m_settings;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        m_settings.loadSettingsForDialogs(settings, specs);
        m_inputColumn.setSelectedColumn(m_settings.getInputColumnName());
        m_inputColumn.update((DataTableSpec)specs[m_inputTable], m_settings.getInputColumnName());
        getRemoveSourceColumn().setSelected(m_settings.isRemoveInputColumn());
        getNewColumnName().setText(m_settings.getNewColumnName());
        notifyListeners();
    }

    /**
     *
     */
    private void notifyListeners() {
        for (ActionListener listener : m_inputColumn.getActionListeners()) {
            listener.actionPerformed(null);
        }
        for (ActionListener listener : getRemoveSourceColumn().getActionListeners()) {
            listener.actionPerformed(null);
        }
        for (ActionListener listener : getNewColumnName().getActionListeners()) {
            listener.actionPerformed(null);
        }
    }

    /**
     * This method gets called when the input column selection is changed (or loaded with the same value).
     *
     * @param combobox The combobox of the input column.
     */
    protected void onSelectedInputColumnChanged(final ColumnSelectionComboxBox combobox) {

    }

    /**
     * This method gets called when the output changes between remove or not (or the model get loaded).
     *
     * @param checkbox The {@link JCheckBox} used to set the remove source column.
     */
    protected void onRemoveSourceColumnChanged(final JCheckBox checkbox) {

    }

    /**
     * This method gets called when the text of the new column textfield changes (or get loaded).
     *
     * @param textfield The new column name {@link JTextField}.
     */
    protected void onNewColumnTextChanged(final JTextField textfield) {

    }

    /**
     * @return the newColumnName
     */
    protected JTextField getNewColumnName() {
        return m_newColumnName;
    }

    /**
     * @return the inputColumn
     */
    protected ColumnSelectionComboxBox getInputColumn() {
        return m_inputColumn;
    }

    /**
     * @param inputColumn the inputColumn to set
     */
    protected void setInputColumn(final ColumnSelectionComboxBox inputColumn) {
        this.m_inputColumn = inputColumn;
    }

    /**
     * @return the removeSourceColumn
     */
    protected JCheckBox getRemoveSourceColumn() {
        return m_removeSourceColumn;
    }
}
