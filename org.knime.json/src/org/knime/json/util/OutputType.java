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
 *   8 Nov. 2014 (Gabor): created
 */
package org.knime.json.util;

import javax.swing.Icon;

import org.knime.core.data.BooleanValue;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.LongValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.blob.BinaryObjectDataCell;
import org.knime.core.data.blob.BinaryObjectDataValue;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.json.JSONCell;
import org.knime.core.data.json.JSONValue;

/**
 * Possible output types for JSONPath and JSONPointer. (JSONPath can return multiple values, so there should be an
 * option to return collections too.)
 *
 * @author Gabor Bakos
 */
public enum OutputType implements StringValue {
    /** Text */
    String,
    /** Object */
    Json,
    /** Base64 content */
    Base64,
    /** Logical */
    Boolean,
    /** Integral */
    Integer,
    /** Real */
    Double,
    /**
     * 64 bit signed integral values
     * @since 3.2
     */
    Long;
    /**
     * {@inheritDoc}
     */
    @Override
    public String getStringValue() {
        switch (this) {
            case Boolean:
                return "Boolean (Boolean cell type)";
            case Integer:
                return "Number (Integer cell type)";
            case Double:
                return "Number (Double cell type)";
            case String:
                return "String (String cell type)";
            case Json:
                return "JSON (JSON cell type)";
            case Base64:
                return "Base64 (Base64 object cell type)";
            case Long:
                return "Number (Long cell type)";
            default:
                throw new IllegalStateException("Unknown enum value: " + this);
        }
    }
    /**
     * @return The {@link DataType} to use in KNIME for this kind of output.
     */
    public DataType getDataType() {
        switch (this) {
            case Base64:
                return BinaryObjectDataCell.TYPE;
            case Boolean:
                return BooleanCell.TYPE;
            case Integer:
                return IntCell.TYPE;
            case Double:
                return DoubleCell.TYPE;
            case String:
                return StringCell.TYPE;
            case Json:
                return JSONCell.TYPE;
            case Long:
                return LongCell.TYPE;
            default:
                throw new IllegalStateException("Unknown enum value: " + this);
        }
    }
    /**
     * @return The visual representation of the type.
     */
    public Icon getIcon() {
        switch (this) {
            case Base64:
                return BinaryObjectDataValue.UTILITY.getIcon();
            case Json:
                return JSONValue.UTILITY.getIcon();
            case Boolean:
                return BooleanValue.UTILITY.getIcon();
            case Integer:
                return IntValue.UTILITY.getIcon();
            case Double:
                return DoubleValue.UTILITY.getIcon();
            case String:
                return StringValue.UTILITY.getIcon();
            case Long:
                return LongValue.UTILITY.getIcon();
            default:
                throw new UnsupportedOperationException("Unknown type: " + this);
        }
    }
}
