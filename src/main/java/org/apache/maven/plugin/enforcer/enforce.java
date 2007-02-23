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
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.execution.RuntimeInformation;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.StringUtils;

/**
 * Goal which fails the build if the jdk isn't the correct version
 * 
 * @goal enforce
 * @author Brian Fox
 * @phase process-sources
 */
public class enforce
    extends AbstractMojo
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
     * Flag to warn only if the mavenVersion check fails.
     * 
     * @parameter expression="${enforcer.warn}" default-value="false"
     */
    private boolean warn = false;

    /**
     * 
     */
    public enforce()
    {
        super();
        // TODO Auto-generated constructor stub
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( StringUtils.isNotEmpty( this.mavenVersion ) )
        {
            ArtifactVersion version = rti.getApplicationVersion();
            this.enforceVersion( "Maven", this.mavenVersion, version, this.warn );
        }

        if ( StringUtils.isNotEmpty( this.jdkVersion ) )
        {
            ArtifactVersion version = new DefaultArtifactVersion(fixJDKVersion(SystemUtils.JAVA_VERSION_TRIMMED));
            
            this.enforceVersion( "JDK", fixJDKVersion(this.jdkVersion), version, this.warn );
        }

    }

    public String fixJDKVersion( String jdkVersion )
    {

       /* String token[] = StringUtils.split( jdkVersion.replace( '_', '.' ), "." );
        StringBuffer buffer = new StringBuffer( jdkVersion.length() );
        for ( int i = 0; i <= 2; i++ )
        {
            buffer.append( token[i] );
            buffer.append( "." );
        }

        String version = buffer.toString();
        return StringUtils.stripEnd(version,".");
        */
        return jdkVersion;
    }

    public void enforceVersion( String variableName, String requiredVersionRange, ArtifactVersion actualVersion,
                               boolean warn )
        throws MojoExecutionException, MojoFailureException
    {
        VersionRange vr = null;
        try
        {
            vr = VersionRange.createFromVersionSpec( requiredVersionRange );
        }
        catch ( InvalidVersionSpecificationException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        Log log = this.getLog();
        String msg = "Detected " + variableName + " Version: " + actualVersion;
        if ( vr.containsVersion( actualVersion ) )
        {
            log.debug( msg + " is allowed." );
        }
        else
        {
            String error = msg + " is not in the allowed range: " + vr;
            if ( warn )
            {
                log.warn( error );
            }
            else
            {
                throw new MojoExecutionException( error );
            }
        }
    }
}
