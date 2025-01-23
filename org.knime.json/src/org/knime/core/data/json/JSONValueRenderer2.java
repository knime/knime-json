/*
 * ------------------------------------------------------------------------
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
 * Created on 24.05.2013 by thor
 */
package org.knime.core.data.json;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.renderer.AbstractDataValueRendererFactory;
import org.knime.core.data.renderer.DataValueRenderer;
import org.knime.core.data.renderer.MultiLineStringValueRenderer;
import org.knime.core.data.xml.XMLValueRenderer;
import org.knime.core.util.EclipseUtil;
import org.knime.json.util.JSONUtil;

/**
 * Default (multi-line String) renderer for JSON values. <br/>
 * Based on {@link XMLValueRenderer}.
 *
 * @author Thorsten Meinl, KNIME AG, Zurich, Switzerland
 * @author Gabor Bakos
 * @author Moditha Hewasinghage
 * @since 4.4
 */
@SuppressWarnings("serial")
public final class JSONValueRenderer2 extends MultiLineStringValueRenderer {
    /**
     * Maximum number of characters to render
     */
    private static final int MAX_RENDER_CHARS_MODERN_UI = 1_000;

    private static final int MAX_RENDER_CHARS_CLASSIC_UI = 10_000;

    /**
     * Factory for {@link JSONValueRenderer2}.
     */
    public static final class Factory extends AbstractDataValueRendererFactory {
        private static final String DESCRIPTION = "JSON value";

        @Override
        public String getDescription() {
            return DESCRIPTION;
        }

        @Override
        public DataValueRenderer createRenderer(final DataColumnSpec colSpec) {
            return new JSONValueRenderer2(DESCRIPTION);
        }
    }

    /**
     * Constructor.
     *
     * @param description a description for the renderer
     */
    JSONValueRenderer2(final String description) {
        super(description);
    }

    /**
     * {@inheritDoc} Performance improvement for large JSON values Only pretty print JSONs smaller than
     * {@link JSONValueRenderer2#MAX_RENDER_CHARS} otherwise truncate the string value
     */
    @Override
    protected void setValue(final Object value) {
        if (!(value instanceof JSONValue jsonValue)) {
            super.setValue(value);
            return;
        }
        final var maxRenderChars =
            EclipseUtil.determineClassicUIUsage() ? MAX_RENDER_CHARS_CLASSIC_UI : MAX_RENDER_CHARS_MODERN_UI;
        super.setValue(JSONUtil.abbreviateOrToPrettyJSONString(jsonValue, maxRenderChars));
    }
}
