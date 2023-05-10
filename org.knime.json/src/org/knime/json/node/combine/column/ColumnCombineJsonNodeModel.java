package org.knime.json.node.combine.column;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.json.JSONCell;
import org.knime.core.data.json.JSONCellFactory;
import org.knime.core.data.json.JSONValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.streamable.simple.SimpleStreamableFunctionNodeModel;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.util.filter.InputFilter;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterConfiguration;
import org.knime.core.util.JsonUtil;
import org.knime.json.util.JSR353Util;
import org.knime.json.util.RootKeyType;

import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;

/**
 * This is the model implementation of ColumnCombineJson. Combines multiple JSON columns to a single.
 *
 * @author Gabor Bakos
 */
class ColumnCombineJsonNodeModel extends SimpleStreamableFunctionNodeModel {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(ColumnCombineJsonNodeModel.class);

    private final ColumnCombineJsonSettings m_settings = new ColumnCombineJsonSettings();

    /**
     * Constructor for the node model.
     */
    protected ColumnCombineJsonNodeModel() {
        super();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ColumnRearranger createColumnRearranger(final DataTableSpec spec) throws InvalidSettingsException {
        ColumnRearranger ret = new ColumnRearranger(spec);
        final int keyIndex = spec.findColumnIndex(m_settings.getRootKeyNameColumn());
        if (m_settings.getRootKeyType() == RootKeyType.DataBound) {
            CheckUtils.checkSetting(keyIndex >= 0,
                "Unknown databound key column name: " + m_settings.getRootKeyNameColumn());
            CheckUtils.checkSetting(spec.getColumnSpec(keyIndex).getType().isCompatible(StringValue.class),
                "Wrong databound key column: " + m_settings.getRootKeyNameColumn());
        }
        ret.append(new SingleCellFactory(new DataColumnSpecCreator(DataTableSpec.getUniqueColumnName(spec,
            m_settings.getNewColumn()), JSONCell.TYPE).createSpec()) {

            //            private final String[] values = m_settings.getValues(), m_keyNames = m_settings.getKeyNames();
            //            private final String[] m_dataBoundKeyNames = m_settings.getDataBoundKeyNames();
            private final String[] m_arrayColumns = m_settings.getFilterConfiguration().applyTo(spec).getIncludes();

            private final int[] m_arrayColumnIndices = new int[m_arrayColumns.length]/*, m_dataBoundValueIndices = new int[m_dataBoundKeyNames.length]*/;
            {
                for (int i = m_arrayColumns.length; i-- > 0;) {
                    m_arrayColumnIndices[i] = spec.findColumnIndex(m_arrayColumns[i]);
                }
                //                String[] colNames = m_settings.getDataBoundValues();
                //                for (int i = m_dataBoundKeyNames.length; i-->0;) {
                //                    m_dataBoundValueIndices[i] = spec.findColumnIndex(colNames[i]);
                //                }
            }

            @Override
            public DataCell getCell(final DataRow row) {
                JsonObjectBuilder builder = JsonUtil.getProvider().createObjectBuilder();
                JsonArrayBuilder array = JsonUtil.getProvider().createArrayBuilder();
                for (int idx : m_arrayColumnIndices) {
                    DataCell cell = row.getCell(idx);
                    try {
                        JSR353Util.addToArrayFromCell(array, cell);
                    } catch (IOException e) {
                        LOGGER.warn("Cannot load binary object (row: " + row.getKey() + ")", e);
                    }
                }
                switch (m_settings.getRootKeyType()) {
                    case Unnamed:
                        return JSONCellFactory.create(array.build());
                    case Constant:
                        builder.add(m_settings.getRootKeyName(), array);
                        break;
                    case DataBound:
                        DataCell cell = row.getCell(keyIndex);
                        if (cell instanceof JSONValue) {
                            JSONValue jv = (JSONValue)cell;
                            if (jv.getJsonValue().getValueType() == JsonValue.ValueType.STRING) {
                                builder.add(((JsonString)jv.getJsonValue()).getString(), array);
                                break;
                            }
                        }
                        builder.add(cell.toString(), array);
                        break;
                    default:
                        throw new UnsupportedOperationException("Unknown root type: " + m_settings.getRootKeyType());
                }
                //                for (int i = 0; i < m_dataBoundKeyNames.length; ++i) {
                //                    JSR353Util.fromCell(m_dataBoundKeyNames[i], row.getCell(m_dataBoundValueIndices[i]), builder);
                //                }
                //                for (int i = 0; i < m_keyNames.length; ++i) {
                //                    builder.add(m_keyNames[i], values[i]);
                //                }
                return JSONCellFactory.create(builder.build());
            }
        });
        if (m_settings.getRemoveSourceColumns()) {
            Set<String> toRemove = new HashSet<>();
            if (m_settings.getRootKeyType() == RootKeyType.DataBound) {
                toRemove.add(m_settings.getRootKeyNameColumn());
            }
            for (String col : m_settings.getFilterConfiguration().applyTo(spec).getIncludes()) {
                toRemove.add(col);
            }
            for (String col : toRemove) {
                ret.remove(col);
            }
        }
        return ret;
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
        new ColumnCombineJsonSettings().loadSettingsModel(settings);
    }

    /**
     * A new configuration to store the settings.
     *
     * @return filter configuration
     */
    static final DataColumnSpecFilterConfiguration createDCSFilterConfiguration() {
        return new DataColumnSpecFilterConfiguration("column_filter", new InputFilter<DataColumnSpec>() {

            @Override
            public boolean include(final DataColumnSpec name) {
                return name.getType().isCompatible(JSONValue.class);
            }
        });
    }
}
