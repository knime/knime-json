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
 *   Apr 20, 2018 (Tobias Urhaug): created
 */
package org.knime.json.node.servicein;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;
import org.knime.core.data.json.servicetable.ServiceTable;
import org.knime.core.data.json.servicetable.ServiceTableData;
import org.knime.core.data.json.servicetable.ServiceTableRow;
import org.knime.core.data.json.servicetable.ServiceTableSpec;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Test suite for serializing/deserializing {@link ServiceTableInputDefaultJsonStructure}.
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 */
public class ServiceTableInputDefaultJsonStructureTest {

    /**
     * Checks that the table spec of the default json structure is correctly deserialized.
     *
     * @throws Exception
     */
    @Test
    public void testDeserializeDefaultJsonStructureTableSpec() throws Exception {
        String defaultJsonStructure = ServiceTableInputDefaultJsonStructure.asString();

        ServiceTable serviceInput =  new ObjectMapper().readValue(defaultJsonStructure, ServiceTable.class);

        ServiceTableSpec tableSpec = serviceInput.getServiceTableSpec();
        assertTrue(tableSpec.contains("column-string", "string"));
        assertTrue(tableSpec.contains("column-int", "int"));
        assertTrue(tableSpec.contains("column-double", "double"));
        assertTrue(tableSpec.contains("column-long", "long"));
        assertTrue(tableSpec.contains("column-boolean", "boolean"));
        assertTrue(tableSpec.contains("column-localdate", "localdate"));
        assertTrue(tableSpec.contains("column-localdatetime", "localdatetime"));
        assertTrue(tableSpec.contains("column-zoneddatetime", "zoneddatetime"));
    }

    /**
     * Checks that the table data of the default json structure is correctly deserialized.
     *
     * @throws Exception
     */
    @Test
    public void testDeserializeDefaultJsonStructureTableData() throws Exception {
        String defaultJsonStructure = ServiceTableInputDefaultJsonStructure.asString();
        ObjectMapper objectMapper = new ObjectMapper();
        ServiceTable serviceInput = objectMapper.readValue(defaultJsonStructure, ServiceTable.class);

        ServiceTableRow firstExpectedRow = new ServiceTableRow(Arrays.asList("value1", 1, 1.5, 1000, true, "2018-03-27", "2018-03-27T08:30:45.111", "2018-03-27T08:30:45.111+01:00[Europe/Paris]"));
        ServiceTableRow secondExpectedRow =new ServiceTableRow(Arrays.asList("value2", 2, 2.5, 2000, false, "2018-03-28", "2018-03-28T08:30:45.111", "2018-03-28T08:30:45.111+01:00[Europe/Paris]"));
        ServiceTableData tableData = serviceInput.getServiceTableData();

        assertEquals(firstExpectedRow, tableData.getServiceTableRows().get(0));
        assertEquals(secondExpectedRow, tableData.getServiceTableRows().get(1));
    }

}
