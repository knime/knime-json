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
 *   May 4, 2018 (Tobias Urhaug, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.json.node.container.output.table;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import javax.json.JsonValue;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.json.container.table.ContainerTableJsonSchema;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.BufferedDataTableHolder;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.dialog.ExternalNodeData;
import org.knime.core.node.dialog.OutputNode;
import org.knime.core.util.FileUtil;
import org.knime.json.node.container.input.table.ContainerTableDefaultJsonStructure;
import org.knime.json.node.container.mappers.ContainerTableMapper;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The model implementation of the Container Output (Table) node.
 * Creates a json output conforming to {@link ContainerTableJsonSchema}
 * and makes it available for a caller through the {@link OutputNode} interface.
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 * @since 3.6
 */
final class ContainerTableOutputNodeModel extends NodeModel implements BufferedDataTableHolder, OutputNode {

    private ContainerTableOutputNodeConfiguration m_configuration = new ContainerTableOutputNodeConfiguration();
    private BufferedDataTable m_inputTable;

    /**
     * Constructor for the node model.
     */
    ContainerTableOutputNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
            throws Exception {
        m_inputTable = inData[0];
        validateInputTable();
        writeOutputAsJsonFileIfDestinationPresent();
        return inData;
    }

    /**
     * This wrapper method is only introduced for improved readability in the execute method.
     * @throws InvalidSettingsException if input table cannot be parsed to {@link ContainerTableJsonSchema}
     */
    private void validateInputTable() throws InvalidSettingsException {
        getOutputContainerTable();
    }

    private JsonValue getOutputContainerTable() throws InvalidSettingsException {
        return m_inputTable == null //
                ? ContainerTableDefaultJsonStructure.asJsonValue() //
                : ContainerTableMapper.toContainerTableJsonValue(m_inputTable); //
    }

    private void writeOutputAsJsonFileIfDestinationPresent() throws InvalidSettingsException {
        Optional<String> outputPathOrUrlOptional = m_configuration.getOutputPathOrUrl();
        if (outputPathOrUrlOptional.isPresent()) {
            writeOutputAsJsonFile(outputPathOrUrlOptional.get());
        }
    }

    private void writeOutputAsJsonFile(final String outputPathOrUrl) throws InvalidSettingsException {
        try {
            URL url = FileUtil.toURL(outputPathOrUrl);
            if (isLocalURL(url)) {
                Path path = FileUtil.resolveToPath(url);
                try (OutputStream outputStream = Files.newOutputStream(path)) {
                    new ObjectMapper().writeValue(outputStream, ContainerTableMapper.toContainerTable(m_inputTable));
                }
            } else {
                URLConnection urlConnection = FileUtil.openOutputConnection(url, "PUT");
                try (OutputStream outputStream = urlConnection.getOutputStream()) {
                    new ObjectMapper().writeValue(outputStream, ContainerTableMapper.toContainerTable(m_inputTable));
                }
            }
        } catch (IOException | URISyntaxException e) {
            throw new InvalidSettingsException("Cannot write to configured path: \"" + outputPathOrUrl + "\"");
        }
    }

    private static boolean isLocalURL(final URL url) {
        return StringUtils.equalsIgnoreCase(url.getProtocol(), "knime")
                || StringUtils.equalsIgnoreCase(url.getProtocol(), "file");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        return inSpecs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExternalNodeData getExternalOutput() {
        return
            ExternalNodeData.builder(m_configuration.getParameterName())
                .description(m_configuration.getDescription())
                .jsonValue(computeExternalOutput())
                .build();
    }

    /**
     * Wrapper method that swallows the InvalidSettingsException thrown when computing the output container table
     * and throws a RuntimeException instead.
     * The method must swallow the exception for the getExternalOutput() method to comply with the method it is
     * overriding.
     */
    private JsonValue computeExternalOutput() {
        try {
            return getOutputContainerTable();
        } catch (InvalidSettingsException e) {
            throw new RuntimeException("Error while parsing output table", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        // Handled by BufferedDataTableHolder
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        // Handled by BufferedDataTableHolder
    }

    /** {@inheritDoc} */
    @Override
    public BufferedDataTable[] getInternalTables() {
        return new BufferedDataTable[] {m_inputTable};
    }

    /** {@inheritDoc} */
    @Override
    public void setInternalTables(final BufferedDataTable[] tables) {
        m_inputTable = tables[0];
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
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        new ContainerTableOutputNodeConfiguration().loadInModel(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_configuration = new ContainerTableOutputNodeConfiguration().loadInModel(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_inputTable = null;
    }

}
