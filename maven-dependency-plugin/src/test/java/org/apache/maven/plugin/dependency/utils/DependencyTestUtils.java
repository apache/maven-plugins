package org.apache.maven.plugin.dependency.utils;

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.shared.model.fileset.FileSet;
import org.apache.maven.shared.model.fileset.util.FileSetManager;

public class DependencyTestUtils
{

    /**
     * Deletes a directory and its contents.
     * 
     * @param dir
     *            The base directory of the included and excluded files.
     * @throws IOException 
     * @throws MojoExecutionException
     *             When a directory failed to get deleted.
     */
    static public void removeDirectory( File dir ) throws IOException
    {
        if ( dir != null )
        {
            FileSetManager fileSetManager = new FileSetManager( new SilentLog(), false );

            FileSet fs = new FileSet();
            fs.setDirectory( dir.getPath() );
            fs.addInclude( "**/**" );
              fileSetManager.delete( fs );

        }
    }

}
