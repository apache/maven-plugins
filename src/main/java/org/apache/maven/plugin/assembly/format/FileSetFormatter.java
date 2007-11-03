package org.apache.maven.plugin.assembly.format;

import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.utils.AssemblyFileUtils;
import org.apache.maven.shared.model.fileset.FileSet;
import org.apache.maven.shared.model.fileset.util.FileSetManager;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;

public class FileSetFormatter
{

    private final AssemblerConfigurationSource configSource;

    private final Logger logger;

    public FileSetFormatter( AssemblerConfigurationSource configSource, Logger logger )
    {
        this.configSource = configSource;
        this.logger = logger;
    }

    public File formatFileSetForAssembly( File archiveBaseDir, org.apache.maven.plugin.assembly.model.FileSet set )
        throws AssemblyFormattingException, IOException
    {
        String lineEndingHint = set.getLineEnding();

        String lineEnding = AssemblyFileUtils.getLineEndingCharacters( lineEndingHint );

        if ( ( lineEnding != null ) || set.isFiltered() )
        {

            FileSet fileSet = new FileSet();
            fileSet.setLineEnding( lineEnding );
            fileSet.setDirectory( set.getDirectory() );
            fileSet.setIncludes( set.getIncludes() );

            fileSet.setExcludes( set.getExcludes() );
            fileSet.setUseDefaultExcludes( true );

            FileSetManager fsm = new FileSetManager( logger );
            String[] files = fsm.getIncludedFiles( fileSet );

            // if we don't have anything to process, let's just skip all of this mess.
            if ( ( files == null ) || ( files.length == 0 ) )
            {
                logger.info( "No files selected for line-ending conversion. Skipping: " + fileSet.getDirectory() );
            }
            else
            {
                File formattedDir =
                    FileUtils.createTempFile( "fileSetFormatter.", ".tmp", configSource.getTemporaryRootDirectory() );

                formattedDir.delete();
                formattedDir.mkdirs();

                FileFormatter fileFormatter = new FileFormatter( configSource, logger );
                for ( int i = 0; i < files.length; i++ )
                {
                    String file = files[i];

                    File targetFile = new File( formattedDir, file );

                    targetFile.getParentFile().mkdirs();

                    File sourceFile = new File( archiveBaseDir, file );
                    try
                    {
                        sourceFile = fileFormatter.format( sourceFile, set.isFiltered(), lineEndingHint, formattedDir );
                        AssemblyFileUtils.copyFile( sourceFile, targetFile );
                    }
                    catch ( AssemblyFormattingException e )
                    {
                        deleteDirectory( formattedDir );
                        throw e;
                    }
                    catch ( IOException e )
                    {
                        deleteDirectory( formattedDir );
                        throw e;
                    }
                }
                return formattedDir;
            }
        }

        return archiveBaseDir;
    }

    private static void deleteDirectory( File formattedDir )
    {
        try
                        {
                            FileUtils.deleteDirectory( formattedDir );
        }
        catch ( IOException e1 )
        {
            // ignore
        }
    }

}
