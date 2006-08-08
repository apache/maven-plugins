package org.apache.maven.plugin.invoker;

import org.apache.maven.shared.invoker.InvocationOutputHandler;
import org.codehaus.plexus.util.IOUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

public class FileLogger
    implements InvocationOutputHandler
{
    
    private PrintStream stream;
    
    private boolean shouldFinalize = true;
    
    public FileLogger( File outputFile )
        throws IOException
    {
        stream = new PrintStream( new FileOutputStream( outputFile  ) );
        
        Runnable finalizer = new Runnable()
        {
            public void run()
            {
                try
                {
                    finalize();
                }
                catch ( Throwable e )
                {
                }
            }
        };
        
        Runtime.getRuntime().addShutdownHook( new Thread( finalizer ) );
    }
    
    public PrintStream getPrintStream()
    {
        return stream;
    }

    public void consumeLine( String line )
    {
        stream.println( line );
        stream.flush();
    }
    
    public void close()
    {
        IOUtil.close( stream );
    }
    
    public void finalize()
    {
        if ( shouldFinalize )
        {
            close();
        }
    }
}
