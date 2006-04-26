package org.apache.maven.plugin.resources;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.plugin.resources.stub.MavenProjectBasicStub;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;

public class ReflectionPropertiesTest
    extends AbstractMojoTestCase
{
    // data
    final static protected String pomFilePath = "/target/test-classes/unit/reflectionproperties-test/plugin-config.xml";


    protected void setUp()
        throws Exception
    {
        super.setUp();
    }

    protected void tearDown()
        throws Exception
    {

    }

    public void testGet_escapeBackslashCharacterInPath()
        throws Exception
    {
        // setup data
        MavenProjectBasicStub project = new MavenProjectBasicStub( "escapeBackSlashCharacterInPath" );

        // set dummy value
        project.setDescription( "c:\\\\org\\apache\\test" );

        ReflectionProperties reflectProp = new ReflectionProperties( project, true );

        // project property to be verified
        String reflectPropValue = (String) reflectProp.get( "description" );

        // expected value is c\:\\\\org\\apache\\test 
        assertTrue( reflectPropValue.equals( "c\\:\\\\\\\\org\\\\apache\\\\test" ) );
    }

    public void testGet_dontEscapeBackslashCharacterInPath()
        throws Exception
    {
        // setup data
        MavenProjectBasicStub project = new MavenProjectBasicStub( "dontEscapeBackSlashCharacterInPath" );

        // set dummy value
        project.setDescription( "c:\\\\org\\apache\\test" );

        // project property to be verified
        ReflectionProperties reflectProp = new ReflectionProperties( project, false );

        // project property to be verified
        String reflectPropValue = (String) reflectProp.get( "description" );

        // expected value is c:\\org\apache\test 
        assertTrue( reflectPropValue.equals( "c:\\\\org\\apache\\test" ) );
    }
}
