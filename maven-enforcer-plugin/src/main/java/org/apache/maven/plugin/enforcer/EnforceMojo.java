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
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

/**
 * This goal checks for required versions of Maven and/or the JDK
 * 
 * @goal enforce
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * @phase process-sources
 */
public class EnforceMojo
    extends AbstractMojo
{
    /**
     * Runtime information containing Maven Version.
     * 
     * @parameter expression="${session}"
     * @required
     * @readonly
     */
    protected MavenSession session;

    /**
     * Flag to fail the build if a version check fails.
     * 
     * @parameter expression="${enforcer.fail}" default-value="true"
     */
    protected boolean fail = true;

    /**
     * Flag to easily skip all checks
     * 
     * @parameter expression="${enforcer.skip}" default-value="false"
     */
    protected boolean skip = false;

    /**
     * @parameter
     * @required
     */
    private EnforcementRule[] rules;

    public void execute()
        throws MojoExecutionException
    {
        if ( !skip )
        {
            Log log = this.getLog();
            EnforcementRuleHelper helper = new DefaultEnforcementRuleHelper( session, log );
            try
            {
                for ( int i = 0; i < rules.length; i++ )
                {
                    rules[i].execute( helper );
                }
            }
            catch ( MojoExecutionException e )
            {
                if ( fail )
                {
                    throw e;
                }
                else
                {
                    log.warn( e.getLocalizedMessage() );
                }
            }
        }
    }

}
