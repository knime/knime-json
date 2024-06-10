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
 *   21 Sept 2014 (Gabor): created
 */
package org.knime.json.internal;

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

    private Configuration m_jsonPathConfiguration;

    private ClassLoader m_jsonSchemaValidatorClassLoader;

    /**
     * {@inheritDoc}
     */
    @Override
    public void start(final BundleContext ctx) throws Exception {
        INSTANCE = this;
        Bundle jsonBundle = null, schemaCoreBundle = null, schemaValidatorBundle = null;
        for (Bundle b : ctx.getBundles()) {
            if ("json-path".equals(b.getSymbolicName())) {
                jsonBundle = b;
                //                } else if ("com.github.fge.json-schema-validator".equals(b.getSymbolicName())) {
            } else if ("com.github.java-json-tools.json-schema-core".equals(b.getSymbolicName())) {
                schemaCoreBundle = b;
            } else if ("com.github.java-json-tools.json-schema-validator".equals(b.getSymbolicName())) {
                schemaValidatorBundle = b;
            }
        }
        if (jsonBundle == null) {
            throw new NullPointerException("JsonPath could not be loaded.");
        }
        if (schemaCoreBundle == null || schemaValidatorBundle == null) {
            throw new NullPointerException("JSON Schema validator could not be loaded.");
        }
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        m_jsonSchemaValidatorClassLoader = schemaValidatorBundle.adapt(BundleWiring.class).getClassLoader();

        //This initializes the SchemaVersion enum with the schema core bundle, as the schema validator bundle
        //does not contain a required schema
        try {
            ClassLoader scl = schemaCoreBundle.adapt(BundleWiring.class).getClassLoader();
            Thread.currentThread().setContextClassLoader(scl);
            Class.forName("com.github.fge.jsonschema.SchemaVersion", true, scl);
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
        }
        //Unfortunately this seems to have no effect on error messages. :(
//        try {
//            Thread.currentThread().setContextClassLoader(m_jsonSchemaValidatorClassLoader);
//            MessageBundles.getBundle((Class<? extends MessageBundleLoader>)Class.forName(
//                JsonSchemaValidationBundle.class.getName(), true, m_jsonSchemaValidatorClassLoader));
//            MessageBundles.getBundle((Class<? extends MessageBundleLoader>)Class.forName(
//                JsonSchemaCoreMessageBundle.class.getName(), true, m_jsonSchemaValidatorClassLoader));
//        } finally {
//            Thread.currentThread().setContextClassLoader(cl);
//        }
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
        //Do nothing
    }

    /**
     * @return the shared {@link Activator} instance.
     */
    public static Activator getInstance() {
        return INSTANCE;
    }

    /**
     * @return the jsonPathConfiguration
     */
    public Configuration getJsonPathConfiguration() {
        return m_jsonPathConfiguration;
    }

    /**
     * @return the jsonSchemaValidatorClassLoader
     */
    public ClassLoader getJsonSchemaValidatorClassLoader() {
        return m_jsonSchemaValidatorClassLoader;
    }
}
