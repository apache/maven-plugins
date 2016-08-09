package org.apache.maven.plugins.ear;

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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.plugins.ear.util.JavaEEVersion;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.interpolation.Interpolator;
import org.codehaus.plexus.interpolation.MapBasedValueSource;
import org.codehaus.plexus.interpolation.StringSearchInterpolator;
import org.codehaus.plexus.interpolation.ValueSource;
import org.codehaus.plexus.util.FileUtils;

/**
 * Generates the EAR deployment descriptor file(s).
 * 
 * @author <a href="snicoll@apache.org">Stephane Nicoll</a>
 * @version $Id: GenerateApplicationXmlMojo.java 1713543 2015-11-09 21:32:15Z khmarbaise $
 */
// CHECKSTYLE_OFF: LineLength
@Mojo( name = "generate-application-xml", defaultPhase = LifecyclePhase.GENERATE_RESOURCES, threadSafe = true, requiresDependencyResolution = ResolutionScope.TEST )
// CHECKSTYLE_ON: LineLength
public class GenerateApplicationXmlMojo
    extends AbstractEarMojo
{

    /**
     * The DEFAULT library folder.
     */
    public static final String DEFAULT = "DEFAULT";

    /**
     * The empty folder.
     */
    public static final String EMPTY = "EMPTY";

    /**
     * The NONE not existent folder.
     */
    public static final String NONE = "NONE";

    /**
     * Whether the application.xml should be generated or not.
     */
    @Parameter( defaultValue = "true" )
    private Boolean generateApplicationXml = Boolean.TRUE;

    /**
     * Whether a module ID should be generated if none is specified.
     */
    @Parameter( defaultValue = "false" )
    private Boolean generateModuleId = Boolean.FALSE;

    /**
     * Application name of the application to be used when the application.xml file is auto-generated. Since JavaEE6.
     */
    @Parameter
    private String applicationName;

    /**
     * Display name of the application to be used when the application.xml file is auto-generated.
     */
    @Parameter( defaultValue = "${project.artifactId}" )
    private String displayName;

    /**
     * Description of the application to be used when the application.xml file is auto-generated.
     */
    @Parameter( defaultValue = "${project.description}" )
    private String description;

    /**
     * Defines how the <tt>library-directory</tt> element should be written in the application.xml file.
     * <p/>
     * Three special values can be set:
     * <ul>
     * <li><code>DEFAULT</code> (default) generates a <tt>library-directory</tt> element with the value of the
     * <tt>defaultLibBundleDir</tt> parameter</li>
     * <li><code>EMPTY</code> generates an empty <tt>library-directory</tt> element. Per spec, this disables the
     * scanning of jar files in the <tt>lib</tt> directory of the ear file</li>
     * <li><code>NONE</code> does not write the library-directory element at all. A corner case that can be used in
     * Oracle Weblogic to delegate the classloading to the container</li>
     * </ul>
     * <p/>
     * Since JavaEE5.
     */
    @Parameter( defaultValue = DEFAULT )
    private String libraryDirectoryMode;

    /**
     * Defines the value of the initialize in order element to be used when the application.xml file is auto-generated.
     * When set to true, modules must be initialized in the order they're listed in this deployment descriptor, with the
     * exception of application client modules, which can be initialized in any order. If initialize-in-order is not set
     * or set to false, the order of initialization is unspecified and may be product-dependent. Since JavaEE6.
     */
    @Parameter
    private Boolean initializeInOrder;

    /**
     * Defines the application id used when generating the deployment descriptor.
     * 
     * @since 2.9
     */
    @Parameter
    private String applicationId;

    /**
     * The security-roles to be added to the auto-generated application.xml file.
     */
    @Parameter
    private PlexusConfiguration security;

    /**
     * The env-entries to be added to the auto-generated application.xml file. Since JavaEE6.
     */
    @Parameter( alias = "env-entries" )
    private PlexusConfiguration envEntries;

    /**
     * The {@code ejb-ref} entries.
     */
    @Parameter( alias = "ejb-refs" )
    private PlexusConfiguration ejbRefs;

    /**
     * {@inheritDoc}
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        // Initializes ear modules
        super.execute();

        // Handle application.xml
        if ( !generateApplicationXml )
        {
            getLog().debug( "Generation of application.xml is disabled" );
        }
        else
        {
            final JavaEEVersion javaEEVersion = JavaEEVersion.getJavaEEVersion( version );

            // Generate deployment descriptor and copy it to the build directory
            getLog().info( "Generating application.xml" );
            try
            {
                generateStandardDeploymentDescriptor( javaEEVersion );
            }
            catch ( EarPluginException e )
            {
                throw new MojoExecutionException( "Failed to generate application.xml", e );
            }

            try
            {
                FileUtils.copyFileToDirectory( new File( generatedDescriptorLocation, "application.xml" ),
                                               new File( getWorkDirectory(), "META-INF" ) );
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Unable to copy application.xml to final destination", e );
            }
        }

        // Handle jboss-app.xml
        if ( getJbossConfiguration() == null )
        {
            getLog().debug( "Generation of jboss-app.xml is disabled" );
        }
        else
        {
            // Generate deployment descriptor and copy it to the build directory
            getLog().info( "Generating jboss-app.xml" );
            try
            {
                generateJbossDeploymentDescriptor();
            }
            catch ( EarPluginException e )
            {
                throw new MojoExecutionException( "Failed to generate jboss-app.xml", e );
            }

            try
            {
                FileUtils.copyFileToDirectory( new File( generatedDescriptorLocation, "jboss-app.xml" ),
                                               new File( getWorkDirectory(), "META-INF" ) );
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Unable to copy jboss-app.xml to final destination", e );
            }
        }
    }

    /**
     * Generates the deployment descriptor.
     * 
     * @param javaEEVersion {@link JavaEEVersion}
     * @throws EarPluginException if the configuration is invalid
     */
    protected void generateStandardDeploymentDescriptor( JavaEEVersion javaEEVersion )
        throws EarPluginException
    {
        File outputDir = new File( generatedDescriptorLocation );
        if ( !outputDir.exists() )
        {
            outputDir.mkdirs();
        }

        File descriptor = new File( outputDir, "application.xml" );

        final ApplicationXmlWriter writer = new ApplicationXmlWriter( javaEEVersion, encoding, generateModuleId );
        final ApplicationXmlWriterContext context =
            new ApplicationXmlWriterContext( descriptor, getModules(), buildSecurityRoles(), buildEnvEntries(),
                                             buildEjbEntries(), displayName, description, getActualLibraryDirectory(),
                                             applicationName, initializeInOrder ).setApplicationId( applicationId );
        writer.write( context );
    }

    /**
     * Generates the jboss deployment descriptor.
     * 
     * @throws EarPluginException if the configuration is invalid
     */
    protected void generateJbossDeploymentDescriptor()
        throws EarPluginException
    {
        File outputDir = new File( generatedDescriptorLocation );
        if ( !outputDir.exists() )
        {
            outputDir.mkdirs();
        }

        File descriptor = new File( outputDir, "jboss-app.xml" );

        JbossAppXmlWriter writer = new JbossAppXmlWriter( encoding );
        writer.write( descriptor, getJbossConfiguration(), getModules() );
    }

    /**
     * Builds the security roles based on the configuration.
     * 
     * @return a list of SecurityRole object(s)
     * @throws EarPluginException if the configuration is invalid
     */
    private List<SecurityRole> buildSecurityRoles()
        throws EarPluginException
    {
        final List<SecurityRole> result = new ArrayList<SecurityRole>();
        if ( security == null )
        {
            return result;
        }
        final PlexusConfiguration[] securityRoles = security.getChildren( SecurityRole.SECURITY_ROLE );

        for ( PlexusConfiguration securityRole : securityRoles )
        {
            final String id = securityRole.getAttribute( SecurityRole.ID_ATTRIBUTE );
            final String childRoleName = securityRole.getChild( SecurityRole.ROLE_NAME ).getValue();
            final String childRoleNameId =
                securityRole.getChild( SecurityRole.ROLE_NAME ).getAttribute( SecurityRole.ID_ATTRIBUTE );
            final String childDescription = securityRole.getChild( SecurityRole.DESCRIPTION ).getValue();
            final String childDescriptionId =
                securityRole.getChild( SecurityRole.DESCRIPTION ).getAttribute( SecurityRole.ID_ATTRIBUTE );

            if ( childRoleName == null )
            {
                throw new EarPluginException( "Invalid security-role configuration, role-name could not be null." );
            }
            else
            {
                result.add( new SecurityRole( childRoleName, childRoleNameId, id, childDescription,
                                              childDescriptionId ) );
            }
        }
        return result;
    }

    /**
     * This help method was needed otherwise the interpolate method of interpolator will make an empty string of a
     * {@code null} element which results in supplemental elements for env-entry.
     * 
     * @param interpolator The interpolator
     * @param element The element
     * @return The interpolated elements.
     * @throws InterpolationException in case of an error.
     */
    private String interpolate( Interpolator interpolator, String element )
        throws InterpolationException
    {
        if ( element == null )
        {
            return element;
        }
        else
        {
            return interpolator.interpolate( element );
        }
    }

    /**
     * Builds the env-entries based on the configuration.
     * 
     * @return a list of EnvEntry object(s)
     * @throws EarPluginException if the configuration is invalid
     */
    private List<EnvEntry> buildEnvEntries()
        throws EarPluginException
    {
        final List<EnvEntry> result = new ArrayList<EnvEntry>();
        if ( envEntries == null )
        {
            return result;
        }
        try
        {
            StringSearchInterpolator ssi = new StringSearchInterpolator();
            ValueSource vs = new MapBasedValueSource( project.getProperties() );
            ssi.addValueSource( vs );

            final PlexusConfiguration[] allEnvEntries = envEntries.getChildren( EnvEntry.ENV_ENTRY );

            for ( PlexusConfiguration envEntry : allEnvEntries )
            {
                // CHECKSTYLE_OFF: LineLength
                final String childDescription =
                    interpolate( ssi, envEntry.getChild( EnvEntry.DESCRIPTION ).getValue() );
                final String childEnvEntryName =
                    interpolate( ssi, envEntry.getChild( EnvEntry.ENV_ENTRY_NAME ).getValue() );
                final String childEnvEntryType =
                    interpolate( ssi, envEntry.getChild( EnvEntry.ENV_ENTRY_TYPE ).getValue() );
                final String childEnvEntryValue =
                    interpolate( ssi, envEntry.getChild( EnvEntry.ENV_ENTRY_VALUE ).getValue() );
                // CHECKSTYLE_ON: LineLength

                try
                {
                    result.add( new EnvEntry( childDescription, childEnvEntryName, childEnvEntryType,
                                              childEnvEntryValue ) );
                }
                catch ( IllegalArgumentException e )
                {
                    throw new EarPluginException( "Invalid env-entry [" + envEntry + "]", e );
                }
            }
            return result;
        }
        catch ( InterpolationException e )
        {
            throw new EarPluginException( "Interpolation exception:", e );
        }

    }

    /**
     * Builds the ejb-ref based on the configuration.
     * 
     * @return a list of EjbRef object(s)
     * @throws EarPluginException if the configuration is invalid
     */
    private List<EjbRef> buildEjbEntries()
        throws EarPluginException
    {
        final List<EjbRef> result = new ArrayList<EjbRef>();
        if ( ejbRefs == null )
        {
            return result;
        }
        try
        {
            StringSearchInterpolator ssi = new StringSearchInterpolator();
            ValueSource vs = new MapBasedValueSource( project.getProperties() );
            ssi.addValueSource( vs );

            final PlexusConfiguration[] allEjbEntries = ejbRefs.getChildren( EjbRef.EJB_REF );

            for ( PlexusConfiguration ejbEntry : allEjbEntries )
            {
                // CHECKSTYLE_OFF: LineLength
                final String childDescription =
                    interpolate( ssi, ejbEntry.getChild( EnvEntry.DESCRIPTION ).getValue() );
                final String childEjbEntryName = interpolate( ssi, ejbEntry.getChild( EjbRef.EJB_NAME ).getValue() );
                final String childEjbEntryType = interpolate( ssi, ejbEntry.getChild( EjbRef.EJB_TYPE ).getValue() );
                final String childEjbLookupNameValue =
                    interpolate( ssi, ejbEntry.getChild( EjbRef.EJB_LOOKUP_NAME ).getValue() );
                // CHECKSTYLE_ON: LineLength

                try
                {
                    result.add( new EjbRef( childDescription, childEjbEntryName, childEjbEntryType,
                                            childEjbLookupNameValue ) );
                }
                catch ( IllegalArgumentException e )
                {
                    throw new EarPluginException( "Invalid ejb-ref [" + ejbEntry + "]", e );
                }
            }
            return result;
        }
        catch ( InterpolationException e )
        {
            throw new EarPluginException( "Interpolation exception:", e );
        }

    }

    /**
     * Returns the value to use for the <tt>library-directory</tt> element, based on the library directory mode.
     */
    private String getActualLibraryDirectory()
        throws EarPluginException
    {
        final String mode = libraryDirectoryMode == null ? DEFAULT : libraryDirectoryMode.toUpperCase();

        if ( DEFAULT.equals( mode ) )
        {
            return defaultLibBundleDir;
        }
        else if ( EMPTY.equals( mode ) )
        {
            return "";
        }
        else if ( NONE.equals( mode ) )
        {
            return null;
        }
        else
        {
            throw new EarPluginException( "Unsupported library directory mode [" + libraryDirectoryMode
                + "] Supported modes " + ( Arrays.asList( DEFAULT, EMPTY, NONE ) ) );
        }
    }
}