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
 *   Aug 1, 2018 (Tobias Urhaug, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.core.data.json.container.credentials;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Defines a json schema for credentials being sent to Container Input (Variable) nodes.
 * Main function is to serve as an interface between JSON and credentials.
 * Can be serialized/deserialized to/from json with jackson.
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 */
public class ContainerCredentialsJsonSchema {

    private final boolean m_isEncrypted;
    private final List<ContainerCredential> m_credentials;

    /**
     * Constructor for the container credentials.
     * @param isEncrypted flag that marks if the password is encrypted
     * @param credentials
     */
    public ContainerCredentialsJsonSchema(
            @JsonProperty("isEncrypted") final boolean isEncrypted,
            @JsonProperty("credentials") final List<ContainerCredential> credentials) {
        m_isEncrypted = isEncrypted;
        m_credentials =credentials;
    }

    /**
     * Gets the isEncrypted flag.
     * @return true if the passwords are encrypted
     */
    @JsonProperty("isEncrypted")
    public boolean isEncrypted() {
        return m_isEncrypted;
    }

    /**
     * Gets the credentials.
     * @return list of credentials
     */
    @JsonProperty("credentials")
    public List<ContainerCredential> getCredentials() {
        return m_credentials;
    }

    /**
     * Checks if a string conforms to the json schema outlined by this class.
     * @param json the json string to be checked
     * @return true if the given string conforms to this schema
     */
    public static boolean hasContainerCredentialJsonSchema(final String json) {
        try {
            new ObjectMapper().readValue(json, ContainerCredentialsJsonSchema.class);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

}
