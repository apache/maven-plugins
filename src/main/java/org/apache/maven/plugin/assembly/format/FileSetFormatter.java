package org.apache.maven.plugin.assembly.format;

import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.utils.AssemblyFileUtils;
import org.apache.maven.shared.model.fileset.FileSet;
import org.apache.maven.shared.model.fileset.util.FileSetManager;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.List;


public class FileSetFormatter
{
    
    private static final String[] EMPTY_STRING_ARRAY = new String[0];

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
        
        if ( set.getLineEnding() != null )
        {
            FileSet fileSet = new FileSet();
            fileSet.setLineEnding( set.getLineEnding() );
            fileSet.setDirectory( set.getDirectory() );
            fileSet.setExcludes( set.getExcludes() );
            fileSet.setIncludes( set.getIncludes() );
            
            String lineEnding = AssemblyFileUtils.getLineEndingCharacters( fileSet.getLineEnding() );

            formattedDir = FileUtils.createTempFile( "", "", configSource.getTemporaryRootDirectory() );
            
            formattedDir.delete();
            formattedDir.mkdirs();
            
            String[] includes = (String[]) fileSet.getIncludes().toArray( EMPTY_STRING_ARRAY );
            if ( includes.length == 0 )
            {
                includes = null;
            }

            // TODO: default excludes should be in the archiver?
            List excludesList = fileSet.getExcludes();
            excludesList.addAll( FileUtils.getDefaultExcludesAsList() );
            
            fileSet.setExcludes( excludesList );
            
            FileSetManager fsm = new FileSetManager( logger );
            String[] files = fsm.getIncludedFiles( fileSet );
            
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
