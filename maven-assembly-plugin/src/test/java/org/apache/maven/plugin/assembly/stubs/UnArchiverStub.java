package org.apache.maven.plugin.assembly.stubs;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
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

import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;

import java.io.File;
import java.io.IOException;

/**
 * @author Edwin Punzalan
 */
public class UnArchiverStub
    implements UnArchiver
{
    private File sourceFile, destDir;

    public void extract()
        throws ArchiverException, IOException
    {
        File extractedFile = new File( destDir, sourceFile.getName() + ".extracted" );

        if ( !extractedFile.exists() )
        {
            extractedFile.createNewFile();
        }
    }

    public File getDestDirectory()
    {
        return null;
    }

    public void setDestDirectory( File file )
    {
        destDir = file;
    }

    public File getDestFile()
    {
        return null;
    }

    public void setDestFile( File file )
    {
    }

    public File getSourceFile()
    {
        return sourceFile;
    }

    public void setSourceFile( File file )
    {
        this.sourceFile = file;
    }

    public void setOverwrite( boolean b )
    {
    }
//
//    public List getUnpackedFiles()
//    {
//        return unpackedFiles;
//    }
//
//    public class UnpackedArchive
//    {
//        private File sourceFile, destDirectory;
//
//        private UnpackedArchive( File sourceFile, File destDirectory )
//        {
//            this.sourceFile = sourceFile;
//            this.destDirectory = destDirectory;
//        }
//
//        public File getSourceFile()
//        {
//            return sourceFile;
//        }
//
//        public File getDestDirectory()
//        {
//            return destDirectory;
//        }
//
//        public boolean equals( Object obj )
//        {
//            boolean equal = false;
//
//            if ( obj instanceof UnpackedArchive )
//            {
//                UnpackedArchive unpacked = (UnpackedArchive) obj;
//                if ( unpacked.getSourceFile().equals( getSourceFile() ) &&
//                     unpacked.getDestDirectory().equals( getDestDirectory() ) )
//                {
//                    equal = true;
//                }
//            }
//
//            return equal;
//        }
//    }
}
