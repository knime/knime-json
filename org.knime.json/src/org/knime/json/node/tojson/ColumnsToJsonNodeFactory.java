package org.knime.json.node.tojson;

import static org.knime.node.impl.description.PortDescription.fixedPort;

import java.util.List;
import java.util.Map;

import org.knime.core.node.NodeDescription;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;
import org.knime.core.webui.node.dialog.NodeDialog;
import org.knime.core.webui.node.dialog.NodeDialogFactory;
import org.knime.core.webui.node.dialog.NodeDialogManager;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultKaiNodeInterface;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeDialog;
import org.knime.core.webui.node.dialog.kai.KaiNodeInterface;
import org.knime.core.webui.node.dialog.kai.KaiNodeInterfaceFactory;
import org.knime.node.impl.description.DefaultNodeDescriptionUtil;
import org.knime.node.impl.description.PortDescription;

/**
 * <code>NodeFactory</code> for the "ColumnsToJson" Node.
 * Converts contents of columns to JSON values rowwise.
 *
 * @author Gabor Bakos
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.1
 */
@SuppressWarnings("restriction")
public class ColumnsToJsonNodeFactory
        extends NodeFactory<ColumnsToJsonNodeModel> implements NodeDialogFactory, KaiNodeInterfaceFactory {

    @Override
    public ColumnsToJsonNodeModel createNodeModel() {
        return new ColumnsToJsonNodeModel();
    }

    @Override
    public int getNrNodeViews() {
        return 0;
    }

    @Override
    public NodeView<ColumnsToJsonNodeModel> createNodeView(final int viewIndex,
            final ColumnsToJsonNodeModel nodeModel) {
        throw new IllegalStateException("No views! " + viewIndex);
    }

    @Override
    public boolean hasDialog() {
        return true;
    }

    private static final String NODE_NAME = "Columns to JSON";
    private static final String NODE_ICON = "./columnstojson.png";
    private static final String SHORT_DESCRIPTION = """
            Converts contents of columns to JSON values row-wise.
            """;
    private static final String FULL_DESCRIPTION = """
            <p>
            The columns values are transformed to JSON objects for each row. When there are conflicting keys, the
                result is undefined, might vary between different versions of KNIME.
                </p><br/>
                <p>
                An example transformation:<br/>
                From table:
                </p>
                <table><tr><th>Main</th><th>Num</th><th>text</th></tr>
                <tr><td>main1</td><td>2</td><td>Hello</td></tr>
                <tr><td>main2</td><td>1</td><td>World</td></tr>
                </table>
                <p>
                with custom key/values:
                <tt>const</tt>
                and
                <tt>val</tt>,
                <tt>Main</tt>
                as data bound key,
                <tt>Num</tt>
                with manual
                <tt>num</tt>
                key and
                <tt>text</tt>
                as automatic:
                </p>
                <table>
                <tr><td>{"main1":{"text": "Hello", "num": 2, "const":"val"}}</td></tr>
                <tr><td>{"main2":{"text": "World", "num": 1, "const":"val"}}</td></tr>
                </table>
            """;
    private static final List<PortDescription> INPUT_PORTS = List.of(
            fixedPort("Table", """
                Table with values.
                """)
    );
    private static final List<PortDescription> OUTPUT_PORTS = List.of(
            fixedPort("Table with JSON", """
                Table with the row-wise generated JSON column.
                """)
    );

    /**
     * @since 5.8
     */
    @Override
    public NodeDialogPane createNodeDialogPane() {
        return NodeDialogManager.createLegacyFlowVariableNodeDialog(createNodeDialog());
    }

    /**
     * @since 5.8
     */
    @Override
    public NodeDialog createNodeDialog() {
        return new DefaultNodeDialog(SettingsType.MODEL, ColumnsToJsonNodeParameters.class);
    }

    @Override
    public NodeDescription createNodeDescription() {
        return DefaultNodeDescriptionUtil.createNodeDescription(
            NODE_NAME,
            NODE_ICON,
            INPUT_PORTS,
            OUTPUT_PORTS,
            SHORT_DESCRIPTION,
            FULL_DESCRIPTION,
            List.of(),
            ColumnsToJsonNodeParameters.class,
            null,
            NodeType.Manipulator,
            List.of(),
            null
        );
    }

    /**
     * @since 5.8
     */
    @Override
    public KaiNodeInterface createKaiNodeInterface() {
        return new DefaultKaiNodeInterface(Map.of(SettingsType.MODEL, ColumnsToJsonNodeParameters.class));
    }
}

