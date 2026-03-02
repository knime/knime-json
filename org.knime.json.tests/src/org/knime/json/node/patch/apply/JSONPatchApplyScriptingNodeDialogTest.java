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
 */
package org.knime.json.node.patch.apply;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.knime.base.node.preproc.stringmanipulation.manipulator.Manipulator;
import org.knime.core.node.workflow.NodeContext;
import org.knime.testing.util.WorkflowManagerUtil;

/**
 * Tests for JSON Patch apply scripting dialog behavior.
 */
@SuppressWarnings("restriction")
class JSONPatchApplyScriptingNodeDialogTest {

	@BeforeAll
	static void mockNodeContext() throws IOException {
		final var wfm = WorkflowManagerUtil.createEmptyWorkflow();
		final var nodeContainer = WorkflowManagerUtil.createAndAddNode(wfm, new JSONPatchApplyNodeFactory());
		NodeContext.pushContext(nodeContainer);
	}

	@AfterAll
	static void popNodeContext() {
		NodeContext.removeLastContext();
	}

	@Test
	void testManipulatorProviderContainsExpectedOperations() {
		final var manipulatorProvider = JsonPatchManipulator.createManipulatorProvider();
		final var manipulators = manipulatorProvider.getManipulators(JsonPatchManipulator.JSON_PATCH_CATEGORY);
		assertThat(manipulators).hasSize(6);
		assertThat(manipulators).extracting(Manipulator::getName)
			.containsExactly(JsonPatchManipulator.OP_ADD, JsonPatchManipulator.OP_REPLACE,
				JsonPatchManipulator.OP_REMOVE, JsonPatchManipulator.OP_COPY, JsonPatchManipulator.OP_MOVE,
				JsonPatchManipulator.OP_TEST);
	}

	@Test
	void testGetInitialDataCreatesBuilder() throws Exception {
	    var dialog = new JSONPatchApplyScriptingNodeDialog();
	    var initialDataBuilder = dialog.getInitialData(NodeContext.getContext());

	    var initialData = initialDataBuilder.toMap();
        System.out.println(initialData.keySet());

	}
}
