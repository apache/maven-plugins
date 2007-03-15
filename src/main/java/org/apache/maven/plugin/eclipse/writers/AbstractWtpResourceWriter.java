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
package org.apache.maven.plugin.eclipse.writers;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.eclipse.Messages;
import org.apache.maven.plugin.ide.IdeDependency;
import org.apache.maven.plugin.ide.IdeUtils;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.XMLWriter;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Base class to hold common constants used by extending classes.
 * 
 * @author <a href="mailto:rahul.thakur.xdev@gmail.com">Rahul Thakur</a>
 * @author <a href="mailto:fgiust@users.sourceforge.net">Fabrizio Giustina</a>
 */
public abstract class AbstractWtpResourceWriter extends AbstractEclipseWriter
{

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

    protected static final String ARTIFACT_MAVEN_WAR_PLUGIN = "maven-war-plugin"; //$NON-NLS-1$

    /**
     * @param project
     * @param writer
     * @throws MojoExecutionException
     */
    protected void writeModuleTypeAccordingToPackaging( MavenProject project, XMLWriter writer,
                                                        File buildOutputDirectory ) throws MojoExecutionException
    {
        if ( "war".equals( project.getPackaging() ) ) //$NON-NLS-1$
        {
            writer.addAttribute( ATTR_MODULE_TYPE_ID, "jst.web" ); //$NON-NLS-1$

            writer.startElement( ELT_VERSION );

            writer.writeText( resolveServletVersion() );
            writer.endElement();

            // use finalName as context root only if it has been explicitely set
            String contextRoot = project.getArtifactId();
            String finalName = project.getBuild().getFinalName();
            if ( !finalName.equals( project.getArtifactId() + "-" + project.getVersion() ) ) //$NON-NLS-1$
            {
                contextRoot = finalName;
            }

            writer.startElement( ELT_PROPERTY );
            writer.addAttribute( ATTR_NAME, "context-root" ); //$NON-NLS-1$
            writer.addAttribute( ATTR_VALUE, contextRoot );
            writer.endElement();
        }
        else if ( "ejb".equals( config.getProject().getPackaging() ) ) //$NON-NLS-1$
        {
            writer.addAttribute( ATTR_MODULE_TYPE_ID, "jst.ejb" ); //$NON-NLS-1$

            writer.startElement( ELT_VERSION );
            writer.writeText( resolveEjbVersion() );

            writer.endElement();

            writer.startElement( ELT_PROPERTY );
            writer.addAttribute( ATTR_NAME, "java-output-path" ); //$NON-NLS-1$
            writer.addAttribute( ATTR_VALUE, "/" + //$NON-NLS-1$
                            IdeUtils.toRelativeAndFixSeparator( config.getProject().getBasedir(), buildOutputDirectory,
                                                                false ) );
            writer.endElement();

        }
        else if ( "ear".equals( config.getProject().getPackaging() ) ) //$NON-NLS-1$
        {
            writer.addAttribute( ATTR_MODULE_TYPE_ID, "jst.ear" ); //$NON-NLS-1$

            writer.startElement( ELT_VERSION );
            writer.writeText( resolveJ2eeVersion() );
            writer.endElement();
        }
        else
        {
            // jar
            writer.addAttribute( ATTR_MODULE_TYPE_ID, "jst.utility" ); //$NON-NLS-1$

            writer.startElement( ELT_PROPERTY );
            writer.addAttribute( ATTR_NAME, "java-output-path" ); //$NON-NLS-1$
            writer.addAttribute( ATTR_VALUE, "/" + //$NON-NLS-1$
                            IdeUtils.toRelativeAndFixSeparator( config.getProject().getBasedir(), buildOutputDirectory,
                                                                false ) );
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
    protected void addDependency( XMLWriter writer, IdeDependency dep, ArtifactRepository localRepository, File basedir )
        throws MojoExecutionException
    {
        String handle;

        if ( dep.isReferencedProject() )
        {
            // <dependent-module deploy-path="/WEB-INF/lib"
            // handle="module:/resource/artifactid/artifactid">
            // <dependency-type>uses</dependency-type>
            // </dependent-module>

            handle = "module:/resource/" + dep.getArtifactId() + "/" + dep.getArtifactId(); //$NON-NLS-1$ //$NON-NLS-2$
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
                handle =
                    "module:/classpath/lib/" //$NON-NLS-1$
                                    + IdeUtils.toRelativeAndFixSeparator( config.getEclipseProjectDirectory(),
                                                                          repoFile, false );
            }
            else
            {
                File localRepositoryFile = new File( localRepository.getBasedir() );

                handle = "module:/classpath/var/M2_REPO/" //$NON-NLS-1$
                                + IdeUtils.toRelativeAndFixSeparator( localRepositoryFile, repoFile, false );
            }
        }

        writer.startElement( ELT_DEPENDENT_MODULE );

        writer.addAttribute( ATTR_DEPLOY_PATH, "/WEB-INF/lib" ); //$NON-NLS-1$
        writer.addAttribute( ATTR_HANDLE, handle );

        writer.startElement( ELT_DEPENDENCY_TYPE );
        writer.writeText( "uses" ); //$NON-NLS-1$
        writer.endElement();

        writer.endElement();
    }

    protected void writeWarOrEarResources( XMLWriter writer, MavenProject project, ArtifactRepository localRepository )
        throws MojoExecutionException
    {

        // dependencies
        for ( int j = 0; j < config.getDeps().length; j++ )
        {
            IdeDependency dep = config.getDeps()[j];
            String type = dep.getType();

            // NB war is needed for ear projects, we suppose nobody adds a war dependency to a war/jar project
            // exclude test and provided deps
            if ( ( !dep.isTestDependency() && !dep.isProvided() )
                            && ( "jar".equals( type ) || "ejb".equals( type ) || "ejb-client".equals( type ) || "war".equals( type ) ) ) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
            {
                addDependency( writer, dep, localRepository, config.getProject().getBasedir() );
            }
        }
    }

    protected String resolveServletVersion()
    {
        String[] artifactIds = new String[] { "servlet-api", "servletapi", "geronimo-spec-servlet" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        String version = IdeUtils.getDependencyVersion( artifactIds, config.getProject().getDependencies(), 3 );

        // For new Geronimo APIs, the version of the artifact isn't the one of the spec
        if ( version == null )
        {
            if ( IdeUtils.getDependencyVersion( new String[] { "geronimo-servlet_2.4_spec" },
                                                config.getProject().getDependencies(), 3 ) != null )
                return "2.4";
        }
        if ( version == null )
        {
            if ( IdeUtils.getDependencyVersion( new String[] { "geronimo-servlet_2.5_spec" },
                                                config.getProject().getDependencies(), 3 ) != null )
                return "2.5";
        }

        if ( version == null )
        {
            // No servlet dependency detected. Try to resolve the servlet
            // version from J2EE/JEE.
            String versionJEE = resolveJ2eeVersionWithoutDefault();

            if ( versionJEE != null )
            {
                // A J2EE version was found, now determine the servlet
                // version to be used from it.
                Map conversionTable = new HashMap();
                conversionTable.put( "1.3", "2.3" );
                conversionTable.put( "1.4", "2.4" );
                conversionTable.put( "5", "2.5" );
                if ( conversionTable.containsKey( versionJEE ) )
                {
                    version = (String) conversionTable.get( versionJEE );
                }
            }
        }
        return version == null ? "2.4" : version; //$NON-NLS-1$
    }

    protected String resolveEjbVersion()
    {
        String[] artifactIds = new String[] { "ejb", "geronimo-spec-ejb" }; //$NON-NLS-1$

        String version = IdeUtils.getDependencyVersion( artifactIds, config.getProject().getDependencies(), 3 );

        // For new Geronimo APIs, the version of the artifact isn't the one of the spec
        if ( version == null )
        {
            if ( IdeUtils.getDependencyVersion( new String[] { "geronimo-ejb_2.1_spec" },
                                                config.getProject().getDependencies(), 3 ) != null )
                return "2.1";
        }
        if ( version == null )
        {
            if ( IdeUtils.getDependencyVersion( new String[] { "geronimo-ejb_3.0_spec" },
                                                config.getProject().getDependencies(), 3 ) != null )
                return "3.0";
        }

        if ( version == null )
        {
            // No ejb dependency detected. Try to resolve the ejb
            // version from J2EE/JEE.
            String versionJEE = resolveJ2eeVersionWithoutDefault();

            if ( versionJEE != null )
            {
                // A J2EE version was found, now determine the ejb
                // version to be used from it.
                Map conversionTable = new HashMap();
                conversionTable.put( "1.3", "2.0" );
                conversionTable.put( "1.4", "2.1" );
                conversionTable.put( "5", "3.0" );
                if ( conversionTable.containsKey( versionJEE ) )
                {
                    version = (String) conversionTable.get( versionJEE );
                }
            }
        }
        return version == null ? "2.1" : version; //$NON-NLS-1$
    }

    protected String resolveJ2eeVersion()
    {
        String version = resolveJ2eeVersionWithoutDefault();

        return version == null ? "1.3" : version; //$NON-NLS-1$
    }

    protected String resolveJ2eeVersionWithoutDefault()
    {
        String[] artifactIds = new String[] { "javaee-api", "j2ee", "geronimo-spec-j2ee" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        String version = IdeUtils.getDependencyVersion( artifactIds, config.getProject().getDependencies(), 3 );

        // For new Geronimo APIs, the version of the artifact isn't the one of the spec
        if ( version == null )
        {
            if ( IdeUtils.getDependencyVersion( new String[] { "geronimo-j2ee_1.4_spec" },
                                                config.getProject().getDependencies(), 3 ) != null )
                return "1.4";
        }

        return version;
    }

    protected String resolveJavaVersion()
    {
        String version = IdeUtils.getCompilerTargetVersion( config.getProject() );
        if ( version == null )
        {
            version = IdeUtils.getCompilerSourceVersion( config.getProject() );
        }

        if ( "1.5".equals( version ) ) //$NON-NLS-1$ //$NON-NLS-2$
        {
            version = "5.0";// see MECLIPSE-47 eclipse only accept 5.0 as a valid version //$NON-NLS-1$
        }
        else if ( "1.6".equals( version ) ) //$NON-NLS-1$ //$NON-NLS-2$
        {
            version = "6.0";
        }
        else if ( version != null && version.length() == 1 )
        {
            version = version + ".0";// 5->5.0 6->6.0 7->7.0 //$NON-NLS-1$
        }

        return version == null ? "1.4" : version; //$NON-NLS-1$
    }

}
