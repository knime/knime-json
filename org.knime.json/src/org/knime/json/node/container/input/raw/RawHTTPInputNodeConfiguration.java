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
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.config.Config;

/**
 * Configuration for the Container Input (Raw HTTP) node.
 *
 * @author Alexander Fillbrunn, KNIME GmbH, Konstanz, Germany
 */
final class RawHTTPInputNodeConfiguration {

    private static final String CFG_QUERY_PARAMS = "query_params";

    private static final String CFG_BODY = "body";

    private static final String CFG_HEADERS = "headers";

    private String m_body = "";

    private Map<String, String> m_headers = new HashMap<>() {{
        put("content-type", "application/octet-stream");
    }};

    private Map<String, String> m_queryParams = new HashMap<>();

    /**
     * @return the body stored in this configuration in base64 format
     */
    public String getBody() {
        return m_body;
    }

    /**
     * @param body the body to set in base64 format
     */
    public void setBody(final String body) {
        m_body = body;
    }

    /**
     * @return the headers stored in this configuration
     */
    public Map<String, String> getHeaders() {
        return m_headers;
    }

    /**
     * Sets the headers stored by this configuration.
     * @param headers a map where the key is the header name and the value the header value
     * @return this instance of the configuration
     */
    public RawHTTPInputNodeConfiguration setHeaders(final Map<String, String> headers) {
        m_headers.clear();
        for (Entry<String, String> e : headers.entrySet()) {
            m_headers.put(e.getKey(), e.getValue());
        }
        return this;
    }

    /**
     * @return the queryParams
     */
    public Map<String, String> getQueryParams() {
        return m_queryParams;
    }

    /**
     * Sets the query parameters stored by this configuration.
     * @param queryParams a map where the key is the parameter name and the value the parameter value
     * @return this instance of the configuration
     */
    public RawHTTPInputNodeConfiguration setQueryParams(final Map<String, String> queryParams) {
        m_queryParams.clear();
        for (Entry<String, String> e : queryParams.entrySet()) {
            m_queryParams.put(e.getKey(), e.getValue());
        }
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
        m_headers.clear();
        Config headerConf = settings.getConfig(CFG_HEADERS);
        for (String key : headerConf.keySet()) {
            String value = headerConf.getString(key);
            m_headers.put(key, value);
        }

        m_queryParams.clear();
        Config qpConf = settings.getConfig(CFG_QUERY_PARAMS);
        for (String key : qpConf.keySet()) {
            String value = qpConf.getString(key);
            m_queryParams.put(key, value);
        }
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

        m_headers.clear();
        try {
            Config headerConf = settings.getConfig(CFG_HEADERS);
            for (String key : headerConf.keySet()) {
                String value = headerConf.getString(key);
                m_headers.put(key, value);
            }
        } catch (InvalidSettingsException e) {
            m_headers = Collections.emptyMap();
        }

        m_queryParams.clear();
        try {
            Config qpConf = settings.getConfig(CFG_QUERY_PARAMS);
            for (String key : qpConf.keySet()) {
                String value = qpConf.getString(key);
                m_queryParams.put(key, value);
            }
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

        Config headerConf = settings.addConfig(CFG_HEADERS);
        for (Entry<String, String> e : m_headers.entrySet()) {
            headerConf.addString(e.getKey(), e.getValue());
        }

        Config qpConf = settings.addConfig(CFG_QUERY_PARAMS);
        for (Entry<String, String> e : m_queryParams.entrySet()) {
            qpConf.addString(e.getKey(), e.getValue());
        }

        return this;
    }
}
