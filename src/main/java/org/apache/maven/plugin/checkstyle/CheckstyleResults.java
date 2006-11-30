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

import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import com.puppycrawl.tools.checkstyle.api.SeverityLevel;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Object holding the references to the CheckstyleResults.
 *
 * @author <a href="mailto:joakim@erdfelt.net">Joakim Erdfelt</a>
 * @todo provide fallback to disk based storage if too many results.
 */
public class CheckstyleResults
{
    private Map files;

    public CheckstyleResults()
    {
        files = new HashMap();
    }

    public List getFileViolations( String file )
    {
        List violations;

        if ( this.files.containsKey( file ) )
        {
            violations = (List) this.files.get( file );
        }
        else
        {
            violations = new LinkedList();
            this.files.put( file, violations );
        }

        return violations;
    }

    public void setFileViolations( String file, List violations )
    {
        this.files.put( file, violations );
    }

    public Map getFiles()
    {
        return files;
    }

    public void setFiles( Map files )
    {
        this.files = files;
    }

    public int getFileCount()
    {
        return this.files.size();
    }

    public long getSeverityCount( SeverityLevel level )
    {
        long count = 0;

        Iterator it = this.files.values().iterator();

        while ( it.hasNext() )
        {
            List errors = (List) it.next();

            count = count + getSeverityCount( errors, level );
        }

        return count;
    }

    public long getSeverityCount( String file, SeverityLevel level )
    {
        long count = 0;

        if ( !this.files.containsKey( file ) )
        {
            return count;
        }

        List violations = (List) this.files.get( file );

        count = getSeverityCount( violations, level );

        return count;
    }

    public long getSeverityCount( List violations, SeverityLevel level )
    {
        long count = 0;

        Iterator it = violations.iterator();

        while ( it.hasNext() )
        {
            AuditEvent event = (AuditEvent) it.next();

            if ( event.getSeverityLevel().equals( level ) )
            {
                count++;
            }
        }

        return count;
    }
}
