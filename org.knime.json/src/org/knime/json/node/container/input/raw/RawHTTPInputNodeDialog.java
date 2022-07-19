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
 *   Created on Feb 15, 2015 by wiswedel
 */
package org.knime.json.node.container.input.raw;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;

/**
 * <code>NodeDialog</code> for the "Container Input (Raw HTTP)" Node.
 *
 * @author Alexander Fillbrunn, KNIME GmbH, Konstanz, Germany
 */
final class RawHTTPInputNodeDialog extends NodeDialogPane {

    // Text input for base64-encoded binary data
    private final RSyntaxTextArea m_body;

    // Table header key-value pairs
    private final KeyValueTable m_headerTable;

    // Table for query parameter key-value pairs
    private final KeyValueTable m_qpTable;

    /**
     * New pane for configuring the Container Input (Raw HTTP) node.
     */
    protected RawHTTPInputNodeDialog() {

        m_body = new RSyntaxTextArea();
        m_body.setPreferredSize(new Dimension(0, 150));
        m_body.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);

        m_headerTable = new KeyValueTable("Name", "Value");
        m_qpTable = new KeyValueTable("Name", "Value");

        addTab("Data", createLayout(), false);
    }

    private JPanel createLayout() {
        var p = new JPanel(new GridBagLayout());
        var gbc = new GridBagConstraints();

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(10, 5, 0, 5);

        gbc.weightx = 0.0;
        p.add(new JLabel("Body (base64 encoded): "), gbc);

        gbc.gridy++;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        p.add(new RTextScrollPane(m_body, true), gbc);

        gbc.gridy++;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        p.add(new JLabel("Expected Headers: "), gbc);

        gbc.gridy++;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        m_headerTable.setPreferredSize(new Dimension(0, 150));
        p.add(m_headerTable, gbc);

        gbc.gridy++;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        p.add(new JLabel("Expected Query Parameters: "), gbc);

        gbc.gridy++;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        m_qpTable.setPreferredSize(new Dimension(0, 150));
        p.add(m_qpTable, gbc);

        return p;
    }

    /** {@inheritDoc} */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        var config = new RawHTTPInputNodeConfiguration();
        config.setBody(m_body.getText());
        config.setHeaders(m_headerTable.getTable());
        config.setQueryParams(m_qpTable.getTable());
        config.save(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
        throws NotConfigurableException {
        var config = new RawHTTPInputNodeConfiguration().loadInDialog(settings);
        m_body.setText(config.getBody());
        m_headerTable.setTable(config.getHeaders());
        m_qpTable.setTable(config.getQueryParams());
    }
}
