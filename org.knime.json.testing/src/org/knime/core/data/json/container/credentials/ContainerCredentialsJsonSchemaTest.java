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

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Test suite for {@link ContainerCredentialsJsonSchema}.
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 */
public class ContainerCredentialsJsonSchemaTest {

    /**
     * Tests the serialization of an isEncrypted property.
     * @throws Exception
     */
    @Test
    public void testSerializeIsEncryptedProperty() throws Exception {
        ContainerCredentialsJsonSchema containerCredentialsJsonSchema =
            new ContainerCredentialsBuilder()
                .withEncrypted(false)
                .build();

        String json = new ObjectMapper().writeValueAsString(containerCredentialsJsonSchema);

        assertThat(json, containsString("\"isEncrypted\":false"));
    }

    /**
     * Tests the deserialization of an isEncrypted property.
     * @throws Exception
     */
    @Test
    public void testDeserializingIsEncryptedProperty() throws Exception {
        String json = "{\"isEncrypted\":true}";

        ContainerCredentialsJsonSchema credentialsJson =
            new ObjectMapper().readValue(json, ContainerCredentialsJsonSchema.class);

        assertThat(credentialsJson.isEncrypted(), is(true));
    }

    /**
     * Tests the serialization of a credential property.
     * @throws Exception
     */
    @Test
    public void testSerializingCredentials() throws Exception {
        ContainerCredential credential = new ContainerCredential("id", "user", "password");

        ContainerCredentialsJsonSchema containerCredentialsJsonSchema =
            new ContainerCredentialsBuilder()
                .withCredentials(credential)
                .build();

        String json = new ObjectMapper().writeValueAsString(containerCredentialsJsonSchema);

        String credentialsJson = "\"credentials\":[{\"id\":\"id\",\"user\":\"user\",\"password\":\"password\"}]";
        assertThat(json, containsString(credentialsJson));
    }

    /**
     * Tests the deserialization of a credential property.
     * @throws Exception
     */
    @Test
    public void testDeserializeCredentials() throws Exception {
        String json = "{\"credentials\":[{\"id\":\"id\",\"user\":\"user\",\"password\":\"password\"}]}";

        ContainerCredentialsJsonSchema credentialsJsonSchema =
            new ObjectMapper().readValue(json, ContainerCredentialsJsonSchema.class);

        assertThat(credentialsJsonSchema.getCredentials(), hasSize(1));
        ContainerCredential credential = credentialsJsonSchema.getCredentials().get(0);
        assertThat(credential.getId(), is("id"));
        assertThat(credential.getUser(), is("user"));
        assertThat(credential.getPassword(), is("password"));
    }

    /**
     * Tests that a json string conforming to {@link ContainerCredentialsJsonSchema} is recognized as valid.
     */
    @Test
    public void testWellFormedJsonIsRecognizedAsValid() {
        String json =
            "{\"isEncrypted\":true},\"credentials\":[{\"id\":\"id\",\"user\":\"user\",\"password\":\"password\"}]}";

        assertThat(ContainerCredentialsJsonSchema.hasValidSchema(json), is(true));
    }

    /**
     * Tests that a json string not conforming to {@link ContainerCredentialsJsonSchema} is recognized as invalid.
     */
    @Test
    public void testNotWellFormedJsonIsRecognizedAsInvalid() {
        String json =
            "{\"wrong-prop-name\":true},\"credentials\":[{\"id\":\"id\"}]}";

        assertThat(ContainerCredentialsJsonSchema.hasValidSchema(json), is(false));
    }

}
