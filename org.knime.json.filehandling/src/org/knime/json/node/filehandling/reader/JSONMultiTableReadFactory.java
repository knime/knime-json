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
 *   18 May 2021 (modithahewasinghage): created
 */
package org.knime.json.node.filehandling.reader;

import java.io.IOException;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.filestore.FileStoreFactory;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.streamable.RowOutput;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.node.table.reader.MultiTableReadFactory;
import org.knime.filehandling.core.node.table.reader.PreviewIteratorException;
import org.knime.filehandling.core.node.table.reader.PreviewRowIterator;
import org.knime.filehandling.core.node.table.reader.SourceGroup;
import org.knime.filehandling.core.node.table.reader.config.MultiTableReadConfig;
import org.knime.filehandling.core.node.table.reader.config.tablespec.TableSpecConfig;
import org.knime.filehandling.core.node.table.reader.selector.RawSpec;
import org.knime.filehandling.core.node.table.reader.selector.TableTransformation;
import org.knime.filehandling.core.node.table.reader.util.MultiTableRead;
import org.knime.filehandling.core.node.table.reader.util.StagedMultiTableRead;

/**
 * Custom implementation of a {@link MultiTableReadFactory} with the purpose of being able to fail execution if no rows
 * were produced when reading in JSON files (which can happen e.g. if the JSON Path expression does not match).
 *
 * @author Moditha Hewasinghage, KNIME GmbH, Berlin, Germany
 *
 */
final class JSONMultiTableReadFactory implements MultiTableReadFactory<FSPath, JSONReaderConfig, DataType> {

    private final MultiTableReadFactory<FSPath, JSONReaderConfig, DataType> m_multiTableReadFactory;

    private static IllegalArgumentException createNoRowsException() {
        return new IllegalArgumentException("Nothing found for JSON Path ");
    }

    @Override
    public StagedMultiTableRead<FSPath, DataType> create(final SourceGroup<FSPath> sourceGroup,
        final MultiTableReadConfig<JSONReaderConfig, DataType> config, final ExecutionMonitor exec) throws IOException {
        final StagedMultiTableRead<FSPath, DataType> stagedultiTableReadFactory =
            m_multiTableReadFactory.create(sourceGroup, config, exec);
        return new JSONStagedMultiTableRead(stagedultiTableReadFactory,
            config.getTableReadConfig().getReaderSpecificConfig());
    }

    JSONMultiTableReadFactory(
        final MultiTableReadFactory<FSPath, JSONReaderConfig, DataType> multiTableReadFactory) {
        m_multiTableReadFactory = multiTableReadFactory;
    }

    @Override
    public StagedMultiTableRead<FSPath, DataType> createFromConfig(final SourceGroup<FSPath> sourceGroup,
        final MultiTableReadConfig<JSONReaderConfig, DataType> config) {
        final StagedMultiTableRead<FSPath, DataType> stagedultiTableReadFactory =
            m_multiTableReadFactory.createFromConfig(sourceGroup, config);
        return new JSONStagedMultiTableRead(stagedultiTableReadFactory,
            config.getTableReadConfig().getReaderSpecificConfig());
    }

    private static final class JSONStagedMultiTableRead implements StagedMultiTableRead<FSPath, DataType> {

        private final StagedMultiTableRead<FSPath, DataType> m_stagedMutltiTableRead;

        private final JSONReaderConfig m_config;

        JSONStagedMultiTableRead(final StagedMultiTableRead<FSPath, DataType> stagedMutltiTableRead,
            final JSONReaderConfig config) {
            m_stagedMutltiTableRead = stagedMutltiTableRead;
            m_config = config;
        }

        @Override
        public MultiTableRead<DataType> withoutTransformation(final SourceGroup<FSPath> sourceGroup) {
            final MultiTableRead<DataType> multiTableRead = m_stagedMutltiTableRead.withoutTransformation(sourceGroup);
            return new JSONMultiTableRead(multiTableRead, m_config);
        }

