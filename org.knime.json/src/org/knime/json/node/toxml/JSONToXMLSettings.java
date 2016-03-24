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
 *   28 Sept 2014 (Gabor): created
 */
package org.knime.json.node.toxml;

import org.knime.core.data.json.JSONValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.json.node.util.ReplaceColumnSettings;

/**
 * Settings object for the JSONToXML node.
 *
 * @author Gabor Bakos
 */
final class JSONToXMLSettings extends ReplaceColumnSettings {
    //Further options would be:
    //Write xml declaration (currently: always true)
    //write xml 1.1 header (currently: always false)
    //How to handle invalid keys (like @a, #text, ...), currently invalid characters are removed.
    //binary values prefix

    private static final String SPECIFY_NAMESPACE = "specify.namespace";

    private static final boolean DEFAULT_SPECIFY_NAMESPACE = false;

    private static final String NAMESPACE = "namespace";

    private static final String DEFAULT_NAMESPACE = null;

    private static final String ROOT_ELEMENT = "root.element";

    private static final String DEFAULT_ROOT_ELEMENT = "root";

    private static final String ITEM_ELEMENT = "array.item.element";

    private static final String DEFAULT_ITEM_ELEMENT = "item";

    private static final String ARRAY_PREFIX = "array.prefix";

    private static final String DEFAULT_ARRAY_PREFIX = "Array";

    private static final String BOOLEAN_PREFIX = "boolean.prefix";

    private static final String DEFAULT_BOOLEAN_PREFIX = "Boolean";

    private static final String INTEGER_PREFIX = "integer.prefix";

    private static final String DEFAULT_INTEGER_PREFIX = "Integer";

    private static final String NULL_PREFIX = "null.prefix";

    private static final String DEFAULT_NULL_PREFIX = "null";

    private static final String DECIMAL_PREFIX = "decimal.prefix";

    private static final String DEFAULT_DECIMAL_PREFIX = "Decimal";

    private static final String STRING_PREFIX = "string.prefix";

    private static final String DEFAULT_STRING_PREFIX = "String";

    private static final String KEEP_TYPE_INFORMATION = "keep.type.information";

    private static final boolean DEFAULT_KEEP_TYPE_INFORMATION = false;

    private static final String CREATE_TEXT_FOR_SPECIFIC_KEYS = "create.text.for.specific.keys";

    private static final boolean DEFAULT_CREATE_TEXT_FOR_SPECIFIC_KEYS = true;

    private static final String KEY_FOR_TEXT = "key.for.text";

    private static final String DEFAULT_KEY_FOR_TEXT = "#text";

    private static final String PARENT_KEY_AS_ELEMENT_NAME = "parent.key.as.element.name";

    private static final boolean DEFAULT_PARENT_KEY_AS_ELEMENT_NAME = false;

    private static final String TRANSLATE_HASHCOMMENT_AS_ELEMENT = "translate.#comment.as.element";

    private static final boolean DEFAULT_TRANSLATE_HASHCOMMENT_AS_ELEMENT = true;

    private static final String TRANSLATE_QUESTIONPREFIX_AS_ELEMENT = "translate.?prefix.as.element";

    private static final boolean DEFAULT_TRANSLATE_QUESTIONPREFIX_AS_ELEMENT = true;

    private String m_namespace = DEFAULT_NAMESPACE, m_root = DEFAULT_ROOT_ELEMENT, m_item = DEFAULT_ITEM_ELEMENT,
            m_array = DEFAULT_ARRAY_PREFIX, m_boolean = DEFAULT_BOOLEAN_PREFIX,
            m_integer = DEFAULT_INTEGER_PREFIX, m_null = DEFAULT_NULL_PREFIX, m_decimal = DEFAULT_DECIMAL_PREFIX,
            m_string = DEFAULT_STRING_PREFIX;

    private boolean m_keepTypeInfo = DEFAULT_KEEP_TYPE_INFORMATION, m_specifyNamespace = DEFAULT_SPECIFY_NAMESPACE;

    private boolean m_createTextForSpecificKeys = DEFAULT_CREATE_TEXT_FOR_SPECIFIC_KEYS;

    private String m_keyForText = DEFAULT_KEY_FOR_TEXT;

    private boolean m_parentKeyAsElementName = DEFAULT_PARENT_KEY_AS_ELEMENT_NAME;

    private boolean m_translateHashCommentToComment = !DEFAULT_TRANSLATE_HASHCOMMENT_AS_ELEMENT;

    private boolean m_translateQuestionPrefixToProcessingInstruction = !DEFAULT_TRANSLATE_QUESTIONPREFIX_AS_ELEMENT;

