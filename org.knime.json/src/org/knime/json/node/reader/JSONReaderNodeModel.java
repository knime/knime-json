package org.knime.json.node.reader;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import org.knime.base.node.util.BufferedFileReader;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.json.JSONCell;
import org.knime.core.data.json.JSONCellFactory;
import org.knime.core.data.json.JSONValue;
import org.knime.core.data.uri.IURIPortObject;
import org.knime.core.data.uri.URIContent;
import org.knime.core.data.uri.URIPortObject;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;

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
        super(new PortType[]{new PortType(URIPortObject.class, true)}, new PortType[]{BufferedDataTable.TYPE});
    }

    /**
     * @return A {@link SettingsModelBoolean} to allow or not comments in the input.
     */
    static SettingsModelBoolean createAllowComments() {
        return new SettingsModelBoolean("allow.comments", false);
    }

    /**
     * @return A {@link SettingsModelBoolean} to filter the content loaded from {@link #m_location} to {@code .json}
     *         files ({@code true}).
     */
    static SettingsModelBoolean createProcessOnlyJson() {
        return new SettingsModelBoolean("process.only.json", true);
    }

    /**
     * @return A {@link SettingsModelString} for the column name of the resulting JSON content.
     */
    static SettingsModelString createColumnName() {
        return new SettingsModelString("column.name", "json");
    }

    /**
     * @return A {@link SettingsModelString} for the location of the JSON content (as a {@link URL}'s String
     *         representation).
     */
    static SettingsModelString createLocation() {
        return new SettingsModelString("location", "");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final PortObject[] inData, final ExecutionContext exec) throws Exception {
        BufferedDataContainer container = exec.createDataContainer(configure((PortObjectSpec[])null)[0]);
        int rowId = 1;
        if (inData[0] != null && inData[0] instanceof IURIPortObject) {
            IURIPortObject uriPo = (IURIPortObject)inData[0];
            for (URIContent content : uriPo.getURIContents()) {
                rowId = readUriContent(container, rowId, content);
            }
        } else {
            rowId = readUriContent(container, rowId, new URIContent(new URI(m_settings.getLocation()), ".json"));
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
        try (BufferedFileReader reader = BufferedFileReader.createNewReader(content.getURI().toURL())) {
            //do {
            if (!content.getExtension().endsWith("json")
                && !(reader.isZippedSource() && reader.getZipEntryName().toLowerCase().endsWith(".json"))) {
                if (m_settings.isProcessOnlyJson()) {
                    throw new IllegalStateException("The file is not a .json file: " + content.getExtension()
                        + (reader.isZippedSource() ? ", " + reader.getZipEntryName() : ""));
                //} else { we can just test whether we can read it.
                }
            }
            container.addRowToTable(new DefaultRow("Row" + rowId++, JSONCellFactory.create(reader,
                m_settings.isAllowComments())));
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
