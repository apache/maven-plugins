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

import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;

import java.io.File;

/**
 * @author Edwin Punzalan
 */
public class ArchiverManagerStub
    implements ArchiverManager
{
    public static Archiver archiverStub;

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
            if( "dir".equals( string ) )
            {
                archiverStub = new DirectoryArchiverStub();
            }
            else if ( "tar".equals( string ) )
            {
                archiverStub = new TarArchiverStub();
            }
            else if ( "war".equals( string ) )
            {
                archiverStub = new WarArchiverStub();
            }
            else
            {
                archiverStub = new JarArchiverStub();
            }
        }

        return archiverStub;
    }

    public void setArchiver( JarArchiverStub archiver )
    {
        archiverStub = archiver;
    }

    public UnArchiver getUnArchiver( String string )
        throws NoSuchArchiverException
    {
        if ( unArchiverStub == null )
        {
            if ( "jar".equals( string ) )
            {
                unArchiverStub = new SignedUnArchiver();
            }
            else
            {
                unArchiverStub = new UnArchiverStub();
            }
        }

        return unArchiverStub;
    }

    public UnArchiver getUnArchiver( File file )
        throws NoSuchArchiverException
    {
        if ( unArchiverStub == null )
        {
            String filename = file.getName();

            unArchiverStub = (UnArchiverStub) getUnArchiver( filename.substring( filename.lastIndexOf( '.' ) + 1 ) );
        }

        return unArchiverStub;
    }    
    
    public Archiver getArchiver( File string )
        throws NoSuchArchiverException
    {
        if ( archiverStub == null )
        {
            archiverStub = new JarArchiverStub();
        }

        return archiverStub;
    }        
    public void setUnArchiver( UnArchiverStub unArchiver )
    {
        unArchiverStub = unArchiver;
    }
}
