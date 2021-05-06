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
 *   20.04.2021 (loescher): created
 */
package org.knime.json.node.container.input.file;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.EnumSet;
import java.util.Optional;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.dialog.ExternalNodeData;
import org.knime.core.node.dialog.InputNode;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.flowvariable.FlowVariablePortObject;
import org.knime.core.node.port.flowvariable.FlowVariablePortObjectSpec;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.util.ThreadUtils.ThreadWithContext;
import org.knime.core.util.pathresolve.ResolverUtil;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.FSFiles;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.connections.RelativeTo;
import org.knime.filehandling.core.connections.location.FSPathProviderFactory;
import org.knime.filehandling.core.data.location.variable.FSLocationVariableType;
import org.knime.filehandling.core.defaultnodesettings.status.NodeModelStatusConsumer;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage;

/**
 * The model for the “Container Input (File)” node.
 *
 * @author Jannik Löscher, KNIME GmbH, Konstanz, Germany
 */
final class ContainerFileInputNodeModel extends NodeModel implements InputNode {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(ContainerFileInputNodeModel.class);

    private static final String INT_FILE_NAME = "internals.xml";

    private static final String INT_SETTINGS_NAME = "containerFileInputNodeInternals";

    private static final String INT_CFG_EXTERNAL_LOCATION_KEY = "externalLocation";

    static final String CFG_PARAMETER_DEFAULT = "input-file";

    private static final String URL_THIS_WORKFLOW_DATA = RelativeTo.WORKFLOW_DATA.getSettingsValue();

    private static final String TEMP_FILE_PREFIX = "knimetemp-";

    private final ContainerNodeSharedConfiguration m_sharedConfig;

    private final NodeConfiguration m_config;

    private Optional<FSLocation> m_externalLocation;

    private Optional<URI> m_externalURI;

    /**
     * Creates a new {@link ContainerFileInputNodeModel} with no input ports and one flow variable output port.
     */
    ContainerFileInputNodeModel() {
        super(new PortType[0], new PortType[]{FlowVariablePortObject.TYPE});
        m_sharedConfig = new ContainerNodeSharedConfiguration(CFG_PARAMETER_DEFAULT);
        m_config = new NodeConfiguration();

        m_externalLocation = Optional.empty();
        m_externalURI = Optional.empty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        if (m_externalURI.isPresent()) {
            return new PortObjectSpec[]{FlowVariablePortObjectSpec.INSTANCE};
        }

        if (m_config.isUsingDefaultFile()) {
            try {
                setWarningMessage(null); // clear previous messages
                final var consumer = new NodeModelStatusConsumer(EnumSet.allOf(StatusMessage.MessageType.class));
                m_config.getFileChooserSettingsModel().configureInModel(inSpecs, consumer);
                consumer.setWarningsIfRequired(this::setWarningMessage);
                m_config.hasDefaultFile();
                return new PortObjectSpec[]{FlowVariablePortObjectSpec.INSTANCE};
            } catch (InvalidSettingsException e) {
                throw new InvalidSettingsException("Invalid default file: " + e.getMessage(), e);
            }
        }

        throw new InvalidSettingsException("No file to work with");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        m_externalLocation.ifPresent(Deleter::runNew); // this should never be executed
        FSLocation location = resolveFile();
        pushFlowVariable(m_config.getOutputVariableName(), FSLocationVariableType.INSTANCE, location);
        return new PortObject[]{FlowVariablePortObject.INSTANCE};
    }

    @Override
    public ExternalNodeData getInputData() {
        return ExternalNodeData.builder(m_sharedConfig.getParameter()).description(m_sharedConfig.getDescription())
            // an arbitrary resource is enough here
            .resource(Paths.get("/a", "file").toUri()).build();
    }

    @Override
    public void validateInputData(final ExternalNodeData inputData) throws InvalidSettingsException {
        if (inputData.getResource() == null) {
            throw new InvalidSettingsException("Expected a resource.");
        }
    }

