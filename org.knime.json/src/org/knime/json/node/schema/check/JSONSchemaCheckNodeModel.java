/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *   24 Sept. 2014 (Gabor): created
 */
package org.knime.json.node.schema.check;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.json.JSONCell;
import org.knime.core.data.json.JSONCellFactory;
import org.knime.core.data.json.JSONValue;
import org.knime.core.data.json.JacksonConversions;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.streamable.simple.SimpleStreamableFunctionNodeModel;
import org.knime.json.internal.Activator;
import org.knime.json.node.util.RemoveOrAddColumnSettings;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.github.fge.jackson.JacksonUtils;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;

/**
 * This is the model implementation of the "JSONSchemaCheck" node. Checks a JSON column's values against a Schema and
 * fails if it do not match.
 *
 * @author Gabor Bakos
 */
public final class JSONSchemaCheckNodeModel extends SimpleStreamableFunctionNodeModel {
    private JSONSchemaCheckSettings m_settings = createJSONSchemaSettings();

    /**
     * Constructor for the node model.
     */
    protected JSONSchemaCheckNodeModel() {
        super();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ColumnRearranger createColumnRearranger(final DataTableSpec dataTableSpec)
        throws InvalidSettingsException {
        if (m_settings.getInputColumn() == null || m_settings.getInputColumn().isEmpty()) {
            m_settings.setInputColumn(handleNonSetColumn(dataTableSpec).getName());
        }
        try {
            final JacksonConversions conv = JacksonConversions.getInstance();
            final JsonSchema schema;
            final ClassLoader jsonSchemaValidatorClassLoader = Activator.getInstance().getJsonSchemaValidatorClassLoader();
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(jsonSchemaValidatorClassLoader);
                schema =
                    JsonSchemaFactory.byDefault().getJsonSchema(
                        conv.toJackson(((JSONValue)JSONCellFactory.create(m_settings.getInputSchema(), false))
                            .getJsonValue()));
            } finally {
                Thread.currentThread().setContextClassLoader(cl);
            }
            final int index = dataTableSpec.findColumnIndex(m_settings.getInputColumn());
            ColumnRearranger ret = new ColumnRearranger(dataTableSpec);
            String col = m_settings.getErrorMessageColumn();
            String errorMessageColumn = DataTableSpec.getUniqueColumnName(dataTableSpec, col == null ? "null" : col);
            ret.append(new SingleCellFactory(new DataColumnSpecCreator(errorMessageColumn, JSONCell.TYPE)
                .createSpec()) {
                @Override
                public DataCell getCell(final DataRow row) {
                    final DataCell cell = row.getCell(index);
                    if (cell instanceof JSONValue) {
                        JSONValue jv = (JSONValue)cell;
                        JsonNode json = conv.toJackson(jv.getJsonValue());
                        RuntimeException e = null;
                        ProcessingReport report = null;
                        try {
                            report = schema.validate(json);
                            if (!report.isSuccess()) {
                                e = new RuntimeException(report.toString());
                                for (ProcessingMessage message : report) {
                                    e.addSuppressed(message.asException());
                                }
                            }
                        } catch (ProcessingException ex) {
                            e = new RuntimeException(ex.getMessage(), ex);
                        }
                        if (e != null) {
                            if (m_settings.isFailOnInvalidJson()) {
                                throw new RuntimeException("Failed to validate row: " + row.getKey() + "\n"
                                    + e.getMessage(), e);
                            }
                            if (report!= null && !report.isSuccess()) {
                                ArrayNode array = new ArrayNode(JacksonUtils.nodeFactory());
                                for (ProcessingMessage processingMessage : report) {
                                    array.add(processingMessage.asJson());
                                }
                                return JSONCellFactory.create(JacksonConversions.getInstance().toJSR353(array));
                            }
                            return JSONCellFactory.create(JacksonConversions.getInstance().toJSR353(new TextNode(e.getMessage())));
                        }
                        return DataType.getMissingCell();
                    }
                    return DataType.getMissingCell();
                }
            });
            return ret;
        } catch (ProcessingException | IOException e) {
            throw new InvalidSettingsException(e.getMessage(), e);
        }
    }

    /**
     * This method gets called when no column was selected.
     *
     * @param tableSpec The input table spec.
     * @return The autoguessed input column spec.
     * @throws InvalidSettingsException Could not find proper column.
     */
    protected DataColumnSpec handleNonSetColumn(final DataTableSpec tableSpec) throws InvalidSettingsException {
        List<String> compatibleCols = new ArrayList<>();
        for (DataColumnSpec c : tableSpec) {
            if (c.getType().isCompatible(JSONValue.class)) {
                compatibleCols.add(c.getName());
            }
        }
        if (compatibleCols.size() == 1) {
            // auto-configure
            m_settings.setInputColumn(compatibleCols.get(0));
            return tableSpec.getColumnSpec(compatibleCols.get(0));
        } else if (compatibleCols.size() > 1) {
            // auto-guessing
            m_settings.setInputColumn(compatibleCols.get(0));
            setWarningMessage("Auto guessing: using column \"" + compatibleCols.get(0) + "\".");
            return tableSpec.getColumnSpec(compatibleCols.get(0));
        } else {
            throw new InvalidSettingsException(
                RemoveOrAddColumnSettings.NO_JSON_COLUMNS_USE_FOR_EXAMPLE_THE_STRING_TO_JSON_NODE_TO_CREATE_ONE);
        }
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
     * @return
     */
    static JSONSchemaCheckSettings createJSONSchemaSettings() {
        return new JSONSchemaCheckSettings();
    }
}
