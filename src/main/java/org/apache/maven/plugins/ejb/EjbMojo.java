package org.apache.maven.plugins.ejb;

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
import java.util.Collections;
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

import com.google.inject.internal.util.Lists;

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
    private static final List<String> DEFAULT_INCLUDES_LIST =
        Collections.unmodifiableList( Lists.newArrayList( "**/**" ) );

    private static final List<String> DEFAULT_CLIENT_EXCLUDES_LIST =
        Collections.unmodifiableList( Lists.newArrayList( "**/*Bean.class", "**/*CMP.class", "**/*Session.class",
                                                          "**/package.html" ) );

    /**
     * Default value for {@link #clientClassifier}
     */
    public static final String DEFAULT_CLIENT_CLASSIFIER = "client";

    /**
     * Default value for {@link #ejbJar}.
     */
    public static final String DEFAULT_EJBJAR = "META-INF/ejb-jar.xml";

    /**
     * The directory location for the generated EJB.
     */
    @Parameter( defaultValue = "${project.build.directory}", required = true, readonly = true )
    private File outputDirectory;

    /**
     * Directory that contains the resources which are packaged into the created archive {@code target/classes}.
     */
    @Parameter( defaultValue = "${project.build.outputDirectory}", required = true )
    private File sourceDirectory;

    /**
     * The name of the EJB file to generate.
     */
    @Parameter( property = "maven.ejb.jarName", defaultValue = "${project.build.finalName}" )
    private String jarName;

    /**
     * Classifier to add to the artifact generated. If given, the artifact will be an attachment instead.
     */
    @Parameter( property = "maven.ejb.classifier" )
    private String classifier;

    /**
     * Classifier which is used for the client artifact.
     * 
     * @since 3.0.0
     */
    @Parameter( property = "maven.ejb.clientClassifier", defaultValue = DEFAULT_CLIENT_CLASSIFIER )
    private String clientClassifier;

    /**
     * You can define the location of <code>ejb-jar.xml</code> file.
     */
    @Parameter( property = "maven.ejb.ejbJar", defaultValue = DEFAULT_EJBJAR )
    private String ejbJar;

    /**
     * Whether the EJB client jar should be generated or not.
     */
    @Parameter( property = "maven.ejb.generateClient", defaultValue = "false" )
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
    @Parameter( property = "maven.ejb.ejbVersion", defaultValue = "3.1" )
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
    @Parameter( property = "maven.ejb.escapeBackslashesInFilePath", defaultValue = "false" )
    private boolean escapeBackslashesInFilePath;

    /**
     * An expression preceded with this String won't be interpolated. \${foo} will be replaced with ${foo}.
     *
     * @since 2.3
     */
    @Parameter( property = "maven.ejb.escapeString" )
    protected String escapeString;

    /**
     * To filter the deployment descriptor.
     *
     * @since 2.3
     */
    @Parameter( property = "maven.ejb.filterDeploymentDescriptor", defaultValue = "false" )
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

    private static final String EJB_TYPE = "ejb";

    private static final String EJB_CLIENT_TYPE = "ejb-client";

    /**
     * Generates an EJB jar and optionally an ejb-client jar.
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

        File jarFile = generateEjb();

        if ( hasClassifier() )
        {
            if ( !isClassifierValid() )
            {
                String message = "The given classifier '" + getClassifier() + "' is not valid.";
                getLog().error( message );
                throw new MojoExecutionException( message );
            }

            //TODO: We should check the attached artifacts to be sure we don't attach 
            // the same file twice...
            projectHelper.attachArtifact( project, EJB_TYPE, getClassifier(), jarFile );
        }
        else
        {
            if ( projectHasAlreadySetAnArtifact() )
            {
                throw new MojoExecutionException( "You have to use a classifier "
                    + "to attach supplemental artifacts to the project instead of replacing them." );
            }

            project.getArtifact().setFile( jarFile );
        }

        if ( generateClient )
        {
            File clientJarFile = generateEjbClient();
            if ( hasClientClassifier() )
            {
                if ( !isClientClassifierValid() )
                {
                    String message = "The given client classifier '" + getClientClassifier() + "' is not valid.";
                    getLog().error( message );
                    throw new MojoExecutionException( message );
                }

                projectHelper.attachArtifact( project, EJB_CLIENT_TYPE, getClientClassifier(), clientJarFile );
            }
            else
            {
                // FIXME: This does not make sense, cause a classifier for the client should always exist otherwise
                // Failure!
                projectHelper.attachArtifact( project, "ejb-client", getClientClassifier(), clientJarFile );
            }

        }
    }

    private boolean projectHasAlreadySetAnArtifact()
    {
        if ( getProject().getArtifact().getFile() != null )
        {
            return getProject().getArtifact().getFile().isFile();
        }
        else
        {
            return false;
        }
    }

    private File generateEjb()
        throws MojoExecutionException
    {
        File jarFile = EjbHelper.getJarFile( outputDirectory, jarName, getClassifier() );

        getLog().info( "Building EJB " + jarName + " with EJB version " + ejbVersion );

        MavenArchiver archiver = new MavenArchiver();

        archiver.setArchiver( jarArchiver );

        archiver.setOutputFile( jarFile );

        File deploymentDescriptor = new File( sourceDirectory, ejbJar );

        checkEJBVersionCompliance( deploymentDescriptor );

        try
        {
            List<String> defaultExcludes = Lists.newArrayList( ejbJar, "**/package.html" );
            List<String> defaultIncludes = DEFAULT_INCLUDES_LIST;

            IncludesExcludes ie =
                new IncludesExcludes( Collections.<String>emptyList(), excludes, defaultIncludes, defaultExcludes );

            archiver.getArchiver().addDirectory( sourceDirectory, ie.resultingIncludes(), ie.resultingExcludes() );

            // FIXME: We should be able to filter more than just the deployment descriptor?
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

        return jarFile;

    }

    private File generateEjbClient()
        throws MojoExecutionException
    {
        File clientJarFile = EjbHelper.getJarFile( outputDirectory, jarName, getClientClassifier() );

        getLog().info( "Building EJB client " + clientJarFile.getPath() );

        MavenArchiver clientArchiver = new MavenArchiver();

        clientArchiver.setArchiver( clientJarArchiver );

        clientArchiver.setOutputFile( clientJarFile );

        try
        {
            List<String> defaultExcludes = DEFAULT_CLIENT_EXCLUDES_LIST;
            List<String> defaultIncludes = DEFAULT_INCLUDES_LIST;

            IncludesExcludes ie =
                new IncludesExcludes( clientIncludes, clientExcludes, defaultIncludes, defaultExcludes );

            clientArchiver.getArchiver().addDirectory( sourceDirectory, ie.resultingIncludes(),
                                                       ie.resultingExcludes() );

            clientArchiver.createArchive( session, project, archive );

        }
        catch ( ArchiverException e )
        {
            throw new MojoExecutionException( "There was a problem creating the EJB client archive: " + e.getMessage(),
                                              e );
        }
        catch ( ManifestException e )
        {
            throw new MojoExecutionException( "There was a problem creating the EJB client archive: " + e.getMessage(),
                                              e );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "There was a problem creating the EJB client archive: " + e.getMessage(),
                                              e );
        }
        catch ( DependencyResolutionRequiredException e )
        {
            throw new MojoExecutionException( "There was a problem creating the EJB client archive: " + e.getMessage(),
                                              e );
        }

        return clientJarFile;
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
            mavenFileFilter.getDefaultFilterWrappers( project, filters, escapeBackslashesInFilePath, this.session,
                                                      mavenResourcesExecution );

        // Create a temporary file that we can copy-and-filter
        File unfilteredDeploymentDescriptor = new File( sourceDirectory, ejbJar + ".unfiltered" );
        FileUtils.copyFile( deploymentDescriptor, unfilteredDeploymentDescriptor );
        mavenFileFilter.copyFile( unfilteredDeploymentDescriptor, deploymentDescriptor, true, filterWrappers,
                                  getEncoding( unfilteredDeploymentDescriptor ) );
        // Remove the temporary file
        FileUtils.forceDelete( unfilteredDeploymentDescriptor );
    }

    /**
     * @return true in case where the classifier is not {@code null} and contains something else than white spaces.
     */
    private boolean hasClassifier()
    {
        return EjbHelper.hasClassifier( getClassifier() );
    }

    /**
     * @return true in case where the clientClassifier is not {@code null} and contains something else than white
     *         spaces.
     */
    private boolean hasClientClassifier()
    {
        return EjbHelper.hasClassifier( getClientClassifier() );
    }

    private boolean isClassifierValid()
    {
        return EjbHelper.isClassifierValid( getClassifier() );
    }

    private boolean isClientClassifierValid()
    {
        return EjbHelper.isClassifierValid( getClientClassifier() );
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
            final String encoding = xmlReader.getEncoding();
            xmlReader.close();
            xmlReader = null;
            return encoding;
        }
        finally
        {
            IOUtils.closeQuietly( xmlReader );
        }
    }

    public String getClassifier()
    {
        return classifier;
    }

    public String getClientClassifier()
    {
        return clientClassifier;
    }

    public MavenProject getProject()
    {
        return project;
    }

}
