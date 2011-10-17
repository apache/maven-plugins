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

import java.util.Properties;

/**
 * Unit Tests for MyEclipse plugin
 * 
 * @author <a href="mailto:olivier.jacob@gmail.com">Olivier Jacob</a>
 */
public class MyEclipsePluginIT
    extends AbstractEclipsePluginIT
{
    /**
     * Web project, no spring/struts/hibernate capability, J2EE 1.3
     * 
     * @throws Exception
     */
    public void testProject01()
        throws Exception
    {
        doMyEclipseProjectTest( "project-myeclipse-01" );
    }

    /**
     * Web project, no spring/struts/hibernate capability, J2EE 1.4
     * 
     * @throws Exception
     */
    public void testProject02()
        throws Exception
    {
        doMyEclipseProjectTest( "project-myeclipse-02" );
    }

    /**
     * Simple project with Spring capability
     * 
     * @throws Exception
     */
    public void testProject03()
        throws Exception
    {
        doMyEclipseProjectTest( "project-myeclipse-03" );
    }

    /**
     * Simple project with Spring and Hibernate capabilities
     * 
     * @throws Exception
     */
    public void testProject04()
        throws Exception
    {
        doMyEclipseProjectTest( "project-myeclipse-04" );
    }

    /**
     * Simple project with additionalConfig
     * 
     * @throws Exception
     */
    public void testProject05()
        throws Exception
    {
        doMyEclipseProjectTest( "project-myeclipse-05" );
    }

    /**
     * Simple project with with spring configuration that points at non-existent directory
     * 
     * @throws Exception
     */
    public void testMyEclipseProject06MECLIPSE427()
        throws Exception
    {
        doMyEclipseProjectTest( "project-myeclipse-06-MECLIPSE-427" );
    }

    /**
     * Verifies spring files created with sub-projects (modules) module-1 should have spring bean files in the
     * .springBeans file. module-2 should not have spring bean files in the .springBeans file.
     * 
     * @throws Exception
     */
    public void testProject07MECLIPSE445()
        throws Exception
    {
        doMyEclipseProjectTest( "project-myeclipse-07-MECLIPSE-445" );
    }

    private void doMyEclipseProjectTest( String project )
        throws Exception
    {
        testProject( project, new Properties(), "myeclipse-clean", "myeclipse" );
    }

}
