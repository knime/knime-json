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
package org.knime.json.node.servicein;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author Tobias Urhaug
 */
public class ServiceInputBuilder {

    private List<ServiceInputColumnSpec> m_columnSpecs;
    private List<ServiceInputTableRow> m_tableRows;

    /**
     *
     */
    public ServiceInputBuilder() {
        m_columnSpecs = new ArrayList<>();
        m_tableRows = new ArrayList<>();
    }

    /**
     * Adds a table spec to the table.
     *
     * @param columnName
     * @param columnType
     * @return this factory
     */
    public ServiceInputBuilder withColumnSpec(final String columnName, final String columnType) {
        m_columnSpecs.add(new ServiceInputColumnSpec(columnName, columnType));
        return this;
    }

    /**
     * Adds a null table spec to the service input.
     *
     * @return this factory
     */
    public ServiceInputBuilder withNullTableSpec() {
        m_columnSpecs = null;
        return this;
    }

    /**
     * Adds table specs to the table.
     *
     * @param serviceInputColumnSpecs
     * @return this factory
     */
    public ServiceInputBuilder withColumnSpecs(final List<ServiceInputColumnSpec> serviceInputColumnSpecs) {
        m_columnSpecs.addAll(serviceInputColumnSpecs);
        return this;
    }

    /**
     * Adds a row to the table.
     *
     * @param tableRow
     * @return this factory
     */
    public ServiceInputBuilder withTableRow(final Object... tableRow) {
        m_tableRows.add(new ServiceInputTableRow(Arrays.asList(tableRow)));
        return this;
    }

    /**
     * Adds a null table data to the service input.
     *
     * @return this factory
     */
    public ServiceInputBuilder withNullTableData() {
        m_tableRows = null;
        return this;
    }

    /**
     * Builds a Service Input object.
     *
     * @return a Service Input object with the factory state
     */
    public ServiceInput build() {
        ServiceInputTableSpec tableSpec = null;
        if (m_columnSpecs != null) {
            tableSpec = new ServiceInputTableSpec(m_columnSpecs);
        }
        ServiceInputTableData tableData = null;
        if (m_tableRows != null) {
            tableData = new ServiceInputTableData(m_tableRows);
        }
        return new ServiceInput(tableSpec, tableData);
    }

}
