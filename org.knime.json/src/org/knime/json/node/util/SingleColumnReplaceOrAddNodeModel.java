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
 *   19 Sept 2014 (Gabor): created
 */
package org.knime.json.node.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.json.JSONCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.streamable.simple.SimpleStreamableFunctionNodeModel;

/**
 * This {@link NodeModel} abstraction handles the case of a single column change or add using a {@link ColumnRearranger}
 * . It provides the settings in the form of custom settings.
 *
 * @author Gabor Bakos
 * @param <S> The type of the model-specific {@link RemoveOrAddColumnSettings}.
 */
public abstract class SingleColumnReplaceOrAddNodeModel<S extends RemoveOrAddColumnSettings> extends
    SimpleStreamableFunctionNodeModel {
    /**
     * The key for the remove or not the source/input column boolean value ({@code true} means remove).
     */
    protected static final String REMOVE_SOURCE = "remove.input.column";

    /**
     * The key for the input column.
     */
    protected static final String INPUT_COLUMN = "input.column";

    /**
     * The key for the new output column name.
     */
    protected static final String NEW_COLUMN_NAME = "new.column.name";

    private final S m_settings = createSettings();

    /**
     * @return The actual settings used in the {@link SingleColumnReplaceOrAddNodeModel}.
     */
    protected abstract S createSettings();

    /**
     * @param nrInDataPorts Number of input data ports. Should be {@code 1}.
     * @param nrOutDataPorts Number of output data ports. Should be {@code 1}.
     */
    public SingleColumnReplaceOrAddNodeModel(final int nrInDataPorts, final int nrOutDataPorts) {
        super();
        if (nrInDataPorts != 1 || nrOutDataPorts != 1) {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Creates the output spec based on the output column name. By default it creates JSON output column, override if it
     * is not suitable.
     *
     * @param outputColName The name of the output column.
     * @return The basic {@link DataColumnSpec} with the name.
     */
    protected DataColumnSpec createOutputSpec(final String outputColName) {
        DataColumnSpec output = new DataColumnSpecCreator(outputColName, JSONCell.TYPE).createSpec();
        return output;
    }

    /**
     * Creates a {@link CellFactory} (if nothing else overridden, this should be a {@link SingleCellFactory}).
     *
     * @param output The output column spec.
     * @param inputIndex The selected input column's ({@code 0}-based) index.
     * @param otherColumns the index of non-input columns that are used in the computation of the values.
     * @return The -usually- {@link SingleCellFactory} that creates the new content.
     * @throws IOException Something went wrong during factory creation.
     * @see DataTableSpec#findColumnIndex(String)
     */
    protected abstract CellFactory createCellFactory(DataColumnSpec output, int inputIndex, int... otherColumns)
        throws IOException;

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
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_settings.validateSettings(settings);
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
            if (c.getType().isCompatible(getSettings().getInputColumnType())) {
                compatibleCols.add(c.getName());
            }
        }
        if (compatibleCols.size() == 1) {
            // auto-configure
            m_settings.setInputColumnName(compatibleCols.get(0));
            return tableSpec.getColumnSpec(compatibleCols.get(0));
        } else if (compatibleCols.size() > 1) {
            // auto-guessing
            m_settings.setInputColumnName(compatibleCols.get(0));
            setWarningMessage("Auto guessing: using column \"" + compatibleCols.get(0) + "\".");
            return tableSpec.getColumnSpec(compatibleCols.get(0));
        } else {
            throw new InvalidSettingsException(m_settings.autoGuessFailedMessage());
        }
    }

    /**
     * Based on the settings it creates the rearranger. <br/>
     * Override if you need more control on how to create the results.
     *
     * @param inSpecs The input table spec.
     * @return The {@link ColumnRearranger}.
     * @throws InvalidSettingsException When autoguessing of column name failed or problem during initialization of
     *             {@link ColumnRearranger}..
     */
    @Override
    protected ColumnRearranger createColumnRearranger(final DataTableSpec inSpecs) throws InvalidSettingsException {
        ColumnRearranger ret = new ColumnRearranger(inSpecs);
        String input = m_settings.getInputColumnName();
        if (input == null || !inSpecs.containsName(input)) {
            input = handleNonSetColumn(inSpecs).getName();
        }
        final int inputIndex = inSpecs.findColumnIndex(input);
        final int[] otherIndices = findOtherIndices(inSpecs);
        String newColumnName = m_settings.getNewColumnName();
        if (m_settings instanceof ReplaceColumnSettings) {
            ReplaceColumnSettings s = (ReplaceColumnSettings)m_settings;
            if (s.isRemoveInputColumn()) {
                newColumnName = s.getInputColumnName();
            }
        }
        if (newColumnName == null || newColumnName.trim().isEmpty()) {
            throw new InvalidSettingsException("Please specify the output column's name");
        }
        DataTableSpec specs = removeInputIfRequired(inSpecs, input);
        String outputColName = DataTableSpec.getUniqueColumnName(specs, newColumnName);
        DataColumnSpec output = createOutputSpec(outputColName);
        CellFactory factory;
        try {
            factory = createCellFactory(output, inputIndex, otherIndices);
            applyFactory(ret, input, factory);
        } catch (IOException e) {
            throw new InvalidSettingsException(e);
        }
        return ret;
    }

    /**
     * Removes the input column if that settings is valid.
     *
     * @param inSpecs The input {@link DataTableSpec}.
     * @param input The input column's name.
     * @return The update {@link DataTableSpec}.
     */
    private DataTableSpec removeInputIfRequired(final DataTableSpec inSpecs, final String input) {
        DataTableSpec specs = inSpecs;
        if (getSettings().isRemoveInputColumn() && inSpecs.containsName(input)) {
            int numColumns = specs.getNumColumns();
            DataColumnSpec[] newSpecs = new DataColumnSpec[numColumns - 1];
            int j = 0;
            for (int i = 0; i < numColumns; ++i) {
                DataColumnSpec spec = specs.getColumnSpec(i);
                if (!spec.getName().equals(input)) {
                    newSpecs[j++] = spec;
                }
            }
            specs = new DataTableSpecCreator().addColumns(newSpecs).createSpec();
        }
        return specs;
    }

    /**
     * Adds the {@code factory} and optionally replaces (in case the settings is a {@link ReplaceColumnSettings}) or removes the {@code input} column.
     *
     * @param rearranger The {@link ColumnRearranger}.
     * @param input The input column's name.
     * @param factory The {@link SingleCellFactory} to add.
     */
    private void applyFactory(final ColumnRearranger rearranger, final String input, final CellFactory factory) {
        if (m_settings instanceof ReplaceColumnSettings) {
            ReplaceColumnSettings s = (ReplaceColumnSettings)m_settings;
            if (s.isRemoveInputColumn()) {
                rearranger.replace(factory, input);
                return;
            }
        }
        rearranger.append(factory);
        if (m_settings.isRemoveInputColumn()) {
            rearranger.remove(input);
        }
    }

    /**
     * The default implementation returns an empty array, <b>override</b> it if you need other indices too.
     *
     * @param inSpecs The input table spec.
     * @return The index of the other (not the main input) columns.
     */
    protected int[] findOtherIndices(final DataTableSpec inSpecs) {
        return new int[0];
    }

    /**
     * @return the settings
     */
    public S getSettings() {
        return m_settings;
    }
}