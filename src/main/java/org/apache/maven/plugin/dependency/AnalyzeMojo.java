package org.apache.maven.plugin.dependency;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.Iterator;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.dependency.analyzer.ProjectDependencyAnalysis;
import org.apache.maven.shared.dependency.analyzer.ProjectDependencyAnalyzer;
import org.apache.maven.shared.dependency.analyzer.ProjectDependencyAnalyzerException;

/**
 * 
 * 
 * @author <a href="mailto:markhobson@gmail.com">Mark Hobson</a>
 * @version $Id$
 * @goal analyze
 * @requiresDependencyResolution test
 * @execute phase="test-compile"
 * @since 2.0-alpha-3
 */
public class AnalyzeMojo extends AbstractMojo
{
    // fields -----------------------------------------------------------------

    /**
     * 
     * 
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * 
     * 
     * @parameter expression="${component.org.apache.maven.shared.dependency.analyzer.ProjectDependencyAnalyzer}"
     * @required
     * @readonly
     */
    private ProjectDependencyAnalyzer analyzer;

    // Mojo methods -----------------------------------------------------------

    /*
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        try
        {
            ProjectDependencyAnalysis analysis = analyzer.analyze( project );

            getLog().info( "Used declared dependencies:" );

            logArtifacts( analysis.getUsedDeclaredArtifacts() );

            getLog().info( "Used undeclared dependencies:" );

            logArtifacts( analysis.getUsedUndeclaredArtifacts() );

            getLog().info( "Unused declared dependencies:" );

            logArtifacts( analysis.getUnusedDeclaredArtifacts() );
        }
        catch ( ProjectDependencyAnalyzerException exception )
        {
            throw new MojoExecutionException( "Cannot analyze dependencies", exception );
        }
    }

    // private methods --------------------------------------------------------

    private void logArtifacts( Set artifacts )
    {
        if ( artifacts.isEmpty() )
        {
            getLog().info( "   None" );
        }
        else
        {
            for ( Iterator iterator = artifacts.iterator(); iterator.hasNext(); )
            {
                Artifact artifact = (Artifact) iterator.next();

                getLog().info( "   " + artifact );
            }
        }
    }
}
