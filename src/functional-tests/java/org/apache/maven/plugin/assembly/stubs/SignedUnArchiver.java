package org.apache.maven.plugin.assembly.stubs;

import org.codehaus.plexus.archiver.ArchiverException;

import java.io.IOException;
import java.io.File;

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

/**
 * @author Edwin Punzalan
 */
public class SignedUnArchiver
    extends UnArchiverStub
{
    public void extract()
        throws ArchiverException, IOException
    {
        super.extract();

        File signatureDir = new File( getDestDirectory(), "META-INF" );
        signatureDir.mkdirs();

        File signatureFile = new File( signatureDir, "security-file.RSA" );
        signatureFile.createNewFile();

        signatureFile = new File( signatureDir, "security-file.DSA" );
        signatureFile.createNewFile();

        signatureFile = new File( signatureDir, "security-file.SF" );
        signatureFile.createNewFile();

        signatureFile = new File( signatureDir, "security-file.rsa" );
        signatureFile.createNewFile();

        signatureFile = new File( signatureDir, "security-file.dsa" );
        signatureFile.createNewFile();

        signatureFile = new File( signatureDir, "security-file.sf" );
        signatureFile.createNewFile();

        signatureFile = new File( signatureDir, "non-security-file" );
        signatureFile.createNewFile();
    }
}
