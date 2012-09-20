package org.apache.maven.plugin.changes;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;



/**
 * @author Olivier Lamy
 * @since 29 juil. 2008
 * @version $Id$
 */
public class ChangesValidatorMojoTest
    extends AbstractMojoTestCase
{

    protected ChangesValidatorMojo mojo;

    public void setUp()
        throws Exception
    {
        super.setUp();
        File pom = new File( getBasedir(), "/src/test/unit/plugin-config.xml" );
        mojo = (ChangesValidatorMojo) lookupMojo( "changes-validate", pom );
    }    
    
    public void testValidationSuccess()
        throws Exception
    {
        File changesXml = new File( getBasedir(), "/src/test/unit/changes.xml" );
        setVariableValueToObject( mojo, "xmlPath", changesXml );
        setVariableValueToObject( mojo, "changesXsdVersion", "1.0.0" );
        setVariableValueToObject( mojo, "failOnError", Boolean.TRUE );
        mojo.execute();
    }

    public void testValidationFailedWithMojoFailure()
        throws Exception
    {
        File changesXml = new File( getBasedir(), "/src/test/unit/non-valid-changes.xml" );
        setVariableValueToObject( mojo, "xmlPath", changesXml );
        setVariableValueToObject( mojo, "changesXsdVersion", "1.0.0" );
        setVariableValueToObject( mojo, "failOnError", Boolean.TRUE );
        try
        {
            mojo.execute();
            fail( " a MojoExecutionException should occur here changes file is not valid and failOnError is true " );
        }
        catch ( MojoExecutionException e )
        {
            // we except exception here
        }
    }
    
    public void testValidationFailedWithNoMojoFailure()
        throws Exception
    {
        File changesXml = new File( getBasedir(), "/src/test/unit/non-valid-changes.xml" );
        setVariableValueToObject( mojo, "xmlPath", changesXml );
        setVariableValueToObject( mojo, "changesXsdVersion", "1.0.0" );
        setVariableValueToObject( mojo, "failOnError", Boolean.FALSE );
        mojo.execute();

    }
}
