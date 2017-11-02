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
 *   2 Oct 2014 (Gabor): created
 */
package org.knime.json.aggregation;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

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
import org.knime.json.node.util.GUIFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.github.fge.jackson.JacksonUtils;
import com.github.fge.jackson.jsonpointer.JsonPointerException;

/**
 * Json Pointer projection and collection operator.
 *
 * @author Gabor Bakos
 */
public class JsonPointer extends AggregationOperator {
    private static final String RESULT_TYPE = "result type", JSON_POINTER = "json pointer",
            NO_MATCH_HANDLING = "on no match";

    private static final String JSON_ARRAY = "JSON array", LIST = "list";

    private static final String FAIL = "fail", MISSING = "missing", SKIP = "skip";

    private final JTextField m_pointer = GUIFactory.createTextField("", 22);

    private com.github.fge.jackson.jsonpointer.JsonPointer m_jsonPointer;

    private final JRadioButton m_resultIsJsonArray = new JRadioButton(JSON_ARRAY, true);

    private final JRadioButton m_resultIsList = new JRadioButton(LIST);

    private final JRadioButton m_noMatchFail = new JRadioButton(FAIL, true);

    private final JRadioButton m_noMatchMissing = new JRadioButton(MISSING);

    private final JRadioButton m_noMatchSkip = new JRadioButton(SKIP);

    private final ButtonGroup m_resultGroup = new ButtonGroup(), m_noMatchGroup = new ButtonGroup();
    {
        m_resultGroup.add(m_resultIsJsonArray);
        m_resultGroup.add(m_resultIsList);
        m_noMatchGroup.add(m_noMatchFail);
        m_noMatchGroup.add(m_noMatchMissing);
        m_noMatchGroup.add(m_noMatchSkip);
    }

    private final List<DataCell> m_cells = new ArrayList<>();

    private ArrayNode m_array = createArrayNode();

    private boolean m_missing = false;

    /**
     * @return creates a new {@link ArrayNode}.
     */
    private ArrayNode createArrayNode() {
        return new ArrayNode(JacksonUtils.nodeFactory());
    }

    /**
     * Default constructor.
     */
    public JsonPointer() {
        this(createOperatorData());
    }

    /**
     * @return The common operator data.
     */
    private static OperatorData createOperatorData() {
        return new OperatorData("Json pointer", false, false, JSONValue.class, false);
    }

    /**
     * @param operatorData
     */
    public JsonPointer(final OperatorData operatorData) {
        this(operatorData, GlobalSettings.DEFAULT, OperatorColumnSettings.DEFAULT_EXCL_MISSING);
    }

