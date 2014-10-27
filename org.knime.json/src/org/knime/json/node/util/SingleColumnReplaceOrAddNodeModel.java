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
 *   19 Sept 2014 (Gabor): created
 */
package org.knime.json.node.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.json.JSONCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortType;

/**
 * This {@link NodeModel} abstraction handles the case of a single column change or add using a {@link ColumnRearranger}
 * . It provides the settings in the form of custom settings.
 *
 * @author Gabor Bakos
 * @param <S> The type of the model-specific {@link ReplaceOrAddColumnSettings}.
 */
public abstract class SingleColumnReplaceOrAddNodeModel<S extends ReplaceOrAddColumnSettings> extends NodeModel {
    /**
     * The key for the append or not boolean value ({@code true} means append).
     */
    protected static final String APPEND = "append";

    /**
     * The key for the input column.
     */
    protected static final String INPUT_COLUMN = "input.column";

    /**
     * The key for the new output column name.
     */
    protected static final String NEW_COLUMN_NAME = "new.column.name";

    private final S m_settings = createSettings();

    //    protected final SettingsModelString m_inputColumnName = createInputColumnName();
    //
    //    protected final SettingsModelBoolean m_append = createAppend();
    //
    //    protected final SettingsModelString m_newColumnName = createNewColumnName();
    //
    //    /**
    //     * @return
    //     */
    //    public static SettingsModelString createNewColumnName() {
    //        return new SettingsModelString(NEW_COLUMN_NAME, "");
    //    }
    //
    /**
     * @return The actual settings used in the {@link SingleColumnReplaceOrAddNodeModel}.
     */
    protected abstract S createSettings();

    //    /**
    //     * @return {@link SettingsModelString} for the input column name.
    //     */
    //    public static SettingsModelString createInputColumnName() {
    //        return new SettingsModelString(INPUT_COLUMN, "");
    //    }
    //
    //    /**
    //     * @return
    //     */
    //    public static SettingsModelBoolean createAppend() {
    //        return new SettingsModelBoolean(APPEND, false);
    //    }
    //
    /**
     * @param nrInDataPorts
     * @param nrOutDataPorts
     */
    public SingleColumnReplaceOrAddNodeModel(final int nrInDataPorts, final int nrOutDataPorts) {
        super(nrInDataPorts, nrOutDataPorts);
    }

    /**
     * @param inPortTypes
     * @param outPortTypes
     */
    public SingleColumnReplaceOrAddNodeModel(final PortType[] inPortTypes, final PortType[] outPortTypes) {
        super(inPortTypes, outPortTypes);
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
     * {@inheritDoc}
     *
     * @throws IOException Problem during execution
     * @throws CanceledExecutionException Execution cancelled
     * @throws InvalidSettingsException Autoguessing of column name failed.
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws IOException, CanceledExecutionException, InvalidSettingsException {
        int inputTableIndex = m_settings.inputTableIndex();
        return new BufferedDataTable[]{exec.createColumnRearrangeTable(inData[inputTableIndex],
            createRearranger(inData[inputTableIndex].getDataTableSpec()), exec)};
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
     * @throws IOException Problem during initialization of {@link ColumnRearranger}.
     * @throws InvalidSettingsException When autoguessing of column name failed.
     */
    protected ColumnRearranger createRearranger(final DataTableSpec inSpecs) throws IOException,
        InvalidSettingsException {
        ColumnRearranger ret = new ColumnRearranger(inSpecs);
        String input = m_settings.getInputColumnName();
        if (input == null) {
            input = handleNonSetColumn(inSpecs).getName();
        }
        final int inputIndex = inSpecs.findColumnIndex(input);
        final int[] otherIndices = findOtherIndices(inSpecs);
        String newColumnName = m_settings.getNewColumnName();
        if (newColumnName == null) {
            newColumnName = input;
        }
        String outputColName =
            m_settings.isAppend() ? DataTableSpec.getUniqueColumnName(inSpecs, newColumnName) : m_settings
                .getInputColumnName();
        DataColumnSpec output = createOutputSpec(outputColName);
        CellFactory factory = createCellFactory(output, inputIndex, otherIndices);
        if (m_settings.isAppend()) {
            ret.append(factory);
        } else {
            ret.replace(factory, input);
        }
        return ret;
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
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        try {
            return new DataTableSpec[]{createRearranger(inSpecs[m_settings.inputTableIndex()]).createSpec()};
        } catch (IOException e) {
            throw new InvalidSettingsException(e.getMessage(), e);
        }
    }

    /**
     * @return the settings
     */
    public S getSettings() {
        return m_settings;
    }
}