/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *   20 Dec 2014 (Gabor): created
 */
package org.knime.json.node.combine.row;

import java.util.ArrayList;
import java.util.List;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.json.JSONValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * Common base class for row combining node's settings.
 *
 * @author Gabor Bakos
 */
public class RowCombineSettings {
    enum ObjectOrArray {
        Array, Object;
    }

    private static final String INPUT_COLUMN = "inputColumn";
    private static final String ADD_ROOT_KEY = "addRootKey";
    private static final String ROOT_KEY = "rootKey";
    private static final String KEYS = "keys";
    private static final String VALUES = "values";
    private static final String OBJECT_OR_ARRAY = "objectOrArray";
    private static final String OBJECT_KEY_COLUMN = "objectKeyColumn";
    private static final String OBJECT_KEY_ROW_ID = "objectKeyRowID";
    static final boolean DEFAULT_ADD_ROOT_KEY = true;
    static final boolean DEFAULT_OBJECT_KEY_ROW_ID = true;
    static final ObjectOrArray DEFAULT_OBJECT_OR_ARRAY = ObjectOrArray.Array;
    private static final String DEFAULT_ROOT_KEY = "root";
    private String m_inputColumn = null;
    private String m_rootKey = DEFAULT_ROOT_KEY;
    private boolean m_addRootKey = DEFAULT_ADD_ROOT_KEY;
    private String[] m_keys = new String[0];
    private String[] m_values = new String[0];
    private ObjectOrArray m_objectOrArray = DEFAULT_OBJECT_OR_ARRAY;
    private String m_objectKeyColumn = null;
    private boolean m_objectKeyIsRowID = DEFAULT_OBJECT_KEY_ROW_ID;

    /**
     * Constructs the object
     */
    RowCombineSettings() {
        super();
    }

    /**
     * @return the inputColumn
     */
    protected String getInputColumn() {
        return m_inputColumn;
    }

    /**
     * @param inputColumn the inputColumn to set
     */
    protected void setInputColumn(final String inputColumn) {
        m_inputColumn = inputColumn;
    }

    /**
     * @return the addRootKey
     */
    protected final boolean isAddRootKey() {
        return m_addRootKey;
    }

    /**
     * @param addRootKey the addRootKey to set
     */
    protected final void setAddRootKey(final boolean addRootKey) {
        this.m_addRootKey = addRootKey;
    }

    /**
     * @return the rootKey
     */
    protected String getRootKey() {
        return m_rootKey;
    }

    /**
     * @param rootKey the rootKey to set (can be {@code null}, which case no wrapper object added)
     */
    protected void setRootKey(final String rootKey) {
        m_rootKey = rootKey;
    }

    /**
     * @return the keys
     */
    protected String[] getKeys() {
        return m_keys;
    }

    /**
     * @param keys the keys to set
     */
    protected void setKeys(final String[] keys) {
        m_keys = keys;
    }

    /**
     * @return the values
     */
    protected String[] getValues() {
        return m_values;
    }

    /**
     * @param values the values to set
     */
    protected void setValues(final String[] values) {
        m_values = values;
    }

    /**
     * @return the objectOrArray
     */
    final ObjectOrArray getObjectOrArray() {
        return m_objectOrArray;
    }

    /**
     * @param objectOrArray the objectOrArray to set
     */
    final void setObjectOrArray(final ObjectOrArray objectOrArray) {
        this.m_objectOrArray = objectOrArray;
    }

    /**
     * @return the objectKeyColumn
     */
    final String getObjectKeyColumn() {
        return m_objectKeyColumn;
    }

    /**
     * @param objectKeyColumn the objectKeyColumn to set
     */
    final void setObjectKeyColumn(final String objectKeyColumn) {
        this.m_objectKeyColumn = objectKeyColumn;
    }

    /**
     * @return the objectKeyIsRowID
     */
    final boolean isObjectKeyIsRowID() {
        return m_objectKeyIsRowID;
    }

