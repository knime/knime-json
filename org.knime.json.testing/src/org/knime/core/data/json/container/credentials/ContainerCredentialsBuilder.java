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
 *   Aug 2, 2018 (Tobias Urhaug, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.core.data.json.container.credentials;

import java.util.ArrayList;
import java.util.List;

/**
 * Builder class that simplifies setting up test fixtures using {@link ContainerCredentialsJsonSchema}.
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 */
public class ContainerCredentialsBuilder {

    private boolean m_isEncrypted;
    private List<ContainerCredential> m_credentials;

    /**
     * Constructs a new builder with default values.
     */
    public ContainerCredentialsBuilder() {
        m_isEncrypted = false;
        m_credentials = new ArrayList<>();
    }

    /**
     * Sets the isEncrypted flag for this builder.
     * @param isEncrypted flag to be set
     * @return this builder with the set encryption flag
     */
    public ContainerCredentialsBuilder withEncrypted(final boolean isEncrypted) {
        m_isEncrypted = isEncrypted;
        return this;
    }

    /**
     * Adds a credential to the builder.
     * @param credential to be added
     * @return this builder with the added credential
     */
    public ContainerCredentialsBuilder withCredential(final ContainerCredential credential) {
        m_credentials.add(credential);
        return this;
    }

    /**
     * @return a new {@link ContainerCredentialsJsonSchema} with the settings of this builder
     */
    public ContainerCredentialsJsonSchema build() {
        return new ContainerCredentialsJsonSchema(m_isEncrypted, m_credentials);
    }

}
