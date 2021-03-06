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
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.stream.Stream;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
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
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.data.location.variable.FSLocationSpecVariableType;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.DialogComponentReaderFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.ReadPathAccessor;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.SettingsModelReaderFileChooser;
import org.knime.filehandling.core.node.table.reader.MultiTableReadFactory;
import org.knime.filehandling.core.node.table.reader.ProductionPathProvider;
import org.knime.filehandling.core.node.table.reader.config.DefaultTableReadConfig;
import org.knime.filehandling.core.node.table.reader.config.TableReadConfig;
import org.knime.filehandling.core.node.table.reader.dialog.SourceIdentifierColumnPanel;
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

    private final JPanel m_jsonModeCardLayout;

    private final JPanel m_legacyModePanel;

    private final JLabel m_warningLabel;

    private final JTextField m_columnName = new JTextField("##########", 10);

    private final JCheckBox m_allowComments;

    private final JCheckBox m_selectPart;

    private final JTextField m_jsonPath = new JTextField("##########", 10);

    private final JCheckBox m_failIfNotFound;

    private final JCheckBox m_limitRowsChecker;

    private final JSpinner m_limitRowsSpinner;

    private final JCheckBox m_skipFirstRowsChecker;

    private final JSpinner m_skipFirstRowsSpinner;

    private final SourceIdentifierColumnPanel m_pathColumnPanel = new SourceIdentifierColumnPanel("Path");

    protected JSONReaderNodeDialog(final SettingsModelReaderFileChooser fileChooser,
        final JSONMultiTableReadConfig config,
        final MultiTableReadFactory<FSPath, JSONReaderConfig, DataType> multiReader,
        final ProductionPathProvider<DataType> productionPathProvider) {
        super(multiReader, productionPathProvider, true, false, true);

        final SettingsModelReaderFileChooser fileChooserModel = fileChooser;
        final FlowVariableModel sourceFvm = createFlowVariableModel(
            Stream.concat(Stream.of(SettingsUtils.CFG_SETTINGS_TAB),
                Arrays.stream(fileChooserModel.getKeysForFSLocation())).toArray(String[]::new),
            FSLocationSpecVariableType.INSTANCE);

        Long stepSize = Long.valueOf(1);
        Long rowStart = Long.valueOf(0);
        Long rowEnd = Long.valueOf(Long.MAX_VALUE);
        Long skipOne = Long.valueOf(1);
        Long initLimit = Long.valueOf(50);

        m_config = config;
        m_fileChooser = fileChooser;

        m_sourceFilePanel = new DialogComponentReaderFileChooser(fileChooserModel, "source_chooser", sourceFvm);

        m_warningLabel = new JLabel("");
        m_selectPart = new JCheckBox("Select with JSONPath");
        m_failIfNotFound = new JCheckBox("Fail if path not found");
        m_failIfNotFound
            .setToolTipText("When unchecked and path does not match any input, empty table will be generated.");
        m_allowComments = new JCheckBox("Allow comments in JSON files");
        m_allowComments.setToolTipText("/*...*/, // or #");

        m_legacyModePanel = makeLegacyModePanel();
        m_jsonModeCardLayout = makeReadModeCardLayout();
        m_selectPart.addChangeListener(l -> handleUsePath());
        m_selectPart.doClick();

        m_skipFirstRowsChecker = new JCheckBox("Skip first data rows ");
        m_skipFirstRowsSpinner = new JSpinner(new SpinnerNumberModel(skipOne, rowStart, rowEnd, stepSize));
        m_skipFirstRowsChecker.addActionListener(e -> controlSpinner(m_skipFirstRowsChecker, m_skipFirstRowsSpinner));
        m_skipFirstRowsChecker.doClick();

        m_limitRowsChecker = new JCheckBox("Limit data rows ");
        m_limitRowsSpinner = new JSpinner(new SpinnerNumberModel(initLimit, rowStart, rowEnd, initLimit));
        m_limitRowsChecker.addActionListener(e -> controlSpinner(m_limitRowsChecker, m_limitRowsSpinner));
        m_limitRowsChecker.doClick();

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
        m_columnName.getDocument().addDocumentListener(documentListener);
        m_allowComments.addActionListener(actionListener);
        m_selectPart.addActionListener(actionListener);
        m_jsonPath.getDocument().addDocumentListener(documentListener);
        m_failIfNotFound.addActionListener(actionListener);

        m_limitRowsChecker.addActionListener(actionListener);
        m_skipFirstRowsChecker.addActionListener(actionListener);

        m_skipFirstRowsSpinner.getModel().addChangeListener(changeListener);
        m_limitRowsSpinner.getModel().addChangeListener(changeListener);
        m_pathColumnPanel.addChangeListener(changeListener);
    }

    /**
     * Enables a {@link JSpinner} based on a corresponding {@link JCheckBox}.
     *
     * @param checker the {@link JCheckBox} which controls if a {@link JSpinner} should be enabled
     * @param spinner a {@link JSpinner} controlled by the {@link JCheckBox}
     */
    private static void controlSpinner(final JCheckBox checker, final JSpinner spinner) {
        spinner.setEnabled(checker.isSelected());
    }

    private void handleUsePath() {
        m_failIfNotFound.setEnabled(m_selectPart.isSelected());
        m_jsonPath.setEnabled(m_selectPart.isSelected());
        // Jsurfer doesn't support JSON comments
        m_allowComments.setEnabled(!m_selectPart.isSelected());
    }

    private void createDialogPanels() {
        addTab("Settings", createSettingsPanel());
        addTab("Limit Rows", getLimitRowsPanel());
    }

    private JPanel createSettingsPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        GBCBuilder gbc = createGBCBuilder().fillHorizontal().setWeightX(1).anchorPageStart();
        panel.add(createSourcePanel(), gbc.build());
        panel.add(m_jsonModeCardLayout, gbc.incY().build());
        panel.add(m_pathColumnPanel, gbc.incY().build());
        gbc.setWeightY(1).resetX().widthRemainder().incY().fillBoth();
        panel.add(createPreview(), gbc.build());
        return panel;
    }

    private JPanel makeReadModeCardLayout() {
        final JPanel panel = new JPanel(new CardLayout());
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Reader options:"));
        panel.add(m_legacyModePanel, JSONReadMode.LEGACY.name());
        return panel;
    }

    private JPanel makeLegacyModePanel() {

        final JPanel optionsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        gbc.weighty = 0;

        optionsPanel.add(new JLabel("Output column name"), gbc);
        gbc.gridx = 1;
        optionsPanel.add(getInFlowLayout(m_columnName), gbc);
        gbc.gridy++;
        gbc.gridx = 0;
        optionsPanel.add(getInFlowLayout(m_selectPart), gbc);
        gbc.gridy++;
        optionsPanel.add(new JLabel("JSONPath"), gbc);
        gbc.gridx = 1;

        optionsPanel.add(getInFlowLayout(m_jsonPath), gbc);

        gbc.gridy++;
        optionsPanel.add(m_warningLabel, gbc);
        gbc.gridy++;
        gbc.gridx = 0;
        optionsPanel.add(getInFlowLayout(m_failIfNotFound), gbc);
        gbc.gridy++;

        optionsPanel.add(getInFlowLayout(m_allowComments), gbc);

        //Filling remaining space
        gbc.gridy++;
        gbc.weighty = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridwidth = 2;
        optionsPanel.add(new JPanel(), gbc);

        return optionsPanel;
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
     * Creates a {@link JPanel} filled with dialog components specific to limiting the number of rows that are read.
     *
     * @return a {@link JPanel} filled with dialog components.
     */
    private JPanel getLimitRowsPanel() {
        final JPanel limitPanel = new JPanel(new GridBagLayout());
        GBCBuilder gbc = createGBCBuilder().fillNone();
        limitPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Limit rows"));
        limitPanel.add(m_skipFirstRowsChecker, gbc.build());
        gbc.incX().setWeightX(1);
        limitPanel.add(m_skipFirstRowsSpinner, gbc.build());
        gbc.incY();
        gbc.setX(0).setWeightX(0);
        limitPanel.add(m_limitRowsChecker, gbc.build());
        gbc.incX().setWeightX(1);
        limitPanel.add(m_limitRowsSpinner, gbc.build());
        gbc.setWeightY(1).resetX().widthRemainder().incY().insetBottom(0).fillBoth();
        limitPanel.add(createPreview(), gbc.build());
        return limitPanel;
    }

    /**
     * Creates a standard setup {@link GBCBuilder}.
     *
     * @return returns a {@link GBCBuilder}
     */
    private static final GBCBuilder createGBCBuilder() {
        return new GBCBuilder().resetPos().fillHorizontal().anchorFirstLineStart();
    }

    private static JPanel getInFlowLayout(final JComponent... comps) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
        for (JComponent c : comps) {
            p.add(c);
        }
        return p;
    }

    @Override
    protected ReadPathAccessor createReadPathAccessor() {
        return m_fileChooser.createReadPathAccessor();
    }

    @Override
    protected JSONMultiTableReadConfig loadSettings(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        m_sourceFilePanel.loadSettingsFrom(SettingsUtils.getOrEmpty(settings, SettingsUtils.CFG_SETTINGS_TAB), specs);
        final JSONReaderConfig jsonReaderConfig = m_config.getReaderSpecificConfig();
        final TableReadConfig<JSONReaderConfig> tableReadConfig = m_config.getTableReadConfig();

        m_config.loadInDialog(settings, specs);

        m_allowComments.setSelected(jsonReaderConfig.allowComments());
        m_columnName.setText(jsonReaderConfig.getColumnName());
        m_selectPart.setSelected(jsonReaderConfig.useJSONPath());
        m_jsonPath.setText(jsonReaderConfig.getJSONPath());
        m_failIfNotFound.setSelected(jsonReaderConfig.failIfNotFound());

        m_skipFirstRowsChecker.setSelected(tableReadConfig.skipRows());
        m_skipFirstRowsSpinner.setValue(tableReadConfig.getNumRowsToSkip());

        m_limitRowsChecker.setSelected(tableReadConfig.limitRows());
        m_limitRowsSpinner.setValue(tableReadConfig.getMaxRows());

        m_pathColumnPanel.load(m_config.appendItemIdentifierColumn(), m_config.getItemIdentifierColumnName());

        controlSpinner(m_skipFirstRowsChecker, m_skipFirstRowsSpinner);
        controlSpinner(m_limitRowsChecker, m_limitRowsSpinner);

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

    private static JSONReadMode getJsonReadMode() {
        return JSONReadMode.LEGACY;
    }

    @Override
    protected JSONMultiTableReadConfig getConfig() throws InvalidSettingsException {
        saveTableReadSettings(m_config.getTableReadConfig());
        saveJsonReaderSettings(m_config.getTableReadConfig().getReaderSpecificConfig());
        m_config.setAppendItemIdentifierColumn(m_pathColumnPanel.isAppendSourceIdentifierColumn());
        m_config.setItemIdentifierColumnName(m_pathColumnPanel.getSourceIdentifierColumnName());
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

        config.setSkipRows(m_skipFirstRowsChecker.isSelected());
        config.setNumRowsToSkip((Long)m_skipFirstRowsSpinner.getValue());

        config.setLimitRows(m_limitRowsChecker.isSelected());
        config.setMaxRows((Long)m_limitRowsSpinner.getValue());
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_sourceFilePanel.saveSettingsTo(SettingsUtils.getOrAdd(settings, SettingsUtils.CFG_SETTINGS_TAB));
        getConfig().saveInDialog(settings);
    }

    @Override
    public void onClose() {
        m_sourceFilePanel.onClose();
        super.onClose();
    }

}