    /**
     * @param objectKeyIsRowID the objectKeyIsRowID to set
     */
    final void setObjectKeyIsRowID(final boolean objectKeyIsRowID) {
        this.m_objectKeyIsRowID = objectKeyIsRowID;
    }

    /**
     * Called from dialog when settings are to be loaded.
     *
     * @param settings To load from
     * @param inSpec Input spec
     */
    protected void loadSettingsDialog(final NodeSettingsRO settings, final DataTableSpec inSpec) {
            m_inputColumn = settings.getString(INPUT_COLUMN, null);
            m_addRootKey = settings.getBoolean(ADD_ROOT_KEY, DEFAULT_ADD_ROOT_KEY);
            m_rootKey = settings.getString(ROOT_KEY, DEFAULT_ROOT_KEY);
            m_keys = settings.getStringArray(KEYS, new String[0]);
            m_values = settings.getStringArray(VALUES, new String[0]);
            m_objectOrArray = ObjectOrArray.valueOf(settings.getString(OBJECT_OR_ARRAY, DEFAULT_OBJECT_OR_ARRAY.name()));
            m_objectKeyColumn = settings.getString(OBJECT_KEY_COLUMN, null);
            m_objectKeyIsRowID = settings.getBoolean(OBJECT_KEY_ROW_ID, DEFAULT_OBJECT_KEY_ROW_ID);
        }

    /**
     * Called from model when settings are to be loaded.
     *
     * @param settings To load from
     * @throws InvalidSettingsException If settings are invalid.
     */
    protected void loadSettingsModel(final NodeSettingsRO settings) throws InvalidSettingsException {
            m_inputColumn = settings.getString(INPUT_COLUMN);
            m_addRootKey = settings.getBoolean(ADD_ROOT_KEY);
            m_rootKey = settings.getString(ROOT_KEY);
            m_keys = settings.getStringArray(KEYS);
            m_values = settings.getStringArray(VALUES);
            m_objectOrArray = ObjectOrArray.valueOf(settings.getString(OBJECT_OR_ARRAY));
            m_objectKeyColumn = settings.getString(OBJECT_KEY_COLUMN);
            m_objectKeyIsRowID = settings.getBoolean(OBJECT_KEY_ROW_ID);
        }

    /**
     * Called from model and dialog to save current settings.
     *
     * @param settings To save to.
     */
    protected void saveSettings(final NodeSettingsWO settings) {
            settings.addString(INPUT_COLUMN, m_inputColumn);
            settings.addBoolean(ADD_ROOT_KEY, m_addRootKey);
            settings.addString(ROOT_KEY, m_rootKey);
            settings.addStringArray(KEYS, m_keys);
            settings.addStringArray(VALUES, m_values);
            settings.addString(OBJECT_OR_ARRAY, m_objectOrArray.name());
            settings.addString(OBJECT_KEY_COLUMN, m_objectKeyColumn);
            settings.addBoolean(OBJECT_KEY_ROW_ID, m_objectKeyIsRowID);
        }

    /**
     * Autoconfigures the input column based on the input {@code dataTableSpec}.
     *
     * @param dataTableSpec The input {@link DataTableSpec}.
     * @return The warning or an empty {@link String}.
     * @throws InvalidSettingsException When there are no JSON columns.
     */
    protected String autoConfigure(final DataTableSpec dataTableSpec) throws InvalidSettingsException {
        if (null == getInputColumn()) {
            List<String> compatibleCols = new ArrayList<String>();
            for (DataColumnSpec c : dataTableSpec) {
                if (c.getType().isCompatible(JSONValue.class)) {
                    compatibleCols.add(c.getName());
                }
            }
            if (compatibleCols.size() == 1) {
                // auto-configure
                setInputColumn(compatibleCols.get(0));
            } else if (compatibleCols.size() > 1) {
                // auto-guessing
                setInputColumn(compatibleCols.get(0));
                return "Auto guessing: using column \"" + compatibleCols.get(0) + "\".";
            } else {
                throw new InvalidSettingsException("No JSON " + "column in input table."
                    + " Try using the Columns to JSON node before this node.");
            }
        }
        return "";
    }
}