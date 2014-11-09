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
import org.knime.json.node.util.ReplaceOrAddColumnSettings;

/**
 * Settings object for the JSONToXML node.
 *
 * @author Gabor Bakos
 */
final class JSONToXMLSettings extends ReplaceOrAddColumnSettings {
    //Further options would be:
    //Write xml declaration (currently: always true)
    //write xml 1.1 header (currently: always false)
    //How to handle invalid keys (like @a, #text, ...), currently invalid characters are removed.

    private String SPECIFY_NAMESPACE = "specify.namespace";
    private boolean DEFAULT_SPECIFY_NAMESPACE = false;
    private String NAMESPACE = "namespace";
    private String DEFAULT_NAMESPACE = null;
    private String ROOT_ELEMENT = "root.element";
    private String DEFAULT_ROOT_ELEMENT = "root";
    private String ITEM_ELEMENT = "array.item.element";
    private String DEFAULT_ITEM_ELEMENT = "item";
    private String ARRAY_PREFIX = "array.prefix";
    private String DEFAULT_ARRAY_PREFIX = "Array";
    private String BINARY_PREFIX = "binary.prefix";
    private String DEFAULT_BINARY_PREFIX = "Binary";
    private String BOOLEAN_PREFIX = "boolean.prefix";
    private String DEFAULT_BOOLEAN_PREFIX = "Boolean";
    private String INTEGER_PREFIX = "integer.prefix";
    private String DEFAULT_INTEGER_PREFIX = "Integer";
    private String NULL_PREFIX = "null.prefix";
    private String DEFAULT_NULL_PREFIX = "null";
    private String DECIMAL_PREFIX = "decimal.prefix";
    private String DEFAULT_DECIMAL_PREFIX = "Decimal";
    private String STRING_PREFIX = "string.prefix";
    private String DEFAULT_STRING_PREFIX = "String";

    private String OMIT_TYPE_INFORMATION = "omit.type.information";
    private boolean DEFAULT_OMIT_TYPE_INFORMATION = true;

    private String m_namespace = DEFAULT_NAMESPACE, m_root = DEFAULT_ROOT_ELEMENT, m_item = DEFAULT_ITEM_ELEMENT, m_array = DEFAULT_ARRAY_PREFIX, m_binary = DEFAULT_BINARY_PREFIX, m_boolean = DEFAULT_BOOLEAN_PREFIX, m_integer = DEFAULT_INTEGER_PREFIX, m_null = DEFAULT_NULL_PREFIX, m_decimal = DEFAULT_DECIMAL_PREFIX, m_string = DEFAULT_STRING_PREFIX;
    private boolean m_omitTypeInfo = DEFAULT_OMIT_TYPE_INFORMATION, m_specifyNamespace = DEFAULT_SPECIFY_NAMESPACE;

    /**
     * Constructs the object.
     */
    public JSONToXMLSettings() {
        super(JSONValue.class);
        setRemoveInputColumn(true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsForDialogs(final NodeSettingsRO settings, final PortObjectSpec[] specs) {
        super.loadSettingsForDialogs(settings, specs);
        m_array = settings.getString(ARRAY_PREFIX, DEFAULT_ARRAY_PREFIX);
        m_binary = settings.getString(BINARY_PREFIX, DEFAULT_BINARY_PREFIX);
        m_boolean = settings.getString(BOOLEAN_PREFIX, DEFAULT_BOOLEAN_PREFIX);
        m_decimal = settings.getString(DECIMAL_PREFIX, DEFAULT_DECIMAL_PREFIX);
        m_integer = settings.getString(INTEGER_PREFIX, DEFAULT_INTEGER_PREFIX);
        m_item = settings.getString(ITEM_ELEMENT, DEFAULT_ITEM_ELEMENT);
        m_namespace = settings.getString(NAMESPACE, DEFAULT_NAMESPACE);
        m_null = settings.getString(NULL_PREFIX, DEFAULT_NULL_PREFIX);
        m_root = settings.getString(ROOT_ELEMENT, DEFAULT_ROOT_ELEMENT);
        m_string = settings.getString(STRING_PREFIX, DEFAULT_STRING_PREFIX);
        m_omitTypeInfo = settings.getBoolean(OMIT_TYPE_INFORMATION, DEFAULT_OMIT_TYPE_INFORMATION);
        m_specifyNamespace = settings.getBoolean(SPECIFY_NAMESPACE, DEFAULT_SPECIFY_NAMESPACE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.loadSettingsFrom(settings);
        m_array = settings.getString(ARRAY_PREFIX);
        m_binary = settings.getString(BINARY_PREFIX);
        m_boolean = settings.getString(BOOLEAN_PREFIX);
        m_decimal = settings.getString(DECIMAL_PREFIX);
        m_integer = settings.getString(INTEGER_PREFIX);
        m_item = settings.getString(ITEM_ELEMENT);
        m_namespace = settings.getString(NAMESPACE);
        m_null = settings.getString(NULL_PREFIX);
        m_root = settings.getString(ROOT_ELEMENT);
        m_string = settings.getString(STRING_PREFIX);
        m_omitTypeInfo = settings.getBoolean(OMIT_TYPE_INFORMATION);
        m_specifyNamespace = settings.getBoolean(SPECIFY_NAMESPACE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        super.saveSettingsTo(settings);
        settings.addString(ARRAY_PREFIX, m_array);
        settings.addString(BINARY_PREFIX, m_binary);
        settings.addString(BOOLEAN_PREFIX, m_boolean);
        settings.addString(DECIMAL_PREFIX, m_decimal);
        settings.addString(INTEGER_PREFIX, m_integer);
        settings.addString(ITEM_ELEMENT, m_item);
        settings.addString(NAMESPACE, m_namespace);
        settings.addString(NULL_PREFIX, m_null);
        settings.addString(ROOT_ELEMENT, m_root);
        settings.addString(STRING_PREFIX, m_string);
        settings.addBoolean(OMIT_TYPE_INFORMATION, m_omitTypeInfo);
        settings.addBoolean(SPECIFY_NAMESPACE, m_specifyNamespace);
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
     * @return the binary
     */
    final String getBinary() {
        return m_binary;
    }

    /**
     * @param binary the binary to set
     */
    final void setBinary(final String binary) {
        this.m_binary = binary;
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
     * @return the omitTypeInfo
     */
    final boolean isOmitTypeInfo() {
        return m_omitTypeInfo;
    }

    /**
     * @param omitTypeInfo the omitTypeInfo to set
     */
    final void setOmitTypeInfo(final boolean omitTypeInfo) {
        this.m_omitTypeInfo = omitTypeInfo;
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
}
