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
 *   Apr 30, 2018 (Tobias Urhaug, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.json.node.container.input.variable;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import org.knime.core.data.json.container.variables.ContainerVariableJsonSchema;
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
import org.knime.json.node.container.input.variable2.ContainerVariableInputNodeFactory2;
import org.knime.json.node.container.input.variable2.ContainerVariableJsonSchema2;
import org.knime.json.node.container.input.variable2.ContainerVariableMapper2;
import org.knime.json.node.container.io.FilePathOrURLReader;
import org.knime.json.node.container.mappers.ContainerVariableMapper;

import jakarta.json.JsonValue;

/**
 * The node model for the Container Input (Variable) node.
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 * @since 3.6
 * @deprecated superseded by {@link ContainerVariableInputNodeFactory2}
 */
@Deprecated(since = "4.4")
final class ContainerVariableInputNodeModel extends NodeModel implements InputNode {

    private JsonValue m_externalValue;

    private ContainerVariableInputNodeConfiguration m_configuration = new ContainerVariableInputNodeConfiguration();

    /**
     * Constructor for the node model.
     */
    ContainerVariableInputNodeModel() {
        super(new PortType[]{FlowVariablePortObject.TYPE_OPTIONAL}, new PortType[]{FlowVariablePortObject.TYPE});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData, final ExecutionContext exec) throws Exception {
        JsonValue externalJsonValue = getExternalVariableInput();
        if (externalJsonValue == null && inData[0] == null) {
            setWarningMessage("Default variables are output");
        }
        return new PortObject[]{FlowVariablePortObject.INSTANCE};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        JsonValue externalJsonValue = getExternalVariableInput();
        if (externalJsonValue != null) {
            pushVariablesToStack(externalJsonValue.toString());
        } else if (inSpecs[0] == null) {
            pushVariablesToStack(ContainerVariableDefaultJsonStructure.asString());
        }

        return new PortObjectSpec[]{FlowVariablePortObjectSpec.INSTANCE};
    }

    private JsonValue getExternalVariableInput() throws InvalidSettingsException {
        Optional<String> inputPathOrUrl = m_configuration.getInputPathOrUrl();
        if (inputPathOrUrl.isPresent()) {
            return FilePathOrURLReader.resolveToJson(inputPathOrUrl.get());
        } else {
            return m_externalValue;
        }
    }

    /**
     * Decode flow variables from given JSON string. Understands the schema of the revised Container Input (Variable)
     * node as well as the deprecated one
     *
     * This is to provide "upwards compatibility" since callers (e.g. Call Workflow nodes) have been adjusted to work
     * with the revised CI(V) node. Callers cannot easily distinguish the type of schema they should adhere to. See
     * AP-17403.
     *
     * @param json A String encoding of JSON data, adhering either to {@link ContainerVariableJsonSchema} or
     *            {@link ContainerVariableJsonSchema2}.
     * @return List of singleton maps. The entry of a singleton map is a single flow variable, the map key and value
     *         being the flow variable's name and value, respectively. See
     *         {@link ContainerVariableMapper#createVariables(Collection)}.
     * @throws InvalidSettingsException
     */
    private List<Map<String, Object>> decode(final String json) throws InvalidSettingsException {
        try { // ... parsing according to old schema
              // ContainerVariableJsonSchema2 also matches a JSON that matches the old schema, so we need to check for
              // this one first. This is because CVJS2 puts no constraints on the value types.
            return ContainerVariableMapper.toContainerVariableJsonSchema(json).getVariables();
        } catch (InvalidSettingsException oldSchemaParseException) {
            // Try parsing according to new schema (simplified or full)
            // Infer whether simplified schema is used
            String simplifiedSchemaName = ContainerVariableMapper2.hasSimpleSchema(json) ?
                    m_configuration.getParameterName() : null;
            ContainerVariableJsonSchema2 newSchemaDecodeResult;
            try {
                // Implementation decides based on nullness of simplifiedSchemaName whether to parse it as a simple or a full
                //   schema.
                newSchemaDecodeResult =
                        ContainerVariableMapper2.toContainerVariableJsonSchema(json, simplifiedSchemaName);
            } catch (InvalidSettingsException e) { // NOSONAR
                // Throw this exception for backwards compatibility: If a JSON is supplied that matches neither the
                // new nor the old schema, the same exception as before should be thrown.
                throw oldSchemaParseException;
            }
            Map<String, Object> variables = newSchemaDecodeResult.getVariables();
            // Must not allow this to accept types that are not allowed by pushVariablesToStack (which sort of
            //  defines an interface depicting what flow variable types this node can output).
            // In fact, ContainerVariableJsonSchema2 has no constraints on the value type whatsoever. This means
            //  complex types such as collections may appear here as well.
            if (!variables.values().stream().allMatch(o -> {
                return Integer.class.equals(o.getClass()) || Double.class.equals(o.getClass())
                    || String.class.equals(o.getClass());
            })) {
                throw oldSchemaParseException;
            }
            // Unpack into singleton maps to have same structure as old format.
            return variables.entrySet().stream() //
                .map(entry -> Collections.singletonMap(entry.getKey(), entry.getValue())) //
                .collect(Collectors.toList());
        }
    }

    private void pushVariablesToStack(final String json) throws InvalidSettingsException {
        var variables = decode(json);
        for (Map<String, Object> variable : variables) {
            for (Entry<String, Object> variableEntry : variable.entrySet()) {
                String name = variableEntry.getKey();
                Object value = variableEntry.getValue();
                if (Integer.class.equals(value.getClass())) {
                    pushFlowVariableInt(name, (int)value);
                } else if (Double.class.equals(value.getClass())) {
                    pushFlowVariableDouble(name, (double)value);
                } else if (String.class.equals(value.getClass())) {
                    pushFlowVariableString(name, (String)value);
                } else {
                    throw new InvalidSettingsException(
                        "Variable \"" + name + "\" has invalid variable class \"" + value + "\"");
                }
            }
        }
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
        m_configuration.save(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_configuration = new ContainerVariableInputNodeConfiguration().loadInModel(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        new ContainerVariableInputNodeConfiguration().loadInModel(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExternalNodeData getInputData() {
        JsonValue value =
            m_externalValue != null ? m_externalValue : ContainerVariableDefaultJsonStructure.asJsonValue();
        return ExternalNodeData.builder(m_configuration.getParameterName())
            .description(m_configuration.getDescription()).jsonValue(value).build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validateInputData(final ExternalNodeData inputData) throws InvalidSettingsException {
        // nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setInputData(final ExternalNodeData inputData) throws InvalidSettingsException {
        m_externalValue = inputData == null ? null : inputData.getJSONValue();
    }

    @Override
    public boolean isInputDataRequired() {
        return false;
    }

    @Override
    public boolean isUseAlwaysFullyQualifiedParameterName() {
        return m_configuration.isUseFQNParamName();
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