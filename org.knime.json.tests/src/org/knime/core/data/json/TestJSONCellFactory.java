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
 *   9. Sept. 2014 (Gabor): created
 */
package org.knime.core.data.json;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;

/**
 * Testcases for {@link JSONCellFactory}.
 *
 * @author Gabor Bakos
 */
public class TestJSONCellFactory {

    /**
     * Test method for {@link org.knime.core.data.json.JSONCellFactory#create(java.lang.String, boolean)}.
     *
     * @throws IOException
     */
    @Test
    public void testCreateString() throws IOException {
        assertNotNull("Empty array", JSONCellFactory.create("[]", false));
        assertNotNull(
            "Object",
            JSONCellFactory
                .create(
                    "{\"name\" : { \"first\" : \"Joe\", \"last\" : \"Sixpack\" },  \"gender\" : \"MALE\", \"verified\" : false,  \"userImage\" : \"Rm9vYmFyIQ==\"}",
                    false));
        assertNotNull("Array", JSONCellFactory.create("[{\"foo\": \"bar\"},{\"foo\": \"biz\"}]", false));
        assertNotNull("String", JSONCellFactory.create("\"foo\"", false));
        assertNotNull("Integer", JSONCellFactory.create("42", false));
        assertNotNull("Floating point number", JSONCellFactory.create("-3.14159", false));
        assertNotNull("Very large number", JSONCellFactory.create("123456789123456789123456789", false));
    }

    /**
     * {@link Double#NaN}s are not supported by JSON: http://stackoverflow.com/q/1423081
     *
     * @throws IOException
     */
    @Test(expected = JsonParseException.class)
    public void testNaN() throws IOException {
        JSONCellFactory.create("NaN", false);
    }

    /**
     * {@link Double#NaN}s are not supported by JSON: http://stackoverflow.com/q/1423081
     *
     * @throws IOException
     */
    @Test(expected = JsonParseException.class)
    public void testOctal() throws IOException {
        JSONCellFactory.create("00", false);
    }

    /**
     * {@link Double#NaN}s are not supported by JSON: http://stackoverflow.com/q/1423081
     *
     * @throws IOException
     */
    @Test(expected = JsonParseException.class)
    public void testHexadecimal() throws IOException {
        JSONCellFactory.create("0x0", false);
    }

    /**
     * {@link Double#POSITIVE_INFINITY} and {@link Double#NEGATIVE_INFINITY} are not supported by json:
     * http://stackoverflow.com/q/1423081
     *
     * @throws IOException
     */
    @Test(expected = JsonParseException.class)
    public void testInfinity() throws IOException {
        JSONCellFactory.create("Infinity", false);
    }
}
