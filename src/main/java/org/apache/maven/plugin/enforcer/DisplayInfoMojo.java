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
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.path.PathTranslator;
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
     * Path Translator needed by the ExpressionEvaluator
     * 
     * @component role="org.apache.maven.project.path.PathTranslator"
     */
    protected PathTranslator translator;

    /**
     * The MavenSession
     * 
     * @parameter expression="${session}"
     */
    protected MavenSession session;

    /**
     * POM
     * 
     * @parameter expression="${project}"
     * @readonly
     * @required
     */
    protected MavenProject project;

    /**
     * Entry point to the mojo
     */
    public void execute()
        throws MojoExecutionException
    {
        try
        {
            EnforcerExpressionEvaluator evaluator = new EnforcerExpressionEvaluator( session, translator, project );
            DefaultEnforcementRuleHelper helper = new DefaultEnforcementRuleHelper( session, evaluator, getLog() );
            RuntimeInformation rti = (RuntimeInformation) helper.getComponent( RuntimeInformation.class );
            getLog().info( "Maven Version: " + rti.getApplicationVersion() );
            getLog().info(
                           "JDK Version: " + SystemUtils.JAVA_VERSION + " normalized as: "
                               + RequireJavaVersion.normalizeJDKVersion( SystemUtils.JAVA_VERSION_TRIMMED ) );
            RequireOS os = new RequireOS();
            os.displayOSInfo( getLog(), true );

        }
        catch ( ComponentLookupException e )
        {
            getLog().warn( "Unable to Lookup component: " + e.getLocalizedMessage() );
        }

    }

}
