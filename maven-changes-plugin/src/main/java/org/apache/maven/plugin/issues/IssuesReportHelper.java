package org.apache.maven.plugin.issues;

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

import java.util.Iterator;
import java.util.List;

/**
 * A helper class for generation of reports based on issues.
 *
 * @author Dennis Lundberg
 * @version $Id$
 */
public class IssuesReportHelper
{
    /**
     * Print a list of values separated by commas.
     *
     * @param values The values to print
     * @return A nicely formatted string of values.
     */
    public static String printValues( List values )
    {
        StringBuffer sb = new StringBuffer();
        if( values != null )
        {
            Iterator iterator = values.iterator();
            while ( iterator.hasNext() )
            {
                String value = (String) iterator.next();
                sb.append( value );
                if ( iterator.hasNext() )
                {
                    sb.append( ", " );
                }
            }
        }
        return sb.toString();
    }
}
