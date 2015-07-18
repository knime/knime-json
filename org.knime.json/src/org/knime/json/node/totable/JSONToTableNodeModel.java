package org.knime.json.node.totable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.MissingCell;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.json.JSONValue;
import org.knime.core.data.json.JacksonConversions;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.json.node.jsonpath.JsonPathUtil;
import org.knime.json.node.jsonpath.util.JsonPathUtils;
import org.knime.json.node.jsonpath.util.OutputKind;
import org.knime.json.node.jsonpath.util.SimplePathParser;
import org.knime.json.node.jsonpath.util.SimplePathParser.Path;
import org.knime.json.util.OutputType;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JacksonUtils;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.PathNotFoundException;
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;

/**
 * This is the model implementation of JSONToTable. Converts JSON values to new columns.
 * <p/>
 * It uses {@link TreeSet}/{@link TreeMap}s, so probably it were more efficient with tries, although jetty 6 did not
 * contain trie implementation, commons collection4 was not used previously, so it is not in the target platform.
 *
 * @author Gabor Bakos
 */
public class JSONToTableNodeModel extends NodeModel {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(JSONToTableNodeModel.class);

    private final JSONToTableSettings m_settings = new JSONToTableSettings();

    /**
     * Constructor for the node model.
     */
    protected JSONToTableNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws CanceledExecutionException {
        ExecutionContext columnSelectionContext = exec.createSubExecutionContext(.5);
        final DataTableSpec spec = inData[0].getSpec();
        ColumnRearranger rearranger = new ColumnRearranger(spec);

        int r = 0, all = inData[0].getRowCount();
        final int jsonIndex = spec.findColumnIndex(m_settings.getInputColumn());
        final JsonPath jsonPath = JsonPath.compile("$..*");
        final JacksonConversions conv = JacksonConversions.getInstance();
        final Configuration conf = Configuration.builder().options(Option.AS_PATH_LIST).build();
        final Map<String, OutputKind> kinds = new LinkedHashMap<>();
        boolean wasRoot = false;
        OutputKind rootKind = new OutputKind(true, null);
        kinds.put("$", rootKind);
        for (DataRow row : inData[0]) {
            columnSelectionContext.checkCanceled();
            columnSelectionContext.setProgress(r++ / (double)all, "Analysing: " + row.getKey());
            DataCell cell = row.getCell(jsonIndex);
            if (cell instanceof JSONValue) {
                JSONValue jv = (JSONValue)cell;
                List<Path> paths;
                try {
                    List<String> rawPaths = jsonPath.read(jv.getJsonValue().toString(), conf);
                    paths = new ArrayList<>(rawPaths.size());
                    for (String rawPath : rawPaths) {
                        paths.add(new SimplePathParser.Path(rawPath));
                    }
                } catch (PathNotFoundException e) {
                    LOGGER.debug("Warning: " + e.getMessage(), e);
                    paths = Collections.emptyList();
                    wasRoot = true;
                }
                TreeSet<Path> origPaths = new TreeSet<>(paths);
                filterArrays(paths);
                filterNonLeaves(paths, origPaths);
                for (Path path : paths) {
                    OutputKind kind = processValueAndPath(jv, path);
                    if (kinds.containsKey(path.toString())) {
                        OutputKind orig = kinds.get(path.toString());
                        boolean single = kind.isSingle() && orig.isSingle();
                        OutputType type = JsonPathUtils.commonRepresentation(orig.getType(), kind.getType());
                        kinds.put(path.toString(), new OutputKind(single, type));
                    } else {
                        kinds.put(path.toString(), kind);
                    }
                }
                OutputKind kind = processValueAndPath(jv, new Path("$"));
                rootKind = new OutputKind(rootKind.isSingle() && kind.isSingle(), JsonPathUtils.commonRepresentation(rootKind.getType(), kind.getType()));
            }
        }
        if (wasRoot) {
            //For root, we do not want collection of JSON values, that would be confusing.
            if (!rootKind.isSingle() && rootKind.getType() != null && rootKind.getType() == OutputType.Json) {
                rootKind = new OutputKind(true, OutputType.Json);
            }
            kinds.put("$", rootKind);
        } else {
            kinds.remove("$");
        }
        removeRedundant(kinds);
        ColumnRearranger dummyRearranger = new ColumnRearranger(spec);
        DataColumnSpec[] specs = new DataColumnSpec[kinds.size()];
        final Map<String, JsonPath> jsonPaths = new LinkedHashMap<>();
        {
            int i = 0;
            for (Entry<String, OutputKind> kindEntry : kinds.entrySet()) {
                final String proposedName = proposedName(kindEntry.getKey());
                //ENH rearranger createSpec in a loop might be really slow when there are many columns.
                final String realName = DataTableSpec.getUniqueColumnName(dummyRearranger.createSpec(), proposedName);
                columnSelectionContext.checkCanceled();
                dummyRearranger.append(new SingleCellFactory(new DataColumnSpecCreator(realName, IntCell.TYPE).createSpec()) {
                    @Override
                    public DataCell getCell(final DataRow row) {
                        assert false;
                        throw new IllegalStateException();
                    }
                });
                specs[i++] = new DataColumnSpecCreator(realName, kindEntry.getValue().getDataType()).createSpec();
                jsonPaths.put(kindEntry.getKey(), JsonPath.compile(kindEntry.getKey()));
            }
        }
        rearranger.append(new AbstractCellFactory(specs) {
            private final DataCell[] m_missingCells = new DataCell[kinds.size()];
            {
                Arrays.fill(m_missingCells, DataType.getMissingCell());
            }

            @Override
            public DataCell[] getCells(final DataRow row) {
                Configuration defaultConfiguration =
                    Configuration.defaultConfiguration()
                        .jsonProvider(new JacksonJsonNodeJsonProvider(JacksonUtils.newMapper()))
                        .mappingProvider(new JacksonMappingProvider(JacksonUtils.newMapper())).setOptions()/*.addOptions(Option.SUPPRESS_EXCEPTIONS)*/;
                DataCell cell = row.getCell(jsonIndex);
                if (cell instanceof JSONValue) {
                    final JSONValue jv = (JSONValue)cell;
                    final DataCell[] ret = new DataCell[kinds.size()];
                    int i = 0;
                    for (Entry<String, JsonPath> entry : jsonPaths.entrySet()) {
                        try {
                            Object unwrap = defaultConfiguration.jsonProvider().unwrap(conv.toJackson(jv.getJsonValue()));
                            Object read = entry.getValue().read(unwrap, defaultConfiguration);
                            ret[i++] =
                                m_settings.isOmitNestedObjects() ? JsonPathUtils.convertObjectToReturnTypeWithoutNestedObjects(read, kinds.get(entry.getKey()),
                                    defaultConfiguration, conv) : JsonPathUtils.convertObjectToReturnType(read, kinds.get(entry.getKey()),
                                    defaultConfiguration, conv);
                        } catch (RuntimeException e) {
                            ret[i++] = new MissingCell(e.getMessage());
                        }
                    }
                    return ret;
                }
                return m_missingCells;
            }
        });
        if (m_settings.isRemoveSourceColumn()) {
            rearranger.remove(jsonIndex);
        }
        ExecutionContext columnApplyContext = exec.createSubExecutionContext(.5);
        return new BufferedDataTable[]{columnApplyContext.createColumnRearrangeTable(inData[0], rearranger,
            columnApplyContext)};
    }

