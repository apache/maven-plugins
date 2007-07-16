package org.apache.maven.plugin.assembly.filter;

import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.components.io.fileselectors.FileInfo;
import org.codehaus.plexus.logging.LogEnabled;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.codehaus.plexus.util.IOUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class SimpleAggregatingDescriptorHandler
    implements ContainerDescriptorHandler, LogEnabled
{

    // component configuration.

    private String filePattern;

    private String outputPath;

    private String commentChars = "#";

    // calculated, temporary values.

    private boolean overrideFilterAction;

    private StringWriter aggregateWriter = new StringWriter();

    private List filenames = new ArrayList();

    // injected by the container.

    private Logger logger;

    public void finalizeArchiveCreation( Archiver archiver )
        throws ArchiverException
    {
        if ( outputPath.endsWith( "/" ) )
        {
            throw new ArchiverException(
                                         "Cannot write aggregated properties to a directory. You must specify a file name in the outputPath configuration for this handler. (handler: "
                                                         + getClass().getName() );
        }

        if ( outputPath.startsWith( "/" ) )
        {
            outputPath = outputPath.substring( 1 );
        }

        File temp = writePropertiesFile();

        overrideFilterAction = true;

        archiver.addFile( temp, outputPath );

        overrideFilterAction = false;
    }

    private File writePropertiesFile()
        throws ArchiverException
    {
        File f;

        Writer writer = null;
        try
        {
            f = File.createTempFile( "maven-assembly-plugin", "tmp" );
            f.deleteOnExit();

            writer = new FileWriter( f );

            writer.write( commentChars + " Aggregated on " + new Date() + " from: " );

            for ( Iterator it = filenames.iterator(); it.hasNext(); )
            {
                String filename = (String) it.next();

                writer.write( "\n" + commentChars + " " + filename );
            }

            writer.write( "\n\n" );

            writer.write( aggregateWriter.toString() );
        }
        catch ( IOException e )
        {
            throw new ArchiverException( "Error adding aggregated properties to finalize archive creation. Reason: "
                                         + e.getMessage(), e );
        }
        finally
        {
            IOUtil.close( writer );
        }

        return f;
    }

    public void finalizeArchiveExtraction( UnArchiver unarchiver )
        throws ArchiverException
    {
    }

    public List getVirtualFiles()
    {
        return Collections.singletonList( outputPath );
    }

    public boolean isSelected( FileInfo fileInfo )
        throws IOException
    {
        System.out.println( "\n\nChecking isSelected(..) for file with name: " + fileInfo.getName() + "\nin: " + getClass().getName() + "\n\n" );
        if ( overrideFilterAction )
        {
            return true;
        }

        String name = fileInfo.getName();

        if ( fileInfo.isFile() && name.matches( filePattern ) )
        {
            readProperties( fileInfo );
            filenames.add( name );

            return false;
        }

        return true;
    }

    private void readProperties( FileInfo fileInfo )
        throws IOException
    {
        StringWriter writer = new StringWriter();
        Reader reader = null;
        try
        {
            reader = new InputStreamReader( fileInfo.getContents() );

            IOUtil.copy( reader, writer );
        }
        finally
        {
            IOUtil.close( reader );
        }

        String content = writer.toString();

        aggregateWriter.write( "\n" );
        aggregateWriter.write( content );
    }

    protected final Logger getLogger()
    {
        if ( logger == null )
        {
            logger = new ConsoleLogger( Logger.LEVEL_INFO, "" );
        }

        return logger;
    }

    public void enableLogging( Logger logger )
    {
        this.logger = logger;
    }

    public String getPropertiesPattern()
    {
        return filePattern;
    }

    public void setPropertiesPattern( String propertiesPattern )
    {
        filePattern = propertiesPattern;
    }

    public String getOutputPath()
    {
        return outputPath;
    }

    public void setOutputPath( String outputPath )
    {
        this.outputPath = outputPath;
    }

}
