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
 *   Aug 3, 2018 (Tobias Urhaug, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.core.data.json.container.credentials;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.crypto.NoSuchPaddingException;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.junit.Test;
import org.knime.core.node.workflow.Credentials;
import org.knime.core.node.workflow.ICredentials;
import org.knime.core.util.crypto.Encrypter;
import org.knime.core.util.crypto.IEncrypter;
import org.knime.json.node.container.input.credentials.ContainerCredentialMapper;

/**
 * Test suite for mapping JsonValues to {@link ContainerCredentialsJsonSchema}.
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 */
public final class ContainerCredentialMapperTest {

    /**
     * Tests that a json value containing a single unencrypted credential is mapped correctly.
     * @throws Exception
     */
    @Test
    public void testSingleNotEncryptedJsonCredentialsIsMapped() throws Exception {
        JsonValue jsonValue =
            new ContainerCredentialsJsonValueBuilder()
                .withEncryption(false)
                .withCredentials(new ContainerCredential("id", "user", "password"))
                .build();

        List<ContainerCredential> mappedCredentials =
                ContainerCredentialMapper.toContainerCredentials(jsonValue, getEncrypter());

        assertEquals(1, mappedCredentials.size());
        ContainerCredential actualCredentials = mappedCredentials.get(0);
        assertEquals("id", actualCredentials.getId());
        assertEquals("user", actualCredentials.getUser());
        assertEquals("password", actualCredentials.getPassword());
    }

    /**
     * Tests that a json value containing multiple unencrypted credentials is mapped correctly.
     * @throws Exception
     */
    @Test
    public void testMultipleNotEncryptedJsonCredentialsAreMapped() throws Exception {
        JsonValue jsonValue =
            new ContainerCredentialsJsonValueBuilder()
                .withEncryption(false)
                .withCredentials(new ContainerCredential("id1", "user1", "password1"))
                .withCredentials(new ContainerCredential("id2", "user2", "password2"))
                .build();

        List<ContainerCredential> mappedCredentials =
                ContainerCredentialMapper.toContainerCredentials(jsonValue, getEncrypter());

        assertEquals(2, mappedCredentials.size());
        ContainerCredential firstCredentials = mappedCredentials.get(0);
        assertEquals("id1", firstCredentials.getId());
        assertEquals("user1", firstCredentials.getUser());
        assertEquals("password1", firstCredentials.getPassword());

        ContainerCredential secondCredentials = mappedCredentials.get(1);
        assertEquals("id2", secondCredentials.getId());
        assertEquals("user2", secondCredentials.getUser());
        assertEquals("password2", secondCredentials.getPassword());
    }

    /**
     * Tests that a json value containing a single encrypted credential is mapped correctly.
     * @throws Exception
     */
    @Test
    public void testSingleEncryptedJsonCredentialIsMappedAndDecrypted() throws Exception {
        String plainPassword = "password";
        IEncrypter encrypter = getEncrypter();
        String encryptedPassword = encrypter.encrypt(plainPassword);

        JsonValue jsonValue =
            new ContainerCredentialsJsonValueBuilder()
                .withEncryption(true)
                .withCredentials(new ContainerCredential("id1", "user1", encryptedPassword))
                .build();

        List<ContainerCredential> mappedCredentials =
                ContainerCredentialMapper.toContainerCredentials(jsonValue, encrypter);

        assertEquals(1, mappedCredentials.size());
        ContainerCredential firstCredentials = mappedCredentials.get(0);
        assertEquals("id1", firstCredentials.getId());
        assertEquals("user1", firstCredentials.getUser());
        assertEquals(plainPassword, firstCredentials.getPassword());
    }

    /**
     * Tests that a json value containing multiple encrypted credentials is mapped correctly.
     * @throws Exception
     */
    @Test
    public void testMultipleEncryptedJsonCredentialsAreMappedAndDecrypted() throws Exception {
        String plainPassword1 = "password1";
        String plainPassword2 = "password2";
        IEncrypter encrypter = getEncrypter();
        String encryptedPassword1 = encrypter.encrypt(plainPassword1);
        String encryptedPassword2 = encrypter.encrypt(plainPassword2);

        JsonValue jsonValue =
            new ContainerCredentialsJsonValueBuilder()
                .withEncryption(true)
                .withCredentials(new ContainerCredential("id1", "user1", encryptedPassword1))
                .withCredentials(new ContainerCredential("id2", "user2", encryptedPassword2))
                .build();

        List<ContainerCredential> mappedCredentials =
                ContainerCredentialMapper.toContainerCredentials(jsonValue, encrypter);

        assertEquals(2, mappedCredentials.size());

        ContainerCredential firstCredentials = mappedCredentials.get(0);
        assertEquals("id1", firstCredentials.getId());
        assertEquals("user1", firstCredentials.getUser());
        assertEquals("password1", firstCredentials.getPassword());

        ContainerCredential secondCredentials = mappedCredentials.get(1);
        assertEquals("id2", secondCredentials.getId());
        assertEquals("user2", secondCredentials.getUser());
        assertEquals("password2", secondCredentials.getPassword());
    }

    /**
     * Tests that a single credentials is encrypted and mapped to {@link ContainerCredentialsJsonSchema}.
     * @throws Exception
     */
    @Test
    public void testSingleCredentialsIsEncryptedAndMappedToJson() throws Exception {
        Credentials credentials = new Credentials("id", "user", "password");
        IEncrypter encrypter = getEncrypter();

        ContainerCredentialsJsonSchema containerCredentials =
                ContainerCredentialMapper.toContainerCredentialsJsonSchema(Arrays.asList(credentials), encrypter);

        assertTrue(containerCredentials.isEncrypted());

        List<ContainerCredential> mappedCredentials = containerCredentials.getCredentials();
        assertEquals(1, mappedCredentials.size());

        ContainerCredential containerCredential = mappedCredentials.get(0);
        assertEquals("id", containerCredential.getId());
        assertEquals("user", containerCredential.getUser());
        assertEquals("password", encrypter.decrypt(containerCredential.getPassword()));
    }

    /**
     * Tests that a multiple credentials are encrypted and mapped to {@link ContainerCredentialsJsonSchema}.
     * @throws Exception
     */
    @Test
    public void testMultipleCredentialsAreEncryptedAndMappedToJson() throws Exception {
        Credentials credentials1 = new Credentials("id1", "user1", "password1");
        Credentials credentials2 = new Credentials("id2", "user2", "password2");
        List<ICredentials> credentials = Arrays.asList(credentials1, credentials2);
        IEncrypter encrypter = getEncrypter();

        ContainerCredentialsJsonSchema containerCredentials =
                ContainerCredentialMapper.toContainerCredentialsJsonSchema(credentials, encrypter);

        assertTrue(containerCredentials.isEncrypted());

        List<ContainerCredential> mappedCredentials = containerCredentials.getCredentials();
        assertEquals(2, mappedCredentials.size());

        ContainerCredential containerCredential1 = mappedCredentials.get(0);
        assertEquals("id1", containerCredential1.getId());
        assertEquals("user1", containerCredential1.getUser());
        assertEquals("password1", encrypter.decrypt(containerCredential1.getPassword()));

        ContainerCredential containerCredential2 = mappedCredentials.get(1);
        assertEquals("id2", containerCredential2.getId());
        assertEquals("user2", containerCredential2.getUser());
        assertEquals("password2", encrypter.decrypt(containerCredential2.getPassword()));
    }

     /**
      * Tests that an empty list of credentials is mapped to {@link ContainerCredentialsJsonSchema}.
     * @throws Exception
     */
    @Test
     public void testEmptyListOfCredentials() throws Exception {
         IEncrypter encrypter = getEncrypter();

         ContainerCredentialsJsonSchema containerCredentials =
                 ContainerCredentialMapper.toContainerCredentialsJsonSchema(Collections.emptyList(), encrypter);

         List<ContainerCredential> mappedCredentials = containerCredentials.getCredentials();
         assertEquals(0, mappedCredentials.size());
     }

    private static IEncrypter getEncrypter() {
        try {
            return new Encrypter("encryption key");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeySpecException ex) {
            throw new RuntimeException("Could not create encrypter: " + ex.getMessage(), ex);
        }
    }

    static class ContainerCredentialsJsonValueBuilder {

        private boolean m_isEncrypted;
        private List<ContainerCredential> m_credentials;

        ContainerCredentialsJsonValueBuilder() {
            m_isEncrypted = false;
            m_credentials = new ArrayList<>();
        }

        ContainerCredentialsJsonValueBuilder withEncryption(final boolean isEncrypted) {
            m_isEncrypted = isEncrypted;
            return this;
        }

        ContainerCredentialsJsonValueBuilder withCredentials(final ContainerCredential credentials) {
            m_credentials.add(credentials);
            return this;
        }

        JsonValue build() {
            JsonBuilderFactory factory = Json.createBuilderFactory(null);

            return
                factory.createObjectBuilder()
                    .add("isEncrypted", m_isEncrypted)
                    .add("credentials", createCredentials(factory))
                    .build();
        }

        private JsonValue createCredentials(final JsonBuilderFactory factory) {
            JsonArrayBuilder arrayBuilder = factory.createArrayBuilder();

            for(ContainerCredential credentials : m_credentials) {
                JsonObject credentialJson =
                    factory.createObjectBuilder()
                        .add("id", credentials.getId())
                        .add("user", credentials.getUser())
                        .add("password", credentials.getPassword())
                        .build();
                arrayBuilder.add(credentialJson);
            }

            return arrayBuilder.build();
        }
    }
}
