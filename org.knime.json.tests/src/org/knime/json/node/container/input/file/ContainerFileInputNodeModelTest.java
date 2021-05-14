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
 *   14.05.2021 (jl): created
 */
package org.knime.json.node.container.input.file;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.knime.core.node.NodeModelWarningListener;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowContext;
import org.knime.core.node.workflow.WorkflowCreationHelper;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.FSFiles;
import org.knime.filehandling.core.connections.RelativeTo;

/**
 * Test the {@link ContainerFileInputNodeModel}
 *
 * @author Jannik Löscher, KNIME GmbH, Konstanz, Germany
 */
public class ContainerFileInputNodeModelTest {

    private ContainerFileInputNodeModel m_model;

    private NodeID m_wfManagerNodeID;

    private static Path m_pathValid;

    private static final String VALID_PATH =
        ContainerFileInputNodeModel.TEMP_FILE_PREFIX + "123456780/externalFile.bin";

    private static Path m_pathInvalidAbsolute;

    private static Path m_pathInvalidTooShort;

    private static Path m_pathInvalidTooLong;

    private static Path m_pathInvalidWrongDirectoryName;

    private static Path m_mockWorkflowDir;

    private static Path createInternalsDir(final String pathValue) throws IOException {
        final var result = Files.createTempDirectory("ContainerFileInputModelTest_internals-");

        final var settings = new NodeSettings(ContainerFileInputNodeModel.INT_SETTINGS_NAME);
        settings.addString(ContainerFileInputNodeModel.INT_CFG_EXTERNAL_LOCATION_KEY, pathValue);

        final var internalsFile = result.resolve(ContainerFileInputNodeModel.INT_FILE_NAME);

        try (final var internalsStream = Files.newOutputStream(internalsFile)) {
            settings.saveToXML(internalsStream);
        }

        return result;
    }

    private static Path createMockWorkflowDir() throws IOException {
        m_mockWorkflowDir = Files.createTempDirectory("ContainerFileInputModelTest_workflow-");
        Files.createDirectories(m_mockWorkflowDir.resolve("data"));
        return m_mockWorkflowDir;
    }

    /**
     * Create internals directories and mock workflow for testing
     *
     * @throws java.lang.Exception if the files could not be created
     */
    @Before
    public void setUp() throws Exception {
        m_pathValid = createInternalsDir(VALID_PATH);
        m_pathInvalidAbsolute = createInternalsDir("/" + VALID_PATH);
        m_pathInvalidTooShort = createInternalsDir("somefile.bin");
        // this still should point to the correct file but it's not what we would expect
        m_pathInvalidTooLong = createInternalsDir("../../../../" + VALID_PATH);
        m_pathInvalidWrongDirectoryName = createInternalsDir("notTheRightPrefix-123456780/externalFile.bin");

        // setup a mock workflow for testing
        MountPointFileSystemAccessMock.enabled = true;
        final var wfContext = new WorkflowContext.Factory(createMockWorkflowDir().toFile()).createContext();
        final var wfHelper = new WorkflowCreationHelper();
        wfHelper.setWorkflowContext(wfContext);
        final var manager = WorkflowManager.ROOT.createAndAddProject("ContainerFileInputModelTest", wfHelper);
        m_wfManagerNodeID = manager.getID();
        final var nodeID = manager.createAndAddNode(new ContainerFileInputNodeFactory());
        final var container = (NativeNodeContainer)manager.getNodeContainer(nodeID);
        m_model = (ContainerFileInputNodeModel)container.getNode().getNodeModel();
        NodeContext.pushContext(container);
    }

    /**
     * Tests {@link ContainerFileInputNodeModel#loadInternals(java.io.File, org.knime.core.node.ExecutionMonitor)} with
     * a valid saved file path.
     *
     * @throws Exception if something did not behave as expected
     */
    @Test
    public void testValidPath() throws Exception {
        // setup for warning checks (because file should not exist)
        final var warningSet = new boolean[1];
        final NodeModelWarningListener listener = s -> warningSet[0] = true;
        m_model.addWarningListener(listener);

        m_model.loadInternals(m_pathValid.toFile(), null);

        // test if path was set correctly
        assertTrue("Expected an external file to be set", m_model.m_externalLocation.isPresent());
        final var setLocation = m_model.m_externalLocation.get();

        assertEquals("Expected category to be relative", FSCategory.RELATIVE, setLocation.getFSCategory());
        assertTrue("Expected there to be a file system specifier", setLocation.getFileSystemSpecifier().isPresent());
        assertEquals("Expected file system specifier to be workflow data area relative",
            RelativeTo.WORKFLOW_DATA.getSettingsValue(), setLocation.getFileSystemSpecifier().get());
        assertEquals("Expected the path to be set to the configured value", VALID_PATH, setLocation.getPath());

        // test warning message
        assertTrue("Expected that a warning message was set because the file does not exist", warningSet[0]);
        m_model.removeWarningListener(listener);
    }

    /**
     * Tests {@link ContainerFileInputNodeModel#loadInternals(java.io.File, org.knime.core.node.ExecutionMonitor)} with
     * a saved file path that is absolute.
     *
     * @throws Exception if something did not behave as expected
     */
    @Test(expected = IOException.class)
    public void testInvalidPathAbsolute() throws Exception {
        m_model.loadInternals(m_pathInvalidAbsolute.toFile(), null);
    }

    /**
     * Tests {@link ContainerFileInputNodeModel#loadInternals(java.io.File, org.knime.core.node.ExecutionMonitor)} with
     * a saved file path that is too short.
     *
     * @throws Exception if something did not behave as expected
     */
    @Test(expected = IOException.class)
    public void testInvalidPathTooShort() throws Exception {
        m_model.loadInternals(m_pathInvalidTooShort.toFile(), null);
    }

    /**
     * Tests {@link ContainerFileInputNodeModel#loadInternals(java.io.File, org.knime.core.node.ExecutionMonitor)} with
     * a saved file path that is too long.
     *
     * @throws Exception if something did not behave as expected
     */
    @Test(expected = IOException.class)
    public void testInvalidPathTooLong() throws Exception {
        m_model.loadInternals(m_pathInvalidTooLong.toFile(), null);
    }

    /**
     * Tests {@link ContainerFileInputNodeModel#loadInternals(java.io.File, org.knime.core.node.ExecutionMonitor)} with
     * a saved file path's directory has a wrong prefix.
     *
     * @throws Exception if something did not behave as expected
     */
    @Test(expected = IOException.class)
    public void testInvalidPathWrongDirectoryName() throws Exception {
        m_model.loadInternals(m_pathInvalidWrongDirectoryName.toFile(), null);
    }

    /**
     * Remove internals directories and mock workflow for testing
     *
     * @throws java.lang.Exception if the files could not be deleted
     */
    @After
    public void tearDown() throws Exception {
        FSFiles.deleteRecursively(m_pathValid);
        FSFiles.deleteRecursively(m_pathInvalidAbsolute);
        FSFiles.deleteRecursively(m_pathInvalidTooShort);
        FSFiles.deleteRecursively(m_pathInvalidTooLong);
        FSFiles.deleteRecursively(m_pathInvalidWrongDirectoryName);

        NodeContext.removeLastContext();
        WorkflowManager.ROOT.removeProject(m_wfManagerNodeID);

        FSFiles.deleteRecursively(m_mockWorkflowDir);
        MountPointFileSystemAccessMock.enabled = false;
    }
}
