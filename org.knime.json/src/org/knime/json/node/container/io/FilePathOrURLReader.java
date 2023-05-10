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
 *   Feb 7, 2019 (Tobias Urhaug, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.json.node.container.io;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.util.FileUtil;
import org.knime.json.util.JSONUtil;

import jakarta.json.JsonValue;

/**
 * Class responsible for reading a file from its path or URL and parsing it to a {@link JsonValue}.
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 */
public class FilePathOrURLReader {

    /**
     * Resolves the provided URL or path to a file and parses it to a {@link JsonValue},
     * if the file is a valid json file and can be resolved.
     *
     * @param filePathOrURL the path or URL of the provided file
     * @return a {@link JsonValue} of the provided file, given it is a valid json file
     * @throws InvalidSettingsException if the provided file cannot be resolved or parsed to json
     */
    public static JsonValue resolveToJson(final String filePathOrURL) throws InvalidSettingsException {
        try (InputStream inputStream = FileUtil.openInputStream(filePathOrURL)){
            String fileAsString = IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
            return JSONUtil.parseJSONValue(fileAsString);
        } catch (IOException  e) {
            throw new InvalidSettingsException("The file \"" + filePathOrURL + "\" could not be resolved to json" , e);
        }
    }

}
