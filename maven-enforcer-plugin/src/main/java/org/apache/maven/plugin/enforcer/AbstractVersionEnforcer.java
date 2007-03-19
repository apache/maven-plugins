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
import java.util.Iterator;

import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.Restriction;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.StringUtils;

/**
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * 
 */
public abstract class AbstractVersionEnforcer
    extends AbstractEnforcer
{



    public boolean enforceVersion( String variableName, String requiredVersionRange, ArtifactVersion actualVersion )
        throws MojoExecutionException
    {
        boolean allowed = false;
        if ( StringUtils.isEmpty( requiredVersionRange ) )
        {
            throw new MojoExecutionException( variableName + " version can't be empty." );
        }
        else
        {

            VersionRange vr;
            Log log = this.getLog();
            String msg = "Detected " + variableName + " Version: " + actualVersion;

            // stort circuit check if the strings are exactly equal
            if ( actualVersion.toString().equals( requiredVersionRange ) )
            {
                log.info( msg + " is allowed in the range " + requiredVersionRange + "." );
                allowed = true;
            }
            else
            {
                try
                {
                    vr = VersionRange.createFromVersionSpec( requiredVersionRange );

                    if ( containsVersion( vr, actualVersion ) )
                    {
                        log.info( msg + " is allowed in the range " + requiredVersionRange + "." );
                        allowed = true;
                    }
                    else
                    {
                        String error = msg + " is not in the allowed range " + vr + ".";
                        if ( !fail )
                        {
                            log.warn( error );
                        }
                        else
                        {
                            throw new MojoExecutionException( error );
                        }
                    }
                }
                catch ( InvalidVersionSpecificationException e )
                {
                    throw new MojoExecutionException( "The requested " + variableName + " version "
                        + requiredVersionRange + " is invalid.", e );
                }
            }
        }
        return allowed;
    }

    /**
     * Copied from Artifact.VersionRange. This is tweaked to handle singular
     * ranges properly. Currently the default containsVersion method assumes a
     * singular version means allow everything. This method assumes that "2.0.4" ==
     * "[2.0.4,)"
     * 
     */
    public static boolean containsVersion( VersionRange allowedRange, ArtifactVersion version )
    {
        boolean matched = false;
        ArtifactVersion recommendedVersion = allowedRange.getRecommendedVersion();
        if ( recommendedVersion == null )
        {

            for ( Iterator i = allowedRange.getRestrictions().iterator(); i.hasNext() && !matched; )
            {
                Restriction restriction = (Restriction) i.next();
                if ( restriction.containsVersion( version ) )
                {
                    matched = true;
                }
            }
        }
        else
        {
            // only singular versions ever have a recommendedVersion
            int compareTo = recommendedVersion.compareTo( version );
            matched = ( compareTo <= 0 );
        }
        return matched;
    }

    /**
     * @return the fail
     */
    public boolean isFail()
    {
        return this.fail;
    }

    /**
     * @param theWarn
     *            the fail to set
     */
    public void setFail( boolean theWarn )
    {
        this.fail = theWarn;
    }
}
