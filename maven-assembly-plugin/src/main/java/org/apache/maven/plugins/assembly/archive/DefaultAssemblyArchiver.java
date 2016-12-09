package org.apache.maven.plugins.assembly.archive;

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
import org.apache.maven.plugins.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugins.assembly.InvalidAssemblerConfigurationException;
import org.apache.maven.plugins.assembly.archive.archiver.AssemblyProxyArchiver;
import org.apache.maven.plugins.assembly.archive.phase.AssemblyArchiverPhase;
import org.apache.maven.plugins.assembly.archive.phase.AssemblyArchiverPhaseComparator;
import org.apache.maven.plugins.assembly.artifact.DependencyResolutionException;
import org.apache.maven.plugins.assembly.filter.ComponentsXmlArchiverFileFilter;
import org.apache.maven.plugins.assembly.filter.ContainerDescriptorHandler;
import org.apache.maven.plugins.assembly.format.AssemblyFormattingException;
import org.apache.maven.plugins.assembly.interpolation.AssemblyExpressionEvaluator;
import org.apache.maven.plugins.assembly.model.Assembly;
import org.apache.maven.plugins.assembly.model.ContainerDescriptorHandlerConfig;
import org.apache.maven.plugins.assembly.utils.AssemblyFileUtils;
import org.apache.maven.plugins.assembly.utils.AssemblyFormatUtils;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.archiver.ArchiveFinalizer;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.diags.DryRunArchiver;
import org.codehaus.plexus.archiver.filters.JarSecurityFileSelector;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.archiver.tar.TarArchiver;
import org.codehaus.plexus.archiver.tar.TarLongFileMode;
import org.codehaus.plexus.archiver.war.WarArchiver;
import org.codehaus.plexus.archiver.zip.AbstractZipArchiver;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Controller component designed to organize the many activities involved in creating an assembly archive. This includes
 * locating and configuring {@link Archiver} instances, executing multiple {@link org.apache.maven.plugins.assembly
 * .archive.phase.AssemblyArchiverPhase} instances to
 * interpret the various sections of the assembly descriptor and determine which files to add, and other associated
 * activities.
 *
 * @version $Id$
 */
