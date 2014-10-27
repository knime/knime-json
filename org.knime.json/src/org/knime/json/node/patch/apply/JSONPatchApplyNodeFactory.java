package org.knime.json.node.patch.apply;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "JSONTransformer" Node. Changes JSON values.
 *
 * @author Gabor Bakos
 */
public final class JSONPatchApplyNodeFactory extends NodeFactory<JSONPatchApplyNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public JSONPatchApplyNodeModel createNodeModel() {
        return new JSONPatchApplyNodeModel();
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
    public NodeView<JSONPatchApplyNodeModel>
        createNodeView(final int viewIndex, final JSONPatchApplyNodeModel nodeModel) {
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
        return new JSONPatchApplyNodeDialog();
    }
}
