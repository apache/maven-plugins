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

import org.apache.maven.plugin.DebugConfigurationListener;
import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.AssemblyContext;
import org.apache.maven.plugin.assembly.DefaultAssemblyContext;
import org.apache.maven.plugin.assembly.InvalidAssemblerConfigurationException;
import org.apache.maven.plugin.assembly.archive.archiver.AssemblyProxyArchiver;
import org.apache.maven.plugin.assembly.archive.phase.AssemblyArchiverPhase;
import org.apache.maven.plugin.assembly.artifact.DependencyResolutionException;
import org.apache.maven.plugin.assembly.artifact.DependencyResolver;
import org.apache.maven.plugin.assembly.filter.ComponentsXmlArchiverFileFilter;
import org.apache.maven.plugin.assembly.filter.ContainerDescriptorHandler;
import org.apache.maven.plugin.assembly.format.AssemblyFormattingException;
import org.apache.maven.plugin.assembly.interpolation.AssemblyExpressionEvaluator;
import org.apache.maven.plugin.assembly.model.Assembly;
import org.apache.maven.plugin.assembly.model.ContainerDescriptorHandlerConfig;
import org.apache.maven.plugin.assembly.utils.AssemblyFileUtils;
import org.apache.maven.plugin.assembly.utils.AssemblyFormatUtils;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.archiver.ArchiveFinalizer;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.filters.JarSecurityFileSelector;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.archiver.tar.TarArchiver;
import org.codehaus.plexus.archiver.tar.TarLongFileMode;
import org.codehaus.plexus.archiver.war.WarArchiver;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.component.configurator.ComponentConfigurator;
import org.codehaus.plexus.component.configurator.ConfigurationListener;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.components.io.fileselectors.FileSelector;
import org.codehaus.plexus.configuration.PlexusConfiguration;
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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Controller component designed to organize the many activities involved in creating an assembly archive. This includes
 * locating and configuring {@link Archiver} instances, executing multiple {@link AssemblyArchiverPhase} instances to
 * interpret the various sections of the assembly descriptor and determine which files to add, and other associated
 * activities.
 * 
 * @version $Id$
 */
