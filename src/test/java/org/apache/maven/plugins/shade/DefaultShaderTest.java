package org.apache.maven.plugins.shade;

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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.net.URLClassLoader;
import java.net.URL;

import junit.framework.TestCase;

import org.apache.maven.plugins.shade.relocation.SimpleRelocator;
import org.apache.maven.plugins.shade.resource.ComponentsXmlResourceTransformer;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;

/**
 * @author Jason van Zyl
 * @author Mauro Talevi
 */
public class DefaultShaderTest
    extends TestCase
{
    private static final String[] EXCLUDES = new String[] {
        "org/codehaus/plexus/util/xml/Xpp3Dom",
        "org/codehaus/plexus/util/xml/pull.*" };

    public void testShaderWithDefaultShadedPattern()
        throws Exception
    {
        shaderWithPattern( null, new File( "target/foo-default.jar" ), EXCLUDES );
    }

    public void testShaderWithStaticInitializedClass()
        throws Exception
    {
        Shader s = newShader();

        Set set = new LinkedHashSet();

        set.add( new File( "src/test/jars/test-artifact-1.0-SNAPSHOT.jar" ) );

        List relocators = new ArrayList();

        relocators.add( new SimpleRelocator( "org.apache.maven.plugins.shade", null, null ) );

        List resourceTransformers = new ArrayList();

        List filters = new ArrayList();

        File file = new File( "target/testShaderWithStaticInitializedClass.jar" );
        s.shade( set, file, filters, relocators, resourceTransformers );

        URLClassLoader cl = new URLClassLoader( new URL[]{file.toURI().toURL()} );
        Class c = cl.loadClass( "hidden.org.apache.maven.plugins.shade.Lib" );
        Object o = c.newInstance();
        assertEquals( "foo.bar/baz", c.getDeclaredField( "CONSTANT" ).get( o ) );
    }

    public void testShaderWithCustomShadedPattern()
        throws Exception
    {
        shaderWithPattern( "org/shaded/plexus/util", new File( "target/foo-custom.jar" ), EXCLUDES );
    }

    public void testShaderWithoutExcludesShouldRemoveReferencesOfOriginalPattern()
        throws Exception
    {
        //FIXME:  shaded jar should not include references to org/codehaus/* (empty dirs) or org.codehaus.* META-INF files.
        shaderWithPattern( "org/shaded/plexus/util", new File( "target/foo-custom-without-excludes.jar" ), new String[] {} );
    }

    public void shaderWithPattern( String shadedPattern, File jar, String[] excludes )
        throws Exception
    {
        DefaultShader s = newShader();

        Set set = new LinkedHashSet();

        set.add( new File( "src/test/jars/test-project-1.0-SNAPSHOT.jar" ) );

        set.add( new File( "src/test/jars/plexus-utils-1.4.1.jar" ) );

        List relocators = new ArrayList();

        relocators.add( new SimpleRelocator( "org/codehaus/plexus/util", shadedPattern, Arrays.asList( excludes ) ) );

        List resourceTransformers = new ArrayList();

        resourceTransformers.add( new ComponentsXmlResourceTransformer() );

        List filters = new ArrayList();

        s.shade( set, jar, filters, relocators, resourceTransformers );
    }

    private static DefaultShader newShader()
    {
        DefaultShader s = new DefaultShader();

        s.enableLogging( new ConsoleLogger( Logger.LEVEL_INFO, "TEST" ) );

        return s;
    }

}
