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
 *   15 Nov 2014 (Gabor): created
 */
package org.knime.json.aggregation;

import java.awt.Component;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.knime.base.data.aggregation.AggregationOperator;
import org.knime.base.data.aggregation.GlobalSettings;
import org.knime.base.data.aggregation.OperatorColumnSettings;
import org.knime.base.data.aggregation.OperatorData;
import org.knime.core.data.BooleanValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.LongValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.collection.CollectionDataValue;
import org.knime.core.data.date.DateAndTimeValue;
import org.knime.core.data.json.JSONCell;
import org.knime.core.data.json.JSONCellFactory;
import org.knime.core.data.json.JSONValue;
import org.knime.core.data.json.JacksonConversions;
import org.knime.core.data.vector.bytevector.ByteVectorValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.json.node.util.ErrorHandling;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jackson.JacksonUtils;

/**
 * Converts a value to json. It also have an option to add column/row info instead of creating an array. <br/>
 * The options: <tt>id as key</tt> - The row id will be used as a key for the resulting array elements.
 * <tt>column name as key</tt> - The column names will be used (within row ids if that is also selected) as keys for the
 * resulting array elements. <tt>data format, date locale</tt> - The parameters for date handling. (They will be
 * serialized as {@link String}s.) <br/>
 * Important special-case: When this is inside the column aggregator node and the column id is selected as a key, only a
 * single object will be created for each row.
 *
 * @author Gabor Bakos
 */
public final class ToJson extends AggregationOperator {
    private final JsonNodeFactory m_factory = JacksonUtils.nodeFactory();

    private ArrayNode m_arrayNode = new ArrayNode(m_factory);

    private static final String ID_AS_KEY = "id.as.key", COLNAME_AS_KEY = "column.name.as.key",
            DATE_FORMAT = "date.format", DATE_LOCALE = "date.locale", SINGLE_OUTPUT = "single.output";

