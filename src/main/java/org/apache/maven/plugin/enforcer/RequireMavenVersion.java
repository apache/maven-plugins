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

import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.execution.RuntimeInformation;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

/**
 * This rule checks that the Maven version is allowd.
 * 
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * @version $Id: AnalyzeMojo.java 522157 2007-03-25 04:34:54Z brianf $
 */
public class RequireMavenVersion
    extends AbstractVersionEnforcer
    implements EnforcementRule
{

    public void execute( EnforcementRuleHelper helper )
        throws MojoExecutionException
    {
        try
        {
            RuntimeInformation rti = helper.getRuntimeInformation();
            ArtifactVersion detectedMavenVersion = rti.getApplicationVersion();
            enforceVersion( helper.getLog(), "Maven", this.version, detectedMavenVersion );
        }
        catch ( ComponentLookupException e )
        {
            throw new MojoExecutionException( "Unable to lookup the component: RuntimeInformation", e );
        }

    }

}
