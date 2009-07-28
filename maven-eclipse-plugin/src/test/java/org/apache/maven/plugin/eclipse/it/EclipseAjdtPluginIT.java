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

/**
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @author <a href="mailto:fgiust@apache.org">Fabrizio Giustina</a>
 * @version $Id$
 */
public class EclipseAjdtPluginIT
    extends AbstractEclipsePluginIT
{
    protected void setUp()
        throws Exception
    {
        super.setUp();
    }

    public void testProjectAjdt01()
        throws Exception
    {
        testProject( "project-ajdt-01" );
    }

    public void testProjectAjdt02()
        throws Exception
    {
        testProject( "project-ajdt-02" );
    }

    public void testProjectAjdt03()
        throws Exception
    {
        testProject( "project-ajdt-03" );
    }

    public void testProjectAjdt04()
        throws Exception
    {
        testProject( "project-ajdt-04" );
    }

    public void testProjectAjdt05()
        throws Exception
    {
        testProject( "project-ajdt-05" );
    }

    public void testProjectAjdt06()
        throws Exception
    {
        testProject( "project-ajdt-06" );
    }

    public void testProjectAjdt07()
        throws Exception
    {
        testProject( "project-ajdt-07" );
    }

    public void testProjectAjdt08()
        throws Exception
    {
        testProject( "project-ajdt-08" );
    }

    public void testProjectAjdt09()
        throws Exception
    {
        testProject( "project-ajdt-09" );
    }

    public void testProjectAjdt10()
        throws Exception
    {
        testProject( "project-ajdt-10-MECLIPSE-538" );
    }

    public void testProjectAjdt11()
        throws Exception
    {
        testProject( "project-ajdt-11-MECLIPSE-104" );
    }

}
