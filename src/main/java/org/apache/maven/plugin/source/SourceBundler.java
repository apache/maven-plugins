package org.apache.maven.plugin.source;

/*
 * Copyright 2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * Helper class that generates the jar file
 *
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @version $Id$
 */
public class SourceBundler
{
    private static final String[] DEFAULT_INCLUDES = new String[]{"**/*",};

    /**
     * Method to create an archive of the specified files
     *
     * @param outputFile        the destination file of the generated archive
     * @param sourceDirectories the directory where the files to be archived are located
     * @param archiver          the archiver object that will create the archive
     * @throws ArchiverException
     * @throws IOException
     */
    public void makeSourceBundle( File outputFile, File[] sourceDirectories, Archiver archiver )
        throws ArchiverException, IOException
    {
        String[] includes = DEFAULT_INCLUDES;

        for ( int i = 0; i < sourceDirectories.length; i++ )
        {
            if ( sourceDirectories[i].exists() )
            {
                archiver.addDirectory( sourceDirectories[i], includes, FileUtils.getDefaultExcludes() );
            }
        }

        archiver.setDestFile( outputFile );

        archiver.createArchive();
    }
}
