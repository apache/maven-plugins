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

import org.apache.maven.plugin.MojoExecutionException;

/**
 * Inteface to be implemented by any rules executed by the enforcer.
 * 
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * @version $Id: AnalyzeMojo.java 522157 2007-03-25 04:34:54Z brianf $
 */
public interface EnforcementRule
{
    /**
     * This is the inteface into the rule. This method should throw an exception
     * containing a reason message if the rule fails the check. The plugin will
     * then decide based on the fail flag if it should stop or just log the
     * message as a warning.
     * 
     * @param helper
     *            The helper provides access to the log, MavenSession and has
     *            helpers to get at common components. It is also able to look
     *            up components by class name.
     * @throws MojoExecutionException
     */
    public void execute( EnforcementRuleHelper helper )
        throws MojoExecutionException;

}
