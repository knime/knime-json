package org.knime.json.node.writer;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
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
        m_extension = GUIFactory.createTextField(".json", 11);
        m_extension.setBorder(null);
        ret.add(m_extension, c);

        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        final JLabel labelOfContainer = new JLabel("Selected directory:");
        ret.add(labelOfContainer, c);
        c.gridx++;
        c.weightx = 1;
//        m_fileChooserPanel = new RemoteFileChooserPanel(ret, labelOfContainer.getText(), false, "org.knime.json.node.writer", RemoteFileChooser.SELECT_FILE_OR_DIR, createFlowVariableModel("jsonOutputContainer", Type.STRING), null);
//        ret.add(m_fileChooserPanel.getPanel(), c);
        m_container =
            new FilesHistoryPanel(createFlowVariableModel("jsonOutputContainer", Type.STRING),
                "org.knime.json.node.writer", LocationValidation.DirectoryOutput, ".json", ".json.gz", ".zip"/*, ".smile", ".smile.gz"*/);
        m_container.requestFocus();
        m_container.setSelectMode(JFileChooser.DIRECTORIES_ONLY);
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
        //m_fileChooserPanel.setSelection(m_settings.getOutputLocation());
//        if (specs.length > 1 && specs[1] instanceof ConnectionInformationPortObjectSpec) {
//            ConnectionInformationPortObjectSpec spec = (ConnectionInformationPortObjectSpec)specs[1];
//            m_fileChooserPanel.setConnectionInformation(spec.getConnectionInformation());
//        }
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
//        m_settings.setOutputLocation(m_fileChooserPanel.getSelection());
        m_settings.setExtension(m_extension.getText());
        m_settings.setCompressionMethod((CompressionMethods)m_compression.getSelectedItem());
        m_settings.setCompressContents(m_settings.getCompressionMethod() != CompressionMethods.NONE);
        m_settings.setFormat((String)m_format.getSelectedItem());
        m_settings.saveSettings(settings);
    }
}
