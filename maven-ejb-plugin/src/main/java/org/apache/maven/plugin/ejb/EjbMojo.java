package org.apache.maven.plugin.ejb;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
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
    // TODO: will null work instead?
    private static final String[] DEFAULT_INCLUDES = new String[]{"**/**"};

    private static final String[] DEFAULT_EXCLUDES = new String[]{"**/*Bean.class", "**/*CMP.class",
        "**/*Session.class", "**/package.html"};

    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    private static final String EJB_JAR_XML = "META-INF/ejb-jar.xml";

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
     * @parameter expression="${project.build.outputDirectory}"
     * @required
     */
    private String outputDirectory;

    /**
     * The name of the EJB file to generate.
     *
     * @parameter expression="${project.build.finalName}"
     * @required
     */
    private String jarName;

    /**
     * Whether the ejb client jar should be generated or not. Default
     * is false.
     *
     * @parameter
     * @todo boolean instead
     */
    private String generateClient = Boolean.FALSE.toString();

    /**
     * Excludes.
     *
     * <br/>Usage:
     * <pre>
     * &lt;clientIncludes&gt;
     *   &lt;clientInclude&gt;**&#47;*Ejb.class&lt;&#47;clientInclude&gt;
     *   &lt;clientInclude&gt;**&#47;*Bean.class&lt;&#47;clientInclude&gt;
     * &lt;&#47;clientIncludes&gt;
     * </pre>
     * <br/>Attribute is used only if client jar is generated.
     * <br/>Default exclusions: **&#47;*Bean.class, **&#47;*CMP.class, **&#47;*Session.class, **&#47;package.html
     * @parameter
     */
    private List clientExcludes;

    /**
     * Includes.
     *
     * <br/>Usage:
     * <pre>
     * &lt;clientIncludes&gt;
     *   &lt;clientInclude&gt;**&#47;*&lt;&#47;clientInclude&gt;
     * &lt;&#47;clientIncludes&gt;
     * </pre>
     * <br/>Attribute is used only if client jar is generated.
     * <br/>Default value: **&#47;**
     * @parameter
     */
    private List clientIncludes;

    /**
     * The maven project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * The Jar archiver.
     *
     * @parameter expression="${component.org.codehaus.plexus.archiver.Archiver#jar}"
     * @required
     */
    private JarArchiver jarArchiver;
    
    /**
     * <p>
     * What EJB version should the ejb-plugin generate? ejbVersion can be "2.x" or "3.x" 
     * (where x is a digit), defaulting to "2.1".  When ejbVersion is "3.x", the 
     * ejb-jar.xml file is optional.
     * </p>
     * 
     * <p>
     * Usage:
     * <pre>
     * &lt;ejbVersion&gt;3.0&lt;&#47;ejbVersion&gt;
     * </pre>
     * </p>
     * @parameter default-value="2.1"
     * @required
     */
    private String ejbVersion;

    /**
     * The client Jar archiver.
     *
     * @parameter expression="${component.org.codehaus.plexus.archiver.Archiver#jar}"
     * @required
     */
    private JarArchiver clientJarArchiver;

    /**
     * The maven project's helper.
     *
     * @parameter expression="${component.org.apache.maven.project.MavenProjectHelper}"
     * @required
     * @readonly
     */
    private MavenProjectHelper projectHelper;

    /**
     * The maven archiver to use.
     *
     * @parameter
     */
    private MavenArchiveConfiguration archive = new MavenArchiveConfiguration();

    /**
     * Generates an ejb jar and optionnaly an ejb-client jar.
     *
     * @todo Add license files in META-INF directory.
     */
    public void execute()
        throws MojoExecutionException
    {
        if ( getLog().isInfoEnabled() )
        {
            getLog().info( "Building ejb " + jarName + " with ejbVersion " + ejbVersion );
        }

        File jarFile = new File( basedir, jarName + ".jar" );

        MavenArchiver archiver = new MavenArchiver();

        archiver.setArchiver( jarArchiver );

        archiver.setOutputFile( jarFile );

        File deploymentDescriptor = new File( outputDirectory, EJB_JAR_XML );

        /* test EJB version compliance */
        if ( !ejbVersion.matches( "\\A[2-3]\\.[0-9]\\z" ) )
        {
            throw new MojoExecutionException( "ejbVersion is not valid: " + ejbVersion
                + ". Must be 2.x or 3.x (where x is a digit)" );
        }

        if ( ejbVersion.matches( "\\A2\\.[0-9]\\z" ) && !deploymentDescriptor.exists() )
        {
            throw new MojoExecutionException( "Error assembling EJB: " + EJB_JAR_XML
                + " is required for ejbVersion 2.x" );
        }

        try
        {
            archiver.getArchiver().addDirectory( new File( outputDirectory ), DEFAULT_INCLUDES,
                                                 new String[] { EJB_JAR_XML, "**/package.html" } );


            if ( deploymentDescriptor.exists() )
            {
                archiver.getArchiver().addFile( deploymentDescriptor, EJB_JAR_XML );
            }
            
            // create archive
            archiver.createArchive( project, archive );
        }
        catch ( ArchiverException e )
        {
            throw new MojoExecutionException( "There was a problem creating the EJB archive: " + e.getMessage() , e );
        }
        catch ( ManifestException e )
        {
            throw new MojoExecutionException( "There was a problem creating the EJB archive: " + e.getMessage() , e );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "There was a problem creating the EJB archive: " + e.getMessage() , e );
        }
        catch ( DependencyResolutionRequiredException e )
        {
            throw new MojoExecutionException( "There was a problem creating the EJB archive: " + e.getMessage() , e );
        }

        project.getArtifact().setFile( jarFile );

        if ( new Boolean( generateClient ).booleanValue() )
        {
            getLog().info( "Building ejb client " + jarName + "-client" );

            String[] excludes = DEFAULT_EXCLUDES;
            String[] includes = DEFAULT_INCLUDES;

            if ( clientIncludes != null && !clientIncludes.isEmpty() )
            {
                includes = (String[]) clientIncludes.toArray( EMPTY_STRING_ARRAY );
            }

            if ( clientExcludes != null && !clientExcludes.isEmpty() )
            {
                excludes = (String[]) clientExcludes.toArray( EMPTY_STRING_ARRAY );
            }

            File clientJarFile = new File( basedir, jarName + "-client.jar" );

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
}
