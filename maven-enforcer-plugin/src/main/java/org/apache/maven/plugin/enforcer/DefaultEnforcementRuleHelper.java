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

import java.io.File;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.RuntimeInformation;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.enforcer.rule.api.EnforcerRuleHelper;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

/**
 * Default implementation of the EnforcementRuleHelper interface. This is used
 * to help retreive information from the session and provide usefull elements
 * like the log.
 * 
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * @version $Id: DefaultEnforcementRuleHelper.java 523141 2007-03-28 02:11:47Z
 *          brianf $
 */
public class DefaultEnforcementRuleHelper
    implements EnforcerRuleHelper
{
    Log log;

    ExpressionEvaluator evaluator;

    MavenSession session;

    public DefaultEnforcementRuleHelper( MavenSession session, ExpressionEvaluator evaluator, Log log )
    {
        this.evaluator = evaluator;
        this.log = log;
        this.session = session;
    }

    public Log getLog()
    {
        return log;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator#alignToBaseDirectory(java.io.File)
     */
    public File alignToBaseDirectory( File theFile )
    {
        return evaluator.alignToBaseDirectory( theFile );
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator#evaluate(java.lang.String)
     */
    public Object evaluate( String theExpression )
        throws ExpressionEvaluationException
    {
        return evaluator.evaluate( theExpression );
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.maven.shared.enforcer.rule.api.EnforcerRuleHelper#getRuntimeInformation()
     */
    public Object getComponent( Class clazz )
        throws ComponentLookupException
    {
        return session.lookup( clazz.getName() );
    }

}
