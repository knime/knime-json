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
 *   Apr 7, 2021 (Moditha): created
 */
package org.knime.json.node.filehandling.reader;

import java.awt.CardLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Stream;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.knime.core.data.DataType;
import org.knime.core.node.FlowVariableModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.filehandling.core.data.location.variable.FSLocationSpecVariableType;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.DialogComponentReaderFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.ReadPathAccessor;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.SettingsModelReaderFileChooser;
import org.knime.filehandling.core.node.table.reader.MultiTableReadFactory;
import org.knime.filehandling.core.node.table.reader.ProductionPathProvider;
import org.knime.filehandling.core.node.table.reader.config.DefaultTableReadConfig;
import org.knime.filehandling.core.node.table.reader.preview.dialog.AbstractPathTableReaderNodeDialog;
import org.knime.filehandling.core.util.GBCBuilder;
import org.knime.filehandling.core.util.SettingsUtils;

/**
 * Node dialog of the JSON reader node.
 *
 * @author Moditha Hewasinghage, KNIME GmbH, Berlin, Germany
 */
final class JSONReaderNodeDialog extends AbstractPathTableReaderNodeDialog<JSONReaderConfig, DataType> {

    private final DialogComponentReaderFileChooser m_sourceFilePanel;

    private final JSONMultiTableReadConfig m_config;

    private final SettingsModelReaderFileChooser m_fileChooser;

    //    private final JRadioButton m_legacyMode;

    //    private final JRadioButton m_streamingMode;

    private final JPanel m_jsonModeCardLayout;

    private final JPanel m_legacyModePanel;

    //    private final JPanel m_streamModePanel;

    private final JLabel m_warningLabel;

    private final JTextField m_columnName = new JTextField("##########", 10);

    private final JCheckBox m_allowComments;

    private final JCheckBox m_selectPart;

    private JTextField m_jsonPath = new JTextField("##########", 10);

    private final JCheckBox m_failIfNotFound;

    /**
     * @param readFactory
     * @param productionPathProvider
     * @param allowsMultipleFiles
     */
    protected JSONReaderNodeDialog(final SettingsModelReaderFileChooser fileChooser,
        final JSONMultiTableReadConfig config,
        final MultiTableReadFactory<Path, JSONReaderConfig, DataType> multiReader,
        final ProductionPathProvider<DataType> productionPathProvider) {
        super(multiReader, productionPathProvider, true);

        final SettingsModelReaderFileChooser fileChooserModel = fileChooser;
        final FlowVariableModel sourceFvm = createFlowVariableModel(
            Stream.concat(Stream.of(SettingsUtils.CFG_SETTINGS_TAB),
                Arrays.stream(fileChooserModel.getKeysForFSLocation())).toArray(String[]::new),
            FSLocationSpecVariableType.INSTANCE);

        m_config = config;
        m_fileChooser = fileChooser;

        m_sourceFilePanel = new DialogComponentReaderFileChooser(fileChooserModel, "source_chooser", sourceFvm);

        //        final ButtonGroup readModeBtnGrp = new ButtonGroup();
        //        m_legacyMode = new JRadioButton(JSONReadMode.LEGACY.getText());
        //        readModeBtnGrp.add(m_legacyMode);
        //        m_streamingMode = new JRadioButton(JSONReadMode.STREAMING.getText());
        //        readModeBtnGrp.add(m_streamingMode);

        m_warningLabel = new JLabel("");
        m_selectPart = new JCheckBox("Select with JSONPath");
        m_failIfNotFound = new JCheckBox("Fail if path not found");
        m_failIfNotFound.setToolTipText("When unchecked and path do not match input, missing value will be generated.");
        m_allowComments = new JCheckBox("Allow comments in JSON files");
        m_allowComments.setToolTipText("/*...*/, // or #");

        m_legacyModePanel = makeLegacyModePanel();
        //        m_streamModePanel = makeStreamingModePanel();
        m_jsonModeCardLayout = makeReadModeCardLayout();
        m_selectPart.addChangeListener(l -> handleUsePath());
        m_selectPart.doClick();
        registerPreviewChangeListeners();
        createDialogPanels();
    }

