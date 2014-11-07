package org.knime.json.node.toxml;

import java.io.File;
import java.io.IOException;

import javax.json.JsonValue;
import javax.xml.parsers.ParserConfigurationException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.data.MissingCell;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.json.JSONValue;
import org.knime.core.data.json.JacksonConversions;
import org.knime.core.data.xml.XMLCell;
import org.knime.core.data.xml.XMLCellFactory;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.json.internal.Activator;
import org.knime.json.node.util.SingleColumnReplaceOrAddNodeModel;
import org.knime.json.util.Json2Xml;

import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * This is the model implementation of JSONToXML.
 *
 * @author Gabor Bakos
 */
public class JSONToXMLNodeModel extends SingleColumnReplaceOrAddNodeModel<JSONToXMLSettings> {
    /**
     * Constructor for the node model.
     */
    protected JSONToXMLNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // No internal state
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        super.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.loadValidatedSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
        // No internal state
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
        // No internal state
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected CellFactory createCellFactory(final DataColumnSpec output, final int inputIndex,
        final int... otherColumns) {
        final JacksonConversions conv = Activator.getInstance().getJacksonConversions();

        //final XMLInputFactory fact = new InputFactoryProviderImpl().createInputFactory();
        //mapper.setSerializerProvider(new OutputFactoryProviderImpl().createOutputFactory());
        return new SingleCellFactory(output) {
            @Override
            public DataCell getCell(final DataRow row) {
                DataCell input = row.getCell(inputIndex);
                if (input instanceof JSONValue) {
                    JSONValue jsonValue = (JSONValue)input;
                    JsonValue json = jsonValue.getJsonValue();
                    TreeNode node = conv.toJackson(json);
                    DataCell xml = createXmlCell(node);
                    return xml;
                }
                return DataType.getMissingCell();
            }

            /**
             * @param node
             * @return
             */
            private DataCell createXmlCell(final TreeNode node) {
                try {
                    DataCell ret = XMLCellFactory.create(new Json2Xml().toXml((JsonNode)node));
                    return ret;
                } catch (IllegalArgumentException | /*JsonProcessingException |*/ParserConfigurationException
                        | IOException e) {
                    return new MissingCell(e.getMessage());
                }
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataColumnSpec createOutputSpec(final String outputColName) {
        return new DataColumnSpecCreator(outputColName, XMLCell.TYPE).createSpec();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected JSONToXMLSettings createSettings() {
        return createJSONToXMLSettings();
    }

    /**
     * @return
     */
    static JSONToXMLSettings createJSONToXMLSettings() {
        return new JSONToXMLSettings();
    }
}
