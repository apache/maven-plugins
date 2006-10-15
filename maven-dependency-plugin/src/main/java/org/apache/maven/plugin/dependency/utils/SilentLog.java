package org.apache.maven.plugin.dependency.utils;

import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.logging.Logger;

public class SilentLog
    implements Log, Logger
{

    public boolean isDebugEnabled()
    {
        return false;
    }

    public void debug( CharSequence content )
    {
    }

    public void debug( CharSequence content, Throwable error )
    {
    }

    public void debug( Throwable error )
    {
    }

    public boolean isInfoEnabled()
    {
        return false;
    }

    public void info( CharSequence content )
    {
    }

    public void info( CharSequence content, Throwable error )
    {
    }

    public void info( Throwable error )
    {
    }

    public boolean isWarnEnabled()
    {
        return false;
    }

    public void warn( CharSequence content )
    {
    }

    public void warn( CharSequence content, Throwable error )
    {
    }

    public void warn( Throwable error )
    {
    }

    public boolean isErrorEnabled()
    {
        return false;
    }

    public void error( CharSequence content )
    {
    }

    public void error( CharSequence content, Throwable error )
    {
    }

    public void error( Throwable error )
    {
    }

    public void debug( String message )
    {
        // TODO Auto-generated method stub
        
    }

    public void debug( String message, Throwable throwable )
    {
        // TODO Auto-generated method stub
        
    }

    public void info( String message )
    {
        // TODO Auto-generated method stub
        
    }

    public void info( String message, Throwable throwable )
    {
        // TODO Auto-generated method stub
        
    }

    public void warn( String message )
    {
        // TODO Auto-generated method stub
        
    }

    public void warn( String message, Throwable throwable )
    {
        // TODO Auto-generated method stub
        
    }

    public void error( String message )
    {
        // TODO Auto-generated method stub
        
    }

    public void error( String message, Throwable throwable )
    {
        // TODO Auto-generated method stub
        
    }

    public void fatalError( String message )
    {
        // TODO Auto-generated method stub
        
    }

    public void fatalError( String message, Throwable throwable )
    {
        // TODO Auto-generated method stub
        
    }

    public boolean isFatalErrorEnabled()
    {
        // TODO Auto-generated method stub
        return false;
    }

    public Logger getChildLogger( String name )
    {
        // TODO Auto-generated method stub
        return null;
    }

    public int getThreshold()
    {
        // TODO Auto-generated method stub
        return 0;
    }

    public String getName()
    {
        // TODO Auto-generated method stub
        return null;
    }
}