    private static final String DEFAULT_DATE_FORMAT, DEFAULT_DATE_LOCALE = Locale.getDefault().getISO3Language();
    static {
        DateFormat dateTimeInstance = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM);
        if (dateTimeInstance instanceof SimpleDateFormat) {
            SimpleDateFormat sdf = (SimpleDateFormat)dateTimeInstance;
            DEFAULT_DATE_FORMAT = sdf.toPattern();
        } else {
            DEFAULT_DATE_FORMAT = new SimpleDateFormat().toPattern();
        }
    }

    private static final boolean DEFAULT_ID_AS_KEY = false, DEFAULT_COLNAME_AS_KEY = true,
            DEFAULT_SINGLE_OUTPUT = false;

    private boolean m_idAsKey = DEFAULT_ID_AS_KEY, m_colNameAsKey = DEFAULT_COLNAME_AS_KEY,
            m_singleOutput = DEFAULT_SINGLE_OUTPUT;

    private String m_dateFormatPattern = DEFAULT_DATE_FORMAT, m_dateLocaleKey = DEFAULT_DATE_LOCALE;

    private ToJsonSettingsPanel m_panel = new ToJsonSettingsPanel();

    private Locale m_dateLocale;

    private DateFormat m_dateFormat;

    private int m_colIndex = 0;

    /**
     * Constructs the {@link AggregationOperator}.
     */
    public ToJson() {
        this(createOperatorData());
    }

    /**
     * @return The {@link OperatorData} specific to the "To JSON" functions.
     */
    private static OperatorData createOperatorData() {
        return new OperatorData("To JSON", "To JSON", "JSON", false, false, DataValue.class, true);
    }

    /**
     * Constructs the {@link AggregationOperator}.
     *
     * @param operatorData The {@link OperatorData}.
     */
    public ToJson(final OperatorData operatorData) {
        this(operatorData, GlobalSettings.DEFAULT, OperatorColumnSettings.DEFAULT_INCL_MISSING);
    }

    /**
     * Constructs the {@link AggregationOperator}.
     *
     * @param operatorData The {@link OperatorData}.
     * @param globalSettings The {@link GlobalSettings}.
     * @param opColSettings The {@link OperatorColumnSettings}.
     */
    public ToJson(final OperatorData operatorData, final GlobalSettings globalSettings,
        final OperatorColumnSettings opColSettings) {
        super(operatorData, globalSettings, opColSettings);
        setDateParameters();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return ("Converts the values to a JSON value. The possible options are:\n" + "<ul>"
            + "<li><tt>Use id as key</tt> when checked, the id (input column name or row key) will be used as a key "
            + "for the values</li>" + "<li><tt>Date format</tt> format to specify dates (in UTC time zone)</li>"
            + "<li><tt>Date locale</tt> - locale used to translate dates</li>"
            + "<li><tt>Output is single</tt> - when checked no array will be created, "
            + "but will fail if multiple values are present" + "are in the input</li>" + "</ul>\n"
            + "Unsupported data types will generate null values.").replaceAll("</?tt>", "\"").replaceAll("<li>", " - ")
            .replaceAll("</li>", ";\n").replaceAll("</?\\w+/?>", "");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AggregationOperator createInstance(final GlobalSettings globalSettings,
        final OperatorColumnSettings opColSettings) {
        ToJson toJson = new ToJson(getOperatorData(), globalSettings, opColSettings);
        toJson.m_idAsKey = m_idAsKey;
        toJson.m_colNameAsKey = m_colNameAsKey;
        toJson.m_singleOutput = m_singleOutput;
        toJson.m_dateFormatPattern = m_dateFormatPattern;
        toJson.m_dateLocaleKey = m_dateLocaleKey;
        toJson.updatePanel();
        return toJson;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean computeInternal(final DataCell cell) {
        JsonNode jsonNode = toJsonNode(cell);
        if (m_idAsKey) {
            m_arrayNode.addObject().set(getOperatorColumnSettings().getOriginalColSpec().getName(), jsonNode);
        } else {
            m_arrayNode.add(jsonNode);
        }
        return false;
    }

    /**
     * @param cell The input.
     * @return The {@code cell} converted to {@link JsonNode}.
     */
    private JsonNode toJsonNode(final DataCell cell) {
        if (cell.isMissing()) {
            return m_factory.nullNode();
        }
        if (cell instanceof BooleanValue) {
            BooleanValue bv = (BooleanValue)cell;
            return m_factory.booleanNode(bv.getBooleanValue());
        }
        if (cell instanceof IntValue) {
            IntValue iv = (IntValue)cell;
            return m_factory.numberNode(iv.getIntValue());
        }
        if (cell instanceof LongValue) {
            LongValue lv = (LongValue)cell;
            return m_factory.numberNode(lv.getLongValue());
        }
        if (cell instanceof DoubleValue) {
            DoubleValue dv = (DoubleValue)cell;
            return m_factory.numberNode(dv.getDoubleValue());
        }
        if (cell instanceof ByteVectorValue) {
            ByteVectorValue bvv = (ByteVectorValue)cell;
            return m_factory.textNode(m_factory.binaryNode(toBytes(bvv)).asText());
        }
        //TODO Should we support dates as text, or not support?
        if (cell instanceof DateAndTimeValue) {
            DateAndTimeValue datv = (DateAndTimeValue)cell;
            return m_factory.textNode(m_dateFormat.format(new Date(datv.getUTCTimeInMillis())));
        }
        if (cell instanceof JSONValue) {
            JSONValue jsonValue = (JSONValue)cell;
            JsonNode jsonNode = JacksonConversions.getInstance().toJackson(jsonValue.getJsonValue());
            return jsonNode;
        }
        if (cell instanceof StringValue) {
            StringValue sv = (StringValue)cell;
            return m_factory.textNode(sv.getStringValue());
        }
        if (cell instanceof CollectionDataValue) {
            CollectionDataValue cdv = (CollectionDataValue)cell;
            ArrayNode ret = m_factory.arrayNode();
            for (DataCell dataCell : cdv) {
                ret.add(toJsonNode(dataCell));
            }
            return ret;
        }
        return m_factory.nullNode();
    }

    /**
     * @param bvv A {@link ByteVectorValue}.
     * @return As a byte array.
     * @throws IllegalArgumentException When the array cannot be created.
     * @throws OutOfMemoryError When the array cannot be created.
     */
    private byte[] toBytes(final ByteVectorValue bvv) {
        if (bvv.length() > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Too large byte-vector: " + bvv.length());
        }
        byte[] ret = new byte[(int)bvv.length()];
        for (int i = ret.length; i-- > 0;) {
            int v = bvv.get(i);
            v &= 0xff;
            ret[i] = (byte)v;
        }
        return ret;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean computeInternal(final DataRow row, final DataCell cell) {
        JsonNode jsonNode = toJsonNode(cell);
        String rowKey = row.getKey().getString();
        if (m_colNameAsKey) {
            String name = getOperatorColumnSettings().getOriginalColSpec().getName();
            //Quite ugly hack to check we are within ColumnAggregator
            if ("Super DataType of selected columns".equals(name)) {
                //ColumnAggregator
                //In this case we do not create array, but a single object.
                if (m_colIndex == 0) {//Only the first column will add to the array
                    //So always get a single object when using column name
                    m_arrayNode.addObject();
                }
                name = getGlobalSettings().getGroupColNames().get(m_colIndex);
                ObjectNode objectNode = (ObjectNode)m_arrayNode.get(0);
                if (m_idAsKey) {
                    if (objectNode.get(rowKey) == null) {
                        objectNode.set(rowKey, objectNode);
                    } else {
                        ((ObjectNode)objectNode.get(rowKey)).set(name, jsonNode);
                    }
                } else {
                    objectNode.set(name, jsonNode);
                }
                m_colIndex++;
                //No need to add further values
                return false;
            }
            jsonNode = m_factory.objectNode().set(name, jsonNode);
        }
        if (m_idAsKey) {
            jsonNode = m_factory.objectNode().set(rowKey, jsonNode);
        }
        m_arrayNode.add(jsonNode);
        m_colIndex++;
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCompatible(final DataType type) {
        return true;
        //        return type.isCompatible(IntValue.class) || type.isCompatible(LongValue.class)
        //            || type.isCompatible(StringValue.class) || type.isCompatible(JSONValue.class)
        //            || type.isCompatible(DoubleValue.class) || type.isCompatible(ByteVectorValue.class)
        //            || type.isCompatible(DateAndTimeValue.class) || type.isCompatible(BooleanValue.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataType getDataType(final DataType origType) {
        return JSONCell.TYPE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataCell getResultInternal() {
        return createValue();
    }

    /**
     * @return
     */
    private DataCell createValue() {
        if (m_singleOutput) {
            if (m_arrayNode.size() > 1) {
                throw new IllegalArgumentException("There are multiple values, when expected a single one:\n"
                    + ErrorHandling.shorten(m_arrayNode.toString(), 77));
            }
            if (m_arrayNode.size() == 0) {
                return DataType.getMissingCell();
            }
            return JSONCellFactory.create(JacksonConversions.getInstance().toJSR353(m_arrayNode.get(0)));
        }
        return JSONCellFactory.create(JacksonConversions.getInstance().toJSR353(m_arrayNode));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void resetInternal() {
        m_arrayNode.removeAll();
        m_colIndex = 0;
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
    public Component getSettingsPanel() {
        return m_panel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec spec)
        throws NotConfigurableException {
        super.loadSettingsFrom(settings, spec);
        m_idAsKey = settings.getBoolean(ID_AS_KEY, DEFAULT_ID_AS_KEY);
        m_colNameAsKey = settings.getBoolean(COLNAME_AS_KEY, DEFAULT_COLNAME_AS_KEY);
        m_singleOutput = settings.getBoolean(SINGLE_OUTPUT, DEFAULT_SINGLE_OUTPUT);
        m_dateFormatPattern = settings.getString(DATE_FORMAT, DEFAULT_DATE_FORMAT);
        m_dateLocaleKey = settings.getString(DATE_LOCALE, DEFAULT_DATE_LOCALE);
        setDateParameters();
        updatePanel();
    }

    /**
     *
     */
    private void setDateParameters() {
        if (m_dateFormatPattern == null) {
            m_dateFormatPattern = DEFAULT_DATE_FORMAT;
        }
        m_dateLocale = new Locale(m_dateLocaleKey.split("\\s")[0]);
        m_dateFormat = new SimpleDateFormat(m_dateFormatPattern, m_dateLocale);
    }

    /**
     *
     */
    private void updatePanel() {
        m_panel.setIdAsKey(m_idAsKey);
        m_panel.setColNameAsKey(m_colNameAsKey);
        m_panel.setSingleOutput(m_singleOutput);
        m_panel.setDateFormat(m_dateFormatPattern);
        m_panel.setDateLocale(m_dateLocaleKey);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadValidatedSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.loadValidatedSettings(settings);
        m_idAsKey = settings.getBoolean(ID_AS_KEY);
        m_colNameAsKey = settings.getBoolean(COLNAME_AS_KEY);
        m_singleOutput = settings.getBoolean(SINGLE_OUTPUT);
        m_dateFormatPattern = settings.getString(DATE_FORMAT);
        m_dateLocaleKey = settings.getString(DATE_LOCALE);
        setDateParameters();
        updatePanel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveSettingsTo(final NodeSettingsWO settings) {
        loadSettingsFromPanel();
        super.saveSettingsTo(settings);
        settings.addBoolean(ID_AS_KEY, m_idAsKey);
        settings.addBoolean(COLNAME_AS_KEY, m_colNameAsKey);
        settings.addBoolean(SINGLE_OUTPUT, m_singleOutput);
        settings.addString(DATE_FORMAT, m_dateFormatPattern);
        settings.addString(DATE_LOCALE, m_dateLocaleKey);
    }

    /**
     *
     */
    private void loadSettingsFromPanel() {
        m_idAsKey = m_panel.getIdAsKey();
        m_colNameAsKey = m_panel.getColNameAsKey();
        m_singleOutput = m_panel.getSingleOutput();
        m_dateFormatPattern = m_panel.getDateFormat();
        m_dateLocaleKey = m_panel.getDateLocaleKey();
    }
}