    private void registerPreviewChangeListeners() {
        final DocumentListener documentListener = new DocumentListener() {

            @Override
            public void removeUpdate(final DocumentEvent e) {
                configChanged();
            }

            @Override
            public void insertUpdate(final DocumentEvent e) {
                configChanged();
            }

            @Override
            public void changedUpdate(final DocumentEvent e) {
                configChanged();
            }
        };
        final ActionListener actionListener = l -> configChanged();
        final ChangeListener changeListener = l -> configChanged();

        m_sourceFilePanel.getModel().addChangeListener(changeListener);

        //        m_legacyMode.addActionListener(l -> handleReadModeUpdate());
        //        m_streamingMode.addActionListener(l -> handleReadModeUpdate());
        m_columnName.getDocument().addDocumentListener(documentListener);
        m_allowComments.addActionListener(actionListener);
        m_selectPart.addActionListener(actionListener);
        m_jsonPath.getDocument().addDocumentListener(documentListener);
        m_failIfNotFound.addActionListener(actionListener);
    }

    private void handleUsePath() {
        m_failIfNotFound.setEnabled(m_selectPart.isSelected());
        m_jsonPath.setEnabled(m_selectPart.isSelected());
        // Jsurfer doesn't support JSON comments
        m_allowComments.setEnabled(m_selectPart.isSelected());
    }

    private void createDialogPanels() {
        addTab("Settings", createSettingsPanel());
    }

