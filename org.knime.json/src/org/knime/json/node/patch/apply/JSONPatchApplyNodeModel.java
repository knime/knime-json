package org.knime.json.node.patch.apply;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.data.MissingCell;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.json.JSONCellFactory;
import org.knime.core.data.json.JSONValue;
import org.knime.core.data.json.JacksonConversions;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.json.internal.Activator;
import org.knime.json.node.util.SingleColumnReplaceOrAddNodeModel;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import com.github.fge.jsonpatch.mergepatch.JsonMergePatch;

/**
 * This is the model implementation of JSONTransformer. Changes {@link JSONValue}s.
 *
 * @author Gabor Bakos
 */
public final class JSONPatchApplyNodeModel extends SingleColumnReplaceOrAddNodeModel<JSONPatchApplySettings> {
    /**
     * Constructor for the node model.
     */
    protected JSONPatchApplyNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     *
     * @throws IOException
     */
    @Override
    protected CellFactory createCellFactory(final DataColumnSpec output, final int inputIndex,
        final int... otherColumns) throws IOException {
        final JacksonConversions conv = Activator.getInstance().getJacksonConversions();
        JsonNode patchNode =
            conv.toJackson(((JSONValue)JSONCellFactory.create(getSettings().getJsonPatch(), true)).getJsonValue());
        final JsonPatch patch;
        final JsonMergePatch mergePatch;
        switch (getSettings().getPatchType()) {
            case JSONPatchApplySettings.PATCH_OPTION:
                patch = JsonPatch.fromJson(patchNode);
                mergePatch = null;
                break;
            case JSONPatchApplySettings.MERGE_PATCH_OPTION:
                try {
                    mergePatch = JsonMergePatch.fromJson(patchNode);
                    patch = null;
                } catch (JsonPatchException e) {
                    throw new IOException("Wrong merge patch format: " + e.getMessage(), e);
                }
                break;
            default:
                throw new IllegalStateException("Not supported patch type: " + getSettings().getPatchType());
        }
        return new SingleCellFactory(output) {
            @Override
            public DataCell getCell(final DataRow row) {
                DataCell cell = row.getCell(inputIndex);
                if (cell instanceof JSONValue) {
                    JSONValue jsonCell = (JSONValue)cell;
                    JsonNode jsonNode = conv.toJackson(jsonCell.getJsonValue());
                    try {
                        JsonNode applied;
                        if (patch == null) {
                            if (mergePatch != null) {
                                applied = mergePatch.apply(jsonNode);
                            } else {
                                throw new IllegalStateException();
                            }
                        } else {
                            applied = patch.apply(jsonNode);
                        }
                        return JSONCellFactory.create(conv.toJSR353(applied));
                    } catch (JsonPatchException e) {
                        return new MissingCell(e.getMessage());
                    }
                }
                return DataType.getMissingCell();
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // No internal state to reset.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        super.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.loadValidatedSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
        //No internal state.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
        //No internal state.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected JSONPatchApplySettings createSettings() {
        return createJSONPatchApplySetting();
    }

    /**
     * @return
     */
    static JSONPatchApplySettings createJSONPatchApplySetting() {
        return new JSONPatchApplySettings();
    }
}
