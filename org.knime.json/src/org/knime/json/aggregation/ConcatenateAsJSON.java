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
 *   29 Sept 2014 (Gabor): created
 */
package org.knime.json.aggregation;

import org.knime.base.data.aggregation.AggregationOperator;
import org.knime.base.data.aggregation.GlobalSettings;
import org.knime.base.data.aggregation.OperatorColumnSettings;
import org.knime.base.data.aggregation.OperatorData;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.json.JSONCellFactory;
import org.knime.core.data.json.JSONValue;
import org.knime.core.data.json.JacksonConversions;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.github.fge.jackson.JacksonUtils;

/**
 * Concatenates the values within the groups.
 *
 * @author Gabor Bakos
 */
public class ConcatenateAsJSON extends AggregationOperator {
    private ArrayNode m_array = initArray();

    /**
     * @return
     */
    private ArrayNode initArray() {
        return new ArrayNode(JacksonUtils.nodeFactory());
    }

    /**
     *
     */
    public ConcatenateAsJSON() {
        this(createOperatorData());
    }

    /**
     * @return
     */
    private static OperatorData createOperatorData() {
        return new OperatorData("Concatenate as JSON array", false, false, JSONValue.class, false);
    }

    /**
     * @param operatorData
     */
    public ConcatenateAsJSON(final OperatorData operatorData) {
        this(operatorData, GlobalSettings.DEFAULT, OperatorColumnSettings.DEFAULT_EXCL_MISSING);
    }

    /**
     * @param operatorData
     * @param globalSettings
     * @param opColSettings
     */
    public ConcatenateAsJSON(final OperatorData operatorData, final GlobalSettings globalSettings,
        final OperatorColumnSettings opColSettings) {
        super(operatorData, globalSettings, AggregationOperator.setInclMissingFlag(opColSettings, false));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Concatenates JSON values as a JSON array";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AggregationOperator createInstance(final GlobalSettings globalSettings,
        final OperatorColumnSettings opColSettings) {
        return new ConcatenateAsJSON(createOperatorData(), globalSettings, AggregationOperator.setInclMissingFlag(
            opColSettings, false));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean computeInternal(final DataCell cell) {
        if (cell instanceof JSONValue) {
            JSONValue jsonVal = (JSONValue)cell;
            m_array.add(JacksonConversions.getInstance().toJackson(jsonVal.getJsonValue()));
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataType getDataType(final DataType origType) {
        if (origType.isCompatible(JSONValue.class)) {
            return origType;
        }
        throw new UnsupportedOperationException("Not supported data type: " + origType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataCell getResultInternal() {
        return JSONCellFactory.create(JacksonConversions.getInstance().toJSR353(m_array));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void resetInternal() {
        m_array = initArray();
    }
}
