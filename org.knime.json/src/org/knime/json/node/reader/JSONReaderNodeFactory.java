package org.knime.json.node.reader;

import org.knime.core.data.json.JSONValue;
import org.knime.core.node.ContextAwareNodeFactory;
import org.knime.core.node.NodeCreationContext;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "JSONReader" node. Reads {@code .json} files to {@link JSONValue}s.
 *
 * @author Gabor Bakos
 */
public final class JSONReaderNodeFactory extends ContextAwareNodeFactory<JSONReaderNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public JSONReaderNodeModel createNodeModel() {
        return new JSONReaderNodeModel();
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
    public NodeView<JSONReaderNodeModel> createNodeView(final int viewIndex, final JSONReaderNodeModel nodeModel) {
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
        return new JSONReaderNodeDialog();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JSONReaderNodeModel createNodeModel(final NodeCreationContext context) {
        JSONReaderNodeModel ret = new JSONReaderNodeModel();
        ret.setUrl(context.getUrl());
        return ret;
    }

}
