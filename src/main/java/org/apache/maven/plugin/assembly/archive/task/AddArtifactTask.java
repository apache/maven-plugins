package org.apache.maven.plugin.assembly.archive.task;

import java.io.File;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.archive.ArchiveCreationException;
import org.apache.maven.plugin.assembly.utils.TypeConversionUtils;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;

public class AddArtifactTask
    implements ArchiverTask
{

    private int directoryMode = -1;

    private int fileMode = -1;

    private boolean unpack = false;

    private List includes;

    private List excludes;

    private final String outputLocation;

    private final Artifact artifact;

    public AddArtifactTask( Artifact artifact, String outputLocation )
    {
        this.artifact = artifact;
        this.outputLocation = outputLocation;
    }

    public void execute( Archiver archiver, AssemblerConfigurationSource configSource )
        throws ArchiveCreationException
    {
        if ( unpack )
        {
            String[] includesArray = TypeConversionUtils.toStringArray( includes );
            String[] excludesArray = TypeConversionUtils.toStringArray( excludes );

            int oldDirMode = archiver.getDefaultDirectoryMode();
            int oldFileMode = archiver.getDefaultFileMode();

            try
            {
                if ( fileMode > -1 )
                {
                    archiver.setDefaultFileMode( fileMode );
                }

                if ( directoryMode > -1 )
                {
                    archiver.setDefaultDirectoryMode( directoryMode );
                }

                archiver.addArchivedFileSet( artifact.getFile(), outputLocation, includesArray, excludesArray );
            }
            catch ( ArchiverException e )
            {
                throw new ArchiveCreationException( "Error adding file-set for '" + artifact.getId() + "' to archive: "
                    + e.getMessage(), e );
            }
            finally
            {
                archiver.setDefaultDirectoryMode( oldDirMode );
                archiver.setDefaultFileMode( oldFileMode );
            }
        }
        else
        {
            try
            {
                if ( fileMode > -1 )
                {
                    File artifactFile = artifact.getFile();
                    
                    archiver.addFile( artifactFile, outputLocation, fileMode );
                }
                else
                {
                    archiver.addFile( artifact.getFile(), outputLocation );
                }
            }
            catch ( ArchiverException e )
            {
                throw new ArchiveCreationException( "Error adding file '" + artifact.getId() + "' to archive: "
                    + e.getMessage(), e );
            }
        }
    }

    public void setDirectoryMode( int directoryMode )
    {
        this.directoryMode = directoryMode;
    }

    public void setExcludes( List excludes )
    {
        this.excludes = excludes;
    }

    public void setFileMode( int fileMode )
    {
        this.fileMode = fileMode;
    }

    public void setIncludes( List includes )
    {
        this.includes = includes;
    }

    public void setUnpack( boolean unpack )
    {
        this.unpack = unpack;
    }

}
