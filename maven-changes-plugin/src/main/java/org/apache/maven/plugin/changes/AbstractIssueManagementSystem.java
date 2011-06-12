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
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.HashMap;
import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;

/**
 * Historically, this plugin started out working against an IMS-neutral XML file, and then added extensive support for
 * JIRA with some small snippets of code for other issue management systems. This class is intended to start a cleaner
 * modularity for support of multiple systems.<br>
 * Initially, all it provides is a structure for mapping from per-IMS issue types to the three categories defined in
 * {@link IssueAdapter}. <br/>
 * Note that the map in here is <strong>not</strong> immutable. It contains the default
 * configuration for an IMS. Users are expected to add entries to the map via configuration
 * to reflect their customizations.
 */
public abstract class AbstractIssueManagementSystem
    implements IssueManagementSystem
{
    protected Map<String, IssueType> issueTypeMap;

    protected AbstractIssueManagementSystem()
    {
        issueTypeMap = new HashMap<String, IssueType>();
    }

    /* (non-Javadoc)
     * @see org.apache.maven.plugin.changes.IssueManagementSystem#getIssueTypeMap()
     */
    public Map<String, IssueType> getIssueTypeMap()
    {
        return issueTypeMap;
    }
    
    /* (non-Javadoc)
     * @see org.apache.maven.plugin.changes.IssueManagementSystem#getName()
     */
    public abstract String getName();

    /* (non-Javadoc)
     * @see org.apache.maven.plugin.changes.IssueManagementSystem#applyConfiguration(java.util.Map)
     */
    public void applyConfiguration( Map<String, String> issueTypes )
        throws MojoExecutionException
    {
        for ( Map.Entry<String, String> me : issueTypes.entrySet() )
        {
            IssueType type = IssueType.lookupByKey( me.getValue() );
            if ( type == null )
            {
                throw new MojoExecutionException( "Invalid issue action " + me.getValue() );
            }
            String imsTypes = me.getKey();
            String[] imsTypeArray = imsTypes.split( "," );
            for ( String imsType : imsTypeArray ) 
            {
                issueTypeMap.put( imsType, type );
            }
        }
    }
}