        @Override
        public MultiTableRead<DataType> withTransformation(final SourceGroup<FSPath> sourceGroup,
            final TableTransformation<DataType> selectorModel) {
            final MultiTableRead<DataType> multiTableRead =
                m_stagedMutltiTableRead.withTransformation(sourceGroup, selectorModel);
            return new JSONMultiTableRead(multiTableRead, m_config);
        }

        @Override
        public RawSpec<DataType> getRawSpec() {
            return m_stagedMutltiTableRead.getRawSpec();
        }

        @Override
        public boolean isValidFor(final SourceGroup<FSPath> sourceGroup) {
            return m_stagedMutltiTableRead.isValidFor(sourceGroup);
        }

    }

    private static final class JSONMultiTableRead implements MultiTableRead<DataType> {

        private final MultiTableRead<DataType> m_multiTableRead;

        private final JSONReaderConfig m_config;

        JSONMultiTableRead(final MultiTableRead<DataType> multiTableRead, final JSONReaderConfig config) {
            m_multiTableRead = multiTableRead;
            m_config = config;
        }

        @Override
        public DataTableSpec getOutputSpec() {
            return m_multiTableRead.getOutputSpec();
        }

        @Override
        public TableSpecConfig<DataType> getTableSpecConfig() {
            return m_multiTableRead.getTableSpecConfig();
        }

        @Override
        public PreviewRowIterator createPreviewIterator() {
            final PreviewRowIterator previewRowIterator = m_multiTableRead.createPreviewIterator();
            if (m_config.failIfNotFound()) {
                return new NonEmptyPreviewIterator(previewRowIterator);
            }
            return previewRowIterator;
        }

        @Override
        public void fillRowOutput(final RowOutput output, final ExecutionMonitor exec, final FileStoreFactory fsFactory)
            throws Exception {
            if (m_config.failIfNotFound()) {
                final CountingRowOutput countingRowOutput = new CountingRowOutput(output);
                m_multiTableRead.fillRowOutput(countingRowOutput, exec, fsFactory);
                if (countingRowOutput.getRowCount() == 0) {
                    throw createNoRowsException();
                }
            } else {
                m_multiTableRead.fillRowOutput(output, exec, fsFactory);
            }
        }

    }

    private static final class CountingRowOutput extends RowOutput {
        private final RowOutput m_rowOutput;

        private long m_rowCount = 0;

        /**
         *
         * @param rowOutput
         */
        public CountingRowOutput(final RowOutput rowOutput) {
            m_rowOutput = rowOutput;
        }

        @Override
        public void push(final DataRow row) throws InterruptedException {
            m_rowCount = getRowCount() + 1;
            m_rowOutput.push(row);
        }

        @Override
        public void close() throws InterruptedException {
            m_rowOutput.close();
        }

        /**
         * @return the rowCount
         */
        public long getRowCount() {
            return m_rowCount;
        }
    }

    private static final class NonEmptyPreviewIterator extends PreviewRowIterator {

        private final PreviewRowIterator m_previewRowIterator;

        private boolean m_firstCall = true;

        NonEmptyPreviewIterator(final PreviewRowIterator previewRowIterator) {
            this.m_previewRowIterator = previewRowIterator;
        }

        @Override
        public void close() {
            m_previewRowIterator.close();
        }

        @Override
        public boolean hasNext() {
            verifyNonEmpty();
            return m_previewRowIterator.hasNext();
        }

        private void verifyNonEmpty() {
            if (m_firstCall) {
                m_firstCall = false;
                if (!m_previewRowIterator.hasNext()) {
                    final IllegalArgumentException ex = createNoRowsException();
                    throw new PreviewIteratorException(ex.getMessage(), ex);
                }
            }
        }

        @Override
        public DataRow next() {
            verifyNonEmpty();
            return m_previewRowIterator.next();
        }

    }
}
