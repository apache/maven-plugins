package org.apache.maven.plugin.assembly.archive;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.format.AssemblyFormattingException;
import org.apache.maven.plugin.assembly.format.FileSetFormatter;
import org.apache.maven.plugin.assembly.utils.AssemblyFormatUtils;
import org.apache.maven.plugins.assembly.model.FileSet;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.logging.Logger;

public final class ArchiveAssemblyUtils
{

    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    private ArchiveAssemblyUtils()
    {
    }

    public static void addFileSets( Archiver archiver, List fileSets, boolean includeBaseDirectory,
                                    AssemblerConfigurationSource configSource,
                                    Logger logger )
        throws ArchiveCreationException, AssemblyFormattingException
    {
        File archiveBaseDir = configSource.getArchiveBaseDirectory();

        if ( archiveBaseDir != null )
        {
            if ( !archiveBaseDir.exists() )
            {
                throw new ArchiveCreationException( "The archive base directory '" + archiveBaseDir.getAbsolutePath()
                    + "' does not exist" );
            }
            else if ( !archiveBaseDir.isDirectory() )
            {
                throw new ArchiveCreationException( "The archive base directory '" + archiveBaseDir.getAbsolutePath()
                    + "' exists, but it is not a directory" );
            }
        }

        File basedir = configSource.getBasedir();
        MavenProject project = configSource.getProject();

        FileSetFormatter fileSetFormatter = new FileSetFormatter( configSource, logger );

        for ( Iterator i = fileSets.iterator(); i.hasNext(); )
        {
            FileSet fileSet = (FileSet) i.next();
            String directory = fileSet.getDirectory();
            String output = fileSet.getOutputDirectory();

            if ( output == null )
            {
                output = directory;
            }

            if ( logger.isDebugEnabled() )
            {
                logger.debug( "FileSet[" + output + "]" + " dir perms: "
                    + Integer.toString( archiver.getDefaultDirectoryMode(), 8 ) + " file perms: "
                    + Integer.toString( archiver.getDefaultFileMode(), 8 )
                    + ( fileSet.getLineEnding() == null ? "" : " lineEndings: " + fileSet.getLineEnding() ) );
            }

            output = AssemblyFormatUtils.getOutputDirectory( output, project, configSource.getFinalName(),
                                                             includeBaseDirectory );

            if ( directory == null )
            {
                directory = basedir.getAbsolutePath();
            }

            File fileSetDir = null;

            if ( archiveBaseDir == null )
            {
                fileSetDir = new File( directory );
            }
            else
            {
                fileSetDir = new File( archiveBaseDir, directory );
            }

            logger.debug( "The archive base directory is '" + archiveBaseDir.getAbsolutePath() + "'" );

            if ( !fileSetDir.isAbsolute() )
            {
                fileSetDir = new File( basedir, directory );
            }

            if ( archiveBaseDir.exists() )
            {
                try
                {
                    archiveBaseDir = fileSetFormatter.formatFileSetForAssembly( archiveBaseDir, fileSet );
                }
                catch ( IOException e )
                {
                    throw new ArchiveCreationException( "Error fixing file-set line endings for assembly: "
                        + e.getMessage(), e );
                }

                logger.debug( "Archive base directory: '" + archiveBaseDir.getAbsolutePath() + "'" );

                int oldDirMode = archiver.getDefaultDirectoryMode();
                int oldFileMode = archiver.getDefaultFileMode();

                archiver.setDefaultDirectoryMode( Integer.parseInt( fileSet.getDirectoryMode(), 8 ) );
                archiver.setDefaultFileMode( Integer.parseInt( fileSet.getFileMode(), 8 ) );

                try
                {
                    ArchiveAssemblyUtils.addDirectory( archiver, archiveBaseDir, output, fileSet.getIncludes(), fileSet
                        .getExcludes() );
                }
                finally
                {
                    archiver.setDefaultDirectoryMode( oldDirMode );
                    archiver.setDefaultFileMode( oldFileMode );
                }
            }
        }
    }

    public static void addDirectory( Archiver archiver, File directory, String outputDirectory, List includes,
                                     List fileSetExcludes )
        throws ArchiveCreationException
    {
        if ( directory.exists() )
        {
            List excludes;
            if ( fileSetExcludes != null && !fileSetExcludes.isEmpty() )
            {
                excludes = new ArrayList( fileSetExcludes );
            }
            else
            {
                excludes = new ArrayList();
            }

            try
            {
                String[] includesArray = null;
                if ( includes != null && !includes.isEmpty() )
                {
                    includesArray = (String[]) includes.toArray( EMPTY_STRING_ARRAY );
                }

                // this one is guaranteed to be non-null by code above.
                String[] excludesArray = (String[]) excludes.toArray( EMPTY_STRING_ARRAY );

                archiver.addDirectory( directory, outputDirectory, includesArray, excludesArray );
            }
            catch ( ArchiverException e )
            {
                throw new ArchiveCreationException( "Error adding directory to archive: " + e.getMessage(), e );
            }
        }
    }

    /**
     * Unpacks the archive file.
     * 
     * @param source
     *            File to be unpacked.
     * @param destDir
     *            Location where to put the unpacked files.
     */
    public static void unpack( File source, File destDir, ArchiverManager archiverManager )
        throws ArchiveExpansionException, NoSuchArchiverException
    {
        try
        {
            UnArchiver unArchiver = archiverManager.getUnArchiver( source );

            unArchiver.setSourceFile( source );

            unArchiver.setDestDirectory( destDir );

            unArchiver.extract();
        }
        catch ( IOException e )
        {
            throw new ArchiveExpansionException( "Error unpacking file: " + source + "to: " + destDir, e );
        }
        catch ( ArchiverException e )
        {
            throw new ArchiveExpansionException( "Error unpacking file: " + source + "to: " + destDir, e );
        }
    }

}
