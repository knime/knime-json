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
 */
package org.knime.core.data.json;

import java.io.IOException;
import java.nio.file.FileStore;

import org.knime.core.data.DataCell;
import org.knime.core.data.v2.ValueFactory;
import org.knime.core.data.v2.filestore.TableOrFileStoreValueFactory;
import org.knime.core.table.access.StructAccess.StructReadAccess;
import org.knime.core.table.access.StructAccess.StructWriteAccess;
import org.knime.core.table.schema.VarBinaryDataSpec.ObjectDeserializer;
import org.knime.core.table.schema.VarBinaryDataSpec.ObjectSerializer;

import jakarta.json.JsonValue;

/**
 * A {@link ValueFactory} to (de-)serialize {@link JSONValue}s - in the form of {@link JSONCell} or {@link JSONBlobCell}
 * - in the columnar backend.
 *
 * {@link JSONBlobCell} instances will be written to a {@link FileStore} so that the table serialized by the columnar
 * backend does not become huge immediately. Smaller {@link JSONValue}s (= not {@link JSONBlobCell}s) are immediately
 * written into the table.
 *
 * @since 5.1
 * @author Carsten Haubold, KNIME GmbH, Konstanz, Germany
 * @author Jonas Klotz, KNIME GmbH, Berlin, Germany
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 */
public class JSONValueFactory extends TableOrFileStoreValueFactory<JSONValue> {

    static final ObjectSerializer<JSONValue> SERIALIZER = (out, value) -> out.writeUTF(value.toString());

    static final ObjectDeserializer<JSONValue> DESERIALIZER = in -> new JSONCellContent(in.readUTF(), false);

    /**
     * Create an instance of the {@link JSONValueFactory}
     */
    public JSONValueFactory() {
        super(SERIALIZER, DESERIALIZER);
    }

    private class JSONReadValue extends TableOrFileStoreReadValue implements JSONValue, JSONCellContentProvider {
        protected JSONReadValue(final StructReadAccess access) {
            super(access);
        }

        @Override
        public JsonValue getJsonValue() {
            return ((JSONValue)getDataCell()).getJsonValue();
        }

        @Override
        public JSONCellContent getJSONCellContent() {
            return ((JSONCell)getDataCell()).getJSONCellContent();
        }

        @Override
        protected DataCell createCell(final JSONValue data) {
            final JSONCellContent cellContent;
            // (DE)SERIALIZER is typed to JSONValue but is effectively JSONCellContent (see above), which represents
            // the Json string and only creates jakarta.json.JsonValue on access.
            if (data instanceof JSONCellContentProvider) {
                cellContent = ((JSONCellContentProvider)data).getJSONCellContent();
            } else {
                // of no practical relevance; 'real' JSONValue implementations also implement JSONCellContentProvider
                cellContent = new JSONCellContent(data.getJsonValue());
            }
            return new JSONCell(cellContent);
        }

        @Override
        protected ObjectSerializerFileStoreCell<?> createFileStoreCell(final Integer hashCode) {
            return new JSONFileStoreCell(hashCode);
        }
    }

    private class JSONWriteValue extends TableOrFileStoreWriteValue {
        protected JSONWriteValue(final StructWriteAccess access) {
            super(access);
        }

        @Override
        protected boolean isCorrespondingReadValue(final JSONValue value) {
            return value instanceof JSONReadValue;
        }

        @Override
        protected JSONFileStoreCell getFileStoreCell(final JSONValue value) throws IOException {
            if (value instanceof JSONFileStoreCell) {
                return (JSONFileStoreCell)value;
            } else if (value instanceof JSONBlobCell) {
                // Explicitly convert legacy blob cells to file store cells
                return new JSONFileStoreCell(createFileStore(), value);
            }
            return null;
        }
    }

    @Override
    public JSONReadValue createReadValue(final StructReadAccess access) {
        return new JSONReadValue(access);
    }

    @Override
    public JSONWriteValue createWriteValue(final StructWriteAccess access) {
        return new JSONWriteValue(access);
    }
}
