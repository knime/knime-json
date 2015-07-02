package org.knime.json.node.fromtable;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "TableToJson" Node.
 * Converts a whole table to a single JSON cell.
 *
 * @author Gabor Bakos
 */
public class TableToJsonNodeFactory
        extends NodeFactory<TableToJsonNodeModel> {

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
        return new TableToJsonNodeDialog();
    }

}

