package org.knime.json.node.reader;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.knime.core.data.json.JSONValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.FilesHistoryPanel;
import org.knime.core.node.util.FilesHistoryPanel.LocationValidation;
import org.knime.core.node.workflow.FlowVariable.Type;
import org.knime.json.node.util.GUIFactory;

import com.github.fge.jackson.jsonpointer.JsonPointer;
import com.github.fge.jackson.jsonpointer.JsonPointerException;

/**
 * <code>NodeDialog</code> for the "JSONReader" node. Reads {@code .json} files to {@link JSONValue}s.
 *
 * @author Gabor Bakos
 */
final class JSONReaderNodeDialog extends NodeDialogPane {
    /**
     * The key for the history of previous locations.
     */
    static final String HISTORY_ID = "JSONReader";

    private JSONReaderSettings m_settings = JSONReaderNodeModel.createSettings();

    private FilesHistoryPanel m_location;

    private JTextField m_columnName;

    private final JCheckBox m_selectPart;

    private final JTextField m_jsonPointer;

    private final JCheckBox m_failIfNotFound;

    private JCheckBox m_allowComments;

    /**
     * New pane for configuring the JSONReader node.
     */
    protected JSONReaderNodeDialog() {
        JPanel panel = new JPanel(new GridBagLayout());
        addTab("Settings", panel);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.weighty = 0;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;

        panel.add(new JLabel("Location"), gbc);
        gbc.gridx++;
        gbc.weightx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        m_location =
            new FilesHistoryPanel(createFlowVariableModel(JSONReaderSettings.LOCATION, Type.STRING), HISTORY_ID,
                LocationValidation.FileInput, "json|json.gz", "");
        panel.add(m_location, gbc);
        gbc.gridy++;
        gbc.gridwidth = 1;

        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0;
        gbc.weightx = 0;
        panel.add(new JLabel("Output column name"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        m_columnName = GUIFactory.createTextField("", 22);
        m_columnName.getDocument().addDocumentListener(new DocumentListener() {
            private void reportError() {
                if (m_columnName.getText().trim().isEmpty()) {
                    error();
                } else {
                    noError();
                }
            }
            private void noError() {
                m_columnName.setBackground(Color.WHITE);
                m_columnName.setToolTipText(null);
            }
            private void error() {
                m_columnName.setBackground(Color.RED);
                m_columnName.setToolTipText("Empty column names are not allowed");
            }
            @Override
            public void insertUpdate(final DocumentEvent e) {
                reportError();
            }

            @Override
            public void removeUpdate(final DocumentEvent e) {
                reportError();
            }

            @Override
            public void changedUpdate(final DocumentEvent e) {
                reportError();
            }});
        panel.add(m_columnName, gbc);

        gbc.gridy++;

        gbc.gridx = 1;
        m_selectPart = new JCheckBox("Select with JSON Pointer");
        panel.add(m_selectPart, gbc);
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.weightx = 0;
        panel.add(new JLabel("JSON Pointer"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        m_jsonPointer = GUIFactory.createTextField("", 22);
        m_jsonPointer.setToolTipText("Hint: Use the annotations to explain it.");
        final JLabel warningLabel = new JLabel();
        m_jsonPointer.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void removeUpdate(final DocumentEvent e) {
                reportError();
            }

            @Override
            public void insertUpdate(final DocumentEvent e) {
                reportError();
            }

            @Override
            public void changedUpdate(final DocumentEvent e) {
                reportError();
            }
            private void reportError() {
                try {
                    new JsonPointer(m_jsonPointer.getText());
                    noError();
                } catch (JsonPointerException | RuntimeException e) {
                    error(e.getMessage());
                }
            }

            private void error(final String message) {
                warningLabel.setText(message);
            }

            private void noError() {
                warningLabel.setText("");
            }
        });
        panel.add(m_jsonPointer, gbc);
        gbc.gridy++;
        panel.add(warningLabel, gbc);
        gbc.gridy++;

        m_failIfNotFound = new JCheckBox("Fail if pointer not found");
        m_failIfNotFound.setToolTipText("When unchecked and pointer do not match input, "
            + "missing value will be generated.");
        gbc.weightx = 1;
        panel.add(m_failIfNotFound, gbc);
        gbc.gridy++;

        gbc.gridy++;
        m_allowComments = new JCheckBox("Allow comments in JSON files");
        panel.add(m_allowComments, gbc);

        m_selectPart.getModel().addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent e) {
                boolean select = m_selectPart.isSelected();
                m_jsonPointer.setEnabled(select);
                m_failIfNotFound.setEnabled(select);
            }
        });
        m_selectPart.setSelected(true);
        m_selectPart.setSelected(false);

        //Filling remaining space
        gbc.gridy++;
        gbc.weighty = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(new JPanel(), gbc);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        //We add the location to history using dummy save
        m_location.addToHistory();
        m_settings.setLocation(m_location.getSelectedFile());
        m_settings.setColumnName(m_columnName.getText());
        m_settings.setSelectPart(m_selectPart.isSelected());
        m_settings.setJsonPointer(m_jsonPointer.getText());
        m_settings.setFailIfNotFound(m_failIfNotFound.isSelected());
        m_settings.setAllowComments(m_allowComments.isSelected());
        m_settings.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        m_settings.loadSettingsForDialogs(settings, specs);
        m_location.setSelectedFile(m_settings.getLocation());
        m_selectPart.setSelected(m_settings.isSelectPart());
        m_jsonPointer.setText(m_settings.getJsonPointer());
        m_failIfNotFound.setSelected(m_settings.isFailIfNotFound());
        m_columnName.setText(m_settings.getColumnName());
        m_allowComments.setSelected(m_settings.isAllowComments());
    }
}
