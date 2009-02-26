package org.apache.maven.plugin.ejb;

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

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.jar.ManifestException;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Build an EJB (and optional client) from the current project.
 *
 * @author <a href="evenisse@apache.org">Emmanuel Venisse</a>
 * @version $Id$
 * @goal ejb
 * @phase package
 */
public class EjbMojo
    extends AbstractMojo
{
    private static final String EJB_JAR_XML = "META-INF/ejb-jar.xml";

    // TODO: will null work instead?
    private static final String[] DEFAULT_INCLUDES = new String[]{"**/**"};

    private static final String[] DEFAULT_EXCLUDES = new String[]{EJB_JAR_XML, "**/package.html"};

    private static final String[] DEFAULT_CLIENT_EXCLUDES =
        new String[]{"**/*Bean.class", "**/*CMP.class", "**/*Session.class", "**/package.html"};

    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    /**
     * The directory for the generated EJB.
     *
     * @parameter expression="${project.build.directory}"
     * @required
     * @readonly
     * @todo use File instead
     */
    private String basedir;

    /**
     * Directory that resources are copied to during the build.
     *
     * @parameter default-value="${project.build.outputDirectory}" expression="${outputDirectory}"
     * @required
     */
    private String outputDirectory;

    /**
     * The name of the EJB file to generate.
     *
     * @parameter default-value="${project.build.finalName}" expression="${jarName}"
     * @required
     */
    private String jarName;

    /**
     * Classifier to add to the artifact generated. If given, the artifact will
     * be an attachment instead.
     *
     * @parameter expression="${ejb.classifier}"
     */
    private String classifier;

    /**
     * Whether the EJB client jar should be generated or not.
     *
     * @parameter default-value="false" expression="${ejb.generateClient}"
     */
    private boolean generateClient;

    /**
     * The files and directories to exclude from the client jar. Usage:
     *
     * <pre>
     * &lt;clientExcludes&gt;
     * &nbsp;&nbsp;&lt;clientExclude&gt;**&#47;*Ejb.class&lt;&#47;clientExclude&gt;
     * &nbsp;&nbsp;&lt;clientExclude&gt;**&#47;*Bean.class&lt;&#47;clientExclude&gt;
     * &lt;&#47;clientExcludes&gt;
     * </pre>
     * <br/>Attribute is used only if client jar is generated.
     * <br/>Default exclusions: **&#47;*Bean.class, **&#47;*CMP.class, **&#47;*Session.class, **&#47;package.html
     *
     * @parameter
     */
    private List clientExcludes;

    /**
     * The files and directories to include in the client jar. Usage:
     *
     * <pre>
     * &lt;clientIncludes&gt;
     * &nbsp;&nbsp;&lt;clientInclude&gt;**&#47;*&lt;&#47;clientInclude&gt;
     * &lt;&#47;clientIncludes&gt;
     * </pre>
     * <br/>Attribute is used only if client jar is generated.
     * <br/>Default value: **&#47;**
     *
     * @parameter
     */
    private List clientIncludes;

    /**
     * The files and directories to exclude from the main EJB jar. Usage:
     *
     * <pre>
     * &lt;excludes&gt;
     *   &lt;exclude&gt;**&#47;*Ejb.class&lt;&#47;exclude&gt;
     *   &lt;exclude&gt;**&#47;*Bean.class&lt;&#47;exclude&gt;
     * &lt;&#47;excludes&gt;
     * </pre>
     * <br/>Default exclusions: META-INF&#47;ejb-jar.xml, **&#47;package.html
     * @parameter
     */
    private List excludes;

    /**
     * The Maven project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * The Jar archiver.
     *
     * @component role="org.codehaus.plexus.archiver.Archiver" roleHint="jar"
     */
    private JarArchiver jarArchiver;

    /**
     * What EJB version should the EJB Plugin generate? Valid values are "2.x" or "3.x"
     * (where x is a digit).  When ejbVersion is "3.x", the
     * <code>ejb-jar.xml</code> file is optional.
     * <p/>
     * Usage:
     * <pre>
     * &lt;ejbVersion&gt;3.0&lt;&#47;ejbVersion&gt;
     * </pre>
     *
     * @parameter default-value="2.1" expression="${ejb.ejbVersion}"
     * @required
     * @since 2.1
     */
    private String ejbVersion;

    /**
     * The client Jar archiver.
     *
     * @component role="org.codehaus.plexus.archiver.Archiver" roleHint="jar"
     */
    private JarArchiver clientJarArchiver;

    /**
     * The Maven project's helper.
     *
     * @component
     */
    private MavenProjectHelper projectHelper;

    /**
     * The archive configuration to use.
     * See <a href="http://maven.apache.org/shared/maven-archiver/index.html">Maven Archiver Reference</a>.
     * This version of the EJB Plugin uses Maven Archiver 2.4.
     *
     * @parameter
     */
    private MavenArchiveConfiguration archive = new MavenArchiveConfiguration();

    /**
     * Generates an EJB jar and optionally an ejb-client jar.
     *
     * @todo Add license files in META-INF directory.
     */
    public void execute()
        throws MojoExecutionException
    {
        if ( getLog().isInfoEnabled() )
        {
            getLog().info( "Building EJB " + jarName + " with EJB version " + ejbVersion );
        }

        File jarFile = getEJBJarFile( basedir, jarName, classifier );

        MavenArchiver archiver = new MavenArchiver();

        archiver.setArchiver( jarArchiver );

        archiver.setOutputFile( jarFile );

        File deploymentDescriptor = new File( outputDirectory, EJB_JAR_XML );

        /* test EJB version compliance */
        if ( !ejbVersion.matches( "\\A[2-3]\\.[0-9]\\z" ) )
        {
            throw new MojoExecutionException(
                "ejbVersion is not valid: " + ejbVersion + ". Must be 2.x or 3.x (where x is a digit)" );
        }

        if ( ejbVersion.matches( "\\A2\\.[0-9]\\z" ) && !deploymentDescriptor.exists() )
        {
            throw new MojoExecutionException(
                "Error assembling EJB: " + EJB_JAR_XML + " is required for ejbVersion 2.x" );
        }

        try
        {
            String[] mainJarExcludes = DEFAULT_EXCLUDES;

            if ( excludes != null && !excludes.isEmpty() ) {
                mainJarExcludes = (String[]) excludes.toArray( EMPTY_STRING_ARRAY );
            }

            archiver.getArchiver().addDirectory( new File( outputDirectory ), DEFAULT_INCLUDES, mainJarExcludes );

            if ( deploymentDescriptor.exists() )
            {
                archiver.getArchiver().addFile( deploymentDescriptor, EJB_JAR_XML );
            }

            // create archive
            archiver.createArchive( project, archive );
        }
        catch ( ArchiverException e )
        {
            throw new MojoExecutionException( "There was a problem creating the EJB archive: " + e.getMessage(), e );
        }
        catch ( ManifestException e )
        {
            throw new MojoExecutionException( "There was a problem creating the EJB archive: " + e.getMessage(), e );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "There was a problem creating the EJB archive: " + e.getMessage(), e );
        }
        catch ( DependencyResolutionRequiredException e )
        {
            throw new MojoExecutionException( "There was a problem creating the EJB archive: " + e.getMessage(), e );
        }

        // Handle the classifier if necessary
        if ( classifier != null )
        {
            projectHelper.attachArtifact( project, "ejb", classifier, jarFile );
        }
        else
        {
            project.getArtifact().setFile( jarFile );
        }

        if ( generateClient )
        {
            String clientJarName = jarName;
            if ( classifier != null )
            {
                clientJarName += "-" + classifier;
            }

            getLog().info( "Building EJB client " + clientJarName + "-client" );

            String[] excludes = DEFAULT_CLIENT_EXCLUDES;
            String[] includes = DEFAULT_INCLUDES;

            if ( clientIncludes != null && !clientIncludes.isEmpty() )
            {
                includes = (String[]) clientIncludes.toArray( EMPTY_STRING_ARRAY );
            }

            if ( clientExcludes != null && !clientExcludes.isEmpty() )
            {
                excludes = (String[]) clientExcludes.toArray( EMPTY_STRING_ARRAY );
            }

            File clientJarFile = new File( basedir, clientJarName + "-client.jar" );

            MavenArchiver clientArchiver = new MavenArchiver();

            clientArchiver.setArchiver( clientJarArchiver );

            clientArchiver.setOutputFile( clientJarFile );

            try
            {
                clientArchiver.getArchiver().addDirectory( new File( outputDirectory ), includes, excludes );

                // create archive
                clientArchiver.createArchive( project, archive );

            }
            catch ( ArchiverException e )
            {
                throw new MojoExecutionException(
                    "There was a problem creating the EJB client archive: " + e.getMessage(), e );
            }
            catch ( ManifestException e )
            {
                throw new MojoExecutionException(
                    "There was a problem creating the EJB client archive: " + e.getMessage(), e );
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException(
                    "There was a problem creating the EJB client archive: " + e.getMessage(), e );
            }
            catch ( DependencyResolutionRequiredException e )
            {
                throw new MojoExecutionException(
                    "There was a problem creating the EJB client archive: " + e.getMessage(), e );
            }

            // TODO: shouldn't need classifer
            projectHelper.attachArtifact( project, "ejb-client", "client", clientJarFile );
        }
    }

    /**
     * Returns the EJB Jar file to generate, based on an optional classifier.
     *
     * @param basedir    the output directory
     * @param finalName  the name of the ear file
     * @param classifier an optional classifier
     * @return the EJB file to generate
     */
    private static File getEJBJarFile( String basedir, String finalName, String classifier )
    {
        if ( classifier == null )
        {
            classifier = "";
        }
        else if ( classifier.trim().length() > 0 && !classifier.startsWith( "-" ) )
        {
            classifier = "-" + classifier;
        }

        return new File( basedir, finalName + classifier + ".jar" );
    }

}
