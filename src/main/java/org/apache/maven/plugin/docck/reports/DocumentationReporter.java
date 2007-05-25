package org.apache.maven.plugin.docck.reports;

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

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * @author Edwin Punzalan
 */
public class DocumentationReporter
{
    List reports = new ArrayList();

    public void info( String message )
    {
        reports.add( new InfoDocumentationReport( "[INFO]  " + message ) );
    }

    public void warn( String message )
    {
        reports.add( new WarningDocumentationReport( "[WARN]  " + message ) );
    }

    public void error( String message )
    {
        reports.add( new ErrorDocumentationReport( "[ERROR] " + message ) );
    }

    public List getMessagesByType( int type )
    {
        List list = new ArrayList();

        for( Iterator iter = reports.iterator(); iter.hasNext(); )
        {
            DocumentationReport report = (DocumentationReport) iter.next();

            if ( report.getType() == type )
            {
                list.add( report.getMessage() );
            }
        }

        return list;
    }

    public List getMessages()
    {
        List list = new ArrayList();

        for( Iterator iter = reports.iterator(); iter.hasNext(); )
        {
            DocumentationReport report = (DocumentationReport) iter.next();

            list.add( report.getMessage() );
        }

        return list;
    }

    public boolean hasErrors()
    {
        for( Iterator iter = reports.iterator(); iter.hasNext(); )
        {
            DocumentationReport report = (DocumentationReport) iter.next();

            if ( report.getType() == DocumentationReport.TYPE_ERROR )
            {
                //first occurrence will do
                return true;
            }
        }

        return false;
    }

    public void clear()
    {
        reports.clear();
    }
}
