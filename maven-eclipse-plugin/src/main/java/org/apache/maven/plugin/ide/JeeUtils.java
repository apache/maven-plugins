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
package org.apache.maven.plugin.ide;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.maven.project.MavenProject;

public class JeeUtils
{
    public static final String ARTIFACT_MAVEN_EAR_PLUGIN = "org.apache.maven.plugins:maven-ear-plugin"; //$NON-NLS-1$

    public static final String ARTIFACT_MAVEN_WAR_PLUGIN = "org.apache.maven.plugins:maven-war-plugin"; //$NON-NLS-1$

    private static final Map ejbMap = new HashMap();

    private static final Map jeeMap = new HashMap();

    private static final Map jspMap = new HashMap();

    private static final Map servletMap = new HashMap();

    /** Names of artifacts of ejb APIs. */
    // private static final String[] EJB_API_ARTIFACTS = new String[] { "ejb", "ejb-api", "geronimo-spec-ejb" };
    // //$NON-NLS-1$
    static
    {
        addJEE( JeeDescriptor.JEE_5_0, JeeDescriptor.EJB_3_0, JeeDescriptor.SERVLET_2_5, JeeDescriptor.JSP_2_1 );
        addJEE( JeeDescriptor.JEE_1_4, JeeDescriptor.EJB_2_1, JeeDescriptor.SERVLET_2_4, JeeDescriptor.JSP_2_0 );
        addJEE( JeeDescriptor.JEE_1_3, JeeDescriptor.EJB_2_0, JeeDescriptor.SERVLET_2_3, JeeDescriptor.JSP_1_2 );
        addJEE( JeeDescriptor.JEE_1_2, JeeDescriptor.EJB_1_1, JeeDescriptor.SERVLET_2_2, JeeDescriptor.JSP_1_1 );

    }

    /**
     * Returns the JEEDescriptor associated to an EJB specifications version.
     * 
     * @param ejbVersion An EJB version as defined by constants JeeDescriptor.EJB_x_x
     * @return a JEEDescriptor
     */
    public final static JeeDescriptor getJeeDescriptorFromEjbVersion( String ejbVersion )
    {
        if ( ejbMap.containsKey( ejbVersion ) )
            return (JeeDescriptor) ejbMap.get( ejbVersion );
        else
            return null;
    }

    /**
     * Returns the JEEDescriptor associated to a JEE specifications version.
     * 
     * @param jeeVersion A JEE version as defined by constants JeeDescriptor.JEE_x_x
     * @return a JEEDescriptor
     */
    public final static JeeDescriptor getJeeDescriptorFromJeeVersion( String jeeVersion )
    {
        if ( jeeMap.containsKey( jeeVersion ) )
            return (JeeDescriptor) jeeMap.get( jeeVersion );
        else
            return null;
    }

    /**
     * Returns the JEEDescriptor associated to a JSP specifications version.
     * 
     * @param jspVersion A JSP version as defined by constants JeeDescriptor.JSP_x_x
     * @return a JEEDescriptor
     */
    public final static JeeDescriptor getJeeDescriptorFromJspVersion( String jspVersion )
    {
        if ( jspMap.containsKey( jspVersion ) )
            return (JeeDescriptor) jspMap.get( jspVersion );
        else
            return null;
    }

    /**
     * Returns the JEEDescriptor associated to a Servlet specifications version.
     * 
     * @param servletVersion A Servlet version as defined by constants JeeDescriptor.SERVLET_x_x
     * @return a JEEDescriptor
     */
    public final static JeeDescriptor getJeeDescriptorFromServletVersion( String servletVersion )
    {
        if ( servletMap.containsKey( servletVersion ) )
            return (JeeDescriptor) servletMap.get( servletVersion );
        else
            return null;
    }

