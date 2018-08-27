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
package org.knime.json.node.container.input.credentials;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.json.JsonValue;

import org.knime.core.data.json.container.credentials.ContainerCredential;
import org.knime.core.data.json.container.credentials.ContainerCredentialsJsonSchema;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.util.crypto.IEncrypter;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Class that converts a JsonValue representing container credentials to {@link ContainerCredentialsJsonSchema}.
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 * @since 3.6
 */
public class ContainerCredentialMapper {

    /**
     * Constructs a new mapper.
     *
     * @param json value to be mapped
     * @param encrypter encrypter
     * @return the mapped credentials
     * @throws InvalidSettingsException
     */
    public static List<ContainerCredential> toCredentials(final JsonValue json, final IEncrypter encrypter)
            throws InvalidSettingsException {
        ContainerCredentialsJsonSchema credentials = mapToContainerCredentials(json);
        if (credentials.isEncrypted()) {
            List<ContainerCredential> decryptedCredentialsList = new ArrayList<>();
            for (ContainerCredential containerCredential : credentials.getCredentials()) {
                decryptedCredentialsList.add(getDecryptedCredentials(encrypter, containerCredential));
            }
            return decryptedCredentialsList;
        } else {
            return credentials.getCredentials();
        }
    }

    private static ContainerCredentialsJsonSchema mapToContainerCredentials(final JsonValue json)
            throws InvalidSettingsException {
        try {
            return new ObjectMapper().readValue(json.toString(), ContainerCredentialsJsonSchema.class);
        } catch (IOException e) {
            throw new InvalidSettingsException("Could not parse JsonValue to credentials", e);
        }
    }

    private static ContainerCredential getDecryptedCredentials(
            final IEncrypter encrypter,
            final ContainerCredential credential) throws InvalidSettingsException {
        try {
            String decryptedPassword = encrypter.decrypt(credential.getPassword());
            return new ContainerCredential(credential.getId(), credential.getUser(), decryptedPassword);
        } catch (InvalidKeyException | BadPaddingException | IllegalBlockSizeException
                | InvalidAlgorithmParameterException | IOException e) {
            throw new InvalidSettingsException("Could not decrypt password", e);
        }
    }

}
