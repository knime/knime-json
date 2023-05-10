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
package org.knime.json.node.container;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.knime.core.data.json.container.table.ContainerTableColumnSpec;
import org.knime.core.data.json.container.table.ContainerTableData;
import org.knime.core.data.json.container.table.ContainerTableJsonSchema;
import org.knime.core.data.json.container.table.ContainerTableRow;
import org.knime.core.data.json.container.table.ContainerTableSpec;
import org.knime.core.util.JsonUtil;
import org.knime.json.util.JSONUtil;

import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonValue;

/**
 * Builder class that simplifies setting up test fixtures using {@link ContainerTableJsonSchema}.
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 */
public class ContainerTableBuilder {

    private List<ContainerTableColumnSpec> m_columnSpecs;
    private List<ContainerTableRow> m_tableRows;

    /**
     * Constructs an empty builder.
     */
    public ContainerTableBuilder() {
        m_columnSpecs = new ArrayList<>();
        m_tableRows = new ArrayList<>();
    }

    /**
     * Adds a table spec to the table.
     *
     * @param columnName the column name of the column spec
     * @param columnType the column type of the column spec
     * @return this factory
     */
    public ContainerTableBuilder withColumnSpec(final String columnName, final String columnType) {
        m_columnSpecs.add(new ContainerTableColumnSpec(columnName, columnType));
        return this;
    }

    /**
     * Adds a null table spec to the service input.
     *
     * @return this factory
     */
    public ContainerTableBuilder withNullTableSpec() {
        m_columnSpecs = null;
        return this;
    }

    /**
     * Adds table specs to the table.
     *
     * @param serviceInputColumnSpecs list of column specs
     * @return this factory
     */
    public ContainerTableBuilder withColumnSpecs(final List<ContainerTableColumnSpec> serviceInputColumnSpecs) {
        m_columnSpecs.addAll(serviceInputColumnSpecs);
        return this;
    }

    /**
     * Adds a row to the table.
     *
     * @param tableRow
     * @return this factory
     */
    public ContainerTableBuilder withTableRow(final Object... tableRow) {
        m_tableRows.add(new ContainerTableRow(Arrays.asList(tableRow)));
        return this;
    }

    /**
     * Adds a null table data to the service input.
     *
     * @return this factory
     */
    public ContainerTableBuilder withNullTableData() {
        m_tableRows = null;
        return this;
    }

    /**
     * Builds a {@link ContainerTableJsonSchema}.
     *
     * @return a {@link ContainerTableJsonSchema} object with the factory state
     */
    public ContainerTableJsonSchema build() {
        ContainerTableSpec tableSpec = null;
        if (m_columnSpecs != null) {
            tableSpec = new ContainerTableSpec(m_columnSpecs);
        }
        ContainerTableData tableData = null;
        if (m_tableRows != null) {
            tableData = new ContainerTableData(m_tableRows);
        }
        return new ContainerTableJsonSchema(tableSpec, tableData);
    }

    /**
     * Builds a {@link JsonValue}.
     *
     * @return a {@link JsonValue} object with the factory state
     * @throws IOException if parsing to JsonValue is not succesful
     */
    public JsonValue buildAsJson() throws IOException {
        JsonBuilderFactory factory = JsonUtil.getProvider().createBuilderFactory(null);
        JsonObjectBuilder builder = factory.createObjectBuilder();

        if (m_columnSpecs != null) {
            builder.add("table-spec", createTableSpec(factory));
        }

        if (m_tableRows != null) {
            builder.add("table-data", createTableData(factory));
        }

        return JSONUtil.parseJSONValue(builder.build().toString());
    }

    private JsonArray createTableSpec(final JsonBuilderFactory factory) {
        JsonArrayBuilder builder = factory.createArrayBuilder();
        for (ContainerTableColumnSpec columnSpec : m_columnSpecs) {
            builder.add(factory.createObjectBuilder().add(columnSpec.getName(), columnSpec.getType()));
        }

        return builder.build();
    }

    private JsonValue createTableData(final JsonBuilderFactory factory) {
        JsonArrayBuilder tableDataBuilder = factory.createArrayBuilder();
        for (ContainerTableRow row : m_tableRows) {
            JsonArrayBuilder rowBuilder = factory.createArrayBuilder();
            for (Object cell : row.getDataCellObjects()) {
                parseAndAdd(cell, rowBuilder);
            }
            tableDataBuilder.add(rowBuilder);
        }

        return tableDataBuilder.build();
    }

    private static void parseAndAdd(final Object cell, final JsonArrayBuilder rowBuilder) {
        if (cell instanceof String) {
            rowBuilder.add((String) cell);
        } else if (cell instanceof Integer) {
            rowBuilder.add((Integer) cell);
        } else if (cell instanceof Double) {
            rowBuilder.add((Double) cell);
        } else if (cell instanceof Long) {
            rowBuilder.add((Long) cell);
        } else if (cell instanceof Boolean) {
            rowBuilder.add((Boolean) cell);
        } else {
            throw new IllegalArgumentException("The test builder can only handle primitive types");
        }
    }

}
