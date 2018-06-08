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
 *   Apr 9, 2018 (Tobias Urhaug): created
 */
package org.knime.core.data.json.servicetable;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;

import org.knime.core.node.util.CheckUtils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Representation of a column spec containing a name and type.
 * Can be serialized/deserialized to/from json with jackson.
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 * @since 3.6
 */
public class ServiceTableColumnSpec {

    private final String m_columnName;
    private final String m_columnType;

    /**
     * Constructs a Service Column Spec from the given name and type.
     *
     * @param columnName name of the column
     * @param columnType type of the column
     */
    public ServiceTableColumnSpec(final String columnName, final String columnType) {
        m_columnName = columnName;
        m_columnType = columnType;
    }

    /**
     * Constructs an instance from the given spec.
     *
     * @param columnSpec spec to be created
     */
    @JsonCreator
    private ServiceTableColumnSpec(final Map<String, String> columnSpec) {
        CheckUtils.checkArgumentNotNull(columnSpec);
        String columnName = null;
        String columnType = null;
        for (Entry<String,String> spec : columnSpec.entrySet()) {
            columnName = spec.getKey();
            columnType = spec.getValue();
        }

        m_columnName = CheckUtils.checkArgumentNotNull(columnName);
        m_columnType = CheckUtils.checkArgumentNotNull(columnType);
    }

    /**
     * Gets the column spec.
     *
     * @return this column spec
     */
    @JsonValue
    private Map<String, String> getServiceInputColumnSpec() {
        return Collections.singletonMap(m_columnName, m_columnType);
    }

    /**
     * Gets the column name of this column spec.
     *
     * @return the column name
     */
    public String getName() {
        return m_columnName;
    }

    /**
     * Gets the column type of this column spec.
     *
     * @return the column type
     */
    public String getType() {
        return m_columnType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((m_columnName == null) ? 0 : m_columnName.hashCode());
        result = prime * result + ((m_columnType == null) ? 0 : m_columnType.hashCode());
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ServiceTableColumnSpec other = (ServiceTableColumnSpec)obj;
        if (m_columnName == null) {
            if (other.m_columnName != null) {
                return false;
            }
        } else if (!m_columnName.equals(other.m_columnName)) {
            return false;
        }
        if (m_columnType == null) {
            if (other.m_columnType != null) {
                return false;
            }
        } else if (!m_columnType.equals(other.m_columnType)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return getName() + ":" + getType();
    }


}