    /**
     * @param kinds
     */
    private void removeRedundant(final Map<String, OutputKind> kinds) {
        for (Iterator<Entry<String, OutputKind>> it = kinds.entrySet().iterator(); it.hasNext();) {
            String next = it.next().getKey();
            if (next.endsWith("[*]")) {
                if (kinds.containsKey(next.substring(0, next.length() - "[*]".length()))) {
                    it.remove();
                }
            }
        }
    }

    /**
     * Finds the common {@link OutputKind} for the {@code jv} {@link JSONValue} and the {@code path} specified.
     *
     * @param jv A {@link JSONValue}.
     * @param path The path to select.
     * @return The {@link OutputKind} of the values at that position.
     */
    protected OutputKind processValueAndPath(final JSONValue jv, final Path path) {
        Object object = JsonPath.read(jv.toString(), path.toString());
        JsonNode jackson = JsonPathUtil.toJackson(JacksonUtils.nodeFactory(), object);
        OutputKind kind = JsonPathUtils.kindOfJackson(jackson);
        if (!kind.isSingle() && m_settings.getArrayHandling() == ArrayHandling.KeepAllArrayAsJsonArray) {
            return new OutputKind(true, OutputType.Json);
        }
        if (m_settings.isOmitNestedObjects() && jackson.isArray()) {
                OutputType type = null;
                for (JsonNode jsonNode : jackson) {
                    OutputKind kindOfJackson = JsonPathUtils.kindOfJackson(jsonNode);
                    if (kindOfJackson.getType() == OutputType.Json) {
                        continue;
                    }
                    type = kindOfJackson.isSingle() ? JsonPathUtils.commonRepresentation(type, kindOfJackson.getType()) : OutputType.Json;
                }
                return new OutputKind(false, type);
        }
        return kind;
    }

