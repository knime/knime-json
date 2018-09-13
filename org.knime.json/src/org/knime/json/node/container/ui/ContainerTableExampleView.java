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
 *   Sep 12, 2018 (Tobias Urhaug, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.json.node.container.ui;

import java.awt.BorderLayout;

import javax.json.JsonValue;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;

import org.knime.base.node.io.filereader.PreviewTableContentView;
import org.knime.core.data.DataTable;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.tableview.TableView;
import org.knime.json.node.container.mappers.ContainerTableMapper;

/**
 * A view that displays an example table and holds an internal template table that can be set as a
 * new example by clicking a button.
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 */
public final class ContainerTableExampleView extends JPanel {

    private static final long serialVersionUID = 6012599572331463639L;

    private TableView m_exampleTableView;
    private JButton m_createExampleButton;
    private BufferedDataTable m_templateTable;
    private JsonValue m_exampleTableJson;

    /**
     * Constructs a new table example view.
     * @param borderTitle the title of the border
     */
    public ContainerTableExampleView(final String borderTitle) {
        m_createExampleButton = new JButton("Create example based on input table");
        m_createExampleButton.addActionListener(e -> createExampleFromTemplateTable());

        PreviewTableContentView ptcv = new PreviewTableContentView();
        m_exampleTableView = new TableView(ptcv);

        JPanel internalPanel = new JPanel(new BorderLayout());
        internalPanel.setBorder(
            BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), borderTitle));
        internalPanel.add(m_createExampleButton, BorderLayout.NORTH);
        internalPanel.add(m_exampleTableView, BorderLayout.CENTER);
        add(internalPanel);
    }

    private void createExampleFromTemplateTable() {
        m_exampleTableView.setDataTable(m_templateTable);
        try {
            m_exampleTableJson = ContainerTableMapper.toContainerTableJsonValue(m_templateTable);
        } catch (InvalidSettingsException e) {
            throw new RuntimeException("Could not map input table to json", e);
        }
    }

    /**
     * Sets the example table that is displayed in the view.
     * @param exampleTable the example table to be set
     */
    public void setExampleTable(final DataTable exampleTable) {
        m_exampleTableView.setDataTable(exampleTable);
    }

    /**
     * Sets the enabled state of the createExampleButton.
     * @param enabled
     */
    public void setCreateExampleButtonEnabled(final boolean enabled) {
        m_createExampleButton.setEnabled(enabled);
    }

    /**
     * Sets the table that an example input can later be created from.
     *
     * @param templateTable the template table
     */
    public void setTemplateTable(final BufferedDataTable templateTable) {
        m_templateTable = templateTable;
    }

    /**
     * Returns the json representation of the table set as a new example. Null if no new example has been set.
     *
     * @return a json representation of the new example table, null if no new example table has been set
     */
    public JsonValue getExampleTableJson() {
        return m_exampleTableJson;
    }
}
