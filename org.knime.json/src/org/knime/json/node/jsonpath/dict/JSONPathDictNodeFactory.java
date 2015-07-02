package org.knime.json.node.jsonpath.dict;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "JSONPathDict" Node.
 * Collect parts of JSON documents based on JSONPath values specified in the second input port.
 *
 * @author KNIME
 */
public class JSONPathDictNodeFactory
        extends NodeFactory<JSONPathDictNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public JSONPathDictNodeModel createNodeModel() {
        return new JSONPathDictNodeModel();
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
    public NodeView<JSONPathDictNodeModel> createNodeView(final int viewIndex,
            final JSONPathDictNodeModel nodeModel) {
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
        return new JSONPathDictNodeDialog();
    }

}

