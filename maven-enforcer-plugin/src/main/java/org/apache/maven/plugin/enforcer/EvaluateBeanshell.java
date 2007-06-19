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

import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.RuntimeInformation;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.enforcer.rule.api.EnforcerRule;
import org.apache.maven.shared.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.shared.enforcer.rule.api.EnforcerRuleHelper;
import org.codehaus.plexus.util.StringUtils;

import bsh.EvalError;
import bsh.Interpreter;

/**
 * @author hugonnem Rule for Maven Enforcer using Beanshell
 *         to evaluate a conditional expression
 * 
 */
public class EvaluateBeanshell
    implements EnforcerRule
{

    /**
     * Beanshell interpreter
     */
    private static final Interpreter bsh = new Interpreter();

    /**
     * The condition to be evaluated.
     * 
     * @parameter
     * @required
     */
    public String condition;

    /**
     * The message to be printed in case the condition
     * returns <b>true</b>
     * 
     * @required
     * @parameter
     */
    public String message;

    public void execute( EnforcerRuleHelper helper )
        throws EnforcerRuleException
    {
        Log log = helper.getLog();

        try
        {
            // get the various expressions out of the
            // helper.
            MavenProject project = (MavenProject) helper.evaluate( "${project}" );
            MavenSession session = (MavenSession) helper.evaluate( "${session}" );
            String target = (String) helper.evaluate( "${project.build.directory}" );
            String artifactId = (String) helper.evaluate( "${project.artifactId}" );
            ArtifactResolver resolver = (ArtifactResolver) helper.getComponent( ArtifactResolver.class );
            RuntimeInformation rti = (RuntimeInformation) helper.getComponent( RuntimeInformation.class );

            log.debug( "Retrieved Target Folder: " + target );
            log.debug( "Retrieved ArtifactId: " + artifactId );
            log.debug( "Retrieved Project: " + project );
            log.debug( "Retrieved RuntimeInfo: " + rti );
            log.debug( "Retrieved Session: " + session );
            log.debug( "Retrieved Resolver: " + resolver );

            log.debug( "Echo condition : " + this.condition );
            // Evaluate condition within Plexus Container
            String script = (String) helper.evaluate( this.condition );
            log.debug( "Echo script : " + script );
            if ( !evaluateCondition( script, log ) )
            {
                if ( StringUtils.isEmpty( message ) )
                {
                    message = "The expression \"" + condition + "\" is not true.";
                }
                throw new EnforcerRuleException( this.message );
            }
        }
        catch ( Exception e )
        {
            throw new EnforcerRuleException( "Unable to lookup a component", e );
        }
    }

    /**
     * Evaluate expression using Beanshell
     * 
     * @param script the expression to be evaluated
     * @param log the logger
     * @return boolean the evaluation of the expression
     */
    protected boolean evaluateCondition( String script, Log log )
    {
        Boolean evaluation = Boolean.FALSE;
        try
        {
            evaluation = (Boolean) bsh.eval( script );
            log.debug( "Echo evaluating : " + evaluation );
        }
        catch ( EvalError ex )
        {
            log.warn( "Couldn't evaluate condition: " + script, ex );
        }
        return evaluation.booleanValue();
    }

}