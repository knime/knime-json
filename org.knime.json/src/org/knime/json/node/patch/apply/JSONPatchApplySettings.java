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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
 *   28 Sept 2014 (Gabor): created
 */
package org.knime.json.node.patch.apply;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.knime.core.data.json.JSONValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.json.node.util.RemoveOrAddColumnSettings;

/**
 * The settings object for the JSONTransformer node.
 *
 * @author Gabor Bakos
 */
final class JSONPatchApplySettings extends RemoveOrAddColumnSettings {
    static final String PATCH_OPTION = "patch";

    static final String MERGE_PATCH_OPTION = "merge and patch";

    static final String JSON_PATCH = "json.patch",
            DEFAULT_JSON_PATCH = "[ { \"op\": \"move\", \"from\": \"/a\", \"path\": \"/c\" } ]";

    static final String PATCH_TYPE = "patch.type";

    static final String KEEP_ORIGINAL_WHEN_TEST_FAILS = "keep original when test fails";

    static final boolean DEFAULT_KEEP_ORIGINAL_WHEN_TEST_FAILS = false, COMPAT_KEEP_ORIGINAL_WHEN_TEST_FAILS = false;

    static final List<String> PATCH_TYPES = Collections.unmodifiableList(Arrays
        .asList(PATCH_OPTION, MERGE_PATCH_OPTION));

    private String m_patchType = PATCH_TYPES.get(0), m_jsonPatch = DEFAULT_JSON_PATCH;

    private boolean m_keepOriginalWhenTestFails = false;

    /**
     * Constructs the {@link JSONPatchApplySettings} object.
     */
    JSONPatchApplySettings() {
        super(JSONValue.class);
        setRemoveInputColumn(true);
    }

    /**
     * @return the patchType
     */
    final String getPatchType() {
        return m_patchType;
    }

    /**
     * @param patchType the patchType to set
     */
    final void setPatchType(final String patchType) {
        this.m_patchType = patchType;
    }

    /**
     * @return the jsonPatch
     */
    final String getJsonPatch() {
        return m_jsonPatch;
    }

    /**
     * @param jsonPatch the jsonPatch to set
     */
    final void setJsonPatch(final String jsonPatch) {
        this.m_jsonPatch = jsonPatch;
    }

    /**
     * @return the keepOriginalWhenTestFails
     */
    final boolean isKeepOriginalWhenTestFails() {
        return m_keepOriginalWhenTestFails;
    }

    /**
     * @param keepOriginalWhenTestFails the keepOriginalWhenTestFails to set
     */
    final void setKeepOriginalWhenTestFails(final boolean keepOriginalWhenTestFails) {
        m_keepOriginalWhenTestFails = keepOriginalWhenTestFails;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsForDialogs(final NodeSettingsRO settings, final PortObjectSpec[] specs) {
        super.loadSettingsForDialogs(settings, specs);
        m_patchType = settings.getString(PATCH_TYPE, PATCH_TYPES.get(0));
        m_jsonPatch = settings.getString(JSON_PATCH, DEFAULT_JSON_PATCH);
        m_keepOriginalWhenTestFails =
            settings.getBoolean(KEEP_ORIGINAL_WHEN_TEST_FAILS, DEFAULT_KEEP_ORIGINAL_WHEN_TEST_FAILS);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.loadSettingsFrom(settings);
        m_patchType = settings.getString(PATCH_TYPE);
        m_jsonPatch = settings.getString(JSON_PATCH);
        m_keepOriginalWhenTestFails =
            settings.getBoolean(KEEP_ORIGINAL_WHEN_TEST_FAILS, COMPAT_KEEP_ORIGINAL_WHEN_TEST_FAILS);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        super.saveSettingsTo(settings);
        settings.addString(PATCH_TYPE, m_patchType);
        settings.addString(JSON_PATCH, m_jsonPatch);
        settings.addBoolean(KEEP_ORIGINAL_WHEN_TEST_FAILS, m_keepOriginalWhenTestFails);
    }
}
