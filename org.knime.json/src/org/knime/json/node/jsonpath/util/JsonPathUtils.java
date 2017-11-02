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
 *   17 Dec. 2014 (Gabor): created
 */
package org.knime.json.node.jsonpath.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.io.IOUtils;
import org.knime.core.data.BooleanValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.LongValue;
import org.knime.core.data.MissingCell;
import org.knime.core.data.StringValue;
import org.knime.core.data.blob.BinaryObjectCellFactory;
import org.knime.core.data.blob.BinaryObjectDataValue;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.ListDataValue;
import org.knime.core.data.collection.SetDataValue;
import org.knime.core.data.def.BooleanCell.BooleanCellFactory;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.json.JSONCellFactory;
import org.knime.core.data.json.JSONValue;
import org.knime.core.data.json.JacksonConversions;
import org.knime.core.data.vector.bytevector.ByteVectorValue;
import org.knime.core.node.util.CheckUtils;
import org.knime.json.node.jsonpath.JsonPathUtil;
import org.knime.json.util.OutputType;

import com.fasterxml.jackson.core.Base64Variants;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BinaryNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jackson.JacksonUtils;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;

/**
 * Utility methods for {@link JsonPath}.
 *
 * @author Gabor Bakos
 */
public class JsonPathUtils {

    /**
     * Hide constructor.
     */
    private JsonPathUtils() {
    }

    /**
     * Disallows long type inference, swallows the warning about not fitting the content.
     * @param jacksonObject A {@link JsonNode}.
     * @return The {@link OutputKind} that can describe {@code jacksonObject}.
     * @deprecated Use the {@link #kindOfJackson(JsonNode, AtomicReference)} to allow warnings.
     */
    @Deprecated
    public static OutputKind kindOfJackson(final JsonNode jacksonObject) {
        return kindOfJackson(jacksonObject, new AtomicReference<>());
    }

    /**
     * @param jacksonObject A {@link JsonNode}.
     * @param warning Possible warning when long should be stored as int.
     * @return The {@link OutputKind} that can describe {@code jacksonObject}.
     */
    public static OutputKind kindOfJackson(final JsonNode jacksonObject, final AtomicReference<String> warning) {
        if (jacksonObject == null) {
            return new OutputKind(true, OutputType.Json);
        }
        if (jacksonObject.isArray()) {
            OutputType type = null;
            for (JsonNode jsonNode : jacksonObject) {
                OutputKind kindOfJackson = kindOfJackson(jsonNode, warning);
                type = kindOfJackson.isSingle() ? commonRepresentation(type, kindOfJackson.getType()) : OutputType.Json;
            }
            return new OutputKind(false, type);
        }
        if (jacksonObject.isObject()) {
            return new OutputKind(true, OutputType.Json);
        }
        if (jacksonObject.isIntegralNumber()) {
            final long l = Long.parseLong(jacksonObject.toString());
            return new OutputKind(true, fitsInt(l) ? OutputType.Integer : OutputType.Long);
        }
        if (jacksonObject.isFloatingPointNumber()) {
            return new OutputKind(true, OutputType.Double);
        }
        if (jacksonObject.isBinary()) {
            return new OutputKind(true, OutputType.Base64);
        }
        if (jacksonObject.isTextual()) {
            return new OutputKind(true, OutputType.String);
        }
        if (jacksonObject.isBoolean()) {
            return new OutputKind(true, OutputType.Boolean);
        }
        assert jacksonObject.isMissingNode() || jacksonObject.isNull() : jacksonObject.getNodeType();
        return new OutputKind(true, null);
    }

    /**
     * @param l
     * @return
     */
    private static boolean fitsInt(final long l) {
        return (int)l == l;
    }

