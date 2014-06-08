package org.apache.maven.plugin.assembly.utils;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.apache.maven.plugin.assembly.format.AssemblyFormattingException;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;

public class TypeConversionUtilsTest
    extends TestCase
{

    public void testModeToInt_InterpretAsOctalWithoutLeadingZero() throws AssemblyFormattingException
    {
        final int check = Integer.decode("0777");
        final int test = TypeConversionUtils.modeToInt( "777", new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ) );

        assertEquals( check, test );
    }

    public void testModeToInt_InterpretValuesWithLeadingZeroAsOctal() throws AssemblyFormattingException
    {
        final int check = Integer.decode("0777");
        final int test = TypeConversionUtils.modeToInt( "0777", new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ) );

        assertEquals( check, test );
    }

    public void testModeToInt_FailOnInvalidOctalValue()
    {
        try
        {
            TypeConversionUtils.modeToInt( "493", new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ) );

            fail( "'493' is an invalid mode and should trigger an exception." );
        }
        catch ( final AssemblyFormattingException e )
        {
            // expected.
        }
    }

    public void testVerifyModeSanity_WarnOnNonsensicalOctalValue_002()
    {
        final List<String> messages = new ArrayList<String>( 2 );
        messages.add( "World has write access, but user does not." );
        messages.add( "World has write access, but group does not." );

        checkFileModeSanity( "002", false, messages );
    }

    public void testVerifyModeSanity_WarnOnNonsensicalOctalValue_020()
    {
        final List<String> messages = new ArrayList<String>( 1 );
        messages.add( "Group has write access, but user does not." );

        checkFileModeSanity( "020", false, messages );
    }

    public void testVerifyModeSanity_ReturnTrueForValidOctalValue_775()
    {
        checkFileModeSanity( "775", true, null );
    }

    private void checkFileModeSanity( final String mode, final boolean isSane,
                                      final List<String> messagesToCheckIfInsane )
    {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final PrintStream ps = new PrintStream( baos );

        final PrintStream oldOut = System.out;
        final PrintStream oldErr = System.err;

        try
        {
            System.setOut( ps );
            System.setErr( ps );

            assertEquals( "Mode sanity should be: " + isSane, isSane,
                          TypeConversionUtils.verifyModeSanity( Integer.parseInt( mode, 8 ),
                                                                new ConsoleLogger( Logger.LEVEL_WARN, "test" ) ) );
        }
        finally
        {
            System.setOut( oldOut );
            System.setErr( oldErr );
        }

        if ( !isSane && messagesToCheckIfInsane != null && !messagesToCheckIfInsane.isEmpty() )
        {
            final String message = new String( baos.toByteArray() );

            for (final String checkMessage : messagesToCheckIfInsane) {
                assertTrue("\'" + checkMessage + "\' is not present in output.", message.contains(checkMessage));
            }
        }
    }

}
