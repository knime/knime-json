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
 *   15 Nov. 2014 (Gabor): created
 */
package org.knime.json.aggregation;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.knime.base.data.aggregation.AggregationOperator;

/**
 * Settings panel for the {@link ToJson} {@link AggregationOperator}.
 *
 * @author Gabor Bakos
 */
@SuppressWarnings("serial")
final class ToJsonSettingsPanel extends JPanel {
    private final JCheckBox m_idAsKey = new JCheckBox("Id as key"), m_columnNameAsKey = new JCheckBox("Column name as key"), m_singleOutput = new JCheckBox("Single output");
    private static final String[] DATE_FORMATS = new String[7];
    static {
        DATE_FORMATS[0] = new SimpleDateFormat().toPattern();
        DATE_FORMATS[1] = pattern(DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL));
        DATE_FORMATS[2] = pattern(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT));
        DATE_FORMATS[3] = pattern(DateFormat.getDateInstance(DateFormat.FULL));
        DATE_FORMATS[4] = pattern(DateFormat.getDateInstance(DateFormat.SHORT));
        DATE_FORMATS[5] = pattern(DateFormat.getTimeInstance(DateFormat.FULL));
        DATE_FORMATS[6] = pattern(DateFormat.getTimeInstance(DateFormat.SHORT));
    }
    private static final String[] LOCALES = new String[7];
    static {
        LOCALES[0] = Locale.getDefault().getLanguage();
        LOCALES[1] = Locale.ENGLISH.getLanguage();
        LOCALES[2] = Locale.GERMAN.getLanguage();
        LOCALES[3] = Locale.CHINESE.getLanguage();
        LOCALES[4] = Locale.FRENCH.getLanguage();
        LOCALES[5] = Locale.ITALIAN.getLanguage();
        LOCALES[6] = Locale.JAPANESE.getLanguage();
    }
    private final JComboBox<String> m_dateFormat = new JComboBox<>(), m_dateLocaleKey = new JComboBox<>(LOCALES);

    /**
     * Constructs the panel.
     */
    public ToJsonSettingsPanel() {
        super(new GridBagLayout(), true);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        add(m_idAsKey, gbc);
        gbc.gridx = 1;
        add(m_singleOutput, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        add(m_columnNameAsKey, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        add(new JLabel("Date format:"), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        add(m_dateFormat, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        add(new JLabel("Date locale:"), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        add(m_dateLocaleKey, gbc);

        m_dateFormat.setEditable(true);
        m_dateLocaleKey.setEditable(true);
    }

    private static String pattern(final DateFormat dateTimeInstance) {
        if (dateTimeInstance instanceof SimpleDateFormat) {
            SimpleDateFormat s = (SimpleDateFormat)dateTimeInstance;
            return s.toPattern();
        }
        return DATE_FORMATS[0];
    }

    /**
     * @return the idAsKey
     */
    final boolean getIdAsKey() {
        return m_idAsKey.isSelected();
    }

    /**
     * @return the singleOutput
     */
    final boolean getSingleOutput() {
        return m_singleOutput.isSelected();
    }

    /**
     * @return the dateFormat
     */
    final String getDateFormat() {
        return (String)m_dateFormat.getSelectedItem();
    }

    /**
     * @return the dateLocaleKey
     */
    final String getDateLocaleKey() {
        return (String)m_dateLocaleKey.getSelectedItem();
    }

    /**
     * @param dateFormat the dateFormat to set
     */
    final void setDateFormat(final String dateFormat) {
        m_dateFormat.setSelectedItem(dateFormat);
    }

    /**
     * @param locale the dateLocale to set
     */
    final void setDateLocale(final String locale) {
        m_dateLocaleKey.setSelectedItem(locale);
    }

    /**
     * @param idAsKey the idAsKey to set
     */
    final void setIdAsKey(final boolean idAsKey) {
        m_idAsKey.setSelected(idAsKey);
    }

    /**
     * @param singleOutput the singleOutput to set
     */
    final void setSingleOutput(final boolean singleOutput) {
        m_singleOutput.setSelected(singleOutput);
    }

    /**
     * @return the colNameAsKey
     */
    final boolean getColNameAsKey() {
        return m_columnNameAsKey.isSelected();
    }

    /**
     * @param colNameAsKey the colNameAsKey to set
     */
    final void setColNameAsKey(final boolean colNameAsKey) {
        m_columnNameAsKey.setSelected(colNameAsKey);
    }
}
