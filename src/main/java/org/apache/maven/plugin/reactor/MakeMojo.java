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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.invoker.Invoker;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.dag.DAG;
import org.codehaus.plexus.util.dag.Vertex;

/**
 * Goal to build a project X and all of the reactor projects on which X depends 
 *
 * @author <a href="mailto:dfabulich@apache.org">Dan Fabulich</a>
 * @goal make
 * @aggregator
 * @phase process-sources
 */
public class MakeMojo
    extends AbstractMojo
{
    /**
     * Location of the POM file; provided by Maven
     * @parameter expression="${basedir}"
     * 
     */
    File baseDir;
    
    /**
     * A list of every project in this reactor; provided by Maven
     * @parameter expression="${project.collectedProjects}"
     */
    List collectedProjects;
    
    /**
     * If you don't specify a groupId in your artifactList, we'll use this as the default groupId.
     * @parameter expression="${make.group}" default-value="${project.groupId}"
     * 
     */
    String defaultGroup;
    
    /**
     * A list of artifacts to build, e.g. "com.mycompany:bar,com.mycompany:foo" or just "foo,bar", or just "foo" 
     * @parameter expression="${make.artifacts}" default-value=""
     * @required
     */
    String artifactList;
    
    /**
     * A list of relative paths to build, e.g. "foo,baz/bar"
     * @parameter expression="${make.folders}" default-value=""
     * @required
     */
    String folderList;
    
    /**
     * Goals to run on subproject.
     * @parameter expression="${make.goals}" default-value="install"
     */
    String goals;
    
    /**
     * Provided by Maven
     * @component
     */
    Invoker invoker;
    
    /**
     * Don't really do anything; just print a command that describes what the command would have done
     * @parameter expression="${make.printOnly}"
     */
    private boolean printOnly = false;
    
    /**
     * @component
     */
    SimpleInvoker simpleInvoker;
    
    /**
     * The artifact from which we'll resume, e.g. "com.mycompany:foo" or just "foo"
     * @parameter expression="${fromArtifact}"
     */
    String continueFromProject;
    
    /**
     * The project folder from which we'll resume
     * @parameter expression="${from}"
     */
    File continueFromFolder;
    
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( artifactList == null && folderList == null ) {
            throw new MojoFailureException("You must specify either folders or projects with -Dmake.folders=foo,baz/bar or -Dmake.projects=com.mycompany:foo,com.mycompany:bar");
        }
        String[] reactorIncludes;
        List sortedProjects;
        try
        {
            if (collectedProjects.size() == 0) {
                throw new NonReactorException();
            }
            SuperProjectSorter ps = new SuperProjectSorter( collectedProjects );
            DAG dag = ps.getDAG();
            
            // gather projects
            collectArtifactListFromFolderList( collectedProjects );
            String[] artifacts = StringUtils.split( artifactList, "," );
            Set visited = new HashSet();
            Set out = new HashSet();
            for (int i = 0; i < artifacts.length; i++) {
                String project = artifacts[i];
                if ( project.indexOf(':') == -1 ) {
                    project = defaultGroup + ":" + project;
                }
                Vertex projectVertex = dag.getVertex( project );
                if ( projectVertex == null ) throw new MissingProjectException(project);
                gatherProjects( projectVertex, ps, visited, out );
            }
            
            // sort them again
            ps = new SuperProjectSorter( new ArrayList( out ) );
            sortedProjects = ps.getSortedProjects();
            
            // construct array of relative POM paths
            reactorIncludes = new String[sortedProjects.size()];
            for ( int i = 0; i < sortedProjects.size(); i++ )
            {
                MavenProject mp = (MavenProject) sortedProjects.get( i );
                String path = RelativePather.getRelativePath( baseDir, mp.getFile() );
                reactorIncludes[i] = path;
            }
        }
        catch (MojoFailureException e) {
            throw e;
        }        
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Problem generating dependency tree", e );
        }

        if (continueFromFolder != null || continueFromProject != null) {
            ResumeMojo resumer = new ResumeMojo();
            resumer.baseDir = baseDir;
            resumer.collectedProjects = sortedProjects;
            resumer.continueFromFolder = continueFromFolder;
            resumer.continueFromProject = continueFromProject;
            resumer.goals = goals;
            resumer.invoker = invoker;
            resumer.simpleInvoker = simpleInvoker;
            resumer.printOnly = printOnly;
            resumer.continueFromGroup = defaultGroup;
            resumer.execute();
        } else {
            simpleInvoker.runReactor( reactorIncludes, Arrays.asList( goals.split( "," ) ), invoker, printOnly, getLog() );
        }

    }

    void collectArtifactListFromFolderList(List collectedProjects) throws MojoFailureException
    {
        if ( folderList == null )
            return;
        String[] folders = StringUtils.split( folderList, "," );
        Set pathSet = new HashSet();
        for ( int i = 0; i < folders.length; i++ )
        {
            File file = new File( baseDir, folders[i] );
            if ( !file.exists() )
            {
                throw new MojoFailureException("Folder doesn't exist: " + file.getAbsolutePath() );
            }
            String path = file.getAbsolutePath();
            pathSet.add( path );
        }
        if (artifactList == null) artifactList = "";
        StringBuffer artifactBuffer = new StringBuffer(artifactList);
        for ( int i = 0; i < collectedProjects.size(); i++ )
        {
            MavenProject mp = (MavenProject) collectedProjects.get( i );
            if ( pathSet.contains( mp.getFile().getParentFile().getAbsolutePath() ) )
            {
                if ( artifactBuffer.length() > 0 )
                {
                    artifactBuffer.append( ',' );
                }
                String id = ArtifactUtils.versionlessKey( mp.getGroupId(), mp.getArtifactId() );
                artifactBuffer.append( id );
            }
        }
        if ( artifactBuffer.length() == 0 )
        {
            throw new MojoFailureException("No folders matched: " + folderList);
        }
        artifactList = artifactBuffer.toString();
    }

    protected Set gatherProjects( Vertex v, SuperProjectSorter ps, Set visited, Set out )
    {
        visited.add( v );
        out.add( ps.getProjectMap().get( v.getLabel() ) );
        List children = v.getChildren();
        for ( int i = 0; i < children.size(); i++ )
        {
            Vertex child = (Vertex) children.get( i );
            if ( visited.contains( child ) )
                continue;
            gatherProjects( child, ps, visited, out );
        }
        return out;
    }
}
