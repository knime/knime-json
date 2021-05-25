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
 *   17.05.2021 (jl): created
 */
package org.knime.json.node.container.input.variable2;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.json.JsonValue;

import org.knime.base.node.io.variablecreator.SettingsModelVariables;
import org.knime.base.node.io.variablecreator.SettingsModelVariables.Type;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.dialog.ExternalNodeData;
import org.knime.core.node.dialog.InputNode;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.flowvariable.FlowVariablePortObject;
import org.knime.core.node.port.flowvariable.FlowVariablePortObjectSpec;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.VariableType;
import org.knime.core.util.Pair;
import org.knime.json.node.container.input.file.ContainerNodeSharedConfiguration;

/**
 * The node model for the Container Input (Variable) node.
 *
 * @author Jannik Löscher, KNIME GmbH, Konstanz, Germany
 * @since 4.4
 */
final class ContainerVariableInputNodeModel2 extends NodeModel implements InputNode {

    static final String SETTINGS_MODEL_CONFIG_NAME = "variableSpecificationDefinition";

    static final Type[] SUPPORTED_VARIABLE_TYPES = {Type.STRING, Type.INTEGER, Type.DOUBLE, Type.BOOLEAN};

    private Optional<JsonValue> m_externalValue;

    private final ContainerNodeSharedConfiguration m_configShared;

    private final SettingsModelVariables m_variableSelection;

    private final ContainerVariableInputNodeConfiguration2 m_configuration;

    /**
     * Constructor for the node model.
     */
    ContainerVariableInputNodeModel2() {
        super(new PortType[]{FlowVariablePortObject.TYPE_OPTIONAL}, new PortType[]{FlowVariablePortObject.TYPE});

        m_externalValue = Optional.empty();
        m_configShared = new ContainerNodeSharedConfiguration("variable-input");
        m_variableSelection = new SettingsModelVariables(SETTINGS_MODEL_CONFIG_NAME, SUPPORTED_VARIABLE_TYPES,
            getAvailableFlowVariables(Type.getAllTypes()));
        m_configuration = new ContainerVariableInputNodeConfiguration2();
        m_variableSelection.setEnabled(m_configuration.isRequireMatchSpecification());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData, final ExecutionContext exec) throws Exception {
        if (m_configuration.isRequireMatchSpecification() && m_variableSelection.getRowCount() == 0) {
            setWarningMessage("Specification is empty; no variables can be received");
        } else if (m_configuration.isRequireMatchSpecification() && m_externalValue.isEmpty()) {
            setWarningMessage("Default variables are output");
        }
        m_externalValue = Optional.empty(); // reset here so that value is not available anymore after reset
        return new PortObject[]{FlowVariablePortObject.INSTANCE};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        final var specificationAndDefaults = getSpecificationAndDefaults();
        if (specificationAndDefaults.map(Map::isEmpty).orElse(false)) {
            setWarningMessage("Specification is empty; no variables can be received");
        }
        if (m_externalValue.isPresent()) {
            pushVariablesToStack(m_externalValue.get().toString(), false, specificationAndDefaults);
        } else if (specificationAndDefaults.isPresent()) {
            final var entries = new ArrayList<>(specificationAndDefaults.get().entrySet());
            Collections.reverse(entries);
            for (final var entry : entries) {
                final var type = entry.getValue().getVariableType();
                pushFlowVariableFromType(Optional.empty(), type, entry.getValue().getValue(type), entry.getKey());
            }
        }

        return new PortObjectSpec[]{FlowVariablePortObjectSpec.INSTANCE};
    }

    private void pushVariablesToStack(final String json, final boolean dryrun,
        final Optional<Map<String, FlowVariable>> specificationDefaults) throws InvalidSettingsException { // NOSONAR: avoid unnecessary unpacking and re-packing
        String simpleVariableName = null;
        if (specificationDefaults.map(m -> m.size() == 1).orElse(false) && m_configuration.hasSimpleJsonSpec()) {
            simpleVariableName = m_configShared.getParameter();
        }
        final var variableInput = ContainerVariableMapper2.toContainerVariableJsonSchema(json, simpleVariableName);

        if (specificationDefaults.isPresent()) {
            final var diff =
                removeIntersectingElements(specificationDefaults.get().keySet(), variableInput.getVariables().keySet());
            CheckUtils.checkSetting(diff.getFirst().isEmpty() && diff.getSecond().isEmpty(),
                "Input does not conform to specification: missing expected variables: %s, unexpected variables: %s",
                diff.getFirst(), diff.getSecond());
        }

        var failed = new StringBuilder("");
        final var entries = new ArrayList<>(variableInput.getVariables().entrySet());
        Collections.reverse(entries); // ensure right order in output
        for (final var variable : entries) {
            final var name = variable.getKey();
            final var value = variable.getValue();
            try {

                final var actualType = getTypeFromClass(value);
                final var specificationType = specificationDefaults.map(m -> m.get(name).getVariableType());
                if (specificationType.isPresent()) {
                    CheckUtils.checkSetting(typeConforms(specificationType.get(), actualType),
                        "The specification demands type %s but %s was provided",
                        specificationType.get().getIdentifier(), actualType.getIdentifier());
                }

                if (!dryrun) {
                    pushFlowVariableFromType(specificationType, actualType, value, name);
                }
            } catch (InvalidSettingsException e) {
                final var msg = name + ": " + e.getMessage();
                getLogger().error(msg, e);
                failed.append(msg + "; ");
            }
        }

        if (failed.length() != 0) {
            failed.setLength(failed.length() - 2);
            throw new InvalidSettingsException(failed.toString());
        }
    }

    private static boolean typeConforms(final VariableType<?> specificationType, final VariableType<?> actualType) {
        return (specificationType.equals(VariableType.DoubleType.INSTANCE)
            && actualType.equals(VariableType.IntType.INSTANCE)) || actualType.equals(specificationType);

    }

