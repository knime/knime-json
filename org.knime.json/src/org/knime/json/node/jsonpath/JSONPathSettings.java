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
package org.knime.json.node.jsonpath;

import java.util.EnumSet;

import org.knime.core.data.StringValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.json.node.util.OutputType;
import org.knime.json.node.util.PathOrPointerSettings;

/**
 * The settings object for the JSONPath node.
 *
 * @author Gabor Bakos
 */
final class JSONPathSettings extends PathOrPointerSettings {
    private static final String JSON_PATH = "jsonpath";

    private static final String DEFAULT_JSON_PATH = "$..*";

    private String m_jsonPath = DEFAULT_JSON_PATH;

    private static final String RESULT_IS_LIST = "result.is.list";

    private static final boolean DEFAULT_RESULT_IS_LIST = true;

    private boolean m_resultIsList = DEFAULT_RESULT_IS_LIST;

    private static final String RETURN_PATHS = "return.paths.instead.of.values";

    private static final boolean DEFAULT_RETURN_PATHS = false;

    private boolean m_returnPaths = DEFAULT_RETURN_PATHS;

    /** Options to do if there are multiple results, but we expected a single. */
    static enum OnMultipleResults implements StringValue {
        Fail, Missing, First, Last, Concatenate;

        public boolean supportsConcatenate(final OutputType type) {
            //TODO should we also support binary?
            return this == Concatenate && (type == OutputType.Json || type == OutputType.String);
        }

        public EnumSet<OutputType> supportedOutputTypes() {
            switch (this) {
                case Concatenate:
                    return EnumSet.of(OutputType.String, OutputType.Json);//TODO OutputType#Binary too?
                case Fail://intentional fall through
                case First://intentional fall through
                case Last://intentional fall through
                case Missing:
                    return EnumSet.allOf(OutputType.class);
                default:
                    throw new UnsupportedOperationException("Unknown type: " + this);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getStringValue() {
            switch (this) {
                case Fail:
                    return "Fail on multiple values";
                case First:
                    return "Select the first value";
                case Last:
                    return "Select the last value";
                case Missing:
                    return "Provide missing value";
                case Concatenate:
                    return "Concatenate the results";
                default:
                    throw new UnsupportedOperationException("Unknown multiple result strategy: " + this);
            }
        }
    }

    /**
     * Constructs the {@link JSONPathSettings} object.
     *
     * @param logger The logger to log warnings and errors.
     */
    JSONPathSettings(final NodeLogger logger) {
        super(logger);
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
     * @return the resultIsList
     */
    final boolean isResultIsList() {
        return m_resultIsList;
    }

    /**
     * @param resultIsList the resultIsList to set
     */
    final void setResultIsList(final boolean resultIsList) {
        this.m_resultIsList = resultIsList;
    }

    /**
     * @return the returnPaths
     */
    final boolean isReturnPaths() {
        return m_returnPaths;
    }

    /**
     * @param returnPaths the returnPaths to set
     */
    final void setReturnPaths(final boolean returnPaths) {
        this.m_returnPaths = returnPaths;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsForDialogs(final NodeSettingsRO settings, final PortObjectSpec[] specs) {
        super.loadSettingsForDialogs(settings, specs);
        m_jsonPath = settings.getString(JSON_PATH, DEFAULT_JSON_PATH);
        m_resultIsList = settings.getBoolean(RESULT_IS_LIST, DEFAULT_RESULT_IS_LIST);
        m_returnPaths = settings.getBoolean(RETURN_PATHS, DEFAULT_RETURN_PATHS);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.loadSettingsFrom(settings);
        m_jsonPath = settings.getString(JSON_PATH);
        m_resultIsList = settings.getBoolean(RESULT_IS_LIST);
        m_returnPaths = settings.getBoolean(RETURN_PATHS);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        super.saveSettingsTo(settings);
        settings.addString(JSON_PATH, m_jsonPath);
        settings.addBoolean(RESULT_IS_LIST, m_resultIsList);
        settings.addBoolean(RETURN_PATHS, m_returnPaths);
    }
}
