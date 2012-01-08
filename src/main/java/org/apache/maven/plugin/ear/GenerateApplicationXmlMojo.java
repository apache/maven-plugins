package org.apache.maven.plugin.ear;

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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.ear.util.JavaEEVersion;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.PlexusConfigurationException;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates the EAR deployment descriptor file(s).
 *
 * @author <a href="snicoll@apache.org">Stephane Nicoll</a>
 * @version $Id$
 * @goal generate-application-xml
 * @phase generate-resources
 * @threadSafe
 * @requiresDependencyResolution test
 */
public class GenerateApplicationXmlMojo
    extends AbstractEarMojo
{


    /**
     * Whether the application.xml should be generated or not.
     *
     * @parameter default-value="true"
     */
    private Boolean generateApplicationXml = Boolean.TRUE;

    /**
     * Whether a module ID should be generated if none is specified.
     *
     * @parameter default-value="false"
     */
    private Boolean generateModuleId = Boolean.FALSE;

    /**
     * Application name of the application to be used when the application.xml
     * file is auto-generated. Since JavaEE6.
     *
     * @parameter
     */
    private String applicationName;

    /**
     * Display name of the application to be used when the application.xml
     * file is auto-generated.
     *
     * @parameter default-value="${project.artifactId}"
     */
    private String displayName;

    /**
     * Description of the application to be used when the application.xml
     * file is auto-generated.
     *
     * @parameter default-value="${project.description}"
     */
    private String description;

    /**
     * Defines the value of the initialize in order element to be used when
     * the application.xml file is auto-generated. When set to true, modules
     * must be initialized in the order they're listed in this deployment descriptor,
     * with the exception of application client modules, which can be
     * initialized in any order. If initialize-in-order is not set or set to
     * false, the order of initialization is unspecified and may be
     * product-dependent. Since JavaEE6.
     *
     * @parameter
     */
    private Boolean initializeInOrder;

    /**
     * The security-roles to be added to the auto-generated
     * application.xml file.
     *
     * @parameter
     */
    private PlexusConfiguration security;


    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        // Initializes ear modules
        super.execute();

        // Handle application.xml
        if ( !generateApplicationXml.booleanValue() )
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
            return;
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
            new ApplicationXmlWriterContext( descriptor, getModules(), buildSecurityRoles(), displayName, description,
                                             defaultLibBundleDir, applicationName, initializeInOrder );
        writer.write( context );
    }

    /**
     * Generates the jboss deployment descriptor.
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
        try
        {
            final PlexusConfiguration[] securityRoles = security.getChildren( SecurityRole.SECURITY_ROLE );

            for ( int i = 0; i < securityRoles.length; i++ )
            {
                PlexusConfiguration securityRole = securityRoles[i];
                final String id = securityRole.getAttribute( SecurityRole.ID_ATTRIBUTE );
                final String roleName = securityRole.getChild( SecurityRole.ROLE_NAME ).getValue();
                final String roleNameId =
                    securityRole.getChild( SecurityRole.ROLE_NAME ).getAttribute( SecurityRole.ID_ATTRIBUTE );
                final String description = securityRole.getChild( SecurityRole.DESCRIPTION ).getValue();
                final String descriptionId =
                    securityRole.getChild( SecurityRole.DESCRIPTION ).getAttribute( SecurityRole.ID_ATTRIBUTE );

                if ( roleName == null )
                {
                    throw new EarPluginException( "Invalid security-role configuration, role-name could not be null." );
                }
                else
                {
                    result.add( new SecurityRole( roleName, roleNameId, id, description, descriptionId ) );
                }
            }
            return result;
        }
        catch ( PlexusConfigurationException e )
        {
            throw new EarPluginException( "Invalid security-role configuration", e );
        }

    }
}