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
 *   Apr 7, 2021 (Moditha): created
 */
package org.knime.json.node.filehandling.reader;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Optional;
import java.util.OptionalLong;

import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.jsfr.json.JsonSurfer;
import org.jsfr.json.JsonSurferJackson;
import org.jsfr.json.compiler.JsonPathCompiler;
import org.jsfr.json.path.JsonPath;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.json.JSONCellFactory;
import org.knime.core.node.NodeLogger;
import org.knime.filehandling.core.node.table.reader.config.TableReadConfig;
import org.knime.filehandling.core.node.table.reader.randomaccess.RandomAccessible;
import org.knime.filehandling.core.node.table.reader.randomaccess.RandomAccessibleUtils;
import org.knime.filehandling.core.node.table.reader.read.Read;
import org.knime.filehandling.core.util.BomEncodingUtils;
import org.knime.filehandling.core.util.CompressionAwareCountingInputStream;

/**
 * Class for the JSON reader which implements {@link Read}.
 *
 * @author Moditha Hewasinghage, KNIME GmbH, Berlin, Germany
 */
public class JSONRead implements Read<Path, DataValue> {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(JSONRead.class);

    private final Path m_path;

    private final BufferedReader m_reader;

    private final CompressionAwareCountingInputStream m_compressionAwareStream;

    private final long m_size;

    private final TableReadConfig<JSONReaderConfig> m_config;

    private final JSONReaderConfig m_jsonReaderConfig;

    private final boolean m_allowComments;

    private final long m_maxRows;

    private long m_linesRead;

    private final boolean m_useJSONPath;

    private final String m_JSONPath;

    private final JsonSurfer m_surfer = JsonSurferJackson.INSTANCE;

    private Iterator<Object> m_iterator;

    private final boolean m_failIfNotFound;

    /**
     * Constructor.
     *
     * @param path the {@link Path} to the file
     * @param config the {@link TableReadConfig} of the node
     * @throws IOException
     */
    JSONRead(final Path path, final TableReadConfig<JSONReaderConfig> config) throws IOException {
        m_config = config;
        m_jsonReaderConfig = m_config.getReaderSpecificConfig();

        m_path = path;
        m_size = Files.size(m_path);

        m_compressionAwareStream = new CompressionAwareCountingInputStream(path);

        final String charSetName = null; // TODO: maybe needed in future config.getReaderSpecificConfig().getCharSetName();
        final Charset charset = charSetName == null ? Charset.forName("UTF-8") : Charset.forName(charSetName);
        m_reader = BomEncodingUtils.createBufferedReader(m_compressionAwareStream, charset);
        m_allowComments = m_jsonReaderConfig.allowComments();
        m_useJSONPath = m_jsonReaderConfig.useJSONPath();
        m_JSONPath = m_jsonReaderConfig.getJSONPath();
        m_failIfNotFound = m_jsonReaderConfig.failIfNotFound();
        m_linesRead = 0;
        m_maxRows = m_config.getMaxRows();
    }

    @Override
    public RandomAccessible<DataValue> next() throws IOException {
        m_linesRead++;
        if (m_useJSONPath) {
            return readWithJSONPath();
        } else {
            return readFileToJSONCell();
        }
    }

    /**
     * @return
     * @throws IOException
     */
    private RandomAccessible<DataValue> readWithJSONPath() throws IOException {
        if (m_linesRead == 1) {
            try {
                JsonPath p = JsonPathCompiler.compile(m_JSONPath);
                m_iterator = m_surfer.iterator(m_compressionAwareStream, JsonPathCompiler.compile(m_JSONPath));
                //                  m_iterator = m_surfer.iterator(m_reader, JsonPathCompiler.compile(m_JSONPath));
            }
            // Invalid JSON Path
            catch (ParseCancellationException e) {
                return handleJSONPathError(e);
            }
            // Nothing in JSON Path
            if (m_iterator == null || !m_iterator.hasNext()) {
                return handleJSONPathError(null);
            }
        }
        if (m_iterator != null && m_iterator.hasNext()) {
            return createRandomAccessible(JSONCellFactory.create(m_iterator.next().toString()));
        } else {
            return null;
        }
    }

    /**
     * Reads entire JSON File as a single cell
     *
     * @return a {@link RandomAccessible}
     * @throws IOException
     */
    private RandomAccessible<DataValue> readFileToJSONCell() throws IOException {
        if (m_linesRead > 1) {
            return null;
        } else {
            return createRandomAccessible(JSONCellFactory.create(m_reader, m_allowComments));
        }
    }

    /**
     *
     * @param e RuntimeException in parsing
     * @return a {@link RandomAccessible}
     * @throws {@link RuntimeException} on invalid JSON Path, {@link NullPointerException} on nothing found for the JSON
     *             Path if {@link #m_failIfNotFound} is set
     */
    private RandomAccessible<DataValue> handleJSONPathError(final RuntimeException e) {
        if (m_failIfNotFound) {
            if (e != null) {
                throw new RuntimeException("Invalid JSON Path " + m_JSONPath, e);
            } else {
                throw new NullPointerException("Nothing found for JSON Path " + m_JSONPath);
            }
        } else {
            return createRandomAccessible(DataType.getMissingCell());
        }
    }

    /**
     * Creates a {@link RandomAccessible} with a row id and a line.
     *
     * @param line the content of a line
     * @return a {@link RandomAccessible}
     */
    private static RandomAccessible<DataValue> createRandomAccessible(final DataValue line) {
        return RandomAccessibleUtils.createFromArray(line);
    }

    @Override
    public OptionalLong getMaxProgress() {
        return OptionalLong.of(m_size);
    }

    @Override
    public long getProgress() {
        return m_compressionAwareStream.getCount();
    }

    @Override
    public Optional<Path> getItem() {
        return Optional.of(m_path);
    }

    @Override
    public void close() throws IOException {
        try {
            m_reader.close();
        } catch (IOException e) {
            LOGGER.error("Something went wrong while closing the BufferedReader. "
                + "For further details please have a look into the log.", e);
        }
        m_compressionAwareStream.close();
    }

}
