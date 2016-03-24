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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import org.knime.core.data.json.JSONValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.CheckUtils;
import org.knime.json.node.util.GUIFactory;
import org.knime.json.node.util.ReplaceColumnDialog;
import org.knime.json.util.Json2Xml;

/**
 * <code>NodeDialog</code> for the "JSONToXML" Node.
 *
 * @author Gabor Bakos
 */
public class JSONToXMLNodeDialog extends ReplaceColumnDialog<JSONToXMLSettings> {
    private JTextField m_array, m_boolean, m_decimal, m_integer, m_item, m_namespace, m_null, m_root,
            m_string, m_keyForText;

    private JCheckBox m_keepTypeInfo, m_specifyNamespace, m_createTextForSpecificKeys;
    private JCheckBox m_useParentKeyAsElementName;
    private JRadioButton m_translateHashCommentAsElement, m_translateHashCommentAsComment;
    private JRadioButton m_translateQuestionPrefixAsElement, m_translateQuestionPrefixAsPI;

    /**
     * New pane for configuring the JSONToXML node.
     */
    protected JSONToXMLNodeDialog() {
        super(JSONToXMLNodeModel.createJSONToXMLSettings(), "JSON column", JSONValue.class);
        addTab("Type information", createPrefixComponent());
    }

    /**
     * @return
     */
    private JPanel createPrefixComponent() {
        JPanel ret = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = createInitialConstraints();
        gbc.gridwidth = 1;
        m_keepTypeInfo = new JCheckBox("Keep type information by adding namespace");
        ret.add(m_keepTypeInfo, gbc);
        gbc.gridy++;

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

        updatePrefixEnabledness(false);

        m_keepTypeInfo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                boolean enabled = m_keepTypeInfo.isSelected();
                updatePrefixEnabledness(enabled);
            }
        });
        return ret;
    }

    /**
     * @param enabled
     */
    private void updatePrefixEnabledness(final boolean enabled) {
        for (JComponent comp : new JComponent[]{m_array, m_boolean, m_decimal, m_integer, m_null, m_string}) {
            comp.setEnabled(enabled);
        }
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
        panel.add(namespacePanel, gbc);
        gbc.gridy++;

        JPanel elementsPanel = createElementsPanel();
        gbc.gridwidth = 2;
        panel.add(elementsPanel, gbc);
        gbc.gridy++;

        JPanel textPanel = createTextPanel();
        panel.add(textPanel, gbc);
        gbc.gridy++;

        JLabel hashComment = new JLabel("Translate JSON keys starting with # to   ");
        gbc.gridwidth = 1;
        gbc.gridx = 0;
        panel.add(hashComment, gbc);
        m_translateHashCommentAsElement = new JRadioButton("an element (<comment>content</comment>)");
        gbc.gridx = 1;
        panel.add(m_translateHashCommentAsElement, gbc);
        m_translateHashCommentAsComment = new JRadioButton("a comment (<!--content-->)");
        gbc.gridy++;
        panel.add(m_translateHashCommentAsComment, gbc);
        ButtonGroup hashCommentGroup = new ButtonGroup();
        hashCommentGroup.add(m_translateHashCommentAsComment);
        hashCommentGroup.add(m_translateHashCommentAsElement);
        gbc.gridy++;

        JLabel questionPrefix = new JLabel("Translate JSON keys starting with ? to   ");
        gbc.gridx = 0;
        panel.add(questionPrefix, gbc);
        gbc.gridx = 1;
        m_translateQuestionPrefixAsElement = new JRadioButton("an element (<pi>content<pi>)");
        panel.add(m_translateQuestionPrefixAsElement, gbc);
        gbc.gridy++;
        m_translateQuestionPrefixAsPI = new JRadioButton("a processing instruction (<?pi content?>)");
        panel.add(m_translateQuestionPrefixAsPI, gbc);
        ButtonGroup questionPrefixGroup = new ButtonGroup();
        questionPrefixGroup.add(m_translateQuestionPrefixAsPI);
        questionPrefixGroup.add(m_translateQuestionPrefixAsElement);
        gbc.gridy++;
    }

    /**
     * @return
     */
    private JPanel createTextPanel() {
        JPanel ret = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = createInitialConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        m_createTextForSpecificKeys = new JCheckBox("Represent values as XML text if key is");
        ret.add(m_createTextForSpecificKeys, gbc);
        gbc.gridx = 1;
        m_keyForText = GUIFactory.createTextField("#text", 11);
        ret.add(m_keyForText, gbc);

        m_createTextForSpecificKeys.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                m_keyForText.setEnabled(m_createTextForSpecificKeys.isSelected());
            }
        });
        return ret;
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
        m_namespace.setEnabled(false);
        ret.add(m_namespace, gbc);
        ret.setBorder(new TitledBorder("Namespace"));
        m_specifyNamespace.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                m_namespace.setEnabled(m_specifyNamespace.isSelected());
            }
        });
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
        gbc.gridy++;

        m_useParentKeyAsElementName = new JCheckBox("Use parent keys as element name for arrays", false);

        m_useParentKeyAsElementName.setSelected(false);

        elements.add(m_useParentKeyAsElementName, gbc);
        return elements;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        if (!m_keepTypeInfo.isSelected()) {
            CheckUtils.checkSetting(!m_array.getText().trim().isEmpty(), "The empty list prefix is missing.");
            CheckUtils.checkSetting(!m_boolean.getText().trim().isEmpty(), "The boolean prefix is missing.");
            CheckUtils.checkSetting(!m_decimal.getText().trim().isEmpty(), "The decimal prefix is missing.");
            CheckUtils.checkSetting(!m_integer.getText().trim().isEmpty(), "The integer prefix is missing.");
            CheckUtils.checkSetting(!m_null.getText().trim().isEmpty(), "The null prefix is missing.");
            CheckUtils.checkSetting(!m_string.getText().trim().isEmpty(), "The string prefix is missing.");
        }
        CheckUtils.checkSetting(!m_item.getText().trim().isEmpty(), "The array item name is missing.");
        CheckUtils.checkSetting(!m_root.getText().trim().isEmpty(), "The root item name is missing.");
        CheckUtils.checkSetting(!m_specifyNamespace.isSelected() || !m_namespace.getText().trim().isEmpty(),
            "The namespace information is missing.");
        getSettings().setArray(m_array.getText());
        getSettings().setBoolean(m_boolean.getText());
        getSettings().setDecimal(m_decimal.getText());
        getSettings().setInteger(m_integer.getText());
        getSettings().setItem(m_item.getText());
        getSettings().setNamespace(m_namespace.getText());
        getSettings().setNull(m_null.getText());
        getSettings().setKeepTypeInfo(m_keepTypeInfo.isSelected());
        getSettings().setRoot(m_root.getText());
        getSettings().setSpecifyNamespace(m_specifyNamespace.isSelected());
        getSettings().setString(m_string.getText());
        getSettings().setCreateTextForSpecificKeys(m_createTextForSpecificKeys.isSelected());
        getSettings().setKeyForText(m_keyForText.getText());
        getSettings().setParentKeyAsElementName(m_useParentKeyAsElementName.isSelected());
        getSettings().setTranslateHashCommentToComment(m_translateHashCommentAsComment.isSelected());
        getSettings().setTranslateQuestionPrefixToProcessingInstruction(m_translateQuestionPrefixAsPI.isSelected());
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
        m_boolean.setText(getSettings().getBoolean());
        m_decimal.setText(getSettings().getDecimal());
        m_integer.setText(getSettings().getInteger());
        m_item.setText(getSettings().getItem());
        m_namespace.setText(getSettings().getNamespace());
        m_null.setText(getSettings().getNull());
        m_root.setText(getSettings().getRoot());
        m_string.setText(getSettings().getString());
        m_keepTypeInfo.setSelected(getSettings().isKeepTypeInfo());
        updatePrefixEnabledness(getSettings().isKeepTypeInfo());
        m_specifyNamespace.setSelected(getSettings().isSpecifyNamespace());
        m_namespace.setEnabled(m_specifyNamespace.isSelected());
        m_createTextForSpecificKeys.setSelected(getSettings().isCreateTextForSpecificKeys());
        m_keyForText.setText(getSettings().getKeyForText());
        m_keyForText.setEnabled(m_createTextForSpecificKeys.isSelected());
        m_useParentKeyAsElementName.setSelected(getSettings().isParentKeyAsElementName());
        m_translateHashCommentAsComment.setSelected(getSettings().isTranslateHashCommentToComment());
        m_translateHashCommentAsElement.setSelected(!getSettings().isTranslateHashCommentToComment());
        m_translateQuestionPrefixAsPI.setSelected(getSettings().isTranslateQuestionPrefixToProcessingInstruction());
        m_translateQuestionPrefixAsElement.setSelected(!getSettings().isTranslateQuestionPrefixToProcessingInstruction());
    }
}
