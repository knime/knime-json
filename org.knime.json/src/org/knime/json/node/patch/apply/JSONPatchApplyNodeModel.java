/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *   24 Sept. 2014 (Gabor): created
 */
package org.knime.json.node.patch.apply;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.knime.core.data.BooleanValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.LongValue;
import org.knime.core.data.MissingCell;
import org.knime.core.data.StringValue;
import org.knime.core.data.collection.CollectionDataValue;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.json.JSONCellFactory;
import org.knime.core.data.json.JSONValue;
import org.knime.core.data.json.JacksonConversions;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.streamable.MergeOperator;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.PortInput;
import org.knime.core.node.streamable.PortOutput;
import org.knime.core.node.streamable.RowInput;
import org.knime.core.node.streamable.StreamableFunction;
import org.knime.core.node.streamable.StreamableOperatorInternals;
import org.knime.core.node.streamable.simple.SimpleStreamableOperatorInternals;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.util.Pair;
import org.knime.json.node.patch.apply.parser.JsonLikeParser;
import org.knime.json.node.patch.apply.parser.JsonLikeParser.TableReferences;
import org.knime.json.node.util.SingleColumnReplaceOrAddNodeModel;

import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.POJONode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import com.github.fge.jsonpatch.mergepatch.JsonMergePatch;

/**
 * This is the model implementation of JSONTransformer. Changes {@link JSONValue}s.
 *
 * @author Gabor Bakos
 */
public final class JSONPatchApplyNodeModel extends SingleColumnReplaceOrAddNodeModel<JSONPatchApplySettings> {
    private static final String ROW_COUNT = "row count";

    private static final int MESSAGE_THRESHOLD = 5;

    private final Map<String, Integer> m_usedColumns = new LinkedHashMap<>();

    private long m_rowCount;

    private Boolean m_distributable;

    private boolean m_requireRowCount, m_usesRowKey;

    private final AtomicLong m_rowIndex = new AtomicLong();

