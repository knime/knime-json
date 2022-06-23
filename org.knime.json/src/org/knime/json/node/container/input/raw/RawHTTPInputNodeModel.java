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
 *   Created on Feb 15, 2015 by wiswedel
 */
package org.knime.json.node.container.input.raw;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.json.JsonObject;
import javax.json.JsonValue;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.RowKey;
import org.knime.core.data.blob.BinaryObjectCellFactory;
import org.knime.core.data.blob.BinaryObjectDataCell;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.dialog.ExternalNodeData;
import org.knime.core.node.dialog.InputNode;

/**
 * This is the model implementation of JSONInput. Allows to read a text and return it as a JSON value.
 *
 * @author Alexander Fillbrunn, KNIME
 */
final class RawHTTPInputNodeModel extends NodeModel implements InputNode {
    /**
     *
     */
    private static final String PARAM_NAME = "raw-http-input";

    private RawHTTPInputNodeConfiguration m_configuration = new RawHTTPInputNodeConfiguration();

    private URI m_resourceURI;
    private JsonObject m_headers;
    private JsonObject m_queryParams;

    private static final String DATA_URI_REGEX = "^data:(?<mediatype>[^;,]+(?:;charset=[^;,]+)?)?(?:;(?<encoding>[^,]+))?,(?<data>.*)$";
    Pattern DATA_URI_PATTERN = Pattern.compile(DATA_URI_REGEX);

    /**
     * Constructor for the node model.
     */
    protected RawHTTPInputNodeModel() {
        super(0, 3);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws Exception {

        DataTableSpec bodySpec = createBodySpec();

        // Body table
        final BufferedDataContainer bodyContainer = exec.createDataContainer(bodySpec);
        DataCell binaryData;
        // No external value: use config
        if (m_resourceURI == null) {
            byte[] data = Base64.getDecoder().decode(m_configuration.getBody());
            binaryData = new BinaryObjectCellFactory().create(data);
        } else {
            Matcher m = DATA_URI_PATTERN.matcher(m_resourceURI.toString());
            if (m.matches()) {
                String data = m.group("data");
                binaryData = new BinaryObjectCellFactory().create(Base64.getDecoder().decode(data));
            } else {
                binaryData = new BinaryObjectCellFactory().create(new FileInputStream(new File(m_resourceURI)));
            }
        }
        bodyContainer.addRowToTable(new DefaultRow(RowKey.createRowKey(0L), binaryData));
        bodyContainer.close();

        // Header table
        BufferedDataContainer headerContainer;
        if (m_headers != null) {
            DataTableSpec headerSpec = createSpecFromJsonObject(m_headers);
            headerContainer = exec.createDataContainer(headerSpec);
            headerContainer.addRowToTable(new DefaultRow(RowKey.createRowKey(0L),
                StreamSupport.stream(m_headers.values().spliterator(), false)
                .map(v -> new StringCell(v.toString()))
                .collect(Collectors.toList())));

        } else {
            DataTableSpec headerSpec = createHeaderSpec();
            headerContainer = exec.createDataContainer(headerSpec);
            headerContainer.addRowToTable(new DefaultRow(RowKey.createRowKey(0L),
                StreamSupport.stream(m_configuration.getHeaders().values().spliterator(), false)
                .map(v -> new StringCell(v))
                .collect(Collectors.toList())));
        }
        headerContainer.close();

        // Query parameter table
        BufferedDataContainer qpContainer;
        if (m_headers != null) {
            DataTableSpec qpSpec = createSpecFromJsonObject(m_queryParams);
            qpContainer = exec.createDataContainer(qpSpec);
            qpContainer.addRowToTable(new DefaultRow(RowKey.createRowKey(0L),
                StreamSupport.stream(m_queryParams.values().spliterator(), false)
                .map(v -> new StringCell(v.toString()))
                .collect(Collectors.toList())));
        } else {
            DataTableSpec qpSpec = createQueryParamSpec();
            qpContainer = exec.createDataContainer(qpSpec);
            qpContainer.addRowToTable(new DefaultRow(RowKey.createRowKey(0L),
                StreamSupport.stream(m_configuration.getQueryParams().values().spliterator(), false)
                .map(v -> new StringCell(v))
                .collect(Collectors.toList())));
        }
        qpContainer.close();

        return new BufferedDataTable[]{
            bodyContainer.getTable(),
            headerContainer.getTable(),
            qpContainer.getTable()};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        //No internal state.
    }

    private static DataTableSpec createSpecFromJsonObject(final JsonObject o) {
        DataTableSpecCreator creator = new DataTableSpecCreator();
        for (String key : o.keySet()) {
            creator.addColumns(new DataColumnSpecCreator(key.toLowerCase(), StringCell.TYPE).createSpec());
        }
        return creator.createSpec();
    }

    private static DataTableSpec createBodySpec() {
        return new DataTableSpecCreator().addColumns(
            new DataColumnSpecCreator("body", BinaryObjectDataCell.TYPE).createSpec()).createSpec();
    }

    private DataTableSpec createHeaderSpec() {
        DataTableSpecCreator creator = new DataTableSpecCreator();
        // TODO: sort?
        for (String header : m_configuration.getHeaders().keySet()) {
            creator.addColumns(new DataColumnSpecCreator(header.toLowerCase(), StringCell.TYPE).createSpec());
        }
        return creator.createSpec();
    }

    private DataTableSpec createQueryParamSpec() {
        DataTableSpecCreator creator = new DataTableSpecCreator();
        // TODO: sort?
        for (String param : m_configuration.getQueryParams().keySet()) {
            creator.addColumns(new DataColumnSpecCreator(param.toLowerCase(), StringCell.TYPE).createSpec());
        }
        return creator.createSpec();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        return new DataTableSpec[]{
            createBodySpec(),
            m_headers != null ? createSpecFromJsonObject(m_headers) : createHeaderSpec(),
            m_queryParams != null ? createSpecFromJsonObject(m_queryParams) : createQueryParamSpec()
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_configuration.save(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_configuration = new RawHTTPInputNodeConfiguration().loadInModel(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        new RawHTTPInputNodeConfiguration().loadInModel(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
        //No internal state.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
        //No internal state.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExternalNodeData getInputData() {
        return ExternalNodeData.builder(PARAM_NAME)
                .resource(URI.create("data:application/octet-stream;base64,ABC"))
                .description("")
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validateInputData(final ExternalNodeData inputData) throws InvalidSettingsException {
        // nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setInputData(final ExternalNodeData inputData) throws InvalidSettingsException {
        if (inputData != null) {
            if (inputData.getResource() != null) {
                m_resourceURI = inputData.getResource();
            }
            if (inputData.getJSONValue() != null) {
                JsonValue jsonValue = inputData.getJSONValue();
                m_headers = jsonValue.asJsonObject().getJsonObject("headers");
                m_queryParams = jsonValue.asJsonObject().getJsonObject("query_parameters");
            }
        }
    }

    @Override
    public boolean isInputDataRequired() {
        return false;
    }

    @Override
    public boolean isUseAlwaysFullyQualifiedParameterName() {
        return true;
    }
}
