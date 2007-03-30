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

import org.apache.commons.lang.SystemUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.RuntimeInformation;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

/**
 * This goal displays the current platform information
 * 
 * @goal display-info
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * @version $Id: EnforceMojo.java 523156 2007-03-28 03:53:54Z brianf $
 */
public class DisplayInfoMojo
    extends AbstractMojo
{
    /**
     * Maven Session.
     * 
     * @parameter expression="${session}"
     * @required
     * @readonly
     */
    protected MavenSession session;

    /**
     * Entry point to the mojo
     */
    public void execute()
        throws MojoExecutionException
    {
        try
        {
            DefaultEnforcementRuleHelper helper = new DefaultEnforcementRuleHelper( session, getLog() );
            RuntimeInformation rti = helper.getRuntimeInformation();
            getLog().info( "Maven Version: " + rti.getApplicationVersion() );
            RequireJavaVersion java = new RequireJavaVersion();
            getLog().info(
                           "JDK Version: " + SystemUtils.JAVA_VERSION + " normalized as: "
                               + java.normalizeJDKVersion( SystemUtils.JAVA_VERSION_TRIMMED ) );
            RequireOS os = new RequireOS();
            os.displayOSInfo( getLog(), true );

        }
        catch ( ComponentLookupException e )
        {
            getLog().warn( "Unable to retreive component." + e.getLocalizedMessage() );
        }

    }

}
