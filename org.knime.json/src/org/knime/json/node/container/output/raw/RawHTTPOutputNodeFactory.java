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
 *   24 Jun 2022 (alexander): created
 */
package org.knime.json.node.container.output.raw;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * This is the factory for the Container Output (Raw HTTP) node.
 *
 * @author Alexander Fillbrunn, KNIME GmbH, Konstanz, Germany
 */
public final class RawHTTPOutputNodeFactory extends NodeFactory<RawHTTPOutputNodeModel> {

    @Override
    public RawHTTPOutputNodeModel createNodeModel() {
        return new RawHTTPOutputNodeModel();
    }

    @Override
    protected int getNrNodeViews() {
        return 0;
    }

    @Override
    public NodeView<RawHTTPOutputNodeModel> createNodeView(final int viewIndex, final RawHTTPOutputNodeModel nodeModel) {
        return null;
    }

    @Override
    protected boolean hasDialog() {
        return true;
    }

    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return new RawHTTPOutputNodeDialog();
    }
}
