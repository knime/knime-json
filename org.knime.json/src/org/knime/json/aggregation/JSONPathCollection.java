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
 *   29 2014 Sept (Gabor): created
 */
package org.knime.json.aggregation;

import java.awt.Component;
import java.awt.FlowLayout;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import org.knime.base.data.aggregation.AggregationOperator;
import org.knime.base.data.aggregation.GlobalSettings;
import org.knime.base.data.aggregation.OperatorColumnSettings;
import org.knime.base.data.aggregation.OperatorData;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.json.JSONCell;
import org.knime.core.data.json.JSONCellFactory;
import org.knime.core.data.json.JSONValue;
import org.knime.core.data.json.JacksonConversions;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.json.internal.Activator;
import org.knime.json.node.util.GUIFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jackson.JacksonUtils;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;

/**
 * JSONPath aggregation with option to create either JSON value or collections.
 *
 * @author Gabor Bakos
 */
public final class JSONPathCollection extends AggregationOperator {
    private static final String RESULT_TYPE = "result.type", JSON_PATH = "JSON-path", DEFAULT_JSON_PATH = "$..*",
            EMPTY_LEAF_AS_MISSING = "empty leaf as missing";

    private static final String JSON_ARRAY = "json array", LIST = "list", LIST_OF_LISTS = "list of lists";

    private JRadioButton m_jsonArray = new JRadioButton("JSON array"), m_list = new JRadioButton("List"),
            m_listOfLists = new JRadioButton("List of lists");

    private final JRadioButton m_emptyIsMissing = new JRadioButton("missing"),
            m_emptyIsError = new JRadioButton("fail");

    private final JTextField m_jsonPathField = GUIFactory.createTextField("", 22);

    private Aggregate m_aggregate;

    private String m_jsonPath = DEFAULT_JSON_PATH, m_resultType = LIST;
    {
        m_jsonPathField.setText(DEFAULT_JSON_PATH);
        m_list.setSelected(true);
        m_emptyIsMissing.setSelected(true);
    }

    private boolean m_emptyLeafAsMissing;

    private JsonPath m_jsonPathCompiled;

    private Configuration m_jsonPathConfiguration = defaultConfig();

    /**
     * @return
     */
    private Configuration defaultConfig() {
        return Activator.getInstance().getJsonPathConfiguration().setOptions(Option.ALWAYS_RETURN_LIST);
    }

    static interface Aggregate {
        void init();

        void addItems(List<Object> values);

        DataCell result();
    }

    static class JSONArrayAggregate implements Aggregate {
        private ArrayNode m_array = createArrayNode();

        /**
         * {@inheritDoc}
         */
        @Override
        public void init() {
            m_array = createArrayNode();
        }

