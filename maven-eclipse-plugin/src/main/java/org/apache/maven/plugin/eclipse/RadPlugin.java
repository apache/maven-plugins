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

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.eclipse.writers.EclipseWriterConfig;
import org.apache.maven.plugin.eclipse.writers.rad.RadApplicationXMLWriter;
import org.apache.maven.plugin.eclipse.writers.rad.RadEjbClasspathWriter;
import org.apache.maven.plugin.eclipse.writers.rad.RadJ2EEWriter;
import org.apache.maven.plugin.eclipse.writers.rad.RadLibCopier;
import org.apache.maven.plugin.eclipse.writers.rad.RadManifestWriter;
import org.apache.maven.plugin.eclipse.writers.rad.RadWebSettingsWriter;
import org.apache.maven.plugin.eclipse.writers.rad.RadWebsiteConfigWriter;
import org.apache.maven.plugin.ide.IdeDependency;
import org.apache.maven.plugin.ide.IdeUtils;
import org.apache.maven.plugin.ide.JeeUtils;
import org.apache.maven.project.MavenProject;

/**
 * Generates the rad-6 configuration files.
 *
 * @author Richard van Nieuwenhoven (patch submission)
 * @author jdcasey
 * @goal rad
 * @execute phase="generate-resources"
 */
public class RadPlugin
    extends EclipsePlugin
{

    private static final String COM_IBM_ETOOLS_J2EE_UI_LIB_DIR_BUILDER = "com.ibm.etools.j2ee.ui.LibDirBuilder";

    private static final String COM_IBM_ETOOLS_SITEEDIT_SITE_NAV_BUILDER = "com.ibm.etools.siteedit.SiteNavBuilder";

    private static final String COM_IBM_ETOOLS_SITEEDIT_SITE_UPDATE_BUILDER =
        "com.ibm.etools.siteedit.SiteUpdateBuilder";

    private static final String COM_IBM_ETOOLS_SITEEDIT_WEB_SITE_NATURE = "com.ibm.etools.siteedit.WebSiteNature";

    private static final String COM_IBM_ETOOLS_VALIDATION_VALIDATIONBUILDER =
        "com.ibm.etools.validation.validationbuilder";

    private static final String COM_IBM_ETOOLS_WEBPAGE_TEMPLATE_TEMPLATEBUILDER =
        "com.ibm.etools.webpage.template.templatebuilder";

    private static final String COM_IBM_ETOOLS_WEBPAGE_TEMPLATE_TEMPLATENATURE =
        "com.ibm.etools.webpage.template.templatenature";

    private static final String COM_IBM_ETOOLS_WEBTOOLS_ADDITIONS_JSPCOMPILATIONBUILDER =
        "com.ibm.etools.webtools.additions.jspcompilationbuilder";

    private static final String COM_IBM_ETOOLS_WEBTOOLS_ADDITIONS_LINKSBUILDER =
        "com.ibm.etools.webtools.additions.linksbuilder";

    private static final String COM_IBM_SSE_MODEL_STRUCTUREDBUILDER = "com.ibm.sse.model.structuredbuilder";

    private static final String COM_IBM_WTP_EJB_EJBNATURE = "com.ibm.wtp.ejb.EJBNature";

    private static final String COM_IBM_WTP_J2EE_EARNATURE = "com.ibm.wtp.j2ee.EARNature";

    private static final String COM_IBM_WTP_J2EE_LIB_COPY_BUILDER = "com.ibm.wtp.j2ee.LibCopyBuilder";

    private static final String COM_IBM_WTP_MIGRATION_MIGRATION_BUILDER = "com.ibm.wtp.migration.MigrationBuilder";

    private static final String COM_IBM_WTP_WEB_WEB_NATURE = "com.ibm.wtp.web.WebNature";

    private static final String NO_GENERATED_RESOURCE_DIRNAME = "none";

    private static final String ORG_ECLIPSE_JDT_CORE_JAVABUILDER = "org.eclipse.jdt.core.javabuilder";

    private static final String ORG_ECLIPSE_JDT_CORE_JAVANATURE = "org.eclipse.jdt.core.javanature";

    /**
     * The context root of the webapplication. This parameter is only used when the current project is a war project,
     * else it will be ignored.
     *
     * @parameter
     */
    private String warContextRoot;

    /**
     * Use this to specify a different generated resources folder than target/generated-resources/rad6. Set to "none" to
     * skip this folder generation.
     *
     * @parameter expression="${generatedResourceDirName}" default-value="target/generated-resources/rad6" since="2.4"
     */
    private String generatedResourceDirName;

    /**
     * @return Returns the warContextRoot.
     */
    public String getWarContextRoot()
    {
        return warContextRoot;
    }

    /**
     * @param warContextRoot The warContextRoot to set.
     */
    public void setWarContextRoot( String warContextRoot )
    {
        this.warContextRoot = warContextRoot;
    }

    /**
     * write all rad6 configuration files. <br/> <b> NOTE: This could change the config! </b>
     *
     * @see EclipsePlugin#writeConfiguration()
     * @param deps resolved dependencies to handle
     * @throws MojoExecutionException if the config files could not be written.
     */
    protected void writeConfigurationExtras( EclipseWriterConfig config )
        throws MojoExecutionException
    {
        super.writeConfigurationExtras( config );

        new RadJ2EEWriter().init( getLog(), config ).write();

        new RadWebSettingsWriter( this.warContextRoot ).init( getLog(), config ).write();

        new RadWebsiteConfigWriter().init( getLog(), config ).write();

        new RadApplicationXMLWriter().init( getLog(), config ).write();

        new RadLibCopier().init( getLog(), config ).write();

        new RadEjbClasspathWriter().init( getLog(), config ).write();
    }

    /**
     * make room for a Manifest file. use a generated resource for JARS and for WARS use the manifest in the
     * webapp/meta-inf directory.
     *
     * @throws MojoExecutionException
     */
    private void addManifestResource( EclipseWriterConfig config )
        throws MojoExecutionException
    {
        if ( isJavaProject() )
        {
            // special case must be done first because it can add stuff to the classpath that will be
            // written by the superclass
            new RadManifestWriter().init( getLog(), config ).write();
        }

        if ( isJavaProject() && !Constants.PROJECT_PACKAGING_EAR.equals( packaging )
            && !Constants.PROJECT_PACKAGING_WAR.equals( packaging )
            && !Constants.PROJECT_PACKAGING_EJB.equals( packaging )
            && !NO_GENERATED_RESOURCE_DIRNAME.equals( this.generatedResourceDirName ) )
        {

            String generatedResourceDir =
                this.project.getBasedir().getAbsolutePath() + File.separatorChar + this.generatedResourceDirName;

            String metainfDir = generatedResourceDir + File.separatorChar + "META-INF";

            new File( metainfDir ).mkdirs();

            final Resource resource = new Resource();

            getLog().debug( "Adding " + this.generatedResourceDirName + " to resources" );

            resource.setDirectory( generatedResourceDir );

            this.executedProject.addResource( resource );
        }

        if ( Constants.PROJECT_PACKAGING_WAR.equals( packaging ) )
        {
            new File( getWebContentBaseDirectory( config ) + File.separatorChar + "META-INF" ).mkdirs();
        }
    }

    /**
     * Returns absolute path to the web content directory based on configuration of the war plugin or default one
     * otherwise.
     *
     * @param project
     * @return absolute directory path as String
     * @throws MojoExecutionException
     */
    private static String getWebContentBaseDirectory( EclipseWriterConfig config )
        throws MojoExecutionException
    {
        // getting true location of web source dir from config
        File warSourceDirectory =
            new File( IdeUtils.getPluginSetting( config.getProject(), JeeUtils.ARTIFACT_MAVEN_WAR_PLUGIN,
                                                 "warSourceDirectory", "src/main/webapp" ) );
        // getting real and correct path to the web source dir
        String webContentDir =
            IdeUtils.toRelativeAndFixSeparator( config.getEclipseProjectDirectory(), warSourceDirectory, false );

        // getting the path to meta-inf base dir
        String result = config.getProject().getBasedir().getAbsolutePath() + File.separatorChar + webContentDir;

        return result;
    }

    /**
     * overwite the default builders with the builders required by RAD6.
     *
     * @param packaging packaging-type (jar,war,ejb,ear)
     */
    protected void fillDefaultBuilders( String packaging )
    {
        super.fillDefaultBuilders( packaging );

        ArrayList buildcommands = new ArrayList();
        if ( Constants.PROJECT_PACKAGING_EAR.equals( packaging ) )
        {
            buildcommands.add( COM_IBM_ETOOLS_VALIDATION_VALIDATIONBUILDER );
            buildcommands.add( COM_IBM_SSE_MODEL_STRUCTUREDBUILDER );
        }
        else if ( Constants.PROJECT_PACKAGING_WAR.equals( packaging ) )
        {
            buildcommands.add( COM_IBM_WTP_MIGRATION_MIGRATION_BUILDER );
            buildcommands.add( ORG_ECLIPSE_JDT_CORE_JAVABUILDER );
            buildcommands.add( COM_IBM_ETOOLS_J2EE_UI_LIB_DIR_BUILDER );
            buildcommands.add( COM_IBM_ETOOLS_WEBTOOLS_ADDITIONS_LINKSBUILDER );
            buildcommands.add( COM_IBM_ETOOLS_WEBPAGE_TEMPLATE_TEMPLATEBUILDER );
            buildcommands.add( COM_IBM_ETOOLS_SITEEDIT_SITE_NAV_BUILDER );
            buildcommands.add( COM_IBM_ETOOLS_SITEEDIT_SITE_UPDATE_BUILDER );
            buildcommands.add( COM_IBM_ETOOLS_VALIDATION_VALIDATIONBUILDER );
            buildcommands.add( COM_IBM_WTP_J2EE_LIB_COPY_BUILDER );
            buildcommands.add( COM_IBM_ETOOLS_WEBTOOLS_ADDITIONS_JSPCOMPILATIONBUILDER );
            buildcommands.add( COM_IBM_SSE_MODEL_STRUCTUREDBUILDER );
        }
        else if ( Constants.PROJECT_PACKAGING_EJB.equals( packaging ) )
        {
            buildcommands.add( ORG_ECLIPSE_JDT_CORE_JAVABUILDER );
            buildcommands.add( COM_IBM_ETOOLS_VALIDATION_VALIDATIONBUILDER );
            buildcommands.add( COM_IBM_WTP_J2EE_LIB_COPY_BUILDER );
            buildcommands.add( COM_IBM_SSE_MODEL_STRUCTUREDBUILDER );
        }
        else if ( isJavaProject() )
        {
            buildcommands.add( ORG_ECLIPSE_JDT_CORE_JAVABUILDER );
            buildcommands.add( COM_IBM_SSE_MODEL_STRUCTUREDBUILDER );
        }
        setBuildcommands( buildcommands );
    }

    /**
     * overwite the default natures with the natures required by RAD6.
     *
     * @param packaging packaging-type (jar,war,ejb,ear)
     */
    protected void fillDefaultNatures( String packaging )
    {
        super.fillDefaultNatures( packaging );

        ArrayList projectnatures = new ArrayList();
        if ( Constants.PROJECT_PACKAGING_EAR.equals( packaging ) )
        {
            projectnatures.add( COM_IBM_WTP_J2EE_EARNATURE );
        }
        else if ( Constants.PROJECT_PACKAGING_WAR.equals( packaging ) )
        {
            projectnatures.add( COM_IBM_WTP_WEB_WEB_NATURE );
            projectnatures.add( ORG_ECLIPSE_JDT_CORE_JAVANATURE );
            projectnatures.add( COM_IBM_ETOOLS_SITEEDIT_WEB_SITE_NATURE );
            projectnatures.add( COM_IBM_ETOOLS_WEBPAGE_TEMPLATE_TEMPLATENATURE );
        }
        else if ( Constants.PROJECT_PACKAGING_EJB.equals( packaging ) )
        {
            projectnatures.add( COM_IBM_WTP_EJB_EJBNATURE );
            projectnatures.add( ORG_ECLIPSE_JDT_CORE_JAVANATURE );
        }
        else if ( isJavaProject() )
        {
            projectnatures.add( ORG_ECLIPSE_JDT_CORE_JAVANATURE );
        }
        setProjectnatures( projectnatures );
    }

    /**
     * Utility method that locates a project producing the given artifact.
     *
     * @param artifact the artifact a project should produce.
     * @return <code>true</code> if the artifact is produced by a reactor projectart.
     */
    protected boolean isAvailableAsAReactorProject( Artifact artifact )
    {
        if ( this.reactorProjects != null
            && ( Constants.PROJECT_PACKAGING_JAR.equals( artifact.getType() )
                || Constants.PROJECT_PACKAGING_EJB.equals( artifact.getType() ) || Constants.PROJECT_PACKAGING_WAR.equals( artifact.getType() ) ) )
        {
            for ( Iterator iter = this.reactorProjects.iterator(); iter.hasNext(); )
            {
                MavenProject reactorProject = (MavenProject) iter.next();

                if ( reactorProject.getGroupId().equals( artifact.getGroupId() )
                    && reactorProject.getArtifactId().equals( artifact.getArtifactId() ) )
                {
                    if ( reactorProject.getVersion().equals( artifact.getVersion() ) )
                    {
                        return true;
                    }
                    else
                    {
                        getLog().info(
                                       "Artifact "
                                           + artifact.getId()
                                           + " already available as a reactor project, but with different version. Expected: "
                                           + artifact.getVersion() + ", found: " + reactorProject.getVersion() );
                    }
                }
            }
        }
        return false;
    }

    /**
     * WARNING: The manifest resources added here will not have the benefit of the dependencies of the project, since
     * that's not provided in the setup() apis...
     */
    protected void setupExtras()
        throws MojoExecutionException
    {
        super.setupExtras();

        IdeDependency[] deps = doDependencyResolution();

        EclipseWriterConfig config = createEclipseWriterConfig( deps );

        addManifestResource( config );
    }

    /**
     * {@inheritDoc}
     */
    public String getProjectNameForArifact( Artifact artifact ) {
        return artifact.getArtifactId();
    }
}
