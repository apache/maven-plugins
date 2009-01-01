package org.apache.maven.plugin.install;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;

import org.codehaus.plexus.digest.Digester;
import org.codehaus.plexus.digest.Md5Digester;
import org.codehaus.plexus.digest.Sha1Digester;
import org.codehaus.plexus.util.FileUtils;

/**
 * A utility class to assist testing.
 * 
 * @author Benjamin Bentmann
 * @version $Id$
 */
public class Utils
{

    /**
     * Verifies a checksum file in the local repo.
     * 
     * @param checksumFile The checksum file to verify, must not be <code>null</code>.
     * @throws Exception If the verification failed.
     */
    public static void verifyChecksum( File checksumFile )
        throws Exception
    {
        File dataFile;
        Digester digester;
        if ( checksumFile.getName().endsWith( ".md5" ) )
        {
            digester = new Md5Digester();
            dataFile = new File( checksumFile.getPath().substring( 0, checksumFile.getPath().length() - 4 ) );
        }
        else if ( checksumFile.getName().endsWith( ".sha1" ) )
        {
            digester = new Sha1Digester();
            dataFile = new File( checksumFile.getPath().substring( 0, checksumFile.getPath().length() - 5 ) );
        }
        else
        {
            throw new IllegalArgumentException( "Unsupported checksum file: " + checksumFile );
        }

        String expected = FileUtils.fileRead( checksumFile, "UTF-8" );
        digester.verify( dataFile, expected );
    }

}
