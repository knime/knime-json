package org.knime.json.node.fromtable;

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
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeFactory;

/**
 * <code>NodeFactory</code> for the "TableToJson" Node.
 * Converts a whole table to a single JSON cell.
 *
 * @author Gabor Bakos
 * @author Leon Wenzler, KNIME GmbH, Konstanz
 * @author AI Migration Pipeline v1.0
 */
@SuppressWarnings("restriction")
public class TableToJsonNodeFactory
        extends NodeFactory<TableToJsonNodeModel> implements NodeDialogFactory, KaiNodeInterfaceFactory {

    /**
     * {@inheritDoc}
     */
    @Override
    public TableToJsonNodeModel createNodeModel() {
        return new TableToJsonNodeModel();
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
    public NodeView<TableToJsonNodeModel> createNodeView(final int viewIndex,
            final TableToJsonNodeModel nodeModel) {
        throw new UnsupportedOperationException("No views: " + viewIndex);
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
    @Override
    public NodeDialogPane createNodeDialogPane() {
        return NodeDialogManager.createLegacyFlowVariableNodeDialog(createNodeDialog());
    }

    /**
     * {@inheritDoc}
     * @since 5.9
     */
    @Override
    public NodeDialog createNodeDialog() {
        return new DefaultNodeDialog(SettingsType.MODEL, TableToJsonNodeParameters.class);
    }

    /**
     * {@inheritDoc}
     * @since 5.9
     */
    @Override
    @SuppressWarnings({"deprecation"})
    public NodeDescription createNodeDescription() {
        final var config = WebUINodeConfiguration.builder()
            .name("Table to JSON") //
            .icon("./tabletojson.png") //
            .shortDescription("""
                Converts some columns of a table to a single JSON cell.
                """) //
            .fullDescription("""
                Converts the selected columns content to a JSON value row-wise or column-wise. It also have an option \
                to "undo" -with some limitations- the JSON to Table transformation and create JSON values for each row \
                based on the column names.<br/>
                Example input table:
                <table>
                <tr><th>a.b</th><th>a.c</th><th>d</th></tr>
                <tr><td>b0</td><td>c0</td><td>d0</td></tr>
                <tr><td>b1</td><td>c1</td><td>d1</td></tr>
                </table>
                With the different parameters, the following JSON values are generated:<br/>
                <b>Row-oriented</b>
                <pre>
                [ {
                  "a.b" : "b0",
                  "a.c" : "c0",
                  "d" : "d0"
                }, {
                  "a.b" : "b1",
                  "a.c" : "c1",
                  "d" : "d1"
                } ]
                </pre>
                <b>Column-oriented</b> (with Row keys as JSON value with key: "ROWID"):
                <pre>
                {
                  "ROWID" : [ "Row0", "Row1" ],
                  "a.b" : [ "b0", "b1" ],
                  "a.c" : [ "c0", "c1" ],
                  "d" : [ "d0", "d1" ]
                }</pre>
                <b>Keep rows</b> (with <b>Column names as paths</b>, separator: <tt>.</tt>):
                <pre>
                {
                  "a" : {
                    "b" : "b0",
                    "c" : "c0"
                  },
                  "d" : "d0"
                }</pre>
                <pre>
                {
                  "a" : {
                    "b" : "b1",
                    "c" : "c1"
                  },
                  "d" : "d1"
                }</pre>
                """) //
            .modelSettingsClass(TableToJsonNodeParameters.class) //
            .nodeType(NodeType.Manipulator) //
            .addInputTable("Table", """
                A datatable
                """) //
            .addOutputTable("JSON", """
                Table containing the JSON column
                """) //
            .build();
        return WebUINodeFactory.createNodeDescription(config);
    }

    /**
     * {@inheritDoc}
     * @since 5.9
     */
    @Override
    public KaiNodeInterface createKaiNodeInterface() {
        return new DefaultKaiNodeInterface(Map.of(SettingsType.MODEL, TableToJsonNodeParameters.class));
    }
}

