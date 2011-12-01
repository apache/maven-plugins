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
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import com.puppycrawl.tools.checkstyle.api.AuditListener;
import com.puppycrawl.tools.checkstyle.api.AutomaticBean;
import com.puppycrawl.tools.checkstyle.api.Configuration;
import com.puppycrawl.tools.checkstyle.api.SeverityLevel;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Listener in charge of receiving events from the Checker.
 *
 * @author <a href="mailto:evenisse@apache.org">Emmanuel Venisse</a>
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 */
public class CheckstyleReportListener
    extends AutomaticBean
    implements AuditListener
{
    private List<File> sourceDirectories;

    private CheckstyleResults results;

    private String currentFile;

    private List<AuditEvent> events;

    private SeverityLevel severityLevel;

    private Configuration checkstyleConfiguration;

    /**
     * @param sourceDirectory assume that is <code>sourceDirectory</code> is a not null directory and exists
     */
    public CheckstyleReportListener( File sourceDirectory )
    {
        this.sourceDirectories = new ArrayList<File>();
        this.sourceDirectories.add( sourceDirectory );
    }
    /**
     * @param sourceDirectory assume that is <code>sourceDirectory</code> is a not null directory and exists
     * @param configuration checkstyle configuration
     * @since 2.5
     */
    public CheckstyleReportListener( File sourceDirectory, Configuration configuration )
    {
        this.sourceDirectories = new ArrayList<File>();
        this.sourceDirectories.add( sourceDirectory );
        this.checkstyleConfiguration = configuration;
    }

    /**
     * @param configuration checkstyle configuration
     * @since 2.5
     */
    public CheckstyleReportListener( Configuration configuration )
    {
        this.sourceDirectories = new ArrayList<File>();
        this.checkstyleConfiguration = configuration;
    }

    /**
     * @param sourceDirectory assume that is <code>sourceDirectory</code> is a not null directory and exists
     */
    public void addSourceDirectory( File sourceDirectory )
    {
        this.sourceDirectories.add( sourceDirectory );
    }

    /**
     * @param severityLevel
     */
    public void setSeverityLevelFilter( SeverityLevel severityLevel )
    {
        this.severityLevel = severityLevel;
    }

    /**
     * @return
     */
    public SeverityLevel getSeverityLevelFilter()
    {
        return severityLevel;
    }

    /** {@inheritDoc} */
    public void auditStarted( AuditEvent event )
    {
        setResults( new CheckstyleResults() );
    }

    /** {@inheritDoc} */
    public void auditFinished( AuditEvent event )
    {
        //do nothing
    }

    /** {@inheritDoc} */
    public void fileStarted( AuditEvent event )
    {
        for ( File sourceDirectory : sourceDirectories )
        {
            String sourceDirectoryPath = sourceDirectory.getPath();
            String fileName = StringUtils.replace( event.getFileName(), "\\", "/" );
            if ( fileName.startsWith( sourceDirectoryPath + "/" ) )
            {
                currentFile = StringUtils.substring( fileName, sourceDirectoryPath.length() + 1 );

                events = getResults().getFileViolations( currentFile );
            }
        }

        if ( events == null )
        {
            events = new ArrayList<AuditEvent>();
        }
    }

    /** {@inheritDoc} */
    public void fileFinished( AuditEvent event )
    {
        getResults().setFileViolations( currentFile, events );
        currentFile = null;
    }

    /** {@inheritDoc} */
    public void addError( AuditEvent event )
    {
        if ( SeverityLevel.IGNORE.equals( event.getSeverityLevel() ) )
        {
            return;
        }

        if ( severityLevel == null || severityLevel.equals( event.getSeverityLevel() ) )
        {
            events.add( event );
        }
    }

    /** {@inheritDoc} */
    public void addException( AuditEvent event, Throwable throwable )
    {
        //Do Nothing
    }

    /**
     * @return
     */
    public CheckstyleResults getResults()
    {
        results.setConfiguration( checkstyleConfiguration );
        return results;
    }

    /**
     * @param results
     */
    public void setResults( CheckstyleResults results )
    {
        this.results = results;
    }

    /**
     * @since 2.5
     */
    public Configuration getCheckstyleConfiguration()
    {
        return checkstyleConfiguration;
    }

    /**
     * @since 2.5
     */
    public void setCheckstyleConfiguration( Configuration checkstyleConfiguration )
    {
        this.checkstyleConfiguration = checkstyleConfiguration;
    }

}

