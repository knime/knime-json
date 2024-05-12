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
 *   11 May 2023 (chaubold): created
 */
package org.knime.core.data.json;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.filestore.FileStore;
import org.knime.core.data.v2.filestore.NoOpSerializer;
import org.knime.core.data.v2.filestore.TableOrFileStoreValueFactory.ObjectSerializerFileStoreCell;
import org.knime.core.table.schema.VarBinaryDataSpec.ObjectDeserializer;
import org.knime.core.table.schema.VarBinaryDataSpec.ObjectSerializer;

import jakarta.json.JsonValue;

/**
 * A {@link DataCell} implementation of {@link JSONValue} that stores the data in a file store.
 *
 * @since 5.1
 * @author Carsten Haubold, KNIME GmbH, Konstanz, Germany
 * @author Jonas Klotz, KNIME GmbH, Berlin, Germany
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 */
public class JSONFileStoreCell extends ObjectSerializerFileStoreCell<JSONCellContent>
    implements JSONValue, StringValue, JSONCellContentProvider {

    private static final ObjectSerializer<JSONCellContent> SERIALIZER =
        (output, object) -> JSONValueFactory.SERIALIZER.serialize(output, object);

    private static final ObjectDeserializer<JSONCellContent> DESERIALIZER =
        input -> new JSONCellContent(JSONValueFactory.DESERIALIZER.deserialize(input).getJsonValue());

    /**
     * @param createFileStore
     * @param content
     */
    JSONFileStoreCell(final FileStore fs, final JSONValue content) {
        super(fs, new JSONCellContent(content.getJsonValue()), SERIALIZER, DESERIALIZER);
    }

    /**
     * Deserialization constructor, FileStore will be provided by framework
     */
    private JSONFileStoreCell() {
        this(null);
    }

    JSONFileStoreCell(final Integer hashCode) {
        super(SERIALIZER, DESERIALIZER, hashCode);
    }

    @Override
    public String getStringValue() {
        return getContent().getStringValue();
    }

    @Override
    protected boolean equalContent(final DataValue otherValue) {
        return JSONValue.equalContent(this, (JSONValue)otherValue);
    }

    @Override
    public JsonValue getJsonValue() {
        return getContent().getJsonValue();
    }

    @Override
    public String toString() {
        return getStringValue();
    }

    @Override
    public JSONCellContent getJSONCellContent() {
        return getContent();
    }

    /**
     * Serializer for {@link JSONFileStoreCell}s
     *
     * @noreference This class is not intended to be referenced by clients.
     */
    public static final class JSONSerializer extends NoOpSerializer<JSONFileStoreCell> {

        /** public for the extension point */
        public JSONSerializer() {
            super(JSONFileStoreCell::new);
        }
    }
}
