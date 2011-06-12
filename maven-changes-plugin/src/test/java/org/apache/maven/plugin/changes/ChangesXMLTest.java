package org.apache.maven.plugin.changes;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.changes.model.Action;
import org.apache.maven.plugins.changes.model.FixedIssue;
import org.apache.maven.plugins.changes.model.Release;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;

/**
 * @author <a href="mailto:olamy@apache.org">olamy</a>
 * @since 27 juil. 2008
 * @version $Id$
 */
public class ChangesXMLTest
    extends PlexusTestCase
{

    private static class MockLog
        implements Log
    {
        Logger consoleLogger;

        private MockLog()
        {
            consoleLogger = new ConsoleLogger( 1, "test" );
        }

        public void debug( CharSequence content )
        {
            consoleLogger.debug( content.toString() );
        }

        public void debug( Throwable error )
        {
            consoleLogger.debug( error.getMessage() );

        }

        public void debug( CharSequence content, Throwable error )
        {
            consoleLogger.debug( error.getMessage(), error );

        }

        public void error( CharSequence content )
        {
            consoleLogger.error( content.toString() );
        }

        public void error( Throwable error )
        {
            consoleLogger.error( error.getMessage() );
        }

        public void error( CharSequence content, Throwable error )
        {
            consoleLogger.error( error.getMessage(), error );

        }

        public void info( CharSequence content )
        {
            consoleLogger.info( content.toString() );
        }

        public void info( Throwable error )
        {
            consoleLogger.info( error.getMessage() );
        }

        public void info( CharSequence content, Throwable error )
        {
            consoleLogger.info( error.getMessage(), error );
        }

        public boolean isDebugEnabled()
        {
            return consoleLogger.isDebugEnabled();
        }

        public boolean isErrorEnabled()
        {
            return consoleLogger.isErrorEnabled();
        }

        public boolean isInfoEnabled()
        {
            return consoleLogger.isInfoEnabled();
        }

        public boolean isWarnEnabled()
        {
            return consoleLogger.isWarnEnabled();
        }

        public void warn( CharSequence content )
        {
            consoleLogger.warn( content.toString() );
        }

        public void warn( Throwable error )
        {
            consoleLogger.warn( error.getMessage() );

        }

        public void warn( CharSequence content, Throwable error )
        {
            consoleLogger.warn( content.toString(), error );

        }

    }

    public void testParseChangesFile()
        throws Exception
    {
        File changesFile = new File( getBasedir() + "/src/test/unit/changes.xml" );
        ChangesXML changesXML = new ChangesXML( changesFile, new MockLog() );
        assertNotNull( changesXML.getChangesDocument() );
        assertEquals( "Changes report Project", changesXML.getTitle() );

        List releases = changesXML.getReleaseList();
        assertEquals( 2, releases.size() );
        for ( Iterator iterator = releases.iterator(); iterator.hasNext(); )
        {
            Release release = (Release) iterator.next();
            if ( "1.0".equals( release.getVersion() ) )
            {
                Action action = (Action) release.getActions().get( 0 );
                assertEquals( 2, action.getFixedIssues().size() );
                assertEquals( "JIRA-XXX", ( (FixedIssue) action.getFixedIssues().get( 0 ) ).getIssue() );
                assertEquals( "JIRA-YYY", ( (FixedIssue) action.getFixedIssues().get( 1 ) ).getIssue() );
                assertEquals( 2, action.getDueTos().size() );
            }
        }
    }
    
}
