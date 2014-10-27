package org.knime.json.node.jsonpath.projection;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "JSONPathProjection" Node. Selects certain paths from the selected JSON column.
 *
 * @author Gabor Bakos
 */
public class JSONPathProjectionNodeFactory extends NodeFactory<JSONPathProjectionNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public JSONPathProjectionNodeModel createNodeModel() {
        return new JSONPathProjectionNodeModel();
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
    public NodeView<JSONPathProjectionNodeModel> createNodeView(final int viewIndex,
        final JSONPathProjectionNodeModel nodeModel) {
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
        return new JSONPathProjectionNodeDialog();
    }
}
