/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
package org.knime.json.node.jsonpath.projection;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.knime.core.data.json.JSONValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.json.node.util.ReplaceOrAddColumnSettings;

/**
 * The settings object for the JSONPathProjection node.
 *
 * @author Gabor Bakos
 */
final class JSONPathProjectionSettings extends ReplaceOrAddColumnSettings {
    private static final String JSON_PATH = "jsonpath", PATH_TYPE = "path.type";

    static final String JSON_PATH_OPTION = "JsonPath", JSON_POINTER_OPTION = "JSON Pointer";

    private static final String DEFAULT_JSON_PATH = "$..*";

    private String m_jsonPath = DEFAULT_JSON_PATH, m_pathType = pathTypes().get(0);

    static final List<String> pathTypes() {
        return Collections.unmodifiableList(Arrays.asList(JSON_PATH_OPTION, JSON_POINTER_OPTION));
    }

    /**
     * Constructs the {@link JSONPathProjectionSettings} object.
     */
    JSONPathProjectionSettings() {
        super(JSONValue.class);
    }

    /**
     * @return the jsonPath
     */
    final String getJsonPath() {
        return m_jsonPath;
    }

    /**
     * @param jsonPath the jsonPath to set
     */
    final void setJsonPath(final String jsonPath) {
        this.m_jsonPath = jsonPath;
    }

    /**
     * @return the pathType
     */
    final String getPathType() {
        return m_pathType;
    }

    /**
     * @param pathType the pathType to set
     */
    final void setPathType(final String pathType) {
        this.m_pathType = pathType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsForDialogs(final NodeSettingsRO settings, final PortObjectSpec[] specs) {
        super.loadSettingsForDialogs(settings, specs);
        m_jsonPath = settings.getString(JSON_PATH, DEFAULT_JSON_PATH);
        m_pathType = settings.getString(PATH_TYPE, pathTypes().get(0));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.loadSettingsFrom(settings);
        m_jsonPath = settings.getString(JSON_PATH);
        m_pathType = settings.getString(PATH_TYPE);
        if (!pathTypes().contains(m_pathType)) {
            throw new InvalidSettingsException("Not supported path selector option: " + m_pathType);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        super.saveSettingsTo(settings);
        settings.addString(JSON_PATH, m_jsonPath);
        settings.addString(PATH_TYPE, m_pathType);
    }
}
