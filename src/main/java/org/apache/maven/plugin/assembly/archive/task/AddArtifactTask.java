package org.apache.maven.plugin.assembly.archive.task;

import java.io.File;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.archive.ArchiveCreationException;
import org.apache.maven.plugin.assembly.format.AssemblyFormattingException;
import org.apache.maven.plugin.assembly.utils.AssemblyFormatUtils;
import org.apache.maven.plugin.assembly.utils.TypeConversionUtils;
import org.apache.maven.project.MavenProject;
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

    private final Artifact artifact;

    private MavenProject project;

    private String outputDirectory;

    private String outputFileNameMapping;

    private boolean includeBaseDirectory;

    public AddArtifactTask( Artifact artifact )
    {
        this.artifact = artifact;
    }

    public void execute( Archiver archiver, AssemblerConfigurationSource configSource )
        throws ArchiveCreationException, AssemblyFormattingException
    {
        String destDirectory = outputDirectory;

        destDirectory = AssemblyFormatUtils.getOutputDirectory( destDirectory, project, configSource.getFinalName(),
                                                                includeBaseDirectory );

        String fileNameMapping = AssemblyFormatUtils.evaluateFileNameMapping( outputFileNameMapping, artifact );

        String outputLocation = destDirectory + fileNameMapping;

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

    public void setDirectoryMode( String rawDirectoryMode )
    {
        this.directoryMode = Integer.parseInt( rawDirectoryMode, 8 );
    }

    public void setExcludes( List excludes )
    {
        this.excludes = excludes;
    }

    public void setFileMode( String rawFileMode )
    {
        this.fileMode = Integer.parseInt( rawFileMode, 8 );
    }

    public void setIncludes( List includes )
    {
        this.includes = includes;
    }

    public void setUnpack( boolean unpack )
    {
        this.unpack = unpack;
    }

    public void setProject( MavenProject project )
    {
        this.project = project;
    }

    public void setOutputDirectory( String outputDirectory )
    {
        this.outputDirectory = outputDirectory;
    }

    public void setFileNameMapping( String outputFileNameMapping )
    {
        this.outputFileNameMapping = outputFileNameMapping;
    }

    public void setIncludeBaseDirectory( boolean includeBaseDirectory )
    {
        this.includeBaseDirectory = includeBaseDirectory;
    }

}
