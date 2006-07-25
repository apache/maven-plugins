package org.apache.maven.plugin.assembly.archive.task;

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
    
    private String outputLocation;
    
    private final Artifact artifact;
    
    public AddArtifactTask( Artifact artifact )
    {
        this.artifact = artifact;
    }

    public void execute( Archiver archiver, AssemblerConfigurationSource configSource )
        throws ArchiveCreationException
    {
        int oldDirMode = archiver.getDefaultDirectoryMode();
        int oldFileMode = archiver.getDefaultFileMode();
        
        if ( fileMode > -1 )
        {
            archiver.setDefaultFileMode( fileMode );
        }
        
        if ( directoryMode > -1 )
        {
            archiver.setDefaultDirectoryMode( directoryMode );
        }
        
        try
        {
            if ( unpack )
            {
                String[] includesArray = TypeConversionUtils.toStringArray( includes );
                String[] excludesArray = TypeConversionUtils.toStringArray( excludes );

                try
                {
                    archiver.addArchivedFileSet( artifact.getFile(), outputLocation, includesArray, excludesArray );
                }
                catch ( ArchiverException e )
                {
                    throw new ArchiveCreationException( "Error adding file-set for '" + artifact.getId()
                        + "' to archive: " + e.getMessage(), e );
                }
            }
            else
            {
                try
                {
                    archiver.addFile( artifact.getFile(), outputLocation );
                }
                catch ( ArchiverException e )
                {
                    throw new ArchiveCreationException( "Error adding file '" + artifact.getId() + "' to archive: "
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

    public void setOutputLocation( String outputLocation )
    {
        this.outputLocation = outputLocation;
    }

    public void setUnpack( boolean unpack )
    {
        this.unpack = unpack;
    }

}
