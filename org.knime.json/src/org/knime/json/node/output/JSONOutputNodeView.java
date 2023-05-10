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

import java.awt.Dimension;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.knime.core.node.NodeView;
import org.knime.json.util.JSONUtil;

import jakarta.json.JsonValue;

/**
 * This is the view of the JSON Output node. It shows the resulting pretty-printed JSON structure.
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
final class JSONOutputNodeView extends NodeView<JSONOutputNodeModel> {
    private final RSyntaxTextArea m_rSyntaxTextArea;

    JSONOutputNodeView(final JSONOutputNodeModel model) {
        super(model);
        m_rSyntaxTextArea = new RSyntaxTextArea();
        m_rSyntaxTextArea.setAntiAliasingEnabled(true);
        m_rSyntaxTextArea.setCodeFoldingEnabled(true);
        m_rSyntaxTextArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
        m_rSyntaxTextArea.setEditable(false);
        final RTextScrollPane comp = new RTextScrollPane(m_rSyntaxTextArea, true);
        comp.setMinimumSize(new Dimension(200, 150));
        comp.setName("JSON Snapshot");
        setComponent(comp);
    }

    /** {@inheritDoc} */
    @Override
    protected void onClose() {
        m_rSyntaxTextArea.setText("");
    }

    /** {@inheritDoc} */
    @Override
    protected void onOpen() {
    }

    /** {@inheritDoc} */
    @Override
    protected void modelChanged() {
        JsonValue viewJSONValue = getNodeModel().getViewJSONObject();
        if (viewJSONValue != null) {
            m_rSyntaxTextArea.setText(JSONUtil.toPrettyJSONString(viewJSONValue));
        }
    }
}
