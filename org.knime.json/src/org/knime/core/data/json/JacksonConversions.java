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
 *   21 Sept 2014(Gabor): created
 */
package org.knime.core.data.json;

import org.knime.core.data.json.internal.JacksonConversionsImpl;

import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import jakarta.json.JsonValue;

/**
 * An interface to convert between JSR-353 {@link JsonValue}s and Jackson {@link JsonNode}s.
 *
 * @author Gabor Bakos
 * @since 2.11
 */
public abstract class JacksonConversions {
    /**
     * Return a preconfigured {@link ObjectMapper}
     *
     * <p>The returned mapper will have the following features enabled:</p>
     *
     * <ul>
     *     <li>{@link DeserializationFeature#USE_BIG_DECIMAL_FOR_FLOATS};</li>
     *     <li>{@link SerializationFeature#WRITE_BIGDECIMAL_AS_PLAIN};</li>
     *     <li>{@link SerializationFeature#INDENT_OUTPUT}.</li>
     * </ul>
     *
     * <p>This returns a new instance each time.</p>
     *
     * @return an {@link ObjectMapper}
     */
    public abstract ObjectMapper newMapper();

    /**
     * @param input A JSR-353 {@link JsonValue}.
     * @return The converted Jackson {@link JsonNode}.
     * @since 5.1
     */
    public abstract JsonNode toJackson(JsonValue input);

    /**
     * @param input A Jackson {@link TreeNode}.
     * @return The converted JSR-353 {@link JsonValue}.
     * @since 5.1
     */
    public abstract JsonValue toJSR353(TreeNode input);

    /**
     * Default implementation of {@link JacksonConversions}.
     *
     * @return a new instance
     */
    public static JacksonConversions getInstance() {
        return JacksonConversionsImpl.INSTANCE;
    }
}