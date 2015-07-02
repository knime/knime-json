package org.knime.json.node.combine.column;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "ColumnCombineJson" Node.
 * Combines multiple JSON columns to a single.
 *
 * @author Gabor Bakos
 */
public class ColumnCombineJsonNodeFactory
        extends NodeFactory<ColumnCombineJsonNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public ColumnCombineJsonNodeModel createNodeModel() {
        return new ColumnCombineJsonNodeModel();
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
    public NodeView<ColumnCombineJsonNodeModel> createNodeView(final int viewIndex,
            final ColumnCombineJsonNodeModel nodeModel) {
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
    @Override
    public NodeDialogPane createNodeDialogPane() {
        return new ColumnCombineJsonNodeDialog();
    }

}

