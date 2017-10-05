/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *   24 Sept. 2014 (Gabor): created
 */
package org.knime.json.node.patch.create;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.json.JSONCellFactory;
import org.knime.core.data.json.JSONValue;
import org.knime.core.data.json.JacksonConversions;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.json.node.util.SingleColumnReplaceOrAddNodeModel;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonpatch.diff.JsonDiff;

/**
 * This is the model implementation of JSONDiff. Compares {@link JSONValue}s.
 *
 * @author Gabor Bakos
 */
public final class JSONPatchCreateNodeModel extends SingleColumnReplaceOrAddNodeModel<JSONPatchCreateSettings> {

    /**
     * Constructor for the node model.
     */
    protected JSONPatchCreateNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     *
     * @throws IOException When cannot read JSON values.
     */
    @Override
    protected CellFactory createCellFactory(final DataColumnSpec output, final int inputIndex,
        final int... otherColumns) throws IOException {
        final int rightIndex = otherColumns[0];
        final JacksonConversions conv = JacksonConversions.getInstance();
        //final JsonPatch patch = JsonPatch.fromJson(patchNode);
        return new SingleCellFactory(output) {
            @Override
            public DataCell getCell(final DataRow row) {
                DataCell cell = row.getCell(inputIndex);
                if (cell instanceof JSONValue) {
                    JSONValue jsonCell = (JSONValue)cell;
                    JsonNode jsonNode = conv.toJackson(jsonCell.getJsonValue());
                    DataCell otherCol = row.getCell(rightIndex);
                    if (otherCol instanceof JSONValue) {
                        JSONValue otherJsonValue = (JSONValue)otherCol;
                        JsonNode other = conv.toJackson(otherJsonValue.getJsonValue());
                        JsonNode diff = JsonDiff.asJson(jsonNode, other);
                        return JSONCellFactory.create(conv.toJSR353(diff));
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
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        DataTableSpec[] res = super.configure(inSpecs);
        if (getSettings().getInputColumnRight().isEmpty()) {
            getSettings().setInputColumnRight(handleNonSetColumn(inSpecs[0]).getName());
        }
        String warningPrefix = getWarningMessage();
        if (warningPrefix == null) {
            warningPrefix = "";
        }
        if (!warningPrefix.isEmpty()) {
            warningPrefix += "\n";
        }
        if (getSettings().getInputColumnRight().equals(getSettings().getInputColumnLeft())) {
            setWarningMessage(warningPrefix + "The same column (" + getSettings().getInputColumnLeft()
                + ") was selected both for the source and target columns.");
        }
        return res;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int[] findOtherIndices(final DataTableSpec inSpecs) {
        return new int[]{inSpecs.findColumnIndex(getSettings().getInputColumnRight())};
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
     * {@inheritDoc}
     */
    @Override
    protected JSONPatchCreateSettings createSettings() {
        return createJSONPatchCreateSettings();
    }

    /**
     * @return
     */
    static JSONPatchCreateSettings createJSONPatchCreateSettings() {
        return new JSONPatchCreateSettings();
    }
}
