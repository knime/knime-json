/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
 *   14 Nov. 2014 (Gabor): created
 */
package org.knime.json.node.toxml;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import org.knime.core.data.json.JSONValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.json.node.util.GUIFactory;
import org.knime.json.node.util.ReplaceColumnDialog;
import org.knime.json.util.Json2Xml;

/**
 * <code>NodeDialog</code> for the "JSONToXML" Node.
 *
 * @author Gabor Bakos
 */
public class JSONToXMLNodeDialog extends ReplaceColumnDialog<JSONToXMLSettings> {
    private JTextField m_array, m_binary, m_boolean, m_decimal, m_integer, m_item, m_namespace, m_null, m_root, m_string;
    private JCheckBox m_omitTypeInfo, m_specifyNamespace;
    /**
     * New pane for configuring the JSONToXML node.
     */
    protected JSONToXMLNodeDialog() {
        super(JSONToXMLNodeModel.createJSONToXMLSettings(), "JSON column", JSONValue.class);
        addTab("Namespace/Prefix", createPrefixComponent());
    }

    /**
     * @return
     */
    private JPanel createPrefixComponent() {
        JPanel ret = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = createInitialConstraints();
        gbc.gridx = 0;
        m_array = GUIFactory.createTextField(null, 11);
        m_array.setToolTipText("Set only if the input JSON is [], to distinguish from {}");
        ret.add(new JLabel("Empty list"), gbc);
        gbc.gridx = 1;
        ret.add(m_array, gbc);
        gbc.gridx = 2;
        ret.add(new JLabel(Json2Xml.LIST_NAMESPACE), gbc);
        gbc.gridy++;
        gbc.gridx = 0;
        m_binary = GUIFactory.createTextField(null, 11);
        ret.add(new JLabel("Binary content"), gbc);
        gbc.gridx = 1;
        ret.add(m_binary, gbc);
        gbc.gridx = 2;
        ret.add(new JLabel(Json2Xml.BINARY_NAMESPACE), gbc);
        gbc.gridy++;

        gbc.gridx = 0;
        m_boolean = GUIFactory.createTextField(null, 11);
        ret.add(new JLabel("Boolean"), gbc);
        gbc.gridx = 1;
        ret.add(m_boolean, gbc);
        gbc.gridx = 2;
        ret.add(new JLabel(Json2Xml.BOOLEAN_NAMESPACE), gbc);
        gbc.gridy++;

        gbc.gridx = 0;
        m_decimal = GUIFactory.createTextField(null, 11);
        ret.add(new JLabel("Decimal"), gbc);
        gbc.gridx = 1;
        ret.add(m_decimal, gbc);
        gbc.gridx = 2;
        ret.add(new JLabel(Json2Xml.DECIMAL_NAMESPACE), gbc);
        gbc.gridy++;

        gbc.gridx = 0;
        m_integer = GUIFactory.createTextField(null, 11);
        ret.add(new JLabel("Integer"), gbc);
        gbc.gridx = 1;
        ret.add(m_integer, gbc);
        gbc.gridx = 2;
        ret.add(new JLabel(Json2Xml.INTEGER_NAMESPACE), gbc);
        gbc.gridy++;

        gbc.gridx = 0;
        m_null = GUIFactory.createTextField(null, 11);
        ret.add(new JLabel("Null"), gbc);
        gbc.gridx = 1;
        ret.add(m_null, gbc);
        gbc.gridx = 2;
        ret.add(new JLabel(Json2Xml.NULL_NAMESPACE), gbc);
        gbc.gridy++;

        gbc.gridx = 0;
        m_string = GUIFactory.createTextField(null, 11);
        ret.add(new JLabel("String"), gbc);
        gbc.gridx = 1;
        ret.add(m_string, gbc);
        gbc.gridx = 2;
        ret.add(new JLabel(Json2Xml.STRING_NAMESPACE), gbc);
        gbc.gridy++;
        return ret;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void afterNewColumnName(final JPanel panel, final int afterNewColName) {
        int afterInputColumn = super.addAfterInputColumn(panel, afterNewColName);
        GridBagConstraints gbc = createInitialConstraints();
        gbc.gridy = afterInputColumn;
        gbc.gridwidth = 2;
        JPanel namespacePanel = createNamespacePanel();
        panel.add(namespacePanel,gbc);
        gbc.gridy++;

        gbc.gridwidth = 1;
        m_omitTypeInfo = new JCheckBox("Omit type information");
        panel.add(m_omitTypeInfo, gbc);
        gbc.gridy++;

        JPanel elementsPanel = createElementsPanel();
        gbc.gridwidth = 2;
        panel.add(elementsPanel, gbc);
        gbc.gridy++;
    }

    /**
     * @return
     */
    private JPanel createNamespacePanel() {
        JPanel ret = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = createInitialConstraints();
        gbc.gridx = 1;
        m_specifyNamespace = new JCheckBox("Specify namespace");
        ret.add(m_specifyNamespace, gbc);
        gbc.gridy++;
        gbc.gridx = 0;
        ret.add(new JLabel("Namespace"), gbc);
        gbc.gridx = 1;
        m_namespace = GUIFactory.createTextField(null, 44);
        ret.add(m_namespace, gbc);
        ret.setBorder(new TitledBorder("Namespace"));
        return ret;
    }

    /**
     * @return
     */
    private JPanel createElementsPanel() {
        JPanel elements = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = createInitialConstraints();
        gbc.gridx = 0;
        elements.add(new JLabel("Root element name:"), gbc);
        m_root = GUIFactory.createTextField("", 11);
        gbc.gridx = 1;
        elements.add(m_root, gbc);
        gbc.gridy++;

        gbc.gridx = 0;
        elements.add(new JLabel("Array item name:"), gbc);
        gbc.gridx = 1;
        m_item = GUIFactory.createTextField("", 11);
        elements.add(m_item, gbc);
        return elements;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        getSettings().setArray(m_array.getText());
        getSettings().setBinary(m_binary.getText());
        getSettings().setBoolean(m_boolean.getText());
        getSettings().setDecimal(m_decimal.getText());
        getSettings().setInteger(m_integer.getText());
        getSettings().setItem(m_item.getText());
        getSettings().setNamespace(m_namespace.getText());
        getSettings().setNull(m_null.getText());
        getSettings().setOmitTypeInfo(m_omitTypeInfo.isSelected());
        getSettings().setRoot(m_root.getText());
        getSettings().setSpecifyNamespace(m_specifyNamespace.isSelected());
        getSettings().setString(m_string.getText());
        super.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        super.loadSettingsFrom(settings, specs);
        m_array.setText(getSettings().getArray());
        m_binary.setText(getSettings().getBinary());
        m_boolean.setText(getSettings().getBoolean());
        m_decimal.setText(getSettings().getDecimal());
        m_integer.setText(getSettings().getInteger());
        m_item.setText(getSettings().getItem());
        m_namespace.setText(getSettings().getNamespace());
        m_null.setText(getSettings().getNull());
        m_root.setText(getSettings().getRoot());
        m_string.setText(getSettings().getString());
        m_omitTypeInfo.setSelected(getSettings().isOmitTypeInfo());
        m_specifyNamespace.setSelected(getSettings().isSpecifyNamespace());
    }
}
