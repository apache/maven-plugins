package org.apache.maven.plugin.enforcer;

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

import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.RuntimeInformation;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

/**
 * Default implementation of the EnforcementRuleHelper interface. This is used
 * to help retreive information from the session and provide usefull elements
 * like the log.
 * 
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * @version $Id$
 */
public class DefaultEnforcementRuleHelper
    implements EnforcementRuleHelper
{
    Log log;

    MavenSession session;

    public DefaultEnforcementRuleHelper( MavenSession session, Log log )
    {
        this.log = log;
        this.session = session;
    }

    public Log getLog()
    {
        return log;
    }

    public MavenSession getSession()
    {
        return session;
    }

    public Object getComponent( Class clazz )
        throws ComponentLookupException
    {
        return session.lookup( clazz.getName() );
    }

    public MavenProject getProject()
        throws ComponentLookupException
    {
        return (MavenProject) getComponent( MavenProject.class );
    }

    public RuntimeInformation getRuntimeInformation()
        throws ComponentLookupException
    {
        return (RuntimeInformation) getComponent( RuntimeInformation.class );
    }
}
