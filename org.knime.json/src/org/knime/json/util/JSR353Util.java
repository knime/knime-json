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
 *   20 Dec 2014 (Gabor): created
 */
package org.knime.json.util;

import java.io.IOException;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;

import org.knime.core.data.BooleanValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.LongValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.blob.BinaryObjectDataValue;
import org.knime.core.data.collection.CollectionDataValue;
import org.knime.core.data.json.JSONValue;
import org.knime.core.data.vector.bytevector.ByteVectorValue;
import org.knime.core.node.NodeLogger;
import org.knime.json.node.jsonpath.util.JsonPathUtils;

import com.fasterxml.jackson.core.Base64Variants;

/**
 * Some helper methods to work with JSR-353 ({@code javax.json}) values.
 *
 * @author Gabor Bakos
 */
public class JSR353Util {

    /**
     * Hidden constructor.
     */
    private JSR353Util() {
    }

    /**
     * Updates the {@code builder} {@link JsonObjectBuilder} with the transformed value of {@code cell} and {@code key}
     * key.
     *
     * @param key The new key to store the (transformed) value of {@code cell}.
     * @param cell The value to transform.
     * @param builder A {@link JsonObjectBuilder}.
     * @return The input {@code builder}.
     * @throws IOException When the there was a problem reading {@link BinaryObjectDataValue}s.
     */
    public static JsonObjectBuilder fromCell(final String key, final DataCell cell, final JsonObjectBuilder builder)
        throws IOException {
        if (cell.isMissing()) {
            builder.addNull(key);
        } else if (cell instanceof BooleanValue) {
            BooleanValue bv = (BooleanValue)cell;
            builder.add(key, bv.getBooleanValue());
        } else if (cell instanceof LongValue) {
            LongValue l = (LongValue)cell;
            builder.add(key, l.getLongValue());
        } else if (cell instanceof DoubleValue) {
            DoubleValue dv = (DoubleValue)cell;
            builder.add(key, dv.getDoubleValue());
        } else if (cell instanceof JSONValue) {
            JSONValue jv = (JSONValue)cell;
            builder.add(key, jv.getJsonValue());
        } else if (cell instanceof ByteVectorValue) {
            ByteVectorValue bvv = (ByteVectorValue)cell;
            builder.add(key, Base64Variants.getDefaultVariant().encode(JsonPathUtils.toBytes(bvv)));
        } else if (cell instanceof BinaryObjectDataValue) {
            BinaryObjectDataValue bodv = (BinaryObjectDataValue)cell;
            builder.add(key, Base64Variants.getDefaultVariant().encode(JsonPathUtils.toBytes(bodv)));
        } else if (cell instanceof StringValue) {
            StringValue sv = (StringValue)cell;
            builder.add(key, sv.getStringValue());
        } else if (cell instanceof CollectionDataValue) {
            CollectionDataValue cdv = (CollectionDataValue)cell;
            builder.add(key, fromCollectionCell(cdv));
        } else {
            builder.addNull(key);
        }
        return builder;
    }

    /**
     * @param cdv A {@link CollectionDataValue} to convert.
     * @return The {@link JsonArrayBuilder} with the transformed content of {@code cdv}.
     */
    public static JsonArrayBuilder fromCollectionCell(final CollectionDataValue cdv) {
        JsonArrayBuilder ret = Json.createArrayBuilder();
        for (DataCell cell : cdv) {
            try {
                addToArrayFromCell(ret, cell);
            } catch (IOException e) {
                NodeLogger.getLogger(JSR353Util.class).warn("Failed to read binary object value, replacing with null.",
                    e);
                ret.addNull();
            }
        }
        return ret;
    }

    /**
     * Adds the {@code cell} transformed to {@code array}.
     *
     * @param array A {@link JsonArrayBuilder}.
     * @param cell The {@link DataCell} to transform.
     * @throws IOException When cannot read binary object data values.
     */
    public static void addToArrayFromCell(final JsonArrayBuilder array, final DataCell cell) throws IOException {
        if (cell.isMissing()) {
            array.addNull();
        } else if (cell instanceof CollectionDataValue) {
            CollectionDataValue inner = (CollectionDataValue)cell;
            array.add(fromCollectionCell(inner));
        } else if (cell instanceof LongValue) {
            LongValue lv = (LongValue)cell;
            array.add(lv.getLongValue());
        } else if (cell instanceof DoubleValue) {
            DoubleValue dv = (DoubleValue)cell;
            array.add(dv.getDoubleValue());
        } else if (cell instanceof JSONValue) {
            JSONValue jv = (JSONValue)cell;
            array.add(jv.getJsonValue());
        } else if (cell instanceof BooleanValue) {
            BooleanValue bv = (BooleanValue)cell;
            array.add(bv.getBooleanValue());
        } else if (cell instanceof ByteVectorValue) {
            ByteVectorValue bvv = (ByteVectorValue)cell;
            array.add(Base64Variants.getDefaultVariant().encode(JsonPathUtils.toBytes(bvv)));
        } else if (cell instanceof BinaryObjectDataValue) {
            BinaryObjectDataValue bodv = (BinaryObjectDataValue)cell;
            array.add(Base64Variants.getDefaultVariant().encode(JsonPathUtils.toBytes(bodv)));
        } else if (cell instanceof StringValue) {
            StringValue sv = (StringValue)cell;
            array.add(sv.getStringValue());
        } else {
            array.addNull();
        }
    }
}
