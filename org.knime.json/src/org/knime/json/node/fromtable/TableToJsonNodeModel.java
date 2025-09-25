package org.knime.json.node.fromtable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.knime.core.data.BooleanValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.json.JSONCell;
import org.knime.core.data.json.JSONCellFactory;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.util.JsonUtil;
import org.knime.core.util.Pair;
import org.knime.json.util.JSR353Util;
import org.knime.node.parameters.widget.choices.Label;

import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonValue;

/**
 * This is the model implementation of TableToJson. Converts a whole table to a single JSON cell.
 *
 * @author Gabor Bakos
 */
class TableToJsonNodeModel extends NodeModel {
    /**
     * The possible directions of how the JSON should be combined.
     */
    protected enum Direction {
            /** aggregate by rows later */
            @Label(value = "Row-oriented (n input rows → 1 output cell)",
                description = "The values from the selected columns are collected "
                    + "and concatenated row-wise to a JSON object/array, "
                    + "after that these are combined by the rows to a single JSON value.")
            RowsOutside, // NOSONAR
            /** do not aggregate by rows */
            @Label(value = "Keep rows (n input rows → n output cells)",
                description = "The selected columns are combined to a new JSON column containing "
                    + "the values from the columns and the name of the columns as keys. "
                    + "This option does not combine the rows of the input table.")
            KeepRows, // NOSONAR
            /** aggregate by columns later */
            @Label(value = "Column-oriented (n input rows → 1 output cell)",
                description = "The values from the selected columns are collected "
                    + "and concatenated column-wise to a JSON object/array, "
                    + "after that these are combined by the columns to a single JSON value.")
            ColumnsOutside; // NOSONAR
    }

    /**
     * Possible options how the row keys should be handled.
     */
    protected enum RowKeyOption {
            /** row keys are not included in the output */
            @Label(value = "Omit row key",
                description = "The row keys will be omitted, not used in the generated JSON value "
                    + "(which are arrays when the rows are not kept).")
            omit, // NOSONAR
            /** row keys are values just as if they were regular columns. */
            @Label(value = "Row key as JSON value with key",
                description = "The row keys will be included in the generated JSON (array) "
                    + "value with the specified key.")
            asValue, // NOSONAR
            /**
             * row keys are keys, as if they were data bound keys, create objects instead of array, not applicable to
             * {@link Direction#ColumnsOutside}.
             */
            @Label(value = "Row key as JSON key",
                description = "The row keys are added to the generated JSON value as a key, "
                    + "in this case not an array, but an object is created.")
            asKey; // NOSONAR
    }

    /**
     * Enum for handling missing values in the JSON output.
     */
    protected enum MissingValueHandling {
            /** Missing values are omitted from the JSON output */
            @Label(value = "are omitted",
                description = "Missing values from the input table do not generate a key in the resulting "
                    + "JSON structure, they are omitted completely. "
                    + "Note that in a column-oriented transformation missing cells will still be inserted "
                    + "as null values in the column's array because otherwise the row arrays for different "
                    + "columns may have different numbers of entries. This would make it impossible to "
                    + "reconstruct the original table.")
            OMITTED,
            /** Missing values are inserted as null values */
            @Label(value = "are inserted as 'null'",
                description = "Missing values from the input table are inserted as null values.")
            AS_NULL;
    }

    private static final NodeLogger LOGGER = NodeLogger.getLogger(TableToJsonNodeModel.class);

    //    private final SettingsModelColumnFilter2 m_selectedColumns = createSelectedColumns();
    //
    //    private final SettingsModelString m_direction = createDirection();
    //
    //    private final SettingsModelString m_colNameSeparator = createColumnNameSeparator();
    //
    //    private final SettingsModelString m_outputColumnName = createOutputColumnName();
    //
    //    private final SettingsModelString m_rowKeyKey = createRowKeyKey();
    //
    //    private final SettingsModelBoolean m_includeRowKey = createIncludeRowKey();
    private final TableToJsonSettings m_settings = new TableToJsonSettings();

