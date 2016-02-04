/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   14 Sept. 2014 (Gabor): created
 */
package org.knime.json.node.fromxml;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.data.MissingCell;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.json.JSONCellFactory;
import org.knime.core.data.json.JacksonConversions;
import org.knime.core.data.xml.XMLValue;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.json.node.util.SingleColumnReplaceOrAddNodeModel;
import org.knime.json.util.Xml2Json;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.fasterxml.jackson.databind.JsonNode;

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
//        final XmlMapper mapper = new XmlMapper();
//        ClassLoader cl = Thread.currentThread().getContextClassLoader();
//        try {
//            Thread.currentThread().setContextClassLoader(Activator.getInstance().getJsr353ClassLoader());
//            mapper.registerModule(new JSR353Module());
//        } finally {
//            Thread.currentThread().setContextClassLoader(cl);
//        }
//        mapper.getFactory().setXMLTextElementName(getSettings().getTextKey());
//        final XMLInputFactory fact = new InputFactoryProviderImpl().createInputFactory();
//        mapper.setNodeFactory(JsonNodeFactory.withExactBigDecimals(true));
        final JacksonConversions conv = JacksonConversions.getInstance();
        return new SingleCellFactory(output) {
            private final Xml2Json xml2Json = Xml2Json.proposedSettings().setTextKey(getSettings().getTextKey())
                .setTranslateComments(getSettings().isTranslateComments())
                .setTranslateProcessingInstructions(getSettings().isTranslateProcessingInstructions());

            @Override
            public DataCell getCell(final DataRow row) {
                    DataCell cell = row.getCell(inputIndex);
                    if (cell instanceof XMLValue) {
                        XMLValue xmlValue = (XMLValue)cell;
                        Document doc = xmlValue.getDocument();
                        try {
                            DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                            Document newRoot = documentBuilder.newDocument();
                            Element element = newRoot.createElement("fakeroot");
                            element.appendChild(newRoot.importNode(doc.getDocumentElement(), true));
                            newRoot.appendChild(element);
                            JsonNode json = xml2Json.toJson(newRoot);
                            return JSONCellFactory.create(conv.toJSR353(json));
                        } catch (FactoryConfigurationError
                                | ParserConfigurationException e) {
                            return new MissingCell(e.getMessage());
                        }
                    }
                    return DataType.getMissingCell();
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
