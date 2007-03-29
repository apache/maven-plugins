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
import org.codehaus.plexus.logging.Logger;

public class AddArtifactTask
    implements ArchiverTask
{

    private String directoryMode;

    private String fileMode;

    private boolean unpack = false;

    private List includes;

    private List excludes;

    private final Artifact artifact;

    private MavenProject project;

    private String outputDirectory;

    private String outputFileNameMapping;

    private final Logger logger;

    public AddArtifactTask( Artifact artifact, Logger logger )
    {
        this.artifact = artifact;
        this.logger = logger;
    }

    public void execute( Archiver archiver, AssemblerConfigurationSource configSource )
        throws ArchiveCreationException, AssemblyFormattingException
    {
        String destDirectory = outputDirectory;

        destDirectory = AssemblyFormatUtils.getOutputDirectory( destDirectory, project, configSource.getFinalName() );

        String fileNameMapping = AssemblyFormatUtils.evaluateFileNameMapping( outputFileNameMapping, artifact );

        String outputLocation = destDirectory + fileNameMapping;

        if ( unpack )
        {
            if ( outputLocation.length() > 0 && !outputLocation.endsWith( "/" ) )
            {
                outputLocation += "/";
            }
            
            String[] includesArray = TypeConversionUtils.toStringArray( includes );
            String[] excludesArray = TypeConversionUtils.toStringArray( excludes );

            int oldDirMode = archiver.getDefaultDirectoryMode();
            int oldFileMode = archiver.getDefaultFileMode();

            try
            {
                if ( fileMode != null )
                {
                    archiver.setDefaultFileMode( TypeConversionUtils.modeToInt( fileMode, logger ) );
                }

                if ( directoryMode != null )
                {
                    archiver.setDefaultDirectoryMode( TypeConversionUtils.modeToInt( directoryMode, logger ) );
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
                if ( fileMode != null )
                {
                    File artifactFile = artifact.getFile();
                    
                    int mode = TypeConversionUtils.modeToInt( fileMode, logger );
                    
                    archiver.addFile( artifactFile, outputLocation, mode );
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

    public void setDirectoryMode( String directoryMode )
    {
        this.directoryMode = directoryMode;
    }

    public void setFileMode( String fileMode )
    {
        this.fileMode = fileMode;
    }

    public void setExcludes( List excludes )
    {
        this.excludes = excludes;
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

    public void setOutputDirectory( String outputDirectory, String defaultOutputDirectory )
    {
        setOutputDirectory( outputDirectory == null ? defaultOutputDirectory : outputDirectory );
    }

    public void setFileNameMapping( String outputFileNameMapping, String defaultOutputFileNameMapping )
    {
        setFileNameMapping( outputFileNameMapping == null ? defaultOutputFileNameMapping : outputFileNameMapping );
    }

}
