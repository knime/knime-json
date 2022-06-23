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
 *   Created on Feb 15, 2015 by wiswedel
 */
package org.knime.json.node.container.input.raw;

import java.util.Collections;
import java.util.Map;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Configuration for the JSON Input node.
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
final class RawHTTPInputNodeConfiguration {
    /**
     *
     */
    private static final String CFG_QUERY_PARAMS = "query_params";
    /**
     *
     */
    private static final String CFG_BODY = "body";
    /**
     *
     */
    private static final String CFG_HEADERS = "headers";
    /**
     *
     */
    private static final String EMPTY_JSON = "{}";
    private String m_body = "";
    private Map<String, String> m_headers = Collections.emptyMap();
    private Map<String, String> m_queryParams = Collections.emptyMap();

    private static Map<String, String> jsonToMap(final String json) throws InvalidSettingsException {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            throw new InvalidSettingsException("Unable to parse JSON: " + e.getMessage(), e);
        }
    }

    private static String mapToJson(final Map<String, String> m) throws InvalidSettingsException {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(m);
        } catch (JsonProcessingException e) {
            throw new InvalidSettingsException("Unable to create JSON: " + e.getMessage(), e);
        }
    }

    /**
     * @return the body
     */
    public String getBody() {
        return m_body;
    }

    /**
     * @param body the body to set
     */
    public void setBody(final String body) {
        m_body = body;
    }

    /**
     * @return the headers
     */
    public Map<String, String> getHeaders() {
        return m_headers;
    }

    /**
     * @param headers the headers to set
     * @throws InvalidSettingsException
     */
    public RawHTTPInputNodeConfiguration setHeaders(final String headers) throws InvalidSettingsException {
        m_headers = jsonToMap(headers);
        return this;
    }

    /**
     * @return the queryParams
     */
    public Map<String, String> getQueryParams() {
        return m_queryParams;
    }

    /**
     * @param queryParams the queryParams to set
     * @throws InvalidSettingsException
     */
    public RawHTTPInputNodeConfiguration setQueryParams(final String queryParams) throws InvalidSettingsException {
        m_queryParams = jsonToMap(queryParams);
        return this;
    }

    /**
     * Loads the settings from the given node settings object. Loading will fail if settings are missing or invalid.
     *
     * @param settings a node settings object
     * @return the updated configuration
     * @throws InvalidSettingsException if settings are missing or invalid
     */
    RawHTTPInputNodeConfiguration loadInModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        setBody(settings.getString(CFG_BODY));
        setHeaders(settings.getString(CFG_HEADERS, EMPTY_JSON));
        setQueryParams(settings.getString(CFG_QUERY_PARAMS, EMPTY_JSON));
        return this;
    }

    /**
     * Loads the settings from the given node settings object. Default values will be used for missing or invalid settings.
     *
     * @param settings a node settings object
     * @return the updated configuration
     */
    RawHTTPInputNodeConfiguration loadInDialog(final NodeSettingsRO settings) {
        setBody(settings.getString(CFG_BODY, ""));
        try {
            setHeaders(settings.getString(CFG_HEADERS, EMPTY_JSON));
        } catch (InvalidSettingsException e) {
            m_headers = Collections.emptyMap();
        }
        try {
            setQueryParams(settings.getString(CFG_QUERY_PARAMS, EMPTY_JSON));
        } catch (InvalidSettingsException e) {
            m_queryParams = Collections.emptyMap();
        }
        return this;
    }

    /**
     * Saves the current configuration to the given settings object.
     *
     * @param settings a settings object
     * @return this object
     */
    RawHTTPInputNodeConfiguration save(final NodeSettingsWO settings) {
        settings.addString(CFG_BODY, m_body);
        try {
            settings.addString(CFG_HEADERS, mapToJson(m_headers));
        } catch (InvalidSettingsException e) {
            settings.addString(CFG_HEADERS, EMPTY_JSON);
        }
        try {
            settings.addString(CFG_QUERY_PARAMS, mapToJson(m_queryParams));
        } catch (InvalidSettingsException e) {
            settings.addString(CFG_QUERY_PARAMS, EMPTY_JSON);
        }
        return this;
    }

    /**
     * @return
     */
    public String getHeadersAsString() {
        try {
            return mapToJson(m_headers);
        } catch (InvalidSettingsException e) {
            return EMPTY_JSON;
        }
    }

    public String getQueryParametersAsString() {
        try {
            return mapToJson(m_queryParams);
        } catch (InvalidSettingsException e) {
            return EMPTY_JSON;
        }
    }

    /** {@inheritDoc} */
    /*
    @Override
    public String toString() {
        return "\"" + m_parameterName + "\": " + m_value.toString();
    }*/
}
