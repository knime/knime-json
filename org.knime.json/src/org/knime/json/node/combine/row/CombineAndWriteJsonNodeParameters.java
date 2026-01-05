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

package org.knime.json.node.combine.row;

import static org.knime.base.node.io.filehandling.webui.OutputFileMessageProvider.LOCAL_URL_PATTERN;
import static org.knime.base.node.io.filehandling.webui.OutputFileMessageProvider.URL_PATTERN;
import static org.knime.core.webui.node.dialog.defaultdialog.setting.singleselection.RowIDChoice.ROW_ID;

import java.util.Optional;

import org.knime.base.node.io.filehandling.webui.OutputFileMessageProvider;
import org.knime.base.node.io.filehandling.webui.OutputFileMessageProvider.OutputFileRef;
import org.knime.base.node.io.filehandling.webui.OverwritePolicy;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.NominalValue;
import org.knime.core.data.json.JSONValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileSystemOption;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileWriterWidget;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.WithFileSystem;
import org.knime.core.webui.node.dialog.defaultdialog.setting.singleselection.RowIDChoice;
import org.knime.core.webui.node.dialog.defaultdialog.setting.singleselection.StringOrEnum;
import org.knime.json.node.combine.row.RowCombineSettings.JsonStructure;
import org.knime.json.node.combine.row.RowCombineSettings.ObjectOrArray;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.array.ArrayWidget;
import org.knime.node.parameters.array.ArrayWidget.ElementLayout;
import org.knime.node.parameters.layout.After;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.persistence.legacy.EnumBooleanPersistor;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.updates.legacy.ColumnNameAutoGuessValueProvider;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;
import org.knime.node.parameters.widget.choices.util.ColumnSelectionUtil;
import org.knime.node.parameters.widget.choices.util.CompatibleColumnsProvider;
import org.knime.node.parameters.widget.message.TextMessage;

