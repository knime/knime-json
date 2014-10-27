package org.knime.json.node.fromxml;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.dom.DOMSource;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.data.MissingCell;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.json.JSONCellFactory;
import org.knime.core.data.json.JSONValue;
import org.knime.core.data.json.JacksonConversions;
import org.knime.core.data.xml.XMLValue;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.json.internal.Activator;
import org.knime.json.node.util.SingleColumnReplaceOrAddNodeModel;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.ctc.wstx.osgi.InputFactoryProviderImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jsr353.JSR353Module;

/**
 * This is the model implementation of XMLToJSON. Converts XML values to JSON values.
 *
 * @author Gabor Bakos
 */
public class XMLToJSONNodeModel extends SingleColumnReplaceOrAddNodeModel<XMLToJSONSettings> {
    /**
     * Constructor for the node model.
     */
    protected XMLToJSONNodeModel() {
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
        final XmlMapper mapper = new XmlMapper();
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(Activator.getInstance().getJsr353ClassLoader());
            mapper.registerModule(new JSR353Module());
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
        }
        mapper.getFactory().setXMLTextElementName(getSettings().getTextKey());
        final XMLInputFactory fact = new InputFactoryProviderImpl().createInputFactory();
        mapper.setNodeFactory(JsonNodeFactory.withExactBigDecimals(true));
        final JacksonConversions conv = Activator.getInstance().getJacksonConversions();
        return new SingleCellFactory(output) {

            @Override
            public DataCell getCell(final DataRow row) {
                ClassLoader clInner = Thread.currentThread().getContextClassLoader();
                try {
                    Thread.currentThread().setContextClassLoader(JSONValue.class.getClassLoader());
                    DataCell cell = row.getCell(inputIndex);
                    if (cell instanceof XMLValue) {
                        XMLValue xmlValue = (XMLValue)cell;
                        Document doc = xmlValue.getDocument();
                        try {
                            DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                            Document newRoot = documentBuilder.newDocument();
                            Element element = newRoot.createElement("fakeroot");
                            element.appendChild(newRoot.importNode(doc.getDocumentElement(), true));
                            XMLStreamReader reader = fact.createXMLStreamReader(new DOMSource(element));
                            JsonNode treeNode = mapper.readValue(reader, JsonNode.class);
                            //TODO find a way to skip serialization to String and parsing to JsonValue.
                            //reader = fact.createXMLStreamReader(new DOMSource(doc.getDocumentElement()));
                            return JSONCellFactory.create(conv.toJSR353(treeNode));
                            /*mapper.readValue(reader, JsonObject.class)*///treeNode.toString(), false);
                        } catch (IOException | XMLStreamException | FactoryConfigurationError
                                | ParserConfigurationException e) {
                            return new MissingCell(e.getMessage());
                        }
                    }
                    return DataType.getMissingCell();
                } finally {
                    Thread.currentThread().setContextClassLoader(clInner);
                }
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected XMLToJSONSettings createSettings() {
        return createXMLToSJONSettings();
    }

    /**
     * @return
     */
    static XMLToJSONSettings createXMLToSJONSettings() {
        return new XMLToJSONSettings();
    }

}
