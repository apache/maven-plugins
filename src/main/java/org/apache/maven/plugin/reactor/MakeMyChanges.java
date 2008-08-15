package org.apache.maven.plugin.reactor;

/*
 * Copyright 2008 The Apache Software Foundation.
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
import org.apache.maven.scm.command.status.StatusScmResult;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.scm.repository.ScmRepository;
import org.codehaus.plexus.util.StringUtils;

/**
 * Goal to build all projects that you personally have changed (according to SCM) 
 *
 * @author <a href="mailto:dfabulich@apache.org">Dan Fabulich</a>
 * @goal makeMyChanges
 * @aggregator
 * @phase process-sources
 */
public class MakeMyChanges
    extends MakeDependentsMojo
{
    /**
     * @parameter expression="${make.scmUrl}" default-value="${project.scm.connection}"
     * @required
     */
    private String scmUrl;
    
    /**
     * @parameter expression="${component.org.apache.maven.scm.manager.ScmManager}"
     */
    private ScmManager scmManager;
    
    private MavenProject project;
    
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        if (collectedProjects.size() == 0) {
            throw new NonReactorException();
        }
        StatusScmResult result = null;
        try
        {
            ScmRepository repository = scmManager.makeScmRepository( scmUrl );
            result = scmManager.status( repository, new ScmFileSet( baseDir ) );
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException("Couldn't configure SCM repository: " + e.getLocalizedMessage(),e);
        }
        
        List changedFiles = result.getChangedFiles();
        // TODO There's a cleverer/faster way to code this...?
        List projectDirectories = getProjectDirectories();
        Set changedDirectories = new HashSet();
        for (int i = 0; i < changedFiles.size(); i++) {
            ScmFile changedScmFile = (ScmFile) changedFiles.get( i );
            File changedFile = new File(changedScmFile.getPath());
            for (int j = 0; j < projectDirectories.size(); j++) {
                File projectDirectory = (File) projectDirectories.get( j );
                if (changedFile.getAbsolutePath().startsWith( projectDirectory.getAbsolutePath() )) {
                    changedDirectories.add( RelativePather.getRelativePath( baseDir, projectDirectory ) );
                    break;
                }
            }
        }
        folderList = StringUtils.join( changedDirectories.iterator(), "," );
        getLog().info( "Going to make dependents for: " + folderList );
        super.execute();

    }
    
    private List getProjectDirectories() {
        List dirs = new ArrayList(collectedProjects.size());
        for (int i = 0; i < collectedProjects.size(); i++)
        {
            MavenProject mp = (MavenProject) collectedProjects.get( i );
            dirs.add( mp.getFile().getParentFile() );
        }
        return dirs;
    }

}
