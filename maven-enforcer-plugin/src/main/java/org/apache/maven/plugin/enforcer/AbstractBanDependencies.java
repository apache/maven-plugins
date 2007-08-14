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
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.enforcer.rule.api.EnforcerRule;
import org.apache.maven.shared.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.shared.enforcer.rule.api.EnforcerRuleHelper;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;

/**
 * Abstract Rule for banning dependencies
 * 
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * @version $Id$
 * 
 */
public abstract class AbstractBanDependencies
    implements EnforcerRule
{

    /**
     * Specify if transitive dependencies should be searched
     * (default) or only look at direct dependencies
     * 
     * @parameter
     */
    public boolean searchTransitive = true;

    /**
     * Specify a friendly message if the rule fails.
     * 
     * @parameter
     */
    public String message = null;

    /**
     * Execute the rule.
     */
    public void execute( EnforcerRuleHelper helper )
        throws EnforcerRuleException
    {

        // get the project
        MavenProject project = null;
        try
        {
            project = (MavenProject) helper.evaluate( "${project}" );
        }
        catch ( ExpressionEvaluationException eee )
        {
            throw new EnforcerRuleException( "Unable to retrieve the MavenProject: ", eee );
        }

        // get the correct list of dependencies
        Set dependencies = null;
        if ( searchTransitive )
        {
            dependencies = project.getArtifacts();
        }
        else
        {
            dependencies = project.getDependencyArtifacts();
        }

        // look for banned dependencies
        Set foundExcludes = checkDependencies( dependencies );

        // if any are found, fail the check but list all of
        // them
        if ( !foundExcludes.isEmpty() )
        {
            StringBuffer buf = new StringBuffer();
            if (message !=null)
            {
                buf.append( message +"\n");
            }
            Iterator iter = foundExcludes.iterator();
            while ( iter.hasNext() )
            {
                buf.append( "Found Banned Dependency: " + ( (Artifact) iter.next() ).getId() + "\n" );
            }
            message = buf.toString();

            throw new EnforcerRuleException( message );
        }

    }

    /**
     * Checks the set of dependencies against the list of
     * excludes
     * 
     * @param dependencies
     * @return
     * @throws EnforcerRuleException
     */
    abstract protected Set checkDependencies( Set dependencies )
        throws EnforcerRuleException;

    /**
     * @return the message
     */
    public String getMessage()
    {
        return this.message;
    }

    /**
     * @param theMessage the message to set
     */
    public void setMessage( String theMessage )
    {
        this.message = theMessage;
    }

    /**
     * @return the searchTransitive
     */
    public boolean isSearchTransitive()
    {
        return this.searchTransitive;
    }

    /**
     * @param theSearchTransitive the searchTransitive to
     *            set
     */
    public void setSearchTransitive( boolean theSearchTransitive )
    {
        this.searchTransitive = theSearchTransitive;
    }
}
