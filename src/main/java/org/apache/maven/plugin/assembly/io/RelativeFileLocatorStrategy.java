package org.apache.maven.plugin.assembly.io;

import org.apache.maven.shared.io.location.FileLocation;
import org.apache.maven.shared.io.location.Location;
import org.apache.maven.shared.io.location.LocatorStrategy;
import org.apache.maven.shared.io.logging.MessageHolder;

import java.io.File;

public class RelativeFileLocatorStrategy
    implements LocatorStrategy
{

    private File basedir;

    public RelativeFileLocatorStrategy( File basedir )
    {
        this.basedir = basedir;
    }

    public Location resolve( String locationSpecification, MessageHolder messageHolder )
    {
        File file = new File( basedir, locationSpecification );
        messageHolder.addInfoMessage( "Searching for file location: " + file.getAbsolutePath() );

        Location location = null;

        if ( file.exists() )
        {
            location = new FileLocation( file, locationSpecification );
        }
        else
        {
            messageHolder.addMessage( "File: " + file.getAbsolutePath() + " does not exist." );
        }

        return location;
    }

}