    @Override
    public void setInputData(final ExternalNodeData inputData) throws InvalidSettingsException {
        if (inputData.getResource() == null) {
            throw new InvalidSettingsException("Expected a resource.");
        }
        m_externalURI = Optional.of(inputData.getResource());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isUseAlwaysFullyQualifiedParameterName() {
        return m_sharedConfig.hasFullyQualifiedName();
    }

    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        final var internalsFile = nodeInternDir.toPath().resolve(INT_FILE_NAME);

        if (Files.exists(internalsFile)) {
            try (final var internalsStream = Files.newInputStream(internalsFile)) {
                final NodeSettingsRO settings = NodeSettings.loadFromXML(internalsStream);

                if (!settings.containsKey(INT_CFG_EXTERNAL_LOCATION_KEY)) {
                    return;
                }

                final var locationStrings = settings.getStringArray(INT_CFG_EXTERNAL_LOCATION_KEY);
                CheckUtils.checkSetting(locationStrings.length == 3,
                    "Expected three elements to represent a the file location.");

                final var location = new FSLocation(locationStrings[0], locationStrings[1], locationStrings[2]);

                if (!checkExists(location)) {
                    setWarningMessage("The external file has been deleted. Please re-execute the node.");
                }

                m_externalLocation = Optional.of(location);

            } catch (InvalidSettingsException e) {
                throw new IOException("Could not load internal settings: " + e.getMessage(), e);
            }
        }
    }

    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        if (m_externalLocation.isEmpty()) {
            return;
        }

        final var location = m_externalLocation.get();

        final var settings = new NodeSettings(INT_SETTINGS_NAME);
        settings.addStringArray(INT_CFG_EXTERNAL_LOCATION_KEY, location.getFileSystemCategory(),
            location.getFileSystemSpecifier().orElse(null), location.getPath());

        final var internalsFile = nodeInternDir.toPath().resolve(INT_FILE_NAME);

