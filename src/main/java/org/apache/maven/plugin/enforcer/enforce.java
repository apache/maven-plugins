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

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.SystemUtils;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.execution.RuntimeInformation;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.StringUtils;

/**
 * Goal which fails the build if the specified version isn't allowed.
 * 
 * @goal enforce
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * @phase process-sources
 */
public class enforce
    extends abstractVersionEnforcer
{
    /**
     * Used to look up Artifacts in the remote repository.
     * 
     * @parameter expression="${component.org.apache.maven.execution.RuntimeInformation}"
     * @required
     * @readonly
     */
    protected RuntimeInformation rti;

    /**
     * Specify the required Version of Maven. Some examples are
     * <ul>
     * <li><code>2.0.4</code> Version 2.0.4</li>
     * <li><code>[2.0,2.1)</code> Versions 2.0 (included) to 2.1 (not
     * included)</li>
     * <li><code>[2.0,2.1]</code> Versions 2.0 to 2.1 (both included)</li>
     * <li><code>[2.0.5,)</code> Versions 2.0.5 and higher</li>
     * <li><code>(,2.0.5],[2.1.1,)</code> Versions up to 2.0.5 (included) and
     * 2.1.1 or higher</li>
     * </ul>
     * 
     * @parameter expression="${enforcer.maven.version}" default-value=""
     * @required
     */
    private String mavenVersion = null;

    /**
     * Specify the required Version of JDK. Some examples are
     * <ul>
     * <li><code>2.0.4</code> Version 2.0.4</li>
     * <li><code>[2.0,2.1)</code> Versions 2.0 (included) to 2.1 (not
     * included)</li>
     * <li><code>[2.0,2.1]</code> Versions 2.0 to 2.1 (both included)</li>
     * <li><code>[2.0.5,)</code> Versions 2.0.5 and higher</li>
     * <li><code>(,2.0.5],[2.1.1,)</code> Versions up to 2.0.5 (included) and
     * 2.1.1 or higher</li>
     * </ul>
     * 
     * @parameter expression="${enforcer.jdk.version}" default-value=""
     * @required
     */
    private String jdkVersion = null;

    /**
     * 
     */
    public enforce()
    {
        super();
    }

    
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( StringUtils.isNotEmpty( this.mavenVersion ) )
        {
            ArtifactVersion detectedMavenVersion = rti.getApplicationVersion();
            enforceVersion( "Maven", this.mavenVersion, detectedMavenVersion );
        }

        if ( StringUtils.isNotEmpty( this.jdkVersion ) )
        {
            ArtifactVersion detectedJdkVersion = new DefaultArtifactVersion(
                                                                             fixJDKVersion( SystemUtils.JAVA_VERSION_TRIMMED ) );
            enforceVersion( "JDK", this.jdkVersion, detectedJdkVersion );
        }

    }

    /**
     * Converts a jdk string from 1.5.0-11 to a single 3 digit version like
     * 1.5.0
     */
    public String fixJDKVersion( String theJdkVersion )
    {
        theJdkVersion = theJdkVersion.replaceAll( "_|-", "." );
        String tokenArray[] = StringUtils.split( theJdkVersion, "." );
        List tokens = Arrays.asList( tokenArray );
        StringBuffer buffer = new StringBuffer( theJdkVersion.length() );

        Iterator iter = tokens.iterator();
        for ( int i = 0; i < tokens.size() && i < 3; i++ )
        {
            buffer.append( iter.next() );
            buffer.append( '.' );
        }

        String version = buffer.toString();
        return StringUtils.stripEnd( version, "." );
    }
}
