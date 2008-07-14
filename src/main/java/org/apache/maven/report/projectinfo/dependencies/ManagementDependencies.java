package org.apache.maven.report.projectinfo.dependencies;

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;

/**
 * @author Nick Stolwijk
 * @version $Id$
 * @since 2.1
 */
public class ManagementDependencies
{
    private final List managementDependencies;

    /**
     * @param projectDependencies
     */
    public ManagementDependencies( List projectDependencies )
    {
        this.managementDependencies = projectDependencies;
    }

    /**
     * @return <code>true</code> if managementDependencies is not null and not empty.
     */
    public boolean hasDependencies()
    {
        return ( managementDependencies != null ) && ( !this.managementDependencies.isEmpty() );
    }

    /**
     * @return managementDependencies
     */
    public List getManagementDependencies()
    {
        return new ArrayList( managementDependencies );
    }

    /**
     * @return the managementDependencies by scope
     * @see Artifact#SCOPE_COMPILE
     * @see Artifact#SCOPE_PROVIDED
     * @see Artifact#SCOPE_RUNTIME
     * @see Artifact#SCOPE_SYSTEM
     * @see Artifact#SCOPE_TEST
     */
    public Map getManagementDependenciesByScope()
    {
        Map dependenciesByScope = new HashMap();
        for ( Iterator i = getManagementDependencies().iterator(); i.hasNext(); )
        {
            Dependency dependency = (Dependency) i.next();
            String scope = dependency.getScope() != null ? dependency.getScope() : Artifact.SCOPE_COMPILE;
            List multiValue = (List) dependenciesByScope.get( scope );
            if ( multiValue == null )
            {
                multiValue = new ArrayList();
            }
            multiValue.add( dependency );
            dependenciesByScope.put( scope, multiValue );
        }

        return dependenciesByScope;
    }
}
