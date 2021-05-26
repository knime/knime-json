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
 *   Apr 7, 2021 (Moditha): created
 */
package org.knime.json.node.filehandling.reader;

import org.knime.filehandling.core.node.table.reader.config.ReaderSpecificConfig;

/**
 * {@link ReaderSpecificConfig} for the JSON reader node.
 *
 * @author Moditha Hewasinghage, KNIME GmbH, Berlin, Germany
 */
public final class JSONReaderConfig implements ReaderSpecificConfig<JSONReaderConfig> {

    private String m_columnName = "json";

    /**
     * For future use, default value should be streaming mode
     */
    private JSONReadMode m_jsonReadMode = JSONReadMode.LEGACY;

    private boolean m_allowComments = false;

    private boolean m_useJSONPath = false;

    private boolean m_failIfNotFound = false;

    private String m_jsonPath = "$";

    /**
     * Constructor.
     */
    JSONReaderConfig() {
    }

    /**
     * @param jsonReaderConfig
     */
    private JSONReaderConfig(final JSONReaderConfig toCopy) {
        setColumnName(toCopy.getColumnName());
        setJsonReadMode(toCopy.getJsonReadMode());
        setAllowComments(toCopy.allowComments());
        setFailIfNotFound(toCopy.failIfNotFound());
        setJSONPath(toCopy.getJSONPath());
        setUseJSONPath(toCopy.useJSONPath());
    }

    @Override
    public JSONReaderConfig copy() {
        return new JSONReaderConfig(this);
    }

    /**
     * @return the columnName
     */
    public String getColumnName() {
        return m_columnName;
    }

    /**
     * @param columnName the columnName to set
     */
    public void setColumnName(final String columnName) {
        m_columnName = columnName;
    }

    /**
     * @return the jsonReadMode
     */
    public JSONReadMode getJsonReadMode() {
        return m_jsonReadMode;
    }

    /**
     * @param jsonReadMode the jsonReadMode to set
     */
    public void setJsonReadMode(final JSONReadMode jsonReadMode) {
        m_jsonReadMode = jsonReadMode;
    }

    /**
     * @return the allowComments
     */
    public boolean allowComments() {
        return m_allowComments;
    }

    /**
     * @param allowComments the allowComments to set
     */
    public void setAllowComments(final boolean allowComments) {
        m_allowComments = allowComments;
    }

    /**
     * @return the useJSONPath
     */
    public boolean useJSONPath() {
        return m_useJSONPath;
    }

    /**
     * @param useJSONPath the useJSONPath to set
     */
    public void setUseJSONPath(final boolean useJSONPath) {
        m_useJSONPath = useJSONPath;
    }

    /**
     * @return the jSONPath
     */
    public String getJSONPath() {
        return m_jsonPath;
    }

    /**
     * @param jSONPath the jSONPath to set
     */
    public void setJSONPath(final String jSONPath) {
        m_jsonPath = jSONPath;
    }

    /**
     * @return the failIfNotFound
     */
    public boolean failIfNotFound() {
        return m_failIfNotFound;
    }

    /**
     * @param failIfNotFound the failIfNotFound to set
     */
    public void setFailIfNotFound(final boolean failIfNotFound) {
        m_failIfNotFound = failIfNotFound;
    }
}
