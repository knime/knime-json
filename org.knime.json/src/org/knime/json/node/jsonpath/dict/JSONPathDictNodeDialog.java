package org.knime.json.node.jsonpath.dict;

import java.util.ArrayList;
import java.util.List;

import org.knime.core.data.StringValue;
import org.knime.core.data.json.JSONValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.json.node.util.OutputType;

/**
 * <code>NodeDialog</code> for the "JSONPathDict" Node. Collect parts of JSON documents based on JSONPath values
 * specified in the second input port.
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows creation of a simple dialog with standard
 * components. If you need a more complex dialog please derive directly from {@link org.knime.core.node.NodeDialogPane}.
 *
 * @author KNIME
 */
@SuppressWarnings("restriction")
public class JSONPathDictNodeDialog extends DefaultNodeSettingsPane {

    /**
     * New pane for configuring the JSONPathDict node.
     */
    protected JSONPathDictNodeDialog() {
        @SuppressWarnings("unchecked")
        DialogComponentColumnNameSelection input =
            new DialogComponentColumnNameSelection(JSONPathDictNodeModel.createInputColumn(), "JSON column",
                JSONPathDictNodeModel.INPUT_TABLE, true, JSONValue.class);
        addDialogComponent(input);
        addDialogComponent(new DialogComponentBoolean(JSONPathDictNodeModel.createRemoveSourceColumn(), "Remove source column"));
        @SuppressWarnings("unchecked")
        DialogComponentColumnNameSelection path =
            new DialogComponentColumnNameSelection(JSONPathDictNodeModel.createPathColumn(), "JSONPath",
                JSONPathDictNodeModel.DICT_TABLE, true, StringValue.class);
        addDialogComponent(path);

        List<String> outputTypes = new ArrayList<>();
        for (OutputType ot : OutputType.values()) {
            outputTypes.add(ot.name());
        }
        for (OutputType ot : OutputType.values()) {
            if (ot != OutputType.Json) {
                outputTypes.add("List(" + ot.name() + ")");
            }
        }
        for (OutputType ot : OutputType.values()) {
            if (ot != OutputType.Json) {
                outputTypes.add("Set(" + ot.name() + ")");
            }
        }
//        DialogComponentStringSelection type = new DialogComponentStringSelection(JSONPathDictNodeModel.createTypeColumn(), "Output type", outputTypes);
        @SuppressWarnings("unchecked")
        DialogComponentColumnNameSelection type = new DialogComponentColumnNameSelection(JSONPathDictNodeModel.createTypeColumn(), "Type of column", JSONPathDictNodeModel.DICT_TABLE, StringValue.class);
        type.setToolTipText("Possible options: " + outputTypes);
        addDialogComponent(type);
        @SuppressWarnings("unchecked")
        DialogComponentColumnNameSelection output = new DialogComponentColumnNameSelection(JSONPathDictNodeModel.createOutputColumn(), "Output column name", JSONPathDictNodeModel.DICT_TABLE, StringValue.class);
        addDialogComponent(output);
    }
}
