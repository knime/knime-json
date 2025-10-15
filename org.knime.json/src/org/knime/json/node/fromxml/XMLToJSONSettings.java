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
 *   28 Sept 2014 (G�bor): created
 */
package org.knime.json.node.fromxml;

import org.knime.core.data.xml.XMLValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.json.node.util.ReplaceColumnSettings;

/**
 * The settings object of the XMLToJSON node.
 *
 * @author Gabor Bakos
 */
final class XMLToJSONSettings extends ReplaceColumnSettings {

    static final String TEXT_KEY = "text.key";
    static final String TRANSLATE_COMMENTS_KEY = "translate.comments";
    static final String TRANSLATE_PROCESSING_INSTRUCTIONS_KEY = "translate.processing.instructions";

    static final String DEFAULT_TEXT = "text";
    static final String DEFAULT_NEW_CLOUMN_NAME = "JSON";
    static final boolean DEFAULT_TRANSLATE_COMMENTS = false;
    static final boolean DEFAULT_TRANSLATE_PROCESSING_INSTRUCTIONS = false;

    private String m_textKey = DEFAULT_TEXT;
    private boolean m_translateComments = DEFAULT_TRANSLATE_COMMENTS;
    private boolean m_translateProcessingInstructions = DEFAULT_TRANSLATE_PROCESSING_INSTRUCTIONS;

    /**
     * Constructs the {@link XMLToJSONSettings} object.
     */
    XMLToJSONSettings() {
        super(XMLValue.class);
        setNewColumnName(DEFAULT_NEW_CLOUMN_NAME);
    }

    /**
     * @return the texts in xml are saved with this key in jsons
     */
    final String getTextKey() {
        return m_textKey;
    }

    /**
     * @param textKey the text key to set for json (text in xml).
     */
    final void setTextKey(final String textKey) {
        this.m_textKey = textKey;
    }

    /**
     * @param translateComments the translateComments to set
     * @since 3.1
     */
    void setTranslateComments(final boolean translateComments) {
        m_translateComments = translateComments;
    }

    /**
     * @return the translateComments
     * @since 3.1
     */
    boolean isTranslateComments() {
        return m_translateComments;
    }

    /**
     * @param translateProcessingInstructions the translateProcessingInstructions to set
     * @since 3.1
     */
    void setTranslateProcessingInstructions(final boolean translateProcessingInstructions) {
        m_translateProcessingInstructions = translateProcessingInstructions;
    }

    /**
     * @return the translateProcessingInstructions
     * @since 3.1
     */
    boolean isTranslateProcessingInstructions() {
        return m_translateProcessingInstructions;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsForDialogs(final NodeSettingsRO settings, final PortObjectSpec[] specs) {
        m_textKey = settings.getString(TEXT_KEY, DEFAULT_TEXT);
        m_translateComments = settings.getBoolean(TRANSLATE_COMMENTS_KEY, DEFAULT_TRANSLATE_COMMENTS);
        m_translateProcessingInstructions =
                settings.getBoolean(TRANSLATE_PROCESSING_INSTRUCTIONS_KEY, DEFAULT_TRANSLATE_PROCESSING_INSTRUCTIONS);
        super.loadSettingsForDialogs(settings, specs);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_textKey = settings.getString(TEXT_KEY);
        try {
            m_translateComments = settings.getBoolean(TRANSLATE_COMMENTS_KEY);
        } catch (InvalidSettingsException e) {
            //For compatibility
            m_translateComments = true;
        }
        try {
            m_translateProcessingInstructions = settings.getBoolean(TRANSLATE_PROCESSING_INSTRUCTIONS_KEY);
        } catch (InvalidSettingsException e) {
            //For compatibility
            m_translateProcessingInstructions = true;
        }
        super.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addString(TEXT_KEY, m_textKey);
        settings.addBoolean(TRANSLATE_COMMENTS_KEY, m_translateComments);
        settings.addBoolean(TRANSLATE_PROCESSING_INSTRUCTIONS_KEY, m_translateProcessingInstructions);
        super.saveSettingsTo(settings);
    }

    /**
     * @return XML column not found message.
     */
    @Override
    protected String autoGuessFailedMessage() {
        return "No XML columns! Use for example the \"String To XML\" node to create one.";
    }
}
