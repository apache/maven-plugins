package org.apache.maven.plugin.assembly.archive;

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.archive.phase.AssemblyArchiverPhase;
import org.apache.maven.plugin.assembly.filter.ComponentsXmlArchiverFileFilter;
import org.apache.maven.plugin.assembly.format.AssemblyFormattingException;
import org.apache.maven.plugins.assembly.model.Assembly;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.jar.Manifest;
import org.codehaus.plexus.archiver.jar.ManifestException;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.archiver.tar.TarArchiver;
import org.codehaus.plexus.archiver.tar.TarLongFileMode;
import org.codehaus.plexus.archiver.war.WarArchiver;
import org.codehaus.plexus.logging.AbstractLogEnabled;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

/**
 * @plexus.component role="org.apache.maven.plugin.assembly.archive.ArchiveCreator"
 *                   role-hint="default"
 */
public class DefaultAssemblyArchiver
    extends AbstractLogEnabled
    implements AssemblyArchiver
{

    /**
     * @plexus.requirement
     */
    private ArchiverManager archiverManager;

    /**
     * @plexus.requirement role="org.apache.maven.plugin.assembly.archive.phase.AssemblyArchiverPhase"
     */
    private List archiverPhases;

    public File createArchive( Assembly assembly, String fullName, String format,
                               AssemblerConfigurationSource configSource )
        throws ArchiveCreationException, AssemblyFormattingException
    {
        File destFile = null;

        String filename = fullName + "." + format;

        try
        {
            Archiver archiver = createArchiver( format, configSource.getTarLongFileMode() );

            destFile = createArchive( archiver, assembly, filename, configSource );
        }
        catch ( NoSuchArchiverException e )
        {
            throw new ArchiveCreationException( "Unable to obtain archiver for extension '" + format + "'" );
        }
        catch ( ArchiverException e )
        {
            throw new ArchiveCreationException( "Error creating assembly: " + e.getMessage(), e );
        }

        return destFile;
    }

    /**
     * Creates the necessary archiver to build the distribution file.
     * 
     * @param format
     *            Archive format
     * @param tarLongFileMode
     * @return archiver Archiver generated
     * @throws org.codehaus.plexus.archiver.ArchiverException
     * @throws org.codehaus.plexus.archiver.manager.NoSuchArchiverException
     */
    protected Archiver createArchiver( String format, String tarLongFileMode )
        throws ArchiverException, NoSuchArchiverException
    {
        Archiver archiver;
        if ( format.startsWith( "tar" ) )
        {
            TarArchiver tarArchiver = (TarArchiver) this.archiverManager.getArchiver( "tar" );
            archiver = tarArchiver;
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

                TarLongFileMode tarFileMode = new TarLongFileMode();

                tarFileMode.setValue( tarLongFileMode );

                tarArchiver.setLongfile( tarFileMode );
            }
        }
        else if ( "war".equals( format ) )
        {
            WarArchiver warArchiver = (WarArchiver) this.archiverManager.getArchiver( "war" );
            warArchiver.setIgnoreWebxml( false ); // See MNG-1274
            archiver = warArchiver;
        }
        else
        {
            archiver = this.archiverManager.getArchiver( format );
        }

        return archiver;
    }

    protected File createArchive( Archiver archiver, Assembly assembly, String filename,
                                  AssemblerConfigurationSource configSource )
        throws ArchiveCreationException, AssemblyFormattingException
    {
        ComponentsXmlArchiverFileFilter componentsXmlFilter = new ComponentsXmlArchiverFileFilter();

        for ( Iterator phaseIterator = archiverPhases.iterator(); phaseIterator.hasNext(); )
        {
            AssemblyArchiverPhase phase = (AssemblyArchiverPhase) phaseIterator.next();

            phase.execute( assembly, archiver, configSource, componentsXmlFilter );
        }

        MavenArchiveConfiguration archive = configSource.getJarArchiveConfiguration();

        try
        {
            componentsXmlFilter.addToArchive( archiver );
        }
        catch ( IOException e )
        {
            throw new ArchiveCreationException( "Error adding component descriptors to assembly archive: "
                + e.getMessage(), e );
        }
        catch ( ArchiverException e )
        {
            throw new ArchiveCreationException( "Error adding component descriptors to assembly archive: "
                + e.getMessage(), e );
        }

        File outputDirectory = configSource.getOutputDirectory();
        File destFile = new File( outputDirectory, filename );

        if ( archiver instanceof JarArchiver )
        {
            // TODO: I'd really prefer to rewrite MavenArchiver as either a
            // separate manifest creation utility (and to
            // create an include pom.properties etc into another archiver), or
            // an implementation of an archiver
            // (the first is preferable).
            MavenArchiver mavenArchiver = new MavenArchiver();

            if ( archive != null )
            {
                try
                {
                    Manifest manifest;
                    File manifestFile = archive.getManifestFile();

                    if ( manifestFile != null )
                    {
                        try
                        {
                            manifest = new Manifest( new FileReader( manifestFile ) );
                        }
                        catch ( FileNotFoundException e )
                        {
                            throw new ArchiveCreationException( "Manifest not found: " + e.getMessage() );
                        }
                        catch ( IOException e )
                        {
                            throw new ArchiveCreationException( "Error processing manifest: " + e.getMessage(), e );
                        }
                    }
                    else
                    {
                        manifest = mavenArchiver.getManifest( configSource.getProject(), archive.getManifest() );
                    }

                    if ( manifest != null )
                    {
                        JarArchiver jarArchiver = (JarArchiver) archiver;
                        jarArchiver.addConfiguredManifest( manifest );
                    }
                }
                catch ( ManifestException e )
                {
                    throw new ArchiveCreationException( "Error creating manifest: " + e.getMessage(), e );
                }
                catch ( DependencyResolutionRequiredException e )
                {
                    throw new ArchiveCreationException( "Dependencies were not resolved: " + e.getMessage(), e );
                }
            }
        }

        archiver.setDestFile( destFile );
        try
        {
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

        return destFile;
    }

}
