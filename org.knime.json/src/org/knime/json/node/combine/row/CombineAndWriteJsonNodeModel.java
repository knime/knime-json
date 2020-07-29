package org.knime.json.node.combine.row;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.json.spi.JsonProvider;
import javax.json.stream.JsonGenerator;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.StringValue;
import org.knime.core.data.json.JSONValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.util.FileUtil;

/**
 * This is the model implementation of CombineAndWriteJson. Combines the values from a JSON column to a single JSON
 * file.
 *
 * @author Gabor Bakos
 */
class CombineAndWriteJsonNodeModel extends NodeModel {
    private final CombineAndWriteJsonSettings m_settings = new CombineAndWriteJsonSettings();

    /**
     * Constructor for the node model.
     */
    protected CombineAndWriteJsonNodeModel() {
        super(1, 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws Exception {
        final BufferedDataTable table = inData[0];
        final DataTableSpec spec = table.getSpec();
        final int idx = spec.findColumnIndex(m_settings.getInputColumn());
        final int objectKeyIndex = table.getSpec().findColumnIndex(m_settings.getObjectKeyColumn());
        final Set<String> processedKeys = new HashSet<>();
        int i = 0;
        double allRows = table.size();
        URL outputUrl = FileUtil.toURL(m_settings.getOutputFile());
        Path outputPath = FileUtil.resolveToPath(outputUrl);
        OutputStream stream;
        if (outputPath != null) {
            if (!m_settings.isOverwrite() && Files.exists(outputPath)) {
                throw new IOException("File '" + outputPath + "' already exists");
            }
            stream = new BufferedOutputStream(Files.newOutputStream(outputPath));
        } else {
            stream =
                new BufferedOutputStream(FileUtil.openOutputStream(outputUrl, "PUT"));
        }
        final String[] keys = m_settings.getKeys(), values = m_settings.getValues();
        try {
            try (final JsonGenerator generator =
                JsonProvider.provider()
                    .createGeneratorFactory(Collections.singletonMap(JsonGenerator.PRETTY_PRINTING, Boolean.TRUE))
                    .createGenerator(stream)) {
                switch (m_settings.getObjectOrArray()) {
                    case Array:
                        if (m_settings.isAddRootKey()) {
                            generator.writeStartObject();
                            generator.writeStartArray(m_settings.getRootKey());
                        } else {
                            generator.writeStartArray();
                        }
                        break;
                    case Object:
                        if (m_settings.isAddRootKey()) {
                            generator.writeStartObject();
                            generator.writeStartObject(m_settings.getRootKey());
                        } else {
                            generator.writeStartObject();
                        }
                        break;
                    default:
                        CheckUtils.checkState(false, "Not supported collection type: " + m_settings.getObjectOrArray());

                }
                for (final DataRow row : table) {
                    exec.checkCanceled();
                    exec.setProgress(i++ / allRows, "Processing row: " + row.getKey().getString());
                    String key = null;
                    switch (m_settings.getObjectOrArray()) {
                        case Array:
                            break;
                        case Object:
                            if (m_settings.isObjectKeyIsRowID()) {
                                key = row.getKey().getString();
                            } else {
                                CheckUtils.checkState(-1 != objectKeyIndex,
                                    "Not found column: " + m_settings.getObjectKeyColumn());
                                final DataCell cell2 = row.getCell(objectKeyIndex);
                                CheckUtils
                                    .checkState(!cell2.isMissing(), "Key cell is missing in row: " + row.getKey());
                                CheckUtils.checkState(cell2 instanceof StringValue,
                                    "The value for key is not a String in row: " + row.getKey());
                                final StringValue sv = (StringValue)cell2;
                                CheckUtils.checkState(processedKeys.add(sv.getStringValue()),
                                    "The value \"" + sv.getStringValue()
                                        + "\" for the key was already present in row: " + row.getKey());
                                key = sv.getStringValue();
                            }
                            break;
                        default:
                            CheckUtils.checkState(false,
                                "Not supported collection type: " + m_settings.getObjectOrArray());
                    }
                    final DataCell jsonCell = row.getCell(idx);
                    if (jsonCell instanceof JSONValue) {
                        JSONValue jv = (JSONValue)jsonCell;
                        switch (m_settings.getObjectOrArray()) {
                            case Array:
                                generator.write(jv.getJsonValue());
                                break;
                            case Object:
                                generator.write(key, jv.getJsonValue());
                                break;
                            default:
                                CheckUtils.checkState(false,
                                    "Not supported collection type: " + m_settings.getObjectOrArray());
                        }
                    } else {
                        switch (m_settings.getObjectOrArray()) {
                            case Array:
                                generator.writeNull();
                                break;
                            case Object:
                                generator.writeNull(key);
                                break;
                            default:
                                CheckUtils.checkState(false,
                                    "Not supported collection type: " + m_settings.getObjectOrArray());
                        }
                    }
                }
                //end array or object
                generator.writeEnd();
                if (m_settings.isAddRootKey()) {
                    for (int c = 0; c < keys.length; ++c) {
                        generator.write(keys[c], values[c]);
                    }
                    //end object
                    generator.writeEnd();
                }
            }
        } finally {
            stream.close();
        }
        return new BufferedDataTable[]{};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        //No internal state
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        StringBuilder warningMessage = new StringBuilder();
        appendWarningMessage(warningMessage,
            CheckUtils.checkDestinationFile(m_settings.getOutputFile(), m_settings.isOverwrite()));
        // validate settings for the JSON column
        appendWarningMessage(warningMessage, m_settings.autoConfigure(inSpecs[0]));
        if (warningMessage.length() > 0) {
            setWarningMessage(warningMessage.toString());
        }
        return new DataTableSpec[]{};
    }

    /**
     * Appends the {@code warning} to {@code warningMessage}.
     *
     * @param warningMessage The warning message collector.
     * @param warning The actual warning to add.
     */
    private void appendWarningMessage(final StringBuilder warningMessage, final String warning) {
        if (warningMessage.length() > 0 && !warning.isEmpty()) {
            warningMessage.append('\n');
        }
        warningMessage.append(warning);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_settings.loadSettingsModel(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        new CombineAndWriteJsonSettings().loadSettingsModel(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
        //No internal state
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
        //No internal state
    }
}