    /**
     * Constructor for the node model.
     */
    protected TableToJsonNodeModel() {
        super(1, 1);
    }

    //    /**
    //     * @return The {@link SettingsModelString} for row key keys.
    //     */
    //    protected static SettingsModelString createRowKeyKey() {
    //        return new SettingsModelString("rowkey.key", "ROWID");
    //    }
    //
    //    /**
    //     * @return The {@link SettingsModelBoolean} to indicate whether row keys should be included in JSON.
    //     */
    //    protected static SettingsModelBoolean createIncludeRowKey() {
    //        return new SettingsModelBoolean("include.rowkeys", false);
    //    }
    //
    //    /**
    //     * @return The {@link SettingsModelString} for the output column name.
    //     */
    //    protected static SettingsModelString createOutputColumnName() {
    //        return new SettingsModelString("output.column.name", "JSON");
    //    }
    //
    //    /**
    //     * @return The column name separator when the column name should define a path.
    //     */
    //    protected static SettingsModelString createColumnNameSeparator() {
    //        return new SettingsModelString("column.name.separator", ".");
    //    }
    //
    //    /**
    //     * @return The {@link SettingsModelString} for the direction (possible values are the {@link Direction#name() names}
    //     *         of {@link Direction#values() values}).
    //     */
    //    protected static SettingsModelString createDirection() {
    //        return new SettingsModelString("direction", Direction.RowsOutside.name());
    //    }
    //
    //    /**
    //     * @return The selected columns' values to convert to JSON.
    //     */
    //    protected static SettingsModelColumnFilter2 createSelectedColumns() {
    //        return new SettingsModelColumnFilter2("selectedColumns", JsonPathUtils.supportedInputDataValuesAsArray());
    //    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws Exception {
        BufferedDataTable ret;
        Direction direction = m_settings.getDirection();
        switch (direction) {
            case RowsOutside:
                ret = rowsOutside(inData[0], exec);
                break;
            case KeepRows:
                ret = keepRows(inData[0], exec);
                break;
            case ColumnsOutside:
                ret = columnsOutside(inData[0], exec);
                break;
            //            case FromColumnNamesKeepRows:
            //                ret = rowwiseFromColumns(inData[0], exec);
            //                break;
            default:
                throw new UnsupportedOperationException("Unknown direction: " + direction);
        }
        return new BufferedDataTable[]{ret};
    }

    /**
     * @param data
     * @param exec
     * @throws InvalidSettingsException
     */
    private BufferedDataTable columnsOutside(final BufferedDataTable data, final ExecutionContext exec)
        throws InvalidSettingsException {
        BufferedDataContainer container = exec.createDataContainer(configure(new DataTableSpec[]{data.getSpec()})[0]);
        container.addRowToTable(new DefaultRow(RowKey.createRowKey(1L), createCellColumnsOutside(data)));
        container.close();
        return container.getTable();
    }

    /**
     * @param data
     * @return
     */
    private DataCell createCellColumnsOutside(final BufferedDataTable data) {
        final String[] includes = m_settings.getSelectedColumns().applyTo(data.getSpec()).getIncludes();
        final int[] indices = new int[includes.length];
        Map<String, Object> structure = new LinkedHashMap<>();
        Map<String, List<String>> keys = new LinkedHashMap<>();
        SortedMap<List<String>, Integer> keysSplit = createListKeySortedMap();
        fillStructures(data.getSpec(), m_settings.getColumnNameSeparator(), includes, indices, keys, keysSplit,
            structure);
        JsonObjectBuilder root = JsonUtil.getProvider().createObjectBuilder();
        Map<String, JsonArrayBuilder> keysToArrays = new LinkedHashMap<>();
        for (int i = 0; i < includes.length; i++) {
            String colName = includes[i];
            JsonArrayBuilder ab = JsonUtil.getProvider().createArrayBuilder();
            keysToArrays.put(colName, ab);
        }
        String key = m_settings.getRowKeyKey();
        switch (m_settings.getRowKey()) {
            case omit:
                break;
            case asValue:
                JsonArrayBuilder ab = JsonUtil.getProvider().createArrayBuilder();
                keysToArrays.put(key, ab);
                break;
            case asKey:
                CheckUtils.checkState(false, "Row key as key is not allowed for the columns outside option.");
                break;
            default:
                CheckUtils.checkState(false, "Unsupported row key option: " + m_settings.getRowKey());
        }
        for (DataRow dataRow : data) {
            for (int i = 0; i < indices.length; i++) {
                final int index = indices[i];
                DataCell cell = dataRow.getCell(index);
                try {
                    JSR353Util.addToArrayFromCell(keysToArrays.get(includes[i]), cell);
                } catch (IOException e) {
                    LOGGER.warn("Failed to read binary object in row (" + dataRow.getKey() + "), replaced with null.",
                        e);
                    keysToArrays.get(includes[i]).addNull();
                }
            }
            if (m_settings.getRowKey() == RowKeyOption.asValue) {
                keysToArrays.get(key).add(dataRow.getKey().getString());
            }
        }
        if (!m_settings.isColumnNamesAsPath()) {
            for (Entry<String, JsonArrayBuilder> entry : keysToArrays.entrySet()) {
                root.add(entry.getKey(), entry.getValue());
            }
        } else {
            if (m_settings.getRowKey() == RowKeyOption.asValue) {
                root.add(key, keysToArrays.get(key));
            }
            visitStructure(structure, root, keysToArrays, Collections.<String> emptyList());
        }
        return JSONCellFactory.create(root.build());
    }

    /**
     * @param data
     * @param exec
     * @return
     * @throws InvalidSettingsException
     */
    private BufferedDataTable keepRows(final BufferedDataTable data, final ExecutionContext exec)
        throws InvalidSettingsException, CanceledExecutionException {
        final String[] includes = m_settings.getSelectedColumns().applyTo(data.getSpec()).getIncludes();
        final int[] indices = new int[includes.length];
        for (int i = 0; i < includes.length; i++) {
            String colName = includes[i];
            indices[i] = data.getSpec().findColumnIndex(colName);
        }
        ColumnRearranger rearranger = createRearranger(data.getSpec());
        return exec.createColumnRearrangeTable(data, rearranger, exec);
        //        BufferedDataContainer container = exec.createDataContainer(configure(new DataTableSpec[]{data.getSpec()})[0]);
        //        int rowIndex = 0;
        //        final int rowCount = data.getRowCount();
        //        for (DataRow dataRow : data) {
        //            exec.checkCanceled();
        //            exec.setProgress(rowIndex++ / (double)rowCount,
        //                String.format("Row %d/%d (\"%s\")", rowIndex, rowCount, dataRow.getKey()));
        //            JsonObjectBuilder row = Json.createObjectBuilder();
        //            for (int i = 0; i < indices.length; i++) {
        //                final int index = indices[i];
        //                DataCell cell = dataRow.getCell(index);
        //                JSR353Util.fromCell(includes[i], cell, row);
        //            }
        //            switch (m_settings.getRowKey()) {
        //                case asKey:
        //                    container.addRowToTable(new DefaultRow(dataRow.getKey(), JSONCellFactory.create(Json
        //                        .createObjectBuilder().add(dataRow.getKey().getString(), row.build()).build())));
        //                    break;
        //                case asValue:
        //                    row.add(m_settings.getRowKeyKey(), dataRow.getKey().getString());
        //                    //intentional fall through
        //                case omit:
        //                    container.addRowToTable(new DefaultRow(dataRow.getKey(), JSONCellFactory.create(row.build())));
        //                    break;
        //                default:
        //                    CheckUtils.checkState(false, "Unsupported row key option: " + m_settings.getRowKey());
        //            }
        //        }
        //        container.close();
        //        return container.getTable();
    }

    /**
     * @param data
     * @param exec
     * @return
     * @throws InvalidSettingsException
     */
    private BufferedDataTable rowsOutside(final BufferedDataTable data, final ExecutionContext exec)
        throws InvalidSettingsException {
        BufferedDataContainer container = exec.createDataContainer(configure(new DataTableSpec[]{data.getSpec()})[0]);
        container.addRowToTable(new DefaultRow(RowKey.createRowKey(1L), createCellRowsOutside(data)));
        container.close();
        return container.getTable();
    }

    /**
     * @param data
     * @return
     */
    private DataCell createCellRowsOutside(final BufferedDataTable data) {
        final String[] includes = m_settings.getSelectedColumns().applyTo(data.getSpec()).getIncludes();
        final int[] indices = new int[includes.length];
        final Map<String, List<String>> keys = new LinkedHashMap<>();
        final SortedMap<List<String>, Integer> keysSplit = createListKeySortedMap();
        final Map<String, Object> structure = new LinkedHashMap<>();
        fillStructures(data.getSpec(), m_settings.getColumnNameSeparator(), includes, indices, keys, keysSplit,
            structure);
        if (m_settings.getRowKey() == RowKeyOption.asKey) {
            JsonObjectBuilder root = JsonUtil.getProvider().createObjectBuilder();
            for (DataRow dataRow : data) {
                final JsonObjectBuilder row = visitRow(includes, indices, structure, dataRow);
                root.add(dataRow.getKey().getString(), row);
            }
            return JSONCellFactory.create(root.build());
        }
        final JsonArrayBuilder root = JsonUtil.getProvider().createArrayBuilder();
        for (DataRow dataRow : data) {
            final JsonObjectBuilder row = visitRow(includes, indices, structure, dataRow);
            switch (m_settings.getRowKey()) {
                case asValue:
                    row.add(m_settings.getRowKeyKey(), dataRow.getKey().getString());
                    break;
                case omit:
                    break;
                case asKey:
                    CheckUtils.checkState(false, "This case should be handled previously");
                    break;
                default:
                    CheckUtils.checkState(false, "Unsupported row key option: " + m_settings.getRowKey());
            }
            root.add(row);
        }
        return JSONCellFactory.create(root.build());
    }

    /**
     * @param includes
     * @param indices
     * @param structure
     * @param dataRow
     * @return
     */
    JsonObjectBuilder visitRow(final String[] includes, final int[] indices, final Map<String, Object> structure,
        final DataRow dataRow) {
        JsonObjectBuilder row = JsonUtil.getProvider().createObjectBuilder();
        if (m_settings.isColumnNamesAsPath()) {
            visitStructure(structure, dataRow, row);
        } else {
            visitSimple(includes, indices, dataRow, row);
        }
        return row;
    }

    /**
     * @param includes
     * @param indices
     * @param dataRow
     * @param row
     */
    void visitSimple(final String[] includes, final int[] indices, final DataRow dataRow, final JsonObjectBuilder row) {
        for (int i = 0; i < indices.length; i++) {
            final int index = indices[i];
            DataCell cell = dataRow.getCell(index);
            try {
                fromCell(row, includes[i], cell);
            } catch (IOException e) {
                LOGGER.warn("Failed to read binary object data value (row: " + dataRow.getKey() + ")", e);
                row.addNull(includes[i]);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        //No internal state
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        //        if (m_settings.getDirection() == Direction.FromColumnNamesKeepRows) {
        //            return null;
        //        }
        if (m_settings.getDirection() == Direction.KeepRows) {
            return new DataTableSpec[]{createRearranger(inSpecs[0]).createSpec()};
        }
        return new DataTableSpec[]{new DataTableSpec(new DataColumnSpecCreator(m_settings.getOutputColumnName(),
            JSONCell.TYPE).createSpec())};
    }

    /**
     * @param spec
     * @return
     */
    private ColumnRearranger createRearranger(final DataTableSpec spec) {
        final ColumnRearranger rearranger = new ColumnRearranger(spec);
        rearranger.append(new SingleCellFactory(new DataColumnSpecCreator(DataTableSpec.getUniqueColumnName(
            rearranger.createSpec(), m_settings.getOutputColumnName()), JSONCell.TYPE).createSpec()) {

            private final String[] m_includes = m_settings.getSelectedColumns().applyTo(spec).getIncludes();

            private final int[] m_indices = new int[m_includes.length];

            private final Map<String, List<String>> m_keys = new LinkedHashMap<>();

            private final SortedMap<List<String>, Integer> m_keysSplit = createListKeySortedMap();

            private final Map<String, Object/*Map<String, rec> | Integer | Pair<Integer, Map<String, rec>*/> m_structure =
                new LinkedHashMap<>();
            {
                fillStructures(spec, m_settings.getColumnNameSeparator(), m_includes, m_indices, m_keys, m_keysSplit,
                    m_structure);
            }

            @Override
            public DataCell getCell(final DataRow row) {
                JsonValue value = createValue(row);
                return JSONCellFactory.create(value);
            }

            private JsonValue createValue(final DataRow dataRow) {
                JsonObjectBuilder root = JsonUtil.getProvider().createObjectBuilder();
                if (m_settings.isColumnNamesAsPath()) {
                    visitStructure(m_structure, dataRow, root);
                } else {
                    visitSimple(m_includes, m_indices, dataRow, root);
                    //                    JsonObjectBuilder row = Json.createObjectBuilder();
                    //                    for (int i = 0; i < m_indices.length; i++) {
                    //                        final int index = m_indices[i];
                    //                        DataCell cell = dataRow.getCell(index);
                    //                        try {
                    //                            JSR353Util.fromCell(m_includes[i], cell, row);
                    //                        } catch (IOException e) {
                    //                            LOGGER.warn("Failed to read binary object data value (row: " + dataRow.getKey()+ ")", e);
                    //                            row.addNull(m_includes[i]);
                    //                        }
                    //                    }
                }
                switch (m_settings.getRowKey()) {
                    case omit:
                        break;
                    case asValue:
                        root.add(m_settings.getRowKeyKey(), dataRow.getKey().getString());
                        break;
                    case asKey:
                        return JsonUtil.getProvider().createObjectBuilder()
                            .add(dataRow.getKey().getString(), root.build()).build();
                    default:
                        CheckUtils.checkState(false, "Unsupported row key option: " + m_settings.getRowKey());
                }
                return root.build();
            }
        });
        if (m_settings.isRemoveSourceColumns()) {
            rearranger.remove(m_settings.getSelectedColumns().applyTo(spec).getIncludes());
        }
        return rearranger;
    }

    /**
     * @param spec
     */
    static void fillStructures(final DataTableSpec spec, final String columnNameSeparator, final String[] includes,
        final int[] indices, final Map<String, List<String>> keys, final SortedMap<List<String>, Integer> keysSplit,
        final Map<String, Object/*Map<String, rec> | Integer | Pair<Integer, Map<String, rec>*/> structure) {
        {
            for (int i = 0; i < includes.length; i++) {
                String incl = includes[i];
                String colName = incl;
                indices[i] = spec.findColumnIndex(colName);
                List<String> list = Arrays.asList(incl.split(Pattern.quote(columnNameSeparator), -1));
                keys.put(incl, list);
                keysSplit.put(list, indices[i]);
            }
            for (Entry<List<String>, Integer> entry : keysSplit.entrySet()) {
                Map<String, Object> current = structure;
                List<String> list = entry.getKey();
                for (String key : list.subList(0, list.size() - 1)) {
                    if (current.containsKey(key)) {
                        Object object = current.get(key);
                        if (object instanceof Integer) {
                            Integer colIndex = (Integer)object;
                            current.put(key, Pair.create(colIndex, new LinkedHashMap<String, Object>()));
                        }
                        if (object instanceof Map<?, ?>) {
                            Map<?, ?> map = (Map<?, ?>)object;
                            @SuppressWarnings("unchecked")
                            Map<String, Object> casted = (Map<String, Object>)map;
                            current = casted;
                        }
                    } else {
                        LinkedHashMap<String, Object> newMap = new LinkedHashMap<>();
                        current.put(key, newMap);
                        current = newMap;
                    }
                }
                current.put(list.get(list.size() - 1), entry.getValue());
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_settings.loadSettingsModel(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        TableToJsonSettings tmp = new TableToJsonSettings();
        tmp.loadSettingsModel(settings);
        //        m_selectedColumns.validateSettings(settings);
        //        m_direction.validateSettings(settings);
        //        m_colNameSeparator.validateSettings(settings);
        //        m_outputColumnName.validateSettings(settings);
        //        m_includeRowKey.validateSettings(settings);
        //        m_rowKeyKey.validateSettings(settings);
        if (tmp.isColumnNamesAsPath()) {
            CheckUtils.checkSetting(!tmp.getColumnNameSeparator().isEmpty(),
                "The separator cannot be empty when column names are hierarchical.");
        }
        Direction direction = tmp.getDirection();
        switch (direction) {
        //            case FromColumnNamesKeepRows:
        //                if (tmp.getColumnNameSeparator().isEmpty()) {
        //                    throw new InvalidSettingsException("The separator cannot be empty for this direction setting.");
        //                }
        //                break;
            case ColumnsOutside:
                //Nothing to check.
                break;
            case RowsOutside:
                //Nothing to check.
                break;
            case KeepRows:
                //Nothing to check.
                break;
            default:
                break;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir, final ExecutionMonitor exec) {
        //No internal state
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir, final ExecutionMonitor exec) {
        //No internal state
    }

    /**
     * @return A {@link TreeMap} with a {@link List} ({@link String}) key.
     * @param <T> Type of the values.
     */
    static <T> TreeMap<List<String>, T> createListKeySortedMap() {
        return new TreeMap<>(new Comparator<List<String>>() {
            @Override
            public int compare(final List<String> o1, final List<String> o2) {
                if (o1.size() == 0) {
                    return o2.size() - o1.size();
                }
                if (o2.size() == 0) {
                    return -1;
                }
                String v1 = o1.get(0), v2 = o2.get(0);
                int comp = v1.compareTo(v2);
                if (comp == 0) {
                    return this.compare(o1.subList(1, o1.size()), o2.subList(1, o2.size()));
                }
                return comp;
            }
        });
    }

    private void visitStructure(
        final Map<String, Object/*Map<String, rec> | Integer | Pair<Integer, Map<String, rec>*/> structure,
        final DataRow row, final JsonObjectBuilder root) {
        for (Entry<String, Object> entry : structure.entrySet()) {
            Object object = entry.getValue();
            if (object instanceof Map<?, ?>) {
                Map<?, ?> map = (Map<?, ?>)object;
                @SuppressWarnings("unchecked")
                Map<String, Object> casted = (Map<String, Object>)map;
                JsonObjectBuilder newBuilder = JsonUtil.getProvider().createObjectBuilder();
                visitStructure(casted, row, newBuilder);
                root.add(entry.getKey(), newBuilder);
            }
            if (object instanceof Pair<?, ?>) {
                Pair<?, ?> pair = (Pair<?, ?>)object;
                Integer index = (Integer)pair.getFirst();
                Map<?, ?> map = (Map<?, ?>)pair.getSecond();
                @SuppressWarnings("unchecked")
                Map<String, Object> casted = (Map<String, Object>)map;
                JsonObjectBuilder newBuilder = JsonUtil.getProvider().createObjectBuilder();
                visitStructure(casted, row, newBuilder);
                root.add(entry.getKey(), newBuilder);
                DataCell cell = row.getCell(index);
                try {
                    fromCell(newBuilder, entry.getKey(), cell);
                } catch (IOException e) {
                    LOGGER.warn("Failed to read binary object data value (row: " + row.getKey() + ")", e);
                    newBuilder.addNull(entry.getKey());
                }
            }
            if (object instanceof Integer) {
                Integer index = (Integer)object;
                DataCell cell = row.getCell(index);
                try {
                    fromCell(root, entry.getKey(), cell);
                } catch (IOException e) {
                    LOGGER.warn("Failed to read binary object data value (row: " + row.getKey() + ")", e);
                    root.addNull(entry.getKey());
                }
            }
        }
    }

    private static IntCell TRUE = new IntCell(1);
    private static IntCell FALSE = new IntCell(0);

    private void fromCell(final JsonObjectBuilder root, final String key, final DataCell cell)
        throws IOException {
        if (!m_settings.isMissingsAreOmitted() || !cell.isMissing()) {
            if (m_settings.isBooleansAsNumbers() && (cell instanceof BooleanValue)) {
                JSR353Util.fromCell(key, ((BooleanValue) cell).getBooleanValue() ? TRUE : FALSE, root);
            } else {
                JSR353Util.fromCell(key, cell, root);
            }
        }
    }

    private void visitStructure(
        final Map<String, Object/*Map<String, rec> | Integer | Pair<Integer, Map<String, rec>*/> structure,
        final JsonObjectBuilder root, final Map<String, JsonArrayBuilder> result, final List<String> currentKeys) {
        for (Entry<String, Object> entry : structure.entrySet()) {
            Object object = entry.getValue();
            List<String> newCurrentKeys = new ArrayList<>(currentKeys);
            newCurrentKeys.add(entry.getKey());
            if (object instanceof Map<?, ?>) {
                Map<?, ?> map = (Map<?, ?>)object;
                @SuppressWarnings("unchecked")
                Map<String, Object> casted = (Map<String, Object>)map;
                JsonObjectBuilder newBuilder = JsonUtil.getProvider().createObjectBuilder();
                visitStructure(casted, newBuilder, result, newCurrentKeys);
                root.add(entry.getKey(), newBuilder);
            }
                if (object instanceof Pair<?, ?>) {
                    Pair<?, ?> pair = (Pair<?, ?>)object;
                    Map<?, ?> map = (Map<?, ?>)pair.getSecond();
                    @SuppressWarnings("unchecked")
                    Map<String, Object> casted = (Map<String, Object>)map;
                    JsonObjectBuilder newBuilder = JsonUtil.getProvider().createObjectBuilder();
                    visitStructure(casted, newBuilder, result, newCurrentKeys);
                    root.add(entry.getKey(), newBuilder);
                    newBuilder.add(entry.getKey(), result.get(join(newCurrentKeys)));
                }
                if (object instanceof Integer) {
                    root.add(entry.getKey(), result.get(join(newCurrentKeys)));
                }
        }
    }

    /**
     * @param list A {@link List} of {@link String}s.
     * @return The parts of {@code list} joined by the {@link TableToJsonSettings#getColumnNameSeparator()}.
     */
    private String join(final List<String> list) {
        //TODO use StringJoiner when Java8 is available.
        final StringBuilder sb = new StringBuilder();
        final String colSeparator = m_settings.getColumnNameSeparator();
        for (Iterator<String> it = list.iterator(); it.hasNext();) {
            String next = it.next();
            sb.append(next);
            if (it.hasNext()) {
                sb.append(colSeparator);
            }
        }
        return sb.toString();
    }
}
