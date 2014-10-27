package org.knime.json.node.fromxml;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "XMLToJSON" Node. Converts XML values to JSON values.
 *
 * @author Gabor Bakos
 */
public class XMLToJSONNodeFactory extends NodeFactory<XMLToJSONNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public XMLToJSONNodeModel createNodeModel() {
        return new XMLToJSONNodeModel();
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
    public NodeView<XMLToJSONNodeModel> createNodeView(final int viewIndex, final XMLToJSONNodeModel nodeModel) {
        throw new IllegalStateException("No views: " + viewIndex);
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
        return new XMLToJSONNodeDialog();
    }

}
