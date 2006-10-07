package org.apache.maven.plugin.dependency.stubs;

import java.io.File;
import java.io.IOException;
import java.net.URI;

public class StubMarkerFile
    extends File
{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public StubMarkerFile( String pathname )
    {
        super( pathname );
        // TODO Auto-generated constructor stub
    }

    public StubMarkerFile( URI uri )
    {
        super( uri );
        // TODO Auto-generated constructor stub
    }

    public StubMarkerFile( File parent, String child )
    {
        super( parent, child );
        // TODO Auto-generated constructor stub
    }

    public StubMarkerFile( String parent, String child )
    {
        super( parent, child );
        // TODO Auto-generated constructor stub
    }

    public boolean createNewFile()
        throws IOException
    {
        throw new IOException( "Intended Error" );
    }
}
