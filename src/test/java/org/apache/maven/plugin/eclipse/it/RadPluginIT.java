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
package org.apache.maven.plugin.eclipse.it;

import java.io.File;
import java.util.Properties;

import org.codehaus.plexus.util.FileUtils;

/**
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @author <a href="mailto:berrach@apache.org">Barrie Treloar</a>
 */
public class RadPluginIT
    extends AbstractEclipsePluginIT
{
    public void testProject1()
        throws Exception
    {
        testProject( "project-rad-1", new Properties(), "rad-clean", "rad" );
    }

    public void testProject2()
        throws Exception
    {
        testProject( "project-rad-2", new Properties(), "rad-clean", "rad" );
    }

    public void testProject3()
        throws Exception
    {
        testProject( "project-rad-3", new Properties(), "rad-clean", "rad" );
    }

    public void testProject4()
        throws Exception
    {
        testProject( "project-rad-4", new Properties(), "rad-clean", "rad" );
    }

    public void testProject5()
        throws Exception
    {
        File basedir = getTestFile( "target/test-classes/projects/project-rad-5" );
        FileUtils.deleteDirectory( new File( basedir, "project-rad-1/META-INF" ) );
        new File( basedir, "project-rad-1/META-INF" ).mkdirs();
        testProject( "project-rad-5", new Properties(), "rad-clean", "rad", true );
    }

    public void testProject6()
        throws Exception
    {
        testProject( "project-rad-6", new Properties(), "rad-clean", "rad" );
    }

    /**
     * Tests warSourceDirectory setting to be reflected in generated .websettings, location of jars in WEB-INF/lib and
     * generation of MANIFEST.MF at the right place
     * 
     * @throws Exception
     */
    public void testProject7()
        throws Exception
    {
        testProject( "project-rad-7", new Properties(), "rad-clean", "rad" );
        assertFalse(
                     "Default path should not exist because it is overridden!",
                     new File( getTestFile( "target/test-classes/projects/project-rad-7" ), "/src/main/webapp" ).exists() );

    }

    public void testProject8()
        throws Exception
    {
        testProject( "project-rad-8", new Properties(), "rad-clean", "rad" );
    }
}