    /**
     * @param origType An {@link OutputType} that we have seen previously. (Can be {@code null}.)
     * @param newType The new {@link OutputType}. (Can be {@code null}.)
     * @return An {@link OutputType} compatible with both. (Can be {@code null}.)
     */
    public static OutputType commonRepresentation(final OutputType origType, final OutputType newType) {
        if (origType == null || origType == newType) {
            return newType;
        }
        if (newType == null) {
            return origType;
        }
        switch (newType) {
            case Base64:
                switch (origType) {
                    case Base64://intentional fall-through
                    case String://intentional fall-through
                    case Json://intentional fall-through
                        return origType;
                    case Boolean://intentional fall-through
                    case Integer://intentional fall-through
                    case Long://intentional fall-through
                    case Double://intentional fall-through
                        return OutputType.Json;
                    default:
                        throw new IllegalStateException("Unknown type: " + origType);
                }
            case Boolean:
                switch (origType) {
                    case Base64://intentional fall-through
                    case String://intentional fall-through
                    case Json://intentional fall-through
                    case Integer://intentional fall-through
                    case Long://intentional fall-through
                    case Double://intentional fall-through
                        return OutputType.Json;
                    default:
                        throw new IllegalStateException("Unknown type: " + origType);
                }
            case Integer:
                switch (origType) {
                    case Integer://intentional fall-through
                    case Long://intentional fall-through
                    case Double://intentional fall-through
                        return origType;
                    case Boolean://intentional fall-through
                    case Base64://intentional fall-through
                    case String://intentional fall-through
                    case Json://intentional fall-through
                        return OutputType.Json;
                    default:
                        throw new IllegalStateException("Unknown type: " + origType);
                }
            case Long:
                switch (origType) {
                    case Integer:
                        return OutputType.Long;
                    case Long://intentional fall-through
                    case Double://intentional fall-through
                        return origType;
                    case Boolean://intentional fall-through
                    case Base64://intentional fall-through
                    case String://intentional fall-through
                    case Json://intentional fall-through
                        return OutputType.Json;
                    default:
                        throw new IllegalStateException("Unknown type: " + origType);
                }
            case Json:
                return OutputType.Json;
            case Double:
                switch (origType) {
                    case Integer://intentional fall-through
                    case Long://intentional fall-through
                    case Double://intentional fall-through
                        return newType;
                    case Boolean://intentional fall-through
                    case Base64://intentional fall-through
                    case String://intentional fall-through
                    case Json://intentional fall-through
                        return OutputType.Json;
                    default:
                        throw new IllegalStateException("Unknown type: " + origType);
                }
            case String:
                switch (origType) {
                    case String://intentional fall-through
                    case Json://intentional fall-through
                        return origType;
                    case Base64://intentional fall-through
                    case Boolean://intentional fall-through
                    case Integer://intentional fall-through
                    case Long://intentional fall-through
                    case Double://intentional fall-through
                        return OutputType.Json;
                    default:
                        throw new IllegalStateException("Unknown type: " + origType);
                }
            default:
                throw new IllegalStateException("Unknown type: " + origType);
        }
    }

    /**
     *
     * @param object An {@link Object} to convert.
     * @param returnType The expected {@link OutputKind}.
     * @param config JsonPath {@link Configuration}.
     * @param conv {@link JacksonConversions}.
     * @return Converted {@code object} to {@link DataCell}. It should be compatible with {@code returnType}.
     */
    public static DataCell convertObjectToReturnType(final Object object, final OutputKind returnType,
        final Configuration config, final JacksonConversions conv) {
        if (returnType.isSingle()) {
            return convertObjectToReturnType(object, returnType.getType(), config, conv);
        }
        List<DataCell> cells = new ArrayList<>();
        JsonNode jackson = JsonPathUtil.toJackson(JacksonUtils.nodeFactory(), object);
        if (jackson.isArray()) {
            if (object instanceof Iterable<?>) {
                Iterable<?> iterable = (Iterable<?>)object;
                for (Object o : iterable) {
                    cells.add(convertObjectToReturnType(o, returnType.getType(), config, conv));
                }
            } else {
                //Probably this never happens.
                for (JsonNode jsonNode : jackson) {
                    cells.add(convertObjectToReturnType(jsonNode, returnType.getType(), config, conv));
                }
            }
        } else {
            cells.add(convertObjectToReturnType(object, returnType.getType(), config, conv));
        }
        return CollectionCellFactory.createListCell(cells);
    }

