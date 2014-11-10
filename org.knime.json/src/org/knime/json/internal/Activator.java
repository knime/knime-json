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
 *   21 Sept 2014 (Gabor): created
 */
package org.knime.json.internal;

import org.knime.core.data.json.JSONCellWriterFactory;
import org.knime.core.data.json.JacksonConversions;
import org.knime.core.data.json.internal.JacksonConversionsImpl;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.BundleWiring;

import com.jayway.jsonpath.Configuration;

/**
 * Activator for the bundle.
 *
 * @author Gabor Bakos
 */
public class Activator implements BundleActivator {
    private static Activator INSTANCE;

    private JacksonConversions m_jacksonConversions;

    private Configuration m_jsonPathConfiguration;

    private ClassLoader m_jsonSchemaCoreClassLoader;

    private ClassLoader m_jsr353ClassLoader;

    private JSONCellWriterFactory m_jsonCellWriterFactory;

    /**
     * {@inheritDoc}
     */
    @Override
    public void start(final BundleContext ctx) throws Exception {
        INSTANCE = this;
        Bundle jsonBundle = null, schemaCoreBundle = null, jsr353Bundle = null;
        for (Bundle b : ctx.getBundles()) {
            if ("com.jayway.jsonpath.json-path".equals(b.getSymbolicName())) {
                jsonBundle = b;
                //                } else if ("com.github.fge.json-schema-validator".equals(b.getSymbolicName())) {
            } else if ("com.github.fge.json-schema-core".equals(b.getSymbolicName())) {
                schemaCoreBundle = b;
            } else if ("org.glassfish.javax.json".equals(b.getSymbolicName())) {
                jsr353Bundle = b;
            }
        }
        if (jsonBundle == null) {
            throw new NullPointerException("JsonPath could not be loaded.");
        }
        if (schemaCoreBundle == null) {
            throw new NullPointerException("JSON Schema validator could not be loaded.");
        }
        if (jsr353Bundle == null) {
            throw new NullPointerException("JSR-353 implementation could not be loaded.");
        }
        m_jsr353ClassLoader = jsr353Bundle.adapt(BundleWiring.class).getClassLoader();
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
            m_jacksonConversions = new JacksonConversionsImpl();
            m_jsonCellWriterFactory = new org.knime.core.data.json.io.JSONCellWriterFactory();
        m_jsonSchemaCoreClassLoader = schemaCoreBundle.adapt(BundleWiring.class).getClassLoader();
        try {
            ClassLoader classLoader = jsonBundle.adapt(BundleWiring.class).getClassLoader();
            Thread.currentThread().setContextClassLoader(classLoader);
            //When JsonPath will contain proper SPI services
//            JsonProvider jsonProvider = ServiceLoader.load(JsonProvider.class, classLoader).iterator().next();
            m_jsonPathConfiguration = Configuration.defaultConfiguration();
            //m_jsonPathConfiguration = m_jsonPathConfiguration.jsonProvider(jsonProvider);
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
        }
        //When JsonPath contain OSGi services
//        ServiceReference<JsonProvider> ref = ctx.getServiceReference(JsonProvider.class);
//        if (ref != null) {
//            JsonProvider service = ctx.getService(ref);
//        } else {
//            System.out.println("Json-path osgi service failed.");
//        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop(final BundleContext ctx) throws Exception {
        // TODO Auto-generated method stub

    }

    /**
     * @return the shared {@link Activator} instance.
     */
    public static Activator getInstance() {
        return INSTANCE;
    }

    /**
     * @return the jacksonConversions
     */
    public JacksonConversions getJacksonConversions() {
        return m_jacksonConversions;
    }

    /**
     * @return the jsonPathConfiguration
     */
    public Configuration getJsonPathConfiguration() {
        return m_jsonPathConfiguration;
    }

    /**
     * @return the jsonSchemaClassLoader
     */
    public ClassLoader getJsonSchemaCoreClassLoader() {
        return m_jsonSchemaCoreClassLoader;
    }

    /**
     * @return the jsr353ClassLoader
     */
    public ClassLoader getJsr353ClassLoader() {
        return m_jsr353ClassLoader;
    }

    /**
     * @return the jsonCellWriterFactory
     */
    public JSONCellWriterFactory getJsonCellWriterFactory() {
        return m_jsonCellWriterFactory;
    }
}
