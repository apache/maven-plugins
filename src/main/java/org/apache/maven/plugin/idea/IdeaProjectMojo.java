package org.apache.maven.plugin.idea;

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

import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;
import org.dom4j.Element;
import org.dom4j.Document;
import org.dom4j.DocumentException;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * Creates the Project files (*.ipr) for IntelliJ IDEA.
 *
 * @author Edwin Punzalan
 * @goal project
 * @execute phase="generate-sources"
 */
public class IdeaProjectMojo
    extends AbstractIdeaMojo
{
    /**
     * Specify the name of the registered IDEA JDK to use
     * for the project.
     *
     * @parameter expression="${jdkName}"
     */
    private String jdkName;

    /**
     * Specify the version of the JDK to use for the project for the purpose of
     * enabled assertions and 5.0 language features.
     * The default value is the specification version of the executing JVM.
     *
     * @parameter expression="${jdkLevel}"
     * @todo would be good to use the compilation source if possible
     */
    private String jdkLevel;

    /**
     * Specify the resource pattern in wildcard format, for example "?*.xml;?*.properties".
     * Currently supports 4.x and 5.x.
     * The default value is any file without a java extension ("!?*.java").
     * Because IDEA doesn't distinguish between source and resources directories, this is needed.
     * Please note that the default value includes package.html files as it's not possible to exclude those.
     *
     * @parameter expression="${wildcardResourcePatterns}" default-value="!?*.java"
     */
    private String wildcardResourcePatterns;

    /**
     * Specify the version of idea to use.  This is needed to identify the default formatting of
     * project-jdk-name used by idea.  Currently supports 4.x and 5.x.
     * <p/>
     * This will only be used when parameter jdkName is not set.
     *
     * @parameter expression="${ideaVersion}"
     * default-value="5.x"
     */
    private String ideaVersion;

    private Set macros;

    public void initParam( MavenProject project, ArtifactFactory artifactFactory, ArtifactRepository localRepo,
                           ArtifactResolver artifactResolver, ArtifactMetadataSource artifactMetadataSource, Log log,
                           boolean overwrite, String jdkName, String jdkLevel, String wildcardResourcePatterns,
                           String ideaVersion, Set macros )
    {
        super.initParam( project, artifactFactory, localRepo, artifactResolver, artifactMetadataSource, log,
                         overwrite );

        this.jdkName = jdkName;

        this.jdkLevel = jdkLevel;

        this.wildcardResourcePatterns = wildcardResourcePatterns;

        this.ideaVersion = ideaVersion;

        this.macros = macros;
    }

    /**
     * Create IDEA (.ipr) project files.
     *
     * @throws org.apache.maven.plugin.MojoExecutionException
     *
     */
    public void execute()
        throws MojoExecutionException
    {
        try
        {
            doDependencyResolution( executedProject, localRepo );
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Unable to build project dependencies.", e );
        }

        rewriteProject();
    }

    public void rewriteProject()
        throws MojoExecutionException
    {
        File projectFile = new File( executedProject.getBasedir(), executedProject.getArtifactId() + ".ipr" );

        try
        {
            Document document = readXmlDocument( projectFile, "project.xml" );

            Element module = document.getRootElement();

            // Set the jdk name if set
            if ( jdkName != null )
            {
                setJdkName( module, jdkName );
            }
            else
            {
                String javaVersion = System.getProperty( "java.version" );
                String defaultJdkName;

                if ( ideaVersion.startsWith( "4" ) )
                {
                    defaultJdkName = "java version &quot;" + javaVersion + "&quot;";
                }
                else
                {
                    defaultJdkName = javaVersion.substring( 0, 3 );
                }
                getLog().info( "jdkName is not set, using [java version" + javaVersion + "] as default." );
                setJdkName( module, defaultJdkName );
            }

            setWildcardResourcePatterns( module, wildcardResourcePatterns );

            Element component = findComponent( module, "ProjectModuleManager" );
            Element modules = findElement( component, "modules" );

            removeOldElements( modules, "module" );

            if ( executedProject.getCollectedProjects().size() > 0 )
            {
                Element m = createElement( modules, "module" );
                String projectPath =
                    new File( executedProject.getBasedir(),
                              executedProject.getArtifactId() + ".iml" ).getAbsolutePath();
                m.addAttribute( "filepath",
                                "$PROJECT_DIR$/" + toRelative( executedProject.getBasedir(), projectPath ) );

                for ( Iterator i = executedProject.getCollectedProjects().iterator(); i.hasNext(); )
                {
                    MavenProject p = (MavenProject) i.next();

                    m = createElement( modules, "module" );
                    String modulePath = new File( p.getBasedir(), p.getArtifactId() + ".iml" ).getAbsolutePath();
                    m.addAttribute( "filepath",
                                    "$PROJECT_DIR$/" + toRelative( executedProject.getBasedir(), modulePath ) );
                }
            }
            else
            {
                Element m = createElement( modules, "module" );
                String modulePath =
                    new File( executedProject.getBasedir(),
                              executedProject.getArtifactId() + ".iml" ).getAbsolutePath();
                m.addAttribute( "filepath", "$PROJECT_DIR$/" + toRelative( executedProject.getBasedir(), modulePath ) );
            }

            // add any PathMacros we've come across
            if ( macros != null && module.elements( "UsedPathMacros" ).size() > 0 )
            {
                Element usedPathMacros = (Element) module.elements( "UsedPathMacros" ).get( 0 );
                removeOldElements( usedPathMacros, "macro" );
                for ( Iterator iterator = macros.iterator(); iterator.hasNext(); )
                {
                    String macro = (String) iterator.next();
                    Element macroElement = createElement( usedPathMacros, "macro" );
                    macroElement.addAttribute( "name", macro );
                }
            }

            writeXmlDocument( projectFile, document );
        }
        catch ( DocumentException e )
        {
            throw new MojoExecutionException( "Error parsing existing IPR file: " + projectFile.getAbsolutePath(), e );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error parsing existing IPR file: " + projectFile.getAbsolutePath(), e );
        }
    }

    /**
     * Sets the name of the JDK to use.
     *
     * @param content Xpp3Dom element.
     * @param jdkName Name of the JDK to use.
     */
    private void setJdkName( Element content, String jdkName )
    {
        Element component = findComponent( content, "ProjectRootManager" );
        component.addAttribute( "project-jdk-name", jdkName );

        String jdkLevel = this.jdkLevel;
        if ( jdkLevel == null )
        {
            jdkLevel = System.getProperty( "java.specification.version" );
        }

        if ( jdkLevel.startsWith( "1.4" ) )
        {
            component.addAttribute( "assert-keyword", "true" );
            component.addAttribute( "jdk-15", "false" );
        }
        else if ( jdkLevel.compareTo( "1.5" ) >= 0 )
        {
            component.addAttribute( "assert-keyword", "true" );
            component.addAttribute( "jdk-15", "true" );
        }
        else
        {
            component.addAttribute( "assert-keyword", "false" );
        }
    }

    /**
     * Sets the wilcard resource patterns.
     *
     * @param content                  Xpp3Dom element.
     * @param wildcardResourcePatterns The wilcard resource patterns.
     */
    private void setWildcardResourcePatterns( Element content, String wildcardResourcePatterns )
    {
        Element compilerConfigurationElement = findComponent( content, "CompilerConfiguration" );
        if ( !StringUtils.isEmpty( wildcardResourcePatterns ) )
        {
            removeOldElements( compilerConfigurationElement, "wildcardResourcePatterns" );
            Element wildcardResourcePatternsElement =
                createElement( compilerConfigurationElement, "wildcardResourcePatterns" );
            StringTokenizer wildcardResourcePatternsTokenizer = new StringTokenizer( wildcardResourcePatterns, ";" );
            while ( wildcardResourcePatternsTokenizer.hasMoreTokens() )
            {
                String wildcardResourcePattern = wildcardResourcePatternsTokenizer.nextToken();
                Element entryElement = createElement( wildcardResourcePatternsElement, "entry" );
                entryElement.addAttribute( "name", wildcardResourcePattern );
            }
        }
    }
}
