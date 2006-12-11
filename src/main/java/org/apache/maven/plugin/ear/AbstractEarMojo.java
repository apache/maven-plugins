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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.ear.util.ArtifactTypeMappingService;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.PlexusConfigurationException;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * A base class for EAR-processing related tasks.
 *
 * @author <a href="snicoll@apache.org">Stephane Nicoll</a>
 * @version $Id$
 */
public abstract class AbstractEarMojo
    extends AbstractMojo
{

    public static final String APPLICATION_XML_URI = "META-INF/application.xml";

    public static final String META_INF = "META-INF";

    public static final String UTF_8 = "UTF-8";

    /**
     * Character encoding for the auto-generated deployment file(s).
     *
     * @parameter
     */
    protected String encoding = UTF_8;

    /**
     * Directory where the deployment descriptor file(s) will be auto-generated.
     *
     * @parameter expression="${project.build.directory}"
     */
    protected String generatedDescriptorLocation;

    /**
     * The maven project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;

    /**
     * The ear modules configuration.
     *
     * @parameter
     */
    private EarModule[] modules;

    /**
     * The artifact type mappings.
     *
     * @parameter
     */
    protected PlexusConfiguration artifactTypeMappings;

    /**
     * The default bundle dir for libraries.
     *
     * @parameter alias="defaultJavaBundleDir"
     */
    private String defaultLibBundleDir;

    /**
     * The file name mapping to use for all dependencies included
     * in the EAR file.
     *
     * @parameter
     */
    private String fileNameMapping;

    /**
     * Directory that resources are copied to during the build.
     *
     * @parameter expression="${project.build.directory}/${project.build.finalName}"
     * @required
     */
    private File workDirectory;

    /**
     * The JBoss specific configuration.
     *
     * @parameter
     */
    private PlexusConfiguration jboss;


    private List earModules;

    private List allModules;

    private JbossConfiguration jbossConfiguration;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        getLog().debug( "Resolving artifact type mappings ..." );
        try
        {
            ArtifactTypeMappingService.getInstance().configure( artifactTypeMappings );
        }
        catch ( EarPluginException e )
        {
            throw new MojoExecutionException( "Failed to initialize artifact type mappings", e );
        }
        catch ( PlexusConfigurationException e )
        {
            throw new MojoExecutionException( "Invalid artifact type mappings configuration", e );
        }

        getLog().debug( "Initializing JBoss configuration if necessary ..." );
        try
        {
            initializeJbossConfiguration();
        }
        catch ( EarPluginException e )
        {
            throw new MojoExecutionException( "Failed to initialize JBoss configuration", e );
        }

        getLog().debug( "Initializing ear execution context" );
        EarExecutionContext.getInstance().initialize( defaultLibBundleDir, jbossConfiguration, fileNameMapping );

        getLog().debug( "Resolving ear modules ..." );
        allModules = new ArrayList();
        try
        {
            if ( modules != null && modules.length > 0 )
            {
                // Let's validate user-defined modules
                EarModule module = null;

                for ( int i = 0; i < modules.length; i++ )
                {
                    module = modules[i];
                    getLog().debug( "Resolving ear module[" + module + "]" );
                    module.resolveArtifact( project.getArtifacts() );
                    allModules.add( module );
                }
            }

            // Let's add other modules
            Set artifacts = project.getArtifacts();
            for ( Iterator iter = artifacts.iterator(); iter.hasNext(); )
            {
                Artifact artifact = (Artifact) iter.next();

                // If the artifact's type is POM, ignore and continue
                // since it's used for transitive deps only.
                if ( "pom".equals( artifact.getType() ) )
                {
                    continue;
                }

                // Artifact is not yet registered and it has neither test, nor a
                // provided scope, not is it optional
                ScopeArtifactFilter filter = new ScopeArtifactFilter( Artifact.SCOPE_RUNTIME );
                if ( !isArtifactRegistered( artifact, allModules ) && !artifact.isOptional() &&
                    filter.include( artifact ) )
                {
                    EarModule module = EarModuleFactory.newEarModule( artifact, defaultLibBundleDir );
                    allModules.add( module );
                }
            }
        }
        catch ( EarPluginException e )
        {
            throw new MojoExecutionException( "Failed to initialize ear modules", e );
        }

        // Now we have everything let's built modules which have not been excluded
        earModules = new ArrayList();
        for ( Iterator iter = allModules.iterator(); iter.hasNext(); )
        {
            EarModule earModule = (EarModule) iter.next();
            if ( earModule.isExcluded() )
            {
                getLog().debug( "Skipping ear module[" + earModule + "]" );
            }
            else
            {
                earModules.add( earModule );
            }
        }

    }

    protected List getModules()
    {
        if ( earModules == null )
        {
            throw new IllegalStateException( "Ear modules have not been initialized" );
        }
        return earModules;
    }

    protected MavenProject getProject()
    {
        return project;
    }

    protected File getWorkDirectory()
    {
        return workDirectory;
    }

    protected JbossConfiguration getJbossConfiguration()
    {
        return jbossConfiguration;
    }

    private static boolean isArtifactRegistered( Artifact a, List currentList )
    {
        Iterator i = currentList.iterator();
        while ( i.hasNext() )
        {
            EarModule em = (EarModule) i.next();
            if ( em.getArtifact().equals( a ) )
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Initializes the JBoss configuration.
     *
     * @throws EarPluginException if the configuration is invalid
     */
    private void initializeJbossConfiguration()
        throws EarPluginException
    {
        if ( jboss == null )
        {
            jbossConfiguration = null;
        }
        else
        {
            try
            {
                String version = jboss.getChild( JbossConfiguration.VERSION ).getValue();
                if ( version == null )
                {
                    getLog().info( "JBoss version not set, using JBoss 4 by default" );
                    version = JbossConfiguration.VERSION_4;
                }
                final String securityDomain = jboss.getChild( JbossConfiguration.SECURITY_DOMAIN ).getValue();
                final String unauthenticatedPrincipal =
                    jboss.getChild( JbossConfiguration.UNAUHTHENTICTED_PRINCIPAL ).getValue();
                final String jmxName = jboss.getChild( JbossConfiguration.JMX_NAME ).getValue();

                jbossConfiguration =
                    new JbossConfiguration( version, securityDomain, unauthenticatedPrincipal, jmxName );
            }
            catch ( PlexusConfigurationException e )
            {
                throw new EarPluginException( "Invalid JBoss configuration", e );
            }
        }
    }
}
