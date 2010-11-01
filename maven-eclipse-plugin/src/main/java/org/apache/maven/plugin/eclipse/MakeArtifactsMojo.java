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
package org.apache.maven.plugin.eclipse;

import java.util.Map;
import java.util.StringTokenizer;

import org.apache.maven.model.Model;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.eclipse.osgiplugin.EclipseOsgiPlugin;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;
import org.codehaus.plexus.util.StringUtils;

/**
 * Add eclipse artifacts from an eclipse installation to the local repo. This mojo automatically analize the eclipse
 * directory, copy plugins jars to the local maven repo, and generates appropriate poms. Use
 * <code>eclipse:to-maven</code> for the latest naming conventions in place, <code>groupId</code>.
 * <code>artifactId</code>.
 * 
 * @author Fabrizio Giustina
 * @author <a href="mailto:carlos@apache.org">Carlos Sanchez</a>
 * @version $Id$
 * @goal make-artifacts
 * @requiresProject false
 * @deprecated use {@link EclipseToMavenMojo} for the latest naming conventions
 */
public class MakeArtifactsMojo
    extends EclipseToMavenMojo
    implements Contextualizable
{

    /**
     * Strip qualifier (fourth token) from the plugin version. Qualifiers are for eclipse plugin the equivalent of
     * timestamped snapshot versions for Maven, but the date is maintained also for released version (e.g. a jar for the
     * release <code>3.2</code> can be named <code>org.eclipse.core.filesystem_1.0.0.v20060603.jar</code>. It's usually
     * handy to not to include this qualifier when generating maven artifacts for major releases, while it's needed when
     * working with eclipse integration/nightly builds.
     * 
     * @parameter expression="${stripQualifier}" default-value="true"
     */
    private boolean stripQualifier;

    /**
     * Default token to use as a qualifier. Tipically qualifiers for plugins in the same eclipse build are different.
     * This parameter can be used to "align" qualifiers so that all the plugins coming from the same eclipse build can
     * be easily identified. For example, setting this to "M3" will force the pluging versions to be "*.*.*.M3"
     * 
     * @parameter expression="${forcedQualifier}"
     */
    private String forcedQualifier;

    /**
     * Resolve version ranges in generated pom dependencies to versions of the other plugins being converted
     * 
     * @parameter expression="${resolveVersionRanges}" default-value="false"
     */
    private boolean resolveVersionRanges;

    protected String osgiVersionToMavenVersion( String version )
    {
        return osgiVersionToMavenVersion( version, forcedQualifier, stripQualifier );
    }

    /**
     * Get the group id as the three first tokens in artifacts Id e.g. <code>org.eclipse.jdt</code> ->
     * <code>org.eclipse.jdt</code>
     * 
     * @param bundleName bundle name
     * @return group id
     */
    protected String createGroupId( String bundleName )
    {
        if ( StringUtils.countMatches( bundleName, "." ) > 1 )
        {
            StringTokenizer st = new StringTokenizer( bundleName, "." );
            int i = 0;
            String groupId = "";
            while ( st.hasMoreTokens() && ( i < 3 ) )
            {
                groupId += "." + st.nextToken();
                i++;
            }
            return groupId.substring( 1 );
        }
        return bundleName;
    }

    /**
     * Get the artifact id equal to the bundleName e.g. <code>org.eclipse.jdt</code> -> <code>org.eclipse.jdt</code>
     * 
     * @param bundleName bundle name
     * @return artifact id
     */
    protected String createArtifactId( String bundleName )
    {
        return bundleName;
    }

    protected void resolveVersionRanges( Model model, Map models )
        throws MojoFailureException
    {
        if ( resolveVersionRanges )
        {
            super.resolveVersionRanges( model, models );
        }
        else
        {
            // do nothing
        }
    }

    protected void processPlugin( EclipseOsgiPlugin plugin, Model model, Map plugins, Map models )
        throws MojoExecutionException, MojoFailureException
    {
        if ( this.resolveVersionRanges && plugins.containsKey( getKey( model ) ) )
        {
            throw new MojoFailureException( "There are two versions of the same plugin, can not resolve versions: "
                + getKey( model ) );
        }

        super.processPlugin( plugin, model, plugins, models );
    }

}
