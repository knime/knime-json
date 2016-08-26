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
 *   24 Sept. 2014 (Gabor): created
 */
package org.knime.json.node.patch.apply;

import java.awt.GridBagConstraints;
import java.util.Vector;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.json.JSONValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.json.node.util.RemoveOrAddColumnDialog;

/**
 * <code>NodeDialog</code> for the "JSONTransformer" Node. Changes JSON values.
 *
 * @author Gabor Bakos
 */
public final class JSONPatchApplyNodeDialog extends RemoveOrAddColumnDialog<JSONPatchApplySettings> {
    private JComboBox<String> m_patchType;

    private JsonPatchMainPanel m_mainControl;

    private JCheckBox m_keepOriginal;

    /**
     * New pane for configuring the JSONTransformer node.
     */
    protected JSONPatchApplyNodeDialog() {
        super(JSONPatchApplyNodeModel.createJSONPatchApplySetting(), "JSON column", JSONValue.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void afterNewColumnName(final JPanel panel, final int afterNewCol) {
        GridBagConstraints gbc = createInitialConstraints();
        gbc.gridx = 0;
        gbc.gridy = afterNewCol;
        panel.add(new JLabel("Patch type:"), gbc);
        gbc.gridx = 1;
        m_patchType = new JComboBox<>(new Vector<>(JSONPatchApplySettings.PATCH_TYPES));
        panel.add(m_patchType, gbc);
        gbc.gridy++;
        gbc.weighty = 0;
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        m_keepOriginal = new JCheckBox("Keep original value when 'test' operation fails");
        panel.add(m_keepOriginal, gbc);
        gbc.gridy++;

        gbc.gridx = 0;
        gbc.weighty = 0;
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        final JLabel patchLabel = new JLabel("Patch:");
        patchLabel.setVerticalAlignment(SwingConstants.TOP);
        panel.add(patchLabel, gbc);

        gbc.weighty = 1;
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        m_mainControl = new JsonPatchMainPanel();
        panel.add(m_mainControl, gbc);

        m_keepOriginal.setSelected(JSONPatchApplySettings.DEFAULT_KEEP_ORIGINAL_WHEN_TEST_FAILS);
        m_patchType.addActionListener(
            e -> m_keepOriginal.setEnabled(JSONPatchApplySettings.PATCH_OPTION.equals(m_patchType.getSelectedItem())));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        super.loadSettingsFrom(settings, specs);
        m_patchType.setSelectedItem(getSettings().getPatchType());
        m_mainControl.update(getSettings().getJsonPatch(), (DataTableSpec)specs[0], getAvailableFlowVariables());
        m_keepOriginal.setSelected(getSettings().isKeepOriginalWhenTestFails());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        getSettings().setPatchType((String)m_patchType.getSelectedItem());
        getSettings().setJsonPatch(m_mainControl.getExpression());
        getSettings().setKeepOriginalWhenTestFails(m_keepOriginal.isSelected());
        super.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean closeOnESC() {
        //@see org.knime.base.node.jsnippet.JavaSnippetNodeDialog.closeOnESC()
        return false;
    }
}
