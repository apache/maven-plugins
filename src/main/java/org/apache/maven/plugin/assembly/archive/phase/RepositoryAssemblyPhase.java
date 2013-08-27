package org.apache.maven.plugin.assembly.archive.phase;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.AssemblyContext;
import org.apache.maven.plugin.assembly.InvalidAssemblerConfigurationException;
import org.apache.maven.plugin.assembly.archive.ArchiveCreationException;
import org.apache.maven.plugin.assembly.archive.phase.wrappers.RepoBuilderConfigSourceWrapper;
import org.apache.maven.plugin.assembly.archive.phase.wrappers.RepoInfoWrapper;
import org.apache.maven.plugin.assembly.archive.task.AddDirectoryTask;
import org.apache.maven.plugin.assembly.format.AssemblyFormattingException;
import org.apache.maven.plugin.assembly.model.Assembly;
import org.apache.maven.plugin.assembly.model.Repository;
import org.apache.maven.plugin.assembly.utils.AssemblyFormatUtils;
import org.apache.maven.plugin.assembly.utils.TypeConversionUtils;
import org.apache.maven.shared.repository.RepositoryAssembler;
import org.apache.maven.shared.repository.RepositoryAssemblyException;
import org.apache.maven.shared.repository.RepositoryBuilderConfigSource;
import org.apache.maven.shared.repository.model.RepositoryInfo;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.AbstractLogEnabled;

/**
 * @version $Id$
 */
@Component( role = AssemblyArchiverPhase.class, hint = "repositories" )
public class RepositoryAssemblyPhase
    extends AbstractLogEnabled
    implements AssemblyArchiverPhase
{

    @Requirement
    private RepositoryAssembler repositoryAssembler;

    public RepositoryAssemblyPhase()
    {
        // used for plexus.
    }

    // introduced for testing.
    public RepositoryAssemblyPhase( final RepositoryAssembler repositoryAssembler )
    {
        this.repositoryAssembler = repositoryAssembler;
    }

    /**
     * {@inheritDoc}
     */
    public void execute( final Assembly assembly, final Archiver archiver,
                         final AssemblerConfigurationSource configSource, final AssemblyContext context )
        throws ArchiveCreationException, AssemblyFormattingException, InvalidAssemblerConfigurationException
    {
        final List<Repository> repositoriesList = assembly.getRepositories();

        final File tempRoot = configSource.getTemporaryRootDirectory();

        for (final Repository repository : repositoriesList) {
            final String outputDirectory =
                    AssemblyFormatUtils.getOutputDirectory(repository.getOutputDirectory(), configSource.getProject(),
                            null, configSource.getFinalName(), configSource);

            final File repositoryDirectory = new File(tempRoot, outputDirectory);

            if (!repositoryDirectory.exists()) {
                repositoryDirectory.mkdirs();
            }

            try {
                getLogger().debug("Assembling repository to: " + repositoryDirectory);
                repositoryAssembler.buildRemoteRepository(repositoryDirectory, wrap(repository), wrap(configSource));
                getLogger().debug("Finished assembling repository to: " + repositoryDirectory);
            } catch (final RepositoryAssemblyException e) {
                throw new ArchiveCreationException("Failed to assemble repository: " + e.getMessage(), e);
            }

            final AddDirectoryTask task = new AddDirectoryTask(repositoryDirectory);

            final int dirMode = TypeConversionUtils.modeToInt(repository.getDirectoryMode(), getLogger());
            if (dirMode != -1) {
                task.setDirectoryMode(dirMode);
            }

            final int fileMode = TypeConversionUtils.modeToInt(repository.getFileMode(), getLogger());
            if (fileMode != -1) {
                task.setFileMode(fileMode);
            }

            task.setOutputDirectory(outputDirectory);

            task.execute(archiver, configSource);
        }
    }

    private RepositoryBuilderConfigSource wrap( final AssemblerConfigurationSource configSource )
    {
        return new RepoBuilderConfigSourceWrapper( configSource );
    }

    private RepositoryInfo wrap( final Repository repository )
    {
        return new RepoInfoWrapper( repository );
    }

}
