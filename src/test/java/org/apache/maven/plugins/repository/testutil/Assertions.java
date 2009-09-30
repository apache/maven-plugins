package org.apache.maven.plugins.repository.testutil;

import org.codehaus.plexus.archiver.zip.ZipFile;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import junit.framework.Assert;

public final class Assertions
{
    
    public static final Set<String> EMPTY_ENTRY_NAMES = Collections.emptySet();

    public static void assertZipContents( Iterable<String> requiredNames, Iterable<String> bannedNames, File bundleSource )
        throws IOException
    {
        ZipFile zf = new ZipFile( bundleSource );

        Set<String> missing = new HashSet<String>();
        for ( String name : requiredNames )
        {
            if ( zf.getEntry( name ) == null )
            {
                missing.add( name );
            }
        }

        Set<String> banned = new HashSet<String>();
        for ( String name : bannedNames )
        {
            if ( zf.getEntry( name ) != null )
            {
                banned.add( name );
            }
        }

        if ( !missing.isEmpty() || !banned.isEmpty() )
        {
            StringBuffer msg = new StringBuffer();
            msg.append( "The following REQUIRED entries were missing from the bundle archive:\n" );

            if ( missing.isEmpty() )
            {
                msg.append( "\nNone." );
            }
            else
            {
                for ( String name : missing )
                {
                    msg.append( "\n" ).append( name );
                }
            }

            msg.append( "\n\nThe following BANNED entries were present from the bundle archive:\n" );

            if ( banned.isEmpty() )
            {
                msg.append( "\nNone.\n" );
            }
            else
            {
                for ( String name : banned )
                {
                    msg.append( "\n" ).append( name );
                }
            }

            Assert.fail( msg.toString() );
        }
    }
}
