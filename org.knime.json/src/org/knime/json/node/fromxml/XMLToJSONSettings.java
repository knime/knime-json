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
 *   28 Sept 2014 (Gábor): created
 */
package org.knime.json.node.fromxml;

import org.knime.core.data.xml.XMLValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.json.node.util.ReplaceOrAddColumnSettings;

/**
 * The settings object of the XMLToJSON node.
 *
 * @author Gabor Bakos
 */
final class XMLToJSONSettings extends ReplaceOrAddColumnSettings {
    /**
     * The default value for key of the text node translation to json.
     */
    private static final String DEFAULT_TEXT = "text";

    /**
     * The configuration key for key of the text node translation to json.
     */
    private static final String TEXT = "text.key";

    private String m_textKey = DEFAULT_TEXT;

    /**
     * Constructs the {@link XMLToJSONSettings} object.
     */
    XMLToJSONSettings() {
        super(XMLValue.class);
        setRemoveInputColumn(true);
        setNewColumnName("JSON");
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
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsForDialogs(final NodeSettingsRO settings, final PortObjectSpec[] specs) {
        m_textKey = settings.getString(TEXT, DEFAULT_TEXT);
        super.loadSettingsForDialogs(settings, specs);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_textKey = settings.getString(TEXT);
        super.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addString(TEXT, m_textKey);
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
