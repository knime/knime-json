package org.knime.json.node.totable;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;
import org.knime.core.webui.node.dialog.NodeDialog;
import org.knime.core.webui.node.dialog.NodeDialogFactory;
import org.knime.core.webui.node.dialog.NodeDialogManager;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeDialog;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultKaiNodeInterface;
import org.knime.core.node.NodeFactory.NodeType;
import org.knime.core.webui.node.dialog.kai.KaiNodeInterface;
import org.knime.core.webui.node.dialog.kai.KaiNodeInterfaceFactory;
import org.knime.core.node.NodeDescription;
import org.knime.node.impl.description.DefaultNodeDescriptionUtil;
import org.knime.node.impl.description.PortDescription;
import java.util.List;
import java.util.Map;
import static org.knime.node.impl.description.PortDescription.fixedPort;

/**
 * <code>NodeFactory</code> for the "JSONToTable" Node.
 * Converts JSON values to new columns.
 *
 * @author Gabor Bakos
 * @author Marc Lehner, KNIME GmbH, Zurich, Switzerland
 * @author AI Migration Pipeline v1.1
 */
@SuppressWarnings("restriction")
public class JSONToTableNodeFactory
        extends NodeFactory<JSONToTableNodeModel> implements NodeDialogFactory, KaiNodeInterfaceFactory {

    /**
     * {@inheritDoc}
     */
    @Override
    public JSONToTableNodeModel createNodeModel() {
        return new JSONToTableNodeModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNrNodeViews() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeView<JSONToTableNodeModel> createNodeView(final int viewIndex,
            final JSONToTableNodeModel nodeModel) {
        throw new IllegalStateException("No views! " + viewIndex);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasDialog() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    private static final String NODE_NAME = "JSON to Table";
    private static final String NODE_ICON = "./jsontotable.png";
    private static final String SHORT_DESCRIPTION = """
            Converts JSON values to new columns.
            """;
    private static final String FULL_DESCRIPTION = """
            Converts a JSON column to multiple columns, whereby the column list is heuristically extracted from the
                JSON structure. It can either extract the primitive leaf elements (like strings and numbers), omitting
                the JSON tree path, or the full JSON structure. The latter, however, may yield some confusing output as
                the types of the columns are again JSON or collections of JSON. Note that this node is meant to be used
                for "well-structured", relatively flat JSON objects which adhere the same schema in all rows. In case
                the JSON objects are more complex it's better to use nodes like JSON Path or JSON Path (Dictionary).
                Some examples on the following JSON column input may help to clarify the generated output. JSON {"a":
                {"b": [1, 2], "c":"c"}} {"a": {"b": [3], "d":null} } Some options with their results: Only leaves, Use
                leaf name (uniquify with (#1)/(#2)/...) Types: b - JSON when Keep as JSON array, list of integers when
                Keep as collection elements c - String d - String (The actual order of the columns might be different.)
                bcd [1,2]c? [3]?? Only up to level 1, Use leaf name (uniquify with (#1)/(#2)/...) Type: a - JSON a {"b":
                [1, 2], "c":"c"} {"b": [3], "d":null} Only leaves, Use path with separator ., Expand to columns Type:
                a.b.0, a.b.1 - integer a.c, a.d - string a.b.0a.b.1a.ca.d 12c? 3??? For nested objects, see the
                following example: JSON {"a":[{"b": 3}, 4]} {"a":[1]} Only up to level 1, Use leaf name (uniquify with
                (#1)/(#2)/...), Omit nested objects, Expand to columns: Type: a - list of integers a [4] [1] Only up to
                level 1 or 2, Use leaf name (uniquify with (#1)/(#2)/...), do not Omit nested objects, Expand to
                columns: Type: a - list of JSON values a [{"b": 3}, 4] [1] Please note that in the first row the value
                is a KNIME list of the two JSON values: {"b": 3} and 4, not a single JSON value, similarly in the second
                row, you get a KNIME list of a single JSON value: 1. Though with Keep as JSON array (regardless of Omit
                nested objects): Type: a - JSON values a [{"b": 3}, 4] [1]
            """;
    private static final List<PortDescription> INPUT_PORTS = List.of(
            fixedPort("JSON", """
                Table containing JSON column.
                """)
    );
    private static final List<PortDescription> OUTPUT_PORTS = List.of(
            fixedPort("Extracted values", """
                Table with values extracted from the selected JSON column.
                """)
    );

    @Override
    public NodeDialogPane createNodeDialogPane() {
        return NodeDialogManager.createLegacyFlowVariableNodeDialog(createNodeDialog());
    }

    @Override
    public NodeDialog createNodeDialog() {
        return new DefaultNodeDialog(SettingsType.MODEL, JSONToTableNodeParameters.class);
    }

    @Override
    public NodeDescription createNodeDescription() {
        return DefaultNodeDescriptionUtil.createNodeDescription(NODE_NAME, NODE_ICON, INPUT_PORTS, OUTPUT_PORTS,
            SHORT_DESCRIPTION, FULL_DESCRIPTION, List.of(), JSONToTableNodeParameters.class, null, NodeType.Manipulator,
            List.of(), null);
    }

    @Override
    public KaiNodeInterface createKaiNodeInterface() {
        return new DefaultKaiNodeInterface(Map.of(SettingsType.MODEL, JSONToTableNodeParameters.class));
    }

}