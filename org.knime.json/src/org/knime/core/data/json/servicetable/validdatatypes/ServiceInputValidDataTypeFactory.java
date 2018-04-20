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
package org.knime.core.data.json.servicetable.validdatatypes;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.time.localdate.LocalDateCell;
import org.knime.core.data.time.localdatetime.LocalDateTimeCell;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeCell;
import org.knime.core.node.InvalidSettingsException;

/**
 * Factory class for valid service input types.
 *
 * @author Tobias Urhaug
 */
public abstract class ServiceInputValidDataTypeFactory {

    /**
     * Gets the DataType
     *
     * @param dataType type that should be converted
     * @return ServiceInputValidDataType object corresponding to the input data type
     * @throws InvalidSettingsException
     */
    public static ServiceInputValidDataType of(final String dataType) throws InvalidSettingsException {
        switch (dataType) {
            case "string" : return ServiceInputValidDataType.STRING;
            case "int" : return ServiceInputValidDataType.INTEGER;
            case "double" : return ServiceInputValidDataType.DOUBLE;
            case "long" : return ServiceInputValidDataType.LONG;
            case "boolean" : return ServiceInputValidDataType.BOOLEAN;
            case "localdate" : return ServiceInputValidDataType.LOCAL_DATE;
            case "localdatetime" : return ServiceInputValidDataType.LOCAL_DATE_TIME;
            case "zoneddatetime" : return ServiceInputValidDataType.ZONED_DATE_TIME;

            default : throw new InvalidSettingsException("Unsupported data type: \"" + dataType + "\"");
        }
    }

    /**
     * Factory method for creating a ServiceInputValidDataType object of
     * the given data type.
     *
     * @param dataType type that should be converted
     * @return ServiceInputValidDataType object corresponding to the input data type
     * @throws InvalidSettingsException
     */
    public static ServiceInputValidDataType of(final DataType dataType) throws InvalidSettingsException {
        Class<? extends DataCell> dataTypeCellClass = dataType.getCellClass();
        assert dataTypeCellClass != null;

        if (StringCell.class.equals(dataTypeCellClass)) {
            return ServiceInputValidDataType.STRING;
        } else if (IntCell.class.equals(dataTypeCellClass)) {
            return ServiceInputValidDataType.INTEGER;
        } else if (DoubleCell.class.equals(dataTypeCellClass)) {
            return ServiceInputValidDataType.DOUBLE;
        } else if (LongCell.class.equals(dataTypeCellClass)) {
            return ServiceInputValidDataType.LONG;
        } else if (BooleanCell.class.equals(dataTypeCellClass)) {
            return ServiceInputValidDataType.BOOLEAN;
        } else if (LocalDateCell.class.equals(dataTypeCellClass)) {
            return ServiceInputValidDataType.LOCAL_DATE;
        } else if (LocalDateTimeCell.class.equals(dataTypeCellClass)) {
            return ServiceInputValidDataType.LOCAL_DATE_TIME;
        } else if (ZonedDateTimeCell.class.equals(dataTypeCellClass)) {
            return ServiceInputValidDataType.ZONED_DATE_TIME;
        } else {
            throw new InvalidSettingsException("Unsupported data type: \"" + dataType + "\"");
        }
    }

}
