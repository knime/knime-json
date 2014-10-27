package org.knime.json.node.schema.check;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "JSONSchemaCheck" node. Checks a JSON column's values against a Schema and fails if
 * it do not match.
 *
 * @author Gabor Bakos
 */
public final class JSONSchemaCheckNodeFactory extends NodeFactory<JSONSchemaCheckNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public JSONSchemaCheckNodeModel createNodeModel() {
        return new JSONSchemaCheckNodeModel();
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
    public NodeView<JSONSchemaCheckNodeModel> createNodeView(final int viewIndex,
        final JSONSchemaCheckNodeModel nodeModel) {
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
        return new JSONSchemaCheckNodeDialog();
    }
}
