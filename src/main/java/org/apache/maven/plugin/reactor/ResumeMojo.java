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
import java.util.Arrays;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectSorter;
import org.apache.maven.shared.invoker.Invoker;

/**
 * Goal to resume building a reactor at a certain point 
 *
 * @author <a href="mailto:dfabulich@apache.org">Dan Fabulich</a>
 * @goal resume
 * @aggregator
 * @phase process-sources
 */
public class ResumeMojo
    extends AbstractMojo
{
    
    /**
     * @parameter expression="${project.collectedProjects}"
     */
    List collectedProjects;
    
    /**
     * Location of the file.
     * @parameter expression="${basedir}"
     */
    File baseDir;
    
    /**
     * @parameter expression="${make.group}" default-value="${project.groupId}"
     */
    String continueFromGroup;
    
    /**
     * The artifact from which we'll resume, e.g. "com.mycompany:foo" or just "foo"
     * @parameter expression="${fromArtifact}" default-value="null"
     * @required
     */
    String continueFromProject;
    
    /**
     * The project folder from which we'll resume
     * @parameter expression="${from}" default-value="null"
     * @required
     */
    File continueFromFolder;
    
    /**
     * Goals to run on subproject
     * @parameter expression="${make.goals}" default-value="install"
     */
    String goals;
    
    /**
     * @component
     */
    Invoker invoker;
    
    /**
     * Don't really do anything; just print a message that describes what the command would have done
     * @parameter expression="${make.printOnly}"
     */
    boolean printOnly = false;
    
    /**
     * @component
     */
    SimpleInvoker simpleInvoker;
    
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
    	if ( "null".equals( continueFromProject ) )
    	{
    	    continueFromProject = null;
    	}
    	if ( new File( "null" ).equals( continueFromFolder ) )
    	{
    	    continueFromFolder = null;
    	}
        if ( continueFromFolder == null && continueFromProject == null )
        {
            throw new MojoFailureException("You must specify either a folder or a project with -Dfrom=baz/bar or -DfromArtifact=com.mycompany:foo (groupId is optional)");
        }
        if (continueFromFolder != null && continueFromProject != null )
        {
            throw new MojoFailureException("You can't specify both a folder (" + continueFromFolder + ") and an artifact (" + continueFromProject + ")");
        }
        if ( continueFromFolder != null && !continueFromFolder.exists() )
        {
            throw new MojoFailureException("Folder doesn't exist: " + continueFromFolder.getAbsolutePath() );
        }
        String[] reactorIncludes;
        try
        {
            
            if (collectedProjects.size() == 0) {
                throw new NonReactorException();
            }
            ProjectSorter ps = new ProjectSorter( collectedProjects );
                        
            List sortedProjects = ps.getSortedProjects();
            
            String projectName = null;
            if ( continueFromProject != null)
            {
                projectName = continueFromProject;
                if ( projectName.indexOf(':') != -1 ) {
                    int index = continueFromProject.indexOf(':');
                    continueFromGroup = continueFromProject.substring( 0, index );
                    projectName = continueFromProject.substring( index+1 );
                }
            }
            
            boolean found = false;
            int i = 0;
            for (; i < sortedProjects.size(); i++) {
                MavenProject mp = (MavenProject) sortedProjects.get( i );
                if ( continueFromFolder == null )
                {
                    if ( continueFromGroup.equals( mp.getGroupId() ) && projectName.equals( mp.getArtifactId() ) )
                    {
                        found = true;
                        break;
                    }
                } else {
                    if ( continueFromFolder.equals( mp.getFile().getParentFile() ) )
                    {
                        found = true;
                        break;
                    }
                }
            }
            
            if (!found) throw new MissingProjectException(continueFromGroup + ":" + projectName);
            
            // construct array of relative POM paths
            reactorIncludes = new String[sortedProjects.size() - i];
            for ( int j = i; j < sortedProjects.size(); j++ )
            {
                MavenProject mp = (MavenProject) sortedProjects.get( j );
                String path = RelativePather.getRelativePath( baseDir, mp.getFile() );
                reactorIncludes[j- i] = path;
            }
        }
        catch (MojoFailureException e) {
            throw e;
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Problem generating dependency tree", e );
        }

        simpleInvoker.runReactor( reactorIncludes, Arrays.asList( goals.split( "," ) ), invoker, printOnly, getLog() );

    }
}
