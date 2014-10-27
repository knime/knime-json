package org.knime.json.node.writer;

import org.knime.core.data.json.JSONValue;
import org.knime.core.node.ContextAwareNodeFactory;
import org.knime.core.node.NodeCreationContext;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "JSONWriter" node. Writes {@code .json} files from {@link JSONValue}s.
 *
 * @author Gabor Bakos
 */
public final class JSONWriterNodeFactory extends ContextAwareNodeFactory<JSONWriterNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public JSONWriterNodeModel createNodeModel() {
        return new JSONWriterNodeModel();
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
    public NodeView<JSONWriterNodeModel> createNodeView(final int viewIndex, final JSONWriterNodeModel nodeModel) {
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
        return new JSONWriterNodeDialog();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JSONWriterNodeModel createNodeModel(final NodeCreationContext context) {
        JSONWriterNodeModel ret = new JSONWriterNodeModel();
        ret.setUrl(context.getUrl());
        return ret;
    }
}
