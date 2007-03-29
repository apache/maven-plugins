package org.apache.maven.plugin.assembly.utils;

import org.apache.maven.plugin.assembly.format.AssemblyFormattingException;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.codehaus.plexus.util.StringOutputStream;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;

public class TypeConversionUtilsTest
    extends TestCase
{

    public void testModeToInt_InterpretAsOctalWithoutLeadingZero()
        throws AssemblyFormattingException
    {
        int check = Integer.decode( "0777" ).intValue();
        int test = TypeConversionUtils.modeToInt( "777", new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ) );

        assertEquals( check, test );
    }

    public void testModeToInt_InterpretValuesWithLeadingZeroAsOctal()
        throws AssemblyFormattingException
    {
        int check = Integer.decode( "0777" ).intValue();
        int test = TypeConversionUtils.modeToInt( "0777", new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ) );

        assertEquals( check, test );
    }

    public void testModeToInt_FailOnInvalidOctalValue()
    {
        try
        {
            TypeConversionUtils.modeToInt( "493", new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ) );

            fail( "'493' is an invalid mode and should trigger an exception." );
        }
        catch ( AssemblyFormattingException e )
        {
            // expected.
        }
    }

    public void testVerifyModeSanity_WarnOnNonsensicalOctalValue_002()
    {
        List messages = new ArrayList( 2 );
        messages.add( "World has write access, but user does not." );
        messages.add( "World has write access, but group does not." );
        
        checkFileModeSanity( "002", false, messages );
    }
    
    public void testVerifyModeSanity_WarnOnNonsensicalOctalValue_020()
    {
        List messages = new ArrayList( 1 );
        messages.add( "Group has write access, but user does not." );
        
        checkFileModeSanity( "020", false, messages );
    }
    
    public void testVerifyModeSanity_ReturnTrueForValidOctalValue_775()
    {
        checkFileModeSanity( "775", true, Collections.EMPTY_LIST );
    }
    
    private void checkFileModeSanity( String mode, boolean isSane, List messagesToCheckIfInsane )
    {
        StringOutputStream sos = new StringOutputStream();
        PrintStream ps = new PrintStream( sos );
        
        PrintStream oldOut = System.out;
        PrintStream oldErr = System.err;
        
        try
        {
            System.setOut( ps );
            System.setErr( ps );
            
            assertEquals( "Mode sanity should be: " + isSane, isSane, TypeConversionUtils.verifyModeSanity( Integer.parseInt( mode, 8 ), new ConsoleLogger( Logger.LEVEL_WARN, "test" ) ) );
        }
        finally
        {
            System.setOut( oldOut );
            System.setErr( oldErr );
        }
        
        if ( !isSane && messagesToCheckIfInsane != null && !messagesToCheckIfInsane.isEmpty() )
        {
            String message = sos.toString();
            
            for ( Iterator it = messagesToCheckIfInsane.iterator(); it.hasNext(); )
            {
                String checkMessage = (String) it.next();
                
                assertTrue( "\'" + checkMessage + "\' is not present in output.", message.indexOf( checkMessage ) > -1 );
            }
        }
    }

}
