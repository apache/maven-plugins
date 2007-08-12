package org.apache.maven.plugin.war.util;

import com.thoughtworks.xstream.XStream;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Serializes {@link WebappStructure} back and forth.
 *
 * @author Stephane Nicoll
 */
public class WebappStructureSerializer
{

    private final XStream xStream;

    /**
     * Creates a new instance of the serializer.
     */
    public WebappStructureSerializer()
    {
        this.xStream = new XStream();

        // Register aliases
        xStream.alias( "webapp-structure", WebappStructure.class );
        xStream.alias( "path-set", PathSet.class );
    }


    /**
     * Reads the {@link WebappStructure} from the specified file.
     *
     * @param file the file containing the webapp structure
     * @return the webapp structure
     * @throws IOException if an error occured while reading the structure
     */
    public WebappStructure fromXml( File file )
        throws IOException
    {
        FileReader reader = null;

        try
        {
            reader = new FileReader( file );
            return (WebappStructure) xStream.fromXML( reader );
        }
        finally
        {
            if ( reader != null )
            {
                reader.close();
            }
        }
    }

    /**
     * Saves the {@link WebappStructure} to the specified file.
     *
     * @param webappStructure the structure to save
     * @param targetFile      the file to use to save the structure
     * @throws IOException if an error occured while saving the webapp structure
     */
    public void toXml( WebappStructure webappStructure, File targetFile )
        throws IOException
    {
        FileWriter writer = null;
        try
        {
            if ( !targetFile.getParentFile().exists() && !targetFile.getParentFile().mkdirs() )
            {
                throw new IOException(
                    "Could not create parent[" + targetFile.getParentFile().getAbsolutePath() + "]" );
            }

            if ( !targetFile.exists() && !targetFile.createNewFile() )
            {
                throw new IOException( "Could not create file[" + targetFile.getAbsolutePath() + "]" );
            }
            writer = new FileWriter( targetFile );
            xStream.toXML( webappStructure, writer );
        }
        finally
        {
            if ( writer != null )
            {
                writer.close();
            }
        }
    }
}
