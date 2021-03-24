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
package org.knime.json.filehandling.reader;

import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JPanel;

import org.knime.core.data.json.JSONValue;
import org.knime.core.node.FlowVariableModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.filehandling.core.data.location.variable.FSLocationVariableType;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.DialogComponentReaderFileChooser;
import org.knime.filehandling.core.util.GBCBuilder;

/**
 * <code>NodeDialog</code> for the "JSONReader" node. Reads {@code .json} files to {@link JSONValue}s.
 *
 * @author Moditha Hewasinghage
 */
final class JSONReaderNodeDialog extends NodeDialogPane {
    /**
     * The key for the history of previous locations.
     */
    private static final String HISTORY_ID = "JSONReader";

    private final DialogComponentString m_columnName;

    private final DialogComponentBoolean m_allowComments;

    private final DialogComponentBoolean m_inferColumns;

    private final JSONReaderNodeConfiguration m_config;

    private final DialogComponentReaderFileChooser m_inputLocation;

    /**
     *
     * New pane for configuring the JSONReader node.
     *
     * @param createSettings
     */
    public JSONReaderNodeDialog(final JSONReaderNodeConfiguration config) {
        m_config = config;

        final FlowVariableModel readFvm = createFlowVariableModel(
            config.getFileChooserSettings().getKeysForFSLocation(), FSLocationVariableType.INSTANCE);
        m_inputLocation = new DialogComponentReaderFileChooser(config.getFileChooserSettings(), "list_files_history", readFvm);
        m_inputLocation.getComponentPanel().setBorder(BorderFactory.createTitledBorder("Read"));

        m_columnName =new DialogComponentString(m_config.getColumnName(), "Output column name");
        m_allowComments = new DialogComponentBoolean(m_config.getAllowComments(),"Allow comments in JSON files");
        m_allowComments.setToolTipText("/*...*/, // or #");
        m_inferColumns = new DialogComponentBoolean(m_config.getInferColumns(), "Infer columns for JSON");

        addTab("Settings", layout());
    }

    private JPanel layout() {
        final JPanel panel = new JPanel(new GridBagLayout());
        GBCBuilder gbc = new GBCBuilder(new Insets(5, 5, 5, 5)).resetX().resetY();
        panel.add(m_inputLocation.getComponentPanel(), gbc.resetX().incY().fillHorizontal().setWeightX(1).setWidth(2).build());
        gbc.setWidth(1);
        gbc.setX(0);
        gbc.incY();
        panel.add(m_inferColumns.getComponentPanel(), gbc.build());
        gbc.incX();
        panel.add(m_columnName.getComponentPanel(), gbc.build());
        gbc.setX(0);
        gbc.incY();
        panel.add(m_allowComments.getComponentPanel(), gbc.build());
        gbc.incY().fillBoth().setWeightY(1);
        panel.add(Box.createVerticalBox(), gbc.build());
        return panel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_inputLocation.saveSettingsTo(settings);
        m_config.saveSettingsForDialog(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        m_inputLocation.loadSettingsFrom(settings, specs);
        try {
            m_config.loadSettingsForDialog(settings);
        } catch (InvalidSettingsException e) {
            throw new NotConfigurableException(e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCancel() {
        super.onCancel();
    }

    @Override
    public void onClose() {
        m_inputLocation.onClose();
    }
}
