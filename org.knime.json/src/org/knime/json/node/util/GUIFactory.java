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
 *   18 Oct. 2014 (Gabor): created
 */
package org.knime.json.node.util;

import javax.swing.Action;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.MenuElement;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.JTextComponent;

/**
 * Some utility methods to create UI components.
 *
 * @author Gabor Bakos
 */
public class GUIFactory {

    ///Hidden constructor
    private GUIFactory() {
    }

    /**
     * Constructs a new {@link JTextField} with cut/copy/paste context menu.
     * @param text The initial text, may be {@code null}.
     * @param numColumns The preferred width of the control in characters.
     * @return The new {@link JTextField}.
     */
    public static JTextField createTextField(final String text, final int numColumns) {
        JTextField ret = new JTextField(text, numColumns);
        addCopyCutPaste(ret);
        return ret;
    }

    /**
     * Adds the Copy/Cut/Paste actions to the {@code component}'s context menu.
     *
     * @param component A {@link JTextComponent}.
     */
    public static void addCopyCutPaste(final JTextComponent component) {
        //@see org.knime.base.node.rules.engine.Util
        JPopupMenu popup = component.getComponentPopupMenu();
        if (popup == null) {
            popup = new JPopupMenu();
            component.setComponentPopupMenu(popup);
        }
        boolean pasteFound = false, cutFound = false, copyFound = false;
        for (MenuElement menu : popup.getSubElements()) {
            if (menu.getComponent() instanceof JMenuItem) {
                JMenuItem item = (JMenuItem)menu.getComponent();
                final String name = item.getName();
                pasteFound |= "Paste".equals(name);
                cutFound |= "Cut".equals(name);
                copyFound |= "Copy".equals(name);
            }
        }
        //TODO should the action request focus first?
        if (!copyFound) {
            popup.add(createNamedMenuItem(new DefaultEditorKit.CopyAction()));
        }
        if (!cutFound) {
            popup.add(createNamedMenuItem(new DefaultEditorKit.CutAction()));
        }
        if (!pasteFound) {
            popup.add(createNamedMenuItem(new DefaultEditorKit.PasteAction()));
        }
    }
    /**
     * @param action An {@link Action}.
     * @return A new {@link JMenuItem} with the specified {@link Action}.
     */
    private static JMenuItem createNamedMenuItem(final Action action) {
        final JMenuItem menuItem = new JMenuItem(action);
        return menuItem;
    }
}
