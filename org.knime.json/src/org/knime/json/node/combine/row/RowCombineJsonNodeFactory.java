package org.knime.json.node.combine.row;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "RowCombineJson" Node.
 * Appends JSON values in the rows to a single JSON value.
 *
 * @author Gabor Bakos
 */
public class RowCombineJsonNodeFactory
        extends NodeFactory<RowCombineJsonNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public RowCombineJsonNodeModel createNodeModel() {
        return new RowCombineJsonNodeModel();
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
    public NodeView<RowCombineJsonNodeModel> createNodeView(final int viewIndex,
            final RowCombineJsonNodeModel nodeModel) {
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
        return new RowCombineJsonNodeDialog();
    }

}

