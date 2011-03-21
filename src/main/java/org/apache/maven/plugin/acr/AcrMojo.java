package org.apache.maven.plugin.acr;

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
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.filtering.MavenFileFilter;
import org.apache.maven.shared.filtering.MavenFilteringException;
import org.apache.maven.shared.filtering.MavenResourcesExecution;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.jar.ManifestException;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.xml.XmlStreamReader;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Build a JEE Application Client jar file from the current project.
 *
 * @author <a href="pablo@anahata-it.com">Pablo Rodriguez</a>
 * @author <a href="snicoll@apache.org">Stephane Nicoll</a>
 * @version $Id:
 * @goal acr
 * @requiresDependencyResolution runtime
 * @threadSafe
 * @phase package
 */
public class AcrMojo
    extends AbstractMojo
{

    private static final String APP_CLIENT_XML = "META-INF/application-client.xml";

    // TODO: will null work instead?
    private static final String[] DEFAULT_INCLUDES = new String[]{ "**/**" };

    private static final String[] DEFAULT_EXCLUDES = new String[]{ APP_CLIENT_XML };

    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    /**
     * The directory for the generated jar.
     *
     * @parameter default-value="${project.build.directory}"
     * @required
     * @readonly
     */
    private File basedir;

    /**
     * Directory that resources are copied to during the build.
     *
     * @parameter default-value="${project.build.outputDirectory}" expression="${outputDirectory}"
     */
    private File outputDirectory;

    /**
     * The name of the Application client JAR file to generate.
     *
     * @parameter default-value="${project.build.finalName}" expression="${jarName}"
     */
    private String jarName;

    /**
     * The files and directories to exclude from the main Application Client jar. Usage:
     * <p/>
     * <pre>
     * &lt;excludes&gt;
     *   &lt;exclude&gt;**&#47;*DevOnly.class&lt;&#47;exclude&gt;
     * &lt;&#47;excludes&gt;
     * </pre>
     * <br/>Default exclusions: META-INF&#47;application-client.xml,
     *
     * @parameter
     */
    private List excludes;

    /**
     * The Maven project.
     *
     * @parameter default-value="${project}"
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
     * The archive configuration to use.
     * See <a href="http://maven.apache.org/shared/maven-archiver/index.html">Maven Archiver Reference</a>.
     *
     * @parameter
     */
    private MavenArchiveConfiguration archive = new MavenArchiveConfiguration();

    /**
     * To escape interpolated value with windows path.
     * c:\foo\bar will be replaced with c:\\foo\\bar.
     *
     * @parameter default-value="false" expression="${acr.escapeBackslashesInFilePath}"
     */
    private boolean escapeBackslashesInFilePath;

    /**
     * An expression preceded with this String won't be interpolated.
     * \${foo} will be replaced with ${foo}.
     *
     * @parameter expression="${car.escapeString}"
     */
    protected String escapeString;

    /**
     * To filter the deployment descriptor.
     *
     * @parameter default-value="false" expression="${acr.filterDeploymentDescriptor}"
     */
    private boolean filterDeploymentDescriptor;

    /**
     * Filters (properties files) to include during the interpolation of the deployment descriptor.
     *
     * @parameter
     */
    private List filters;

    /**
     * @component role="org.apache.maven.shared.filtering.MavenFileFilter" role-hint="default"
     * @required
     */
    private MavenFileFilter mavenFileFilter;

    /**
     * @parameter expression="${session}"
     * @readonly
     * @required
     */
    private MavenSession session;

    /**
     * Generates the application client jar file
     *
     * @todo Add license files in META-INF directory.
     */
    public void execute()
        throws MojoExecutionException
    {
        if ( getLog().isInfoEnabled() )
        {
            getLog().info( "Building JavaEE Application client: " + jarName );
        }

        File jarFile = getAppClientJarFile( basedir, jarName );

        MavenArchiver archiver = new MavenArchiver();

        archiver.setArchiver( jarArchiver );

        archiver.setOutputFile( jarFile );

        File deploymentDescriptor = new File( outputDirectory, APP_CLIENT_XML );
        try
        {
            String[] mainJarExcludes = DEFAULT_EXCLUDES;

            if ( excludes != null && !excludes.isEmpty() )
            {
                excludes.add( APP_CLIENT_XML );
                mainJarExcludes = (String[]) excludes.toArray( EMPTY_STRING_ARRAY );
            }

            if ( !outputDirectory.exists() )
            {
                getLog().info(
                    "JAR will only contain the META-INF/application-client.xml as no content was marked for inclusion" );
            }
            else
            {
                archiver.getArchiver().addDirectory( outputDirectory, DEFAULT_INCLUDES, mainJarExcludes );
            }

            if ( deploymentDescriptor.exists() )
            {
                if ( filterDeploymentDescriptor )
                {
                    getLog().debug( "Filtering deployment descriptor." );
                    MavenResourcesExecution mavenResourcesExecution = new MavenResourcesExecution();
                    mavenResourcesExecution.setEscapeString( escapeString );
                    List filterWrappers =
                        mavenFileFilter.getDefaultFilterWrappers( project, filters, escapeBackslashesInFilePath,
                                                                  this.session, mavenResourcesExecution );

                    // Create a temporary file that we can copy-and-filter
                    File unfilteredDeploymentDescriptor = new File( outputDirectory, APP_CLIENT_XML + ".unfiltered" );
                    FileUtils.copyFile( deploymentDescriptor, unfilteredDeploymentDescriptor );
                    mavenFileFilter.copyFile( unfilteredDeploymentDescriptor, deploymentDescriptor, true,
                                              filterWrappers, getEncoding( unfilteredDeploymentDescriptor ) );
                    // Remove the temporary file
                    FileUtils.forceDelete( unfilteredDeploymentDescriptor );
                }
                archiver.getArchiver().addFile( deploymentDescriptor, APP_CLIENT_XML );
            }

            // create archive
            archiver.createArchive( project, archive );
        }
        catch ( ArchiverException e )
        {
            throw new MojoExecutionException(
                "There was a problem creating the JavaEE Application Client  archive: " + e.getMessage(), e );
        }
        catch ( ManifestException e )
        {
            throw new MojoExecutionException(
                "There was a problem reading / creating the manifest for the JavaEE Application Client  archive: " +
                    e.getMessage(), e );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException(
                "There was a I/O problem creating the JavaEE Application Client archive: " + e.getMessage(), e );
        }
        catch ( DependencyResolutionRequiredException e )
        {
            throw new MojoExecutionException(
                "There was a problem resolving dependencies while creating the JavaEE Application Client archive: " +
                    e.getMessage(), e );
        }
        catch ( MavenFilteringException e )
        {
            throw new MojoExecutionException(
                "There was a problem filtering the deployment descriptor: " + e.getMessage(), e );
        }

        project.getArtifact().setFile( jarFile );


    }

    /**
     * Returns the App-client Jar file to generate.
     *
     * @param basedir   the output directory
     * @param finalName the name of the ear file
     * @return the Application client JAR file to generate
     */
    private static File getAppClientJarFile( File basedir, String finalName )
    {
        return new File( basedir, finalName + ".jar" );
    }

    /**
     * Get the encoding from an XML-file.
     *
     * @param xmlFile the XML-file
     * @return The encoding of the XML-file, or UTF-8 if it's not specified in the file
     * @throws IOException if an error occurred while reading the file
     */
    private String getEncoding( File xmlFile )
        throws IOException
    {
        XmlStreamReader xmlReader = new XmlStreamReader( xmlFile );
        return xmlReader.getEncoding();
    }
}
