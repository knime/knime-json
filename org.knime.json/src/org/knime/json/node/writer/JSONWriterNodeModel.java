package org.knime.json.node.writer;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FilenameUtils;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.json.JSONCellWriter;
import org.knime.core.data.json.JSONCellWriterFactory;
import org.knime.core.data.json.JSONValue;
import org.knime.core.data.uri.URIContent;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.util.FileUtil;
import org.knime.json.internal.Activator;
import org.knime.json.node.util.ReplaceOrAddColumnSettings;

/**
 * This is the model implementation of JSONWriter. Writes {@code .json} files from {@link JSONValue}s.
 *
 * @author Gabor Bakos
 */
public final class JSONWriterNodeModel extends NodeModel {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(JSONWriterNodeModel.class);

    private static final String OUTPUT_LOCATION_FLOW_VAR_NAME = "json.writer.location";

    /** The settings for the save. */
    private final JSONWriterNodeSettings m_settings = new JSONWriterNodeSettings();

    /**
     * Constructor for the node model.
     */
    protected JSONWriterNodeModel() {
        super(1, 0);
    }

    static interface Container extends AutoCloseable {
        /**
         * @param fileName The name of the file to be created.
         * @return The {@link OutputStream} for the file. <b>Do not call {@link OutputStream#close()}, use instead
         *         {@link #closeStream(OutputStream)}!</b>
         * @throws IOException Something went wrong creating the {@link OutputStream} (probably the file already
         *             exists).
         * @see {@link #closeStream(OutputStream)}
         */
        OutputStream nextOutputStream(String fileName) throws IOException;

        /**
         * Closes the stream obtained by {@link #nextOutputStream(String)} if necessary.
         *
         * @param stream A stream obtained by {@link #nextOutputStream(String)}.
         * @throws IOException Could not close.
         */
        void closeStream(OutputStream stream) throws IOException;

        /**
         * @param fileName The file name to check for existence and then delete if exists.
         * @throws IOException If cannot delete the file.
         */
        void ifExistsDelete(String fileName) throws IOException;

        /**
         * Deletes the generated resources if possible.
         * @throws IOException When cannot delete the file from the file system.
         */
        void deleteGenerated() throws IOException;
    }

    static class ZipContainer implements Container {
        private ZipOutputStream m_zipOutputStream;
        private Path m_path;

