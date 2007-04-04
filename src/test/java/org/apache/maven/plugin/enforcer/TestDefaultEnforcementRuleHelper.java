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

import junit.framework.TestCase;

import org.apache.maven.execution.RuntimeInformation;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

/**
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * 
 */
public class TestDefaultEnforcementRuleHelper
    extends TestCase
{
    public void testHelper()
        throws ComponentLookupException, ExpressionEvaluationException
    {
        Log log = new SystemStreamLog();
        DefaultEnforcementRuleHelper helper = (DefaultEnforcementRuleHelper) EnforcerTestUtils.getHelper();

        assertNotNull( helper.getLog() );
        assertNotNull( helper.evaluate( "${session}" ) );
        assertNotNull( helper.evaluate( "${project}" ) );
        assertNotNull( helper.getComponent( RuntimeInformation.class ) );

    }
}
