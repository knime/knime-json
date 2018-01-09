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
 *   25 Sept. 2014 (Gabor): created
 */
package org.knime.json.node.writer;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Collections;
import java.util.Vector;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.StringValue;
import org.knime.core.data.json.JSONValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.ColumnSelectionComboxBox;
import org.knime.core.node.util.FilesHistoryPanel;
import org.knime.core.node.util.FilesHistoryPanel.LocationValidation;
import org.knime.core.node.workflow.FlowVariable.Type;
import org.knime.json.node.util.GUIFactory;
import org.knime.json.node.writer.JSONWriterNodeSettings.CompressionMethods;

/**
 * <code>NodeDialog</code> for the "JSONWriter" node. Writes {@code .json} files from {@link JSONValue}s.
 *
 * @author Gabor Bakos
 */
final class JSONWriterNodeDialog extends NodeDialogPane {
    /**
     *
     */
    private static final String DOT_JSON_GZ = ".json.gz";

    /**
     *
     */
    private static final String DOT_JSON = ".json";

    private final JSONWriterNodeSettings m_settings = new JSONWriterNodeSettings();

    private ColumnSelectionComboxBox m_inputColumn;

    private JCheckBox m_overwriteExisting;

    private FilesHistoryPanel m_container;

    private JComboBox<String> m_format;

    private JComboBox<CompressionMethods> m_compression;

    private JTextField m_extension;

//    private RemoteFileChooserPanel m_fileChooserPanel;

    /**
     * New pane for configuring the JSONWriter node.
     */
    protected JSONWriterNodeDialog() {
        JPanel panel = createPanel();
        addTab("Settings", panel);
    }

    /**
     * @return The main settings panel.
     */
    private JPanel createPanel() {
        JPanel ret = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(4, 4, 4, 4);
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.weightx = 0;
        c.weighty = 0;

        ret.add(new JLabel("JSON column:"), c);
        c.gridx++;
        c.weightx = 1;
        {
            @SuppressWarnings("unchecked")
            ColumnSelectionComboxBox columnSelectionComboxBox = new ColumnSelectionComboxBox(JSONValue.class);
            m_inputColumn = columnSelectionComboxBox;
        }
        m_inputColumn.setBorder(null);
        ret.add(m_inputColumn, c);

        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        ret.add(new JLabel("Export format:"), c);
        c.gridx++;
        c.weightx = 1;
        m_format = new JComboBox<>(new Vector<>(Collections.singleton("JSON")));
        DefaultListCellRenderer stringValueRenderer = new DefaultListCellRenderer() {
            private static final long serialVersionUID = 4074232838857030698L;

            /**
             * {@inheritDoc}
             */
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index,
                final boolean isSelected, final boolean cellHasFocus) {
                if (value instanceof StringValue) {
                    StringValue sv = (StringValue)value;
                    return super.getListCellRendererComponent(list, sv.getStringValue(), index, isSelected,
                        cellHasFocus);
                }
                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }
        };
        m_format.setRenderer(stringValueRenderer);
        m_format.setBorder(null);
        ret.add(m_format, c);

        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        ret.add(new JLabel("Compress:"), c);
        c.gridx++;
        c.weightx = 1;
        m_compression = new JComboBox<>(CompressionMethods.values());
        m_compression.setRenderer(stringValueRenderer);
        m_compression.setBorder(null);
        ret.add(m_compression, c);

        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        ret.add(new JLabel("File extension:"), c);
        c.gridx++;
        c.weightx = 1;
        m_extension = GUIFactory.createTextField(DOT_JSON, 11);
        ret.add(m_extension, c);
        m_compression.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                CompressionMethods selected = (CompressionMethods)m_compression.getSelectedItem();
                switch (selected) {
                    case GZIP:
                        if (DOT_JSON.equals(m_extension.getText())) {
                            m_extension.setText(DOT_JSON_GZ);
                        }
                        break;
                    case NONE:
                        if (DOT_JSON_GZ.equals(m_extension.getText())) {
                            m_extension.setText(DOT_JSON);
                        }
                        break;
                    default:
                        throw new UnsupportedOperationException("Unknown compression format: " + selected);
                }
            }});

        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        final JLabel labelOfContainer = new JLabel("Selected directory:");
        ret.add(labelOfContainer, c);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.BOTH;
        c.gridx++;
        c.weightx = 1;
        m_container =
            new FilesHistoryPanel(createFlowVariableModel(JSONWriterNodeSettings.OUTPUT_LOCATION, Type.STRING),
                "org.knime.json.node.writer", LocationValidation.DirectoryOutput, DOT_JSON, DOT_JSON_GZ, ".zip"/*, ".smile", ".smile.gz"*/);
        m_container.requestFocus();
        m_container.setBorder(null);
        ret.add(m_container, c);
        m_compression.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    Object item = e.getItem();
                    if (item instanceof CompressionMethods) {
                        CompressionMethods cm = (CompressionMethods)item;
                        if (cm.supportsMultipleFiles()) {
                            labelOfContainer.setText("Selected file:");
                            m_container.setSelectMode(JFileChooser.FILES_ONLY);
                        } else {
                            labelOfContainer.setText("Selected directory:");
                            m_container.setSelectMode(JFileChooser.DIRECTORIES_ONLY);
                        }
                    }
                }
            }
        });

        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        c.gridwidth = 2;
        m_overwriteExisting = new JCheckBox("Overwrite existing files.");
        ret.add(m_overwriteExisting, c);

        c.gridy++;
        c.weighty = 1;
        ret.add(new JPanel(), c);
        ret.setPreferredSize(new Dimension(600, 400));
        return ret;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        m_settings.loadSettingsDialog(settings, (DataTableSpec)specs[0]);
        m_inputColumn.setSelectedColumn(m_settings.getInputColumn());
        m_inputColumn.update((DataTableSpec)specs[0], m_settings.getInputColumn());
        m_overwriteExisting.setSelected(m_settings.getOverwriteExistingFiles());
        m_container.setSelectedFile(m_settings.getOutputLocation());
        m_extension.setText(m_settings.getExtension());
        m_format.setSelectedItem(m_settings.getFormat());
        m_compression.setSelectedItem(m_settings.getCompressionMethod());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_settings.setInputColumn(m_inputColumn.getSelectedColumn());
        m_settings.setOverwriteExisting(m_overwriteExisting.isSelected());
        m_settings.setOutputLocation(m_container.getSelectedFile());
        m_container.addToHistory();
        m_settings.setExtension(m_extension.getText());
        m_settings.setCompressionMethod((CompressionMethods)m_compression.getSelectedItem());
        m_settings.setCompressContents(m_settings.getCompressionMethod() != CompressionMethods.NONE);
        m_settings.setFormat((String)m_format.getSelectedItem());
        m_settings.saveSettings(settings);
    }
}
