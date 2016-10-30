package org.apache.maven.plugins.help;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.monitor.logging.DefaultLog;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.PluginParameterExpressionEvaluator;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.components.interactivity.InputHandler;
import org.codehaus.plexus.logging.Logger;

/**
 * Test class for the evaluate mojo of the Help Plugin.
 */
public class EvaluateMojoTest
    extends AbstractMojoTestCase
{

    private InterceptingLog interceptingLogger;

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();
        interceptingLogger = new InterceptingLog( getContainer().getLoggerManager().getLoggerForComponent( Mojo.ROLE ) );
    }

    /**
     * Tests evaluation of an expression in interactive mode with a mock input handler.
     * @throws Exception in case of errors.
     */
    public void testEvaluateWithoutExpression()
        throws Exception
    {
        File testPom = new File( getBasedir(), "target/test-classes/unit/evaluate/plugin-config.xml" );

        EvaluateMojo mojo = (EvaluateMojo) lookupMojo( "evaluate", testPom );

        InputHandler inputHandler = mock( InputHandler.class );
        when( inputHandler.readLine() ).thenReturn( "${project.groupId}", "0" );

        ExpressionEvaluator expressionEvaluator = mock( PluginParameterExpressionEvaluator.class );
        when( expressionEvaluator.evaluate( anyString() ) ).thenReturn( "My result" );

        setUpMojo( mojo, inputHandler, expressionEvaluator );

        mojo.execute();

        assertTrue( interceptingLogger.infoLogs.contains( "\nMy result" ) );
        assertTrue( interceptingLogger.warnLogs.isEmpty() );
        verify( expressionEvaluator ).evaluate( "${project.groupId}" );
        verify( inputHandler, times( 2 ) ).readLine();
    }

    /**
     * Tests evaluation of an expression in interactive mode with a mock input handler, when "output" is set.
     * @throws Exception in case of errors.
     */
    public void testEvaluateWithoutExpressionWithOutput()
        throws Exception
    {
        File testPom = new File( getBasedir(), "target/test-classes/unit/evaluate/plugin-config-output.xml" );

        EvaluateMojo mojo = (EvaluateMojo) lookupMojo( "evaluate", testPom );

        InputHandler inputHandler = mock( InputHandler.class );
        when( inputHandler.readLine() ).thenReturn( "${project.artifactId}", "0" );

        ExpressionEvaluator expressionEvaluator = mock( PluginParameterExpressionEvaluator.class );
        when( expressionEvaluator.evaluate( anyString() ) ).thenReturn( "My result" );

        setUpMojo( mojo, inputHandler, expressionEvaluator );

        mojo.execute();

        assertTrue( interceptingLogger.infoLogs.contains( "\nMy result" ) );
        assertFalse( interceptingLogger.warnLogs.isEmpty() );
        verify( expressionEvaluator ).evaluate( "${project.artifactId}" );
        verify( inputHandler, times( 2 ) ).readLine();
    }

    private void setUpMojo( EvaluateMojo mojo, InputHandler inputHandler, ExpressionEvaluator expressionEvaluator )
        throws IllegalAccessException
    {
        setVariableValueToObject( mojo, "inputHandler", inputHandler );
        setVariableValueToObject( mojo, "log", interceptingLogger );
        setVariableValueToObject( mojo, "settings", new Settings() );
        setVariableValueToObject( mojo, "project", new MavenProjectStub() );
        setVariableValueToObject( mojo, "evaluator", expressionEvaluator );
    }

    private static final class InterceptingLog
        extends DefaultLog
    {
        List<String> infoLogs = new ArrayList<String>();
        List<String> warnLogs = new ArrayList<String>();

        public InterceptingLog( Logger logger )
        {
            super( logger );
        }

        @Override
        public void info( CharSequence content )
        {
            super.info( content );
            infoLogs.add( content.toString() );
        }
        
        @Override
        public void warn( CharSequence content )
        {
            super.warn( content );
            warnLogs.add( content.toString() );
        }
    }

}