    /**
     *
     * @param object An {@link Object} to convert.
     * @param returnType The expected {@link OutputKind}.
     * @param config JsonPath {@link Configuration}.
     * @param conv {@link JacksonConversions}.
     * @return Converted {@code object} to {@link DataCell}. It should be compatible with {@code returnType}.
     */
    public static DataCell convertObjectToReturnTypeWithoutNestedObjects(final Object object, final OutputKind returnType,
        final Configuration config, final JacksonConversions conv) {
        if (returnType.isSingle()) {
            return convertObjectToReturnType(object, returnType.getType(), config, conv);
        }
        List<DataCell> cells = new ArrayList<>();
        JsonNode jackson = JsonPathUtil.toJackson(JacksonUtils.nodeFactory(), object);
        if (jackson.isArray()) {
            if (object instanceof Iterable<?>) {
                Iterable<?> iterable = (Iterable<?>)object;
                for (Object o : iterable) {
                    try {
                        if (!(o instanceof ObjectNode) || returnType.getType() == OutputType.Json) {
                            cells.add(convertObjectToReturnType(o, returnType.getType(), config, conv));
                        }
                    } catch (RuntimeException e) {
                        continue;
                    }
                }
            } else {
                //Probably this never happens.
                for (JsonNode jsonNode : jackson) {
                    try {
                        cells.add(convertObjectToReturnType(jsonNode, returnType.getType(), config, conv));
                    } catch (RuntimeException e) {
                        continue;
                    }
                }
            }
        } else {
            cells.add(convertObjectToReturnType(object, returnType.getType(), config, conv));
        }
        return CollectionCellFactory.createListCell(cells);
    }

    /**
     * Converts {@code object} to a non-collection {@link DataCell}. (Generates no warnings.)
     *
     * @param object An {@link Object} to convert.
     * @param returnType The expected {@link OutputType}.
     * @param config JsonPath {@link Configuration}.
     * @param conv {@link JacksonConversions}.
     * @return Converted {@code object} to {@link DataCell}. It should be compatible with {@code returnType}.
     * @deprecated Use {@link #convertObjectToReturnType(Object, OutputType, Configuration, JacksonConversions, Runnable)}
     */
    @Deprecated
    public static DataCell convertObjectToReturnType(final Object object, final OutputType returnType,
        final Configuration config, final JacksonConversions conv) {
        return convertObjectToReturnType(object, returnType, config, conv, () -> {});
    }

    /**
     * Converts {@code object} to a non-collection {@link DataCell}.
     *
     * @param object An {@link Object} to convert.
     * @param returnType The expected {@link OutputType}.
     * @param config JsonPath {@link Configuration}.
     * @param conv {@link JacksonConversions}.
     * @param setWarning A {@link Runnable} that should set the warning for the wrong values.
     * @return Converted {@code object} to {@link DataCell}. It should be compatible with {@code returnType}.
     * @since 3.2
     */
    public static DataCell convertObjectToReturnType(final Object object, final OutputType returnType,
        final Configuration config, final JacksonConversions conv, final Runnable setWarning) {
        try {
            switch (returnType) {
                case Boolean:
                    Boolean bool = config.mappingProvider().map(object, Boolean.class, config);
                    if (bool == null) {
                        return BooleanCellFactory.create(object.toString());
                    }
                    return BooleanCellFactory.create(bool.booleanValue());
                case Integer:
                    Integer integer = config.mappingProvider().map(object, Integer.class, config);
                    if (integer == null) {
                        return new IntCell(Integer.parseInt(object.toString()));
                    }
                    return new IntCell(integer.intValue());
                case Long:
                    Long longVal = config.mappingProvider().map(object, Long.class, config);
                    if (longVal == null) {
                        return new LongCell(Long.parseLong(object.toString()));
                    }
                    return new LongCell(longVal.longValue());
                case Json:
                    return asJson(object, conv);
                case Double:
                    Double d = config.mappingProvider().map(object, Double.class, config);
                    if (d == null) {
                        return DataType.getMissingCell();
                    }
                    return new DoubleCell(d.doubleValue());
                case String:
                    return object == null ? DataType.getMissingCell() : new StringCell(config.mappingProvider().map(
                        object, String.class, config));
                case Base64:
                    if (object == null) {
                        return DataType.getMissingCell();
                    }
                    byte[] arr;
                    if (object instanceof byte[]) {
                        arr = (byte[])object;
                    } else if (object instanceof String) {
                        String str = (String)object;
                        arr = Base64Variants.getDefaultVariant().decode(str);
                    } else if (object instanceof BinaryNode) {
                        BinaryNode node = (BinaryNode)object;
                        arr = node.binaryValue();
                    } else {
                        throw new IllegalArgumentException("Unkown binary type: " + object.getClass());
                    }
                    BinaryObjectCellFactory cellFactory = new BinaryObjectCellFactory();
                    return cellFactory.create(arr);
                default:
                    throw new UnsupportedOperationException("Unsupported return type: " + returnType);
            }
        } catch (RuntimeException | IOException e) {
            checkLongProblem(returnType, object, setWarning);
            return new MissingCell(e.getMessage());
        }
    }