    /**
     * Search in dependencies a version of EJB APIs (or of JEE APIs).
     * 
     * @param artifacts The list of dependencies where we search the information
     * @return An EJB version as defined by constants JeeDescriptor.EJB_x_x. By default, if nothing is found, returns
     *         JeeDescriptor.EJB_2_1.
     */
    public static String resolveEjbVersion( MavenProject project )
    {
        String version = findEjbVersionInDependencies( project );

        if ( version == null )
        {
            // No ejb dependency detected. Try to resolve the ejb
            // version from J2EE/JEE.
            JeeDescriptor descriptor = getJeeDescriptorFromJeeVersion( findJeeVersionInDependencies( project ) );
            if ( descriptor != null )
                version = descriptor.getEjbVersion();
        }
        return version == null ? JeeDescriptor.EJB_2_1 : version; //$NON-NLS-1$
    }

    /**
     * Search in dependencies a version of JEE APIs.
     * 
     * @param artifacts The list of dependencies where we search the information
     * @return A JEE version as defined by constants JeeDescriptor.JEE_x_x. By default, if nothing is found, returns
     *         JeeDescriptor.JEE_1_4.
     */
    public static String resolveJeeVersion( MavenProject project )
    {
        // try to find version in dependencies
        String version = findJeeVersionInDependencies( project );
        if ( version == null )
        {
            // No JEE dependency detected. Try to resolve the JEE
            // version from EJB.
            JeeDescriptor descriptor = getJeeDescriptorFromEjbVersion( findEjbVersionInDependencies( project ) );
            if ( descriptor != null )
                version = descriptor.getJeeVersion();
        }
        if ( version == null )
        {
            // No JEE dependency detected. Try to resolve the JEE
            // version from SERVLET.
            JeeDescriptor descriptor = getJeeDescriptorFromServletVersion( findServletVersionInDependencies( project ) );
            if ( descriptor != null )
                version = descriptor.getJeeVersion();
        }
        if ( version == null )
        {
            // No JEE dependency detected. Try to resolve the JEE
            // version from JSP.
            JeeDescriptor descriptor = getJeeDescriptorFromJspVersion( findJspVersionInDependencies( project ) );
            if ( descriptor != null )
                version = descriptor.getJeeVersion();
        }
        return version == null ? JeeDescriptor.JEE_1_4 : version; //$NON-NLS-1$
    }

    /**
     * Search in dependencies a version of JSP APIs (or from JEE APIs, or from Servlet APIs).
     * 
     * @param artifacts The list of dependencies where we search the information
     * @return A JSP version as defined by constants JeeDescriptor.JSP_x_x. By default, if nothing is found, returns
     *         JeeDescriptor.JSP_2_0.
     */

    public static String resolveJspVersion( MavenProject project )
    {
        String version = findJspVersionInDependencies( project );

        if ( version == null )
        {
            // No jsp dependency detected. Try to resolve the jsp
            // version from J2EE/JEE.
            JeeDescriptor descriptor = getJeeDescriptorFromJeeVersion( findJeeVersionInDependencies( project ) );
            if ( descriptor != null )
                version = descriptor.getJspVersion();
        }
        if ( version == null )
        {
            // No jsp dependency detected. Try to resolve the jsp
            // version from Servlet.
            JeeDescriptor descriptor = getJeeDescriptorFromServletVersion( findServletVersionInDependencies( project ) );
            if ( descriptor != null )
                version = descriptor.getJspVersion();
        }
        return version == null ? JeeDescriptor.JSP_2_0 : version; //$NON-NLS-1$
    }

    /**
     * Search in dependencies a version of Servlet APIs (or of JEE APIs).
     * 
     * @param artifacts The list of dependencies where we search the information
     * @return A SERVLET version as defined by constants JeeDescriptor.SERLVET_x_x. By default, if nothing is found,
     *         returns JeeDescriptor.SERVLET_2_4.
     */
    public static String resolveServletVersion( MavenProject project )
    {
        String version = findServletVersionInDependencies( project );

        if ( version == null )
        {
            // No servlet dependency detected. Try to resolve the servlet
            // version from J2EE/JEE.
            JeeDescriptor descriptor = getJeeDescriptorFromJeeVersion( findJeeVersionInDependencies( project ) );
            if ( descriptor != null )
                version = descriptor.getServletVersion();
        }
        return version == null ? JeeDescriptor.SERVLET_2_4 : version; //$NON-NLS-1$
    }

