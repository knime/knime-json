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
 *   8 Nov. 2014 (Gabor): created
 */
package org.knime.json.node.util;

import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.EnumMap;
import java.util.Map.Entry;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.json.JSONValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;

/**
 * Dialog common parts for the JSONPath and JSONPointer nodes.
 *
 * @author Gabor Bakos
 * @param <S> The type of the implementation-specific {@link PathOrPointerSettings}.
 */
public abstract class PathOrPointerDialog<S extends PathOrPointerSettings> extends ReplaceOrAddColumnDialog<S> {
    private CardLayout m_cardLayout;
    private EnumMap<OutputType, JPanel> m_configPanels;
    private EnumMap<OutputType, ButtonModel> m_buttonModels;
    private JCheckBox m_hasDate, m_hasTime, m_hasMillis;
    private ButtonGroup m_typeGroup;

    /**
     * Constructs {@link PathOrPointerDialog}.
     * Using the first input table with a required {@link JSONValue} column.
     *
     * @param settings The implementation-specific settings.
     * @param inputColumnLabel The label for the input column.
     */
    protected PathOrPointerDialog(final S settings, final String inputColumnLabel) {
        super(settings, inputColumnLabel, 0, JSONValue.class);
    }
    /**
     * Same as {@link #PathOrPointerDialog(PathOrPointerSettings, String)} with the label: "JSON column".
     *
     * @param settings The implementation specific settings.
     * @see PathOrPointerDialog#PathOrPointerDialog(PathOrPointerSettings, String)
     */
    protected PathOrPointerDialog(final S settings) {
        this(settings, "JSON column");
    }

    /**
     * Adds the output and the output-specific controls to the {@code panel}.
     *
     * @param panel The (main) configuration panel.
     * @param gridy The current position with {@link GridBagConstraints}.
     * @return The new y position of {@link GridBagConstraints}.
     */
    protected int addOutputTypePanel(final JPanel panel, final int gridy) {
        m_buttonModels = new EnumMap<>(OutputType.class);
        m_configPanels = new EnumMap<>(OutputType.class);
        final GridBagConstraints gbc = createInitialConstraints();
        gbc.gridy = gridy;
        final JPanel typePanel = new JPanel(new GridBagLayout());
        typePanel.setName("Type selector and related configs");
        final JPanel subPanel = new JPanel();
        subPanel.setLayout(new BoxLayout(subPanel, BoxLayout.PAGE_AXIS));
        subPanel.setName("Just type selector");
        m_typeGroup = new ButtonGroup();
        m_cardLayout = new CardLayout();
        final JPanel configPanel = new JPanel(m_cardLayout );
        configPanel.setName("Type-related configs");

        gbc.gridwidth = 2;
        typePanel.setBorder(new TitledBorder("Result type"));
        GridBagConstraints gbcType = createInitialConstraints();
        typePanel.add(subPanel, gbcType);
        gbcType.gridy = 1;
        typePanel.add(configPanel, gbcType);
        for (final OutputType type : OutputType.values()) {
            final JRadioButton radioButton = new JRadioButton(type.getStringValue());
            radioButton.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(final ChangeEvent e) {
                    if (radioButton.isSelected()) {
                        m_cardLayout.show(configPanel, type.name());
                        outputTypeGotSelected(type);
                    }
                }
            });
            m_typeGroup.add(radioButton);
            m_buttonModels.put(type, radioButton.getModel());
            subPanel.add(radioButton);
            final JPanel specConfig = createConfigPanel(type);
            m_configPanels.put(type, specConfig);
            configPanel.add(specConfig, type.name());
        }
        panel.add(typePanel, gbc);
        gbc.gridy++;
        gbc.gridwidth = 1;

        return gbc.gridy;
    }
    /**
     * On selection of the {@code type} radiobutton, this method gets called.
     *
     * @param type The selected {@link OutputType}.
     */
    protected void outputTypeGotSelected(final OutputType type) {
        //Do nothing by default
    }

    /**
     * @param type The {@link OutputType} to create the config panel.
     * @return The create config panel.
     */
    protected JPanel createConfigPanel(final OutputType type) {
        m_hasDate = new JCheckBox("Keep date (year, month, day) part");
        m_hasTime = new JCheckBox("Keep time part (hours, minutes, seconds)");
        m_hasMillis = new JCheckBox("Keep the milliseconds");
        JPanel ret = new JPanel();
        ret.setName(type.name());
        switch (type) {
            case DateTime:
                ret.setLayout(new FlowLayout(FlowLayout.LEADING));
                ret.add(m_hasDate);
                ret.add(m_hasTime);
                ret.add(m_hasMillis);
                break;
            default:
                //No other requires further adjustments.
                break;
        }
        return ret;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        getSettings().setHasDate(m_hasDate.isSelected());
        getSettings().setHasTime(m_hasTime.isSelected());
        getSettings().setHasMillis(m_hasMillis.isSelected());
        OutputType outputType = null;
        for (Entry<OutputType, ButtonModel> entry : m_buttonModels.entrySet()) {
            if (entry.getValue().isSelected()) {
                outputType = entry.getKey();
            }
        }
        if (outputType == null) {
            throw new InvalidSettingsException("No result type was selected! Please specify");
        }
        getSettings().setReturnType(outputType);
        super.saveSettingsTo(settings);
    }

    protected final ButtonModel getOutputTypeButtonModel(final OutputType type) {
        return m_buttonModels.get(type);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs) throws NotConfigurableException {
        super.loadSettingsFrom(settings, specs);
        m_buttonModels.get(getSettings().getReturnType()).setSelected(true);
        m_hasDate.setSelected(getSettings().isHasDate());
        m_hasTime.setSelected(getSettings().isHasTime());
        m_hasMillis.setSelected(getSettings().isHasMillis());
    }
}
