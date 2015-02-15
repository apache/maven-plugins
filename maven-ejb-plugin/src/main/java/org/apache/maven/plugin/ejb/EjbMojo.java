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

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.XmlStreamReader;
import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.shared.filtering.MavenFileFilter;
import org.apache.maven.shared.filtering.MavenFilteringException;
import org.apache.maven.shared.filtering.MavenResourcesExecution;
import org.apache.maven.shared.utils.io.FileUtils.FilterWrapper;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.jar.ManifestException;
import org.codehaus.plexus.util.FileUtils;

/**
 * Build an EJB (and optional client) from the current project.
 *
 * @author <a href="evenisse@apache.org">Emmanuel Venisse</a>
 * @version $Id$
 */
// CHECKSTYLE_OFF: LineLength
@Mojo( name = "ejb", requiresDependencyResolution = ResolutionScope.RUNTIME, threadSafe = true, defaultPhase = LifecyclePhase.PACKAGE )
// CHECKSTYLE_ON: LineLength
public class EjbMojo
    extends AbstractMojo
{
    // TODO: will null work instead?
    private static final String[] DEFAULT_INCLUDES = new String[] { "**/**" };

    private static final String[] DEFAULT_CLIENT_EXCLUDES = new String[] { "**/*Bean.class", "**/*CMP.class",
        "**/*Session.class", "**/package.html" };

    /**
     * The directory location for the generated EJB.
     */
    @Parameter( defaultValue = "${project.build.directory}", required = true, readonly = true )
    private File outputDirectory;

    /**
     * Directory that contains the resources which are packaged into
     * the created archive {@code target/classes}.
     */
    @Parameter( defaultValue = "${project.build.outputDirectory}", required = true )
    private File sourceDirectory;

    /**
     * The name of the EJB file to generate.
     */
    @Parameter( property = "jarName", defaultValue = "${project.build.finalName}" )
    private String jarName;

    /**
     * Classifier to add to the artifact generated. If given, the artifact will be an attachment instead.
     */
    @Parameter( property = "ejb.classifier" )
    private String classifier;

    /**
     * You can define the location of <code>ejb-jar.xml</code> file.
     */
    @Parameter( property = "ejb.ejbJar", defaultValue = "META-INF/ejb-jar.xml" )
    // The initialization is needed to get the unit tests running which seemed to lack lookup for the defaultValue.
    private String ejbJar = "META-INF/ejb-jar.xml";

    /**
     * Whether the EJB client jar should be generated or not.
     */
    @Parameter( property = "ejb.generateClient", defaultValue = "false" )
    private boolean generateClient;

    /**
     * The files and directories to exclude from the client jar. Usage:
     * <p/>
     * 
     * <pre>
     * &lt;clientExcludes&gt;
     * &nbsp;&nbsp;&lt;clientExclude&gt;**&#47;*Ejb.class&lt;&#47;clientExclude&gt;
     * &nbsp;&nbsp;&lt;clientExclude&gt;**&#47;*Bean.class&lt;&#47;clientExclude&gt;
     * &lt;&#47;clientExcludes&gt;
     * </pre>
     * 
     * <br/>
     * Attribute is used only if client jar is generated. <br/>
     * Default exclusions: **&#47;*Bean.class, **&#47;*CMP.class, **&#47;*Session.class, **&#47;package.html
     */
    @Parameter
    private List<String> clientExcludes;

    /**
     * The files and directories to include in the client jar. Usage:
     * <p/>
     * 
     * <pre>
     * &lt;clientIncludes&gt;
     * &nbsp;&nbsp;&lt;clientInclude&gt;**&#47;*&lt;&#47;clientInclude&gt;
     * &lt;&#47;clientIncludes&gt;
     * </pre>
     * 
     * <br/>
     * Attribute is used only if client jar is generated. <br/>
     * Default value: **&#47;**
     */
    @Parameter
    private List<String> clientIncludes;

    /**
     * The files and directories to exclude from the main EJB jar. Usage:
     * <p/>
     * 
     * <pre>
     * &lt;excludes&gt;
     *   &lt;exclude&gt;**&#47;*Ejb.class&lt;&#47;exclude&gt;
     *   &lt;exclude&gt;**&#47;*Bean.class&lt;&#47;exclude&gt;
     * &lt;&#47;excludes&gt;
     * </pre>
     * 
     * <br/>
     * Default exclusions: META-INF&#47;ejb-jar.xml, **&#47;package.html
     */
    @Parameter
    private List<String> excludes;

    /**
     * The Maven project.
     */
    @Parameter( defaultValue = "${project}", readonly = true, required = true )
    private MavenProject project;

    /**
     * The Jar archiver.
     */
    @Component( role = Archiver.class, hint = "jar" )
    private JarArchiver jarArchiver;

    /**
     * What EJB version should the EJB Plugin generate? Valid values are "2.x" or "3.x" (where x is a digit). When
     * ejbVersion is "3.x", the <code>ejb-jar.xml</code> file is optional.
     * <p/>
     * Usage:
     * 
     * <pre>
     * &lt;ejbVersion&gt;3.0&lt;&#47;ejbVersion&gt;
     * </pre>
     *
     * @since 2.1
     */
    @Parameter( property = "ejb.ejbVersion", defaultValue = "2.1" )
    private String ejbVersion;

    /**
     * The client Jar archiver.
     */
    @Component( role = Archiver.class, hint = "jar" )
    private JarArchiver clientJarArchiver;

    /**
     * The Maven project's helper.
     */
    @Component
    private MavenProjectHelper projectHelper;

    /**
     * The archive configuration to use. See <a href="http://maven.apache.org/shared/maven-archiver/index.html">Maven
     * Archiver Reference</a>.
     */
    @Parameter
    private MavenArchiveConfiguration archive = new MavenArchiveConfiguration();

    /**
     * To escape interpolated value with windows path. c:\foo\bar will be replaced with c:\\foo\\bar.
     *
     * @since 2.3
     */
    @Parameter( property = "ejb.escapeBackslashesInFilePath", defaultValue = "false" )
    private boolean escapeBackslashesInFilePath;

    /**
     * An expression preceded with this String won't be interpolated. \${foo} will be replaced with ${foo}.
     *
     * @since 2.3
     */
    @Parameter( property = "ejb.escapeString" )
    protected String escapeString;

    /**
     * To filter the deployment descriptor.
     *
     * @since 2.3
     */
    @Parameter( property = "ejb.filterDeploymentDescriptor", defaultValue = "false" )
    private boolean filterDeploymentDescriptor;

    /**
     * Filters (properties files) to include during the interpolation of the deployment descriptor.
     *
     * @since 2.3
     */
    @Parameter
    private List<String> filters;

    /**
     * @since 2.3
     */
    @Component( role = MavenFileFilter.class, hint = "default" )
    private MavenFileFilter mavenFileFilter;

    /**
     * @since 2.3
     */
    @Parameter( defaultValue = "${session}", readonly = true, required = true )
    private MavenSession session;

    /**
     * Generates an EJB jar and optionally an ejb-client jar.
     *
     * @todo Add license files in META-INF directory.
     */
    public void execute()
        throws MojoExecutionException
    {

        if ( !sourceDirectory.exists() )
        {
            getLog().warn( "The created EJB jar will be empty cause the " + sourceDirectory.getPath()
                               + " did not exist." );
            sourceDirectory.mkdirs();
        }

        if ( getLog().isInfoEnabled() )
        {
            getLog().info( "Building EJB " + jarName + " with EJB version " + ejbVersion );
        }

        File jarFile = getEJBJarFile( outputDirectory, jarName, classifier );

        MavenArchiver archiver = new MavenArchiver();

        archiver.setArchiver( jarArchiver );

        archiver.setOutputFile( jarFile );

        File deploymentDescriptor = new File( sourceDirectory, ejbJar );

        /* test EJB version compliance */
        checkEJBVersionCompliance( deploymentDescriptor );

        try
        {
            // TODO: This should be handled different.
            String[] mainJarExcludes = new String[] { ejbJar, "**/package.html" };

            if ( excludes != null && !excludes.isEmpty() )
            {
                excludes.add( ejbJar );
                mainJarExcludes = (String[]) excludes.toArray( new String[excludes.size()] );
            }

            archiver.getArchiver().addDirectory( sourceDirectory, DEFAULT_INCLUDES, mainJarExcludes );

            if ( deploymentDescriptor.exists() )
            {
                // EJB-34 Filter ejb-jar.xml
                if ( filterDeploymentDescriptor )
                {
                    filterDeploymentDescriptor( deploymentDescriptor );
                }
                archiver.getArchiver().addFile( deploymentDescriptor, ejbJar );
            }

            // create archive
            archiver.createArchive( session, project, archive );
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
        catch ( MavenFilteringException e )
        {
            throw new MojoExecutionException( "There was a problem filtering the deployment descriptor: "
                + e.getMessage(), e );
        }

        // Handle the classifier if necessary
        // TODO: For 3.0 this should be changed having a separate classifier for main artifact and ejb-client.
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
            generateEjbClient();
        }
    }

    private void generateEjbClient()
        throws MojoExecutionException
    {
        String clientJarName = jarName;
        if ( classifier != null )
        {
            clientJarName += "-" + classifier;
        }

        String resultingClientJarNameWithClassifier = clientJarName + "-client";
        getLog().info( "Building EJB client " + resultingClientJarNameWithClassifier );

        String[] excludes = DEFAULT_CLIENT_EXCLUDES;
        String[] includes = DEFAULT_INCLUDES;

        if ( clientIncludes != null && !clientIncludes.isEmpty() )
        {
            includes = (String[]) clientIncludes.toArray( new String[clientIncludes.size()] );
        }

        if ( clientExcludes != null && !clientExcludes.isEmpty() )
        {
            excludes = (String[]) clientExcludes.toArray( new String[clientExcludes.size()] );
        }

        File clientJarFile = new File( outputDirectory, resultingClientJarNameWithClassifier + ".jar" );

        MavenArchiver clientArchiver = new MavenArchiver();

        clientArchiver.setArchiver( clientJarArchiver );

        clientArchiver.setOutputFile( clientJarFile );

        try
        {
            clientArchiver.getArchiver().addDirectory( sourceDirectory, includes, excludes );

            // create archive
            clientArchiver.createArchive( session, project, archive );

        }
        catch ( ArchiverException e )
        {
            throw new MojoExecutionException( "There was a problem creating the EJB client archive: "
                + e.getMessage(), e );
        }
        catch ( ManifestException e )
        {
            throw new MojoExecutionException( "There was a problem creating the EJB client archive: "
                + e.getMessage(), e );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "There was a problem creating the EJB client archive: "
                + e.getMessage(), e );
        }
        catch ( DependencyResolutionRequiredException e )
        {
            throw new MojoExecutionException( "There was a problem creating the EJB client archive: "
                + e.getMessage(), e );
        }

        // TODO: shouldn't need classifer
        // TODO: For 3.0 this should be changed having a separate classifier for main artifact and ejb-client.
        if ( classifier != null )
        {
            projectHelper.attachArtifact( project, "ejb-client", classifier + "-client", clientJarFile );
        }
        else
        {
            projectHelper.attachArtifact( project, "ejb-client", "client", clientJarFile );
        }
    }

    private void checkEJBVersionCompliance( File deploymentDescriptor )
        throws MojoExecutionException
    {
        if ( !ejbVersion.matches( "\\A[2-3]\\.[0-9]\\z" ) )
        {
            throw new MojoExecutionException( "ejbVersion is not valid: " + ejbVersion
                + ". Must be 2.x or 3.x (where x is a digit)" );
        }

        if ( ejbVersion.matches( "\\A2\\.[0-9]\\z" ) && !deploymentDescriptor.exists() )
        {
            throw new MojoExecutionException( "Error assembling EJB: " + ejbJar + " is required for ejbVersion 2.x" );
        }
    }

    private void filterDeploymentDescriptor( File deploymentDescriptor )
        throws MavenFilteringException, IOException
    {
        getLog().debug( "Filtering deployment descriptor." );
        MavenResourcesExecution mavenResourcesExecution = new MavenResourcesExecution();
        mavenResourcesExecution.setEscapeString( escapeString );
        List<FilterWrapper> filterWrappers =
            mavenFileFilter.getDefaultFilterWrappers( project, filters, escapeBackslashesInFilePath,
                                                      this.session, mavenResourcesExecution );

        // Create a temporary file that we can copy-and-filter
        File unfilteredDeploymentDescriptor = new File( sourceDirectory, ejbJar + ".unfiltered" );
        FileUtils.copyFile( deploymentDescriptor, unfilteredDeploymentDescriptor );
        mavenFileFilter.copyFile( unfilteredDeploymentDescriptor, deploymentDescriptor, true,
                                  filterWrappers, getEncoding( unfilteredDeploymentDescriptor ) );
        // Remove the temporary file
        FileUtils.forceDelete( unfilteredDeploymentDescriptor );
    }

    /**
     * Returns the EJB Jar file to generate, based on an optional classifier.
     *
     * @param basedir the output directory
     * @param finalName the name of the ear file
     * @param classifier an optional classifier
     * @return the EJB file to generate
     */
    private static File getEJBJarFile( File basedir, String finalName, String classifier )
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
        XmlStreamReader xmlReader = null;
        try
        {
            xmlReader = new XmlStreamReader( xmlFile );
            return xmlReader.getEncoding();
        }
        finally
        {
            IOUtils.closeQuietly( xmlReader );
        }
    }

}
