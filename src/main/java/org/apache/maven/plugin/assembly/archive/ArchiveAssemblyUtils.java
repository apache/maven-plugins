package org.apache.maven.plugin.assembly.archive;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.filter.ComponentsXmlArchiverFileFilter;
import org.apache.maven.plugin.assembly.format.AssemblyFormattingException;
import org.apache.maven.plugin.assembly.format.FileSetFormatter;
import org.apache.maven.plugin.assembly.utils.AssemblyFormatUtils;
import org.apache.maven.plugins.assembly.model.FileSet;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.model.fileset.util.FileSetManager;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class ArchiveAssemblyUtils
{

    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    private ArchiveAssemblyUtils()
    {
    }

    public static void addFileSets( Archiver archiver, List fileSets, boolean includeBaseDirectory,
                              AssemblerConfigurationSource configSource,
                              ComponentsXmlArchiverFileFilter componentsXmlFilter, Logger logger )
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
                logger.debug(
                    "FileSet[" + output + "]" + " dir perms: "
                        + Integer.toString( archiver.getDefaultDirectoryMode(), 8 ) + " file perms: "
                        + Integer.toString( archiver.getDefaultFileMode(), 8 )
                        + (fileSet.getLineEnding() == null ? "" : " lineEndings: " + fileSet.getLineEnding()) );
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
                        .getExcludes(), componentsXmlFilter );
                }
                finally
                {
                    archiver.setDefaultDirectoryMode( oldDirMode );
                    archiver.setDefaultFileMode( oldFileMode );
                }
            }
        }
    }

    public static void addArtifactToArchive( Artifact artifact, Archiver archiver, ArchiverManager archiverManager, String outputDirectory,
                                       String fileNameMapping, boolean unpack, int dirMode, int fileMode,
                                       AssemblerConfigurationSource configSource,
                                       ComponentsXmlArchiverFileFilter componentsXmlFilter, Logger logger )
        throws ArchiveCreationException
    {
        int oldDirMode = archiver.getDefaultDirectoryMode();
        int oldFileMode = archiver.getDefaultFileMode();

        try
        {
            if ( unpack )
            {
                // TODO: something like zipfileset in plexus-archiver
                // archiver.addJar( )

                File tempLocation = new File( configSource.getWorkingDirectory(), fileNameMapping );
                boolean process = false;
                if ( !tempLocation.exists() )
                {
                    tempLocation.mkdirs();
                    process = true;
                }
                else if ( artifact.getFile().lastModified() > tempLocation.lastModified() )
                {
                    process = true;
                }

                if ( process )
                {
                    try
                    {
                        unpack( artifact.getFile(), tempLocation, archiverManager );
                    }
                    catch ( NoSuchArchiverException e )
                    {
                        throw new ArchiveCreationException( "Unable to obtain unarchiver for file '"
                            + artifact.getFile() + "'" );
                    }
                    catch ( ArchiveExpansionException e )
                    {
                        throw new ArchiveCreationException( "Unable to expand archive: '" + artifact.getFile() + "'" );
                    }

                    /*
                     * If the assembly is 'jar-with-dependencies', remove the
                     * security files in all dependencies that will prevent the
                     * uberjar to execute. Please see MASSEMBLY-64 for details.
                     */
                    if ( archiver instanceof JarArchiver )
                    {
                        String[] securityFiles = { "*.RSA", "*.DSA", "*.SF", "*.rsa", "*.dsa", "*.sf" };
                        org.apache.maven.shared.model.fileset.FileSet securityFileSet = new org.apache.maven.shared.model.fileset.FileSet();
                        securityFileSet.setDirectory( tempLocation.getAbsolutePath() + "/META-INF/" );

                        for ( int sfsi = 0; sfsi < securityFiles.length; sfsi++ )
                        {
                            securityFileSet.addInclude( securityFiles[sfsi] );
                        }

                        FileSetManager fsm = new FileSetManager( logger );
                        try
                        {
                            fsm.delete( securityFileSet );
                        }
                        catch ( IOException e )
                        {
                            throw new ArchiveCreationException( "Failed to delete security files: " + e.getMessage(), e );
                        }
                    }
                }

                ArchiveAssemblyUtils.addDirectory( archiver, tempLocation, outputDirectory, null, FileUtils
                    .getDefaultExcludesAsList(), componentsXmlFilter );
            }
            else
            {
                try
                {
                    archiver.addFile( artifact.getFile(), outputDirectory + fileNameMapping );
                }
                catch ( ArchiverException e )
                {
                    throw new ArchiveCreationException( "Error adding file '" + artifact.getFile() + "' to archive: "
                        + e.getMessage(), e );
                }
            }
        }
        finally
        {
            archiver.setDefaultDirectoryMode( oldDirMode );
            archiver.setDefaultFileMode( oldFileMode );
        }
    }

    public static void addDirectory( Archiver archiver, File directory, String output, List includes,
                                     List fileSetExcludes, ComponentsXmlArchiverFileFilter componentsXmlFilter )
        throws ArchiveCreationException
    {
        // TODO Handle this in the archiver!
        List excludes = new ArrayList( fileSetExcludes );
        excludes.addAll( FileUtils.getDefaultExcludesAsList() );
        
        if ( directory.exists() )
        {
            List adaptedExcludes = excludes;

            // TODO: more robust set of filters on added files in the archiver
            File componentsXml = new File( directory, ComponentsXmlArchiverFileFilter.COMPONENTS_XML_PATH );
            if ( componentsXml.exists() )
            {
                try
                {
                    componentsXmlFilter.addComponentsXml( componentsXml );
                }
                catch ( IOException e )
                {
                    throw new ArchiveCreationException( "Error reading components.xml to merge: " + e.getMessage(), e );
                }
                catch ( XmlPullParserException e )
                {
                    throw new ArchiveCreationException( "Error reading components.xml to merge: " + e.getMessage(), e );
                }
                adaptedExcludes = new ArrayList( excludes );
                adaptedExcludes.add( ComponentsXmlArchiverFileFilter.COMPONENTS_XML_PATH );
            }

            try
            {
                String[] includesArray = (String[]) includes.toArray( EMPTY_STRING_ARRAY );
                String[] excludesArray = (String[]) adaptedExcludes.toArray( EMPTY_STRING_ARRAY );
                
                archiver.addDirectory( directory, output, includesArray, excludesArray );
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
     * @param file
     *            File to be unpacked.
     * @param location
     *            Location where to put the unpacked files.
     */
    public static void unpack( File file, File location, ArchiverManager archiverManager )
        throws ArchiveExpansionException, NoSuchArchiverException
    {
        try
        {
            UnArchiver unArchiver = archiverManager.getUnArchiver( file );

            unArchiver.setSourceFile( file );

            unArchiver.setDestDirectory( location );

            unArchiver.extract();
        }
        catch ( IOException e )
        {
            throw new ArchiveExpansionException( "Error unpacking file: " + file + "to: " + location, e );
        }
        catch ( ArchiverException e )
        {
            throw new ArchiveExpansionException( "Error unpacking file: " + file + "to: " + location, e );
        }
    }

}
