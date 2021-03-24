/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
package org.knime.json.filehandling.reader;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.def.StringCell.StringCellFactory;
import org.knime.core.data.json.JSONCell;
import org.knime.core.data.json.JSONCellFactory;
import org.knime.core.data.json.JSONValue;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.streamable.BufferedDataTableRowOutput;
import org.knime.core.node.streamable.InputPortRole;
import org.knime.core.node.streamable.OutputPortRole;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.PortInput;
import org.knime.core.node.streamable.PortOutput;
import org.knime.core.node.streamable.RowOutput;
import org.knime.core.node.streamable.StreamableOperator;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.ReadPathAccessor;
import org.knime.filehandling.core.defaultnodesettings.status.NodeModelStatusConsumer;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage.MessageType;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingJsonFactory;

/**
 * This is the model implementation of JSONReader. Reads {@code .json} files to {@link JSONValue}s.
 *
 * @author Moditha Hewasinghage
 */
public final class JSONReaderNodeModel extends NodeModel {

    /**
     *
     */
    private static final int DOCS_TO_EXPLORE = 50;

    private final boolean m_hasInputPorts;

    private final NodeModelStatusConsumer m_statusConsumer;

    private final JSONReaderNodeConfiguration m_config;

    private final List<String> m_columnNames = new ArrayList<String>(); // column names when inferring JSON document

    /**
     * Constructor for the node model.
     *
     * @param portsConfig
     * @param config
     */
    public JSONReaderNodeModel(final PortsConfiguration portsConfig, final JSONReaderNodeConfiguration config) {
        super(portsConfig.getInputPorts(), portsConfig.getOutputPorts());
        m_hasInputPorts = portsConfig.getInputPorts().length > 0;
        m_config = config;
        m_statusConsumer = new NodeModelStatusConsumer(EnumSet.of(MessageType.ERROR, MessageType.WARNING));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData, final ExecutionContext exec) throws Exception {
        final BufferedDataTableRowOutput rowOutput =
            new BufferedDataTableRowOutput(exec.createDataContainer(createOutputSpec()));
        writeOutput(rowOutput, exec);
        return new PortObject[]{rowOutput.getDataTable()};
    }

    /**
     * Reads a JSON document into a row
     *
     * @param rowOutput
     * @param exec
     */
    private void writeOutput(final RowOutput rowOutput, final ExecutionContext exec)
        throws IOException, InvalidSettingsException, InterruptedException, CanceledExecutionException {

        try (final ReadPathAccessor accessor = m_config.getFileChooserSettings().createReadPathAccessor()) {
            final List<FSPath> fsPaths = accessor.getFSPaths(m_statusConsumer);
            m_statusConsumer.setWarningsIfRequired(this::setWarningMessage);
            long rec = 0;
            long rows = 0;
            final int numEntries = fsPaths.size();
            for (final FSPath p : fsPaths) {
                try (BufferedReader reader = Files.newBufferedReader(p)) {
                    if (!m_config.getInferColumns().getBooleanValue()) {
                        readAsOneCell(rowOutput, rec, reader);
                    } else {
                        rows = readAsTokens(rowOutput, rows, reader);
                    }
                    rec++;
                    final long curEntry = rec;
                    exec.checkCanceled();
                    exec.setProgress(rec / (double)numEntries,
                        () -> String.format("Processing file %d out of %d", curEntry, numEntries));
                }
            }
        } finally {
            rowOutput.close();
        }
    }

