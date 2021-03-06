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
 *   Apr 26, 2019 (Tobias Urhaug, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.json.node.container.io;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.util.FileUtil;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Class responsible for writing an object as a json file to the provided path or URL.
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 */
public class FilePathOrURLWriter {

    /**
     * Writes the provided object as a json file to the given path or URL.
     *
     * @param outputPathOrUrl destination to which the object should be written
     * @param value the value to be written as a json file
     * @throws IOException if the provided object cannot be parsed to json
     * @throws URISyntaxException if the provided path or URL is not valid
     */
    public static void writeAsJson(final String outputPathOrUrl, final Object value)
            throws IOException, URISyntaxException {
        URL url = FileUtil.toURL(outputPathOrUrl);
        if (isLocalURL(url)) {
            Path path = FileUtil.resolveToPath(url);
            try (OutputStream outputStream = Files.newOutputStream(path)) {
                new ObjectMapper().writeValue(outputStream, value);
            }
        } else {
            try (OutputStream outputStream = FileUtil.openOutputStream(url, "PUT")) {
                new ObjectMapper().writeValue(outputStream, value);
            }
        }
    }

    private static boolean isLocalURL(final URL url) {
        return StringUtils.equalsIgnoreCase(url.getProtocol(), "knime")
                || StringUtils.equalsIgnoreCase(url.getProtocol(), "file");
    }

}
