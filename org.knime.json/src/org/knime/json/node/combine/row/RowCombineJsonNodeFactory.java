package org.knime.json.node.combine.row;

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
 * <code>NodeFactory</code> for the "RowCombineJson" Node.
 * Appends JSON values in the rows to a single JSON value.
 *
 * @author Gabor Bakos
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.1
 */
@SuppressWarnings("restriction")
public class RowCombineJsonNodeFactory
        extends NodeFactory<RowCombineJsonNodeModel> implements NodeDialogFactory, KaiNodeInterfaceFactory {

    @Override
    public RowCombineJsonNodeModel createNodeModel() {
        return new RowCombineJsonNodeModel();
    }

    @Override
    public int getNrNodeViews() {
        return 0;
    }

    @Override
    public NodeView<RowCombineJsonNodeModel> createNodeView(final int viewIndex,
            final RowCombineJsonNodeModel nodeModel) {
        throw new IndexOutOfBoundsException("No views! " + viewIndex);
    }

    @Override
    public boolean hasDialog() {
        return true;
    }

    private static final String NODE_NAME = "JSON Row Combiner";
    private static final String NODE_ICON = "./rowcombine.png";
    private static final String SHORT_DESCRIPTION = """
            Appends JSON values in the rows to a single JSON value.
            """;
    private static final String FULL_DESCRIPTION = """
            All of the values in the selected JSON column are combined into a single JSON structure. Depending on the
            <b>Place resulting JSON</b> setting, the combined rows are either placed <b>Under root key</b> (as the
            value of that key) or <b>At top level</b> in the output. You can use <b>Additional properties</b> to add
            custom key/value pairs next to the combined JSON. The <b>Combine rows as</b> option controls whether
            the rows are collected into an <b>Array</b> or into an <b>Object with key</b> (using a key column to
            name each entry). For example, when combining rows as an array under a root key with additional
            properties, the output looks like this:<br /> <pre>
            {
              "JSON key": [
                { "json": "from Row1" },
                { "json": "from Row2" }
              ],
              "custom key": "custom value"
            }</pre>
            All columns and rows will be removed or collapsed to a single cell.
            """;
    private static final List<PortDescription> INPUT_PORTS = List.of(
            fixedPort("Table with JSON", """
                Table with a JSON column
                """)
    );
    private static final List<PortDescription> OUTPUT_PORTS = List.of(
            fixedPort("JSON value", """
                A single JSON value
                """)
    );

    @Override
    public NodeDialogPane createNodeDialogPane() {
        return NodeDialogManager.createLegacyFlowVariableNodeDialog(createNodeDialog());
    }

    /**
     * @since 5.8
     */
    @Override
    public NodeDialog createNodeDialog() {
        return new DefaultNodeDialog(SettingsType.MODEL, RowCombineJsonNodeParameters.class);
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
            RowCombineJsonNodeParameters.class,
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
        return new DefaultKaiNodeInterface(Map.of(SettingsType.MODEL, RowCombineJsonNodeParameters.class));
    }

}

