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

    public void testProjectAjdt1()
        throws Exception
    {
        testProject( "project-ajdt1" );
    }

    public void testProjectAjdt2()
        throws Exception
    {
        testProject( "project-ajdt2" );
    }

    public void testProjectAjdt3()
        throws Exception
    {
        testProject( "project-ajdt3" );
    }

    public void testProjectAjdt4()
        throws Exception
    {
        testProject( "project-ajdt4" );
    }

    public void testProjectAjdt5()
        throws Exception
    {
        testProject( "project-ajdt5" );
    }

    public void testProjectAjdt6()
        throws Exception
    {
        testProject( "project-ajdt6" );
    }

    public void testProjectAjdt7()
        throws Exception
    {
        testProject( "project-ajdt7" );
    }

    public void testProjectAjdt8()
        throws Exception
    {
        testProject( "project-ajdt8" );
    }

    public void testProjectAjdt9()
        throws Exception
    {
        testProject( "project-ajdt9" );
    }
    
    public void testProjectAjdt10()
        throws Exception
    {
        testProject( "project-ajdt10-MECLIPSE-538" );
    }

  public void testProjectAjdt11()
        throws Exception
    {
        testProject( "project-ajdt-11-MECLIPSE-104" );
    }
    
}
