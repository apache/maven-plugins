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
import org.apache.maven.shared.enforcer.rule.api.EnforcerRule;
import org.apache.maven.shared.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.shared.enforcer.rule.api.EnforcerRuleHelper;
import org.codehaus.plexus.util.StringUtils;

/**
 * This rule checks that the Java version is allowed.
 * 
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * @version $Id$
 */
public class RequireJavaVersion
    extends AbstractVersionEnforcer
    implements EnforcerRule
{

    public void execute( EnforcerRuleHelper helper )
        throws EnforcerRuleException
    {

        ArtifactVersion detectedJdkVersion = new DefaultArtifactVersion(
                                                                         fixJDKVersion( SystemUtils.JAVA_VERSION_TRIMMED ) );
        enforceVersion( helper.getLog(), "JDK", version, detectedJdkVersion );
    }

    /**
     * Converts a jdk string from 1.5.0-11 to a single 3 digit version like
     * 1.5.0
     * 
     * @param theJdkVersion
     *            to be converted.
     * @return the converted string.
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
}