@Component( role = AssemblyArchiver.class, instantiationStrategy = "per-lookup" )
public class DefaultAssemblyArchiver
    extends AbstractLogEnabled
    implements AssemblyArchiver, Contextualizable
{

    @Requirement
    private ArchiverManager archiverManager;

    @Requirement( role = AssemblyArchiverPhase.class )
    private List<AssemblyArchiverPhase> assemblyPhases;

    @SuppressWarnings( "MismatchedQueryAndUpdateOfCollection" )
    @Requirement( role = ContainerDescriptorHandler.class )
    private Map<String, ContainerDescriptorHandler> containerDescriptorHandlers;

    private PlexusContainer container;

    @SuppressWarnings( "UnusedDeclaration" )
    public DefaultAssemblyArchiver()
    {
    }

    // introduced for testing.

    /**
     * @param archiverManager The archive manager.
     * @param assemblyPhases  The list of {@link AssemblyArchiverPhase}
     */
    protected DefaultAssemblyArchiver( final ArchiverManager archiverManager,
                                       final List<AssemblyArchiverPhase> assemblyPhases )
    {
        this.archiverManager = archiverManager;
        this.assemblyPhases = assemblyPhases;
    }

    private List<AssemblyArchiverPhase> sortedPhases()
    {
        List<AssemblyArchiverPhase> sorted = new ArrayList<AssemblyArchiverPhase>( assemblyPhases );
        Collections.sort( sorted, new AssemblyArchiverPhaseComparator() );
        return sorted;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public File createArchive( final Assembly assembly, final String fullName, final String format,
                               final AssemblerConfigurationSource configSource, boolean recompressZippedFiles,
                               String mergeManifestMode )
        throws ArchiveCreationException, AssemblyFormattingException, InvalidAssemblerConfigurationException
    {
        validate( assembly );

        String filename = fullName;
        if ( !configSource.isIgnoreDirFormatExtensions() || !format.startsWith( "dir" ) )
        {
            filename += "." + format;
        }

        AssemblyFileUtils.verifyTempDirectoryAvailability( configSource.getTemporaryRootDirectory() );

        final File outputDirectory = configSource.getOutputDirectory();

        final File destFile = new File( outputDirectory, filename );

        try
        {
            final String finalName = configSource.getFinalName();
            final String specifiedBasedir = assembly.getBaseDirectory();

            String basedir = finalName;

            if ( specifiedBasedir != null )
            {
                basedir = AssemblyFormatUtils.getOutputDirectory( specifiedBasedir, finalName, configSource,
                                                                  AssemblyFormatUtils.moduleProjectInterpolator(
                                                                      configSource.getProject() ),
                                                                  AssemblyFormatUtils.artifactProjectInterpolator(
                                                                      null ) );
            }

            final List<ContainerDescriptorHandler> containerHandlers =
                selectContainerDescriptorHandlers( assembly.getContainerDescriptorHandlers(), configSource );

            final Archiver archiver =
                createArchiver( format, assembly.isIncludeBaseDirectory(), basedir, configSource, containerHandlers,
                                recompressZippedFiles, mergeManifestMode );

            archiver.setDestFile( destFile );

            for ( AssemblyArchiverPhase phase : sortedPhases() )
            {
                phase.execute( assembly, archiver, configSource );
            }

            archiver.createArchive();
        }
        catch ( final ArchiverException e )
        {
            throw new ArchiveCreationException(
                "Error creating assembly archive " + assembly.getId() + ": " + e.getMessage(), e );
        }
        catch ( final IOException e )
        {
            throw new ArchiveCreationException(
                "Error creating assembly archive " + assembly.getId() + ": " + e.getMessage(), e );
        }
        catch ( final NoSuchArchiverException e )
        {
            throw new ArchiveCreationException(
                "Unable to obtain archiver for extension '" + format + "', for assembly: '" + assembly.getId() + "'",
                e );
        }
        catch ( final DependencyResolutionException e )
        {
            throw new ArchiveCreationException(
                "Unable to resolve dependencies for assembly '" + assembly.getId() + "'", e );
        }

        return destFile;
    }

    private void validate( final Assembly assembly )
        throws InvalidAssemblerConfigurationException
    {
        if ( assembly.getId() == null || assembly.getId().trim().length() < 1 )
        {
            throw new InvalidAssemblerConfigurationException( "Assembly ID must be present and non-empty." );
        }
    }

    // CHECKSTYLE_OFF: LineLength
    private List<ContainerDescriptorHandler> selectContainerDescriptorHandlers(
        List<ContainerDescriptorHandlerConfig> requestedContainerDescriptorHandlers,
        final AssemblerConfigurationSource configSource )
        throws InvalidAssemblerConfigurationException
    // CHECKSTYLE_ON: LineLength
    {
        getLogger().debug( "All known ContainerDescriptorHandler components: " + ( containerDescriptorHandlers == null
            ? "none; map is null."
            : "" + containerDescriptorHandlers.keySet() ) );

        if ( requestedContainerDescriptorHandlers == null )
        {
            requestedContainerDescriptorHandlers = new ArrayList<ContainerDescriptorHandlerConfig>();
        }

        final List<ContainerDescriptorHandler> handlers = new ArrayList<ContainerDescriptorHandler>();
        final List<String> hints = new ArrayList<String>();

        if ( !requestedContainerDescriptorHandlers.isEmpty() )
        {
            for ( final ContainerDescriptorHandlerConfig config : requestedContainerDescriptorHandlers )
            {
                final String hint = config.getHandlerName();
                final ContainerDescriptorHandler handler = containerDescriptorHandlers.get( hint );

                if ( handler == null )
                {
                    throw new InvalidAssemblerConfigurationException(
                        "Cannot find ContainerDescriptorHandler with hint: " + hint );
                }

                getLogger().debug(
                    "Found container descriptor handler with hint: " + hint + " (component: " + handler + ")" );

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
     * @param format                Archive format
     * @param includeBaseDir        the base directory for include.
     * @param finalName             The final name.
     * @param configSource          {@link AssemblerConfigurationSource}
     * @param containerHandlers     The list of {@link ContainerDescriptorHandler}
     * @param recompressZippedFiles recompress zipped files.
     * @param mergeManifestMode     how to handle already existing Manifest files
     * @return archiver Archiver generated
     * @throws org.codehaus.plexus.archiver.ArchiverException
     * @throws org.codehaus.plexus.archiver.manager.NoSuchArchiverException
     */
    protected Archiver createArchiver( final String format, final boolean includeBaseDir, final String finalName,
                                       final AssemblerConfigurationSource configSource,
                                       final List<ContainerDescriptorHandler> containerHandlers,
                                       boolean recompressZippedFiles, String mergeManifestMode )
        throws NoSuchArchiverException
    {
        Archiver archiver;
        if ( "txz".equals( format ) || "tgz".equals( format ) || "tbz2".equals( format ) || format.startsWith( "tar" ) )
        {
            archiver = createTarArchiver( format, TarLongFileMode.valueOf( configSource.getTarLongFileMode() ) );
        }
        else if ( "war".equals( format ) )
        {
            archiver = createWarArchiver();
        }
        else
        {
            archiver = archiverManager.getArchiver( format );
        }

        if ( archiver instanceof AbstractZipArchiver )
        {
            ( (AbstractZipArchiver) archiver ).setRecompressAddedZips( recompressZippedFiles );
        }

        final List<FileSelector> extraSelectors = new ArrayList<FileSelector>();
        final List<ArchiveFinalizer> extraFinalizers = new ArrayList<ArchiveFinalizer>();
        if ( archiver instanceof JarArchiver )
        {
            if ( mergeManifestMode != null )
            {
                ( (JarArchiver) archiver ).setFilesetmanifest(
                    JarArchiver.FilesetManifestConfig.valueOf( mergeManifestMode ) );
            }

            extraSelectors.add( new JarSecurityFileSelector() );

            extraFinalizers.add(
                new ManifestCreationFinalizer( configSource.getMavenSession(), configSource.getProject(),
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

        archiver = new AssemblyProxyArchiver( prefix, archiver, containerHandlers, extraSelectors, extraFinalizers,
                                              configSource.getWorkingDirectory(), getLogger() );
        if ( configSource.isDryRun() )
        {
            archiver = new DryRunArchiver( archiver, getLogger() );
        }

        archiver.setUseJvmChmod( configSource.isUpdateOnly() );
        archiver.setIgnorePermissions( configSource.isIgnorePermissions() );
        archiver.setForced( !configSource.isUpdateOnly() );

        return archiver;
    }

    private void configureContainerDescriptorHandler( final ContainerDescriptorHandler handler, final Xpp3Dom config,
                                                      final AssemblerConfigurationSource configSource )
        throws InvalidAssemblerConfigurationException
    {
        getLogger().debug( "Configuring handler: '" + handler.getClass().getName() + "' -->" );

        try
        {
            configureComponent( handler, config, configSource );
        }
        catch ( final ComponentConfigurationException e )
        {
            throw new InvalidAssemblerConfigurationException(
                "Failed to configure handler: " + handler.getClass().getName(), e );
        }
        catch ( final ComponentLookupException e )
        {
            throw new InvalidAssemblerConfigurationException(
                "Failed to lookup configurator for setup of handler: " + handler.getClass().getName(), e );
        }

        getLogger().debug( "-- end configuration --" );
    }

    private void configureArchiver( final Archiver archiver, final AssemblerConfigurationSource configSource )
    {
        Xpp3Dom config;
        try
        {
            config = Xpp3DomBuilder.build( new StringReader( configSource.getArchiverConfig() ) );
        }
        catch ( final XmlPullParserException e )
        {
            throw new ArchiverException( "Failed to parse archiver configuration for: " + archiver.getClass().getName(),
                                         e );
        }
        catch ( final IOException e )
        {
            throw new ArchiverException( "Failed to parse archiver configuration for: " + archiver.getClass().getName(),
                                         e );
        }

        getLogger().debug( "Configuring archiver: '" + archiver.getClass().getName() + "' -->" );

        try
        {
            configureComponent( archiver, config, configSource );
        }
        catch ( final ComponentConfigurationException e )
        {
            throw new ArchiverException( "Failed to configure archiver: " + archiver.getClass().getName(), e );
        }
        catch ( final ComponentLookupException e )
        {
            throw new ArchiverException(
                "Failed to lookup configurator for setup of archiver: " + archiver.getClass().getName(), e );
        }

        getLogger().debug( "-- end configuration --" );
    }

    private void configureComponent( final Object component, final Xpp3Dom config,
                                     final AssemblerConfigurationSource configSource )
        throws ComponentLookupException, ComponentConfigurationException
    {
        final ComponentConfigurator configurator = container.lookup( ComponentConfigurator.class, "basic" );

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
                ComponentConfigurator.class.getMethod( "configureComponent", Object.class, PlexusConfiguration.class,
                                                       ExpressionEvaluator.class, (Class<?>) containerRealm[1],
                                                       ConfigurationListener.class );

            configureComponent.invoke( configurator, component, configuration, expressionEvaluator, containerRealm[0],
                                       listener );
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
            final Method getContainerRealm = container.getClass().getMethod( "getContainerRealm" );
            return new Object[]{ getContainerRealm.invoke( container ), getContainerRealm.getReturnType() };
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

    protected Archiver createTarArchiver( final String format, final TarLongFileMode tarLongFileMode )
        throws NoSuchArchiverException
    {
        final TarArchiver tarArchiver = (TarArchiver) archiverManager.getArchiver( "tar" );
        final int index = format.indexOf( '.' );
        if ( index >= 0 )
        {
            TarArchiver.TarCompressionMethod tarCompressionMethod;
            // TODO: this should accept gz and bz2 as well so we can skip
            // TODO: over the switch
            final String compression = format.substring( index + 1 );
            if ( "gz".equals( compression ) )
            {
                tarCompressionMethod = TarArchiver.TarCompressionMethod.gzip;
            }
            else if ( "bz2".equals( compression ) )
            {
                tarCompressionMethod = TarArchiver.TarCompressionMethod.bzip2;
            }
            else if ( "xz".equals( compression ) )
            {
                tarCompressionMethod = TarArchiver.TarCompressionMethod.xz;
            }
            else if ( "snappy".equals( compression ) )
            {
                tarCompressionMethod = TarArchiver.TarCompressionMethod.snappy;
            }
            else
            {
                // TODO: better handling
                throw new IllegalArgumentException( "Unknown compression format: " + compression );
            }
            tarArchiver.setCompression( tarCompressionMethod );
        }
        else if ( "tgz".equals( format ) )
        {
            tarArchiver.setCompression( TarArchiver.TarCompressionMethod.gzip );
        }
        else if ( "tbz2".equals( format ) )
        {
            tarArchiver.setCompression( TarArchiver.TarCompressionMethod.bzip2 );
        }
        else if ( "txz".equals( format ) )
        {
            tarArchiver.setCompression( TarArchiver.TarCompressionMethod.xz );
        }

        tarArchiver.setLongfile( tarLongFileMode );

        return tarArchiver;
    }

    @Override
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
