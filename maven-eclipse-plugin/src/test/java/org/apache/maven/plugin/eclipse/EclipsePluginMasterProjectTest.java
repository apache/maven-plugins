/*
 * Copyright 2001-2005 The Apache Software Foundation.
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

package org.apache.maven.plugin.eclipse;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.codehaus.plexus.util.IOUtil;

/**
 * <p>
 * Master test for eclipse .classpath and .wtpmodules generation.
 * </p>
 * <p>
 * This test use a 2 modules project with all the mvn dependencies flavours (direct, transitive, with  
 * compile/test/provided/system scope, required and optional, artifacts and modules).
 * </p>
 * <p>
 * In order to fully test the eclipse plugin execution in a such complex environment mvn is executed from a command line.
 * Mvn is started using a custom settings.xml file, created on the fly. The custom settings.xml only adds a mirror for
 * the central repository which is actually a local (file://) repository for loading files from <code>src/test/m2repo</code>
 * </p>
 * <p>The following is the base layout of modules/dependencies. The actual test is to check generated files for module-2</p>
 * <pre>
 * 
 *            +----------------+       +-----------------+       +-----------------+
 *           /| module 1 (jar) | ----> |   refproject    | ----> | deps-refproject |
 *          / +----------------+       +-----------------+       +-----------------+  
 *         /           ^
 *    root             | (depends on)
 *         \           |
 *          \ +----------------+       +-----------------+       +-----------------+
 *           \| module 2 (war) | ----> |     direct      | ----> |   deps-direct   |
 *            +----------------+       +-----------------+       +-----------------+   
 * 
 * </pre>
 * @todo a know problem with this approach is that tests are running with the installed version of the plugin! Don't
 * enable test in pom.xml at the moment or you will never be able to build.
 * @author Fabrizio Giustina
 * @version $Id$
 */