        try (final var internalsStream = Files.newOutputStream(internalsFile)) {
            settings.saveToXML(internalsStream);
        }
    }

    private static boolean checkExists(final FSLocation location) throws IOException {
        try (final var factory = FSPathProviderFactory.newFactory(Optional.empty(), location);
                final var provider = factory.create(location)) {
            final var path = provider.getPath();
            return FSFiles.exists(path);
        } catch (AccessDeniedException e) {
            throw new IOException("Could not read file: " + e.getMessage(), e);
        }
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_sharedConfig.saveSettingsTo(settings);
        m_config.saveSettingsTo(settings);

    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        ContainerNodeSharedConfiguration.validateSettings(settings);
        m_config.validateSettings(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_sharedConfig.loadValidatedSettingsFrom(settings);
        m_config.loadValidatedSettingsFrom(settings);
    }

    @Override
    protected void reset() {
        m_externalLocation.ifPresent(Deleter::runNew);
        m_externalLocation = Optional.empty();
        // we do not reset the external location here because this method is directly called after the input is set
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onDispose() {
        m_externalLocation.ifPresent(Deleter::runNew);
        m_externalLocation = Optional.empty();
        m_externalURI = Optional.empty();
        super.onDispose();
    }

    private FSLocation resolveFile() throws InvalidSettingsException {
        FSLocation location;
        if (m_externalURI.isPresent()) {
            location = resolveUriToTempLocation(m_externalURI.get()); // NOSONAR: check in line above
            // we do not want that future executions without an external resource still think that
            // there is some resource present (in that case we want to offer the default file again)
            m_externalURI = Optional.empty();
            m_externalLocation = Optional.of(location);
        } else if (m_config.hasDefaultFile()) {
            location = m_config.getDefaultFile();
        } else {
            throw new InvalidSettingsException("No file to process");
        }
        return location;
    }

    private static FSLocation resolveUriToTempLocation(final URI uri) throws InvalidSettingsException {
        try {
            final var currentLocation = ResolverUtil.resolveURItoLocalOrTempFile(uri).toPath();
            boolean isTemporaryAlready = !currentLocation.toUri().equals(uri); // We assume that the file is temporary if the URI is different
            return moveOrCopyFileToTempLocation(currentLocation, isTemporaryAlready);
        } catch (IOException e) {
            throw new InvalidSettingsException("Could not resolve external file: " + e.getMessage(), e);
        }
    }

    private static FSLocation moveOrCopyFileToTempLocation(final Path currentLocation, final boolean isTemporaryAlready)
        throws InvalidSettingsException {
        final var tempDir = getTempLocation();

        try (final var factory = FSPathProviderFactory.newFactory(Optional.empty(), tempDir);
                final var provider = factory.create(tempDir)) {

            final var targetLocation = provider.getPath().resolve(currentLocation.getFileName().toString());
            // if the file is in the local file system, we'll copy it (to be sure)
            if (isTemporaryAlready) {
                tryMove(currentLocation, targetLocation);
            } else {
                Files.copy(currentLocation, targetLocation);
            }
            return new FSLocation(tempDir.getFileSystemCategory(), tempDir.getFileSystemSpecifier().orElse(null),
                Paths.get(tempDir.getPath(), currentLocation.getFileName().toString()).toString());
        } catch (IOException e) {
            throw new InvalidSettingsException("Could not copy or move file:" + e.getMessage(), e);
        }

    }

    private static final void tryMove(final Path source, final Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            LOGGER.warn("Could not move file, trying to copy instead", e);
            // we could not move the file (probably because of permissions)
            // try to copy it as a last resort
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static FSLocation getTempLocation() throws InvalidSettingsException {

        final String reativeType = URL_THIS_WORKFLOW_DATA;
        final String relativeLocation = "";

        final var entrypoint = new FSLocation(FSCategory.RELATIVE.toString(), reativeType, relativeLocation);

        try (final var factory = FSPathProviderFactory.newFactory(Optional.empty(), entrypoint);
                final var provider = factory.create(entrypoint)) {

            // find dir and create it if necessary
            final var dirPath = provider.getPath();
            final var tempDirPath = FSFiles.createRandomizedDirectory(dirPath, TEMP_FILE_PREFIX, "");
            return new FSLocation(entrypoint.getFileSystemCategory(), reativeType,
                Paths.get(entrypoint.getPath(), tempDirPath.getFileName().toString()).toString());

        } catch (IOException e) {
            throw new InvalidSettingsException("Could not create temp file: " + e.getMessage(), e);
        }
    }

    private static final class Deleter extends ThreadWithContext {
        private final FSLocation m_toDelete;

        private Deleter(final FSLocation toDelete) {
            m_toDelete = toDelete;
        }

        @Override
        protected void runWithContext() {
            deleteLocalFile(m_toDelete);
        }

        private static void deleteLocalFile(final FSLocation location) {
            try (final var factory = FSPathProviderFactory.newFactory(Optional.empty(), location);
                    final var providerFile = factory.create(location)) {

                final var tempfile = providerFile.getPath();
                final var tempdir = tempfile.getParent();
                FSFiles.deleteSafely(tempfile);
                if (tempdir != null && Files.exists(tempdir) && tempdir.getFileName().toString().startsWith(TEMP_FILE_PREFIX)) {
                    deleteDirectoryIfNotEmpty(tempdir);
                }
            } catch (IOException e) {
                throw new IllegalStateException("Could not clean up local file!", e);
            }
        }

        private static void deleteDirectoryIfNotEmpty(final Path tempdir) throws IOException {
            try (final var list = Files.newDirectoryStream(tempdir)) {
                if (!list.iterator().hasNext()) {
                    Files.delete(tempdir);
                }
            }
        }

        static void runNew(final FSLocation location) {
            new Deleter(location).start();
        }
    }
}
