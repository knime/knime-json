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
 */

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

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettings(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_settings.loadSettingsModel(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        new ColumnsToJsonSettings().loadSettingsModel(settings);
    }

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
    protected static Pair<OutputType, Integer> outputType(final DataColumnSpec columnSpec)
        throws InvalidSettingsException {
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
