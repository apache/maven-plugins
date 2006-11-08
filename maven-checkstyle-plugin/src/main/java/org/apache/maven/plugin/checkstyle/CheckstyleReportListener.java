package org.apache.maven.plugin.checkstyle;

/*
 * Copyright 2004-2006 The Apache Software Foundation.
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

import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import com.puppycrawl.tools.checkstyle.api.AuditListener;
import com.puppycrawl.tools.checkstyle.api.AutomaticBean;
import com.puppycrawl.tools.checkstyle.api.SeverityLevel;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
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
    private List sourceDirectories;

    private CheckstyleResults results;

    private String currentFile;

    private List events;

    private SeverityLevel severityLevel;

    /**
     * @param sourceDirectory assume that is <code>sourceDirectory</code> is a not null directory and exists
     */
    public CheckstyleReportListener( File sourceDirectory )
    {
        this.sourceDirectories = new ArrayList();
        this.sourceDirectories.add( sourceDirectory );
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

    /**
     * @see com.puppycrawl.tools.checkstyle.api.AuditListener#auditStarted(com.puppycrawl.tools.checkstyle.api.AuditEvent)
     */
    public void auditStarted( AuditEvent event )
    {
        setResults( new CheckstyleResults() );
    }

    /**
     * @see com.puppycrawl.tools.checkstyle.api.AuditListener#auditFinished(com.puppycrawl.tools.checkstyle.api.AuditEvent)
     */
    public void auditFinished( AuditEvent event )
    {
        //do nothing
    }

    /**
     * @see com.puppycrawl.tools.checkstyle.api.AuditListener#fileStarted(com.puppycrawl.tools.checkstyle.api.AuditEvent)
     */
    public void fileStarted( AuditEvent event )
    {
        for ( Iterator it = sourceDirectories.iterator(); it.hasNext(); )
        {
            File sourceDirectory = (File) it.next();

            currentFile = StringUtils.substring( event.getFileName(), sourceDirectory.getPath().length() + 1 );
            currentFile = StringUtils.replace( currentFile, "\\", "/" );

            events = getResults().getFileViolations( currentFile );
        }

        if ( events == null )
        {
            events = new ArrayList();
        }
    }

    /**
     * @see com.puppycrawl.tools.checkstyle.api.AuditListener#fileFinished(com.puppycrawl.tools.checkstyle.api.AuditEvent)
     */
    public void fileFinished( AuditEvent event )
    {
        getResults().setFileViolations( currentFile, events );
        currentFile = null;
    }

    /**
     * @see com.puppycrawl.tools.checkstyle.api.AuditListener#addError(com.puppycrawl.tools.checkstyle.api.AuditEvent)
     */
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

    /**
     * @see com.puppycrawl.tools.checkstyle.api.AuditListener#addException(com.puppycrawl.tools.checkstyle.api.AuditEvent, java.lang.Throwable)
     */
    public void addException( AuditEvent event, Throwable throwable )
    {
        //Do Nothing
    }

    /**
     * @return
     */
    public CheckstyleResults getResults()
    {
        return results;
    }

    /**
     * @param results
     */
    public void setResults( CheckstyleResults results )
    {
        this.results = results;
    }
}

