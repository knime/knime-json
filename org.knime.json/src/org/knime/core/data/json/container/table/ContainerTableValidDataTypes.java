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
 *   Apr 5, 2018 (Tobias Urhaug): created
 */
package org.knime.core.data.json.container.table;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.DataTypeRegistry;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.time.localdate.LocalDateCell;
import org.knime.core.data.time.localdate.LocalDateCellFactory;
import org.knime.core.data.time.localdatetime.LocalDateTimeCell;
import org.knime.core.data.time.localdatetime.LocalDateTimeCellFactory;
import org.knime.core.data.time.localtime.LocalTimeCell;
import org.knime.core.data.time.localtime.LocalTimeCellFactory;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeCell;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeCellFactory;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.util.CheckUtils;

/**
 * Holds hard coded simple names for the valid primitive types of a {@link ContainerTableJsonSchema}
 * and is responsible for conversion from Strings to {@link DataType} and back.
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 * @since 3.6
 */
public class ContainerTableValidDataTypes {

    private static final String STRING_NAME = "string";
    private static final String DOUBLE_NAME = "double";
    private static final String INT_NAME = "int";
    private static final String LONG_NAME = "long";
    private static final String BOOLEAN_NAME = "boolean";
    private static final String LOCAL_TIME_NAME = "localtime";
    private static final String LOCAL_DATE_NAME = "localdate";
    private static final String LOCAL_DATE_TIME_NAME = "localdatetime";
    private static final String ZONED_DATE_TIME_NAME = "zoneddatetime";

    /**
     * Gets the {@link DataType} of the given string.
     *
     * @param dataType string representation of type to be converted
     * @return DataType object corresponding to the input data type
     * @throws InvalidSettingsException
     */
    public static DataType parse(final String dataType) throws InvalidSettingsException {
        switch (dataType) {
            case STRING_NAME : return StringCell.TYPE;
            case INT_NAME : return IntCell.TYPE;
            case DOUBLE_NAME : return DoubleCell.TYPE;
            case LONG_NAME : return LongCell.TYPE;
            case BOOLEAN_NAME : return BooleanCell.TYPE;
            case LOCAL_DATE_NAME : return LocalDateCellFactory.TYPE;
            case LOCAL_TIME_NAME : return LocalTimeCellFactory.TYPE;
            case LOCAL_DATE_TIME_NAME : return LocalDateTimeCellFactory.TYPE;
            case ZONED_DATE_TIME_NAME : return ZonedDateTimeCellFactory.TYPE;

            default : return getDataTypeByIdentifier(dataType);
        }
    }

    /**
     * Gets the data type of the given string if the type is present in the data type registry.
     * Comparison is done by the getName method of {@link DataType}.
     *
     * @param dataType the string representation of the data type
     * @throws InvalidSettingsException if the given data type is not supported
     */
    private static DataType getDataTypeByIdentifier(final String dataType) throws InvalidSettingsException {
        DataType result = null;
        for (DataType type : DataTypeRegistry.getInstance().availableDataTypes()) {
            final var identifier = type.getIdentifier();
            final var legacyName = type.getLegacyName();
            if (identifier.equals(dataType) || legacyName.equals(dataType)) {
                if (result != null) {
                    throw new InvalidSettingsException("Ambiguous return for value: \"" + dataType
                        + "\". Two or more data types use this name / identifier.");
                }
                result = type;
            }
        }
        if (result == null) {
            throw new InvalidSettingsException("Unsupported data type: \"" + dataType + "\"");
        }
        return result;
    }

    /**
     * Gets the string name of the given {@link DataType}.
     *
     * @param dataType type to be converted
     * @return string representation of the data type
     * @throws InvalidSettingsException if dataType is not valid
     */
    public static String parse(final DataType dataType) throws InvalidSettingsException {
        CheckUtils.checkArgumentNotNull(dataType);
        Class<? extends DataCell> cellClass = dataType.getCellClass();

        if (cellClass == null) {
            throw new InvalidSettingsException("The column type: \"" + dataType.getName() +"\" is not supported");
        }

        if (cellClass.equals(StringCell.class)) {
            return STRING_NAME;
        } else if (cellClass.equals(DoubleCell.class)) {
            return DOUBLE_NAME;
        } else if (cellClass.equals(IntCell.class)) {
            return INT_NAME;
        } else if (cellClass.equals(LongCell.class)) {
            return LONG_NAME;
        } else if (cellClass.equals(BooleanCell.class)) {
            return BOOLEAN_NAME;
        } else if (cellClass.equals(LocalDateCell.class)) {
            return LOCAL_DATE_NAME;
        } else if (cellClass.equals(LocalTimeCell.class)) {
            return LOCAL_TIME_NAME;
        } else if (cellClass.equals(LocalDateTimeCell.class)) {
            return LOCAL_DATE_TIME_NAME;
        } else if (cellClass.equals(ZonedDateTimeCell.class)) {
            return ZONED_DATE_TIME_NAME;
        } else {
            return dataType.getIdentifier();
        }
    }

}