/**
 * Node parameters for JSON Row Combiner and Writer.
 *
 * @author Tim Crundall, TNG Technology Consulting GmbH
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction")
final class CombineAndWriteJsonNodeParameters implements NodeParameters {

    @Section(title = "Output File")
    interface OutputSection {
    }

    @Section(title = "JSON Structure")
    @After(OutputSection.class)
    interface JSONStructureSection {
    }

    @Widget(title = "JSON column", description = "The JSON column providing the values for the resulting JSON.")
    @ChoicesProvider(JSONColumnChoicesProvider.class)
    @ValueReference(JSONInputColumnRef.class)
    @ValueProvider(JSONColumnProvider.class)
    String m_inputColumn;

    private interface JSONInputColumnRef extends ParameterReference<String> {
    }

    private static class JSONColumnChoicesProvider extends CompatibleColumnsProvider {
        protected JSONColumnChoicesProvider() {
            super(JSONValue.class);
        }
    }

    private static final class JSONColumnProvider extends ColumnNameAutoGuessValueProvider {

        protected JSONColumnProvider() {
            super(JSONInputColumnRef.class);
        }

        @Override
        protected Optional<DataColumnSpec> autoGuessColumn(final NodeParametersInput parametersInput) {
            final var compatibleColumns =
                ColumnSelectionUtil.getCompatibleColumnsOfFirstPort(parametersInput, JSONValue.class);
            return compatibleColumns.isEmpty() ? Optional.empty()
                : Optional.of(compatibleColumns.get(compatibleColumns.size() - 1));
        }

    }

    @Layout(OutputSection.class)
    @Widget(title = "Output file location", description = "Location to save the file (can also be a URL).")
    @FileWriterWidget(fileExtension = "json")
    @WithFileSystem(FileSystemOption.LOCAL)
    @Persist(configKey = "file")
    @ValueReference(OutputFileRef.class)
    String m_outputFile = "";

    @Layout(OutputSection.class)
    @TextMessage(value = OutputFileMessageProvider.class)
    Void m_invalidSchemeMessage;

    @Layout(OutputSection.class)
    @Widget(title = "If output file already exists", description = "How to handle output file already existing.")
    @Persistor(OverwritePolicyPersistor.class)
    @Effect(predicate = HideIfURL.class, type = EffectType.HIDE)
    @ValueSwitchWidget
    OverwritePolicy m_overwriteExistingFile = OverwritePolicy.PREVENT;

    private static class HideIfURL implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getString(OutputFileRef.class).matchesPattern(URL_PATTERN)
                .and(i.getString(OutputFileRef.class).matchesPattern(LOCAL_URL_PATTERN).negate());
        }
    }

    private static class OverwritePolicyPersistor extends EnumBooleanPersistor<OverwritePolicy> {
        public OverwritePolicyPersistor() {
            super("overwriteExistingFile", OverwritePolicy.class, OverwritePolicy.OVERWRITE);
        }
    }

    @Layout(OutputSection.class)
    @Widget(title = "Format", description = "Format for the resulting JSON.")
    @Persistor(JSONFormatPersistor.class)
    @ValueSwitchWidget
    JSONFormat m_jsonFormat = JSONFormat.PRETTY;

    private enum JSONFormat {
            @Label(value = "Dense", description = "No whitespace.")
            DENSE, //
            @Label(value = "Pretty print", description = "Use multiple lines and indentation.")
            PRETTY,
    }

    private static class JSONFormatPersistor extends EnumBooleanPersistor<JSONFormat> {
        public JSONFormatPersistor() {
            super("prettyPrint", JSONFormat.class, JSONFormat.PRETTY);
        }
    }

    @Layout(JSONStructureSection.class)
    @Widget(title = "Combine rows as", description = "Configure how to collect the JSON values.")
    @ValueSwitchWidget
    @ValueReference(ObjectOrArrayRef.class)
    ObjectOrArray m_objectOrArray = RowCombineSettings.DEFAULT_OBJECT_OR_ARRAY;

    private static class ObjectOrArrayRef implements ParameterReference<ObjectOrArray> {
    }

    @Layout(JSONStructureSection.class)
    @Widget(title = "Object key column",
        description = "Column which provides keys when collecting rows into JSON object.")
    @ChoicesProvider(NominalColumnsProvider.class)
    @Persistor(ObjectKeyColumnPersistor.class)
    @Effect(predicate = IsObjectCollection.class, type = EffectType.SHOW)
    StringOrEnum<RowIDChoice> m_objectKeyColumn = new StringOrEnum<>(RowIDChoice.ROW_ID);

    private static final class NominalColumnsProvider extends CompatibleColumnsProvider {
        NominalColumnsProvider() {
            super(NominalValue.class);
        }
    }

    private static final class ObjectKeyColumnPersistor implements NodeParametersPersistor<StringOrEnum<RowIDChoice>> {

        @Override
        public StringOrEnum<RowIDChoice> load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return settings.getBoolean(RowCombineSettings.OBJECT_KEY_ROW_ID) ? new StringOrEnum<>(ROW_ID)
                : new StringOrEnum<>(settings.getString(RowCombineSettings.OBJECT_KEY_COLUMN));
        }

        @Override
        public void save(final StringOrEnum<RowIDChoice> param, final NodeSettingsWO settings) {
            settings.addBoolean(RowCombineSettings.OBJECT_KEY_ROW_ID, param.getEnumChoice().isPresent());
            settings.addString(RowCombineSettings.OBJECT_KEY_COLUMN, param.getStringChoice());
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{RowCombineSettings.OBJECT_KEY_ROW_ID}, {RowCombineSettings.OBJECT_KEY_COLUMN}};
        }

    }

    private static class IsObjectCollection implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(ObjectOrArrayRef.class).isOneOf(ObjectOrArray.Object);
        }

    }

    @Layout(JSONStructureSection.class)
    @Widget(title = "Place resulting JSON", description = "How to structure the resulting JSON output.")
    @ValueSwitchWidget
    @Persistor(JsonStructurePersistor.class)
    @ValueReference(JsonStructureRef.class)
    JsonStructure m_jsonStructure = JsonStructure.NESTED;

    private static class JsonStructureRef implements ParameterReference<JsonStructure> {
    }

    private static class JsonStructurePersistor extends EnumBooleanPersistor<JsonStructure> {
        public JsonStructurePersistor() {
            super("addRootKey", JsonStructure.class, JsonStructure.NESTED);
        }
    }

    private static class IsNestedStructure implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(JsonStructureRef.class).isOneOf(JsonStructure.NESTED);
        }
    }

    @Layout(JSONStructureSection.class)
    @Widget(title = "Root object key", description = "Key under which to place the resulting JSON.")
    @Effect(type = EffectType.SHOW, predicate = IsNestedStructure.class)
    String m_rootKey = "";

    @Layout(JSONStructureSection.class)
    @Widget(title = "Additional properties", description = "Additional key/value pairs to add to the root object.")
    @ArrayWidget(elementLayout = ElementLayout.HORIZONTAL_SINGLE_LINE, addButtonText = "Add property",
        showSortButtons = false)
    @Persistor(KeyValuePairPersistor.class)
    @Effect(type = EffectType.SHOW, predicate = IsNestedStructure.class)
    KeyValuePairSettings[] m_keyValuePairs = new KeyValuePairSettings[0];

}
