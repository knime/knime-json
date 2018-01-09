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
package org.knime.json.node.jsonpointer;

import java.io.File;
import java.io.IOException;

import javax.json.JsonValue;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.data.MissingCell;
import org.knime.core.data.blob.BinaryObjectCellFactory;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.BooleanCell.BooleanCellFactory;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.json.JSONCellFactory;
import org.knime.core.data.json.JSONValue;
import org.knime.core.data.json.JacksonConversions;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.json.node.util.OutputType;
import org.knime.json.node.util.SingleColumnReplaceOrAddNodeModel;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BinaryNode;
import com.github.fge.jackson.jsonpointer.JsonPointer;
import com.github.fge.jackson.jsonpointer.JsonPointerException;

/**
 * This is the model implementation of JSONPointer. Selects certain pointers from the selected JSON column.
 *
 * @author Gabor Bakos
 */
public class JSONPointerNodeModel extends SingleColumnReplaceOrAddNodeModel<JSONPointerSettings> {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(JSONPointerNodeModel.class);
    /**
     * Constructor for the node model.
     */
    protected JSONPointerNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // No internal state
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        super.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.loadValidatedSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
        // No internal state
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
        // No internal state
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected CellFactory createCellFactory(final DataColumnSpec output, final int inputIndex,
        final int... otherColumns) {
        final JacksonConversions conv = JacksonConversions.getInstance();
        final JsonPointer pointer;
        try {
            pointer = new JsonPointer(getSettings().getJsonPointer());
        } catch (JsonPointerException e) {
            throw new IllegalStateException("Invalid pointer: " + e.getMessage(), e);
        }
        final OutputType returnType = getSettings().getReturnType();
        return new SingleCellFactory(output) {

            @Override
            public DataCell getCell(final DataRow row) {
                DataCell cell = row.getCell(inputIndex);
                if (cell instanceof JSONValue) {
                    JSONValue jsonCell = (JSONValue)cell;
                    JsonValue jsonValue = jsonCell.getJsonValue();
                    try {
                        JsonNode value = pointer.path(conv.toJackson(jsonValue));
                        if (value.isMissingNode()) {
                            return DataType.getMissingCell();
                        }
                        switch (returnType) {
                            case Bool:
                                return BooleanCellFactory.create(value.toString());
                            case Int:
                                return new IntCell(Integer.parseInt(value.toString()));
                            case Json:
                                return JSONCellFactory.create(conv.toJSR353(value));
                            case Real:
                                return new DoubleCell(Double.parseDouble(value.toString()));
                            case String:
                                return new StringCell(value.toString());
                            case Binary:
                                if (value instanceof BinaryNode) {
                                    final BinaryNode bn = (BinaryNode)value;
                                    final byte[] bs = bn.binaryValue();
                                    return new BinaryObjectCellFactory().create(bs);
                                }
                                return DataType.getMissingCell();
                            default:
                                throw new UnsupportedOperationException("Not supported return type: " + returnType);
                        }
                    } catch (RuntimeException | IOException e) {
                        return new MissingCell(e.getMessage());
                    }
                }
                return DataType.getMissingCell();
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataColumnSpec createOutputSpec(final String outputColName) {
        return new DataColumnSpecCreator(outputColName, getSettings().getReturnType().getDataType()).createSpec();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected JSONPointerSettings createSettings() {
        return createJSONPathProjectionSettings();
    }

    /**
     * @return
     */
    static JSONPointerSettings createJSONPathProjectionSettings() {
        return new JSONPointerSettings(LOGGER);
    }
}
