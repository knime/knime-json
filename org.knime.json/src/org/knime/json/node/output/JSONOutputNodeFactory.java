/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright by KNIME AG, Zurich, Switzerland
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.com
 * email: contact@knime.com
 * ---------------------------------------------------------------------
 *
 * History
 *   Created on Feb 15, 2015 by wiswedel
 */
package org.knime.json.node.output;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * This is the factory for the JSON Output node.
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public final class JSONOutputNodeFactory extends NodeFactory<JSONOutputNodeModel> {
    /** {@inheritDoc} */
    @Override
    public JSONOutputNodeModel createNodeModel() {
        return new JSONOutputNodeModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int getNrNodeViews() {
        return 1;
    }

    /** {@inheritDoc} */
    @Override
    public NodeView<JSONOutputNodeModel> createNodeView(final int viewIndex, final JSONOutputNodeModel nodeModel) {
        return new JSONOutputNodeView(nodeModel);
    }

    /** {@inheritDoc} */
    @Override
    protected boolean hasDialog() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return new JSONOutputNodeDialog();
    }
}