@Component( role = AssemblyArchiver.class )
public class DefaultAssemblyArchiver
    extends AbstractLogEnabled
    implements AssemblyArchiver, Contextualizable
{

    @Requirement
    private ArchiverManager archiverManager;

    @Requirement
    private DependencyResolver dependencyResolver;

    @Requirement( role = AssemblyArchiverPhase.class )
    private List<AssemblyArchiverPhase> assemblyPhases;

    @Requirement( role = ContainerDescriptorHandler.class )
    private Map<String, ContainerDescriptorHandler> containerDescriptorHandlers;

    private PlexusContainer container;

    public DefaultAssemblyArchiver()
    {
        // needed for plexus
    }

    // introduced for testing.
    protected DefaultAssemblyArchiver( final ArchiverManager archiverManager, final DependencyResolver resolver,
                                       final List<AssemblyArchiverPhase> assemblyPhases )
    {
        this.archiverManager = archiverManager;
        dependencyResolver = resolver;
        this.assemblyPhases = assemblyPhases;
    }

    /**
     * Create the assembly archive. Generally:
     * 
     * <ol>
     * <li>Setup any directory structures for temporary files</li>
     * <li>Calculate the output directory/file for the assembly</li>
     * <li>Setup any handler components for special descriptor files we may encounter</li>
     * <li>Lookup and configure the {@link Archiver} to be used</li>
     * <li>Determine what, if any, dependency resolution will be required, and resolve any dependency-version conflicts
     * up front to produce a managed-version map for the whole assembly process.</li>
     * <li>Iterate through the available {@link AssemblyArchiverPhase} instances, executing each to handle a different
     * top-level section of the assembly descriptor, if that section is present.</li>
     * </ol>
     */
    public File createArchive( final Assembly assembly, final String fullName, final String format,
                               final AssemblerConfigurationSource configSource, boolean useJvmChmod )
        throws ArchiveCreationException, AssemblyFormattingException, InvalidAssemblerConfigurationException
    {
        validate( assembly );
        
        String filename = fullName;
        if ( !configSource.isIgnoreDirFormatExtensions() || !format.startsWith( "dir" ) )
        {
            filename += "." + format;
        }

        AssemblyFileUtils.verifyTempDirectoryAvailability( configSource.getTemporaryRootDirectory(), getLogger() );

        final File outputDirectory = configSource.getOutputDirectory();

        final File destFile = new File( outputDirectory, filename );

        try
        {
            final String finalName = configSource.getFinalName();
            final String specifiedBasedir = assembly.getBaseDirectory();

            String basedir = finalName;

            if ( specifiedBasedir != null )
            {
                basedir =
                    AssemblyFormatUtils.getOutputDirectory( specifiedBasedir, configSource.getProject(), null,
                                                            finalName, configSource );
            }

            final List<ContainerDescriptorHandler> containerHandlers =
                selectContainerDescriptorHandlers( assembly.getContainerDescriptorHandlers(), configSource );

            final Archiver archiver =
                createArchiver( format, assembly.isIncludeBaseDirectory(), basedir, configSource, containerHandlers, useJvmChmod );

            archiver.setDestFile( destFile );

            final AssemblyContext context = new DefaultAssemblyContext();

            dependencyResolver.resolve( assembly, configSource, context );

            for ( final Iterator<AssemblyArchiverPhase> phaseIterator = assemblyPhases.iterator(); phaseIterator.hasNext(); )
            {
                final AssemblyArchiverPhase phase = phaseIterator.next();

                phase.execute( assembly, archiver, configSource, context );
            }

            archiver.createArchive();
        }
        catch ( final ArchiverException e )
        {
            throw new ArchiveCreationException( "Error creating assembly archive " + assembly.getId() + ": "
                            + e.getMessage(), e );
        }
        catch ( final IOException e )
        {
            throw new ArchiveCreationException( "Error creating assembly archive " + assembly.getId() + ": "
                            + e.getMessage(), e );
        }
        catch ( final NoSuchArchiverException e )
        {
            throw new ArchiveCreationException( "Unable to obtain archiver for extension '" + format
                            + "', for assembly: '" + assembly.getId() + "'", e );
        }
        catch ( final DependencyResolutionException e )
        {
            throw new ArchiveCreationException( "Unable to resolve dependencies for assembly '" + assembly.getId()
                            + "'", e );
        }

        return destFile;
    }

    private void validate( Assembly assembly )
        throws InvalidAssemblerConfigurationException
    {
        if ( assembly.getId() == null || assembly.getId().trim().length() < 1 )
        {
            throw new InvalidAssemblerConfigurationException( "Assembly ID must be present and non-empty." );
        }
    }

    private List<ContainerDescriptorHandler> selectContainerDescriptorHandlers( List<ContainerDescriptorHandlerConfig> requestedContainerDescriptorHandlers,
                                                                                final AssemblerConfigurationSource configSource )
        throws InvalidAssemblerConfigurationException
    {
        getLogger().debug( "All known ContainerDescritporHandler components: "
                                           + ( containerDescriptorHandlers == null ? "none; map is null." : ""
                                                           + containerDescriptorHandlers.keySet() ) );

        if ( requestedContainerDescriptorHandlers == null )
        {
            requestedContainerDescriptorHandlers = new ArrayList<ContainerDescriptorHandlerConfig>();
        }

        final List<ContainerDescriptorHandler> handlers = new ArrayList<ContainerDescriptorHandler>();
        final List<String> hints = new ArrayList<String>();

        if ( ( requestedContainerDescriptorHandlers != null ) && !requestedContainerDescriptorHandlers.isEmpty() )
        {
            for ( final Iterator<ContainerDescriptorHandlerConfig> it = requestedContainerDescriptorHandlers.iterator(); it.hasNext(); )
            {
                final ContainerDescriptorHandlerConfig config = it.next();

                final String hint = config.getHandlerName();
                final ContainerDescriptorHandler handler = containerDescriptorHandlers.get( hint );

                if ( handler == null )
                {
                    throw new InvalidAssemblerConfigurationException(
                                                                      "Cannot find ContainerDescriptorHandler with hint: "
                                                                                      + hint );
                }

                getLogger().debug( "Found container descriptor handler with hint: " + hint + " (component: " + handler
                                                   + ")" );

                if ( config.getConfiguration() != null )
                {
                    getLogger().debug( "Configuring handler with:\n\n" + config.getConfiguration() + "\n\n" );

                    configureContainerDescriptorHandler( handler, (Xpp3Dom) config.getConfiguration(), configSource );
                }

                handlers.add( handler );
                hints.add( hint );
            }
        }

        if ( !hints.contains( "plexus" ) )
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
     * @param finalName
     * @param configSource
     * @param containerHandlers
     * @return archiver Archiver generated
     * @throws org.codehaus.plexus.archiver.ArchiverException
     * @throws org.codehaus.plexus.archiver.manager.NoSuchArchiverException
     */
    protected Archiver createArchiver( final String format, final boolean includeBaseDir, final String finalName,
                                       final AssemblerConfigurationSource configSource,
                                       final List<ContainerDescriptorHandler> containerHandlers, boolean useJvmChmod )
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

        final List<FileSelector> extraSelectors = new ArrayList<FileSelector>();
        final List<ArchiveFinalizer> extraFinalizers = new ArrayList<ArchiveFinalizer>();
        if ( archiver instanceof JarArchiver )
        {
            extraSelectors.add( new JarSecurityFileSelector() );

            extraFinalizers.add( new ManifestCreationFinalizer( configSource.getProject(),
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

        archiver =
            new AssemblyProxyArchiver( prefix, archiver, containerHandlers, extraSelectors, extraFinalizers,
                                       configSource.getWorkingDirectory(), getLogger(), configSource.isDryRun() );

        archiver.setUseJvmChmod( useJvmChmod );
        archiver.setForced( !configSource.isUpdateOnly() );
        
        return archiver;
    }

    private void configureContainerDescriptorHandler( final ContainerDescriptorHandler handler, final Xpp3Dom config,
                                                      final AssemblerConfigurationSource configSource )
        throws InvalidAssemblerConfigurationException
    {
        getLogger().debug( "Configuring handler: '" + handler.getClass()
                                                             .getName() + "' -->" );

        try
        {
            configureComponent( handler, config, configSource );
        }
        catch ( final ComponentConfigurationException e )
        {
            throw new InvalidAssemblerConfigurationException( "Failed to configure handler: " + handler.getClass()
                                                                                                       .getName(), e );
        }
        catch ( final ComponentLookupException e )
        {
            throw new InvalidAssemblerConfigurationException( "Failed to lookup configurator for setup of handler: "
                            + handler.getClass()
                                     .getName(), e );
        }

        getLogger().debug( "-- end configuration --" );
    }

    private void configureArchiver( final Archiver archiver, final AssemblerConfigurationSource configSource )
        throws ArchiverException
    {
        Xpp3Dom config;
        try
        {
            config = Xpp3DomBuilder.build( new StringReader( configSource.getArchiverConfig() ) );
        }
        catch ( final XmlPullParserException e )
        {
            throw new ArchiverException( "Failed to parse archiver configuration for: " + archiver.getClass()
                                                                                                  .getName(), e );
        }
        catch ( final IOException e )
        {
            throw new ArchiverException( "Failed to parse archiver configuration for: " + archiver.getClass()
                                                                                                  .getName(), e );
        }

        getLogger().debug( "Configuring archiver: '" + archiver.getClass()
                                                               .getName() + "' -->" );

        try
        {
            configureComponent( archiver, config, configSource );
        }
        catch ( final ComponentConfigurationException e )
        {
            throw new ArchiverException( "Failed to configure archiver: " + archiver.getClass()
                                                                                    .getName(), e );
        }
        catch ( final ComponentLookupException e )
        {
            throw new ArchiverException( "Failed to lookup configurator for setup of archiver: " + archiver.getClass()
                                                                                                           .getName(),
                                         e );
        }

        getLogger().debug( "-- end configuration --" );
    }

    private void configureComponent( final Object component, final Xpp3Dom config,
                                     final AssemblerConfigurationSource configSource )
        throws ComponentLookupException, ComponentConfigurationException
    {
        final ComponentConfigurator configurator =
            (ComponentConfigurator) container.lookup( ComponentConfigurator.ROLE, "basic" );

        final ConfigurationListener listener = new DebugConfigurationListener( getLogger() );

        final ExpressionEvaluator expressionEvaluator = new AssemblyExpressionEvaluator( configSource );

        final XmlPlexusConfiguration configuration = new XmlPlexusConfiguration( config );

        final Object[] containerRealm = getContainerRealm();

        /*
         * NOTE: The signature of configureComponent() has changed in Maven 3.x, the reflection prevents a linkage error
         * and makes the code work with both Maven 2 and 3.
         */
        try
        {
            final Method configureComponent =
                ComponentConfigurator.class.getMethod( "configureComponent", new Class[] { Object.class,
                    PlexusConfiguration.class, ExpressionEvaluator.class, (Class<?>) containerRealm[1],
                    ConfigurationListener.class } );

            configureComponent.invoke( configurator, new Object[] { component, configuration, expressionEvaluator,
                containerRealm[0], listener } );
        }
        catch ( final NoSuchMethodException e )
        {
            throw new RuntimeException( e );
        }
        catch ( final IllegalAccessException e )
        {
            throw new RuntimeException( e );
        }
        catch ( final InvocationTargetException e )
        {
            if ( e.getCause() instanceof ComponentConfigurationException )
            {
                throw (ComponentConfigurationException) e.getCause();
            }
            throw new RuntimeException( e.getCause() );
        }
    }

    private Object[] getContainerRealm()
    {
        /*
         * NOTE: The return type of getContainerRealm() has changed in Maven 3.x, the reflection prevents a linkage
         * error and makes the code work with both Maven 2 and 3.
         */
        try
        {
            final Method getContainerRealm = container.getClass()
                                                      .getMethod( "getContainerRealm" );
            return new Object[] { getContainerRealm.invoke( container ), getContainerRealm.getReturnType() };
        }
        catch ( final NoSuchMethodException e )
        {
            throw new RuntimeException( e );
        }
        catch ( final IllegalAccessException e )
        {
            throw new RuntimeException( e );
        }
        catch ( final InvocationTargetException e )
        {
            throw new RuntimeException( e.getCause() );
        }
    }

    protected Archiver createWarArchiver()
        throws NoSuchArchiverException
    {
        final WarArchiver warArchiver = (WarArchiver) archiverManager.getArchiver( "war" );
        warArchiver.setIgnoreWebxml( false ); // See MNG-1274

        return warArchiver;
    }

    protected Archiver createTarArchiver( final String format, final String tarLongFileMode )
        throws NoSuchArchiverException, ArchiverException
    {
        final TarArchiver tarArchiver = (TarArchiver) archiverManager.getArchiver( "tar" );
        final int index = format.indexOf( '.' );
        if ( index >= 0 )
        {
            // TODO: this needs a cleanup in plexus archiver - use a real
            // typesafe enum
            final TarArchiver.TarCompressionMethod tarCompressionMethod = new TarArchiver.TarCompressionMethod();
            // TODO: this should accept gz and bz2 as well so we can skip
            // over the switch
            final String compression = format.substring( index + 1 );
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

        final TarLongFileMode tarFileMode = new TarLongFileMode();

        tarFileMode.setValue( tarLongFileMode );

        tarArchiver.setLongfile( tarFileMode );

        return tarArchiver;
    }

    public void contextualize( final Context context )
        throws ContextException
    {
        container = (PlexusContainer) context.get( PlexusConstants.PLEXUS_KEY );
    }

    protected void setContainer( final PlexusContainer container )
    {
        this.container = container;
    }

}
