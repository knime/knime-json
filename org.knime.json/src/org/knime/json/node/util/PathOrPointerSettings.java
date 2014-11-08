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
 *   8 Nov. 2014 (Gabor): created
 */
package org.knime.json.node.util;

import org.knime.core.data.date.DateAndTimeCell;
import org.knime.core.data.json.JSONValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObjectSpec;

/**
 * Common settings for JSONPath and JSONPointer.
 *
 * @author Gabor Bakos
 */
public class PathOrPointerSettings extends ReplaceOrAddColumnSettings {
    //TODO option to fail or result in missing value if not found or cannot be converted to result type?
    //TODO probably a locale and date format would be a good idea to not rely on computer (and lib) settings for dates.

    /** Default value for the return type settings. */
    protected static final OutputType DEFAULT_RETURN_TYPE = OutputType.Json;

    /** Key for the String type return type settings (encoded as enum by its {@link Enum#name()}). */
    protected static final String RETURN_TYPE = "returnType";

    private OutputType m_returnType = DEFAULT_RETURN_TYPE;

    /** Key for date and time has date or not @see DateAndTimeCell#DateAndTimeCell(long, boolean, boolean, boolean) */
    protected static final String DATETIME_HAS_DATE = "date-time.has.date";

    /** Key for date and time has time or not @see DateAndTimeCell#DateAndTimeCell(long, boolean, boolean, boolean) */
    protected static final String DATETIME_HAS_TIME = "date-time.has.time";

    /** Key for date and time has millisecs or not @see DateAndTimeCell#DateAndTimeCell(long, boolean, boolean, boolean) */
    protected static final String DATETIME_HAS_MILLIS = "date-time.has.millis";

    /** Default value for hasDate */
    protected static final boolean DEFAULT_DATETIME_HAS_DATE = true;

    /** Default value for hasTime */
    protected static final boolean DEFAULT_DATETIME_HAS_TIME = true;

    /** Default value for hasMillis */
    protected static final boolean DEFAULT_DATETIME_HAS_MILLIS = true;

    private boolean m_hasDate = DEFAULT_DATETIME_HAS_DATE, m_hasTime = DEFAULT_DATETIME_HAS_TIME,
            m_hasMillis = DEFAULT_DATETIME_HAS_MILLIS;

    private final NodeLogger m_logger;

    /**
     * @param logger The logger used to report warnings, errors.
     */
    public PathOrPointerSettings(final NodeLogger logger) {
        super(JSONValue.class);
        this.m_logger = logger;
        setRemoveInputColumn(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsForDialogs(final NodeSettingsRO settings, final PortObjectSpec[] specs) {
        super.loadSettingsForDialogs(settings, specs);
        try {
            m_returnType = toOutputType(settings.getString(RETURN_TYPE, DEFAULT_RETURN_TYPE.name()));
        } catch (InvalidSettingsException e) {
            getLogger().debug(
                "Failed to load settings. Probably tried to load a workflow saved a newer version of KNIME", e);
            m_returnType = DEFAULT_RETURN_TYPE;
        }

        m_hasDate = settings.getBoolean(DATETIME_HAS_DATE, DEFAULT_DATETIME_HAS_DATE);
        m_hasTime = settings.getBoolean(DATETIME_HAS_TIME, DEFAULT_DATETIME_HAS_TIME);
        m_hasMillis = settings.getBoolean(DATETIME_HAS_MILLIS, DEFAULT_DATETIME_HAS_MILLIS);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.loadSettingsFrom(settings);
        m_returnType = toOutputType(settings.getString(RETURN_TYPE));

        m_hasDate = settings.getBoolean(DATETIME_HAS_DATE);
        m_hasTime = settings.getBoolean(DATETIME_HAS_TIME);
        m_hasMillis = settings.getBoolean(DATETIME_HAS_MILLIS);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        super.saveSettingsTo(settings);
        settings.addString(RETURN_TYPE, m_returnType.name());
        settings.addBoolean(DATETIME_HAS_DATE, m_hasDate);
        settings.addBoolean(DATETIME_HAS_TIME, m_hasTime);
        settings.addBoolean(DATETIME_HAS_MILLIS, m_hasMillis);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.validateSettings(settings);
        String outputType = settings.getString(RETURN_TYPE);
        toOutputType(outputType);

        settings.getBoolean(DATETIME_HAS_DATE);
        settings.getBoolean(DATETIME_HAS_TIME);
        settings.getBoolean(DATETIME_HAS_MILLIS);
    }

    /**
     * @return the returnType
     */
    public final OutputType getReturnType() {
        return m_returnType;
    }

    /**
     * @param returnType the returnType to set
     */
    public final void setReturnType(final OutputType returnType) {
        this.m_returnType = returnType;
    }

    /**
     * @param outputType {@link OutputType}'s {@link OutputType#name()}.
     * @return The parsed {@link OutputType}.
     * @throws InvalidSettingsException Wrong format, or {@code null}.
     */
    private OutputType toOutputType(final String outputType) throws InvalidSettingsException {
        try {
            return OutputType.valueOf(outputType);
        } catch (RuntimeException e) {
            throw new InvalidSettingsException("Invalid return type: " + outputType, e);
        }
    }

    /**
     * @return the hasDate
     * @see DateAndTimeCell#DateAndTimeCell(long, boolean, boolean, boolean)
     */
    public final boolean isHasDate() {
        return m_hasDate;
    }

    /**
     * @param hasDate the hasDate to set
     * @see DateAndTimeCell#DateAndTimeCell(long, boolean, boolean, boolean)
     */
    public final void setHasDate(final boolean hasDate) {
        this.m_hasDate = hasDate;
    }

    /**
     * @return the hasTime
     * @see DateAndTimeCell#DateAndTimeCell(long, boolean, boolean, boolean)
     */
    public final boolean isHasTime() {
        return m_hasTime;
    }

    /**
     * @param hasTime the hasTime to set
     * @see DateAndTimeCell#DateAndTimeCell(long, boolean, boolean, boolean)
     */
    public final void setHasTime(final boolean hasTime) {
        this.m_hasTime = hasTime;
    }

    /**
     * @return the hasMillis
     * @see DateAndTimeCell#DateAndTimeCell(long, boolean, boolean, boolean)
     */
    public final boolean isHasMillis() {
        return m_hasMillis;
    }

    /**
     * @param hasMillis the hasMillis to set
     * @see DateAndTimeCell#DateAndTimeCell(long, boolean, boolean, boolean)
     */
    public final void setHasMillis(final boolean hasMillis) {
        this.m_hasMillis = hasMillis;
    }

    /**
     * @return the logger
     */
    protected final NodeLogger getLogger() {
        return m_logger;
    }
}
