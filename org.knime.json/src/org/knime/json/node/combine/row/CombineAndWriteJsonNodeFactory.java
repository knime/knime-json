package org.knime.json.node.combine.row;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "CombineAndWriteJson" Node.
 * Combines the values from a JSON column to a single JSON file.
 *
 * @author Gabor Bakos
 */
public class CombineAndWriteJsonNodeFactory
        extends NodeFactory<CombineAndWriteJsonNodeModel> {

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
    @Override
    public NodeDialogPane createNodeDialogPane() {
        return new CombineAndWriteJsonNodeDialog();
    }

}

