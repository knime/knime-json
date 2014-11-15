/**
 * Common utility classes for nodes. For example a {@link org.knime.core.node.NodeModel} that
 * replaces or appends a new column: {@link org.knime.json.node.util.SingleColumnReplaceOrAddNodeModel}.
 * (It extends {@link org.knime.core.node.streamable.simple.SimpleStreamableFunctionNodeModel}.)
 * It has two basic settings: <ul><li>{@link org.knime.json.node.util.RemoveOrAddColumnSettings} and
 * <li>{@link org.knime.json.node.util.ReplaceColumnSettings}</li></ul> and two basic dialogs:
 * <ul><li>{@link org.knime.json.node.util.RemoveOrAddColumnDialog} and</li>
 * <li>{@link org.knime.json.node.util.ReplaceColumnDialog}</li></ul>.
 * The {@link org.knime.json.node.util.GUIFactory} can be used to create consistent text fields.
 */
package org.knime.json.node.util;