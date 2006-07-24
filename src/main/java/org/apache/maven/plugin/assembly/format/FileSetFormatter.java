package org.apache.maven.plugin.assembly.format;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.utils.AssemblyFileUtils;
import org.apache.maven.shared.model.fileset.FileSet;
import org.apache.maven.shared.model.fileset.util.FileSetManager;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;


public class FileSetFormatter
{
    
    private final AssemblerConfigurationSource configSource;

    private final Logger logger;

    public FileSetFormatter( AssemblerConfigurationSource configSource, Logger logger )
    {
        this.configSource = configSource;
        this.logger = logger;
    }

    public File formatFileSetForAssembly( File archiveBaseDir, org.apache.maven.plugins.assembly.model.FileSet set )
        throws AssemblyFormattingException, IOException
    {
        File formattedDir = archiveBaseDir;
        
        String lineEndingHint = set.getLineEnding();
        
        String lineEnding = AssemblyFileUtils.getLineEndingCharacters( lineEndingHint );

        if ( lineEnding != null )
        {
            FileSet fileSet = new FileSet();
            fileSet.setLineEnding( lineEnding );
            fileSet.setDirectory( set.getDirectory() );
            fileSet.setIncludes( set.getIncludes() );
            
            formattedDir = FileUtils.createTempFile( "fileSetFormatter.", ".tmp", configSource.getTemporaryRootDirectory() );
            
            formattedDir.delete();
            formattedDir.mkdirs();
            
            fileSet.setExcludes( set.getExcludes() );
            fileSet.setUseDefaultExcludes( true );
            
            FileSetManager fsm = new FileSetManager( logger );
            String[] files = fsm.getIncludedFiles( fileSet );
            
            // if we don't have anything to process, let's just skip all of this mess.
            if ( files == null || files.length == 0 )
            {
                logger.info( "No files selected for line-ending conversion. Skipping: " + fileSet.getDirectory() );
                
                formattedDir.delete();
                
                return archiveBaseDir;
            }
            
            for ( int i = 0; i < files.length; i++ )
            {
                String file = files[i];
                
                File targetFile = new File( formattedDir, file );

                targetFile.getParentFile().mkdirs();

                File sourceFile = new File( archiveBaseDir, file );
                
                Reader sourceReader = null;
                try
                {
                    sourceReader = new BufferedReader( new FileReader( sourceFile ) );
                    
                    AssemblyFileUtils.convertLineEndings( sourceReader, targetFile, lineEnding );
                }
                finally
                {
                    IOUtil.close( sourceReader );
                }
            }
        }

        return formattedDir;
    }

}
