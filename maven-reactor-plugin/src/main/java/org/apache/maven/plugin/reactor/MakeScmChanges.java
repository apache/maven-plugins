package org.apache.maven.plugin.reactor;

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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.scm.ScmFile;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.ScmFileStatus;
import org.apache.maven.scm.command.status.StatusScmResult;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.scm.repository.ScmRepository;
import org.codehaus.plexus.util.StringUtils;

/**
 * Goal to build all projects that you personally have changed (according to SCM)
 * 
 * @author <a href="mailto:dfabulich@apache.org">Dan Fabulich</a>
 * @goal make-scm-changes
 * @aggregator
 * @phase process-sources
 */
public class MakeScmChanges
    extends MakeDependentsMojo
{
    /**
     * The SCM connection/provider info.  Should be specified in your POM.
     * @parameter expression="${make.scmConnection}" default-value="${project.scm.connection}"
     * @required
     */
    String scmConnection;

    /**
     * Ignore files in the "unknown" status (created but not added to source control)
     * 
     * @parameter expression="${make.ignoreUnknown}" default-value=true
     */
    private boolean ignoreUnknown = true;

    /**
     * @component
     */
    ScmManager scmManager;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( collectedProjects.size() == 0 )
        {
            throw new NonReactorException();
        }
        if ( scmConnection == null )
        {
            throw new MojoFailureException("No SCM connection specified.  You must specify an SCM connection by adding a <connection> element to your <scm> element in your POM");
        }
        StatusScmResult result = null;
        try
        {
            ScmRepository repository = scmManager.makeScmRepository( scmConnection );
            result = scmManager.status( repository, new ScmFileSet( baseDir ) );
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Couldn't configure SCM repository: " + e.getLocalizedMessage(), e );
        }

        List changedFiles = result.getChangedFiles();
        
        List projectDirectories = getProjectDirectories();
        Set changedDirectories = new HashSet();
        for ( int i = 0; i < changedFiles.size(); i++ )
        {
            ScmFile changedScmFile = (ScmFile) changedFiles.get( i );
            getLog().debug( changedScmFile.toString() );
            ScmFileStatus status = changedScmFile.getStatus();
            if ( !status.isStatus() )
            {
                getLog().debug( "Not a diff: " + status );
                continue;
            }
            if ( ignoreUnknown && ScmFileStatus.UNKNOWN.equals( status ) )
            {
                getLog().debug( "Ignoring unknown" );
                continue;
            }

            File changedFile = new File( changedScmFile.getPath() );
            boolean found = false;
            // TODO There's a cleverer/faster way to code this, right?  This is O(n^2)
            for ( int j = 0; j < projectDirectories.size(); j++ )
            {
                File projectDirectory = (File) projectDirectories.get( j );
                if ( changedFile.getAbsolutePath().startsWith( projectDirectory.getAbsolutePath() + File.separator ) )
                {
                    String path = RelativePather.getRelativePath( baseDir, projectDirectory );
                    if ( !changedDirectories.contains( path ) )
                    {
                        getLog().debug( "Including " + path );
                    }
                    changedDirectories.add( path );
                    found = true;
                    break;
                }
            }
            if ( !found )
            {
                getLog().debug( "Couldn't find file in any reactor root: " + changedFile.getAbsolutePath() );
            }
        }
        folderList = StringUtils.join( changedDirectories.iterator(), "," );
        getLog().info( "Going to make dependents for: " + folderList );
        super.execute();

    }

    private List getProjectDirectories()
    {
        List dirs = new ArrayList( collectedProjects.size() );
        for ( int i = 0; i < collectedProjects.size(); i++ )
        {
            MavenProject mp = (MavenProject) collectedProjects.get( i );
            dirs.add( mp.getFile().getParentFile() );
        }
        return dirs;
    }

}