public class EclipsePluginMasterProjectTest
    extends AbstractEclipsePluginTestCase
{

    protected File basedir;

    /**
     * @see org.apache.maven.plugin.eclipse.AbstractEclipsePluginTestCase#setUp()
     */
    protected void setUp()
        throws Exception
    {
        basedir = getTestFile( "target/test-classes/projects/master-test" );
        super.setUp();
    }

    protected void executeMaven2()
        throws Exception
    {
        File pom = new File( basedir, "pom.xml" );

        Properties properties = new Properties();
        properties.setProperty( "wtpversion", "R7" );

        String pluginSpec = getPluginCLISpecification();

        List goals = new ArrayList();

        goals.add( pluginSpec + "clean" );
        goals.add( pluginSpec + "eclipse" );

        executeMaven( pom, properties, goals );

    }

    public void testModule1Project()
        throws Exception
    {
        executeMaven2();
        assertFileEquals( null, new File( basedir, "module-1/expected/.project" ), //
                          new File( basedir, "module-1/.project" ) );
    }

    public void testModule1Classpath()
        throws Exception
    {
        executeMaven2();
        InputStream fis = new FileInputStream( new File( basedir, "module-1/.classpath" ) );
        String classpath = IOUtil.toString( fis );
        IOUtil.close( fis );

        // direct dependencies, include all
        assertContains( "Invalid classpath", classpath, "/refproject-compile" );
        assertContains( "Invalid classpath", classpath, "refproject-sysdep" );
        assertContains( "Invalid classpath", classpath, "/refproject-test" );
        assertContains( "Invalid classpath", classpath, "/refproject-optional" );
        assertContains( "Invalid classpath", classpath, "/refproject-provided" );

        // transitive dependencies
        assertContains( "Invalid classpath", classpath, "/deps-refproject-compile" );
        assertDoesNotContain( "Invalid classpath", classpath, "/deps-refproject-test" );
        assertDoesNotContain( "Invalid classpath", classpath, "/deps-refproject-optional" );
        assertDoesNotContain( "Invalid classpath", classpath, "/deps-refproject-provided" );

    }

    public void testModule1Wtpmodules()
        throws Exception
    {
        executeMaven2();
        assertFileEquals( null, new File( basedir, "module-1/expected/.wtpmodules" ), //
                          new File( basedir, "module-1/.wtpmodules" ) );
    }

    public void testModule2Project()
        throws Exception
    {
        executeMaven2();
        assertFileEquals( null, new File( basedir, "module-2/expected/.project" ), //
                          new File( basedir, "module-2/.project" ) );
    }

    public void testModule2Classpath()
        throws Exception
    {
        executeMaven2();
        InputStream fis = new FileInputStream( new File( basedir, "module-2/.classpath" ) );
        String classpath = IOUtil.toString( fis );
        IOUtil.close( fis );

        // direct dependencies: include all
        assertContains( "Invalid classpath", classpath, "/direct-compile" );
        assertContains( "Invalid classpath", classpath, "/direct-test" );
        assertContains( "Invalid classpath", classpath, "direct-sysdep" );
        assertContains( "Invalid classpath", classpath, "/direct-optional" );
        assertContains( "Invalid classpath", classpath, "/direct-provided" );

        // referenced project: not required, but it's not a problem to have them included
        assertContains( "Invalid classpath", classpath, "/module-1" );
        // assertDoesNotContain( "Invalid classpath", classpath, "/refproject-compile" );
        // assertDoesNotContain( "Invalid classpath", classpath, "/refproject-sysdep" );
        assertDoesNotContain( "Invalid classpath", classpath, "/refproject-test" );
        assertDoesNotContain( "Invalid classpath", classpath, "/refproject-optional" );
        assertDoesNotContain( "Invalid classpath", classpath, "/refproject-provided" );

        // transitive dependencies from referenced projects
        assertContains( "Invalid classpath", classpath, "/deps-direct-compile" );
        assertDoesNotContain( "Invalid classpath", classpath, "/deps-direct-test" );
        assertDoesNotContain( "Invalid classpath", classpath, "/deps-direct-optional" );
        // @todo should this be included? see MNG-514
        assertDoesNotContain( "Invalid classpath", classpath, "/deps-direct-provided" );

        // transitive dependencies from referenced projects
        assertContains( "Invalid classpath", classpath, "/deps-refproject-compile" );
        assertDoesNotContain( "Invalid classpath", classpath, "/deps-refproject-test" );
        assertDoesNotContain( "Invalid classpath", classpath, "/deps-refproject-optional" );
        assertDoesNotContain( "Invalid classpath", classpath, "/deps-refproject-provided" );
    }

    public void testModule2Wtpmodules()
        throws Exception
    {
        executeMaven2();
        InputStream fis = new FileInputStream( new File( basedir, "module-2/.wtpmodules" ) );
        String wtpmodules = IOUtil.toString( fis );
        IOUtil.close( fis );

        // direct dependencies: include only runtime (also optional) dependencies
        assertContains( "Invalid wtpmodules", wtpmodules, "/direct-compile" );
        assertDoesNotContain( "Invalid wtpmodules", wtpmodules, "/direct-test" );
        assertContains( "Invalid wtpmodules", wtpmodules, "/direct-sysdep" );
        assertContains( "Invalid wtpmodules", wtpmodules, "/direct-optional" );
        assertDoesNotContain( "Invalid wtpmodules", wtpmodules, "/direct-provided" );

        // referenced project: only runtime deps
        assertContains( "Invalid wtpmodules", wtpmodules, "/module-1" );
        assertContains( "Invalid wtpmodules", wtpmodules, "/refproject-compile" );
        assertContains( "Invalid wtpmodules", wtpmodules, "refproject-sysdep" );
        assertDoesNotContain( "Invalid wtpmodules", wtpmodules, "/refproject-test" );
        assertDoesNotContain( "Invalid wtpmodules", wtpmodules, "/refproject-optional" );
        assertDoesNotContain( "Invalid wtpmodules", wtpmodules, "/refproject-provided" );

        // transitive dependencies from referenced projects
        assertContains( "Invalid wtpmodules", wtpmodules, "/deps-direct-compile" );
        assertDoesNotContain( "Invalid wtpmodules", wtpmodules, "/deps-direct-test" );
        assertDoesNotContain( "Invalid wtpmodules", wtpmodules, "/deps-direct-optional" );
        assertDoesNotContain( "Invalid wtpmodules", wtpmodules, "/deps-direct-provided" );

        // transitive dependencies from referenced projects
        assertContains( "Invalid wtpmodules", wtpmodules, "/deps-refproject-compile" );
        assertDoesNotContain( "Invalid wtpmodules", wtpmodules, "/deps-refproject-test" );
        assertDoesNotContain( "Invalid wtpmodules", wtpmodules, "/deps-refproject-optional" );
        assertDoesNotContain( "Invalid wtpmodules", wtpmodules, "/deps-refproject-provided" );
    }

}
