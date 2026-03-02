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
 *   19 May 2016 (Gabor Bakos): created
 */
package org.knime.json.node.patch.apply;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.knime.base.node.preproc.stringmanipulation.manipulator.Manipulator;
import org.knime.base.node.util.ManipulatorProvider;

/**
 * Common interface for the JSON Patch {@link Manipulator}s.
 *
 * @author Gabor Bakos
 */
@FunctionalInterface
interface JsonPatchManipulator extends Manipulator {
    /**
     * The JSON Patch category name.
     */
    public static final String JSON_PATCH_CATEGORY = "JSON Patch";

    public static final String OP_ADD = "add";

    public static final String OP_REPLACE = "replace";

    public static final String OP_REMOVE = "remove";

    public static final String OP_COPY = "copy";

    public static final String OP_MOVE = "move";

    public static final String OP_TEST = "test";

    static Collection<? extends Manipulator> createManipulators() {
        return List.of(new ValueManipulator(OP_ADD), new ValueManipulator(OP_REPLACE), new RemoveManipulator(),
            new FromManipulator(OP_COPY), new FromManipulator(OP_MOVE), new ValueManipulator(OP_TEST));
    }

    static ManipulatorProvider createManipulatorProvider() {
        return new ManipulatorProvider() {

            @Override
            public Collection<? extends Manipulator> getManipulators(final String category) {
                return createManipulators();
            }

            @Override
            public Collection<String> getCategories() {
                return Collections.singleton(JSON_PATCH_CATEGORY);
            }
        };
    }

    static class ValueManipulator implements JsonPatchManipulator {
        private String m_name;


        ValueManipulator(final String name) {
            m_name = name;
        }


        @Override
        public String getName() {
            return m_name;
        }
    }
    static class FromManipulator implements JsonPatchManipulator {
        private String m_name;

        FromManipulator(final String name) {
            m_name = name;
        }


        @Override
        public String getName() {
            return m_name;
        }


        @Override
        public String getDisplayName() {
            return "{ \"op\": \"" +getName()+ "\", \"from\": \"\" , \"path\": \"\"}";
        }

        @Override
        public String getDescription() {
            return getName() + "s from (a JSON Pointer expression) to the path (which is also a JSON Pointer expression)".replaceAll("ys", "ies");
        }
    }

    static class RemoveManipulator implements JsonPatchManipulator {

        @Override
        public String getName() {
            return OP_REMOVE;
        }

        @Override
        public int getNrArgs() {
            return 1;
        }


        @Override
        public String getDisplayName() {
            return "{ \"op\": \"" +getName()+ "\", \"path\": \"\" }";
        }
    }

    @Override
    default String getCategory() {
        return JSON_PATCH_CATEGORY;
    }


    @Override
    default int getNrArgs() {
        return 2;
    }


    @Override
    default Class<?> getReturnType() {
        return String.class;
    }

    @Override
    default String getDisplayName() {
        return "{ \"op\": \"" +getName()+ "\", \"path\": \"\", \"value\": \"\" }";
    }

    @Override
    default String getDescription() {
        return getName() + "s the path (which is a JSON Pointer expression) with value";
    }
}