    /**
     * Constructs the object.
     */
    public JSONToXMLSettings() {
        super(JSONValue.class);
        setNewColumnName("XML");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsForDialogs(final NodeSettingsRO settings, final PortObjectSpec[] specs) {
        super.loadSettingsForDialogs(settings, specs);
        m_array = settings.getString(ARRAY_PREFIX, DEFAULT_ARRAY_PREFIX);
        m_boolean = settings.getString(BOOLEAN_PREFIX, DEFAULT_BOOLEAN_PREFIX);
        m_decimal = settings.getString(DECIMAL_PREFIX, DEFAULT_DECIMAL_PREFIX);
        m_integer = settings.getString(INTEGER_PREFIX, DEFAULT_INTEGER_PREFIX);
        m_item = settings.getString(ITEM_ELEMENT, DEFAULT_ITEM_ELEMENT);
        m_namespace = settings.getString(NAMESPACE, DEFAULT_NAMESPACE);
        m_null = settings.getString(NULL_PREFIX, DEFAULT_NULL_PREFIX);
        m_root = settings.getString(ROOT_ELEMENT, DEFAULT_ROOT_ELEMENT);
        m_string = settings.getString(STRING_PREFIX, DEFAULT_STRING_PREFIX);
        m_keepTypeInfo = settings.getBoolean(KEEP_TYPE_INFORMATION, DEFAULT_KEEP_TYPE_INFORMATION);
        m_specifyNamespace = settings.getBoolean(SPECIFY_NAMESPACE, DEFAULT_SPECIFY_NAMESPACE);
        m_createTextForSpecificKeys =
            settings.getBoolean(CREATE_TEXT_FOR_SPECIFIC_KEYS, DEFAULT_CREATE_TEXT_FOR_SPECIFIC_KEYS);
        m_keyForText = settings.getString(KEY_FOR_TEXT, DEFAULT_KEY_FOR_TEXT);
        m_parentKeyAsElementName = settings.getBoolean(PARENT_KEY_AS_ELEMENT_NAME, DEFAULT_PARENT_KEY_AS_ELEMENT_NAME);
        m_translateHashCommentToComment = !settings.getBoolean(TRANSLATE_HASHCOMMENT_AS_ELEMENT, DEFAULT_TRANSLATE_HASHCOMMENT_AS_ELEMENT);
        m_translateQuestionPrefixToProcessingInstruction = !settings.getBoolean(TRANSLATE_QUESTIONPREFIX_AS_ELEMENT, DEFAULT_TRANSLATE_QUESTIONPREFIX_AS_ELEMENT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.loadSettingsFrom(settings);
        m_array = settings.getString(ARRAY_PREFIX);
        m_boolean = settings.getString(BOOLEAN_PREFIX);
        m_decimal = settings.getString(DECIMAL_PREFIX);
        m_integer = settings.getString(INTEGER_PREFIX);
        m_item = settings.getString(ITEM_ELEMENT);
        m_namespace = settings.getString(NAMESPACE);
        m_null = settings.getString(NULL_PREFIX);
        m_root = settings.getString(ROOT_ELEMENT);
        m_string = settings.getString(STRING_PREFIX);
        m_keepTypeInfo = settings.getBoolean(KEEP_TYPE_INFORMATION);
        m_specifyNamespace = settings.getBoolean(SPECIFY_NAMESPACE);
        m_createTextForSpecificKeys =
            settings.getBoolean(CREATE_TEXT_FOR_SPECIFIC_KEYS, DEFAULT_CREATE_TEXT_FOR_SPECIFIC_KEYS);
        m_keyForText = settings.getString(KEY_FOR_TEXT, DEFAULT_KEY_FOR_TEXT);
        m_parentKeyAsElementName = settings.getBoolean(PARENT_KEY_AS_ELEMENT_NAME, DEFAULT_PARENT_KEY_AS_ELEMENT_NAME);
        m_translateHashCommentToComment = !settings.getBoolean(TRANSLATE_HASHCOMMENT_AS_ELEMENT, DEFAULT_TRANSLATE_HASHCOMMENT_AS_ELEMENT);
        m_translateQuestionPrefixToProcessingInstruction = !settings.getBoolean(TRANSLATE_QUESTIONPREFIX_AS_ELEMENT, DEFAULT_TRANSLATE_QUESTIONPREFIX_AS_ELEMENT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        super.saveSettingsTo(settings);
        settings.addString(ARRAY_PREFIX, m_array);
        settings.addString(BOOLEAN_PREFIX, m_boolean);
        settings.addString(DECIMAL_PREFIX, m_decimal);
        settings.addString(INTEGER_PREFIX, m_integer);
        settings.addString(ITEM_ELEMENT, m_item);
        settings.addString(NAMESPACE, m_namespace);
        settings.addString(NULL_PREFIX, m_null);
        settings.addString(ROOT_ELEMENT, m_root);
        settings.addString(STRING_PREFIX, m_string);
        settings.addBoolean(KEEP_TYPE_INFORMATION, m_keepTypeInfo);
        settings.addBoolean(SPECIFY_NAMESPACE, m_specifyNamespace);
        settings.addBoolean(CREATE_TEXT_FOR_SPECIFIC_KEYS, m_createTextForSpecificKeys);
        settings.addString(KEY_FOR_TEXT, m_keyForText);
        settings.addBoolean(PARENT_KEY_AS_ELEMENT_NAME, m_parentKeyAsElementName);
        settings.addBoolean(TRANSLATE_HASHCOMMENT_AS_ELEMENT, !m_translateHashCommentToComment);
        settings.addBoolean(TRANSLATE_QUESTIONPREFIX_AS_ELEMENT, !m_translateQuestionPrefixToProcessingInstruction);
    }

    /**
     * @return the namespace
     */
    final String getNamespace() {
        return m_namespace;
    }

    /**
     * @param namespace the namespace to set
     */
    final void setNamespace(final String namespace) {
        this.m_namespace = namespace;
    }

    /**
     * @return the root
     */
    final String getRoot() {
        return m_root;
    }

    /**
     * @param root the root to set
     */
    final void setRoot(final String root) {
        this.m_root = root;
    }

    /**
     * @return the item
     */
    final String getItem() {
        return m_item;
    }

    /**
     * @param item the item to set
     */
    final void setItem(final String item) {
        this.m_item = item;
    }

    /**
     * @return the array
     */
    final String getArray() {
        return m_array;
    }

    /**
     * @param array the array to set
     */
    final void setArray(final String array) {
        this.m_array = array;
    }

    /**
     * @return the boolean prefix
     */
    final String getBoolean() {
        return m_boolean;
    }

    /**
     * @param booleanPrefix the boolean prefix to set
     */
    final void setBoolean(final String booleanPrefix) {
        this.m_boolean = booleanPrefix;
    }

    /**
     * @return the integer
     */
    final String getInteger() {
        return m_integer;
    }

    /**
     * @param integer the integer to set
     */
    final void setInteger(final String integer) {
        this.m_integer = integer;
    }

    /**
     * @return the null prefix
     */
    final String getNull() {
        return m_null;
    }

    /**
     * @param nullPrefix the null prefix to set
     */
    final void setNull(final String nullPrefix) {
        this.m_null = nullPrefix;
    }

    /**
     * @return the decimal
     */
    final String getDecimal() {
        return m_decimal;
    }

    /**
     * @param decimal the decimal to set
     */
    final void setDecimal(final String decimal) {
        this.m_decimal = decimal;
    }

    /**
     * @return the string
     */
    final String getString() {
        return m_string;
    }

    /**
     * @param string the string to set
     */
    final void setString(final String string) {
        this.m_string = string;
    }

    /**
     * @return the keepTypeInfo
     */
    final boolean isKeepTypeInfo() {
        return m_keepTypeInfo;
    }

    /**
     * @param keepTypeInfo the keepTypeInfo to set
     */
    final void setKeepTypeInfo(final boolean keepTypeInfo) {
        this.m_keepTypeInfo = keepTypeInfo;
    }

    /**
     * @return the specifyNamespace
     */
    final boolean isSpecifyNamespace() {
        return m_specifyNamespace;
    }

    /**
     * @param specifyNamespace the specifyNamespace to set
     */
    final void setSpecifyNamespace(final boolean specifyNamespace) {
        this.m_specifyNamespace = specifyNamespace;
    }

    /**
     * @return the createTextForSpecificKeys
     */
    final boolean isCreateTextForSpecificKeys() {
        return m_createTextForSpecificKeys;
    }

    /**
     * @param createTextForSpecificKeys the createTextForSpecificKeys to set
     */
    final void setCreateTextForSpecificKeys(final boolean createTextForSpecificKeys) {
        this.m_createTextForSpecificKeys = createTextForSpecificKeys;
    }

    /**
     * @return the keyForText
     */
    final String getKeyForText() {
        return m_keyForText;
    }

    /**
     * @param keyForText the keyForText to set
     */
    final void setKeyForText(final String keyForText) {
        this.m_keyForText = keyForText;
    }

    /**
     * @return the parentKeyAsElementName
     */
    final boolean isParentKeyAsElementName() {
        return m_parentKeyAsElementName;
    }

    /**
     * @param parentKeyAsElementName the parentKeyAsElementName to set
     */
    final void setParentKeyAsElementName(final boolean parentKeyAsElementName) {
        this.m_parentKeyAsElementName = parentKeyAsElementName;
    }

    /**
     * @return the translateHashCommentToComment
     * @since 3.2
     */
    boolean isTranslateHashCommentToComment() {
        return m_translateHashCommentToComment;
    }

    /**
     * @param translateHashCommentToComment the translateHashCommentToComment to set
     * @since 3.2
     */
    void setTranslateHashCommentToComment(final boolean translateHashCommentToComment) {
        m_translateHashCommentToComment = translateHashCommentToComment;
    }

    /**
     * @return the translateQuestionPrefixToProcessingInstruction
     * @since 3.2
     */
    boolean isTranslateQuestionPrefixToProcessingInstruction() {
        return m_translateQuestionPrefixToProcessingInstruction;
    }

    /**
     * @param translateQuestionPrefixToProcessingInstruction the translateQuestionPrefixToProcessingInstruction to set
     * @since 3.2
     */
    void setTranslateQuestionPrefixToProcessingInstruction(
        final boolean translateQuestionPrefixToProcessingInstruction) {
        m_translateQuestionPrefixToProcessingInstruction = translateQuestionPrefixToProcessingInstruction;
    }
}
