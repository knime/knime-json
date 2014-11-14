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
 *   14 Nov. 2014 (Gabor): created
 */
package org.knime.json.node.util;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.json.JSONValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.ColumnSelectionComboxBox;

/**
 * A dialog with option to replace the original column (not just possibly remove and add).
 *
 * @author Gabor Bakos
 * @param <S> The actual type of the {@link ReplaceColumnSettings} object.
 */
public class ReplaceColumnDialog<S extends ReplaceColumnSettings> extends RemoveOrAddColumnDialog<S> {
    private JRadioButton m_replaceButton;
    private JRadioButton m_addButton;
    private ButtonGroup m_buttonGroup;

    /**
     * @param settings
     * @param inputColumnLabel
     */
    public ReplaceColumnDialog(final S settings, final String inputColumnLabel) {
        this(settings, inputColumnLabel, settings.getInputColumnType());
    }
    /**
     * @param settings
     * @param inputColumnLabel
     * @param inputValueClass
     */
    public ReplaceColumnDialog(final S settings, final String inputColumnLabel, final Class<? extends DataValue> inputValueClass) {
        this(settings, inputColumnLabel, 0, inputValueClass);
    }
    /**
     * @param settings
     * @param inputColumnLabel
     * @param inputTable
     * @param inputValueClass
     */
    public ReplaceColumnDialog(final S settings, final String inputColumnLabel, final int inputTable,
        final Class<? extends DataValue> inputValueClass) {
        super(settings, inputColumnLabel, inputTable, inputValueClass);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void addInputColumn(final String inputColumnLabel, final Class<? extends DataValue> inputValueClass, final JPanel panel,
        final GridBagConstraints gbc, final int gridY) {
        m_buttonGroup = new ButtonGroup();
        gbc.gridy = gridY;
        m_replaceButton = new JRadioButton("Replace/" + inputColumnLabel);
        m_buttonGroup.add(m_replaceButton);
        panel.add(m_replaceButton, gbc);
        gbc.gridx++;
        gbc.weightx = 1;
        {
            @SuppressWarnings("unchecked")
            ColumnSelectionComboxBox columnSelectionComboxBox = new ColumnSelectionComboxBox(inputValueClass);
            setInputColumn(columnSelectionComboxBox);
            getInputColumn().addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    onSelectedInputColumnChanged(getInputColumn());
                }
            });
        }
        getInputColumn().setBorder(null);
        panel.add(getInputColumn(), gbc);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void addRemoveAndAddNewColumn(final JPanel panel, final GridBagConstraints gbc) {
        gbc.gridx = 0;
        m_addButton = new JRadioButton("Append");
        panel.add(m_addButton, gbc);
        m_buttonGroup.add(m_addButton);
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
        final JPanel newColumnPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
        newColumnPanel.add(new JLabel(getOutputIcon()));
        newColumnPanel.add(getNewColumnName());
        panel.add(newColumnPanel, gbc);
        if (getSettings().isRemoveInputColumn() && (getSettings().getNewColumnName() == null || getSettings().getNewColumnName().isEmpty())) {
            getNewColumnName().setText(getInputColumn().getSelectedColumn());
        }
        gbc.gridy++;

        m_replaceButton.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent e) {
                getNewColumnName().setEnabled(!m_replaceButton.isSelected());
            }
        });
    }

    /**
     * @return The icon for the output type.
     */
    protected Icon getOutputIcon() {
        return DataType.getUtilityFor(JSONValue.class).getIcon();
    }
    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs) throws NotConfigurableException {
        super.loadSettingsFrom(settings, specs);
        if (getSettings().isRemoveInputColumn()) {
            m_replaceButton.setSelected(true);
        } else {
            m_addButton.setSelected(true);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
//        getSettings().setReplaceColumn(m_replaceButton.isSelected());
        m_removeSourceColumn.setSelected(m_replaceButton.isSelected());
        super.saveSettingsTo(settings);
    }
}
