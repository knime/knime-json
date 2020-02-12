package org.knime.json.node.container.output.row;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Optional;

import javax.json.JsonValue;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.CloseableRowIterator;
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
import org.knime.core.node.dialog.OutputNode;
import org.knime.json.node.container.io.FilePathOrURLWriter;
import org.knime.json.node.container.mappers.row.ContainerRowMapper;

/**
 * This is the model implementation of ContainerRowOutput.
 *
 * Creates a simple json representation of the first row of the input table and makes it available to external callers.
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 * @since 4.2
 */
public class ContainerRowOutputNodeModel extends NodeModel implements OutputNode {

    /**
     * Helper to save the node's configuration pre-configured with some values to a nodes settings object.
     *
     * @param settings
     * @param parameterName
     * @throws InvalidSettingsException
     */
    public static void saveConfigAsNodeSettings(final NodeSettingsWO settings, final String parameterName)
        throws InvalidSettingsException {
        ContainerRowOutputNodeConfiguration config = new ContainerRowOutputNodeConfiguration();
        config.setParameterName(parameterName);
        config.save(settings);
    }


    private ContainerRowOutputNodeConfiguration m_configuration = new ContainerRowOutputNodeConfiguration();
    private JsonValue m_outputRow;

    /**
     * Constructor for the node model.
     */
    protected ContainerRowOutputNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
            throws Exception {
        m_outputRow = ContainerRowMapper.firstRowToJsonValue(inData[0]);
        writeOutputAsJsonFileIfDestinationPresent();
        return new BufferedDataTable[]{ createTableFromFirstRow(exec, inData[0]) };
    }

    private void writeOutputAsJsonFileIfDestinationPresent() throws InvalidSettingsException {
        Optional<String> outputPathOrUrlOptional = m_configuration.getOutputPathOrUrl();
        if (outputPathOrUrlOptional.isPresent()) {
            String outputPathOrUrl = outputPathOrUrlOptional.get();
            try {
                FilePathOrURLWriter.writeAsJson(outputPathOrUrl, m_outputRow);
            } catch (IOException e) {
                throw new InvalidSettingsException("Error when writing the table as json file ", e);
            } catch (URISyntaxException e) {
                throw new InvalidSettingsException("The path or URL '" + outputPathOrUrl + "' is invalid", e);
            }
        }
    }

    private static BufferedDataTable createTableFromFirstRow(final ExecutionContext exec,
        final BufferedDataTable inputTable) {
        BufferedDataContainer dataContainer = exec.createDataContainer(inputTable.getDataTableSpec());
        try (CloseableRowIterator iterator = inputTable.iterator()) {
            if (iterator.hasNext()) {
                dataContainer.addRowToTable(iterator.next());
            }
        }
        dataContainer.close();
        return dataContainer.getTable();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        return inSpecs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExternalNodeData getExternalOutput() {
        JsonValue jsonValue = m_outputRow != null ? m_outputRow : JsonValue.NULL;
        return
            ExternalNodeData.builder(m_configuration.getParameterName())
                .description(m_configuration.getDescription())
                .jsonValue(jsonValue)
                .build();
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
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_configuration = new ContainerRowOutputNodeConfiguration().loadInModel(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        new ContainerRowOutputNodeConfiguration().loadInModel(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        //No internal state.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        //No internal state.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // No internal state.
    }

}

