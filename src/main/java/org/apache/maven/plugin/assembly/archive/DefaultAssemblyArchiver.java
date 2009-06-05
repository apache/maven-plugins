package org.apache.maven.plugin.assembly.archive;

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

import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.plugin.DebugConfigurationListener;
import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.AssemblyContext;
import org.apache.maven.plugin.assembly.DefaultAssemblyContext;
import org.apache.maven.plugin.assembly.InvalidAssemblerConfigurationException;
import org.apache.maven.plugin.assembly.archive.archiver.AssemblyProxyArchiver;
import org.apache.maven.plugin.assembly.archive.phase.AssemblyArchiverPhase;
import org.apache.maven.plugin.assembly.artifact.DependencyResolver;
import org.apache.maven.plugin.assembly.filter.ComponentsXmlArchiverFileFilter;
import org.apache.maven.plugin.assembly.filter.ContainerDescriptorHandler;
import org.apache.maven.plugin.assembly.format.AssemblyFormattingException;
import org.apache.maven.plugin.assembly.interpolation.AssemblyExpressionEvaluator;
import org.apache.maven.plugin.assembly.model.Assembly;
import org.apache.maven.plugin.assembly.model.ContainerDescriptorHandlerConfig;
import org.apache.maven.plugin.assembly.utils.AssemblyFileUtils;
import org.apache.maven.plugin.assembly.utils.AssemblyFormatUtils;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.filters.JarSecurityFileSelector;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.archiver.tar.TarArchiver;
import org.codehaus.plexus.archiver.tar.TarLongFileMode;
import org.codehaus.plexus.archiver.war.WarArchiver;
import org.codehaus.plexus.collections.ActiveCollectionManager;
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.component.configurator.ComponentConfigurator;
import org.codehaus.plexus.component.configurator.ConfigurationListener;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Controller component designed to organize the many activities involved in 
 * creating an assembly archive. This includes locating and configuring {@link Archiver}
 * instances, executing multiple {@link AssemblyArchiverPhase} instances to 
 * interpret the various sections of the assembly descriptor and determine which
 * files to add, and other associated activities.
 * 
 * @version $Id$
 * @plexus.component role="org.apache.maven.plugin.assembly.archive.AssemblyArchiver" role-hint="default"
 */
