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
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.archiver.jar.JarArchiver;

import java.io.File;
import java.util.List;

/**
 * Build an EJB (and optional client) from the current project.
 *
 * @author <a href="evenisse@apache.org">Emmanuel Venisse</a>
 * @version $Id$
 * @goal ejb
 * @phase package
 * @description build an ejb
 */
public class EjbMojo
    extends AbstractMojo
{
    // TODO: will null work instead?
    private static final String[] DEFAULT_INCLUDES = new String[]{"**/**"};

    private static final String[] DEFAULT_EXCLUDES = new String[]{"**/*Bean.class", "**/*CMP.class",
        "**/*Session.class", "**/package.html"};

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
        getLog().info( "Building ejb " + jarName );

        File jarFile = new File( basedir, jarName + ".jar" );

        MavenArchiver archiver = new MavenArchiver();

        archiver.setArchiver( jarArchiver );

        archiver.setOutputFile( jarFile );

        String ejbJarXmlFile = "META-INF/ejb-jar.xml";

        try
        {
            archiver.getArchiver().addDirectory( new File( outputDirectory ), DEFAULT_INCLUDES,
                                                 new String[]{ejbJarXmlFile, "**/package.html"} );

            archiver.getArchiver().addFile( new File( outputDirectory, ejbJarXmlFile ), ejbJarXmlFile );

            // create archive
            archiver.createArchive( project, archive );

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

                clientArchiver.getArchiver().addDirectory(
                        new File( outputDirectory ), includes, excludes );

                // create archive
                clientArchiver.createArchive( project, archive );

                // TODO: shouldn't need classifer
                projectHelper.attachArtifact( project, "ejb-client", "client", clientJarFile );
            }
        }
        catch ( Exception e )
        {
            // TODO: improve error handling
            throw new MojoExecutionException( "Error assembling EJB", e );
        }
    }
}
