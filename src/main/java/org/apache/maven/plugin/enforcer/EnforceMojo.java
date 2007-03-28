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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.shared.enforcer.rule.api.EnforcerRule;
import org.apache.maven.shared.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.shared.enforcer.rule.api.EnforcerRuleHelper;

/**
 * This goal executes the defined enforcer-rules once per module.
 * 
 * @goal enforce
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * @phase verify
 * @version $Id$
 */
public class EnforceMojo
    extends AbstractMojo
{
    /**
     * Runtime information containing Maven Version.
     * 
     * @parameter expression="${session}"
     * @required
     * @readonly
     */
    protected MavenSession session;

    /**
     * Flag to fail the build if a version check fails.
     * 
     * @parameter expression="${enforcer.fail}" default-value="true"
     */
    protected boolean fail = true;

    /**
     * Flag to easily skip all checks
     * 
     * @parameter expression="${enforcer.skip}" default-value="false"
     */
    protected boolean skip = false;

    /**
     * Fail on the first rule that doesn't pass
     * 
     * @parameter expression="${enforcer.failFast}" default-value="false"
     */
    protected boolean failFast = false;

    /**
     * @parameter
     * @required
     */
    private EnforcerRule[] rules;

    /**
     * Entry point to the mojo
     */
    public void execute()
        throws MojoExecutionException
    {
        Log log = this.getLog();

        // the entire execution can be easily skipped
        if ( !skip )
        {
            // list to store exceptions
            ArrayList list = new ArrayList();

            // make sure the rules exist
            if ( rules != null && rules.length > 0 )
            {
                String currentRule = "Unknown";

                // create my helper
                EnforcerRuleHelper helper = new DefaultEnforcementRuleHelper( session, log );

                // if we are only warning, then disable failFast
                if ( !fail )
                {
                    failFast = false;
                }

                // go through each rule unless
                for ( int i = 0; i < rules.length; i++ )
                {

                    // prevent against empty rules
                    EnforcerRule rule = rules[i];
                    if ( rule != null )
                    {
                        // store the current rule for loggin purposes
                        currentRule = rule.getClass().getSimpleName();
                        log.debug( "Executing rule: " + currentRule );
                        try
                        {
                            // execute the rule
                            rules[i].execute( helper );
                        }
                        catch ( EnforcerRuleException e )
                        {
                            // i can throw an exception because failfast will be
                            // false if fail is false.
                            if ( failFast )
                            {
                                throw new MojoExecutionException( currentRule + " failed with message: "
                                    + e.getMessage(), e );
                            }
                            else
                            {
                                list.add( "Rule "+i+": "+currentRule+" failed with message: " + e.getMessage());
                            }
                        }
                    }
                }

                // if we found anything
                if ( !list.isEmpty() )
                {
                    Iterator iter = list.iterator();
                    while ( iter.hasNext() )
                    {
                        String failure = (String) iter.next();
                        log.warn( failure );
                    }
                    if ( fail )
                    {
                        throw new MojoExecutionException( "Some rules have failed." );
                    }
                }
            }
            else
            {
                throw new MojoExecutionException(
                                                  "No rules are configured. Use the skip flag if you want to disable execution." );
            }
        }
        else
        {
            log.info( "Skipping Rule Enforcement." );
        }
    }

    /**
     * @return the fail
     */
    public boolean isFail()
    {
        return this.fail;
    }

    /**
     * @param theFail
     *            the fail to set
     */
    public void setFail( boolean theFail )
    {
        this.fail = theFail;
    }

    /**
     * @return the rules
     */
    public EnforcerRule[] getRules()
    {
        return this.rules;
    }

    /**
     * @param theRules
     *            the rules to set
     */
    public void setRules( EnforcerRule[] theRules )
    {
        this.rules = theRules;
    }

    /**
     * @return the session
     */
    public MavenSession getSession()
    {
        return this.session;
    }

    /**
     * @param theSession
     *            the session to set
     */
    public void setSession( MavenSession theSession )
    {
        this.session = theSession;
    }

    /**
     * @return the skip
     */
    public boolean isSkip()
    {
        return this.skip;
    }

    /**
     * @param theSkip
     *            the skip to set
     */
    public void setSkip( boolean theSkip )
    {
        this.skip = theSkip;
    }

    /**
     * @return the failFast
     */
    public boolean isFailFast()
    {
        return this.failFast;
    }

    /**
     * @param theFailFast the failFast to set
     */
    public void setFailFast( boolean theFailFast )
    {
        this.failFast = theFailFast;
    }

}