    /**
     * parse the JSON as a stream of tokens
     *
     * @param rowOutput
     * @param rows
     * @param reader
     * @return
     * @throws IOException
     * @throws InterruptedException
     * @throws JsonParseException
     */
    private long readAsTokens(final RowOutput rowOutput, long rows, final BufferedReader reader)
        throws IOException, InterruptedException, JsonParseException {
        final JsonFactory jfactory = new MappingJsonFactory();
        try (final JsonParser jParser = jfactory.createParser(reader)) {
            JsonToken token = jParser.nextToken();
            if (token == JsonToken.START_OBJECT) { // Top level JSON is a document
                extractDocumentCells(rowOutput, rows, jParser, token); // One row per file. Each attribute becomes a cell
                rows++;
            } else if (token == JsonToken.START_ARRAY) { // JSON is an array of objects.
                while (token != JsonToken.END_ARRAY) { // Each object inside the array becomes a row
                    token = jParser.nextToken();
                    if (token == JsonToken.START_OBJECT) {
                        extractDocumentCells(rowOutput, rows, jParser, token);
                        rows++;
                    } else if (token != JsonToken.END_ARRAY) {
                        //error in json
                        throw new IOException("Malformed JSON File");
                    }
                }
            } else {
                // Invalid JSON
                throw new IOException("Malformed JSON File");
            }
        }
        return rows;
    }

    /**
     * Read JSON content into one cell
     *
     * @param rowOutput
     * @param rec
     * @param reader
     * @throws IOException
     * @throws InterruptedException
     */
    private void readAsOneCell(final RowOutput rowOutput, final long rec, final BufferedReader reader)
        throws IOException, InterruptedException {
        final JSONValue jsonValue =
            (JSONValue)JSONCellFactory.create(reader, m_config.getAllowComments().getBooleanValue());
        final DataCell[] cells;
        cells = new DataCell[]{(DataCell)jsonValue};
        rowOutput.push(new DefaultRow(RowKey.createRowKey(rec), cells));
    }

    /**
     * Extracts cells per document attribute Array, document -> JSON Cell Others -> String Cell TODO : Infer other cell
     * types
     *
     * @param rowOutput
     * @param rec
     * @param jParser
     * @param token
     * @throws IOException
     * @throws InterruptedException
     */
    private void extractDocumentCells(final RowOutput rowOutput, final long row, final JsonParser jParser,
        JsonToken token) throws IOException, InterruptedException {
        final List<DataCell> cellList = new ArrayList<DataCell>();
        final HashMap<String, DataCell> colMap = new HashMap<String, DataCell>();
        while (token != JsonToken.END_OBJECT) {
            token = jParser.nextToken();
            switch (token) {
                case START_ARRAY:
                    JsonNode arrNode = jParser.readValueAsTree();
                    final JSONValue jsonValue = (JSONValue)JSONCellFactory.create(arrNode.toString(),
                        m_config.getAllowComments().getBooleanValue());
                    colMap.put(jParser.getCurrentName(), (DataCell)jsonValue);
                    break;
                case START_OBJECT:
                    JsonNode objNode = jParser.readValueAsTree();
                    final JSONValue objJsonValue = (JSONValue)JSONCellFactory.create(objNode.toString(),
                        m_config.getAllowComments().getBooleanValue());
                    colMap.put(jParser.getCurrentName(), (DataCell)objJsonValue);
                    break;
                case VALUE_FALSE:
                case VALUE_TRUE:
                case VALUE_NUMBER_FLOAT:
                case VALUE_NUMBER_INT:
                case VALUE_STRING:
                    final DataCell stringValue = StringCellFactory.create(jParser.getValueAsString());
                    colMap.put(jParser.getCurrentName(), stringValue);
                    break;
                default:
                    break;
            }
        }
        m_columnNames.forEach(c -> {
            if (colMap.containsKey(c)) {
                cellList.add(colMap.get(c));
            } else {
                cellList.add(DataType.getMissingCell());
            }
        });
        rowOutput.push(new DefaultRow(RowKey.createRowKey(row), cellList));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // No internal state to reset.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        m_config.getFileChooserSettings().configureInModel(inSpecs, m_statusConsumer);
        m_statusConsumer.setWarningsIfRequired(this::setWarningMessage);
        return new PortObjectSpec[]{null};

    }

