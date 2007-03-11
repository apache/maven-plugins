package org.apache.maven.plugin.pmd;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.xml.pull.XmlPullParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * Fail the build if there were any CPD violations in the source code.
 *
 * @goal cpd-check
 * @phase verify
 * @execute goal="cpd"
 */
public class CpdViolationCheckMojo
    extends AbstractPmdViolationCheckMojo
{

    /**
     * Skip the CPD violation checks.  Most useful on the command line
     * via "-Dmaven.cpd.skip=true".
     *
     * @parameter expression="${maven.cpd.skip}" default-value="false"
     */
    private boolean skip;
    
    /**
     * @see org.apache.maven.plugin.AbstractMojo#execute()
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( !skip )
        {
            executeCheck( "cpd.xml", "duplication", "CPD duplication", 10 );
        }
    }
    
    /**
     * Formats the failure details and prints them as an INFO message
     * 
     * @param item
     */
    protected void printError( Map item, String severity )
    {
        String lines = (String) item.get( "lines" );
        
        
        StringBuffer buff = new StringBuffer( 100 );
        buff.append( "CPD " + severity + ": Found " );
        buff.append(lines).append(" lines of duplicated code at locations:");
        this.getLog().info( buff.toString() );
        
        buff.setLength(0);
        buff.append("    ");
        Map file = (Map)item.get( "file" );
        buff.append( file.get( "path" ) );
        buff.append(" line ").append( file.get("line") );
        this.getLog().info( buff.toString() );
        
        buff.setLength(0);
        buff.append("    ");
        file = (Map)item.get( "file1" );
        buff.append( file.get( "path" ) );
        buff.append(" line ").append( file.get("line") );
        this.getLog().info( buff.toString() );
    }

    protected Map getErrorDetails( XmlPullParser xpp )
        throws XmlPullParserException, IOException
    {
        int index = 0;
        int attributeCount = 0;
        HashMap msgs = new HashMap();

        attributeCount = xpp.getAttributeCount();
        while ( index < attributeCount )
        {
            msgs.put( xpp.getAttributeName( index ), xpp.getAttributeValue( index ) );

            index++;
        }

        int tp = xpp.next();
        while ( tp != XmlPullParser.END_TAG )
        {
            // get the tag's text
            switch ( tp ) 
            {
            case XmlPullParser.TEXT:
                msgs.put( "text", xpp.getText().trim() );
                break;
            case XmlPullParser.START_TAG:
                {
                    String nm = xpp.getName();
                    if ( msgs.containsKey( nm ) )
                    {
                        int cnt = 1;
                        while ( msgs.containsKey( nm + cnt) )
                        {
                            ++cnt;
                        }
                        nm = nm + cnt;
                    }
                    msgs.put( nm, getErrorDetails( xpp ) );
                    break;
                }
            }
            tp = xpp.next();
        }
        return msgs;
    }
}
