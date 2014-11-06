package org.knime.json.node.jsonpointer;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "JSONPathProjection" Node. Selects certain paths from the selected JSON column.
 *
 * @author Gabor Bakos
 */
public class JSONPointerNodeFactory extends NodeFactory<JSONPointerNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public JSONPointerNodeModel createNodeModel() {
        return new JSONPointerNodeModel();
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
    public NodeView<JSONPointerNodeModel> createNodeView(final int viewIndex,
        final JSONPointerNodeModel nodeModel) {
        throw new IllegalArgumentException("No views: " + viewIndex);
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
        return new JSONPointerNodeDialog();
    }
}