    private static void addJEE( String jeeVersion, String ejbVersion, String servletVersion, String jspVersion )
    {
        JeeDescriptor descriptor = new JeeDescriptor( jeeVersion, ejbVersion, servletVersion, jspVersion );
        jeeMap.put( jeeVersion, descriptor );
        ejbMap.put( ejbVersion, descriptor );
        servletMap.put( servletVersion, descriptor );
        jspMap.put( jspVersion, descriptor );
    }

    private static String findEjbVersionInDependencies( MavenProject project )
    {

        String version =
            IdeUtils.getArtifactVersion( new String[] { "ejb", "ejb-api", "geronimo-spec-ejb" },
                                         project.getDependencies(), 3 );
        // For new Geronimo APIs, the version of the artifact isn't the one of the spec
        if ( version == null )
        {
            if ( IdeUtils.getArtifactVersion( new String[] { "geronimo-ejb_2.1_spec" }, project.getDependencies(), 3 ) != null )
                return JeeDescriptor.EJB_2_1;
        }
        if ( version == null )
        {
            if ( IdeUtils.getArtifactVersion( new String[] { "geronimo-ejb_3.0_spec" }, project.getDependencies(), 3 ) != null )
                return JeeDescriptor.EJB_3_0;
        }

        // if no version found try dependencies of referenced projects
        if ( version == null )
        {
            Iterator it = project.getProjectReferences().keySet().iterator();
            while ( it.hasNext() )
            {
                Object key = it.next();
                MavenProject refProject = (MavenProject) project.getProjectReferences().get( key );
                version = findEjbVersionInDependencies( refProject );
                if ( version != null ) // version found in dependencies
                {
                    break;
                }
            }
        }
        return version;
    }

    private static String findJeeVersionInDependencies( MavenProject project )
    {
        String version =
            IdeUtils.getArtifactVersion( new String[] { "javaee-api", "j2ee", "geronimo-spec-j2ee" },
                                         project.getDependencies(), 3 );

        // For new Geronimo APIs, the version of the artifact isn't the one of the spec
        if ( version == null )
        {
            if ( IdeUtils.getArtifactVersion( new String[] { "geronimo-j2ee_1.4_spec" }, project.getDependencies(), 3 ) != null )
                return JeeDescriptor.JEE_1_4;
        }

        // if no version found try dependencies of referenced projects
        if ( version == null )
        {
            Iterator it = project.getProjectReferences().keySet().iterator();
            while ( it.hasNext() )
            {
                Object key = it.next();
                MavenProject refProject = (MavenProject) project.getProjectReferences().get( key );
                version = findJeeVersionInDependencies( refProject );
                if ( version != null ) // version found in dependencies
                {
                    break;
                }
            }
        }

        return version;
    }

    private static String findJspVersionInDependencies( MavenProject project )
    {
        return null;
    }

    private static String findServletVersionInDependencies( MavenProject project )
    {
        String version =
            IdeUtils.getArtifactVersion( new String[] { "servlet-api", "servletapi", "geronimo-spec-servlet" },
                                         project.getDependencies(), 3 );

        // For new Geronimo APIs, the version of the artifact isn't the one of the spec
        if ( version == null )
        {
            if ( IdeUtils.getArtifactVersion( new String[] { "geronimo-servlet_2.4_spec" }, project.getDependencies(),
                                              3 ) != null )
                return JeeDescriptor.SERVLET_2_4;
        }
        if ( version == null )
        {
            if ( IdeUtils.getArtifactVersion( new String[] { "geronimo-servlet_2.5_spec" }, project.getDependencies(),
                                              3 ) != null )
                return JeeDescriptor.SERVLET_2_5;
        }

        // if no version found try dependencies of referenced projects
        if ( version == null )
        {
            Iterator it = project.getProjectReferences().keySet().iterator();
            while ( it.hasNext() )
            {
                Object key = it.next();
                MavenProject refProject = (MavenProject) project.getProjectReferences().get( key );
                version = findServletVersionInDependencies( refProject );
                if ( version != null ) // version found in dependencies
                {
                    break;
                }
            }
        }
        return version;
    }
}