    private JPanel createSettingsPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        GBCBuilder gbc = createGBCBuilder().fillHorizontal().setWeightX(1).anchorPageStart();
        panel.add(createSourcePanel(), gbc.build());
        //        panel.add(createReadModePanel(), gbc.incY().build());
        gbc.incY();
        panel.add(m_jsonModeCardLayout, gbc.build());
        gbc.incY();
        gbc.setWeightY(1).resetX().widthRemainder().incY().fillBoth();
        panel.add(createPreview(), gbc.build());
        return panel;
    }

    //    private JPanel createReadModePanel() {
    //        final JPanel readModePanel = new JPanel(new GridBagLayout());
    //        GBCBuilder gbc = createGBCBuilder().fillHorizontal();
    //        readModePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Read Mode"));
    //        readModePanel.add(m_legacyMode, gbc.build());
    //        gbc.incY();
    //        readModePanel.add(m_streamingMode, gbc.build());
    //        return readModePanel;
    //    }

    private JPanel makeReadModeCardLayout() {
        final JPanel panel = new JPanel(new CardLayout());
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Reader options:"));
        panel.add(m_legacyModePanel, JSONReadMode.LEGACY.name());
        //        panel.add(m_streamModePanel, JSONReadMode.STREAMING.name());
        return panel;
    }

    //    private void handleReadModeUpdate() {
    //        final CardLayout cl = (CardLayout)(m_jsonModeCardLayout.getLayout());
    //        final String readMode = m_legacyMode.isSelected() ? JSONReadMode.LEGACY.name() : JSONReadMode.STREAMING.name();
    //        cl.show(m_jsonModeCardLayout, readMode);
    //        configChanged();
    //        //TODO : which one is better get from config or button ?
    //    }

    private JPanel makeLegacyModePanel() {

        final JPanel optionsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.gridwidth = 1;

        optionsPanel.add(new JLabel("Output column name"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        optionsPanel.add(m_columnName, gbc);

        gbc.gridy++;

        gbc.gridx = 1;
        optionsPanel.add(m_selectPart, gbc);
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.weightx = 0;
        optionsPanel.add(new JLabel("JSONPath"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        optionsPanel.add(m_jsonPath, gbc);
        gbc.gridy++;
        optionsPanel.add(m_warningLabel, gbc);
        gbc.gridy++;

        gbc.weightx = 1;
        optionsPanel.add(m_failIfNotFound, gbc);
        gbc.gridy++;

        gbc.gridy++;
        optionsPanel.add(m_allowComments, gbc);

        //Filling remaining space
        gbc.gridy++;
        gbc.weighty = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.BOTH;
        optionsPanel.add(new JPanel(), gbc);

        final JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(Box.createVerticalStrut(5));
        panel.add(optionsPanel);

        return panel;
    }

    private JPanel makeStreamingModePanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        panel.add(new JLabel("Autodetect"));
        return panel;
    }

    /**
     * Creates the source file {@link JPanel}.
     *
     * @return the source file {@link JPanel}
     */
    private JPanel createSourcePanel() {
        final JPanel sourcePanel = new JPanel(new GridBagLayout());
        sourcePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Input location"));
        GBCBuilder gbc = createGBCBuilder().setWeightX(1);
        sourcePanel.add(m_sourceFilePanel.getComponentPanel(), gbc.build());
        return sourcePanel;
    }

    /**
     * Creates a standard setup {@link GBCBuilder}.
     *
     * @return returns a {@link GBCBuilder}
     */
    private static final GBCBuilder createGBCBuilder() {
        return new GBCBuilder().resetPos().fillHorizontal().anchorFirstLineStart();
    }

    @Override
    protected ReadPathAccessor createReadPathAccessor() {
        return m_fileChooser.createReadPathAccessor();
    }

    @Override
    protected JSONMultiTableReadConfig loadSettings(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        m_sourceFilePanel.loadSettingsFrom(SettingsUtils.getOrEmpty(settings, SettingsUtils.CFG_SETTINGS_TAB), specs);
        final DefaultTableReadConfig<JSONReaderConfig> tableReadConfig = m_config.getTableReadConfig();
        final JSONReaderConfig jsonReaderConfig = m_config.getReaderSpecificConfig();

        m_config.loadInDialog(settings, specs);

        //        m_legacyMode.setSelected(jsonReaderConfig.getJsonReadMode() == JSONReadMode.LEGACY);
        //        m_streamingMode.setSelected(jsonReaderConfig.getJsonReadMode() == JSONReadMode.STREAMING);
        m_allowComments.setSelected(jsonReaderConfig.allowComments());
        m_columnName.setText(jsonReaderConfig.getColumnName());
        m_selectPart.setSelected(jsonReaderConfig.useJSONPath());
        m_jsonPath.setText(jsonReaderConfig.getJSONPath());
        m_failIfNotFound.setSelected(jsonReaderConfig.failIfNotFound());
        return m_config;
    }

    /**
     * Saves the {@link LineReaderConfig2}.
     *
     * @param config the {@link LineReaderConfig2}
     */
    private void saveJsonReaderSettings(final JSONReaderConfig config) {
        config.setJsonReadMode(getJsonReadMode());
        config.setAllowComments(m_allowComments.isSelected());
        config.setColumnName(m_columnName.getText());
        config.setJSONPath(m_jsonPath.getText());
        config.setUseJSONPath(m_selectPart.isSelected());
        config.setFailIfNotFound(m_failIfNotFound.isSelected());
    }

    private JSONReadMode getJsonReadMode() {
        //        if (m_legacyMode.isSelected()) {
        return JSONReadMode.LEGACY;
        //        }
        //        else {
        //            return JSONReadMode.STREAMING;
        //        }
    }

    @Override
    protected JSONMultiTableReadConfig getConfig() throws InvalidSettingsException {
        saveTableReadSettings(m_config.getTableReadConfig());
        saveJsonReaderSettings(m_config.getTableReadConfig().getReaderSpecificConfig());

        return m_config;
    }

    /**
     * Saves the {@link DefaultTableReadConfig}.
     *
     * @param config the {@link DefaultTableReadConfig}
     */
    private void saveTableReadSettings(final DefaultTableReadConfig<JSONReaderConfig> config) {
        config.setUseRowIDIdx(false);
        config.setRowIDIdx(-1);
        config.setColumnHeaderIdx(0);
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_sourceFilePanel.saveSettingsTo(SettingsUtils.getOrAdd(settings, SettingsUtils.CFG_SETTINGS_TAB));
        saveTableReadSettings(m_config.getTableReadConfig());
        saveJsonReaderSettings(m_config.getTableReadConfig().getReaderSpecificConfig());

        m_config.saveInDialog(settings);
    }

    @Override
    public void onClose() {
        m_sourceFilePanel.onClose();
    }

}
