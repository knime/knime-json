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
 *   27 Sept 2014 (Gabor): created
 */
package org.knime.json.node.fromstring;

import org.knime.core.data.StringValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.json.node.util.ReplaceColumnSettings;

/**
 * Node settings for the StringToJSON node.
 *
 * @author Gabor Bakos
 */
final class StringToJSONSettings extends ReplaceColumnSettings {
    private static String ALLOW_COMMENTS = "allow.comments", FAIL_ON_ERROR = "fail.on.error";

    private boolean m_allowComments = true, m_failOnError = true;

    /**
     *
     */
    StringToJSONSettings() {
        super(StringValue.class);
        setRemoveInputColumn(true);
        setNewColumnName("JSON");
    }

    /**
     * @return the allowComments
     */
    final boolean isAllowComments() {
        return m_allowComments;
    }

    /**
     * @param allowComments the allowComments to set
     */
    final void setAllowComments(final boolean allowComments) {
        this.m_allowComments = allowComments;
    }

    /**
     * @return the failOnError
     */
    final boolean isFailOnError() {
        return m_failOnError;
    }

    /**
     * @param failOnError the failOnError to set
     */
    final void setFailOnError(final boolean failOnError) {
        this.m_failOnError = failOnError;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        super.saveSettingsTo(settings);
        settings.addBoolean(ALLOW_COMMENTS, m_allowComments);
        settings.addBoolean(FAIL_ON_ERROR, m_failOnError);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsForDialogs(final NodeSettingsRO settings, final PortObjectSpec[] specs) {
        super.loadSettingsForDialogs(settings, specs);
        m_allowComments = settings.getBoolean(ALLOW_COMMENTS, true);
        m_failOnError = settings.getBoolean(FAIL_ON_ERROR, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.loadSettingsFrom(settings);
        m_allowComments = settings.getBoolean(ALLOW_COMMENTS);
        m_failOnError = settings.getBoolean(FAIL_ON_ERROR);
    }

    /**
     * @return No String column found error message.
     */
    @Override
    protected String autoGuessFailedMessage() {
        return "No String columns! Use for example the \"String Manipulator\" node to create one.";
    }

    /**
     * @return The selected input column's name.
     */
    String inputColumnName() {
        return getInputColumnName();
    }
}