    /**
     * Constructor for the node model.
     */
    protected JSONPatchApplyNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int[] findOtherIndices(final DataTableSpec inSpecs) {
        final Stream<DataColumnSpec> replaceCompatibleColumns = Arrays.stream(inSpecs.getColumnNames())
            .filter(n -> !n.contains("$")).map(v -> inSpecs.getColumnSpec(v));
        final String jsonPatchRaw = getSettings().getJsonPatch();
        try (final StringReader reader = new StringReader(jsonPatchRaw);
                final JsonLikeParser jsonLikeParser =
                    new JsonLikeParser(reader, Feature.collectDefaults() | Feature.ALLOW_COMMENTS.getMask(),
                        getAvailableInputFlowVariables().entrySet().stream()
                            .map(e -> Pair.create(e.getKey(), e.getValue().getType()))
                            .collect(Collectors.toMap(p -> p.getFirst(), p -> p.getSecond())),
                    replaceCompatibleColumns.map(c -> c.getName()).collect(Collectors.toSet()))) {
            final TreeNode valueAsTree = jsonLikeParser.readValueAsTree();
            final Set<String> usedColumnNames = collectColumnNames(valueAsTree);
            int[] ret = usedColumnNames.stream().mapToInt(n -> inSpecs.findColumnIndex(n)).sorted().toArray();
            m_usedColumns.clear();
            final String[] columnNames = inSpecs.getColumnNames();
            Arrays.stream(ret).forEach(i -> m_usedColumns.put(columnNames[i], i));
            return ret;
        } catch (final IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    /**
     * @param valueAsTree
     * @return
     */
    private Set<String> collectColumnNames(final TreeNode valueAsTree) {
        Set<String> ret = new LinkedHashSet<>();
        return collectColumnNames(valueAsTree, ret);
    }

    private Set<String> collectColumnNames(final TreeNode valueAsTree, final Set<String> res) {
        if (valueAsTree.isArray()) {
            for (JsonNode jsonNode : (ArrayNode)valueAsTree) {
                collectColumnNames(jsonNode, res);
            }
        } else if (valueAsTree.isObject()) {
            final ObjectNode objectNode = (ObjectNode)valueAsTree;
            for (final Iterator<Entry<String, JsonNode>> fields = objectNode.fields(); fields.hasNext();) {
                final Entry<String, JsonNode> entry = fields.next();
                collectColumnNames(entry.getValue(), res);
            }
        } else if (valueAsTree instanceof POJONode) {
            final POJONode pojo = (POJONode)valueAsTree;
            final Object pojoObject = pojo.getPojo();
            if (pojoObject instanceof String) {
                res.add((String)pojoObject);
            }
        }
        return res;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws Exception {
        m_rowCount = inData[getStreamableInPortIdx()].size();
        return super.execute(inData, exec);
    }

    /**
     * {@inheritDoc}
     *
     * @throws IOException
     */
    @Override
    protected CellFactory createCellFactory(final DataColumnSpec output, final int inputIndex,
        final int... otherColumns) throws IOException {
        if (m_usedColumns.size() > 0 || !isDistributable() || m_usesRowKey) {
            //Special case when rows should be used for replacement
            return createCellFactoryWithColumnReferences(output, inputIndex, otherColumns);
        }
        final JacksonConversions conv = JacksonConversions.getInstance();
        final String jsonPatchRaw = getSettings().getJsonPatch();
        //It is safe to use null for row and -1 for row index.
        final String jsonPatch = replaceReferences(jsonPatchRaw, getAvailableInputFlowVariables(), null, -1L);
        final JsonNode patchNode = conv.toJackson(((JSONValue)JSONCellFactory.create(jsonPatch, true)).getJsonValue());
        final JsonPatch patch;
        final JsonMergePatch mergePatch;
        final boolean keepOriginalForFailedTests;
        switch (getSettings().getPatchType()) {
            case JSONPatchApplySettings.PATCH_OPTION:
                patch = JsonPatch.fromJson(patchNode);
                boolean testCheck = getSettings().isKeepOriginalWhenTestFails();
                if (testCheck) {
                    testCheck = false;
                    CheckUtils.checkArgument(patchNode.isArray(), "Should be in array");
                    for (final JsonNode opNode: ((ArrayNode)patchNode)) {
                        final JsonNode opValue = opNode.get("op");
                        CheckUtils.checkArgument(opValue != null && !opValue.isNull(), "op is missing for some of the patch parts: " + opNode);
                        testCheck |= "test".equals(opValue.asText());
                    }
                }
                keepOriginalForFailedTests = testCheck;
                mergePatch = null;
                break;
            case JSONPatchApplySettings.MERGE_PATCH_OPTION:
                try {
                    keepOriginalForFailedTests = false;
                    mergePatch = JsonMergePatch.fromJson(patchNode);
                    patch = null;
                } catch (JsonPatchException e) {
                    throw new IOException("Wrong merge patch format: " + e.getMessage(), e);
                }
                break;
            default:
                throw new IllegalStateException("Not supported patch type: " + getSettings().getPatchType());
        }
        return new SingleCellFactory(output) {
            private int m_patchFailedCount = 0;

            @Override
            public DataCell getCell(final DataRow row) {
                DataCell cell = row.getCell(inputIndex);
                if (cell.isMissing()) {
                    return cell;
                }
                if (cell instanceof JSONValue) {
                    JSONValue jsonCell = (JSONValue)cell;
                    JsonNode jsonNode = conv.toJackson(jsonCell.getJsonValue());
                    try {
                        JsonNode applied;
                        if (patch == null) {
                            if (mergePatch != null) {
                                applied = mergePatch.apply(jsonNode);
                            } else {
                                throw new IllegalStateException();
                            }
                        } else {
                            applied = patch.apply(jsonNode);
                        }
                        return JSONCellFactory.create(conv.toJSR353(applied));
                    } catch (JsonPatchException e) {
                        m_patchFailedCount++;
                        logError(e, m_patchFailedCount);
                        return keepOriginalForFailedTests ? cell : new MissingCell(e.getMessage());
                    }
                }
                return DataType.getMissingCell();
            }

            /** {@inheritDoc} */
            @Override
            public void afterProcessing() {
                super.afterProcessing();
                if (m_patchFailedCount > 0) {
                    setWarningMessage("There were " + m_patchFailedCount + " rows where the transformation failed.");
                }
            }
        };
    }

    /**
     * @param e The {@link JsonPatchException} occurred.
     * @param patchFailedCount The number of failed transformations.
     */
    private void logError(final JsonPatchException e, final int patchFailedCount) {
        if (patchFailedCount < MESSAGE_THRESHOLD) {
            getLogger().warn(e.getMessage(), e);
        }
        if (patchFailedCount == MESSAGE_THRESHOLD) {
            getLogger().info("There were additional errors transforming JSON values.");
        }
    }

    /**
     * In case we use other columns, we shall need to recreate the transformation for each row.
     *
     * @param output The output {@link DataColumnSpec}.
     * @param inputIndex The input column's index.
     * @param otherColumns The index of output columns (unused).
     */
    private SingleCellFactory createCellFactoryWithColumnReferences(final DataColumnSpec output, final int inputIndex,
        final int[] otherColumns) {
        final JacksonConversions conv = JacksonConversions.getInstance();
        final String jsonPatchRaw = getSettings().getJsonPatch();
        return new SingleCellFactory(output) {
            private int m_patchFailedCount = 0;

            @Override
            public DataCell getCell(final DataRow row) {
                try {
                    final String jsonPatch = replaceReferences(jsonPatchRaw, getAvailableInputFlowVariables(), row,
                        m_rowIndex.getAndIncrement());
                    final JsonNode patchNode =
                        conv.toJackson(((JSONValue)JSONCellFactory.create(jsonPatch, true)).getJsonValue());
                    final JsonPatch patch;
                    final JsonMergePatch mergePatch;
                    switch (getSettings().getPatchType()) {
                        case JSONPatchApplySettings.PATCH_OPTION:
                            patch = JsonPatch.fromJson(patchNode);
                            mergePatch = null;
                            break;
                        case JSONPatchApplySettings.MERGE_PATCH_OPTION:
                            try {
                                mergePatch = JsonMergePatch.fromJson(patchNode);
                                patch = null;
                            } catch (JsonPatchException e) {
                                throw new IOException("Wrong merge patch format: " + e.getMessage(), e);
                            }
                            break;
                        default:
                            throw new IllegalStateException(
                                "Not supported patch type: " + getSettings().getPatchType());
                    }
                    DataCell cell = row.getCell(inputIndex);
                    if (cell instanceof JSONValue) {
                        JSONValue jsonCell = (JSONValue)cell;
                        JsonNode jsonNode = conv.toJackson(jsonCell.getJsonValue());
                        try {
                            JsonNode applied;
                            if (patch == null) {
                                if (mergePatch != null) {
                                    applied = mergePatch.apply(jsonNode);
                                } else {
                                    throw new IllegalStateException();
                                }
                            } else {
                                applied = patch.apply(jsonNode);
                            }
                            return JSONCellFactory.create(conv.toJSR353(applied));
                        } catch (JsonPatchException e) {
                            m_patchFailedCount++;
                            logError(e, m_patchFailedCount);
                            return new MissingCell(e.getMessage());
                        }
                    }
                    return DataType.getMissingCell();
                } catch (IOException e1) {
                    return new MissingCell(e1.getMessage());
                }
            }

            /** {@inheritDoc} */
            @Override
            public void afterProcessing() {
                super.afterProcessing();
                if (m_patchFailedCount > 0) {
                    setWarningMessage("There were " + m_patchFailedCount + " rows where the transformation failed.");
                }
            }
        };
    }

    /**
     * @param jsonPatchRaw The raw value of JSON Patch (unparsed String).
     * @param availableInputFlowVariables The available flow variables.
     * @param row The current {@link DataRow}, can be {@code null} when no column refrences are used.
     * @return The transformed JSON Patch/Merge Patch.
     */
    private String replaceReferences(final String jsonPatchRaw,
        final Map<String, FlowVariable> availableInputFlowVariables, final DataRow row, final long rowIndex) {
        try (final StringReader reader = new StringReader(jsonPatchRaw);
                final JsonLikeParser jsonLikeParser = createParser(reader)) {
            final TreeNode valueAsTree = jsonLikeParser.readValueAsTree();
            replaceReferences(valueAsTree, availableInputFlowVariables, row, rowIndex);
            return valueAsTree.toString();
        } catch (final IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    /**
     * @param reader A {@link Reader}.
     * @return The parser for the transformation rules.
     */
    private JsonLikeParser createParser(final StringReader reader) {
        return new JsonLikeParser(reader, Feature.collectDefaults() | Feature.ALLOW_COMMENTS.getMask(),
            getAvailableInputFlowVariables().entrySet().stream()
                .map(e -> Pair.create(e.getKey(), e.getValue().getType()))
                .collect(Collectors.toMap(p -> p.getFirst(), p -> p.getSecond())),
            m_usedColumns.keySet());
    }

    /**
     * @param input A mutable {@link TreeNode}.
     * @param availableInputFlowVariables The available flow variables.
     * @param row The current {@link DataRow}, can be {@code null} when no column refrences are used.
     */
    private JsonNode replaceReferences(final TreeNode input, final Map<String, FlowVariable> availableInputFlowVariables,
        final DataRow row, final long rowIndex) {
        if (input.isArray()) {
            ArrayNode arrayNode = (ArrayNode)input;
            for (int i = 0; i < arrayNode.size(); ++i) {
                arrayNode.set(i, replaceReferences(arrayNode.get(i), availableInputFlowVariables, row, rowIndex));
            }
            return arrayNode;
        } else if (input.isObject()) {
            final ObjectNode objectNode = (ObjectNode)input;
            final Set<String> toBeChangedKeys = new LinkedHashSet<>();
            for (final Iterator<Entry<String, JsonNode>> fields = objectNode.fields(); fields.hasNext();) {
                final Entry<String, JsonNode> entry = fields.next();
                if (entry.getValue() instanceof POJONode) {
                    toBeChangedKeys.add(entry.getKey());
                }
                replaceReferences(entry.getValue(), availableInputFlowVariables, row, rowIndex);
            }
            for (final String key : toBeChangedKeys) {
                objectNode.set(key, replaceReferences(objectNode.get(key), availableInputFlowVariables, row, rowIndex));
            }
        } else if (input instanceof POJONode) {
            final Object pojo = ((POJONode)input).getPojo();
            JsonNode newValue = NullNode.getInstance();
            if (pojo instanceof String) {
                final String colName = (String)pojo;
                final int colIndex = m_usedColumns.getOrDefault(colName, -1);
                final DataCell cell = row.getCell(colIndex);
                newValue = cellToJsonNode(JsonNodeFactory.withExactBigDecimals(true), cell);

            } else if (pojo instanceof Pair<?, ?>) {
                final Pair<?, ?> fv = (Pair<?, ?>)pojo;
                final FlowVariable variable = availableInputFlowVariables.get(fv.getFirst());
                switch (variable.getType()) {
                    case DOUBLE:
                        newValue = DoubleNode.valueOf(variable.getDoubleValue());
                        break;
                    case INTEGER:
                        newValue = IntNode.valueOf(variable.getIntValue());
                        break;
                    case STRING:
                        newValue = TextNode.valueOf(variable.getStringValue());
                        break;
                    default:
                        throw new UnsupportedOperationException("Not supported type: " + variable.getType());
                }
            } else if (pojo instanceof TableReferences) {
                switch ((TableReferences)pojo) {
                    case RowId:
                        newValue = TextNode.valueOf(row.getKey().getString());
                        break;
                    case RowCount:
                        newValue = LongNode.valueOf(m_rowCount);
                        break;
                    case RowIndex:
                        newValue = LongNode.valueOf(rowIndex);
                        break;
                    default:
                        throw new IllegalStateException("Unknown table reference: " + pojo);
                }
            }
            return newValue;
        }
        return (JsonNode)input;
    }

    /**
     * @param nf A JsonNodeFactory to create array nodes.
     * @param cell The DataCell to convert.
     * @return The converted JsonNode.
     */
    private JsonNode cellToJsonNode(final JsonNodeFactory nf, final DataCell cell) {
        if (cell.isMissing()) {
            return NullNode.getInstance();
        } else if (cell instanceof BooleanValue) {
            return BooleanNode.valueOf(((BooleanValue)cell).getBooleanValue());
        } else if (cell instanceof IntValue) {
            final IntValue iv = (IntValue)cell;
            return IntNode.valueOf(iv.getIntValue());
        } else if (cell instanceof LongValue) {
            final LongValue lv = (LongValue)cell;
            return LongNode.valueOf(lv.getLongValue());
        } else if (cell instanceof DoubleValue) {
            final DoubleValue dv = (DoubleValue)cell;
            return DoubleNode.valueOf(dv.getDoubleValue());
        } else if (cell instanceof JSONValue) {
            final JSONValue jv = (JSONValue)cell;
            return JacksonConversions.getInstance().toJackson(jv.getJsonValue());
        } else if (cell instanceof StringValue) {
            final StringValue sv = (StringValue)cell;
            return TextNode.valueOf(sv.getStringValue());
        } else if (cell instanceof CollectionDataValue) {
            final CollectionDataValue cdv = (CollectionDataValue)cell;
            final List<JsonNode> items = new ArrayList<>();
            for (final DataCell dataCell : cdv) {
                items.add(cellToJsonNode(nf, dataCell));
            }
            return new ArrayNode(nf, items);
        }
        return TextNode.valueOf(cell.toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_rowCount = -1L;
        m_rowIndex.set(0L);
        m_distributable = null;
        m_requireRowCount = false;
        m_usesRowKey = false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        super.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.loadValidatedSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        //No internal state.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        //No internal state.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected JSONPatchApplySettings createSettings() {
        return createJSONPatchApplySetting();
    }

    /**
     * @return
     */
    static JSONPatchApplySettings createJSONPatchApplySetting() {
        return new JSONPatchApplySettings();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isDistributable() {
        if (m_distributable != null) {
            return m_distributable.booleanValue();
        }
        final String jsonPatch = getSettings().getJsonPatch();
        try (final StringReader reader = new StringReader(jsonPatch);
                final JsonLikeParser jsonLikeParser = createParser(reader)) {
            final TreeNode patch = jsonLikeParser.readValueAsTree();
            m_distributable = !hasRowIndexReference(patch);
            return m_distributable.booleanValue();
        } catch (final IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    /**
     * @param input A JSON Patch segment parsed.
     * @return Whether it has a reference to {@value TableReferences#RowIndex}.
     */
    private boolean hasRowIndexReference(final TreeNode input) {
        if (input.isArray()) {
            for (JsonNode jsonNode : (ArrayNode)input) {
                if (hasRowIndexReference(jsonNode)) {
                    return true;
                }
            }
        } else if (input.isObject()) {
            final ObjectNode objectNode = (ObjectNode)input;
            for (final Iterator<Entry<String, JsonNode>> fields = objectNode.fields(); fields.hasNext();) {
                final Entry<String, JsonNode> entry = fields.next();
                if (hasRowIndexReference(entry.getValue())) {
                    return true;
                }
            }
        } else if (input instanceof POJONode) {
            final Object pojo = ((POJONode)input).getPojo();
            m_requireRowCount |= pojo == TableReferences.RowCount;
            m_usesRowKey |= pojo == TableReferences.RowId;
            return pojo == TableReferences.RowIndex;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StreamableOperatorInternals createInitialStreamableOperatorInternals() {
        return new SimpleStreamableOperatorInternals();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean iterate(final StreamableOperatorInternals internals) {
        SimpleStreamableOperatorInternals ssoi = (SimpleStreamableOperatorInternals)internals;
        return !ssoi.getConfig().containsKey(ROW_COUNT) && m_requireRowCount;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StreamableFunction createStreamableOperator(final PartitionInfo partitionInfo,
        final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        if (partitionInfo.getPartitionCount() > 1) {
            //Computes also m_requireRowCount, m_usesRowKey on remote machines
            isDistributable();
        }
        if (!m_requireRowCount) {
            return super.createStreamableOperator(partitionInfo, inSpecs);
        }
        return new StreamableFunction(0, 0) {
            private SimpleStreamableOperatorInternals m_internals;

            /**
             * {@inheritDoc}
             */
            @Override
            public void loadInternals(final StreamableOperatorInternals internals) {
                m_internals = (SimpleStreamableOperatorInternals)internals;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void runIntermediate(final PortInput[] inputs, final ExecutionContext exec) throws Exception {
                //count number of rows
                long count = 0;
                RowInput rowInput = (RowInput)inputs[0];
                while (rowInput.poll() != null) {
                    count++;
                }
                m_internals.getConfig().addLong(ROW_COUNT, count);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public StreamableOperatorInternals saveInternals() {
                return m_internals;
            }

            @Override
            public void runFinal(final PortInput[] inputs, final PortOutput[] outputs, final ExecutionContext exec)
                throws Exception {
                if (m_internals.getConfig().containsKey(ROW_COUNT)) {
                    m_rowCount = m_internals.getConfig().getLong(ROW_COUNT);
                }
                createColumnRearranger(((RowInput)inputs[0]).getDataTableSpec()).createStreamableFunction()
                    .runFinal(inputs, outputs, exec);
            }

            @Override
            public DataRow compute(final DataRow input) throws Exception {
                throw new IllegalStateException("Should not be called");
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MergeOperator createMergeOperator() {
        return new MergeOperator() {

            /**
             * {@inheritDoc}
             */
            @Override
            public StreamableOperatorInternals mergeIntermediate(final StreamableOperatorInternals[] operators) {
                //sum up the row counts if necessary
                long count = 0;
                for (int i = 0; i < operators.length; i++) {
                    SimpleStreamableOperatorInternals simpleInternals = (SimpleStreamableOperatorInternals)operators[i];
                    CheckUtils.checkState(simpleInternals.getConfig().containsKey(ROW_COUNT),
                        "Config for key " + ROW_COUNT + " isn't set.");
                    try {
                        count += simpleInternals.getConfig().getLong(ROW_COUNT);
                    } catch (InvalidSettingsException e) {
                        // should not happen since we checked already
                        throw new RuntimeException(e);
                    }
                }

                SimpleStreamableOperatorInternals res = new SimpleStreamableOperatorInternals();
                if (count > 0) {
                    res.getConfig().addLong(ROW_COUNT, count);
                }
                m_rowCount = count;
                return res;
            }

            @Override
            public StreamableOperatorInternals mergeFinal(final StreamableOperatorInternals[] operators) {
                //nothing to do here
                return null;
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void finishStreamableExecution(final StreamableOperatorInternals internals, final ExecutionContext exec,
        final PortOutput[] output) throws Exception {
        //TODO warnings?
    }
}
