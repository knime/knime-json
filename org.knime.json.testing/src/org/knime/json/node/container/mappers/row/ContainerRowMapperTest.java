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
 *   Apr 12, 2019 (Tobias Urhaug, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.json.node.container.mappers.row;

import java.io.IOException;
import java.math.BigInteger;
import java.util.HashMap;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import org.knime.core.data.container.ContainerTable;
import org.knime.core.node.DefaultNodeProgressMonitor;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.Node;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.SingleNodeContainer;
import org.knime.core.node.workflow.virtual.parchunk.VirtualParallelizedChunkPortObjectInNodeFactory;
import org.knime.json.util.JSONUtil;

/**
 * Marker class for easy look up for {@link ContainerRowMapper} test classes.
 *
 * For the concrete test classes look at the type hierarchy of this class.
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 */
abstract class ContainerRowMapperTest {

    @SuppressWarnings("deprecation")
    static ExecutionContext getTestExecutionCtx() {
        @SuppressWarnings({"unchecked", "rawtypes"})
        NodeFactory<NodeModel> dummyFactory =
            (NodeFactory)new VirtualParallelizedChunkPortObjectInNodeFactory(new PortType[0]);
        return new ExecutionContext(new DefaultNodeProgressMonitor(), new Node(dummyFactory),
            SingleNodeContainer.MemoryPolicy.CacheOnDisc, new HashMap<Integer, ContainerTable>());
    }

    /**
     * Helper class for easy instantiation of JsonValue in a fluent style.
     *
     * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
     */
    class JsonValueBuilder {

        private final JsonObjectBuilder m_builder;
        private final JsonBuilderFactory m_factory;
        private final JsonArrayBuilder m_arrayBuilder;

        JsonValueBuilder() {
            m_factory = Json.createBuilderFactory(null);
            m_builder = m_factory.createObjectBuilder();
            m_arrayBuilder = m_factory.createArrayBuilder();
        }

        JsonValueBuilder withStringObject(final String key, final String value) {
            m_builder.add(key, value);
            return this;
        }

        JsonValueBuilder withDoubleObject(final String key, final double value) {
            m_builder.add(key, value);
            return this;
        }

        JsonValueBuilder withIntObject(final String key, final int value) {
            m_builder.add(key, value);
            return this;
        }

        JsonValueBuilder withLongObject(final String key, final long value) {
            m_builder.add(key, value);
            return this;
        }

        JsonValueBuilder withBooleanObject(final String key, final boolean value) {
            m_builder.add(key, value);
            return this;
        }

        JsonValueBuilder withStringArrayObject(final String key, final String... values) {
            for (String value : values) {
                m_arrayBuilder.add(value);
            }
            m_builder.add(key, m_arrayBuilder);
            return this;
        }

        JsonValueBuilder withBigIntegerObject(final String key, final BigInteger value) {
            m_builder.add(key, value);
            return this;
        }

        public JsonValueBuilder withJsonPersonObject() {
            JsonObjectBuilder personBuilder = m_factory.createObjectBuilder();
            JsonObject personObject = personBuilder.add("name", "Flodve").add("age", 32).build();
            m_builder.add("person", personObject);
            return this;
        }

        JsonValueBuilder withNullObject(final String key) {
            m_builder.addNull(key);
            return this;
        }

        JsonValue build() throws IOException {
            return JSONUtil.parseJSONValue(m_builder.build().toString());
        }

    }

}