        /**
         * @return
         */
        private ArrayNode createArrayNode() {
            return new ArrayNode(JacksonUtils.nodeFactory());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void addItems(final List<Object> values) {
            ArrayList<JsonNode> nodes = new ArrayList<>(values.size());
            JsonNodeFactory nodeFactory = JacksonUtils.nodeFactory();
            for (Object object : values) {
                if (object instanceof JsonNode) {
                    JsonNode node = (JsonNode)object;
                    nodes.add(node);
                } else {
                    nodes.add(toJacksonNode(object, nodeFactory));
                }
            }
            m_array.addAll(nodes);
        }

        /**
         * @param object
         * @param nodeFactory
         * @return
         */
        private JsonNode toJacksonNode(final Object object, final JsonNodeFactory nodeFactory) {
            if (object instanceof JsonNode) {
                JsonNode n = (JsonNode)object;
                return n;
            }
            if (object instanceof Map<?, ?>) {
                Map<?, ?> m = (Map<?, ?>)object;
                Map<String, JsonNode> newMap = new LinkedHashMap<String, JsonNode>(m.size());
                ObjectNode objectNode = nodeFactory.objectNode();
                for (Entry<?, ?> entry : m.entrySet()) {
                    newMap.put(entry.getKey().toString(), toJacksonNode(entry.getValue(), nodeFactory));
                }
                objectNode.setAll(newMap);
                return objectNode;
            }
            if (object instanceof String) {
                String str = (String)object;
                return nodeFactory.textNode(str);
            }
            if (object instanceof Integer) {
                Integer i = (Integer)object;
                return nodeFactory.numberNode(i);
            }
            if (object instanceof Long) {
                Long l = (Long)object;
                return nodeFactory.numberNode(l);
            }
            if (object instanceof Short) {
                Short s = (Short)object;
                return nodeFactory.numberNode(s);
            }
            if (object instanceof byte[]) {
                byte[] bs = (byte[])object;
                return nodeFactory.binaryNode(bs);
            }
            if (object instanceof Double) {
                Double d = (Double)object;
                return nodeFactory.numberNode(d);
            }
            if (object instanceof Float) {
                Float f = (Float)object;
                return nodeFactory.numberNode(f);
            }
            if (object instanceof BigDecimal) {
                BigDecimal bd = (BigDecimal)object;
                return nodeFactory.numberNode(bd);
            }
            if (object instanceof BigInteger) {
                BigInteger bi = (BigInteger)object;
                return nodeFactory.numberNode(bi);
            }
            if (object instanceof Boolean) {
                Boolean b = (Boolean)object;
                return nodeFactory.booleanNode(b);
            }
            if (object instanceof Object[]) {
                Object[] os = (Object[])object;
                ArrayNode arrayNode = nodeFactory.arrayNode();
                int i = 0;
                for (Object obj : os) {
                    arrayNode.insert(i++, toJacksonNode(obj, nodeFactory));
                }
                return arrayNode;
            }
            if (object instanceof Collection<?>) {
                Collection<?> c = (Collection<?>)object;
                ArrayNode arrayNode = nodeFactory.arrayNode();
                int i = 0;
                for (Object obj : c) {
                    arrayNode.insert(i++, toJacksonNode(obj, nodeFactory));
                }
                return arrayNode;
            }
            if (object == null) {
                return nodeFactory.nullNode();
            }
            throw new UnsupportedOperationException("Not supported object type: " + object.getClass() + " (" + object
                + ")");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DataCell result() {
            return JSONCellFactory.create(JacksonConversions.getInstance().toJSR353(m_array));
        }
    }

    class ListAggregate implements Aggregate {
        private final List<DataCell> m_cells = new ArrayList<>();

        /**
         * {@inheritDoc}
         */
        @Override
        public void init() {
            m_cells.clear();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void addItems(final List<Object> values) {
            StringBuilder sb = new StringBuilder();
            String delimiter = getGlobalSettings().getValueDelimiter();
            for (Object object : values) {
                sb.append(object).append(delimiter);
            }
            if (sb.length() > 0) {
                sb.setLength(sb.length() - delimiter.length());
            }
            m_cells.add(new StringCell(sb.toString()));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DataCell result() {
            return CollectionCellFactory.createListCell(m_cells);
        }
    }

    static class ListOfListsAggregate implements Aggregate {
        private final List<DataCell> m_cells = new ArrayList<>();

        /**
         * {@inheritDoc}
         */
        @Override
        public void init() {
            m_cells.clear();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void addItems(final List<Object> values) {
            List<DataCell> cells = new ArrayList<>();
            for (Object object : values) {
                cells.add(object == null ? DataType.getMissingCell() : new StringCell(object.toString()));
            }
            m_cells.add(CollectionCellFactory.createListCell(cells));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DataCell result() {
            return CollectionCellFactory.createListCell(m_cells);
        }

    }

    /**
     *
     */
    public JSONPathCollection() {
        this(createOperatorData());
    }

    /**
     * @return
     */
    private static OperatorData createOperatorData() {
        return new OperatorData("JSONPath", false, false, JSONValue.class, false);
    }

    /**
     * @param operatorData
     */
    public JSONPathCollection(final OperatorData operatorData) {
        this(operatorData, GlobalSettings.DEFAULT, OperatorColumnSettings.DEFAULT_EXCL_MISSING);
    }

    /**
     * @param operatorData
     * @param globalSettings
     * @param opColSettings
     */
    public JSONPathCollection(final OperatorData operatorData, final GlobalSettings globalSettings,
        final OperatorColumnSettings opColSettings) {
        super(operatorData, globalSettings, AggregationOperator.setInclMissingFlag(opColSettings, false));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Selects values from JSON specified by path and returns the results either as JSON array or a collection";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AggregationOperator createInstance(final GlobalSettings globalSettings,
        final OperatorColumnSettings opColSettings) {
        JSONPathCollection ret =
            new JSONPathCollection(createOperatorData(), globalSettings, AggregationOperator.setInclMissingFlag(
                opColSettings, false));
        ret.m_jsonPathCompiled = m_jsonPathCompiled;
        ret.m_aggregate = m_aggregate;
        ret.m_jsonPath = m_jsonPath;
        ret.m_jsonPathField.setText(m_jsonPath);
        ret.m_resultType = m_resultType;
        try {
            ret.resultTypeLoad(ret.m_resultType);
        } catch (NotConfigurableException | RuntimeException e) {
            //Should not happen
        }
        ret.m_emptyLeafAsMissing = m_emptyLeafAsMissing;
        ret.m_emptyIsMissing.setSelected(m_emptyLeafAsMissing);
        ret.m_emptyIsError.setSelected(!m_emptyLeafAsMissing);
        ret.updateEmptyLeafHandling();
        ret.m_jsonPathConfiguration = m_jsonPathConfiguration;
        return ret;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean computeInternal(final DataCell cell) {
        if (cell instanceof JSONValue) {
            JSONValue jv = (JSONValue)cell;
            String json = JacksonConversions.getInstance().toJackson(jv.getJsonValue()).toString();
            List<Object> res = m_jsonPathCompiled.read(json, m_jsonPathConfiguration);
            m_aggregate.addItems(res);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataType getDataType(final DataType origType) {
        if (!origType.isCompatible(JSONValue.class)) {
            throw new UnsupportedOperationException("Not supported datatype: " + origType);
        }
        switch (m_resultType) {
            case JSON_ARRAY:
                return JSONCell.TYPE;
            case LIST:
                return ListCell.getCollectionType(StringCell.TYPE);
            case LIST_OF_LISTS:
                return ListCell.getCollectionType(ListCell.getCollectionType(StringCell.TYPE));
            default:
                throw new UnsupportedOperationException("Not supported result type: " + m_resultType);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataCell getResultInternal() {
        return m_aggregate.result();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void resetInternal() {
        if (m_jsonPathCompiled == null) {
            m_jsonPathCompiled = JsonPath.compile(m_jsonPath);
        }
        m_aggregate.init();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Component getSettingsPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEADING));
        ButtonGroup group = new ButtonGroup();
        panel.add(m_jsonArray);
        panel.add(m_list);
        panel.add(m_listOfLists);
        group.add(m_jsonArray);
        group.add(m_list);
        group.add(m_listOfLists);
        panel.add(new JLabel("JSON path: "));
        panel.add(m_jsonPathField);
        panel.add(new JLabel("Empty leaf: "));
        ButtonGroup emptyMissingOrFail = new ButtonGroup();
        panel.add(m_emptyIsMissing);
        panel.add(m_emptyIsError);
        emptyMissingOrFail.add(m_emptyIsMissing);
        emptyMissingOrFail.add(m_emptyIsError);
        return panel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasOptionalSettings() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec spec)
        throws NotConfigurableException {
        super.loadSettingsFrom(settings, spec);
        m_jsonPath = m_jsonPath == null ? DEFAULT_JSON_PATH : m_jsonPath;
        String resType = settings.getString(RESULT_TYPE, LIST);
        resultTypeLoad(resType);
        m_jsonPath = settings.getString(JSON_PATH, DEFAULT_JSON_PATH);
        m_jsonPathField.setText(m_jsonPath);
        m_emptyLeafAsMissing = settings.getBoolean(EMPTY_LEAF_AS_MISSING, true);
        updateEmptyLeafHandling();
    }

    /**
     *
     */
    private void updateEmptyLeafHandling() {
        if (m_emptyLeafAsMissing) {
            m_emptyIsMissing.setSelected(true);
            m_jsonPathConfiguration = defaultConfig().setOptions(Option.DEFAULT_PATH_LEAF_TO_NULL);
        } else {
            m_emptyIsError.setSelected(true);
            m_jsonPathConfiguration = defaultConfig();
        }
    }

    /**
     * @param resType
     * @throws NotConfigurableException
     */
    private void resultTypeLoad(final String resType) throws NotConfigurableException {
        switch (resType) {
            case JSON_ARRAY:
                m_jsonArray.setSelected(true);
                m_aggregate = new JSONArrayAggregate();
                break;
            case LIST:
                m_list.setSelected(true);
                m_aggregate = new ListAggregate();
                break;
            case LIST_OF_LISTS:
                m_listOfLists.setSelected(true);
                m_aggregate = new ListOfListsAggregate();
                break;
            default:
                throw new NotConfigurableException("Not supported result type: " + resType);
        }
        m_resultType = resType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadValidatedSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.loadValidatedSettings(settings);
        String resType = settings.getString(RESULT_TYPE);
        try {
            resultTypeLoad(resType);
        } catch (NotConfigurableException e) {
            throw new InvalidSettingsException(e.getMessage(), e);
        }
        m_jsonPath = settings.getString(JSON_PATH);
        m_jsonPathField.setText(m_jsonPath);
        m_jsonPathCompiled = JsonPath.compile(m_jsonPath);
        m_emptyLeafAsMissing = settings.getBoolean(EMPTY_LEAF_AS_MISSING);
        updateEmptyLeafHandling();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveSettingsTo(final NodeSettingsWO settings) {
        super.saveSettingsTo(settings);
        String text = m_jsonPathField.getText();
        m_jsonPath = text;
        m_resultType = m_jsonArray.isSelected() ? JSON_ARRAY : m_listOfLists.isSelected() ? LIST_OF_LISTS : LIST;
        settings.addString(RESULT_TYPE, m_resultType);
        settings.addString(JSON_PATH, m_jsonPath);
        m_emptyLeafAsMissing = m_emptyIsMissing.isSelected();
        settings.addBoolean(EMPTY_LEAF_AS_MISSING, m_emptyLeafAsMissing);
    }
}
