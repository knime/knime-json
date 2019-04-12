package org.knime.json.node.container.output.row;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "ContainerRowOutput" Node.
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 */
public class ContainerRowOutputNodeFactory
        extends NodeFactory<ContainerRowOutputNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public ContainerRowOutputNodeModel createNodeModel() {
        return new ContainerRowOutputNodeModel();
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
    public NodeView<ContainerRowOutputNodeModel> createNodeView(final int viewIndex,
            final ContainerRowOutputNodeModel nodeModel) {
        return null;
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
        return new ContainerRowOutputNodeDialog();
    }

}

