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
 * ------------------------------------------------------------------------
 */

package org.knime.json.node.toxml;

import org.knime.core.data.json.JSONValue;
import org.knime.json.node.util.SingleColumnReplaceOrAddNodeModel;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.After;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.persistence.legacy.EnumBooleanPersistor;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.RadioButtonsWidget;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;
import org.knime.node.parameters.widget.choices.util.CompatibleColumnsProvider;
import org.knime.node.parameters.widget.text.TextInputWidget;

/**
 * Node parameters for JSON to XML.
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.1
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction")
class JSONToXMLNodeParameters implements NodeParameters {

    @Section(title = "Namespace Configuration")
    interface NamespaceSection {
    }

    @Section(title = "Translation Configuration")
    @After(NamespaceSection.class)
    interface TranslationSection {
    }

    @Widget(title = "Input column", description = "Column containing JSON values")
    @ChoicesProvider(JSONColumnsProvider.class)
    @Persist(configKey = SingleColumnReplaceOrAddNodeModel.INPUT_COLUMN)
    String m_inputColumn = "";

    @Widget(title = "Output column", description = "")
    @ValueSwitchWidget
    @ValueReference(ReplaceInputOrAppendOutputColumnRef.class)
    @Persistor(ReplaceOrAppendInputColumnPersistor.class)
    ReplaceInputOrAppendOutputColumn m_appendOrReplace = ReplaceInputOrAppendOutputColumn.REPLACE;

    @Widget(title = "New column name", description = "Name of the new (XML) column")
    @TextInputWidget
    @Effect(predicate = IsAppendColumn.class, type = EffectType.SHOW)
    @Persist(configKey = SingleColumnReplaceOrAddNodeModel.NEW_COLUMN_NAME)
    String m_newColumnName = JSONToXMLSettings.DEFAULT_NEW_COLUMN_NAME;

    @Layout(NamespaceSection.class)
    @Widget(title = "Specify namespace", description = "If unchecked, no base namespace will be set.")
    @Persist(configKey = JSONToXMLSettings.SPECIFY_NAMESPACE)
    @ValueReference(SpecifyNamespaceRef.class)
    boolean m_specifyNamespace = JSONToXMLSettings.DEFAULT_SPECIFY_NAMESPACE;

    @Layout(NamespaceSection.class)
    @Widget(title = "Namespace", description = "The base namespace")
    @TextInputWidget
    @Effect(predicate = IsSpecifyNamespace.class, type = EffectType.SHOW)
    @Persist(configKey = JSONToXMLSettings.NAMESPACE)
    String m_namespace = JSONToXMLSettings.DEFAULT_NAMESPACE;

    @Layout(NamespaceSection.class)
    @Widget(title = "Keep type information by adding namespace", description = """
            When checked, the JSON type information will be preserved, numbers, booleans, ... will be
            represented as text, but their node will have a prefix identifying its type. Otherwise all kind
            of values will be represented as text.
            """)
    @Persist(configKey = JSONToXMLSettings.KEEP_TYPE_INFORMATION)
    @ValueReference(KeepTypeInformationRef.class)
    boolean m_keepTypeInformation = JSONToXMLSettings.DEFAULT_KEEP_TYPE_INFORMATION;

    @Layout(NamespaceSection.class)
    @Widget(title = "Empty list (https://www.w3.org/2001/XMLSchema/list)", description = """
            Only used for the <code>[]</code> JSON array when the Omit type information is unchecked. Otherwise it is
            not possible to distinguish between <code>[]</code> and <code>{}</code>.
            """)
    @TextInputWidget
    @Persist(configKey = JSONToXMLSettings.ARRAY_PREFIX)
    @Effect(predicate = IsKeepTypeInfo.class, type = EffectType.SHOW)
    String m_arrayPrefix = JSONToXMLSettings.DEFAULT_ARRAY_PREFIX;

    @Layout(NamespaceSection.class)
    @Widget(title = "Boolean (https://www.w3.org/2001/XMLSchema/boolean)",
        description = "Prefix for the boolean values.")
    @TextInputWidget
    @Persist(configKey = JSONToXMLSettings.BOOLEAN_PREFIX)
    @Effect(predicate = IsKeepTypeInfo.class, type = EffectType.SHOW)
    String m_booleanPrefix = JSONToXMLSettings.DEFAULT_BOOLEAN_PREFIX;

    @Layout(NamespaceSection.class)
    @Widget(title = "Decimal (https://www.w3.org/2001/XMLSchema/decimal)",
        description = "Prefix for the floating point/decimal values.")
    @TextInputWidget
    @Persist(configKey = JSONToXMLSettings.DECIMAL_PREFIX)
    @Effect(predicate = IsKeepTypeInfo.class, type = EffectType.SHOW)
    String m_decimalPrefix = JSONToXMLSettings.DEFAULT_DECIMAL_PREFIX;

    @Layout(NamespaceSection.class)
    @Widget(title = "Integer (https://www.w3.org/2001/XMLSchema/integer)",
        description = "Prefix for the integer values.")
    @TextInputWidget
    @Persist(configKey = JSONToXMLSettings.INTEGER_PREFIX)
    @Effect(predicate = IsKeepTypeInfo.class, type = EffectType.SHOW)
    String m_integerPrefix = JSONToXMLSettings.DEFAULT_INTEGER_PREFIX;

    @Layout(NamespaceSection.class)
    @Widget(title = "Null (https://www.w3.org/2001/XMLSchema)", description = "Prefix for the null values.")
    @TextInputWidget
    @Persist(configKey = JSONToXMLSettings.NULL_PREFIX)
    @Effect(predicate = IsKeepTypeInfo.class, type = EffectType.SHOW)
    String m_nullPrefix = JSONToXMLSettings.DEFAULT_NULL_PREFIX;

    @Layout(NamespaceSection.class)
    @Widget(title = "String (https://www.w3.org/2001/XMLSchema/string)",
        description = "Prefix for the String/text values.")
    @TextInputWidget
    @Persist(configKey = JSONToXMLSettings.STRING_PREFIX)
    @Effect(predicate = IsKeepTypeInfo.class, type = EffectType.SHOW)
    String m_stringPrefix = JSONToXMLSettings.DEFAULT_STRING_PREFIX;

    @Layout(TranslationSection.class)
    @Widget(title = "Root element name", description = "There is always a root element, you can specify its name.")
    @TextInputWidget
    @Persist(configKey = JSONToXMLSettings.ROOT_ELEMENT)
    String m_rootElementName = JSONToXMLSettings.DEFAULT_ROOT_ELEMENT;

    @Layout(TranslationSection.class)
    @Widget(title = "Array item name", description = """
            Array items not always can be represented as trees (for example arrays within arrays, or
            primitive types within arrays). In this case an XML element with the specified name will be
            created for each array entry.
            """)
    @TextInputWidget
    @Persist(configKey = JSONToXMLSettings.ITEM_ELEMENT)
    String m_arrayItemName = JSONToXMLSettings.DEFAULT_ITEM_ELEMENT;

    @Layout(TranslationSection.class)
    @Widget(title = "Use parent keys as element name for arrays", description = """
            When checked, JSON such as <pre><code>{
                "a" : [
                    {"b" : 2},
                    {"c" : 3}
                ]
            }</code></pre> gets translated to
            <pre><code>&lt;a&gt;
             &lt;b&gt;2&lt;/b&gt;
            &lt;/a&gt;
            &lt;a&gt;
             &lt;c&gt;3&lt;/c&gt;
            &lt;/a&gt;
            </code></pre> otherwise, it is translated to:
            <pre><code>&lt;a&gt;
             &lt;item&gt;
              &lt;b&gt;2&lt;/b&gt;
             &lt;/item&gt;
             &lt;item&gt;
              &lt;c&gt;3&lt;/c&gt;
             &lt;/item&gt;
            &lt;/a&gt;
            </code></pre>
            """)
    @Persist(configKey = JSONToXMLSettings.PARENT_KEY_AS_ELEMENT_NAME)
    boolean m_useParentKeyAsElementName = JSONToXMLSettings.DEFAULT_PARENT_KEY_AS_ELEMENT_NAME;

    @Layout(TranslationSection.class)
    @Widget(title = "Represent values as XML text if key is", description = """
                   When checked, simple values with the specified key will not create attributes, but provide the
                   single text of the surrounding element.
                   """)
    @Persist(configKey = JSONToXMLSettings.CREATE_TEXT_FOR_SPECIFIC_KEYS)
    @ValueReference(CreateTextForSpecificKeysRef.class)
    boolean m_createTextForSpecificKeys = JSONToXMLSettings.DEFAULT_CREATE_TEXT_FOR_SPECIFIC_KEYS;

    @Layout(TranslationSection.class)
    @Widget(title = "Key for text", description = "The key name to represent as XML text")
    @TextInputWidget
    @Effect(predicate = IsCreateTextEnabled.class, type = EffectType.SHOW)
    @Persist(configKey = JSONToXMLSettings.KEY_FOR_TEXT)
    String m_keyForText = JSONToXMLSettings.DEFAULT_KEY_FOR_TEXT;

    @Layout(TranslationSection.class)
    @Widget(title = "Translate JSON keys starting with # to", description = """
                   JSON objects with keys starting with a hash sign such as
                   <pre><code>{"#comment": "content"}</code></pre>
                   can either be translated to XML elements
                   <pre><code>&lt;comment
                    ns:originalKey="#comment"&gt;
                     content
                   &lt;/comment&gt;
                   </code></pre>
                   or to XML comments
                   <pre><code>&lt;!--content--&gt;</code></pre>
                   """)
    @RadioButtonsWidget
    @Persistor(HashCommentHandlingPersistor.class)
    HashCommentHandling m_hashCommentHandling = HashCommentHandling.ELEMENT;

    @Layout(TranslationSection.class)
    @Widget(title = "Translate JSON keys starting with ? to", description = """
                   JSON objects with keys starting with a question mark such as
                   <pre><code>{"?pi": "content"}</code></pre>
                   can either be translated to XML elements
                   <pre><code>&lt;pi ns:originalKey="?pi"&gt;
                    content
                   &lt;/pi&gt;
                   </code></pre>
                   or to XML processing instructions
                   <pre><code>&lt;?pi content?&gt;</code></pre>
                   """)
    @RadioButtonsWidget
    @Persistor(QuestionPrefixHandlingPersistor.class)
    QuestionPrefixHandling m_questionPrefixHandling = QuestionPrefixHandling.ELEMENT;

    @Layout(TranslationSection.class)
    @Widget(title = "Remove invalid XML characters from values", description = """
            XML 1.0 supports only a limited set of characters (see
            <a href="https://www.w3.org/TR/xml/#charsets">RFC</a>
            ). When checked, invalid characters
            will be removed from values.
            """)
    @Persist(configKey = JSONToXMLSettings.VALUE_REMOVE_INVALID_CHARS)
    boolean m_removeInvalidChars = JSONToXMLSettings.DEFAULT_VALUE_REMOVE_INVALID_CHARS;

    enum HashCommentHandling {
        @Label("an element (<comment>content</comment>)")
        ELEMENT,
        @Label("a comment (<!--content-->)")
        COMMENT;
    }

    enum QuestionPrefixHandling {
            @Label("an element (<pi>content</pi>)")
            ELEMENT,
            @Label("a processing instruction (<?pi content?>)")
            PROCESSING_INSTRUCTION;
    }

    enum ReplaceInputOrAppendOutputColumn {
        @Label(value = "Replace input column", description = "Replace input column, keep its name")
        REPLACE, //
        @Label(value = "Append new column", description = "Name of the new (XML) column")
        APPEND;
    }

    static final class ReplaceInputOrAppendOutputColumnRef
        implements ParameterReference<ReplaceInputOrAppendOutputColumn> {
    }

    static final class SpecifyNamespaceRef implements ParameterReference<Boolean> {
    }

    static final class CreateTextForSpecificKeysRef implements ParameterReference<Boolean> {
    }

    static final class KeepTypeInformationRef implements ParameterReference<Boolean> {
    }

    static final class IsAppendColumn implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(ReplaceInputOrAppendOutputColumnRef.class)
                    .isOneOf(ReplaceInputOrAppendOutputColumn.APPEND);
        }
    }

    static final class IsSpecifyNamespace implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getBoolean(SpecifyNamespaceRef.class).isTrue();
        }
    }

    static final class IsCreateTextEnabled implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getBoolean(CreateTextForSpecificKeysRef.class).isTrue();
        }
    }

    static final class IsKeepTypeInfo implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getBoolean(KeepTypeInformationRef.class).isTrue();
        }
    }

    static final class ReplaceOrAppendInputColumnPersistor
        extends EnumBooleanPersistor<ReplaceInputOrAppendOutputColumn> {

        protected ReplaceOrAppendInputColumnPersistor() {
            super(SingleColumnReplaceOrAddNodeModel.REMOVE_SOURCE, ReplaceInputOrAppendOutputColumn.class,
                ReplaceInputOrAppendOutputColumn.REPLACE);
        }

    }

    static final class HashCommentHandlingPersistor extends EnumBooleanPersistor<HashCommentHandling> {

        protected HashCommentHandlingPersistor() {
            super(JSONToXMLSettings.TRANSLATE_HASHCOMMENT_AS_ELEMENT , HashCommentHandling.class,
                HashCommentHandling.ELEMENT);
        }

    }

    static final class QuestionPrefixHandlingPersistor extends EnumBooleanPersistor<QuestionPrefixHandling> {

        protected QuestionPrefixHandlingPersistor() {
            super(JSONToXMLSettings.TRANSLATE_HASHCOMMENT_AS_ELEMENT , QuestionPrefixHandling.class,
                QuestionPrefixHandling.ELEMENT);
        }

    }

    static final class JSONColumnsProvider extends CompatibleColumnsProvider {

        protected JSONColumnsProvider() {
            super(JSONValue.class);
        }

    }

}