        /**
         * @throws IOException Error opening the connection.
         * @throws URISyntaxException Wrong container.
         */
        ZipContainer(final URL container) throws IOException, URISyntaxException {
            m_path = FileUtil.resolveToPath(container);
            m_zipOutputStream =
                new ZipOutputStream(m_path == null ? openConnection(container)
                    : Files.newOutputStream(m_path, StandardOpenOption.CREATE));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void close() throws IOException {
            m_zipOutputStream.close();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public OutputStream nextOutputStream(final String fileName) throws IOException {
            ZipEntry ret = new ZipEntry(fileName);
            m_zipOutputStream.putNextEntry(ret);
            return m_zipOutputStream;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void closeStream(final OutputStream stream) throws IOException {
            m_zipOutputStream.closeEntry();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void ifExistsDelete(final String fileName) {
            //Do nothing, it should not exists
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void deleteGenerated() throws IOException {
            if (m_path != null) {
                Files.deleteIfExists(m_path);
            }
        }
    }

    static class FolderContainer implements Container {
        private URL m_folder;

        private Path m_path, m_lastPath;

        private boolean m_overwrite;

        /**
         * @throws IOException When cannot create parent folders.
         * @throws URISyntaxException Wrong container.
         */
        FolderContainer(final URL container, final boolean overwrite) throws IOException, URISyntaxException {
            this.m_folder = container;
            m_path = FileUtil.resolveToPath(container);
            this.m_overwrite = overwrite;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public OutputStream nextOutputStream(final String fileName) throws IOException {
            if (m_path != null) {
                m_lastPath = m_path.resolve(fileName);
                if (!m_overwrite && Files.exists(m_lastPath)) {
                    throw new IOException("File '" + m_lastPath + "' already exists");
                }
                return Files.newOutputStream(m_lastPath, StandardOpenOption.CREATE);
            }
            return new BufferedOutputStream(openConnection(combine(m_folder, fileName)));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void close() {
            //Do nothing
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void closeStream(final OutputStream stream) throws IOException {
            stream.close();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void ifExistsDelete(final String fileName) throws IOException {
            if (m_path != null) {
                Path path = m_path.resolve(fileName);
                Files.deleteIfExists(path);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void deleteGenerated() throws IOException {
            if (m_lastPath != null) {
                Files.deleteIfExists(m_lastPath);
            }
        }

    }

    static class GzipperContainer implements Container {
        private Container m_container;

        /**
         * @param container
         *
         */
        GzipperContainer(final Container container) {
            this.m_container = container;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public OutputStream nextOutputStream(final String fileName) throws IOException {
            return new GZIPOutputStream(m_container.nextOutputStream(fileName));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void close() throws Exception {
            m_container.close();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void closeStream(final OutputStream stream) throws IOException {
            stream.close();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void ifExistsDelete(final String fileName) throws IOException {
            m_container.ifExistsDelete(fileName);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void deleteGenerated() throws IOException {
            m_container.deleteGenerated();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec) throws Exception {
        BufferedDataTable table = inData[0];
        DataTableSpec inputSpec = inData[0].getSpec();
        JSONCellWriterFactory factory = Activator.getInstance().getJsonCellWriterFactory();
        int index = inputSpec.findColumnIndex(m_settings.getInputColumn());
        List<URIContent> uriContents = new ArrayList<>();
        double max = table.getRowCount();
        int count = 0;
        int missingCellCount = 0;

        checkOutputLocation();
        URL container = FileUtil.toURL(m_settings.getOutputLocation());
        if (m_settings.isCompressContents() && m_settings.getCompressionMethod().supportsMultipleFiles()) {
            uriContents.add(new URIContent(container.toURI(), FilenameUtils.getExtension(container.toString())));
        }
        Path containerPath = FileUtil.resolveToPath(container);
        if (containerPath != null) {
            if (m_settings.isCompressContents() && m_settings.getCompressionMethod().supportsMultipleFiles()) {
                if (!m_settings.getOverwriteExistingFiles() && Files.exists(containerPath)) {
                    throw new IOException("File '" + containerPath + "' already exists");
                }
                Path parentPath = containerPath.getParent();
                createFolder(parentPath);
            } else {
                //According to noding guidelines we cannot create parent folders.
            }
        } else {
            //We have to use URLConnections, though cannot create folders.
        }

        try (Container abstractContainer = createContainer(container)) {
            for (DataRow row : table) {
                try {
                    exec.checkCanceled();
                } catch (CanceledExecutionException e) {
                    abstractContainer.deleteGenerated();
                    throw e;
                }
                exec.setProgress(count / max, "Writing " + row.getKey() + m_settings.getExtension());

                if (!m_settings.isCompressContents() || !m_settings.getCompressionMethod().supportsMultipleFiles()) {
                    if (containerPath != null) {
                        Path jsonFile = containerPath.resolve(row.getKey() + m_settings.getExtension());
                        if (!m_settings.getOverwriteExistingFiles() && Files.exists(jsonFile)) {
                            throw new IOException("File '" + jsonFile + "' already exists");
                        }
                        uriContents.add(new URIContent(jsonFile.toUri(), m_settings.getExtension()));
                    }
                }

                DataCell cell = row.getCell(index);
                if (!cell.isMissing()) {
                    OutputStream os = nextStream(abstractContainer, row);
                    try {
                        JSONCellWriter jsonCellWriter = factory.create(os);
                        jsonCellWriter.write((JSONValue)cell);
                        //Do not close the stream because of the Zip case
                    } finally {
                        abstractContainer.closeStream(os);
                    }
                } else {
                    missingCellCount++;
                    abstractContainer.ifExistsDelete(fileName(row));
                    LOGGER.debug("Skip row " + row.getKey().getString() + " since the cell is a missing data cell.");
                }
                count++;
            }
        }

        if (missingCellCount > 0) {
            setWarningMessage("Skipped " + missingCellCount + " rows due " + "to missing values.");
        }
        pushFlowVariableString(OUTPUT_LOCATION_FLOW_VAR_NAME, m_settings.getOutputLocation());
        return new BufferedDataTable[0];
    }

    /**
     * @param folder
     * @param fileName
     * @return
     * @throws MalformedURLException
     */
    private static URL combine(final URL folder, final String fileName) throws MalformedURLException {
        return new URL(folder, fileName);
    }

    private static final OutputStream openConnection(final URL url) throws IOException {
        return FileUtil.openOutputConnection(url, "PUT").getOutputStream();
    }

    /**
     * @param parentPath
     * @throws IOException
     */
    private void createFolder(final Path parentPath) throws IOException {
        if (!Files.exists(parentPath)) {
            Files.createDirectories(parentPath);
            LOGGER.info("Created directory for specified output file: " + parentPath);
        }
    }

    /**
     * @param row
     * @return
     */
    private String fileName(final DataRow row) {
        return row.getKey().toString() + m_settings.getExtension();
    }

    /**
     * @param container
     * @return
     * @throws IOException
     * @throws URISyntaxException Wrong container.
     */
    private Container createContainer(final URL container) throws IOException, URISyntaxException {
        switch (m_settings.getCompressionMethod()) {
            case NONE:
                return new FolderContainer(container, m_settings.getOverwriteExistingFiles());
            case GZIP:
                return new GzipperContainer(new FolderContainer(container, m_settings.getOverwriteExistingFiles()));
//            case ZIP:
//                return new ZipContainer(container);
            default:
                throw new UnsupportedOperationException("Not supported compression method: "
                    + m_settings.getCompressionMethod().getStringValue());
        }
    }

    /**
     * @param container
     * @param key
     * @return
     * @throws IOException Problem opening the {@link OutputStream}.
     */
    private OutputStream nextStream(final Container container, final DataRow row) throws IOException {
        String fileName = fileName(row);
        return container.nextOutputStream(fileName);
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
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        final DataTableSpec table = inSpecs[0];
        DataColumnSpec column = table.getColumnSpec(m_settings.getInputColumn());
        if (column == null) {
            column = handleNonSetColumn(table);
        }
        checkOutputLocation();
        if (column.getType().isCompatible(JSONValue.class)) {
            return new DataTableSpec[0];
        }
        throw new InvalidSettingsException("The selected column (" + m_settings.getInputColumn()
            + ") is not a JSON column.");
    }

    /**
     * @throws InvalidSettingsException
     */
    private void checkOutputLocation() throws InvalidSettingsException {
        if (m_settings.getCompressionMethod().supportsMultipleFiles()) {
            CheckUtils.checkDestinationFile(m_settings.getOutputLocation(), m_settings.getOverwriteExistingFiles());
        } else {
            CheckUtils.checkDestinationDirectory(m_settings.getOutputLocation());
        }
    }

    /**
     * @param table
     * @return
     * @throws InvalidSettingsException
     */
    private DataColumnSpec handleNonSetColumn(final DataTableSpec table) throws InvalidSettingsException {
        List<String> compatibleCols = new ArrayList<>();
        for (DataColumnSpec c : table) {
            if (c.getType().isCompatible(JSONValue.class)) {
                compatibleCols.add(c.getName());
            }
        }
        if (compatibleCols.size() == 1) {
            // auto-configure
            m_settings.setInputColumn(compatibleCols.get(0));
            return table.getColumnSpec(compatibleCols.get(0));
        } else if (compatibleCols.size() > 1) {
            // auto-guessing
            m_settings.setInputColumn(compatibleCols.get(0));
            setWarningMessage("Auto guessing: using column \"" + compatibleCols.get(0) + "\".");
            return table.getColumnSpec(compatibleCols.get(0));
        } else {
            throw new InvalidSettingsException(
                ReplaceOrAddColumnSettings.NO_JSON_COLUMNS_USE_FOR_EXAMPLE_THE_STRING_TO_JSON_NODE_TO_CREATE_ONE);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_settings.loadSettingsModel(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        new JSONWriterNodeSettings().loadSettingsModel(settings);
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
        m_settings.setOutputLocation(url.toString());
    }
}
