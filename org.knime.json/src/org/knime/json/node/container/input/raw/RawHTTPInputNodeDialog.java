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
 *   24 Jun 2022 (alexander): created
 */
package org.knime.json.node.container.input.raw;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.util.Arrays;
import java.util.stream.Stream;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.filehandling.core.data.location.variable.FSLocationVariableType;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.DialogComponentReaderFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.SettingsModelReaderFileChooser;
import org.knime.filehandling.core.util.SettingsUtils;

/**
 * <code>NodeDialog</code> for the "Container Input (Raw HTTP)" Node.
 *
 * @author Alexander Fillbrunn, KNIME GmbH, Konstanz, Germany
 */
final class RawHTTPInputNodeDialog extends NodeDialogPane {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(RawHTTPInputNodeDialog.class);

    private static final String EMPTY_FILE_MESSAGE = "<no file specified>";

    // File chooser for selecting binary data files
    private final DialogComponentReaderFileChooser m_bodyFileChooser;

    // Checkbox for enabling/disabling the file selection
    private final JCheckBox m_fileSelectionEnabler;

    // Table header key-value pairs
    private final InteractiveKeyValueTable m_headerTable;

    // Table for query parameter key-value pairs
    private final InteractiveKeyValueTable m_qpTable;

    /**
     * Constructor for a node without an input port.
     */
    protected RawHTTPInputNodeDialog() {
        this(null);
    }

    /**
     * New pane for configuring the Container Input (Raw HTTP) node.
     * @param portsConfig
     */
    protected RawHTTPInputNodeDialog(final PortsConfiguration portsConfig) {
        var settingsModel = portsConfig != null ? RawHTTPInputNodeModel.createFileChooserModel(portsConfig)
            : RawHTTPInputNodeModel.createDefaultFileChooserModel();
        var fvm = createFlowVariableModel(
                Stream.concat(Stream.of(SettingsUtils.CFG_SETTINGS_TAB),
                    Arrays.stream(settingsModel.getKeysForFSLocation())).toArray(String[]::new),
                FSLocationVariableType.INSTANCE);

        // dialog elements (body file selector, key-value tables for headers and parameters
        m_bodyFileChooser =
            new DialogComponentReaderFileChooser(settingsModel, RawHTTPInputNodeConfiguration.HISTORY_ID, fvm);
        m_fileSelectionEnabler = new JCheckBox("Enable file output for body");
        m_headerTable = new InteractiveKeyValueTable("Name", "Value");
        m_qpTable = new InteractiveKeyValueTable("Name", "Value");

        addTab("Data", createLayout(), false);
    }

    private JPanel createLayout() {
        var p = new JPanel(new GridBagLayout());
        var gbc = new GridBagConstraints();

        m_fileSelectionEnabler.addItemListener(e -> {
            var isSelected = e.getStateChange() == ItemEvent.SELECTED;
            m_bodyFileChooser.getSettingsModel().setEnabled(isSelected);
            setPlaceHolderIfNecessary(m_bodyFileChooser.getSettingsModel(), isSelected);
        });

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(10, 5, 0, 5);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.weightx = 0.0;
        p.add(new JLabel("File for response body: "), gbc);

        gbc.gridy++;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        p.add(m_fileSelectionEnabler, gbc);

        gbc.gridy++;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        p.add(m_bodyFileChooser.getComponentPanel(), gbc);

        gbc.gridy++;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        p.add(new JLabel("Default headers: "), gbc);

        gbc.gridy++;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        m_headerTable.setPreferredSize(new Dimension(0, 150));
        p.add(m_headerTable, gbc);

        gbc.gridy++;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        p.add(new JLabel("Default query parameters: "), gbc);

        gbc.gridy++;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        m_qpTable.setPreferredSize(new Dimension(0, 150));
        p.add(m_qpTable, gbc);

        return p;
    }

    /**
     * Sets a place holder message in the file selection field, if the field is gettings disabled
     * and the file selection field is blank.
     * In this case, the SettingsModel would issue a "Please specify a file warning" which is
     * circumvented by settings this message.
     *
     * @param settingsModel SettingsModelReaderFileChooser
     * @param isEnabled is the settings model getting enabled?
     */
    private static void setPlaceHolderIfNecessary(final SettingsModelReaderFileChooser settingsModel,
        final boolean isEnabled) {
        var path = settingsModel.getPath();
        if (isEnabled) {
            if (path.equals(EMPTY_FILE_MESSAGE)) {
                settingsModel.setPath("");
            }
        } else if (path == null || path.isBlank()) {
            settingsModel.setPath(EMPTY_FILE_MESSAGE);
        }
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        var config = new RawHTTPInputNodeConfiguration();
        config.setFileSelectionEnabled(m_fileSelectionEnabler.isSelected());
        config.setHeaders(m_headerTable.getTable());
        config.setQueryParams(m_qpTable.getTable());
        config.save(settings);
        m_bodyFileChooser.saveSettingsTo(settings);
    }

    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs) {
        var config = new RawHTTPInputNodeConfiguration().loadInDialog(settings);
        m_fileSelectionEnabler.setSelected(config.isFileSelectionEnabled());
        m_headerTable.setTable(config.getHeaders());
        m_qpTable.setTable(config.getQueryParams());
        try {
            m_bodyFileChooser.loadSettingsFrom(settings, specs);
        } catch (NotConfigurableException e) {
            LOGGER.debug(e);
        }
    }
}
