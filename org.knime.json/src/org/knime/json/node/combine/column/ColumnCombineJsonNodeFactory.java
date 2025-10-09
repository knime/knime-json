package org.knime.json.node.combine.column;

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
 * <code>NodeFactory</code> for the "ColumnCombineJson" Node.
 * Combines multiple JSON columns to a single.
 *
 * @author Gabor Bakos
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.1
 */
@SuppressWarnings("restriction")
public class ColumnCombineJsonNodeFactory
        extends NodeFactory<ColumnCombineJsonNodeModel> implements NodeDialogFactory, KaiNodeInterfaceFactory {

    @Override
    public ColumnCombineJsonNodeModel createNodeModel() {
        return new ColumnCombineJsonNodeModel();
    }

    @Override
    public int getNrNodeViews() {
        return 0;
    }

    @Override
    public NodeView<ColumnCombineJsonNodeModel> createNodeView(final int viewIndex,
            final ColumnCombineJsonNodeModel nodeModel) {
        throw new IndexOutOfBoundsException("No views! " + viewIndex);
    }

    @Override
    public boolean hasDialog() {
        return true;
    }

    private static final String NODE_NAME = "JSON Column Combiner";
    private static final String NODE_ICON = "./columncombine.png";
    private static final String SHORT_DESCRIPTION = """
            Combines multiple JSON columns to a single.
            """;
    private static final String FULL_DESCRIPTION = """
            Combines JSON values to a single JSON value (an array optionally wrapped in an object) for each row.
                Example configuration:<br/>
                Input table:<br/>
                <table>
                    <tr>
                        <th>S</th>
                        <th>R</th>
                        <th>V</th>
                    </tr>
                    <tr>
                        <td>2</td>
                        <td>[1]</td>
                        <td>"S"</td>
                    </tr>
                    <tr>
                        <td>3</td>
                        <td>{"a":2}</td>
                        <td>{"T": null}</td>
                    </tr>
                </table>
                <i>Selected JSON columns</i>: <b>R</b> and <b>V</b><br/>
                <i>Data bound key</i>: <b>S</b> (String)<br/>
                Results:
                <table>
                    <tr>
                        <td>{"2": [[1], "S"]}</td>
                    </tr>
                    <tr>
                        <td>{"3": [{"a": 2}, {"T": null}]}</td>
                    </tr>
                </table>
            """;
    private static final List<PortDescription> INPUT_PORTS = List.of(
            fixedPort("Table with JSON", """
                Table with JSON column(s)
                """)
    );
    private static final List<PortDescription> OUTPUT_PORTS = List.of(
            fixedPort("Table with JSON", """
                Table with the combined JSON column
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
        return new DefaultNodeDialog(SettingsType.MODEL, ColumnCombineJsonNodeParameters.class);
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
            ColumnCombineJsonNodeParameters.class,
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
        return new DefaultKaiNodeInterface(Map.of(SettingsType.MODEL, ColumnCombineJsonNodeParameters.class));
    }

}

