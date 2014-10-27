package org.knime.json.node.toxml;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "JSONToXML" Node.
 *
 *
 * @author Gabor Bakos
 */
public class JSONToXMLNodeFactory extends NodeFactory<JSONToXMLNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public JSONToXMLNodeModel createNodeModel() {
        return new JSONToXMLNodeModel();
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
    public NodeView<JSONToXMLNodeModel> createNodeView(final int viewIndex, final JSONToXMLNodeModel nodeModel) {
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
        return new JSONToXMLNodeDialog();
    }
}
