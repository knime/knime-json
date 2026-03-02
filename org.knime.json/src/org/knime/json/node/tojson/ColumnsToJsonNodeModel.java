package org.knime.json.node.tojson;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.knime.core.data.BooleanValue;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.LongValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.blob.BinaryObjectDataValue;
import org.knime.core.data.collection.CollectionDataValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.json.JSONValue;
import org.knime.core.data.vector.bytevector.ByteVectorValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.streamable.simple.SimpleStreamableFunctionNodeModel;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.util.Pair;
import org.knime.json.util.OutputType;
import org.knime.json.util.RootKeyType;

/**
 * This is the model implementation of ColumnsToJson. Converts contents of columns to JSON values rowwise.
 *
 * @author Gabor Bakos
 */
public class ColumnsToJsonNodeModel extends SimpleStreamableFunctionNodeModel {
    private final ColumnsToJsonSettings m_settings = new ColumnsToJsonSettings();

    /**
     * Constructor for the node model.
     */
    protected ColumnsToJsonNodeModel() {
        super();
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
        new ColumnsToJsonSettings().loadSettingsModel(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ColumnRearranger createColumnRearranger(final DataTableSpec spec) throws InvalidSettingsException {
        if (m_settings.isNotConfigured()) {
            throw new InvalidSettingsException("The node is not configured yet");
        }
        final var ret = new ColumnRearranger(spec);
        final int keyIndex = spec.findColumnIndex(m_settings.getKeyNameColumn());
        if (m_settings.getRootKeyType() == RootKeyType.DataBound) {
            CheckUtils.checkSetting(keyIndex >= 0,
                "Unknown databound key column name: " + m_settings.getKeyNameColumn());
            CheckUtils.checkSetting(spec.getColumnSpec(keyIndex).getType().isCompatible(StringValue.class),
                "Wrong databound key column: " + m_settings.getKeyNameColumn());
        }
        ret.append(new ColumnsToJsonCellFactory(spec, m_settings));
        if (m_settings.isRemoveSourceColumns()) {
            final Set<String> colsToRemove = new HashSet<>();
            if (m_settings.getRootKeyType() == RootKeyType.DataBound) {
                colsToRemove.add(m_settings.getKeyNameColumn());
            }
            // collect all columns to be removed
            final var colsKeys = m_settings.getDataBoundKeyColumns();
            Collections.addAll(colsToRemove, colsKeys);
            final var colsIncludes = m_settings.getDataBoundColumnsAutoConfiguration().applyTo(spec).getIncludes();
            Collections.addAll(colsToRemove, colsIncludes);
            // apply all columns to be removed
            for (String col : colsToRemove) {
                ret.remove(col);
            }
        }
        return ret;
    }

    /**
     * @param columnSpec
     * @return A pair of {@link OutputType} and depth of nested collections.
     * @throws InvalidSettingsException when not supported type was selected.
     */
    protected static Pair<OutputType, Integer> outputType(final DataColumnSpec columnSpec) throws InvalidSettingsException {
        try {
            return outputType(columnSpec.getType(), 0);
        } catch (UnsupportedOperationException e) {
            throw new InvalidSettingsException(
                columnSpec.getName() + " column has not supported type: " + columnSpec.getType(), e);
        }
    }

    /**
     * Collects the output type with the depth of {@link CollectionDataValue}s.
     *
     * @param type A possibly {@link CollectionDataValue}'s {@link DataType}.
     * @param depth The previous depth of nesting.
     * @return The {@link Pair} of {@link OutputType} and the depth of nesting.
     * @throws UnsupportedOperationException Not supported column type.
     */
    private static Pair<OutputType, Integer> outputType(final DataType type, final int depth) {
        if (type.isCollectionType() && !type.isMissingValueType()) {
            // make sure that the element type is non-null here
            return outputType(type.getCollectionElementType(), depth + 1);
        }
        final var oType = findCompatibleOutputType(type);
        if (type.isMissingValueType() || oType == null) {
            throw new UnsupportedOperationException("Not supported type: " + type);
        }
        return Pair.create(oType, depth);
    }

    private static OutputType findCompatibleOutputType(final DataType type) {
        OutputType oType = null;
        if (type.isCompatible(BooleanValue.class)) {
            oType = OutputType.Boolean;
        } else if (type.isCompatible(IntValue.class)) {
            oType = OutputType.Integer;
        } else if (type.isCompatible(LongValue.class)) {
            oType = OutputType.Long;
        } else if (type.isCompatible(DoubleValue.class)) {
            oType = OutputType.Double;
        } else if (type.isCompatible(JSONValue.class)) {
            oType = OutputType.Json;
        } else if (type.isCompatible(StringValue.class)) {
            oType = OutputType.String;
        } else if (type.isCompatible(ByteVectorValue.class)) {
            oType = OutputType.Base64;
        } else if (type.isCompatible(BinaryObjectDataValue.class)) {
            oType = OutputType.Base64;
        }
        return oType;
    }

}
