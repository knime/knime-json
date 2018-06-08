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

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Representation of a Table Spec.
 * Can be serialized/deserialized to/from json with jackson.
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 * @since 3.6
 */
public class ServiceTableSpec {

    private final List<ServiceTableColumnSpec> m_columnSpecs;

    /**
     * Constructs a TableSpec from the given columnSpecs.
     *
     * @param serviceInputColumnSpecs the column specs
     */
    @JsonCreator
    public ServiceTableSpec(final List<ServiceTableColumnSpec> serviceInputColumnSpecs) {
        m_columnSpecs = serviceInputColumnSpecs;
    }

    /**
     * Returns the list of column specs for this table spec.
     *
     * @return list of column specs
     */
    @JsonValue
    public List<ServiceTableColumnSpec> getServiceTableColumnSpecs() {
        return m_columnSpecs;
    }

    /**
     * Checks if a given column name/type pair is contained in this table spec.
     *
     * @param columnName name of the column
     * @param columnType type of the column
     * @return true if this table spec contains the name/type pair
     */
    public boolean contains(final String columnName, final String columnType) {
        return m_columnSpecs.contains(new ServiceTableColumnSpec(columnName, columnType));
    }

    /**
     * Returns the number of column specs in this table spec.
     *
     * @return the number of column specs
     */
    public int size() {
        return m_columnSpecs.size();
    }
}
