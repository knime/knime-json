package org.knime.json.node.combine.row;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;
import org.knime.core.webui.node.dialog.NodeDialog;
import org.knime.core.webui.node.dialog.NodeDialogFactory;
import org.knime.core.webui.node.dialog.NodeDialogManager;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeDialog;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultKaiNodeInterface;
import org.knime.core.webui.node.dialog.kai.KaiNodeInterface;
import org.knime.core.webui.node.dialog.kai.KaiNodeInterfaceFactory;
import org.knime.core.node.NodeDescription;
import org.knime.node.impl.description.DefaultNodeDescriptionUtil;
import java.util.Map;
import org.knime.node.impl.description.PortDescription;
import java.util.List;
import static org.knime.node.impl.description.PortDescription.fixedPort;

/**
 * <code>NodeFactory</code> for the "CombineAndWriteJson" Node.
 * Combines the values from a JSON column to a single JSON file.
 *
 * @author Gabor Bakos
 * @author Tim Crundall, TNG Technology Consulting GmbH
 * @author AI Migration Pipeline v1.2
 */
@SuppressWarnings("restriction")
public class CombineAndWriteJsonNodeFactory
        extends NodeFactory<CombineAndWriteJsonNodeModel> implements NodeDialogFactory, KaiNodeInterfaceFactory {

    /**
     * {@inheritDoc}
     */
    @Override
    public CombineAndWriteJsonNodeModel createNodeModel() {
        return new CombineAndWriteJsonNodeModel();
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
    public NodeView<CombineAndWriteJsonNodeModel> createNodeView(final int viewIndex,
            final CombineAndWriteJsonNodeModel nodeModel) {
        throw new IndexOutOfBoundsException("No views! " + viewIndex);
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
    private static final String NODE_NAME = "JSON Row Combiner and Writer";
    private static final String NODE_ICON = "./rowcombinewrite.png";
    private static final String SHORT_DESCRIPTION = """
            Combines the values from a JSON column to a single JSON file.
            """;
    private static final String FULL_DESCRIPTION = """
            All of the values in the selected JSON column are combined into a single JSON structure
            and written to a file. Depending on the <b>Place resulting JSON</b> setting, the
            combined rows are either placed <b>Under root key</b> (as the value of that key) or
            <b>At top level</b> in the file. You can use <b>Additional properties</b> to add custom
            key/value pairs next to the combined JSON. The <b>Combine rows as</b> option controls
            whether the rows are collected into an <b>Array</b> or into an <b>Object with key</b>
            (using a key column to name each entry). (The JSON content in the file can be
            pretty-printed.) For example, when combining rows as an array under a root key with
            additional properties, the output looks like this:<br /> <pre>
            {
              "JSON key": [
                { "json": "from Row1" },
                { "json": "from Row2" }
              ],
              "custom key": "custom value"
            }</pre>
            """;
    private static final List<PortDescription> INPUT_PORTS = List.of(
            fixedPort("Table with JSON", """
                Table with a JSON column
                """)
    );
    private static final List<PortDescription> OUTPUT_PORTS = List.of();

    @Override
    public NodeDialogPane createNodeDialogPane() {
        return NodeDialogManager.createLegacyFlowVariableNodeDialog(createNodeDialog());
    }

    @Override
    public NodeDialog createNodeDialog() {
        return new DefaultNodeDialog(SettingsType.MODEL, CombineAndWriteJsonNodeParameters.class);
    }

    @Override
    public NodeDescription createNodeDescription() {
        return DefaultNodeDescriptionUtil.createNodeDescription( //
            NODE_NAME, //
            NODE_ICON, //
            INPUT_PORTS, //
            OUTPUT_PORTS, //
            SHORT_DESCRIPTION, //
            FULL_DESCRIPTION, //
            List.of(), //
            CombineAndWriteJsonNodeParameters.class, //
            null, //
            NodeType.Sink, //
            List.of(), //
            null //
        );
    }

    @Override
    public KaiNodeInterface createKaiNodeInterface() {
        return new DefaultKaiNodeInterface(Map.of(SettingsType.MODEL, CombineAndWriteJsonNodeParameters.class));
    }

}

