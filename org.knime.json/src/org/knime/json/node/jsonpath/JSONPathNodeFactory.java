package org.knime.json.node.jsonpath;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "JSONPath" Node. Selects certain paths from the selected JSON column.
 *
 * @author Gabor Bakos
 */
public class JSONPathNodeFactory extends NodeFactory<JSONPathNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public JSONPathNodeModel createNodeModel() {
        return new JSONPathNodeModel();
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
    public NodeView<JSONPathNodeModel> createNodeView(final int viewIndex,
        final JSONPathNodeModel nodeModel) {
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
        return new JSONPathNodeDialog();
    }
}
