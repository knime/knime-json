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
 *   20.04.2021 (jl): created
 */
package org.knime.json.node.container.input.file;

import java.io.Serializable;
import java.util.Objects;
import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.dialog.DialogNode;
import org.knime.core.node.util.CheckUtils;

/**
 * Contains the configurations that are shared by all Container nodes.
 *
 * @author Jannik Löscher, KNIME GmbH, Konstanz, Germany
 * @since 4.4
 */
public final class ContainerNodeSharedConfiguration
    implements Serializable /* because ContainerNodeSharedDialog is a JPanel */ {

    private static final long serialVersionUID = 5712049520488842255L;

    private static final String CFG_PARAMETER_KEY = "parameterName";

    static final Predicate<String> CFG_PARAMETER_VERIFIER =
        ((Predicate<String>)Objects::nonNull).and(DialogNode.PARAMETER_NAME_PATTERN.asMatchPredicate());

    private static final String CFG_DESCRIPTION_KEY = "description";

    private static final String CFG_DESCRIPTION_DEFAULT = "";

    private static final String CFG_USE_FULLY_QUAL_NAME_KEY = "useFullyQualifiedName";

    private static final String CFG_INPUT_PATH_URL_KEY = "inputPathOrUrl";

    static final String MSG_PARAMETER_FORMAT_DESC = "Must start with a letter, followed by other lettes, "
        + "digits or single dashes or underscores. Must end with a letter.";

    static final String MSG_PARAMETER_FORMAT_REGEX_DESC =
        " (Regular expression: “" + DialogNode.PARAMETER_NAME_PATTERN.pattern() + "”)";

    private final String m_paramterDefault;

    private String m_parameter;

    private String m_description;

    private boolean m_fullyQualifiedName;

    private String m_inputPathOrUrl;

    /**
     * Constructs a new configuration that is shared between container nodes.
     *
     * @param defaultParameter the default name of the parameter
     */
    public ContainerNodeSharedConfiguration(final String defaultParameter) {
        if (!CFG_PARAMETER_VERIFIER.test(defaultParameter)) {
            throw new IllegalArgumentException(
                "Illegal default parameter name! " + MSG_PARAMETER_FORMAT_DESC + MSG_PARAMETER_FORMAT_REGEX_DESC);
        }
        m_paramterDefault = defaultParameter;
        reset();
    }

    /**
     * Resets this configuration's values to the default ones.
     */
    public void reset() {
        try {
            setParameter(m_paramterDefault);
        } catch (InvalidSettingsException e) {
            // this was checked before and should not happen
            NodeLogger.getLogger(getClass()).error("Unexpected exception: " + e.getMessage(), e);
        }
        m_description = CFG_DESCRIPTION_DEFAULT;
        m_inputPathOrUrl = "";
        m_fullyQualifiedName = false;
    }

    /**
     * @return whether a fully qualified name is used.
     */
    public boolean hasFullyQualifiedName() {
        return m_fullyQualifiedName;
    }

    /**
     * @param fullyQualifiedName the fullyQualifiedName to set
     */
    public void setFullyQualifiedName(final boolean fullyQualifiedName) {
        m_fullyQualifiedName = fullyQualifiedName;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return m_description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(final String description) {
        m_description = Objects.requireNonNull(description);
    }

    /**
     * @return the parameter name that is exposed
     */
    public String getParameter() {
        return m_parameter;
    }

    /**
     * @param parameter the parameter name that is exposed. This value must adhere to
     *            {@link DialogNode#PARAMETER_NAME_PATTERN}.
     * @throws InvalidSettingsException if the parameter is not valid.
     */
    public void setParameter(final String parameter) throws InvalidSettingsException {
        CheckUtils.checkSetting(StringUtils.isNotEmpty(parameter), "parameter name must not be null or empty");
        CheckUtils.checkSetting(DialogNode.PARAMETER_NAME_PATTERN.matcher(parameter).matches(),
            "Parameter doesn't match pattern - must start with character, followed by other characters, digits, "
                + "or single dashes or underscores:\n  Input: %s\n  Pattern: %s",
            parameter, DialogNode.PARAMETER_NAME_PATTERN.pattern());
        m_parameter = parameter;
    }

    /**
     * @return the inputPathOrUrl
     */
    public String getInputPathOrUrl() {
        return m_inputPathOrUrl;
    }

    /**
     * @param inputPathOrUrl the inputPathOrUrl to be set. This may be <code>null</code>.
     */
    public void setInputPathOrUrl(final String inputPathOrUrl) {
        m_inputPathOrUrl = Objects.requireNonNull(inputPathOrUrl);
    }

    /**
     * Saves the settings of this {@link ContainerNodeSharedConfiguration} to the given settings object.
     *
     * @param settings the settings to save to.
     */
    public void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addString(CFG_PARAMETER_KEY, m_parameter);
        settings.addString(CFG_DESCRIPTION_KEY, m_description);
        settings.addBoolean(CFG_USE_FULLY_QUAL_NAME_KEY, m_fullyQualifiedName);
        settings.addString(CFG_INPUT_PATH_URL_KEY, m_inputPathOrUrl.isEmpty() ? null : m_inputPathOrUrl);
    }

    /**
     * Validates the given settings. Please refer to the given setters. The {@link #getInputPathOrUrl()} expects a
     * string that parses into a valid URI-
     *
     * @param settings the settings to be validated.
     * @throws InvalidSettingsException if the settings were invalid
     */
    public static void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        CheckUtils.checkSetting(CFG_PARAMETER_VERIFIER.test(settings.getString(CFG_PARAMETER_KEY)),
            "Parameter doesn't match pattern - must start with character, followed by other characters, digits, "
                + "or single dashes or underscores:\n  Input: %s\n  Pattern: %s",
            settings.getString(CFG_PARAMETER_KEY), DialogNode.PARAMETER_NAME_PATTERN.pattern());
        CheckUtils.checkSetting(settings.getString(CFG_DESCRIPTION_KEY) != null,
            "Please supply a non-null description.");
        settings.getBoolean(CFG_USE_FULLY_QUAL_NAME_KEY);
        final var inputPathUrl = settings.getString(CFG_INPUT_PATH_URL_KEY);
        CheckUtils.checkSetting(inputPathUrl == null || StringUtils.isNotEmpty(inputPathUrl),
            "Please supply a non-null, non-empty input path or URL.");
    }

    /**
     * Loads the validated settings from the given settings object.
     *
     * @param settings the settings from which to load.
     * @throws InvalidSettingsException if the needed settings are not present
     */
    public void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        setParameter(settings.getString(CFG_PARAMETER_KEY));
        setDescription(settings.getString(CFG_DESCRIPTION_KEY));
        setFullyQualifiedName(settings.getBoolean(CFG_USE_FULLY_QUAL_NAME_KEY));
        final String uri = settings.getString(CFG_INPUT_PATH_URL_KEY);
        if (null == uri) {
            setInputPathOrUrl("");
        } else {
            setInputPathOrUrl(uri);
        }
    }
}
