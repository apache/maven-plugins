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

import java.io.File;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.dependency.analyzer.ProjectDependencyAnalysis;
import org.apache.maven.shared.dependency.analyzer.ProjectDependencyAnalyzer;
import org.apache.maven.shared.dependency.analyzer.ProjectDependencyAnalyzerException;
import org.codehaus.plexus.util.xml.PrettyPrintXMLWriter;

/**
 * Analyzes the dependencies of this project and determines which are: used and declared; used and undeclared; unused
 * and declared.
 *
 * @author <a href="mailto:markhobson@gmail.com">Mark Hobson</a>
 * @version $Id$
 * @since 2.0-alpha-5
 */
public abstract class AbstractAnalyzeMojo
    extends AbstractMojo
{
    // fields -----------------------------------------------------------------

    /**
     * The Maven project to analyze.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * The Maven project dependency analyzer to use.
     *
     * @component
     * @required
     * @readonly
     */
    private ProjectDependencyAnalyzer analyzer;

    /**
     * Whether to fail the build if a dependency warning is found.
     *
     * @parameter expression="${failOnWarning}" default-value="false"
     */
    private boolean failOnWarning;

    /**
     * Output used dependencies
     *
     * @parameter expression="${verbose}" default-value="false"
     */
    private boolean verbose;

    /**
     * Ignore Runtime,Provide,Test,System scopes for unused dependency analysis
     *
     * @parameter expression="${ignoreNonCompile}" default-value="false"
     */
    private boolean ignoreNonCompile;

    /**
     * Output the xml for the missing dependencies
     *
     * @parameter expression="${outputXML}" default-value="false"
     * @since 2.0-alpha-5
     */
    private boolean outputXML;

    /**
     * Output scriptable values
     *
     * @parameter expression="${scriptableOutput}" default-value="false"
     * @since 2.0-alpha-5
     */
    private boolean scriptableOutput;

    /**
     * Flag to use for scriptable output
     *
     * @parameter expression="${scriptableFlag}" default-value="$$$%%%"
     * @since 2.0-alpha-5
     */
    private String scriptableFlag;

    /**
     * Flag to use for scriptable output
     *
     * @parameter expression="${basedir}"
     * @readonly
     * @since 2.0-alpha-5
     */
    private File baseDir;

    /**
     * Target folder
     *
     * @parameter expression="${project.build.directory}"
     * @readonly
     * @since 2.0-alpha-5
     */
    private File outputDirectory;

    // Mojo methods -----------------------------------------------------------

    /*
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( "pom".equals( project.getPackaging() ) )
        {
            getLog().info( "Skipping pom project" );
            return;
        }

        if ( outputDirectory == null || !outputDirectory.exists() )
        {
            getLog().info( "Skipping project with no build directory" );
            return;
        }

        boolean warning = checkDependencies();

        if ( warning && failOnWarning )
        {
            throw new MojoExecutionException( "Dependency problems found" );
        }
    }

    // private methods --------------------------------------------------------

    private boolean checkDependencies()
        throws MojoExecutionException
    {
        ProjectDependencyAnalysis analysis;
        try
        {
            analysis = analyzer.analyze( project );
        }
        catch ( ProjectDependencyAnalyzerException exception )
        {
            throw new MojoExecutionException( "Cannot analyze dependencies", exception );
        }

        Set usedDeclared = analysis.getUsedDeclaredArtifacts();
        Set usedUndeclared = analysis.getUsedUndeclaredArtifacts();
        Set unusedDeclared = analysis.getUnusedDeclaredArtifacts();

        if ( ignoreNonCompile )
        {
            Set filteredUnusedDeclared = new HashSet( unusedDeclared );
            Iterator iter = filteredUnusedDeclared.iterator();
            while ( iter.hasNext() )
            {
                Artifact artifact = (Artifact) iter.next();
                if ( !artifact.getScope().equals( Artifact.SCOPE_COMPILE ) )
                {
                    iter.remove();
                }
            }
            unusedDeclared = filteredUnusedDeclared;
        }

        if ( ( !verbose || usedDeclared.isEmpty() ) && usedUndeclared.isEmpty() && unusedDeclared.isEmpty() )
        {
            getLog().info( "No dependency problems found" );
            return false;
        }

        if ( verbose && !usedDeclared.isEmpty() )
        {
            getLog().info( "Used declared dependencies found:" );

            logArtifacts( analysis.getUsedDeclaredArtifacts(), false );
        }

        if ( !usedUndeclared.isEmpty() )
        {
            getLog().warn( "Used undeclared dependencies found:" );

            logArtifacts( usedUndeclared, true );
        }

        if ( !unusedDeclared.isEmpty() )
        {
            getLog().warn( "Unused declared dependencies found:" );

            logArtifacts( unusedDeclared, true );
        }

        if ( outputXML )
        {
            writeDependencyXML( usedUndeclared );
        }

        if ( scriptableOutput )
        {
            writeScriptableOutput( usedUndeclared );
        }

        return !usedUndeclared.isEmpty() || !unusedDeclared.isEmpty();
    }

    private void logArtifacts( Set artifacts, boolean warn )
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

                // called because artifact will set the version to -SNAPSHOT only if I do this. MNG-2961
                artifact.isSnapshot();

                if ( warn )
                {
                    getLog().warn( "   " + artifact );
                }
                else
                {
                    getLog().info( "   " + artifact );
                }

            }
        }
    }

    private void writeDependencyXML( Set artifacts )
    {
        if ( !artifacts.isEmpty() )
        {
            getLog().info( "Add the following to your pom to correct the missing dependencies: " );

            StringWriter out = new StringWriter();
            PrettyPrintXMLWriter writer = new PrettyPrintXMLWriter( out );

            Iterator iter = artifacts.iterator();
            while ( iter.hasNext() )
            {
                Artifact artifact = (Artifact) iter.next();

                // called because artifact will set the version to -SNAPSHOT only if I do this. MNG-2961
                artifact.isSnapshot();

                writer.startElement( "dependency" );
                writer.startElement( "groupId" );
                writer.writeText( artifact.getGroupId() );
                writer.endElement();
                writer.startElement( "artifactId" );
                writer.writeText( artifact.getArtifactId() );
                writer.endElement();
                writer.startElement( "version" );
                writer.writeText( artifact.getBaseVersion() );
                if ( !StringUtils.isBlank( artifact.getClassifier() ) )
                {
                    writer.startElement( "classifier" );
                    writer.writeText( artifact.getClassifier() );
                    writer.endElement();
                }
                writer.endElement();

                if ( !Artifact.SCOPE_COMPILE.equals( artifact.getScope() ) )
                {
                    writer.startElement( "scope" );
                    writer.writeText( artifact.getScope() );
                    writer.endElement();
                }
                writer.endElement();
            }

            getLog().info( "\n" + out.getBuffer() );
        }
    }

    private void writeScriptableOutput( Set artifacts )
    {
        if ( !artifacts.isEmpty() )
        {
            getLog().info( "Missing dependencies: " );
            String pomFile = baseDir.getAbsolutePath() + File.separatorChar + "pom.xml";
            StringBuffer buf = new StringBuffer();
            Iterator iter = artifacts.iterator();
            while ( iter.hasNext() )
            {
                Artifact artifact = (Artifact) iter.next();

                // called because artifact will set the version to -SNAPSHOT only if I do this. MNG-2961
                artifact.isSnapshot();

                buf.append( scriptableFlag + ":" + pomFile + ":" + artifact.getDependencyConflictId() + ":"
                                + artifact.getClassifier() + ":" + artifact.getBaseVersion() + ":"
                                + artifact.getScope() + "\n" );
            }
            getLog().info( "\n" + buf );
        }
    }
}
