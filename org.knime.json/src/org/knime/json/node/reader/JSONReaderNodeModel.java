package org.knime.json.node.reader;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.InvalidPathException;

import org.apache.commons.io.FilenameUtils;
import org.knime.base.node.util.BufferedFileReader;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.json.JSONCell;
import org.knime.core.data.json.JSONCellFactory;
import org.knime.core.data.json.JSONValue;
import org.knime.core.data.json.JacksonConversions;
import org.knime.core.data.uri.URIContent;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.util.FileUtil;
import org.knime.json.internal.Activator;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.jsonpointer.JsonPointer;
import com.github.fge.jackson.jsonpointer.JsonPointerException;

/**
 * This is the model implementation of JSONReader. Reads {@code .json} files to {@link JSONValue}s.
 *
 * @author Gabor Bakos
 */
public final class JSONReaderNodeModel extends NodeModel {
    private final JSONReaderSettings m_settings = createSettings();

    /**
     * Constructor for the node model.
     */
    protected JSONReaderNodeModel() {
        super(0, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec) throws Exception {
        BufferedDataContainer container = exec.createDataContainer(configure((PortObjectSpec[])null)[0]);
        int rowId = 1;
        URL url = FileUtil.toURL(m_settings.getLocation());
        try {
            File file = FileUtil.getFileFromURL(url);
            rowId =
                readUriContent(container, rowId,
                    new URIContent(file.toURI(), FilenameUtils.getExtension(file.getName())));
        } catch (IllegalArgumentException e) {
            //No problems
            //We read as a file, this is not a folder.
            rowId =
                readUriContent(container, rowId, new URIContent(url.toURI(), FilenameUtils.getExtension(url.getPath())));
        }
        container.close();
        return new BufferedDataTable[]{container.getTable()};
    }

    /**
     * Reads the {@link URI}s.
     *
     * @param container The output container.
     * @param rowId The current row id.
     * @param content The {@link URIContent} to read.
     * @return The new row id.
     * @throws IOException Cannot read the content.
     * @throws MalformedURLException Wrong {@link URI} input.
     */
    private int readUriContent(final BufferedDataContainer container, int rowId, final URIContent content)
        throws IOException, MalformedURLException {
        JsonPointer jsonPointer;
        try {
            jsonPointer = new JsonPointer(m_settings.getJsonPointer());
        } catch (JsonPointerException e) {
            throw new IllegalStateException("The pointer has invalid syntax: " + m_settings.getJsonPointer());
        }
        JacksonConversions jacksonConversions = Activator.getInstance().getJacksonConversions();
        try (BufferedFileReader reader = BufferedFileReader.createNewReader(content.getURI().toURL())) {
            //do {
            JSONValue jsonValue = (JSONValue)JSONCellFactory.create(reader,
                m_settings.isAllowComments());
            DataCell value = (DataCell)jsonValue;
            if (m_settings.isSelectPart()) {
                JsonNode found = jsonPointer.get(jacksonConversions.toJackson(jsonValue.getJsonValue()));
                if (found == null) {
                    if (m_settings.isFailIfNotFound()) {
                        throw new NullPointerException("Not found " + m_settings.getJsonPointer() + " in\n" + jsonValue);
                } else {
                    value = DataType.getMissingCell();
                }
                } else {
                    value = JSONCellFactory.create(jacksonConversions.toJSR353(found));
                }
            }//else we already set.
            container.addRowToTable(new DefaultRow(RowKey.createRowKey(rowId++), value));
            //} while (reader.hasMoreZipEntries());
        }
        return rowId;
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
    protected DataTableSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        URL url;
        try {
            url = FileUtil.toURL(m_settings.getLocation());
        } catch (MalformedURLException | InvalidPathException e) {
            throw new InvalidSettingsException("Not a valid location: " + m_settings.getLocation(), e);
        }
        try {
            File file = FileUtil.getFileFromURL(url);
            if (!file.exists()) {
                throw new InvalidSettingsException("File do not exists: " + m_settings.getLocation());
            }
            if (file.isDirectory()) {
                throw new InvalidSettingsException("Selected location is a directory: " + m_settings.getLocation());
            }
        } catch (IllegalArgumentException e) {
            //We do not mind if it is remote file, but in this case we do not check for existence either.
        }
        final DataColumnSpec column = new DataColumnSpecCreator(m_settings.getColumnName(), JSONCell.TYPE).createSpec();
        return new DataTableSpec[]{new DataTableSpec(column)};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_settings.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_settings.validateSettings(settings);
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
     * @param url The new url where the location setting should point to.
     */
    public void setUrl(final URL url) {
        m_settings.setLocation(url.toString());
    }

    /**
     * @return The default settings.
     */
    static JSONReaderSettings createSettings() {
        return new JSONReaderSettings();
    }
}
