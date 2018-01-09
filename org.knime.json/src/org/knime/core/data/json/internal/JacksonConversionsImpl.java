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
 *   21 Sept 2014 (Gabor): created
 */
package org.knime.core.data.json.internal;

import javax.json.JsonValue;

import org.knime.core.data.json.JacksonConversions;

import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.datatype.jsr353.JSR353Module;

/**
 * Converts between JSR353 and Jackson datatypes.
 *
 * @author Gabor Bakos
 */
public final class JacksonConversionsImpl extends JacksonConversions {
    /**
     * Singleton instance.
     */
    public static final JacksonConversions INSTANCE = new JacksonConversionsImpl();

    private final ObjectMapper m_mapper = newMapper().registerModule(new JSR353Module());

    private JacksonConversionsImpl() {}

    /**
     * {@inheritDoc}
     */
    @Override
    public ObjectMapper newMapper() {
        return new ObjectMapper().setNodeFactory(JsonNodeFactory.instance)
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .enable(SerializationFeature.WRITE_BIGDECIMAL_AS_PLAIN)
            .enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonNode toJackson(final JsonValue input) {
        return m_mapper.convertValue(input, JsonNode.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonValue toJSR353(final TreeNode input) {
        return m_mapper.convertValue(input, JsonValue.class);
    }
}
