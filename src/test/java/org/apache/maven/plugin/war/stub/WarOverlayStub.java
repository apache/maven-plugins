package org.apache.maven.plugin.war.stub;

import java.io.File;

/**
 * @author Stephane Nicoll
 */
public class WarOverlayStub
    extends AbstractArtifactStub
{


    private final String artifactId;

    private File file;

    public WarOverlayStub( String _basedir, String artifactId, File warFile )
    {
        super( _basedir );
        if ( artifactId == null )
        {
            throw new NullPointerException( "Id could not be null." );
        }
        if ( warFile == null )
        {
            throw new NullPointerException( "warFile could not be null." );

        }
        else if ( !warFile.exists() )
        {
            throw new IllegalStateException( "warFile[" + file.getAbsolutePath() + "] should exist." );
        }
        this.artifactId = artifactId;
        this.file = warFile;
    }

    public String getType()
    {
        return "war";
    }

    public String getArtifactId()
    {
        return artifactId;
    }

    public File getFile()
    {
        return file;
    }

}
