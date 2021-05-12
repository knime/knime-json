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

import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;

import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.jsfr.json.JsonSurfer;
import org.jsfr.json.JsonSurferJackson;
import org.jsfr.json.compiler.JsonPathCompiler;
import org.jsfr.json.path.JsonPath;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.json.JSONCellFactory;
import org.knime.filehandling.core.node.table.reader.config.TableReadConfig;
import org.knime.filehandling.core.node.table.reader.randomaccess.RandomAccessible;
import org.knime.filehandling.core.node.table.reader.read.Read;

/**
 * Class for the JSON reader which implements {@link Read}.
 *
 * @author Moditha Hewasinghage, KNIME GmbH, Berlin, Germany
 */
public class JSONPathRead extends JSONRead implements Read<Path, DataValue> {

    private final String m_jsonPath;

    private static final JsonSurfer m_surfer = JsonSurferJackson.INSTANCE;

    private Iterator<Object> m_iterator;

    private final boolean m_failIfNotFound;

    /**
     * Constructor.
     *
     * @param path the {@link Path} to the file
     * @param config the {@link TableReadConfig} of the node
     * @throws IOException
     */
    JSONPathRead(final Path path, final TableReadConfig<JSONReaderConfig> config) throws IOException {
        super(path, config);

        m_jsonPath = m_jsonReaderConfig.getJSONPath();
        m_failIfNotFound = m_jsonReaderConfig.failIfNotFound();
        m_linesRead = 0;
    }

    @Override
    public RandomAccessible<DataValue> next() throws IOException {
        m_linesRead++;
        RandomAccessible<DataValue> dataValue = null;
        if (m_linesRead == 1) {
            try {
                final JsonPath p = JsonPathCompiler.compile(m_jsonPath);
                m_iterator = m_surfer.iterator(m_compressionAwareStream, p);
            } catch (ParseCancellationException e) { // Invalid JSON Path
                dataValue = handleJSONPathError(e);
            }
            // Nothing in JSON Path
            if (!m_iterator.hasNext()) {
                dataValue = handleJSONPathError(null);
            }
        }
        if (m_iterator.hasNext()) {
            dataValue = createRandomAccessible(JSONCellFactory.create(m_iterator.next().toString()));
        }
        return dataValue;
    }

    /**
     *
     * @param e RuntimeException in parsing
     * @return a {@link RandomAccessible}
     * @throws IOException
     * @throws {@link RuntimeException} on invalid JSON Path, {@link NullPointerException} on nothing found for the JSON
     *             Path if {@link #m_failIfNotFound} is set
     */
    private RandomAccessible<DataValue> handleJSONPathError(final RuntimeException e) throws IOException {
        if (m_failIfNotFound) {
            if (e != null) {
                throw new IOException("Invalid JSON Path " + m_jsonPath, e);
            } else {
                throw new IOException("Nothing found for JSON Path " + m_jsonPath);
            }
        } else {
            return createRandomAccessible(DataType.getMissingCell());
        }
    }

    @Override
    public void close() throws IOException {
        m_compressionAwareStream.close();
    }

}