    /**
     * @param paths
     */
    private void filterArrays(final List<Path> paths) {
        switch (m_settings.getArrayHandling()) {
            case KeepAllArrayAsJsonArray://intentional fall through
            case GenerateCollectionCells: {
                final LinkedHashSet<Path> result = new LinkedHashSet<>();
                for (Path path : paths) {
                    result.add(path.endsWithIndex() ? path.replaceLastWithStar() : path);
                }
                paths.clear();
                paths.addAll(result);
                LOGGER.debug(paths);
            }
                break;
            case GenerateColumns:
                break;
            default:
                break;
        }
    }

    /**
     * @param path A JSONPath path.
     * @return The converted, more user-friendly value.
     */
    private String proposedName(final String path) {
        StringBuilder ret = new StringBuilder(path.length());
        boolean withinQuotes = false, lastWasNumber = false;
        switch (m_settings.getColumnNameStrategy()) {
            case JsonPathWithCustomSeparator:
                for (int i = 1; i < path.length(); ++i) {
                    char c = path.charAt(i);
                    if (c == '\'') {
                        withinQuotes ^= true;
                        if (!withinQuotes) {
                            ret.append(m_settings.getSeparator());
                        }
                        continue;
                    }
                    if (!withinQuotes && Character.isDigit(c)) {
                        lastWasNumber = true;
                    }
                    if (c == ']' && lastWasNumber) {
                        ret.append(m_settings.getSeparator());
                    }
                    lastWasNumber = Character.isDigit(c);
                    if (withinQuotes || (c != '.' && c != '[' && c != ']')) {
                        ret.append(path.charAt(i));
                    }
                }
                if (path.length() > 2 && path.charAt(path.length() - 2) == '*' && ret.charAt(ret.length() - 1) == '*') {
                    ret.setLength(ret.length() - 1);
                }
                if (!m_settings.getSeparator().isEmpty()) {
                    while (ret.length() > 0 && ret.toString().endsWith(m_settings.getSeparator())) {
                        ret.setLength(ret.length() - m_settings.getSeparator().length());
                    }
                }
                if (ret.length() == 0) {
                    return "unnamed";
                }
                return ret.toString();
            case UniquifiedLeafNames:
                int lastQuote = path.lastIndexOf('\'');
                int prevQuote = path.lastIndexOf('\'', lastQuote - 1);
                if (prevQuote < 0) {
                    //array
                    return "unnamed";
                }
                return path.substring(prevQuote + 1, lastQuote);
            default:
                throw new UnsupportedOperationException("Unknown column naming strategy: "
                    + m_settings.getColumnNameStrategy());
        }
    }