    /**
     * @param object The {@link Object} to convert.
     * @param conv A {@link JacksonConversions}.
     * @return The {@code object} as a JSON {@link DataCell}.
     */
    public static DataCell asJson(final Object object, final JacksonConversions conv) {
        if (object instanceof JsonNode) {
            return JSONCellFactory.create(conv.toJSR353((JsonNode)object));
        }
        try {
            return JSONCellFactory.create(conv.toJSR353(JsonPathUtil.toJackson(JacksonUtils.nodeFactory(), object)));
        } catch (RuntimeException e) {
            return new MissingCell(e.getMessage());
        }
    }

    /**
     * @param bvv A {@link ByteVectorValue}.
     * @return As a byte array.
     * @throws IllegalArgumentException When the array cannot be created.
     * @throws OutOfMemoryError When the array cannot be created.
     */
    public static byte[] toBytes(final ByteVectorValue bvv) {
        if (bvv.length() > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Too large byte-vector: " + bvv.length());
        }
        byte[] ret = new byte[(int)bvv.length()];
        for (int i = ret.length; i-- > 0;) {
            int v = bvv.get(i);
            v &= 0xff;
            ret[i] = (byte)v;
        }
        return ret;
    }


    /**
     * @param bodv A {@link BinaryObjectDataValue}.
     * @return As a byte array.
     * @throws IOException When reading the binary value.
     * @throws IllegalArgumentException When the array cannot be created.
     * @throws OutOfMemoryError When the array cannot be created.
     */
    public static byte[] toBytes(final BinaryObjectDataValue bodv) throws IOException {
        CheckUtils.checkArgument(bodv.length() <= Integer.MAX_VALUE, "Too large byte-vector: " + bodv.length());
        try (InputStream is = bodv.openInputStream()) {
            byte[] ret = new byte[(int)bodv.length()];
            IOUtils.readFully(is, ret);
            return ret;
        }
    }

    /**
     * @return The (unmodifiable) {@link List} of supported input {@link DataValue} classes.
     */
    public static List<Class<? extends DataValue>> supportedInputDataValues() {
        return Collections.unmodifiableList(Arrays.asList(supportedInputDataValuesAsArray()));
    }

    @SafeVarargs
    private static <T> Class<? extends T>[] asArray(final Class<? extends T>... classes) {
        return classes;
    }

    /**
     * @return The array of supported input {@link DataValue} classes.
     */
    public static Class<? extends DataValue>[] supportedInputDataValuesAsArray() {
        return asArray(ListDataValue.class, SetDataValue.class, LongValue.class, DoubleValue.class, BooleanValue.class, ByteVectorValue.class, BinaryObjectDataValue.class, JSONValue.class, StringValue.class);
    }

    /**
     * @return The (unmodifiable) {@link List} of supported output {@link DataValue} classes.
     */
    public static List<Class<? extends DataValue>> supportedOutputDataValues() {
        return Collections.unmodifiableList(Arrays.asList(supportedOutputValuesAsArray()));
    }

    /**
     * @return The array of supported output {@link DataValue} classes.
     */
    public static Class<? extends DataValue>[] supportedOutputValuesAsArray() {
        return asArray(LongValue.class, DoubleValue.class, BooleanValue.class, BinaryObjectDataValue.class, JSONValue.class, StringValue.class);
    }

    /**
     * Checks whether the problem is caused by wrong casting from long to int, calls {@code setWarning} in that case.
     *
     * @param returnType The expected return type.
     * @param object The actual result.
     * @param setWarning Thing to do when we are out of range for an integer, but got a long for the expected integer.
     * @since 3.2
     */
    public static void checkLongProblem(final OutputType returnType, final Object object, final Runnable setWarning) {
        if (object instanceof Long) {
            final Long l = (Long)object;
            if (returnType == OutputType.Integer && Math.abs(l) > Integer.MAX_VALUE
                && l != Integer.MIN_VALUE) {
                setWarning.run();
            }
        }
    }
}