    private void pushFlowVariableFromType(final Optional<? extends VariableType<?>> specificationType, // NOSONAR: avoid unnecessary unpacking and re-packing
        final VariableType<?> actualType, final Object value, final String name) throws InvalidSettingsException {
        final var typeObject = specificationType.map(e -> (Object)e).orElse(actualType);

        if (typeObject.equals(VariableType.StringType.INSTANCE)) {
            pushFlowVariableString(name, (String)value);
        } else if (typeObject.equals(VariableType.IntType.INSTANCE)) {
            pushFlowVariableInt(name, (int)value);
        } else if (typeObject.equals(VariableType.DoubleType.INSTANCE)) {
            pushFlowVariableDouble(name, ((Number)value).doubleValue());
        } else if (typeObject.equals(VariableType.BooleanType.INSTANCE)) {
            pushFlowVariable(name, VariableType.BooleanType.INSTANCE, (Boolean)value);
        } else {
            throw new InvalidSettingsException(
                "Variable \"" + name + "\" has invalid variable class \"" + value + "\"");
        }

    }

    static VariableType<?> getTypeFromClass(final Object value) throws InvalidSettingsException { // NOSONAR: Any other syntax is not valid
        CheckUtils.checkSetting(value != null, "Value must not be null!");
        final var clazz = value.getClass();
        if (String.class.equals(clazz)) {
            return VariableType.StringType.INSTANCE;
        } else if (Integer.class.equals(clazz)) {
            return VariableType.IntType.INSTANCE;
        } else if (Double.class.equals(clazz)) {
            return VariableType.DoubleType.INSTANCE;
        } else if (Boolean.class.equals(clazz)) {
            return VariableType.BooleanType.INSTANCE;
        } else if (Long.class.equals(clazz)) {
            throw new InvalidSettingsException("Value must not be a " + Type.LONG.getIdentifier() + " ("
                + Type.INTEGER.getIdentifier() + " out of bounds)!");
        } else if (List.class.isAssignableFrom(clazz)) {
            throw new InvalidSettingsException("Value must not be an array!");
        } else if (Map.class.isAssignableFrom(clazz)) {
            throw new InvalidSettingsException("Value must not be an object!");
        } else {
            throw new InvalidSettingsException("Unsupported type class: " + clazz.getSimpleName());
        }
    }

    private Optional<Map<String, FlowVariable>> getSpecificationAndDefaults() {
        if (m_configuration.isRequireMatchSpecification()) {
            final var variables = new LinkedHashMap<String, FlowVariable>(m_variableSelection.getRowCount());

            for (int row = 0; row  < m_variableSelection.getRowCount(); row++) {
                final var type = m_variableSelection.getType(row);
                final var name = m_variableSelection.getName(row);
                final var val = m_variableSelection.getValue(row);

                switch (type) {
                    case STRING:
                        variables.put(name, new FlowVariable(name, VariableType.StringType.INSTANCE, (String)val));
                        break;
                    case INTEGER:
                        variables.put(name, new FlowVariable(name, VariableType.IntType.INSTANCE, (Integer)val));
                        break;
                    case DOUBLE:
                        variables.put(name, new FlowVariable(name, VariableType.DoubleType.INSTANCE, (Double)val));
                        break;
                    case BOOLEAN:
                        variables.put(name, new FlowVariable(name, VariableType.BooleanType.INSTANCE, (Boolean)val));
                        break;
                    default:
                        throw new IllegalStateException("Unsupported variable type!");
                }

            }
            return Optional.of(variables);
        } else {
            return Optional.empty();
        }
    }

    private static <T> Pair<Set<T>, Set<T>> removeIntersectingElements(final Set<T> a, final Set<T> b) {
        final var onlyA = new LinkedHashSet<T>(a);
        final var onlyB = new LinkedHashSet<T>(b);
        onlyA.removeAll(b);
        onlyB.removeAll(a);
        return new Pair<>(onlyA, onlyB);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        //No internal state.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_configShared.saveSettingsTo(settings);
        m_configuration.save(settings);
        m_variableSelection.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_configShared.loadValidatedSettingsFrom(settings);
        m_configuration.loadInModel(settings);
        m_variableSelection.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        ContainerNodeSharedConfiguration.validateSettings(settings);
        m_configuration.loadInModel(settings);
        m_variableSelection.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExternalNodeData getInputData() {
        JsonValue value;
        try {
            value = ContainerVariableMapper2.toContainerVariableJsonValue(
                getSpecificationAndDefaults().orElse(Collections.emptyMap()), m_configuration.hasSimpleJsonSpec());
        } catch (InvalidSettingsException e) { // this should not happen
            getLogger().coding("Could not build default values!", e);
            value = null;
        }
        return ExternalNodeData.builder(m_configShared.getParameter()).description(m_configShared.getDescription())
            .jsonValue(value).build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validateInputData(final ExternalNodeData inputData) throws InvalidSettingsException {
        try {
            CheckUtils.checkSetting(inputData.getJSONValue() != null, "Expected a JSON value.");
            pushVariablesToStack(inputData.getJSONValue().toString(), false, getSpecificationAndDefaults());
        } catch (InvalidSettingsException e) {
            // re-throw with parameter name as context
            throw new InvalidSettingsException(m_configShared.getParameter() + ": " + e.getMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setInputData(final ExternalNodeData inputData) throws InvalidSettingsException {
        m_externalValue = Optional.of(inputData.getJSONValue());
    }

    @Override
    public boolean isUseAlwaysFullyQualifiedParameterName() {
        return m_configShared.hasFullyQualifiedName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        //No internal state.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        //No internal state.
    }

}