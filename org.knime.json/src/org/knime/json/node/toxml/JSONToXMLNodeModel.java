package org.knime.json.node.toxml;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.json.JsonValue;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.data.MissingCell;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.ListCell;
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
import org.xml.sax.SAXException;

import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator.Feature;

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
        JacksonXmlModule module = new JacksonXmlModule();
        module.setDefaultUseWrapper(false);
        final XmlMapper mapper = new XmlMapper(module);
        mapper.configure(Feature.WRITE_XML_DECLARATION, true);
        mapper.configure(Feature.WRITE_XML_1_1, true);
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
                    if (node.isArray()) {
                        if (getSettings().isCreateArrays()) {
                            return createArrayCell(node);
                        }
                        throw new IllegalArgumentException("Arrays cannot be saved as a single xml (row: "
                            + row.getKey() + ").");
                    }
                    DataCell xml = createXmlCell(node);
                    return getSettings().isCreateArrays() ? CollectionCellFactory.createListCell(Collections
                        .singletonList(xml)) : xml;
                }
                return DataType.getMissingCell();
            }

            private DataCell createArrayCell(final TreeNode node) {
                if (node.isArray()) {
                    List<DataCell> cells = new ArrayList<>(node.size());
                    for (int i = 0; i < node.size(); i++) {
                        cells.add(createXmlCell(node.get(i)));
                    }
                    return CollectionCellFactory.createListCell(cells);
                }
                return createXmlCell(node);
            }

            /**
             * @param node
             * @return
             */
            private DataCell createXmlCell(final TreeNode node) {
                //              try {
                //DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                //DomElementJsonDeserializer dejd = new DomElementJsonDeserializer(builder);
                //dejd.deserialize(node.traverse(), ctxt)
                //Document dom = mapper.readValue(json.toString(), Document.class);
                try {
                    //                    System.out.println(mapper.writeValueAsString(node));
                    //Node elem = mapper.convertValue(node, Node.class);
                    DataCell ret = XMLCellFactory.create(/*elem.getOwnerDocument()*/mapper.writeValueAsString(node));
                    return ret;
                } catch (IllegalArgumentException | /*JsonProcessingException |*/ParserConfigurationException
                        | SAXException | XMLStreamException | IOException e) {
                    return new MissingCell(e.getMessage());
                }
                //              } catch (IOException | ParserConfigurationException e) {
                //              return new MissingCell(e.getMessage());
                //          }
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataColumnSpec createOutputSpec(final String outputColName) {
        return new DataColumnSpecCreator(outputColName, getSettings().isCreateArrays()
            ? ListCell.getCollectionType(XMLCell.TYPE) : XMLCell.TYPE).createSpec();
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
