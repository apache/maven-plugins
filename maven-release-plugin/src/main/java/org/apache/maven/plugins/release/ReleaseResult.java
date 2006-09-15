package org.apache.maven.plugins.release;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

/**
 * @author Edwin Punzalan
 */
public class ReleaseResult
{
    public static final int UNDEFINED = -1, SUCCESS = 0, ERROR = 1;

    private StringBuffer stdOut = new StringBuffer();

    private int resultCode = UNDEFINED;

    private long startTime;

    private long endTime;

    private final String LS = System.getProperty( "line.separator" );

    public void appendInfo( String message )
    {
        stdOut.append( "[INFO] " ).append( message ).append( LS );
    }

    public void appendWarn( String message )
    {
        stdOut.append( "[WARN] " ).append( message ).append( LS );
    }

    public void appendDebug( String message )
    {
        stdOut.append( "[DEBUG] " ).append( message ).append( LS );
    }

    public void appendDebug( String message, Exception e )
    {
        appendDebug( message );

        stdOut.append( getStackTrace( e ) ).append( LS );
    }

    public void appendError( String message )
    {
        stdOut.append( "[ERROR] " ).append( message ).append( LS );

        setResultCode( ERROR );
    }

    public void appendError( Exception e )
    {
        appendError( getStackTrace( e ) );
    }

    public void appendError( String message, Exception e )
    {
        appendError( message );

        stdOut.append( getStackTrace( e ) ).append( LS );
    }

    public void appendOutput( String message )
    {
        stdOut.append( message );
    }

    public String getOutput()
    {
        return stdOut.toString();
    }

    public int getResultCode()
    {
        return resultCode;
    }

    public void setResultCode( int resultCode )
    {
        this.resultCode = resultCode;
    }

    public long getStartTime()
    {
        return startTime;
    }

    public void setStartTime( long startTime )
    {
        this.startTime = startTime;
    }

    public long getEndTime()
    {
        return endTime;
    }

    public void setEndTime( long endTime )
    {
        this.endTime = endTime;
    }

    private String getStackTrace( Exception e )
    {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();

        PrintStream stream = new PrintStream( byteStream );

        e.printStackTrace( stream );

        stream.flush();

        return byteStream.toString();
    }

    public StringBuffer getOutputBuffer()
    {
        return stdOut;
    }
}
