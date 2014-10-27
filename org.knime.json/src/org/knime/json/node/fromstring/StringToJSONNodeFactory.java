package org.knime.json.node.fromstring;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "StringToJSON" Node. Converts String values to JSON values.
 *
 * @author Gabor Bakos
 */
public final class StringToJSONNodeFactory extends NodeFactory<StringToJSONNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public StringToJSONNodeModel createNodeModel() {
        return new StringToJSONNodeModel();
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
    public NodeView<StringToJSONNodeModel> createNodeView(final int viewIndex, final StringToJSONNodeModel nodeModel) {
        throw new UnsupportedOperationException("No views yet.");
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
        return new StringToJSONNodeDialog();
    }
}
