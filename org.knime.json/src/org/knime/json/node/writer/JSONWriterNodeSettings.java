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
 *   25 Sept. 2014 (Gabor): created
 */
package org.knime.json.node.writer;

import java.net.URL;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.StringValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * Node settings for the JSON writer node. Based on {@link org.knime.xml.node.writer.XMLWriterNodeSettings} from Heiko
 * Hofer.
 *
 * @author Gabor Bakos
 */
class JSONWriterNodeSettings {
    /**
     *
     */
    private static final String JSON = "JSON";

    /**
     * Support for multiple files interface.
     */
    static interface SupportsMultipleFiles {
        /**
         * @return {@code true}, iff multiple files can be saved using that compression method.
         */
        boolean supportsMultipleFiles();
    }

    static enum CompressionMethods implements StringValue, SupportsMultipleFiles {
        NONE {
            /**
             * {@inheritDoc}
             */
            @Override
            public String getStringValue() {
                return "<none>";
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public boolean supportsMultipleFiles() {
                return false;
            }
        },
        GZIP {
            /**
             * {@inheritDoc}
             */
            @Override
            public String getStringValue() {
                return "gzip";
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public boolean supportsMultipleFiles() {
                return false;
            }
        };
    }

    /** Config key for the input column. */
    private static final String INPUT_COLUMN = "inputColumn";

    /** Config key for the output file (can be overrided with the URI port object). */
    static final String OUTPUT_LOCATION = "outputLocation";

    /** Config key for: Overwrite existing files? */
    private static final String OVERWRITE_EXISTING = "overwriteExistingFiles";

    /** Config key for the file name extension. */
    private static final String EXTENSION = "extension";

    /** Config key for whether to compress the contents. */
    private static final String COMPRESS_CONTENTS = "compressContents";

    /** Config key for the compression method. @see CompressionMethods */
    private static final String COMPRESSION_METHOD = "compressionMethod";

    /** The JSON serialization format to use. @see JSONFormats */
    private static final String FORMAT = "format";

    private String m_inputColumn = null;

    private String m_outputLocation = null;

    private boolean m_overwriteExistingFiles = false;

    private String m_extension = ".json";

    private boolean m_compressContents = false;

    private CompressionMethods m_compressionMethod = CompressionMethods.NONE;

    private String m_format = JSON;

    /**
     * @return the inputColumn
     */
    String getInputColumn() {
        return m_inputColumn;
    }

    /**
     * @param inputColumn the inputColumn to set
     */
    void setInputColumn(final String inputColumn) {
        m_inputColumn = inputColumn;
    }

    /**
     * @return the output location (either folder, or a file when {@link SupportsMultipleFiles#supportsMultipleFiles()}
     *         {@code ==true}) as a {@link URL}'s {@link String} format.
     */
    String getOutputLocation() {
        return m_outputLocation;
    }

    /**
     * @param location the folder or file to set
     */
    void setOutputLocation(final String location) {
        m_outputLocation = location;
    }

    /**
     * @return the overwriteExistingFiles
     */
    boolean getOverwriteExistingFiles() {
        return m_overwriteExistingFiles;
    }

    /**
     * @param overwriteExistingFiles the overwriteExistingFiles to set
     */
    void setOverwriteExisting(final boolean overwriteExistingFiles) {
        m_overwriteExistingFiles = overwriteExistingFiles;
    }

    /**
     * @return the extension
     */
    final String getExtension() {
        return m_extension;
    }

    /**
     * @param extension the extension to set
     */
    final void setExtension(final String extension) {
        this.m_extension = extension;
    }

    /**
     * @return the compressContents
     */
    final boolean isCompressContents() {
        return m_compressContents;
    }

    /**
     * @param compressContents the compressContents to set
     */
    final void setCompressContents(final boolean compressContents) {
        this.m_compressContents = compressContents;
    }

    /**
     * @return the compressionMethod
     */
    final CompressionMethods getCompressionMethod() {
        return m_compressionMethod;
    }

    /**
     * @param compressionMethod the compressionMethod to set
     */
    final void setCompressionMethod(final CompressionMethods compressionMethod) {
        this.m_compressionMethod = compressionMethod;
    }

    /**
     * @return the format
     */
    final String getFormat() {
        return m_format;
    }

    /**
     * @param format the format to set
     */
    final void setFormat(final String format) {
        this.m_format = format;
    }

    /**
     * Called from dialog when settings are to be loaded.
     *
     * @param settings To load from
     * @param inSpec Input spec
     */
    void loadSettingsDialog(final NodeSettingsRO settings, final DataTableSpec inSpec) {
        m_inputColumn = settings.getString(INPUT_COLUMN, null);
        m_outputLocation = settings.getString(OUTPUT_LOCATION, null);
        m_overwriteExistingFiles = settings.getBoolean(OVERWRITE_EXISTING, false);
        m_extension = settings.getString(EXTENSION, ".json");
        m_compressContents = settings.getBoolean(COMPRESS_CONTENTS, false);
        //TODO ask preferred way of storing enums.
        m_compressionMethod = CompressionMethods.values()[settings.getInt(COMPRESSION_METHOD, 0)];
        m_format = settings.getString(FORMAT, JSON);
    }

    /**
     * Called from model when settings are to be loaded.
     *
     * @param settings To load from
     * @throws InvalidSettingsException If settings are invalid.
     */
    void loadSettingsModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_inputColumn = settings.getString(INPUT_COLUMN);
        m_outputLocation = settings.getString(OUTPUT_LOCATION);
        m_overwriteExistingFiles = settings.getBoolean(OVERWRITE_EXISTING);
        m_extension = settings.getString(EXTENSION);
        m_compressContents = settings.getBoolean(COMPRESS_CONTENTS);
        m_compressionMethod = CompressionMethods.values()[settings.getInt(COMPRESSION_METHOD)];
        m_format = settings.getString(FORMAT, JSON);
        if (m_compressContents && m_compressionMethod == CompressionMethods.NONE) {
            throw new InvalidSettingsException("Compression was selected, though compression method is <none>!");
        }
    }

    /**
     * Called from model and dialog to save current settings.
     *
     * @param settings To save to.
     */
    void saveSettings(final NodeSettingsWO settings) {
        settings.addString(INPUT_COLUMN, m_inputColumn);
        settings.addString(OUTPUT_LOCATION, m_outputLocation);
        settings.addBoolean(OVERWRITE_EXISTING, m_overwriteExistingFiles);
        settings.addString(EXTENSION, m_extension);
        settings.addBoolean(COMPRESS_CONTENTS, m_compressContents);
        settings.addInt(COMPRESSION_METHOD, m_compressionMethod.ordinal());
        settings.addString(FORMAT, m_format);
    }
}
