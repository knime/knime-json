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
 *   14 Sept. 2014 (Gabor): created
 */
package org.knime.json.node.reader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import org.apache.commons.io.FilenameUtils;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.MissingCell;
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
import org.knime.core.node.util.CheckUtils;
import org.knime.core.util.FileUtil;
import org.knime.json.internal.Activator;
import org.knime.json.node.jsonpath.JsonPathUtil;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.github.fge.jackson.JacksonUtils;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ReadContext;

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
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws Exception {
        final BufferedDataContainer container = exec.createDataContainer(configure((PortObjectSpec[])null)[0]);
        long rowId = 0;
        final URL url = FileUtil.toURL(m_settings.getLocation());
        try {
            final File file = FileUtil.getFileFromURL(url);
            CheckUtils.checkArgument(file != null, "Probably on server.");
            @SuppressWarnings("null")
            final URI uri = file.toURI();
            rowId = readUriContent(container, rowId,
                new URIContent(uri, FilenameUtils.getExtension(file.getName())));
        } catch (final IllegalArgumentException e) {
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
    private long readUriContent(final BufferedDataContainer container, long rowId, final URIContent content)
        throws IOException, MalformedURLException {
        JsonPath jsonPath;
        try {
            jsonPath = JsonPath.compile(m_settings.isSelectPart() ? m_settings.getJsonPath() : "$");
        } catch (RuntimeException e) {
            throw new IllegalStateException("The path has invalid syntax: " + m_settings.getJsonPath());
        }
        JacksonConversions jacksonConversions = JacksonConversions.getInstance();
        try (InputStream is = content.getURI().toURL().openStream()) {
            //do {
            JSONValue jsonValue = (JSONValue)JSONCellFactory.create(is, m_settings.isAllowComments());
            DataCell value;
            if (m_settings.isSelectPart()) {
                Configuration config = Activator.getInstance().getJsonPathConfiguration();
                ReadContext context = JsonPath.parse(jsonValue.getJsonValue().toString(), config);
                try {
                    Object read = context.read(jsonPath);
                    if (read == null) {
                        value = handleNotFound(null, content.getURI());
                    } else {
                        JsonNode jackson = JsonPathUtil.toJackson(
                            JacksonUtils.nodeFactory(), read);
                        if (jackson.isArray() && !jsonPath.isDefinite()) {
                            for (final JsonNode node: (ArrayNode)jackson) {
                                container.addRowToTable(new DefaultRow(RowKey.createRowKey(rowId++), JSONCellFactory.create(jacksonConversions.toJSR353(node))));
                            }
                            return rowId;
                        }
                        value =
                            JSONCellFactory.create(jacksonConversions.toJSR353(jackson));
                    }
                } catch (RuntimeException e) {
                    value = handleNotFound(e, content.getURI());
                }
            } else {
                value = (DataCell)jsonValue;
            }
            container.addRowToTable(new DefaultRow(RowKey.createRowKey(rowId++), value));
            //} while (reader.hasMoreZipEntries());
        }
        return rowId;
    }

    /**
     * @param e A possible error (can be {@code null}).
     * @param uri The location we read from.
     * @return The missing cell if we should not fail on reading.
     * @throws RuntimeException Fail on error.
     */
    protected DataCell handleNotFound(final RuntimeException e, final URI uri) {
        if (e == null) {
            if (m_settings.isFailIfNotFound()) {
                throw new NullPointerException("Not found " + m_settings.getJsonPath() + "\n" + uri);
            }
            return DataType.getMissingCell();
        }
        if (m_settings.isFailIfNotFound()) {
            throw new RuntimeException(e.getMessage() + "\n" + uri, e);
        }
        return new MissingCell(e.getMessage());
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
        String location = m_settings.getLocation();
        String warning = CheckUtils.checkSourceFile(location);
        if (warning != null) {
            setWarningMessage(warning);
        }
        m_settings.checkColumnName();
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
