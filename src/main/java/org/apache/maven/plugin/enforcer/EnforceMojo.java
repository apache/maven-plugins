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
import org.codehaus.plexus.util.StringUtils;

/**
 * This goal checks for required versions of Maven and/or the JDK
 * 
 * @goal enforce
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * @phase process-sources
 */
public class EnforceMojo
    extends AbstractVersionEnforcer
{
    /**
     * Runtime information containing Maven Version.
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
     * <li><code>1.4.2</code> Version 1.4.2</li>
     * <li><code>[1.4,1.5)</code> Versions 1.4 (included) to 1.5 (not
     * included)</li>
     * <li><code>[1.4,1.5]</code> Versions 1.4 to 1.5 (both included)</li>
     * <li><code>[1.4.2,)</code> Versions 1.4.2 and higher</li>
     * <li><code>(,1.4.2],[1.5.0,)</code> Versions up to 1.4.2 (included) and
     * 1.5.0 or higher</li>
     * </ul>
     * 
     * @parameter expression="${enforcer.jdk.version}" default-value=""
     * @required
     */
    private String jdkVersion = null;

    /**
     * 
     */
    public EnforceMojo()
    {
        super();
    }

    public void execute()
        throws MojoExecutionException
    {
        if ( !skip )
        {
            boolean foundVersionToCheck = false;
            if ( StringUtils.isNotEmpty( this.mavenVersion ) )
            {
                foundVersionToCheck = true;
                ArtifactVersion detectedMavenVersion = rti.getApplicationVersion();
                enforceVersion( "Maven", this.mavenVersion, detectedMavenVersion );
            }

            if ( StringUtils.isNotEmpty( this.jdkVersion ) )
            {
                foundVersionToCheck = true;
                ArtifactVersion detectedJdkVersion = new DefaultArtifactVersion(
                                                                                 fixJDKVersion( SystemUtils.JAVA_VERSION_TRIMMED ) );
                enforceVersion( "JDK", this.jdkVersion, detectedJdkVersion );
            }

            if ( !foundVersionToCheck )
            {
                throw new MojoExecutionException( "There is no version range specified to be checked." );
            }
        }
    }

    /**
     * Converts a jdk string from 1.5.0-11 to a single 3 digit version like
     * 1.5.0
     */
    public static String fixJDKVersion( String theJdkVersion )
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

    /**
     * @return the jdkVersion
     */
    public String getJdkVersion()
    {
        return this.jdkVersion;
    }

    /**
     * @param theJdkVersion
     *            the jdkVersion to set
     */
    public void setJdkVersion( String theJdkVersion )
    {
        this.jdkVersion = theJdkVersion;
    }

    /**
     * @return the mavenVersion
     */
    public String getMavenVersion()
    {
        return this.mavenVersion;
    }

    /**
     * @param theMavenVersion
     *            the mavenVersion to set
     */
    public void setMavenVersion( String theMavenVersion )
    {
        this.mavenVersion = theMavenVersion;
    }

    /**
     * @return the rti
     */
    public RuntimeInformation getRti()
    {
        return this.rti;
    }

    /**
     * @param theRti
     *            the rti to set
     */
    public void setRti( RuntimeInformation theRti )
    {
        this.rti = theRti;
    }

}
