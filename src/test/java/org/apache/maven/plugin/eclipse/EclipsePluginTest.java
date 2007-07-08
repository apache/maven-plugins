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
package org.apache.maven.plugin.eclipse;

import java.util.Properties;

import org.apache.maven.plugin.MojoExecutionException;

/**
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @author <a href="mailto:fgiust@apache.org">Fabrizio Giustina</a>
 * @version $Id$
 */
public class EclipsePluginTest extends AbstractEclipsePluginTestCase
{
    protected void setUp()
    throws Exception
    {
        super.setUp();
    }

    
    
    /**
     * MECLIPSE-287 : dependencies with and without classifiers
     * MECLIPSE-151 : test jar source attachments
     * 
     * @throws Exception
     *             any exception thrown during test
     */
    /*@TODO temporarily disabled, since it randomly fails due to a different order for dependencies in classpath and
     wtpmodules. This is not a problem, since order could be ignored in this test, but we should rewrite the
     file-comparing step which at the moment just does line by line comparison   
    project 7 is affected by this as well.
    public void testProject33() throws Exception
    {
            testProject( "project-33" );
    }*/
    
    
    /*TODO: Add a test for downloadJavadocs. Currently, eclipse doesn't support having variables in the javadoc
     * path. This means that the expected .classpath can't match the final result as the result will
     * have the absolute path to the user's local repo.
     */
    /**
     * MECLIPSE-165: Ability to exclude filtered resources from eclipse's source directories
     */
    public void testProject34() throws Exception
    {
            testProject( "project-34" );
    }
}
