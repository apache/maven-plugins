package org.apache.maven.jira;

/*
 * Copyright 2006 The Apache Software Foundation.
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

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.maven.plugin.logging.Log;

import java.text.NumberFormat;
import java.text.ParsePosition;

/**
 * A helper class with common JIRA related functionality.
 *
 * @author Dennis Lundberg
 * @version $Id: JiraHelper.java 422265 2006-07-15 16:49:50 +0000 (lö, 15 jul 2006) dennisl $
 */
public class JiraHelper
{
    /**
     * Try to get a JIRA pid from the issue management URL.
     *
     * @param log     Used to tell the user what happened
     * @param issueManagementUrl The URL to the issue management system
     * @param client  The client used to connect to JIRA
     * @return The JIRA id for the project, or null if it can't be found
     */
    public static String getPidFromJira( Log log, String issueManagementUrl, HttpClient client )
    {
        String jiraId = null;
        GetMethod gm = new GetMethod( issueManagementUrl );
        log.info( "JIRA URL " + issueManagementUrl + " doesn't include a pid, trying to get it" );
        try
        {
            client.executeMethod( gm );
            log.info( "Successfully reached JIRA." );
        }
        catch ( Exception e )
        {
            if ( log.isDebugEnabled() )
            {
                log.error( "Unable to reach JIRA project page:", e );
            }
            else
            {
                log.error( "Unable to reach JIRA project page. Cause is: " + e.getLocalizedMessage() );
            }
        }
        String projectPage = gm.getResponseBodyAsString();
        int pidIndex = projectPage.indexOf( "pid=" );

        if ( pidIndex == -1 )
        {
            log.error( "Unable to get JIRA pid using the url " + issueManagementUrl );
        }
        else
        {
            NumberFormat nf = NumberFormat.getInstance();
            Number pidNumber = nf.parse( projectPage, new ParsePosition( pidIndex + 4 ) );
            jiraId = Integer.toString( pidNumber.intValue() );
            log.debug( "Found the pid " + jiraId + " at " + issueManagementUrl );
        }
        return jiraId;
    }
}
