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
 * ------------------------------------------------------------------------
 */
package org.knime.json.node.filehandling.writer;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.json.JSONCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.NodeParametersInputImpl;
import org.knime.core.webui.node.dialog.defaultdialog.NodeParametersUtil;
import org.knime.core.webui.node.dialog.defaultdialog.setting.singleselection.RowIDChoice;
import org.knime.core.webui.node.dialog.defaultdialog.setting.singleselection.StringOrEnum;
import org.knime.testing.node.dialog.DefaultNodeSettingsSnapshotTest;
import org.knime.testing.node.dialog.SnapshotTestConfiguration;
import org.knime.testing.node.dialog.updates.DialogUpdateSimulator;

/**
 * Snapshot test for {@link JSONWriterNodeParameters}.
 *
 * @author AI Migration Pipeline
 */
@SuppressWarnings("restriction")
final class JSONWriterNodeParametersTest extends DefaultNodeSettingsSnapshotTest {

    JSONWriterNodeParametersTest() {
        super(getConfig());
    }

    private static SnapshotTestConfiguration getConfig() {
        return SnapshotTestConfiguration.builder() //
            .withInputPortObjectSpecs(createInputPortSpecs()) //
            .testJsonFormsForModel(JSONWriterNodeParameters.class) //
            .testJsonFormsWithInstance(SettingsType.MODEL, () -> readSettings()) //
            .testNodeSettingsStructure(() -> readSettings()) //
            .build();
    }

    private static JSONWriterNodeParameters readSettings() {
        try {
            var path = getSnapshotPath(JSONWriterNodeParameters.class).getParent().resolve("node_settings")
                .resolve("JSONWriterNodeParameters.xml");
            try (var fis = new FileInputStream(path.toFile())) {
                var nodeSettings = NodeSettings.loadFromXML(fis);
                return NodeParametersUtil.loadSettings(nodeSettings.getNodeSettings(SettingsType.MODEL.getConfigKey()),
                    JSONWriterNodeParameters.class);
            }
        } catch (IOException | InvalidSettingsException e) {
            throw new IllegalStateException(e);
        }
    }

    private static PortObjectSpec[] createInputPortSpecs() {
        return new PortObjectSpec[]{createDefaultTestTableSpec()};
    }

    private static DataTableSpec createDefaultTestTableSpec() {
        return new DataTableSpec(new String[]{"json_col"}, new DataType[]{DataType.getType(JSONCell.class)});
    }

    @Test
    void testAutoGuessValueWithNullTableSpec() {
        final var context = NodeParametersInputImpl.createDefaultNodeSettingsContext(
            new PortType[]{BufferedDataTable.TYPE}, new PortObjectSpec[]{null}, null, null);

        final var parameters = new JSONWriterNodeParameters();
        parameters.m_fileNameColumn = new StringOrEnum<>((String)null);
        final var simulator = new DialogUpdateSimulator(Map.of(SettingsType.MODEL, parameters), context);

        final var result = simulator.simulateAfterOpenDialog();

        final var valueUpdate = result.getValueUpdateAt("fileNameColumn");
        assertThat(valueUpdate).isEqualTo(new StringOrEnum<>(RowIDChoice.ROW_ID));
    }

    @Test
    void testAutoGuessValueWithStringColumn() {
        final var inputSpec = new DataTableSpec(new String[]{"an_integer", "first_string", "second_string"},
            new DataType[]{DataType.getType(IntCell.class), DataType.getType(StringCell.class),
                DataType.getType(StringCell.class)});

        final var context = NodeParametersInputImpl.createDefaultNodeSettingsContext(
            new PortType[]{BufferedDataTable.TYPE}, new PortObjectSpec[]{inputSpec}, null, null);

        final var parameters = new JSONWriterNodeParameters();
        parameters.m_fileNameColumn = new StringOrEnum<>((String)null);
        final var simulator = new DialogUpdateSimulator(Map.of(SettingsType.MODEL, parameters), context);

        final var result = simulator.simulateAfterOpenDialog();

        final var valueUpdate = result.getValueUpdateAt("fileNameColumn");
        assertThat(valueUpdate).isEqualTo(new StringOrEnum<RowIDChoice>("first_string"));
    }

}
