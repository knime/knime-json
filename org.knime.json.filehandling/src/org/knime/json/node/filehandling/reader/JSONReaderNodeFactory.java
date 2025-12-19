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
 *   Apr 7, 2021 (Moditha): created
 */
package org.knime.json.node.filehandling.reader;

import static org.knime.node.impl.description.PortDescription.dynamicPort;
import static org.knime.node.impl.description.PortDescription.fixedPort;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.knime.base.node.io.filehandling.webui.reader2.MultiFileSelectionParameters;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.json.JSONCell;
import org.knime.core.node.NodeDescription;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.core.node.context.url.URLConfiguration;
import org.knime.core.webui.node.dialog.NodeDialog;
import org.knime.core.webui.node.dialog.NodeDialogFactory;
import org.knime.core.webui.node.dialog.NodeDialogManager;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultKaiNodeInterface;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeDialog;
import org.knime.core.webui.node.dialog.kai.KaiNodeInterface;
import org.knime.core.webui.node.dialog.kai.KaiNodeInterfaceFactory;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.defaultnodesettings.EnumConfig;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.SettingsModelReaderFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filtermode.SettingsModelFilterMode.FilterMode;
import org.knime.filehandling.core.node.table.reader.AbstractTableReaderNodeFactory;
import org.knime.filehandling.core.node.table.reader.GenericTableReader;
import org.knime.filehandling.core.node.table.reader.MultiTableReadFactory;
import org.knime.filehandling.core.node.table.reader.ProductionPathProvider;
import org.knime.filehandling.core.node.table.reader.ReadAdapterFactory;
import org.knime.filehandling.core.node.table.reader.TableReader;
import org.knime.filehandling.core.node.table.reader.preview.dialog.AbstractPathTableReaderNodeDialog;
import org.knime.filehandling.core.node.table.reader.type.hierarchy.TreeTypeHierarchy;
import org.knime.filehandling.core.node.table.reader.type.hierarchy.TypeHierarchy;
import org.knime.filehandling.core.node.table.reader.type.hierarchy.TypeTester;
import org.knime.json.node.filehandling.reader.api.JSONReadAdapterFactory;
import org.knime.node.impl.description.DefaultNodeDescriptionUtil;
import org.knime.node.impl.description.PortDescription;

/**
 * Node factory for the JSON reader node.
 *
 * @author Moditha Hewasinghage, KNIME GmbH, Berlin, Germany
 * @author Paul Baernreuther, KNIME GmbH, Germany
 * @author AI Migration Pipeline v1.2
 */
@SuppressWarnings("restriction")
public final class JSONReaderNodeFactory extends AbstractTableReaderNodeFactory<JSONReaderConfig, DataType, DataValue>
    implements NodeDialogFactory, KaiNodeInterfaceFactory {

    private static final TypeHierarchy<DataType, DataType> TYPE_HIERARCHY =
        TreeTypeHierarchy.builder(createTypeTester(JSONCell.TYPE)).build();

    private static TypeTester<DataType, DataType> createTypeTester(final DataType type) {
        return TypeTester.createTypeTester(type, s -> true);
    }

    @Override
    protected MultiTableReadFactory<FSPath, JSONReaderConfig, DataType>
        createMultiTableReadFactory(final GenericTableReader<FSPath, JSONReaderConfig, DataType, DataValue> reader) {
        final MultiTableReadFactory<FSPath, JSONReaderConfig, DataType> multiTableReadFactory =
            super.createMultiTableReadFactory(reader);
        return new JSONMultiTableReadFactory(multiTableReadFactory);
    }

    @Override
    protected TableReader<JSONReaderConfig, DataType, DataValue> createReader() {
        return new JSONReader();
    }

    @Override
    protected AbstractPathTableReaderNodeDialog<JSONReaderConfig, DataType> createNodeDialogPane(
        final NodeCreationConfiguration creationConfig,
        final MultiTableReadFactory<FSPath, JSONReaderConfig, DataType> readFactory,
        final ProductionPathProvider<DataType> defaultProductionPathFn) {

        return new JSONReaderNodeDialog(createPathSettings(creationConfig), createConfig(creationConfig), readFactory,
            defaultProductionPathFn);
    }

    @Override
    protected SettingsModelReaderFileChooser createPathSettings(final NodeCreationConfiguration nodeCreationConfig) {
        final SettingsModelReaderFileChooser settingsModel = new SettingsModelReaderFileChooser("file_selection",
            nodeCreationConfig.getPortConfig().orElseThrow(IllegalStateException::new), FS_CONNECT_GRP_ID,
            EnumConfig.create(FilterMode.FILE, FilterMode.FILES_IN_FOLDERS));
        final Optional<? extends URLConfiguration> urlConfig = nodeCreationConfig.getURLConfig();
        if (urlConfig.isPresent()) {
            settingsModel.setLocation(MultiFileSelectionParameters.urlToSupportedFSLocation(urlConfig.get().getUrl()));
        }
        return settingsModel;
    }

    @Override
    protected ReadAdapterFactory<DataType, DataValue> getReadAdapterFactory() {
        return JSONReadAdapterFactory.INSTANCE;
    }

    @Override
    protected String extractRowKey(final DataValue value) {
        return value.toString();
    }

    @Override
    protected TypeHierarchy<DataType, DataType> getTypeHierarchy() {
        return TYPE_HIERARCHY;
    }

    @Override
    protected JSONMultiTableReadConfig createConfig(final NodeCreationConfiguration nodeCreationConfig) {
        return new JSONMultiTableReadConfig();
    }

    private static final String NODE_NAME = "JSON Reader";

    private static final String NODE_ICON = "./jsonreader.png";

    private static final String SHORT_DESCRIPTION = """
            Reads .json files to JSON values.
            """;

    private static final String FULL_DESCRIPTION =
        """
                <p> This node reads the .json file and parses it as JSON value. </p> <p> <i>This node can access a
                    variety of different</i> <a
                    href="https://docs.knime.com/2021-06/analytics_platform_file_handling_guide/index.html#analytics-platform-file-systems"><i>file
                    systems.</i></a> <i>More information about file handling in KNIME can be found in the official</i> <a
                    href="https://docs.knime.com/latest/analytics_platform_file_handling_guide/index.html"><i>File Handling
                    Guide.</i></a> </p>
                """;

    private static final List<PortDescription> INPUT_PORTS =
        List.of(dynamicPort("File System Connection", "File system connection", """
                The file system connection.
                """));

    private static final List<PortDescription> OUTPUT_PORTS = List.of(fixedPort("JSON table", """
            Table with the read JSON values
            """));

    @Override
    public NodeDialogPane createNodeDialogPane() {
        return NodeDialogManager.createLegacyFlowVariableNodeDialog(createNodeDialog());
    }

    @Override
    public NodeDialog createNodeDialog() {
        return new DefaultNodeDialog(SettingsType.MODEL, JSONReaderNodeParameters.class);
    }

    @Override
    public NodeDescription createNodeDescription() {
        return DefaultNodeDescriptionUtil.createNodeDescription( //
            NODE_NAME, //
            NODE_ICON, //
            INPUT_PORTS, //
            OUTPUT_PORTS, //
            SHORT_DESCRIPTION, //
            FULL_DESCRIPTION, //
            List.of(), //
            JSONReaderNodeParameters.class, //
            null, //
            NodeType.Source, //
            List.of(), //
            null //
        );
    }

    @Override
    public KaiNodeInterface createKaiNodeInterface() {
        return new DefaultKaiNodeInterface(Map.of(SettingsType.MODEL, JSONReaderNodeParameters.class));
    }

}