public class DefaultAssemblyArchiver
    extends AbstractLogEnabled
    implements AssemblyArchiver, Contextualizable
{

    /**
     * @plexus.requirement
     */
    private ArchiverManager archiverManager;

    /**
     * @plexus.requirement role="org.apache.maven.plugin.assembly.archive.phase.AssemblyArchiverPhase"
     */
    private List assemblyPhases;

    /**
     * @plexus.requirement role="org.apache.maven.plugin.assembly.filter.ContainerDescriptorHandler"
     */
    private Map containerDescriptorHandlers;
    
    /**
     * @plexus.requirement
     */
    private DependencyResolver dependencyResolver;

    private PlexusContainer container;

    public DefaultAssemblyArchiver()
    {
        // needed for plexus
    }

    // introduced for testing.
    protected DefaultAssemblyArchiver( ArchiverManager archiverManager, ActiveCollectionManager collectionManager,
                                    DependencyResolver resolver, List assemblyPhases )
    {
        this.archiverManager = archiverManager;
        dependencyResolver = resolver;
        this.assemblyPhases = assemblyPhases;
    }
    
    /**
     * Create the assembly archive. Generally:
     * 
     * <ol>
     *   <li>Setup any directory structures for temporary files</li>
     *   <li>Calculate the output directory/file for the assembly</li>
     *   <li>Setup any handler components for special descriptor files we may encounter</li>
     *   <li>Lookup and configure the {@link Archiver} to be used</li>
     *   <li>Determine what, if any, dependency resolution will be required, and 
     *       resolve any dependency-version conflicts up front to produce a 
     *       managed-version map for the whole assembly process.</li>
     *   <li>Iterate through the available {@link AssemblyArchiverPhase} instances,
     *       executing each to handle a different top-level section of the
     *       assembly descriptor, if that section is present.</li>
     * </ol>
     */
    public File createArchive( Assembly assembly, String fullName, String format,
                               AssemblerConfigurationSource configSource )
        throws ArchiveCreationException, AssemblyFormattingException, InvalidAssemblerConfigurationException
    {
        String filename = fullName;
        if ( !configSource.isIgnoreDirFormatExtensions() || !format.startsWith( "dir" ) )
        {
            filename += "." + format;
        }

        AssemblyFileUtils.verifyTempDirectoryAvailability( configSource.getTemporaryRootDirectory(), getLogger() );

        File outputDirectory = configSource.getOutputDirectory();

        File destFile = new File( outputDirectory, filename );

        try
        {
            String finalName = configSource.getFinalName();
            String specifiedBasedir = assembly.getBaseDirectory();

            String basedir = finalName;

            if ( specifiedBasedir != null )
            {
                basedir = AssemblyFormatUtils.getOutputDirectory( specifiedBasedir, configSource.getProject(),
                                                                  null, finalName, configSource );
            }

            List containerHandlers = selectContainerDescriptorHandlers( assembly.getContainerDescriptorHandlers(), configSource );

            Archiver archiver = createArchiver( format, assembly.isIncludeBaseDirectory(), basedir, configSource,
                                                containerHandlers );

            archiver.setDestFile( destFile );
            
            Map managedVersionMap = dependencyResolver.buildManagedVersionMap( assembly, configSource );
            
            AssemblyContext context = new DefaultAssemblyContext().setManagedVersionMap( managedVersionMap );

            for ( Iterator phaseIterator = assemblyPhases.iterator(); phaseIterator.hasNext(); )
            {
                AssemblyArchiverPhase phase = (AssemblyArchiverPhase) phaseIterator.next();

                phase.execute( assembly, archiver, configSource, context );
            }

            archiver.createArchive();
        }
        catch ( ArchiverException e )
        {
            throw new ArchiveCreationException( "Error creating assembly archive " + assembly.getId() + ": " + e.getMessage(), e );
        }
        catch ( IOException e )
        {
            throw new ArchiveCreationException( "Error creating assembly archive " + assembly.getId() + ": " + e.getMessage(), e );
        }
        catch ( NoSuchArchiverException e )
        {
            throw new ArchiveCreationException( "Unable to obtain archiver for extension '" + format + "'" );
        }
        catch ( ArtifactResolutionException e )
        {
            throw new ArchiveCreationException( "Unable to create managed-version map for all assembly activities: " + e.getMessage(), e );
        }
        catch ( InvalidVersionSpecificationException e )
        {
            throw new ArchiveCreationException( "Unable to create managed-version map for all assembly activities: " + e.getMessage(), e );
        }
        catch ( InvalidDependencyVersionException e )
        {
            throw new ArchiveCreationException( "Unable to create managed-version map for all assembly activities: " + e.getMessage(), e );
        }

        return destFile;
    }

    private List selectContainerDescriptorHandlers( List requestedContainerDescriptorHandlers, AssemblerConfigurationSource configSource )
        throws InvalidAssemblerConfigurationException
    {
        getLogger().debug(
                           "All known ContainerDescritporHandler components: "
                                           + ( containerDescriptorHandlers == null ? "none; map is null."
                                                           : "" + containerDescriptorHandlers.keySet() ) );

        if ( requestedContainerDescriptorHandlers == null )
        {
            requestedContainerDescriptorHandlers = new ArrayList();
        }

        List handlers = new ArrayList();
        boolean foundPlexus = false;

        if ( ( requestedContainerDescriptorHandlers != null ) && !requestedContainerDescriptorHandlers.isEmpty() )
        {
            for ( Iterator it = requestedContainerDescriptorHandlers.iterator(); it.hasNext(); )
            {
                ContainerDescriptorHandlerConfig config = (ContainerDescriptorHandlerConfig) it.next();

                String hint = config.getHandlerName();
                ContainerDescriptorHandler handler = (ContainerDescriptorHandler) containerDescriptorHandlers.get( hint );

                if ( handler == null )
                {
                    throw new InvalidAssemblerConfigurationException( "Cannot find ContainerDescriptorHandler with hint: " + hint );
                }

                getLogger().debug( "Found container descriptor handler with hint: " + hint + " (component: " + handler + ")" );
                
                if ( config.getConfiguration() != null )
                {
                    getLogger().debug( "Configuring handler with:\n\n" + config.getConfiguration() + "\n\n" );
                    
                    configureContainerDescriptorHandler( handler, (Xpp3Dom) config.getConfiguration(), configSource );
                }
                
                handlers.add( handler );

                if ( "plexus".equals( hint ) )
                {
                    foundPlexus = true;
                }
            }
        }

        if ( !foundPlexus )
        {
            handlers.add( new ComponentsXmlArchiverFileFilter() );
        }

        return handlers;
    }

    /**
     * Creates the necessary archiver to build the distribution file.
     *
     * @param format
     *            Archive format
     * @param includeBaseDir
     * @param configSource
     * @param finalName
     * @param string
     * @return archiver Archiver generated
     * @throws org.codehaus.plexus.archiver.ArchiverException
     * @throws org.codehaus.plexus.archiver.manager.NoSuchArchiverException
     */
    protected Archiver createArchiver( String format, boolean includeBaseDir, String finalName,
                                       AssemblerConfigurationSource configSource,
                                       List containerHandlers )
        throws ArchiverException, NoSuchArchiverException
    {
        Archiver archiver;
        if ( format.startsWith( "tar" ) )
        {
            archiver = createTarArchiver( format, configSource.getTarLongFileMode() );
        }
        else if ( "war".equals( format ) )
        {
            archiver = createWarArchiver();
        }
        else
        {
            archiver = archiverManager.getArchiver( format );
        }

        List extraSelectors = null;
        List extraFinalizers = null;
        if ( archiver instanceof JarArchiver )
        {
            extraSelectors = Collections.singletonList( new JarSecurityFileSelector() );
            
            extraFinalizers = Collections.singletonList( new ManifestCreationFinalizer( configSource.getProject(),
                                                                                        configSource.getJarArchiveConfiguration() ) );

        }
        
        if ( configSource.getArchiverConfig() != null )
        {
            configureArchiver( archiver, configSource );
        }

        String prefix = "";
        if ( includeBaseDir )
        {
            prefix = finalName;
        }

        archiver = new AssemblyProxyArchiver( prefix, archiver, containerHandlers, extraSelectors, extraFinalizers, getLogger(), configSource.isDryRun() );

        return archiver;
    }
    
    private void configureContainerDescriptorHandler( ContainerDescriptorHandler handler, Xpp3Dom config,
                                                      AssemblerConfigurationSource configSource )
        throws InvalidAssemblerConfigurationException
    {
        ComponentConfigurator configurator;
        try
        {
            configurator = (ComponentConfigurator) container.lookup( ComponentConfigurator.ROLE, "basic" );
        }
        catch ( ComponentLookupException e )
        {
            throw new InvalidAssemblerConfigurationException( "Failed to lookup configurator component for setup of handler: " + handler.getClass().getName(), e );
        }
        
        XmlPlexusConfiguration configuration = new XmlPlexusConfiguration( config );
        
        ConfigurationListener listener = new DebugConfigurationListener( getLogger() );
        ExpressionEvaluator expressionEvaluator = new AssemblyExpressionEvaluator( configSource );

        getLogger().debug( "Configuring handler: '" + handler.getClass().getName() + "' -->" );
        
        try
        {
            configurator.configureComponent( handler, configuration, expressionEvaluator,
                                             container.getContainerRealm(), listener );
        }
        catch ( ComponentConfigurationException e )
        {
            throw new InvalidAssemblerConfigurationException( "Failed to configure handler: " + handler.getClass().getName(), e );
        }
        
        getLogger().debug( "-- end configuration --" );
    }

    private void configureArchiver( Archiver archiver, AssemblerConfigurationSource configSource )
        throws ArchiverException
    {
        ComponentConfigurator configurator;
        try
        {
            configurator = (ComponentConfigurator) container.lookup( ComponentConfigurator.ROLE, "basic" );
        }
        catch ( ComponentLookupException e )
        {
            throw new ArchiverException( "Failed to lookup configurator component for setup of archiver: " + archiver.getClass().getName(), e );
        }
        
        Xpp3Dom config;
        try
        {
            config = Xpp3DomBuilder.build( new StringReader( configSource.getArchiverConfig() ) );
        }
        catch ( XmlPullParserException e )
        {
            throw new ArchiverException( "Failed to parse archiver configuration for: " + archiver.getClass().getName(), e );
        }
        catch ( IOException e )
        {
            throw new ArchiverException( "Failed to parse archiver configuration for: " + archiver.getClass().getName(), e );
        }
        
        XmlPlexusConfiguration configuration = new XmlPlexusConfiguration( config );
        
        ConfigurationListener listener = new DebugConfigurationListener( getLogger() );
        ExpressionEvaluator expressionEvaluator = new AssemblyExpressionEvaluator( configSource );

        getLogger().debug( "Configuring archiver: '" + archiver.getClass().getName() + "' -->" );
        
        try
        {
            configurator.configureComponent( archiver, configuration, expressionEvaluator,
                                             container.getContainerRealm(), listener );
        }
        catch ( ComponentConfigurationException e )
        {
            throw new ArchiverException( "Failed to configure archiver: " + archiver.getClass().getName(), e );
        }
        
        getLogger().debug( "-- end configuration --" );
    }

    protected Archiver createWarArchiver()
        throws NoSuchArchiverException
    {
        WarArchiver warArchiver = (WarArchiver) archiverManager.getArchiver( "war" );
        warArchiver.setIgnoreWebxml( false ); // See MNG-1274

        return warArchiver;
    }

    protected Archiver createTarArchiver( String format, String tarLongFileMode )
        throws NoSuchArchiverException, ArchiverException
    {
        TarArchiver tarArchiver = (TarArchiver) archiverManager.getArchiver( "tar" );
        int index = format.indexOf( '.' );
        if ( index >= 0 )
        {
            // TODO: this needs a cleanup in plexus archiver - use a real
            // typesafe enum
            TarArchiver.TarCompressionMethod tarCompressionMethod = new TarArchiver.TarCompressionMethod();
            // TODO: this should accept gz and bz2 as well so we can skip
            // over the switch
            String compression = format.substring( index + 1 );
            if ( "gz".equals( compression ) )
            {
                tarCompressionMethod.setValue( "gzip" );
            }
            else if ( "bz2".equals( compression ) )
            {
                tarCompressionMethod.setValue( "bzip2" );
            }
            else
            {
                // TODO: better handling
                throw new IllegalArgumentException( "Unknown compression format: " + compression );
            }
            tarArchiver.setCompression( tarCompressionMethod );
        }

        TarLongFileMode tarFileMode = new TarLongFileMode();

        tarFileMode.setValue( tarLongFileMode );

        tarArchiver.setLongfile( tarFileMode );

        return tarArchiver;
    }

    public void contextualize( Context context )
        throws ContextException
    {
        container = (PlexusContainer) context.get( PlexusConstants.PLEXUS_KEY );
    }

    protected void setContainer( PlexusContainer container )
    {
        this.container = container;
    }

}
