package org.knime.json.node.jsonpointer;

import java.awt.Dimension;
import java.awt.GridBagConstraints;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.json.node.util.GUIFactory;
import org.knime.json.node.util.PathOrPointerDialog;

import com.github.fge.jackson.jsonpointer.JsonPointer;
import com.github.fge.jackson.jsonpointer.JsonPointerException;

/**
 * <code>NodeDialog</code> for the "JSONPointer" Node. Selects certain pointers from the selected JSON column.
 *
 * @author Gabor Bakos
 */
public class JSONPointerNodeDialog extends PathOrPointerDialog<JSONPointerSettings> {
    private JTextField m_pointer;
    private JLabel m_error;

    /**
     * New pane for configuring the JSONPointer node.
     */
    protected JSONPointerNodeDialog() {
        super(JSONPointerNodeModel.createJSONPathProjectionSettings());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int addAfterInputColumn(final JPanel panel, final int afterInput) {
        panel.setPreferredSize(new Dimension(800, 300));
        m_error = new JLabel();
        GridBagConstraints gbc = createInitialConstraints();
        gbc.gridy = afterInput;
        gbc.gridx = 0;
        panel.add(new JLabel("JSONPointer:"), gbc);
        gbc.gridx = 1;
        m_pointer = GUIFactory.createTextField("", 33);
        panel.add(m_pointer, gbc);
        gbc.gridy++;

        panel.add(m_error, gbc);
        gbc.gridy++;

        m_pointer.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void removeUpdate(final DocumentEvent e) {
                updateError();
            }

            @Override
            public void insertUpdate(final DocumentEvent e) {
                updateError();
            }

            @Override
            public void changedUpdate(final DocumentEvent e) {
                updateError();
            }
        });
        return addOutputTypePanel(panel, gbc.gridy);
    }

    /**
     *
     */
    protected void updateError() {
        m_error.setText("");
        try {
            new JsonPointer(m_pointer.getText());
        } catch (JsonPointerException e) {
            m_error.setText(e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        super.loadSettingsFrom(settings, specs);
        m_pointer.setText(getSettings().getJsonPointer());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        try {
            new JsonPointer(m_pointer.getText());
        } catch (RuntimeException | JsonPointerException e) {
            throw new InvalidSettingsException("Wrong pointer: " + m_pointer.getText() + "\n" + e.getMessage(), e);
        }
        getSettings().setJsonPath(m_pointer.getText());
        super.saveSettingsTo(settings);
    }
}
