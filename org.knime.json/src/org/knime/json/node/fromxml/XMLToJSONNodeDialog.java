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
package org.knime.json.node.fromxml;

import java.awt.GridBagConstraints;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.knime.core.data.xml.XMLValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.json.node.util.GUIFactory;
import org.knime.json.node.util.ReplaceColumnDialog;

/**
 * <code>NodeDialog</code> for the "XMLToJSON" Node. Converts XML values to JSON values.
 *
 * @author Gabor Bakos
 */
public class XMLToJSONNodeDialog extends ReplaceColumnDialog<XMLToJSONSettings> {

    private JTextField m_textKey;

    private JCheckBox m_translateComments;

    private JCheckBox m_translateProcessingInstructions;

    /**
     * New pane for configuring the XMLToJSON node.
     */
    protected XMLToJSONNodeDialog() {
        super(XMLToJSONNodeModel.createXMLToSJONSettings(), "XML column:", XMLValue.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void afterNewColumnName(final JPanel panel, final int afterInput) {
        GridBagConstraints gbc = createInitialConstraints();
        gbc.gridx = 0;
        gbc.gridy = afterInput;
        panel.add(new JLabel("Text body translated to JSON with key: "), gbc);
        gbc.gridx = 1;
        m_textKey = GUIFactory.createTextField("", 22);
        panel.add(m_textKey, gbc);
        gbc.gridy++;
        m_translateComments = new JCheckBox("Translate comments");
        gbc.gridx = 0;
        panel.add(m_translateComments, gbc);
        gbc.gridy++;
        m_translateProcessingInstructions = new JCheckBox("Translate processing instructions");
        panel.add(m_translateProcessingInstructions, gbc);
        gbc.gridy++;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        getSettings().setTextKey(m_textKey.getText());
        getSettings().setTranslateComments(m_translateComments.isSelected());
        getSettings().setTranslateProcessingInstructions(m_translateProcessingInstructions.isSelected());
        super.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        super.loadSettingsFrom(settings, specs);
        m_textKey.setText(getSettings().getTextKey());
        m_translateComments.setSelected(getSettings().isTranslateComments());
        m_translateProcessingInstructions.setSelected(getSettings().isTranslateProcessingInstructions());
    }
}
