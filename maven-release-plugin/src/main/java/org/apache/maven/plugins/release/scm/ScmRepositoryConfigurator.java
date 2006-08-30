package org.apache.maven.plugins.release.scm;

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

import org.apache.maven.plugins.release.config.ReleaseDescriptor;
import org.apache.maven.scm.manager.NoSuchScmProviderException;
import org.apache.maven.scm.provider.ScmProvider;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.scm.repository.ScmRepositoryException;
import org.apache.maven.settings.Settings;

/**
 * Configure an SCM repository using release configuration.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public interface ScmRepositoryConfigurator
{
    /**
     * The Plexus role.
     */
    String ROLE = ScmRepositoryConfigurator.class.getName();

    /**
     * Construct a configured SCM repository from a release configuration.
     *
     * @param releaseDescriptor the configuration to insert into the repository
     * @param settings          the settings.xml configuraiton
     * @return the repository created
     * @throws ScmRepositoryException     if it is not possible to create a suitable SCM repository
     * @throws NoSuchScmProviderException if the requested SCM provider is not available
     */
    ScmRepository getConfiguredRepository( ReleaseDescriptor releaseDescriptor, Settings settings )
        throws ScmRepositoryException, NoSuchScmProviderException;

    /**
     * Get the SCM provider used for the given SCM repository.
     *
     * @param repository the SCM repository
     * @return the SCM provider
     * @throws NoSuchScmProviderException if the requested SCM provider is not available
     */
    ScmProvider getRepositoryProvider( ScmRepository repository )
        throws NoSuchScmProviderException;
}
