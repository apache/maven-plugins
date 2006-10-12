package org.apache.maven.plugin.assembly.archive;

import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.InvalidAssemblerConfigurationException;
import org.apache.maven.plugin.assembly.archive.archiver.PrefixingProxyArchiver;
import org.apache.maven.plugin.assembly.archive.phase.AssemblyArchiverPhase;
import org.apache.maven.plugin.assembly.filter.ComponentsXmlArchiverFileFilter;
import org.apache.maven.plugin.assembly.format.AssemblyFormattingException;
import org.apache.maven.plugins.assembly.model.Assembly;
import org.codehaus.plexus.archiver.ArchiveFileFilter;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.FilterEnabled;
import org.codehaus.plexus.archiver.FinalizerEnabled;
import org.codehaus.plexus.archiver.filters.JarSecurityFileFilter;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.archiver.tar.TarArchiver;
import org.codehaus.plexus.archiver.tar.TarLongFileMode;
import org.codehaus.plexus.archiver.war.WarArchiver;
import org.codehaus.plexus.logging.AbstractLogEnabled;

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
    implements AssemblyArchiver
{

    private static final ArchiveFileFilter JAR_SECURITY_FILE_FILTER = new JarSecurityFileFilter();

    /**
     * @plexus.requirement
     */
    private ArchiverManager archiverManager;

    /**
     * @plexus.requirement role="org.apache.maven.plugin.assembly.archive.phase.AssemblyArchiverPhase"
     */
    private List assemblyPhases;
    
    public DefaultAssemblyArchiver()
    {
        // needed for plexus
    }
    
    // introduced for testing.
    public DefaultAssemblyArchiver( ArchiverManager archiverManager, List assemblyPhases )
    {
        this.archiverManager = archiverManager;
        this.assemblyPhases = assemblyPhases;
    }

    public File createArchive( Assembly assembly, String fullName, String format,
                               AssemblerConfigurationSource configSource )
        throws ArchiveCreationException, AssemblyFormattingException, InvalidAssemblerConfigurationException
    {
        String filename = fullName + "." + format;
        
        ComponentsXmlArchiverFileFilter componentsXmlFilter = new ComponentsXmlArchiverFileFilter();

        File outputDirectory = configSource.getOutputDirectory();

        File destFile = new File( outputDirectory, filename );

        try
        {
            Archiver archiver = createArchiver( format, assembly.isIncludeBaseDirectory(), configSource.getFinalName(), configSource.getTarLongFileMode(), componentsXmlFilter );
            System.out.println( "Archiver is: " + archiver );

            for ( Iterator phaseIterator = assemblyPhases.iterator(); phaseIterator.hasNext(); )
            {
                AssemblyArchiverPhase phase = ( AssemblyArchiverPhase ) phaseIterator.next();

                phase.execute( assembly, archiver, configSource );
            }

            archiver.setDestFile( destFile );

            System.out.println( "Creating archive with: " + archiver );
            
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

    /**
     * Creates the necessary archiver to build the distribution file.
     * 
     * @param format
     *            Archive format
     * @param includeBaseDir 
     * @param tarLongFileMode
     * @param finalName 
     * @return archiver Archiver generated
     * @throws org.codehaus.plexus.archiver.ArchiverException
     * @throws org.codehaus.plexus.archiver.manager.NoSuchArchiverException
     */
    protected Archiver createArchiver( String format, boolean includeBaseDir, String finalName,
                                       String tarLongFileMode, ComponentsXmlArchiverFileFilter componentsXmlFilter )
        throws ArchiverException, NoSuchArchiverException
    {
        Archiver archiver;
        if ( format.startsWith( "tar" ) )
        {
            archiver = createTarArchiver( format, tarLongFileMode );
        }
        else if ( "war".equals( format ) )
        {
            archiver = createWarArchiver( );
        }
        else
        {
            archiver = this.archiverManager.getArchiver( format );
        }

        configureArchiverFilters( archiver, componentsXmlFilter );

        configureArchiverFinalizers( archiver, componentsXmlFilter );
        
        if ( includeBaseDir )
        {
            archiver = new PrefixingProxyArchiver( finalName, archiver );
        }

        return archiver;
    }

    protected void configureArchiverFinalizers( Archiver archiver, ComponentsXmlArchiverFileFilter componentsXmlFilter )
    {
        if ( archiver instanceof FinalizerEnabled )
        {
            System.out.println( "Adding components.xml merge finalizer." );
            
            ( ( FinalizerEnabled ) archiver ).setArchiveFinalizers( Collections.singletonList( componentsXmlFilter ) );
        }
    }

    protected void configureArchiverFilters( Archiver archiver, ComponentsXmlArchiverFileFilter componentsXmlFilter )
    {
        /*
         * If the assembly is 'jar-with-dependencies', remove the security files in all dependencies that will prevent
         * the uberjar to execute. Please see MASSEMBLY-64 for details.
         */
        if ( archiver instanceof FilterEnabled )
        {
            List filters = new ArrayList();

            filters.add( componentsXmlFilter );

            if ( archiver instanceof JarArchiver )
            {
                filters.add( JAR_SECURITY_FILE_FILTER );
            }

            ( ( FilterEnabled ) archiver ).setArchiveFilters( filters );
        }
    }

    protected Archiver createWarArchiver()
        throws NoSuchArchiverException
    {
        WarArchiver warArchiver = ( WarArchiver ) this.archiverManager.getArchiver( "war" );
        warArchiver.setIgnoreWebxml( false ); // See MNG-1274

        return warArchiver;
    }

    protected Archiver createTarArchiver( String format, String tarLongFileMode )
        throws NoSuchArchiverException, ArchiverException
    {
        TarArchiver tarArchiver = ( TarArchiver ) this.archiverManager.getArchiver( "tar" );
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

}
