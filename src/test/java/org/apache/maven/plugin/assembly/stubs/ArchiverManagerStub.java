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

import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.UnArchiver;

/**
 * @author Edwin Punzalan
 */
public class ArchiverManagerStub
    implements ArchiverManager
{
    public static ArchiverStub archiverStub;

    public static UnArchiverStub unArchiverStub;

    public ArchiverManagerStub()
    {
        archiverStub = null;

        unArchiverStub = null;
    }

    public Archiver getArchiver( String string )
        throws NoSuchArchiverException
    {
        if ( archiverStub == null )
        {
            archiverStub = new ArchiverStub();
        }

        return archiverStub;
    }

    public void setArchiver( ArchiverStub archiver )
    {
        archiverStub = archiver;
    }

    public UnArchiver getUnArchiver( String string )
        throws NoSuchArchiverException
    {
        if ( unArchiverStub == null )
        {
            unArchiverStub = new UnArchiverStub();
        }

        return unArchiverStub;
    }

    public void setUnArchiver( UnArchiverStub unArchiver )
    {
        unArchiverStub = unArchiver;
    }
}
