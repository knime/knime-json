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
 *   21.04.2021 (loescher): created
 */
package org.knime.json.node.container.input.file;

import java.util.EnumSet;
import java.util.Objects;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.FSFiles;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.defaultnodesettings.EnumConfig;
import org.knime.filehandling.core.defaultnodesettings.filechooser.FileChooserPathAccessor;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.SettingsModelReaderFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filesystemchooser.FixedPortsConfiguration;
import org.knime.filehandling.core.defaultnodesettings.filtermode.SettingsModelFilterMode.FilterMode;
import org.knime.filehandling.core.defaultnodesettings.status.PriorityStatusConsumer;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage.MessageType;

/**
 * Contains the “Container Input (File)” node specific settings
 *
 * @author Jannik Löscher, KNIME GmbH, Konstanz, Germany
 */
final class NodeConfiguration {

    static final String CFG_DEFAULT_FILE_KEY = "defaultFile";

    static final String CFG_USE_DEFAULT_FILE_KEY = "useDefaultFile";

    static final String CFG_OUT_VAR_NAME_KEY = "outputVariableName";

    static final String CFG_WRITE_WS_KEY = "saveInWorkflow";

    static final String CFG_OUT_VAR_NAME_DEFAULT = "location";

    private boolean m_useDefaultFile;

    private boolean m_writeInWorkflow;

    private String m_outVarName;

    private SettingsModelReaderFileChooser m_fileChooserSettingsModel;

    /**
     * Constructs the “Container Input (File)” node specific settings
     */
    NodeConfiguration() {
        m_fileChooserSettingsModel = new SettingsModelReaderFileChooser(NodeConfiguration.CFG_DEFAULT_FILE_KEY,
            new FixedPortsConfiguration.FixedPortsConfigurationBuilder().build(), "",
            EnumConfig.create(FilterMode.FILE), EnumSet.complementOf(EnumSet.of(FSCategory.CONNECTED)));
        reset();
    }

    /**
     * Resets the internal values to their default. This does not clean the files got by {@link #getExternalURI()} or
     * {@link #getLocalLocation()}.
     */
    void reset() {
        m_useDefaultFile = false;
        m_writeInWorkflow = true;
        m_outVarName = CFG_OUT_VAR_NAME_DEFAULT;

        m_fileChooserSettingsModel.setEnabled(false);
        m_fileChooserSettingsModel.setLocation(new FSLocation(FSCategory.LOCAL, ""));
    }

    /**
     * @return the settings model used to store the default file
     */
    SettingsModelReaderFileChooser getFileChooserSettingsModel() {
        return m_fileChooserSettingsModel;
    }

    /**
     * @return the default file
     */
    FSLocation getDefaultFile() {
        return m_fileChooserSettingsModel.getLocation();
    }

    /**
     * @return {@link #isUsingDefaultFile()}. If it is used, checks whether it is accessible, otherwise an exception is
     *         thrown
     * @throws InvalidSettingsException if the default file could not be accessed
     */
    boolean hasDefaultFile() throws InvalidSettingsException {
        if (!m_useDefaultFile) {
            return false;
        }

        try (final var accessor = m_fileChooserSettingsModel.createReadPathAccessor()) {
            final var consumer = new PriorityStatusConsumer();
            final var file = ((FileChooserPathAccessor)accessor).getOutputPath(consumer); // only this methods checks the sanity of the chosen path
            final var statusMessage = consumer.get();
            if (statusMessage.isPresent() && statusMessage.get().getType() == MessageType.ERROR) {
                throw new InvalidSettingsException(statusMessage.get().getMessage());
            }
            if (!FSFiles.exists(file)) {
                throw new InvalidSettingsException("File does not exist");
            }
        } catch (InvalidSettingsException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidSettingsException("Could not access file", e);
        }
        return true;
    }

    /**
     * @return the default file should be used (if it is present)
     */
    boolean isUsingDefaultFile() {
        return m_useDefaultFile;
    }

    /**
     * @return whether the file is written in the workflow data area
     */
    boolean isWritingInWorkflow() {
        return m_writeInWorkflow;
    }

    /**
     * @return the name of the variable that will be send to the output.
     */
    String getOutputVariableName() {
        return m_outVarName;
    }

    /**
     * @param defaultFile the default file to set or {@link FSLocation#NULL} if it should be removed
     */
    void setDefaultFile(final FSLocation defaultFile) {
        m_fileChooserSettingsModel.setLocation(Objects.requireNonNull(defaultFile));
    }

    /**
     * @param useDefaultFile whether the default file should be used (if it is set)
     */
    void setUseDefaultFile(final boolean useDefaultFile) {
        m_useDefaultFile = useDefaultFile;
    }

    /**
     * @param writeInWorkflow whether the file should be saved in the workspace
     */
    void setWriteInWorkflow(final boolean writeInWorkflow) {
        m_writeInWorkflow = writeInWorkflow;
    }

    /**
     * @param outputVariableName the name to be set. It must not be blank, i.e. contain of only white space
     */
    void setOutputVarianleName(final String outputVariableName) {
        if (Objects.requireNonNull(outputVariableName).isBlank()) {
            throw new IllegalArgumentException("The output variable name is empty");
        }
        m_outVarName = outputVariableName;
    }

    /**
     * Saves the settings of this configuration to the given settings object.
     *
     * @param settings the settings to save to.
     */
    void saveSettingsTo(final NodeSettingsWO settings) {
        m_fileChooserSettingsModel.saveSettingsTo(settings);
        settings.addBoolean(CFG_USE_DEFAULT_FILE_KEY, m_useDefaultFile);
        settings.addString(CFG_OUT_VAR_NAME_KEY, m_outVarName);
        settings.addBoolean(CFG_WRITE_WS_KEY, m_writeInWorkflow);
    }

    /**
     * Validates the given settings. Please refer to the given setters.
     *
     * @param settings the settings to be validated.
     * @throws InvalidSettingsException if the settings were invalid
     */
    void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        settings.getBoolean(CFG_USE_DEFAULT_FILE_KEY);
        if (settings.getString(CFG_OUT_VAR_NAME_KEY).isBlank()) {
            throw new InvalidSettingsException("Please make sure the output variale name is not blank");
        }
        m_fileChooserSettingsModel.validateSettings(settings);
        settings.getBoolean(CFG_WRITE_WS_KEY);
    }

    /**
     * Loads the validated settings from the given settings object.
     *
     * @param settings the settings from which to load.
     * @throws InvalidSettingsException if the needed settings are not present
     */
    void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_useDefaultFile = settings.getBoolean(CFG_USE_DEFAULT_FILE_KEY);
        m_writeInWorkflow = settings.getBoolean(CFG_WRITE_WS_KEY);
        m_outVarName = settings.getString(CFG_OUT_VAR_NAME_KEY);
        m_fileChooserSettingsModel.loadSettingsFrom(settings);
        m_fileChooserSettingsModel.setEnabled(m_useDefaultFile);
    }

}
