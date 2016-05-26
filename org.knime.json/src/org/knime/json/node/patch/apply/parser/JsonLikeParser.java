/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
 *   14 May 2016 (Gabor Bakos): created
 */
package org.knime.json.node.patch.apply.parser;

import java.io.IOException;
import java.io.Reader;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.FlowVariable.Type;
import org.knime.core.util.Pair;
import org.knime.ext.sun.nodes.script.expression.Expression;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.json.ReaderBasedJsonParser;
import com.fasterxml.jackson.core.sym.CharsToNameCanonicalizer;
import com.fasterxml.jackson.core.util.BufferRecycler;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A {@link JsonParser}, that can read flow variable or column references in values of objects' key/value pairs.
 *
 * @author Gabor Bakos
 */
public class JsonLikeParser extends ReaderBasedJsonParser {
    /**
     * Reference for a table property.
     */
    public enum TableReferences {
        /** {@code 0}-based index */
        RowIndex,
        /** Number of rows */
        RowCount,
        /** Row key/id. */
        RowId;
    }

    private static final String ROWINDEX = "$" + Expression.ROWINDEX, ROWCOUNT = "$" + Expression.ROWCOUNT,
            ROWID = "$" + Expression.ROWID;

    private Map<String, Type> m_flowVariables;

    private Set<String> m_compatibleColumnNames;

    /**
     * @param reader A {@link Reader} of the content.
     * @param features The {@link com.fasterxml.jackson.core.JsonParser.Feature}s of the parser.
     * @param flowVariables The known flow variables. (Should not contain {@code $} sign,)
     * @param compatibleColumnNames The compatible column names. (Should not contain {@code $} sign,)
     *
     */
    public JsonLikeParser(final Reader reader, final int features, final Map<String, Type> flowVariables,
        final Set<String> compatibleColumnNames) {
        super(new IOContext(new BufferRecycler(), reader, false), features, reader, new ObjectMapper(),
            CharsToNameCanonicalizer.createRoot());
        CheckUtils.checkArgument(!compatibleColumnNames.stream().anyMatch(v -> v.contains("$")),
            "Column names with $ in it are not supported.");
        CheckUtils.checkArgument(!flowVariables.keySet().stream().anyMatch(v -> v.contains("}")),
            "Flow variable names with $$ in it are not supported.");
        m_flowVariables = new LinkedHashMap<>(flowVariables);
        m_compatibleColumnNames = new LinkedHashSet<>(compatibleColumnNames);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected JsonToken _handleOddValue(final int i) throws IOException {
        if (i == '$') {
            return handleDollar();
        }
        return super._handleOddValue(i);
    }

    /**
     * Handles the case when the token started with a {@code $} sign.
     *
     * @return The {@link JsonToken#VALUE_EMBEDDED_OBJECT} upon successful parse.
     * @throws IOException Reached EOF.
     * @throws JsonParseException Wrong input.
     */
    private JsonToken handleDollar() throws JsonParseException, IOException {
        //Based on _handleApos()
        char[] outBuf = _textBuffer.emptyAndGetCurrentSegment();
        int outPtr = _textBuffer.getCurrentSegmentSize();
        boolean isColumnReference = true;
        char flowVarType = 0;
        int position = -1, numDollars = 0;

        while (true) {
            ++position;
            if (_inputPtr >= _inputEnd) {
                if (!loadMore()) {
                    _reportInvalidEOF(": was expecting closing quote for a string value");
                }
            }
            char c = _inputBuffer[_inputPtr++];
            int i = c;
            if (i <= '\\') {
                if (i == '\\') {
                    /* Although chars outside of BMP are to be escaped as
                     * an UTF-16 surrogate pair, does that affect decoding?
                     * For now let's assume it does not.
                     */
                    c = _decodeEscaped();
                } else if (i <= '\'') {
                    if (c == '$') {
                        if (position == 0) {
                            isColumnReference = false;
                            ++numDollars;
                            flowVarType = 1;
                            outBuf[outPtr++] = c;
                            continue;
                        }
                        //End of column reference
                        if (isColumnReference) {
                            break;
                        }
                        if (numDollars < 2) {
                            numDollars++;
                            continue;
                        }
                        break;
                    }
                    if (i < INT_SPACE) {
                        _throwUnquotedSpace(i, "string value");
                    }
                }
                if (flowVarType == 2) {
                    switch (c) {
                        case 'S':
                        case 'I':
                        case 'D':
                            flowVarType = c;
                            break;
                        default:
                            _reportUnexpectedChar(c, "Expected S, I or D referring to the flow variable's type.");
                    }
                }
            }
            if (flowVarType == 1) {
                final boolean notTableRef = c != 'R';
                if (c != '{' && notTableRef) {
                    _reportUnexpectedChar(c, "Expected '{' as part of a flow variable reference");
                }
                if (notTableRef) {
                    flowVarType = 2;
                } else {
                    flowVarType = 3;
                }
            }
            // Need more room?
            if (outPtr >= outBuf.length) {
                outBuf = _textBuffer.finishCurrentSegment();
                outPtr = 0;
            }
            // Ok, let's add char to output:
            outBuf[outPtr++] = c;
        }
        _textBuffer.setCurrentLength(outPtr);
        return JsonToken.VALUE_EMBEDDED_OBJECT;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getEmbeddedObject() throws IOException {
        String str = _textBuffer.contentsAsString();
        if (ROWINDEX.equals(str)) {
            return TableReferences.RowIndex;
        }
        if (ROWCOUNT.equals(str)) {
            return TableReferences.RowCount;
        }
        if (ROWID.equals(str)) {
            return TableReferences.RowId;
        }
        if (str.startsWith("${") && str.length() > "${X}".length()) {
            String name = str.substring("${X".length(), str.length() - 1);
            if (str.charAt(str.length() - 1) != '}') {
                _reportError("Expected } after the flow variable's name: " + str);
            }
            switch (str.charAt("${".length())) {
                case 'S':
                    if (m_flowVariables.get(name) == Type.STRING) {
                        return Pair.create(name, Type.STRING);
                    }
                    break;
                case 'D':
                    if (m_flowVariables.get(name) == Type.DOUBLE) {
                        return Pair.create(name, Type.DOUBLE);
                    }
                    break;
                case 'I':
                    if (m_flowVariables.get(name) == Type.INTEGER) {
                        return Pair.create(name, Type.INTEGER);
                    }
                    break;
                default:
                    _reportError("Not a flow variable: " + str);
            }
            _reportError("Not a flow variable: " + str);
        }
        if (m_compatibleColumnNames.contains(str)) {
            return str;
        }
        _reportError("Unknown column: " + str);
        //Just to make the compiler happy:
        throw new IllegalStateException();
    }
}
