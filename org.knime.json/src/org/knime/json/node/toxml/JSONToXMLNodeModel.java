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
 *   14 Nov. 2014 (Gabor): created
 */
package org.knime.json.node.toxml;

import java.io.File;
import java.io.IOException;

import javax.json.JsonValue;
import javax.xml.parsers.ParserConfigurationException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.data.MissingCell;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.json.JSONValue;
import org.knime.core.data.json.JacksonConversions;
import org.knime.core.data.xml.XMLCell;
import org.knime.core.data.xml.XMLCellFactory;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.CheckUtils;
import org.knime.json.node.util.SingleColumnReplaceOrAddNodeModel;
import org.knime.json.util.Json2Xml;
import org.knime.json.util.Json2Xml.Json2XmlSettings;

import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * This is the model implementation of JSONToXML.
 *
 * @author Gabor Bakos
 */
public class JSONToXMLNodeModel extends SingleColumnReplaceOrAddNodeModel<JSONToXMLSettings> {
    /**
     * Constructor for the node model.
     */
    protected JSONToXMLNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // No internal state
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
        JSONToXMLSettings s = getSettings();
        if (!s.isKeepTypeInfo()) {
            CheckUtils.checkSetting(!s.getArray().trim().isEmpty(), "The empty list prefix is missing.");
            CheckUtils.checkSetting(!s.getBoolean().trim().isEmpty(), "The boolean prefix is missing.");
            CheckUtils.checkSetting(!s.getDecimal().trim().isEmpty(), "The decimal prefix is missing.");
            CheckUtils.checkSetting(!s.getInteger().trim().isEmpty(), "The integer prefix is missing.");
            CheckUtils.checkSetting(!s.getNull().trim().isEmpty(), "The null prefix is missing.");
            CheckUtils.checkSetting(!s.getString().trim().isEmpty(), "The string prefix is missing.");
        }
        CheckUtils.checkSetting(!s.getItem().trim().isEmpty(), "The array item name is missing.");
        CheckUtils.checkSetting(!s.getRoot().trim().isEmpty(), "The root item name is missing.");
        CheckUtils.checkSetting(!s.isSpecifyNamespace() || !s.getNamespace().trim().isEmpty(),
            "The namespace information is missing.");

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
        // No internal state
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
        // No internal state
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected CellFactory createCellFactory(final DataColumnSpec output, final int inputIndex,
        final int... otherColumns) {
        final JacksonConversions conv = JacksonConversions.getInstance();
        final Json2Xml converter = createConverter();

        return new SingleCellFactory(output) {
            @Override
            public DataCell getCell(final DataRow row) {
                final DataCell input = row.getCell(inputIndex);
                if (input instanceof JSONValue) {
                    final JSONValue jsonValue = (JSONValue)input;
                    final JsonValue json = jsonValue.getJsonValue();
                    final TreeNode node = conv.toJackson(json);
                    try {
                        return XMLCellFactory.create(converter.toXml((JsonNode)node));
                    } catch (RuntimeException | ParserConfigurationException | IOException e) {
                        setWarningMessage("There were problems translating to XML, check the missing values.");
                        return new MissingCell(e.getMessage());
                    }
                }
                return DataType.getMissingCell();
            }
        };
    }

    /**
     * @return
     */
    private Json2Xml createConverter() {
        Json2XmlSettings settings = new Json2XmlSettings();
        settings.setArrayPrefix(getSettings().getArray());
        settings.setBool(getSettings().getBoolean());
        settings.setInt(getSettings().getInteger());
        settings.setNamespace(getSettings().isSpecifyNamespace() ? getSettings().getNamespace() : null);
        settings.setNull(getSettings().getNull());
        settings.setReal(getSettings().getDecimal());
        settings.setRootName(getSettings().getRoot());
        settings.setText(getSettings().getString());
        settings.setTextKey(getSettings().isCreateTextForSpecificKeys() ? getSettings().getKeyForText() : null);
        settings.setTranslateHashCommentToComment(getSettings().isTranslateHashCommentToComment());
        settings.setTranslateQuestionPrefixToProcessingInstruction(getSettings().isTranslateQuestionPrefixToProcessingInstruction());
        Json2Xml ret = getSettings().isParentKeyAsElementName() ? Json2Xml.createWithUseParentKeyWhenPossible(settings) : new Json2Xml(settings);
        ret.setLooseTypeInfo(!getSettings().isKeepTypeInfo());
        return ret;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataColumnSpec createOutputSpec(final String outputColName) {
        return new DataColumnSpecCreator(outputColName, XMLCell.TYPE).createSpec();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected JSONToXMLSettings createSettings() {
        return createJSONToXMLSettings();
    }

    /**
     * @return
     */
    static JSONToXMLSettings createJSONToXMLSettings() {
        return new JSONToXMLSettings();
    }
}
