package org.apache.maven.plugins.release.config;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Storage for reading and writing release configuration.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public interface ReleaseConfigurationStore
{
    /**
     * The Plexus role.
     */
    String ROLE = ReleaseConfigurationStore.class.getName();

    /**
     * Read a configuration.
     *
     * @param mergeConfiguration configuration to merge with the loaded configuration. Some values are used as defaults,
     *                           while others are used to override
     * @return the configuration
     */
    ReleaseConfiguration read( ReleaseConfiguration mergeConfiguration )
        throws ReleaseConfigurationStoreException;

    /**
     * Read a configuration.
     *
     * @return the configuration
     */
    ReleaseConfiguration read()
        throws ReleaseConfigurationStoreException;

    /**
     * Save a configuration.
     *
     * @param config the configuration
     */
    void write( ReleaseConfiguration config )
        throws ReleaseConfigurationStoreException;
}