    /**
     * Reads the first file to infer the document structure Array, document -> JSON Cell Others -> String Cell Keep the
     * column names in {@value #m_columnNames}. TODO: Better solution for this TODO : Infer other cell types, read
     * specific number of files/lines
     *
     * @return
     * @throws InvalidSettingsException
     * @throws IOException
     */
    private DataTableSpec createOutputSpec() throws JsonParseException, IOException, InvalidSettingsException {
        final Set<DataColumnSpec> columns = new LinkedHashSet<DataColumnSpec>();
        if (!m_config.getInferColumns().getBooleanValue()) {
            m_config.checkColumnName();
            columns
                .add(new DataColumnSpecCreator(m_config.getColumnName().getStringValue(), JSONCell.TYPE).createSpec());
        } else {
            try (final ReadPathAccessor accessor = m_config.getFileChooserSettings().createReadPathAccessor()) {
                final List<FSPath> fsPaths = accessor.getFSPaths(m_statusConsumer);
                final FSPath path = fsPaths.get(0);

                try (final BufferedReader reader = Files.newBufferedReader(path)) {
                    final JsonFactory jfactory = new MappingJsonFactory();
                    try (final JsonParser jParser = jfactory.createParser(reader)) {
                        JsonToken token = jParser.nextToken();
                        if (token == JsonToken.START_OBJECT) {
                            extractColumns(columns, jParser, token);
                        } else if (token == JsonToken.START_ARRAY) { // array of objects
                            int count = 0;
                            while (token != JsonToken.END_ARRAY && count < DOCS_TO_EXPLORE) {
                                token = jParser.nextToken();
                                if (token == JsonToken.START_OBJECT) {
                                    extractColumns(columns, jParser, token);
                                    count++;
                                } else if (token != JsonToken.END_ARRAY) {
                                    throw new JsonParseException(jParser, "Invalid JSON file in" + path.toString(),
                                        jParser.getCurrentLocation());
                                }
                            }
                        } else {
                            throw new JsonParseException(jParser, "Invalid JSON file in" + path.toString(),
                                jParser.getCurrentLocation());
                        }
                    } catch (JsonParseException e) {
                        throw e;
                    }
                }
            } catch (IOException e) {
                throw e;
            }
        }

        m_columnNames.clear();
        columns.forEach(c -> {
            m_columnNames.add(c.getName());
        });

        return new DataTableSpec(columns.toArray(new DataColumnSpec[columns.size()]));
    }

    /**
     * similar to {@link #extractDocumentCells}
     *
     * @param columns
     * @param jParser
     * @param token
     * @throws IOException
     */
    private static void extractColumns(final Set<DataColumnSpec> columns, final JsonParser jParser, JsonToken token)
        throws IOException {
        while (token != JsonToken.END_OBJECT) {
            token = jParser.nextToken();
            switch (token) {
                case START_ARRAY:
                    columns.add(new DataColumnSpecCreator(jParser.getCurrentName(), JSONCell.TYPE).createSpec());
                    jParser.skipChildren();
                    break;
                case START_OBJECT:
                    columns.add(new DataColumnSpecCreator(jParser.getCurrentName(), JSONCell.TYPE).createSpec());
                    jParser.skipChildren();
                    break;
                case VALUE_FALSE:
                case VALUE_TRUE:
                case VALUE_NUMBER_FLOAT:
                case VALUE_NUMBER_INT:
                case VALUE_STRING:
                    columns.add(new DataColumnSpecCreator(jParser.getCurrentName(), StringCell.TYPE).createSpec());
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_config.saveSettingsForModel(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_config.loadSettingsForModel(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_config.validateSettingsForModel(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        //No internal state.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        //No internal state.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StreamableOperator createStreamableOperator(final PartitionInfo partitionInfo,
        final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return new StreamableOperator() {
            @Override
            public void runFinal(final PortInput[] inputs, final PortOutput[] outputs, final ExecutionContext exec)
                throws Exception {
                writeOutput((RowOutput)outputs[0], exec);
            }

        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputPortRole[] getInputPortRoles() {
        if (m_hasInputPorts) {
            return new InputPortRole[]{InputPortRole.NONDISTRIBUTED_NONSTREAMABLE};
        } else {
            return new InputPortRole[]{};
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OutputPortRole[] getOutputPortRoles() {
        return new OutputPortRole[]{OutputPortRole.DISTRIBUTED};
    }
}