    /**
     * @param paths The JSONPath paths containing possibly non-leaf paths too (which get remove when only leaves should
     *            be used).
     * @param origPaths The {@link Set} of original paths.
     * @see #m_onlyLeaves
     */
    private void filterNonLeaves(final List<Path> paths, final TreeSet<Path> origPaths) {
        switch (m_settings.getExpansion()) {
            case OnlyLeaves: {
                SortedMap<Path, List<Path>> origMap = new TreeMap<>();
                for (Path origPath : origPaths) {
                    //String starred = lastArrayIndicesToStar(origPath);
                    Path starred = origPath.lastIndexToStar();
                    if (!origMap.containsKey(starred)) {
                        origMap.put(starred, new ArrayList<Path>());
                    }
                    origMap.get(starred).add(origPath);
                }
                final TreeSet<Path> set = new TreeSet<>();
                for (Path path : paths) {
                    Path higher = set.higher(path);
                    Path lower = set.lower(path);
                    if (lower != null && (path.startsWith(lower) || path.lastIndexToStar().startsWith(lower))) {
                        //check whether all branches has a descendant or not
                        boolean allHasDescendant = true;
                        for (Path orig: origMap.containsKey(lower) ? origMap.get(lower) : Collections.<Path>emptyList()) {
                            Path h = origPaths.higher(orig);
                            allHasDescendant &= h != null && h.startsWith(orig);
                        }
                        //we have to keep if we have a leaf descendant
                        if (allHasDescendant) {
                            set.remove(lower);
                        }
                    }
                    if (higher == null || !higher.startsWith(path)) {
                        set.add(path);
                    }
                }
                paths.retainAll(set);
//                LOGGER.debug(set);
            }
                break;
            case OnlyUpTo: {
                final TreeSet<Path> set = new TreeSet<>();
                for (Path path : paths) {
                    if (countOf("][", path) < m_settings.getUpToNLevel()) {
                        set.add(path);
                    }
                }
                paths.retainAll(set);
            }
                break;
            default:
                break;
        }
    }

    /**
     * @param what
     * @param in
     * @return
     */
    private static int countOf(final String what, final Path in) {
        int lastIdx = 0, ret = 0;
        String path = in.toString();
        while ((lastIdx = path.indexOf(what, lastIdx + 1)) >= 0) {
            ret++;
        }
        return ret;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        //No internal state
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        if (!inSpecs[0].containsCompatibleType(JSONValue.class)) {
            throw new InvalidSettingsException("No JSON columns are available.");
        }
        DataColumnSpec selectedColumn = inSpecs[0].getColumnSpec(m_settings.getInputColumn());
        if (selectedColumn == null || !selectedColumn.getType().isCompatible(JSONValue.class)) {
            DataColumnSpec inputColumn = null;
            for (int i = 0; i < inSpecs[0].getNumColumns(); i++) {
                final DataColumnSpec spec = inSpecs[0].getColumnSpec(i);
                if (spec.getType().isCompatible(JSONValue.class)) {
                    if (inputColumn != null) {
                        setWarningMessage("There are multiple JSON columns. Automatically selected \"" + spec.getName()
                            + "\" as input column.");
                    }
                    inputColumn = spec;
                }
            }
            assert inputColumn != null : inSpecs[0];
            m_settings.setInputColumn(inputColumn == null ?/* should not happen */ null : inputColumn.getName());
        }
        //Cannot predict the new column names in advance.
        return new DataTableSpec[]{null};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_settings.loadSettingsModel(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        new JSONToTableSettings().loadSettingsModel(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
        //No internal state
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
        //No internal state
    }
}
