package org.apache.maven.plugin.assembly.stubs;

import java.io.File;

import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;

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
public class ArchiverManagerWithExceptionStub
    extends ArchiverManagerStub
{
    public UnArchiver getUnArchiver( String string )
        throws NoSuchArchiverException
    {
        throw new NoSuchArchiverException( "Expected exception" );
    }

    public Archiver getArchiver( String string )
        throws NoSuchArchiverException
    {
        throw new NoSuchArchiverException( "Expected exception" );
    }

    public UnArchiver getUnArchiver( File string )
        throws NoSuchArchiverException
    {
        throw new NoSuchArchiverException( "Expected exception" );
    }

    public Archiver getArchiver( File string )
        throws NoSuchArchiverException
    {
        throw new NoSuchArchiverException( "Expected exception" );
    }
}
