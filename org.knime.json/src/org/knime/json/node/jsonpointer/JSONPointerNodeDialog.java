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
 *   14 Sept. 2014 (Gabor): created
 */
package org.knime.json.node.jsonpointer;

import java.awt.GridBagConstraints;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.json.node.util.GUIFactory;
import org.knime.json.node.util.PathOrPointerDialog;

import com.github.fge.jackson.jsonpointer.JsonPointer;
import com.github.fge.jackson.jsonpointer.JsonPointerException;

/**
 * <code>NodeDialog</code> for the "JSONPointer" Node. Selects certain pointers from the selected JSON column.
 *
 * @author Gabor Bakos
 */
public class JSONPointerNodeDialog extends PathOrPointerDialog<JSONPointerSettings> {
    private JTextField m_pointer;
    private JLabel m_error;

    /**
     * New pane for configuring the JSONPointer node.
     */
    protected JSONPointerNodeDialog() {
        super(JSONPointerNodeModel.createJSONPathProjectionSettings());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void afterNewColumnName(final JPanel panel, final int afterInput) {
        m_error = new JLabel();
        GridBagConstraints gbc = createInitialConstraints();
        gbc.gridy = afterInput;
        gbc.gridx = 0;
        panel.add(new JLabel("JSONPointer:"), gbc);
        gbc.gridx = 1;
        m_pointer = GUIFactory.createTextField("", 11);
        panel.add(m_pointer, gbc);
        gbc.gridy++;

        panel.add(m_error, gbc);
        gbc.gridy++;

        m_pointer.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void removeUpdate(final DocumentEvent e) {
                updateError();
            }

            @Override
            public void insertUpdate(final DocumentEvent e) {
                updateError();
            }

            @Override
            public void changedUpdate(final DocumentEvent e) {
                updateError();
            }
        });
        addOutputTypePanel(panel, gbc.gridy);
    }

    /**
     *
     */
    protected void updateError() {
        m_error.setText("");
        try {
            new JsonPointer(m_pointer.getText());
        } catch (JsonPointerException e) {
            m_error.setText(e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        super.loadSettingsFrom(settings, specs);
        m_pointer.setText(getSettings().getJsonPointer());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        try {
            new JsonPointer(m_pointer.getText());
        } catch (RuntimeException | JsonPointerException e) {
            throw new InvalidSettingsException("Wrong pointer: " + m_pointer.getText() + "\n" + e.getMessage(), e);
        }
        getSettings().setJsonPath(m_pointer.getText());
        super.saveSettingsTo(settings);
    }
}
