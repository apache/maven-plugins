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
package org.apache.maven.plugin.eclipse.writers.wtp;

import java.io.File;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.eclipse.Constants;
import org.apache.maven.plugin.eclipse.Messages;
import org.apache.maven.plugin.eclipse.writers.AbstractEclipseWriter;
import org.apache.maven.plugin.ide.IdeDependency;
import org.apache.maven.plugin.ide.IdeUtils;
import org.apache.maven.plugin.ide.JeeUtils;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.XMLWriter;

/**
 * Base class to hold common constants used by extending classes.
 * 
 * @author <a href="mailto:rahul.thakur.xdev@gmail.com">Rahul Thakur</a>
 * @author <a href="mailto:fgiust@users.sourceforge.net">Fabrizio Giustina</a>
 */
public abstract class AbstractWtpResourceWriter
    extends AbstractEclipseWriter
{

    private static final String ELT_DEPENDENCY_OBJECT = "dependent-object"; //$NON-NLS-1$

    private static final String ELT_DEPENDENCY_TYPE = "dependency-type"; //$NON-NLS-1$

    private static final String ATTR_HANDLE = "handle"; //$NON-NLS-1$

    private static final String ELT_DEPENDENT_MODULE = "dependent-module"; //$NON-NLS-1$

    protected static final String ATTR_VALUE = "value"; //$NON-NLS-1$

    protected static final String ATTR_NAME = "name"; //$NON-NLS-1$

    protected static final String ELT_PROPERTY = "property"; //$NON-NLS-1$

    protected static final String ELT_VERSION = "version"; //$NON-NLS-1$

    protected static final String ATTR_MODULE_TYPE_ID = "module-type-id"; //$NON-NLS-1$

    protected static final String ATTR_SOURCE_PATH = "source-path"; //$NON-NLS-1$

    protected static final String ATTR_DEPLOY_PATH = "deploy-path"; //$NON-NLS-1$

    protected static final String ELT_WB_RESOURCE = "wb-resource"; //$NON-NLS-1$

    protected static final String ELT_MODULE_TYPE = "module-type"; //$NON-NLS-1$

    protected static final String ATTR_DEPLOY_NAME = "deploy-name"; //$NON-NLS-1$

    protected static final String ELT_WB_MODULE = "wb-module"; //$NON-NLS-1$

    protected static final String ATTR_MODULE_ID = "id"; //$NON-NLS-1$

    protected static final String ATTR_PROJECT_VERSION = "project-version"; //$NON-NLS-1$

    protected static final String ELT_PROJECT_MODULES = "project-modules"; //$NON-NLS-1$

    /**
     * @param project
     * @param writer
     * @throws MojoExecutionException
     */
    protected void writeModuleTypeAccordingToPackaging( MavenProject project, XMLWriter writer,
                                                        File buildOutputDirectory )
        throws MojoExecutionException
    {
        if ( Constants.PROJECT_PACKAGING_WAR.equals( config.getPackaging() ) ) //$NON-NLS-1$
        {
            writer.addAttribute( ATTR_MODULE_TYPE_ID, "jst.web" ); //$NON-NLS-1$

            writer.startElement( ELT_VERSION );
            String servletVersion;
            if ( config.getJeeVersion() != null )
            {
                servletVersion = JeeUtils.getJeeDescriptorFromJeeVersion( config.getJeeVersion() ).getServletVersion();
            }
            else
            {
                servletVersion = JeeUtils.resolveServletVersion( config.getProject() );
            }
            writer.writeText( servletVersion );
            writer.endElement();

            String contextRoot = config.getContextName();

            writer.startElement( ELT_PROPERTY );
            writer.addAttribute( ATTR_NAME, "context-root" ); //$NON-NLS-1$
            writer.addAttribute( ATTR_VALUE, contextRoot );
            writer.endElement();
        }
        else if ( Constants.PROJECT_PACKAGING_EJB.equals( config.getPackaging() ) ) //$NON-NLS-1$
        {
            writer.addAttribute( ATTR_MODULE_TYPE_ID, "jst.ejb" ); //$NON-NLS-1$

            writer.startElement( ELT_VERSION );

            String ejbVersion;
            if ( config.getJeeVersion() != null )
            {
                ejbVersion = JeeUtils.getJeeDescriptorFromJeeVersion( config.getJeeVersion() ).getEjbVersion();
            }
            else
            {
                ejbVersion = JeeUtils.resolveEjbVersion( config.getProject() );
            }
            writer.writeText( ejbVersion );

            writer.endElement();

            writer.startElement( ELT_PROPERTY );
            writer.addAttribute( ATTR_NAME, "java-output-path" ); //$NON-NLS-1$
            writer.addAttribute( ATTR_VALUE, "/" + //$NON-NLS-1$
                IdeUtils.toRelativeAndFixSeparator( config.getProject().getBasedir(), buildOutputDirectory, false ) );
            writer.endElement();

        }
        else if ( Constants.PROJECT_PACKAGING_EAR.equals( config.getPackaging() ) ) //$NON-NLS-1$
        {
            writer.addAttribute( ATTR_MODULE_TYPE_ID, "jst.ear" ); //$NON-NLS-1$

            writer.startElement( ELT_VERSION );
            String jeeVersion;
            if ( config.getJeeVersion() != null )
            {
                jeeVersion = JeeUtils.getJeeDescriptorFromJeeVersion( config.getJeeVersion() ).getJeeVersion();
            }
            else
            {
                jeeVersion = JeeUtils.resolveJeeVersion( config.getProject() );
            }
            writer.writeText( jeeVersion );
            writer.endElement();
        }
        else
        {
            // jar
            writer.addAttribute( ATTR_MODULE_TYPE_ID, "jst.utility" ); //$NON-NLS-1$

            writer.startElement( ELT_PROPERTY );
            writer.addAttribute( ATTR_NAME, "java-output-path" ); //$NON-NLS-1$
            writer.addAttribute( ATTR_VALUE, "/" + //$NON-NLS-1$
                IdeUtils.toRelativeAndFixSeparator( config.getProject().getBasedir(), buildOutputDirectory, false ) );
            writer.endElement();
        }
    }

    /**
     * Adds dependency for Eclipse WTP project.
     * 
     * @param writer
     * @param artifact
     * @param localRepository
     * @param basedir
     * @throws MojoExecutionException
     */
    protected void addDependency( XMLWriter writer, IdeDependency dep, ArtifactRepository localRepository,
                                  File basedir, String deployPath )
        throws MojoExecutionException
    {
        String handle;
        String dependentObject = null;
        String archiveName;

        // ejb's and wars must always be toplevel
        if ( Constants.PROJECT_PACKAGING_WAR.equals( dep.getType() )
            || Constants.PROJECT_PACKAGING_EJB.equals( dep.getType() ) )
        {
            deployPath = "/";
        }

        if ( dep.isReferencedProject() )
        {
            // <dependent-module deploy-path="/WEB-INF/lib"
            // handle="module:/resource/artifactid/artifactid">
            // <dependency-type>uses</dependency-type>
            // </dependent-module>

            handle = "module:/resource/" + dep.getEclipseProjectName() + "/" + dep.getEclipseProjectName(); //$NON-NLS-1$ //$NON-NLS-2$

            String archiveExtension = dep.getType();
            if ( Constants.PROJECT_PACKAGING_EJB.equals( dep.getType() ) )
            {
                dependentObject = "EjbModule_";
                // an EJB module is packed as a .jar file
                archiveExtension = Constants.PROJECT_PACKAGING_JAR;
            }
            else if ( Constants.PROJECT_PACKAGING_WAR.equals( dep.getType() ) )
            {
                dependentObject = "WebModule_";
            }
            archiveName = dep.getEclipseProjectName() + "." + archiveExtension;
        }
        else
        {
            // <dependent-module deploy-path="/WEB-INF/lib"
            // handle="module:/classpath/var/M2_REPO/cl/cl/2.1/cl-2.1.jar">
            // <dependency-type>uses</dependency-type>
            // </dependent-module>

            File artifactPath = dep.getFile();

            if ( artifactPath == null )
            {
                log.error( Messages.getString( "EclipsePlugin.artifactpathisnull", dep.getId() ) ); //$NON-NLS-1$
                return;
            }

            String fullPath = artifactPath.getPath();
            File repoFile = new File( fullPath );

            if ( dep.isSystemScoped() )
            {
                handle = "module:/classpath/lib/" //$NON-NLS-1$
                    + IdeUtils.toRelativeAndFixSeparator( config.getEclipseProjectDirectory(), repoFile, false );
            }
            else
            {
                File localRepositoryFile = new File( localRepository.getBasedir() );
                String relativePath = IdeUtils.toRelativeAndFixSeparator( localRepositoryFile, repoFile, false );

                if ( !new File( relativePath ).isAbsolute() )
                {
                    handle = "module:/classpath/var/M2_REPO/" //$NON-NLS-1$
                        + relativePath;
                }
                else
                {
                    handle = "module:/classpath/lib/" //$NON-NLS-1$
                        + IdeUtils.toRelativeAndFixSeparator( config.getEclipseProjectDirectory(), repoFile, false );
                }
            }
            if ( Constants.PROJECT_PACKAGING_EAR.equals( this.config.getPackaging() ) && !"/".equals( deployPath ) )
            {
                // This is a very ugly hack around a WTP bug! a delpoydir in the configuration file is duplicated.
                // a deploy dir like "lib" will be used as "lib/lib" the only workig workaround is to include a ..
                // in the archive name.
                archiveName = "../" + artifactPath.getName();
            }
            else
            {
                archiveName = artifactPath.getName();
            }
        }

        writer.startElement( ELT_DEPENDENT_MODULE );

        writer.addAttribute( "archiveName", archiveName );

        writer.addAttribute( ATTR_DEPLOY_PATH, deployPath ); //$NON-NLS-1$
        writer.addAttribute( ATTR_HANDLE, handle );

        if ( dependentObject != null && config.getWtpVersion() >= 2.0f )
        {
            writer.startElement( ELT_DEPENDENCY_OBJECT );
            writer.writeText( dependentObject + System.identityHashCode( dep ) );
            writer.endElement();
        }

        writer.startElement( ELT_DEPENDENCY_TYPE );
        writer.writeText( "uses" ); //$NON-NLS-1$
        writer.endElement();

        writer.endElement();
    }

    protected void writeWarOrEarResources( XMLWriter writer, MavenProject project, ArtifactRepository localRepository )
        throws MojoExecutionException
    {
        // use /WEB-INF/lib for war projects and / or the configured defaultLibBundleDir for ear projects
        String deployDir =
            IdeUtils.getPluginSetting( config.getProject(), JeeUtils.ARTIFACT_MAVEN_EAR_PLUGIN, "defaultLibBundleDir",
                                       "/" );

        if ( project.getPackaging().equals( Constants.PROJECT_PACKAGING_WAR ) )
        {
            deployDir = "/WEB-INF/lib";
        }
        // dependencies
        for ( int j = 0; j < config.getDeps().length; j++ )
        {
            IdeDependency dep = config.getDeps()[j];
            String type = dep.getType();

            // NB war is needed for ear projects, we suppose nobody adds a war dependency to a war/jar project
            // exclude test and provided and system dependencies outside the project
            if ( ( !dep.isTestDependency() && !dep.isProvided() && !dep.isSystemScopedOutsideProject( project ) )
                && ( Constants.PROJECT_PACKAGING_JAR.equals( type ) || Constants.PROJECT_PACKAGING_EJB.equals( type )
                    || "ejb-client".equals( type ) || Constants.PROJECT_PACKAGING_WAR.equals( type ) ) ) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
            {
                addDependency( writer, dep, localRepository, config.getProject().getBasedir(), deployDir );
            }
        }
    }

}
