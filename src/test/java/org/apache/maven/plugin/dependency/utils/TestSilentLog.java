package org.apache.maven.plugin.dependency.utils;

import junit.framework.TestCase;

import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.logging.Logger;

public class TestSilentLog
    extends TestCase
{

    public void testLog()
    {
        Log log = new SilentLog();
        String text = new String( "Text" );
        Throwable e = new RuntimeException();
        log.debug( text );
        log.debug( text, e );
        log.debug( e );
        log.info( text );
        log.info( text, e );
        log.info( e );
        log.warn( text );
        log.warn( text, e );
        log.warn( e );
        log.error( text );
        log.error( text, e );
        log.error( e );
        log.isDebugEnabled();
        log.isErrorEnabled();
        log.isWarnEnabled();
        log.isInfoEnabled();
    }

    public void testLogger()
    {
        Logger log = new SilentLog();
        String text = new String( "Text" );
        Throwable e = new RuntimeException();

        log.debug( text );
        log.debug( text, e );
        log.error( text );
        log.error( text, e );
        log.warn( text );
        log.warn( text, e );
        log.info(text);
        log.info(text,e);
        
        log.fatalError(text);
        log.fatalError(text,e);
        log.getChildLogger(text);
        log.getName();
        log.getThreshold();
        log.isDebugEnabled();
        log.isErrorEnabled();
        log.isFatalErrorEnabled();
        log.isInfoEnabled();
        log.isWarnEnabled();
    }

}
