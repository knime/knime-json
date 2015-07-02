package org.knime.json.node.tojson;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "ColumnsToJson" Node.
 * Converts contents of columns to JSON values rowwise.
 *
 * @author Gabor Bakos
 */
public class ColumnsToJsonNodeFactory
        extends NodeFactory<ColumnsToJsonNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public ColumnsToJsonNodeModel createNodeModel() {
        return new ColumnsToJsonNodeModel();
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
    public NodeView<ColumnsToJsonNodeModel> createNodeView(final int viewIndex,
            final ColumnsToJsonNodeModel nodeModel) {
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
        return new ColumnsToJsonNodeDialog();
    }
}

