package org.knime.json.node.jsonpath;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.EnumSet;

import javax.swing.ButtonModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.json.node.jsonpath.JSONPathSettings.OnMultipleResults;
import org.knime.json.node.util.GUIFactory;
import org.knime.json.node.util.OutputType;
import org.knime.json.node.util.PathOrPointerDialog;

import com.jayway.jsonpath.JsonPath;

/**
 * <code>NodeDialog</code> for the "JSONPath" Node. Selects certain paths from the selected JSON column.
 *
 * @author Gabor Bakos
 */
public class JSONPathNodeDialog extends PathOrPointerDialog<JSONPathSettings> {
    //TODO error message on typing
    private JTextField m_path;

    private JCheckBox m_resultIsList;

    private final JCheckBox m_returnPaths = new JCheckBox("Return the paths instead of values");

    private JComboBox<OnMultipleResults> m_onMultipleResults;

    private JLabel m_nonDefiniteWarning, m_syntaxError;

    /**
     * New pane for configuring the JSONPath node.
     */
    protected JSONPathNodeDialog() {
        super(JSONPathNodeModel.createJSONPathProjectionSettings());
        final JPanel advanced = new JPanel();
        addTab("Advanced", advanced);
        advanced.add(m_returnPaths);
        m_returnPaths.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                updateEnabled();
            }
        });
        getPanel().setPreferredSize(new Dimension(600, 500));
        updateEnabled();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int addAfterInputColumn(final JPanel panel, final int afterInput) {
        m_nonDefiniteWarning = new JLabel("Path is non-definite");
        m_syntaxError = new JLabel();
        panel.setPreferredSize(new Dimension(800, 300));
        GridBagConstraints gbc = createInitialConstraints();
        gbc.gridy = afterInput;
        gbc.gridx = 0;
        panel.add(new JLabel("JSONPath:"), gbc);
        gbc.gridx = 1;
        m_path = GUIFactory.createTextField("", 22);
        m_path.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(final DocumentEvent e) {
                updateWarnings();
            }

            @Override
            public void removeUpdate(final DocumentEvent e) {
                updateWarnings();
            }

            @Override
            public void changedUpdate(final DocumentEvent e) {
                updateWarnings();
            }
            });
        panel.add(m_path, gbc);
        gbc.gridy++;
        panel.add(m_syntaxError, gbc);
        gbc.gridy++;
        gbc.gridy = addOutputTypePanel(panel, gbc.gridy);

        gbc.gridx = 0;
        panel.add(m_nonDefiniteWarning, gbc);
        m_nonDefiniteWarning.setVisible(false);
        gbc.gridx = 1;
        m_resultIsList = new JCheckBox("Result is list");
        panel.add(m_resultIsList, gbc);
        gbc.gridy++;

        gbc.gridx = 0;
        panel.add(new JLabel("Strategy for multiple results:"), gbc);
        gbc.gridx = 1;
        m_onMultipleResults = new JComboBox<>(OnMultipleResults.values());
        panel.add(m_onMultipleResults, gbc);
        gbc.gridy++;

        m_resultIsList.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                updateWarnings();
                updateEnabled();
            }
        });
        m_onMultipleResults.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent e) {
                updateEnabled();
            }
        });
        return gbc.gridy;
    }

    private void updateEnabled() {
        if (m_returnPaths.isSelected() && !(getOutputTypeButtonModel(OutputType.String).isSelected())) {
            getOutputTypeButtonModel(OutputType.String).setSelected(true);
        }
        m_resultIsList.setEnabled(!m_returnPaths.isSelected());
        m_onMultipleResults.setEnabled(!m_resultIsList.isSelected() && !m_returnPaths.isSelected());
        if (!m_resultIsList.isSelected()) {
            boolean wasSelected = false;
            EnumSet<OutputType> supportedOutputTypes =
                ((OnMultipleResults)m_onMultipleResults.getSelectedItem()).supportedOutputTypes();
            if (m_returnPaths.isSelected()) {
                supportedOutputTypes = EnumSet.of(OutputType.String);
            }
            for (OutputType type : EnumSet.allOf(OutputType.class)) {
                final ButtonModel model = getOutputTypeButtonModel(type);
                boolean supported = supportedOutputTypes.contains(type);
                wasSelected |= supported && model.isSelected();
                model.setEnabled(supported);
            }
            if (!wasSelected && !supportedOutputTypes.isEmpty()) {
                getOutputTypeButtonModel(supportedOutputTypes.iterator().next()).setSelected(true);
            }
        } else if (!m_returnPaths.isSelected()) {
            for (OutputType type : EnumSet.allOf(OutputType.class)) {
                getOutputTypeButtonModel(type).setEnabled(true);
            }
        }
    }

    private void updateWarnings() {
        String text = m_path.getText();
        m_nonDefiniteWarning.setVisible(false);
        m_syntaxError.setText("");
        try {
            if (!JsonPath.compile(text).isDefinite()) {
                m_nonDefiniteWarning.setVisible(!m_resultIsList.isSelected() && !m_returnPaths.isSelected());
            }
        } catch (RuntimeException e) {
            m_syntaxError.setText(e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        super.loadSettingsFrom(settings, specs);
        m_path.setText(getSettings().getJsonPath());
        m_resultIsList.setSelected(getSettings().isResultIsList());
        m_returnPaths.setSelected(getSettings().isReturnPaths());
        OnMultipleResults onMultipleResults = getSettings().getOnMultipleResults();
        if (onMultipleResults != null) {
            m_onMultipleResults.setSelectedItem(onMultipleResults);
        }
        updateEnabled();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        try {
            JsonPath.compile(m_path.getText());
        } catch (RuntimeException e) {
            throw new InvalidSettingsException("Wrong path: " + m_path.getText() + "\n" + e.getMessage(), e);
        }
        getSettings().setJsonPath(m_path.getText());
        getSettings().setResultIsList(m_resultIsList.isSelected());
        getSettings().setReturnPaths(m_returnPaths.isSelected());
        final OnMultipleResults onMultipleResults = (OnMultipleResults)m_onMultipleResults.getSelectedItem();
        if (onMultipleResults == null && !m_resultIsList.isSelected()) {
            throw new InvalidSettingsException(
                "No strategy selected for the case of multiple results! Please select one");
        }
        getSettings().setOnMultipleResults(onMultipleResults);
        super.saveSettingsTo(settings);
    }
}
