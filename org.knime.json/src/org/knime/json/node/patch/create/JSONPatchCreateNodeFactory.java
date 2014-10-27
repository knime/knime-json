package org.knime.json.node.patch.create;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "JSONDiff" Node. Compares JSON values.
 *
 * @author Gabor Bakos
 */
public final class JSONPatchCreateNodeFactory extends NodeFactory<JSONPatchCreateNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public JSONPatchCreateNodeModel createNodeModel() {
        return new JSONPatchCreateNodeModel();
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
    public NodeView<JSONPatchCreateNodeModel> createNodeView(final int viewIndex,
        final JSONPatchCreateNodeModel nodeModel) {
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
        return new JSONPatchCreateNodeDialog();
    }

}
