package org.apache.maven.plugin.checkstyle;

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

import junit.framework.TestCase;
import com.puppycrawl.tools.checkstyle.api.SeverityLevel;
import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import com.puppycrawl.tools.checkstyle.api.LocalizedMessage;

import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * @author Edwin Punzalan
 */
public class CheckstyleResultsTest
    extends TestCase
{
    private CheckstyleResults results;

    protected void setUp()
        throws Exception
    {
        results = new CheckstyleResults();
    }

    public void testEmptyResults()
    {
        assertEquals( "test total files", 0, results.getFiles().size() );

        assertEquals( "test file count", 0, results.getFileCount() );

        assertEquals( "test zero file violations", 0, results.getFileViolations( "filename" ).size() );

        assertEquals( "test INFO severity count", 0, results.getSeverityCount( SeverityLevel.INFO ) );

        assertEquals( "test WARNING severity count", 0, results.getSeverityCount( SeverityLevel.WARNING ) );

        assertEquals( "test ERROR severity count", 0, results.getSeverityCount( SeverityLevel.ERROR ) );

        assertEquals( "test IGNORE severity count", 0, results.getSeverityCount( SeverityLevel.IGNORE ) );
    }

    public void testResults()
    {
        Map files = new HashMap();

        LocalizedMessage message = new LocalizedMessage( 0, 0, "", "", null, SeverityLevel.INFO, null, getClass() );
        AuditEvent event = new AuditEvent( this, "file1", message );
        files.put( "file1", Collections.singletonList( event ) );

        message = new LocalizedMessage( 0, 0, "", "", null, SeverityLevel.WARNING, null, getClass() );
        List events = new ArrayList();
        events.add( new AuditEvent( this, "file2", message ) );
        events.add( new AuditEvent( this, "file2", message ) );
        files.put( "file2", events );

        message = new LocalizedMessage( 0, 0, "", "", null, SeverityLevel.ERROR, null, getClass() );
        events = new ArrayList();
        events.add( new AuditEvent( this, "file3", message ) );
        events.add( new AuditEvent( this, "file3", message ) );
        events.add( new AuditEvent( this, "file3", message ) );
        files.put( "file3", events );

        message = new LocalizedMessage( 0, 0, "", "", null, SeverityLevel.IGNORE, null, getClass() );
        events = new ArrayList();
        events.add( new AuditEvent( this, "file4", message ) );
        events.add( new AuditEvent( this, "file4", message ) );
        events.add( new AuditEvent( this, "file4", message ) );
        events.add( new AuditEvent( this, "file4", message ) );
        files.put( "file4", events );

        results.setFiles( files );

        assertEquals( "test total files", 4, results.getFiles().size() );
        assertEquals( "test file count", 4, results.getFileCount() );

        assertEquals( "test file severities", 0, results.getSeverityCount( "file0", SeverityLevel.INFO ) );
        assertEquals( "test file severities", 0, results.getSeverityCount( "file0", SeverityLevel.WARNING ) );
        assertEquals( "test file severities", 0, results.getSeverityCount( "file0", SeverityLevel.ERROR ) );
        assertEquals( "test file severities", 0, results.getSeverityCount( "file0", SeverityLevel.IGNORE ) );

        assertEquals( "test file violations", 1, results.getFileViolations( "file1" ).size() );
        assertEquals( "test file severities", 1, results.getSeverityCount( "file1", SeverityLevel.INFO ) );
        assertEquals( "test file severities", 0, results.getSeverityCount( "file1", SeverityLevel.WARNING ) );
        assertEquals( "test file severities", 0, results.getSeverityCount( "file1", SeverityLevel.ERROR ) );
        assertEquals( "test file severities", 0, results.getSeverityCount( "file1", SeverityLevel.IGNORE ) );

        assertEquals( "test file violations", 2, results.getFileViolations( "file2" ).size() );
        assertEquals( "test file severities", 0, results.getSeverityCount( "file2", SeverityLevel.INFO ) );
        assertEquals( "test file severities", 2, results.getSeverityCount( "file2", SeverityLevel.WARNING ) );
        assertEquals( "test file severities", 0, results.getSeverityCount( "file2", SeverityLevel.ERROR ) );
        assertEquals( "test file severities", 0, results.getSeverityCount( "file2", SeverityLevel.IGNORE ) );

        assertEquals( "test file violations", 3, results.getFileViolations( "file3" ).size() );
        assertEquals( "test file severities", 0, results.getSeverityCount( "file3", SeverityLevel.INFO ) );
        assertEquals( "test file severities", 0, results.getSeverityCount( "file3", SeverityLevel.WARNING ) );
        assertEquals( "test file severities", 3, results.getSeverityCount( "file3", SeverityLevel.ERROR ) );
        assertEquals( "test file severities", 0, results.getSeverityCount( "file3", SeverityLevel.IGNORE ) );

        assertEquals( "test file violations", 4, results.getFileViolations( "file4" ).size() );
        assertEquals( "test file severities", 0, results.getSeverityCount( "file4", SeverityLevel.INFO ) );
        assertEquals( "test file severities", 0, results.getSeverityCount( "file4", SeverityLevel.WARNING ) );
        assertEquals( "test file severities", 0, results.getSeverityCount( "file4", SeverityLevel.ERROR ) );
        assertEquals( "test file severities", 4, results.getSeverityCount( "file4", SeverityLevel.IGNORE ) );

        assertEquals( "test INFO severity count", 1, results.getSeverityCount( SeverityLevel.INFO ) );
        assertEquals( "test WARNING severity count", 2, results.getSeverityCount( SeverityLevel.WARNING ) );
        assertEquals( "test ERROR severity count", 3, results.getSeverityCount( SeverityLevel.ERROR ) );
        assertEquals( "test IGNORE severity count", 4, results.getSeverityCount( SeverityLevel.IGNORE ) );

        results.setFileViolations( "file", Collections.EMPTY_LIST );
        assertEquals( "test file violations", 0, results.getFileViolations( "file" ).size() );
    }
}
