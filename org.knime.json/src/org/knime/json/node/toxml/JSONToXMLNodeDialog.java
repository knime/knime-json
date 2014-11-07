package org.knime.json.node.toxml;

import org.knime.core.data.json.JSONValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.json.node.util.ReplaceOrAddColumnDialog;

/**
 * <code>NodeDialog</code> for the "JSONToXML" Node.
 *
 * @author Gabor Bakos
 */
public class JSONToXMLNodeDialog extends ReplaceOrAddColumnDialog<JSONToXMLSettings> {
    /**
     * New pane for configuring the JSONToXML node.
     */
    protected JSONToXMLNodeDialog() {
        super(JSONToXMLNodeModel.createJSONToXMLSettings(), "JSON column", JSONValue.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        super.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        super.loadSettingsFrom(settings, specs);
    }
}
