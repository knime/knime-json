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
import org.knime.core.webui.node.dialog.NodeDialog;
import org.knime.core.webui.node.dialog.NodeDialogFactory;
import org.knime.core.webui.node.dialog.NodeDialogManager;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeDialog;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultKaiNodeInterface;
import org.knime.core.node.NodeFactory.NodeType;
import org.knime.core.webui.node.dialog.kai.KaiNodeInterface;
import org.knime.core.webui.node.dialog.kai.KaiNodeInterfaceFactory;
import org.knime.core.node.NodeDescription;
import org.knime.node.impl.description.DefaultNodeDescriptionUtil;
import java.util.Map;
import org.knime.node.impl.description.PortDescription;
import java.util.List;
import org.knime.node.impl.description.ViewDescription;
import static org.knime.node.impl.description.PortDescription.fixedPort;

/**
 * This is the factory for the JSON Output node.
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 * @author Paul Baernreuther, KNIME GmbH, Germany
 * @author AI Migration Pipeline v1.1
 */
@SuppressWarnings("restriction")
public final class JSONOutputNodeFactory extends NodeFactory<JSONOutputNodeModel> implements NodeDialogFactory, KaiNodeInterfaceFactory {
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
    private static final String NODE_NAME = "Container Output (JSON)";
    private static final String NODE_ICON = "./json-out.png";
    private static final String SHORT_DESCRIPTION = """
            Reads the content of a JSON column and makes it available to an external caller.
            """;
    private static final String FULL_DESCRIPTION = """
            Reads the content of a JSON column and makes it available to an external caller. This node is used in
                workflows deployed as REST services, where the result of the web service is represented by the input of
                this node. By default a JSON array is returned, where each row of the input table is an element in the
                array. Tables with no or only one row result in an array with zero or one element. This behavior can be
                changed in the dialog, see the settings below.
            """;
    private static final List<PortDescription> INPUT_PORTS = List.of(
            fixedPort("JSON Input", """
                A table containing a JSON column whose content is made available to the caller.
                """)
    );
    private static final List<PortDescription> OUTPUT_PORTS = List.of();
    private static final List<ViewDescription> VIEWS = List.of(
            new ViewDescription("JSON Snapshot", """
                Shows the JSON structure as read from the input (possibly converted into JSON array)
                """)
    );

    @Override
    public NodeDialogPane createNodeDialogPane() {
        return NodeDialogManager.createLegacyFlowVariableNodeDialog(createNodeDialog());
    }

    @Override
    public NodeDialog createNodeDialog() {
        return new DefaultNodeDialog(SettingsType.MODEL, JSONOutputNodeParameters.class);
    }

    @Override
    public NodeDescription createNodeDescription() {
        return DefaultNodeDescriptionUtil.createNodeDescription(
            NODE_NAME,
            NODE_ICON,
            INPUT_PORTS,
            OUTPUT_PORTS,
            SHORT_DESCRIPTION,
            FULL_DESCRIPTION,
            List.of(),
            JSONOutputNodeParameters.class,
            VIEWS,
            NodeType.Container,
            List.of(),
            null
        );
    }

    @Override
    public KaiNodeInterface createKaiNodeInterface() {
        return new DefaultKaiNodeInterface(Map.of(SettingsType.MODEL, JSONOutputNodeParameters.class));
    }
}
