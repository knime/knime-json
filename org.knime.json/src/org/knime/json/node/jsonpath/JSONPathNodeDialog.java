package org.knime.json.node.jsonpath;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.util.EnumSet;

import javax.swing.JCheckBox;
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
    private JTextField m_path;

    private JCheckBox m_resultIsList;

    private final JCheckBox m_returnPaths = new JCheckBox("Return the paths instead of values");

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
//        getPanel().setPreferredSize(new Dimension(600, 500));
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

        m_resultIsList.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                updateWarnings();
                updateEnabled();
            }
        });
        return gbc.gridy;
    }

    private void updateEnabled() {
        if (m_returnPaths.isSelected() && !(OutputType.String == getOutputTypeModel().getSelectedItem())) {
            getOutputTypeModel().setSelectedItem(OutputType.String);
        }
        m_resultIsList.setEnabled(!m_returnPaths.isSelected());
        OutputType selectedOutputType = (OutputType)getOutputTypeModel().getSelectedItem();
        if (!m_resultIsList.isSelected()) {
            EnumSet<OutputType> supportedOutputTypes =
                EnumSet.allOf(OutputType.class);
            if (m_returnPaths.isSelected()) {
                supportedOutputTypes = EnumSet.of(OutputType.String);
            }
            getOutputTypeModel().removeAllElements();
            for (OutputType outputType : supportedOutputTypes) {
                getOutputTypeModel().addElement(outputType);
            }
            if (selectedOutputType != null) {
                getOutputTypeModel().setSelectedItem(selectedOutputType);
            }
        } else if (!m_returnPaths.isSelected()) {
            getOutputTypeModel().removeAllElements();
            for (OutputType type : OutputType.values()) {
                getOutputTypeModel().addElement(type);
            }
            getOutputTypeModel().setSelectedItem(selectedOutputType);
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
        super.saveSettingsTo(settings);
    }
}
