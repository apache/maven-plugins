package org.apache.maven.plugin.assembly.archive;

import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.InvalidAssemblerConfigurationException;
import org.apache.maven.plugin.assembly.archive.archiver.PrefixingProxyArchiver;
import org.apache.maven.plugin.assembly.archive.phase.AssemblyArchiverPhase;
import org.apache.maven.plugin.assembly.filter.ComponentsXmlArchiverFileFilter;
import org.apache.maven.plugin.assembly.filter.ContainerDescriptorHandler;
import org.apache.maven.plugin.assembly.format.AssemblyFormattingException;
import org.apache.maven.plugin.assembly.model.Assembly;
import org.apache.maven.plugin.assembly.model.ContainerDescriptorHandlerConfig;
import org.apache.maven.plugin.assembly.utils.AssemblyFileUtils;
import org.apache.maven.plugin.assembly.utils.AssemblyFormatUtils;
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
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
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

//    /**
//     * @plexus.requirement
//     */
//    private ActiveCollectionManager collectionManager;

    private PlexusContainer container;

    public DefaultAssemblyArchiver()
    {
        // needed for plexus
    }

    // introduced for testing.
    public DefaultAssemblyArchiver( ArchiverManager archiverManager, ActiveCollectionManager collectionManager, List assemblyPhases )
    {
        this.archiverManager = archiverManager;
//        this.collectionManager = collectionManager;
        this.assemblyPhases = assemblyPhases;
    }

    public File createArchive( Assembly assembly, String fullName, String format,
                               AssemblerConfigurationSource configSource )
        throws ArchiveCreationException, AssemblyFormattingException, InvalidAssemblerConfigurationException
    {
        String filename = fullName + "." + format;

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
                                                                  finalName );
            }

            List containerHandlers = createContainerDescriptorHandlers( assembly.getContainerDescriptorHandlers() );

            Archiver archiver = createArchiver( format, assembly.isIncludeBaseDirectory(), basedir, configSource,
                                                containerHandlers );

            for ( Iterator phaseIterator = assemblyPhases.iterator(); phaseIterator.hasNext(); )
            {
                AssemblyArchiverPhase phase = (AssemblyArchiverPhase) phaseIterator.next();

                phase.execute( assembly, archiver, configSource );
            }

            archiver.setDestFile( destFile );

            archiver.createArchive();
        }
        catch ( ArchiverException e )
        {
            throw new ArchiveCreationException( "Error creating assembly archive: " + e.getMessage(), e );
        }
        catch ( IOException e )
        {
            throw new ArchiveCreationException( "Error creating assembly archive: " + e.getMessage(), e );
        }
        catch ( NoSuchArchiverException e )
        {
            throw new ArchiveCreationException( "Unable to obtain archiver for extension '" + format + "'" );
        }

        return destFile;
    }

    private List createContainerDescriptorHandlers( List containerDescriptorHandlers )
        throws InvalidAssemblerConfigurationException
    {
        if ( containerDescriptorHandlers == null )
        {
            containerDescriptorHandlers = new ArrayList();
        }

        List handlers = new ArrayList();
        boolean foundPlexus = false;

        if ( ( containerDescriptorHandlers != null ) && !containerDescriptorHandlers.isEmpty() )
        {
//            ActiveMap handlerMap = collectionManager.getActiveMap( ContainerDescriptorHandler.class );

            for ( Iterator it = containerDescriptorHandlers.iterator(); it.hasNext(); )
            {
                ContainerDescriptorHandlerConfig config = (ContainerDescriptorHandlerConfig) it.next();

                String hint = config.getHandlerName();
                ContainerDescriptorHandler handler;

                try
                {
                    handler = (ContainerDescriptorHandler) container.lookup( ContainerDescriptorHandler.class.getName(), hint );
//                    handler = (ContainerDescriptorHandler) handlerMap.checkedGet( hint );
                }
                catch ( ComponentLookupException e )
                {
                    getLogger().debug( "Failed to load container descriptor handler: " + hint, e );
                    throw new InvalidAssemblerConfigurationException( "containerDescriptorHandler: " + hint + " could not be loaded.", e );
                }

                System.out.println( "Loaded container descriptor handler with hint: " + hint + " (component: " + handler + ")" );

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
        if ( archiver instanceof JarArchiver )
        {
            extraSelectors = Collections.singletonList( new JarSecurityFileSelector() );
        }

        List extraFinalizers = null;
        if ( "jar".equals( format ) )
        {
            extraFinalizers = Collections.singletonList( new ManifestCreationFinalizer( configSource.getProject(),
                                                                                        configSource.getJarArchiveConfiguration() ) );

        }

        String prefix = "";
        if ( includeBaseDir )
        {
            prefix = finalName;
        }

        archiver = new PrefixingProxyArchiver( prefix, archiver, containerHandlers, extraSelectors, extraFinalizers );

        return archiver;
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

}
