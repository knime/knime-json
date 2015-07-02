package org.knime.json.node.totable;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "JSONToTable" Node.
 * Converts JSON values to new columns.
 *
 * @author Gabor Bakos
 */
public class JSONToTableNodeFactory
        extends NodeFactory<JSONToTableNodeModel> {

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
    @Override
    public NodeDialogPane createNodeDialogPane() {
        return new JSONToTableNodeDialog();
    }

}

