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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 *
 * @author Tobias Urhaug
 */
public class ServiceTableColumnSpec {

    private final Map<String, String> m_columnSpec;

    /**
     * @param columnName
     * @param columnType
     */
    public ServiceTableColumnSpec(final String columnName, final String columnType) {
        m_columnSpec = new HashMap<>();
        m_columnSpec.put(columnName, columnType);
    }

    /**
     * @param columnSpec
     */
    @JsonCreator
    public ServiceTableColumnSpec(final Map<String, String> columnSpec) {
        m_columnSpec = columnSpec;
    }

    /**
     * Gets the column spec.
     *
     * @return this column spec
     */
    @JsonValue
    public Map<String, String> getServiceInputColumnSpec() {
        return m_columnSpec;
    }

    /**
     * Gets the column name of this column spec.
     *
     * @return the column name
     */
    public String getName() {
        String columnName = null;
        for (Entry<String,String> spec : m_columnSpec.entrySet()) {
            columnName = spec.getKey();
        }
        return columnName;
    }

    /**
     * Gets the column type of this column spec.
     *
     * @return the column type
     */
    public String getType() {
        String columnType = null;
        for (Entry<String,String> spec : m_columnSpec.entrySet()) {
            columnType = spec.getValue();
        }
        return columnType;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((m_columnSpec == null) ? 0 : m_columnSpec.hashCode());
        return result;
    }

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
        ServiceTableColumnSpec other = (ServiceTableColumnSpec) obj;
        if (m_columnSpec == null) {
            if (other.m_columnSpec != null) {
                return false;
            }
        } else if (!m_columnSpec.equals(other.m_columnSpec)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return getName() + ":" + getType();
    }


}
