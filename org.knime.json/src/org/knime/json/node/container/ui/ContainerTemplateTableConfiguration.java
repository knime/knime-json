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
 *   Oct 29, 2018 (Tobias Urhaug, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.json.node.container.ui;

import java.io.IOException;

import javax.json.JsonValue;

import org.knime.core.data.json.container.table.ContainerTableJsonSchema;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.json.node.container.input.table.ContainerTableDefaultJsonStructure;
import org.knime.json.util.JSONUtil;

/**
 * Configuration holding all relevant configurations of {@link ContainerTemplateTablePanel}.
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 */
final public class ContainerTemplateTableConfiguration {

    private static final JsonValue DEFAULT_TEMPLATE = ContainerTableDefaultJsonStructure.asJsonValue();
    private static final boolean DEFAULT_USE_ENTIRE_TABLE = true;
    private static final int DEFAULT_NUMBER_OF_ROWS = 10;
    private static final boolean DEFAULT_OMIT_TABLE_SPEC = false;

    private JsonValue m_template;
    private boolean m_useEntireTable;
    private int m_numberOfRows;
    private boolean m_omitTableSpec;

    /*
     *  This is necessary for backwards compatibility as the template table is stored under a different key in the
     *  Container Input (Table) ("exampleInput") as in the Container Output (Table) ("exampleOutput").
     */
    private final String m_templateTableKey;

    /**
     * Creates a new container template configuration.
     * @param templateTableKey the settings key for the template table
     */
    public ContainerTemplateTableConfiguration(final String templateTableKey) {
        m_template = DEFAULT_TEMPLATE;
        m_useEntireTable = DEFAULT_USE_ENTIRE_TABLE;
        m_numberOfRows = DEFAULT_NUMBER_OF_ROWS;
        m_omitTableSpec = DEFAULT_OMIT_TABLE_SPEC;
        m_templateTableKey = templateTableKey;
    }

    /**
     * Gets the template.
     * @return the template
     */
    public JsonValue getTemplate() {
        return m_template;
    }

    /**
     * Sets the template.
     * @param template the template to be set
     * @throws InvalidSettingsException if the template does not comply with {@link ContainerTableJsonSchema}
     */
    public void setTemplate(final JsonValue template) throws InvalidSettingsException {
        if (ContainerTableJsonSchema.hasContainerTableJsonSchema(template)) {
            m_template = template;
        } else {
            throw new InvalidSettingsException("Template has wrong format.");
        }
    }

    /**
     * Gets the use entire table flag.
     * @return the use entire table flag
     */
    public boolean getUseEntireTable() {
        return m_useEntireTable;
    }

    /**
     * Sets the use entire table flag.
     * @param useEntireTable the flag to be set
     */
    public void setUseEntireTable(final boolean useEntireTable) {
        m_useEntireTable = useEntireTable;
    }

    /**
     * Gets the number of rows the template table uses.
     * @return the number of rows the template table uses
     */
    public int getNumberOfRows() {
        return m_numberOfRows;
    }

    /**
     * Sets the number of rows the template table uses.
     * @param numberOfRows the number of rows the template table uses
     */
    public void setNumberOfRows(final int numberOfRows) {
        m_numberOfRows = numberOfRows;
    }

    /**
     * Gets a flag telling if the table spec should be omitted or not.
     * @return a flag telling if the table spec should be omitted or not
     */
    public boolean getOmitTableSpec() {
        return m_omitTableSpec;
    }

    /**
     * Sets a flag telling if the table spec should be omitted or not.
     * @param omitTableSpec the flag telling if the table spec should be omitted or not
     */
    public void setOmitTableSpec(final boolean omitTableSpec) {
        m_omitTableSpec = omitTableSpec;
    }

    /**
     * Loads the settings from the given node settings object. Loading will fail if settings are missing or invalid.
     *
     * @param settings a node settings object
     * @return the updated configuration
     * @throws InvalidSettingsException if settings are missing or invalid
     */
    public ContainerTemplateTableConfiguration loadInModel(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        setUseEntireTable(settings.getBoolean("useEntireTable", DEFAULT_USE_ENTIRE_TABLE));
        setNumberOfRows(settings.getInt("numberOfRows", DEFAULT_NUMBER_OF_ROWS));
        setOmitTableSpec(settings.getBoolean("omitTableSpec", DEFAULT_OMIT_TABLE_SPEC));
        String jsonString = settings.getString(m_templateTableKey, ContainerTableDefaultJsonStructure.asString());
        try {
            JsonValue jsonValue = JSONUtil.parseJSONValue(jsonString);
            setTemplate(jsonValue);
        } catch (IOException e) {
            throw new InvalidSettingsException("Example input has wrong format.", e);
        }

        return this;
    }

    /**
     * Loads the settings from the given node settings object.
     * Default values will be used for missing or invalid settings.
     *
     * @param settings a node settings object
     * @return the updated configuration
     */
    public ContainerTemplateTableConfiguration loadInDialog(final NodeSettingsRO settings) {
        setUseEntireTable(settings.getBoolean("useEntireTable", DEFAULT_USE_ENTIRE_TABLE));
        setNumberOfRows(settings.getInt("numberOfRows", DEFAULT_NUMBER_OF_ROWS));
        setOmitTableSpec(settings.getBoolean("omitTableSpec", DEFAULT_OMIT_TABLE_SPEC));
        String jsonString = settings.getString(m_templateTableKey, ContainerTableDefaultJsonStructure.asString());
        try {
            JsonValue jsonValue = JSONUtil.parseJSONValue(jsonString);
            setTemplate(jsonValue);
        } catch (IOException | InvalidSettingsException e) {
            m_template = DEFAULT_TEMPLATE;
        }
        return this;
    }

    /**
     * Saves the current configuration to the given settings object.
     *
     * @param settings a settings object
     * @return this object
     */
    public ContainerTemplateTableConfiguration save(final NodeSettingsWO settings) {
        settings.addBoolean("useEntireTable", m_useEntireTable);
        settings.addInt("numberOfRows", m_numberOfRows);
        settings.addBoolean("omitTableSpec", m_omitTableSpec);
        if (m_template != null ) {
            settings.addString(m_templateTableKey, JSONUtil.toPrettyJSONString(m_template));
        }
        return this;
    }
}