    /**
     * @param operatorData
     * @param globalSettings
     * @param opColSettings
     */
    public JsonPointer(final OperatorData operatorData, final GlobalSettings globalSettings,
        final OperatorColumnSettings opColSettings) {
        super(operatorData, globalSettings, opColSettings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        String html =
            "Selects and collects certain values from the documents.<br/>"
                + "It uses <a href=\"http://tools.ietf.org/html/rfc6901\">JSON Pointer</a>s to select the values.<br/>"
                + "Examples:<br/>"
                + "<ul><li>/foo/0</li><li></li><li>/foo/bar</li><li>/0</li></ul><br/>"
                + "Parameters: <ul>"
                + "<li><b>JSON Pointer</b>: the JSON Pointer path</li>"
                + "<li><b>Result type</b>: <i>JSON array</i>: a JSON value concatenated the selected results; <i>List</i>: a KNIME collection type of String cells</li>"
                + "<li><b>No match</b>: how to handle the case when there is no match:<ul>"
                + "<li><b>fail</b>: execution stops</li>" + "<li><b>missing</b>: results in a missing value</li>"
                + "<li><b>skip</b>: add nothing for that cell to the collection</li>" + "</ul></li>" + "</ul>";
        return html.replaceAll("<br/>", "\n").replaceAll("</li></ul>", ".\n").replaceAll("</li>", ",\n")
            .replaceAll("<.*?>", "");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AggregationOperator createInstance(final GlobalSettings globalSettings,
        final OperatorColumnSettings opColSettings) {
        final JsonPointer ret = new JsonPointer();
        ret.m_noMatchFail.setSelected(m_noMatchFail.isSelected());
        ret.m_noMatchMissing.setSelected(m_noMatchMissing.isSelected());
        ret.m_noMatchSkip.setSelected(m_noMatchSkip.isSelected());
        ret.m_resultIsJsonArray.setSelected(m_resultIsJsonArray.isSelected());
        ret.m_resultIsList.setSelected(m_resultIsList.isSelected());
        try {
            ret.setPointer(m_pointer.getText());
        } catch (NotConfigurableException e) {
            throw new IllegalStateException(e);
        }
        return ret;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean computeInternal(final DataCell cell) {
        if (cell instanceof JSONValue) {
            JSONValue jv = (JSONValue)cell;
            JsonNode jsonNode = m_jsonPointer.path(JacksonConversions.getInstance().toJackson(jv.getJsonValue()));
            if (jsonNode.isMissingNode()) {
                if (m_noMatchFail.isSelected()) {
                    throw new IllegalArgumentException("Could not select \"" + m_pointer.getText() + "\" from "
                        + jv.getJsonValue());
                }
                if (m_noMatchMissing.isSelected()) {
                    m_missing = true;
                } else if (m_noMatchSkip.isSelected()) {
                    //Do nothing
                } else {
                    throw new IllegalStateException("No supported no match strategy.");
                }
            } else {
                if (m_resultIsJsonArray.isSelected()) {
                    m_array.add(jsonNode);
                } else if (m_resultIsList.isSelected()) {
                    m_cells.add(new StringCell(jsonNode.toString()));
                } else {
                    throw new IllegalStateException("Not supported result type.");
                }
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataType getDataType(final DataType origType) {
        return m_resultIsJsonArray.isSelected() ? JSONCell.TYPE : ListCell.getCollectionType(StringCell.TYPE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataCell getResultInternal() {
        if (m_missing) {
            return DataType.getMissingCell();
        }
        return m_resultIsJsonArray.isSelected() ? JSONCellFactory.create(JacksonConversions.getInstance().toJSR353(
            m_array)) : CollectionCellFactory.createListCell(m_cells);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void resetInternal() {
        m_array = createArrayNode();
        m_cells.clear();
        m_missing = false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec spec)
        throws NotConfigurableException {
        super.loadSettingsFrom(settings, spec);
        setResultType(settings);
        setPointer(settings);
        setNoMatchHandling(settings);
    }

    /**
     * @param settings
     * @throws NotConfigurableException
     */
    private void setNoMatchHandling(final NodeSettingsRO settings) throws NotConfigurableException {
        String noMatch = settings.getString(NO_MATCH_HANDLING, FAIL);
        setNoMatchHandling(noMatch);
    }

    /**
     * @param noMatch
     * @throws NotConfigurableException
     */
    private void setNoMatchHandling(final String noMatch) throws NotConfigurableException {
        switch (noMatch) {
            case FAIL:
                m_noMatchFail.setSelected(true);
                break;
            case MISSING:
                m_noMatchMissing.setSelected(true);
                break;
            case SKIP:
                m_noMatchSkip.setSelected(true);
                break;
            default:
                throw new NotConfigurableException("Not supported option to handle not found values: " + noMatch);
        }
    }

    /**
     * @param settings
     * @throws NotConfigurableException
     */
    private void setPointer(final NodeSettingsRO settings) throws NotConfigurableException {
        String value = settings.getString(JSON_POINTER, "");
        setPointer(value);
    }

    /**
     * @param value
     * @throws NotConfigurableException
     */
    private void setPointer(final String value) throws NotConfigurableException {
        m_pointer.setText(value);
        try {
            m_jsonPointer = new com.github.fge.jackson.jsonpointer.JsonPointer(m_pointer.getText());
        } catch (JsonPointerException e) {
            throw new NotConfigurableException(e.getMessage(), e);
        }
    }

    /**
     * @param settings
     * @throws NotConfigurableException
     */
    private void setResultType(final NodeSettingsRO settings) throws NotConfigurableException {
        String resType = settings.getString(RESULT_TYPE, JSON_ARRAY);
        setResultType(resType);
    }

    /**
     * @param resType
     * @throws NotConfigurableException
     */
    private void setResultType(final String resType) throws NotConfigurableException {
        switch (resType) {
            case JSON_ARRAY:
                m_resultIsJsonArray.setSelected(true);
                break;
            case LIST:
                m_resultIsList.setSelected(true);
                break;
            default:
                throw new NotConfigurableException("Not supported result type: " + resType);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadValidatedSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.loadValidatedSettings(settings);
        try {
            setNoMatchHandling(settings.getString(NO_MATCH_HANDLING));
            setPointer(settings.getString(JSON_POINTER));
            setResultType(settings.getString(RESULT_TYPE));
        } catch (NotConfigurableException e) {
            throw new InvalidSettingsException(e.getCause() == null ? e : e.getCause());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveSettingsTo(final NodeSettingsWO settings) {
        super.saveSettingsTo(settings);
        if (m_resultIsJsonArray.isSelected()) {
            settings.addString(RESULT_TYPE, JSON_ARRAY);
        } else if (m_resultIsList.isSelected()) {
            settings.addString(RESULT_TYPE, LIST);
        } else {
            throw new IllegalStateException("Not supported result type");
        }
        settings.addString(JSON_POINTER, m_pointer.getText());
        if (m_noMatchFail.isSelected()) {
            settings.addString(NO_MATCH_HANDLING, FAIL);
        } else if (m_noMatchMissing.isSelected()) {
            settings.addString(NO_MATCH_HANDLING, MISSING);
        } else if (m_noMatchSkip.isSelected()) {
            settings.addString(NO_MATCH_HANDLING, SKIP);
        }
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
        final JPanel ret = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(4, 4, 4, 4);
        c.gridx = 0;
        c.gridy = 0;
        ret.add(new JLabel("JSON Pointer"), c);
        c.gridx = 1;
        ret.add(m_pointer, c);

        c.gridy++;
        c.gridx = 0;
        ret.add(new JLabel("Result type"), c);
        JPanel resultOptions = new JPanel(new FlowLayout(FlowLayout.LEADING));
        resultOptions.add(m_resultIsJsonArray);
        resultOptions.add(m_resultIsList);
        c.gridx++;
        ret.add(resultOptions, c);

        c.anchor = GridBagConstraints.NORTHWEST;
        c.gridy++;
        c.gridx = 0;
        ret.add(new JLabel("No match"), c);
        c.gridx++;
        JPanel noMatchOptions = new JPanel(new GridLayout(3, 1));
        noMatchOptions.add(m_noMatchFail);
        noMatchOptions.add(m_noMatchMissing);
        noMatchOptions.add(m_noMatchSkip);
        ret.add(noMatchOptions, c);
        return ret;
    }
}
