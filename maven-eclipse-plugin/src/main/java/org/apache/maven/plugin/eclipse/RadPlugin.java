package org.apache.maven.plugin.eclipse;

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
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

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

    private static final String COM_IBM_ETOOLS_SITEEDIT_SITE_UPDATE_BUILDER = "com.ibm.etools.siteedit.SiteUpdateBuilder";

    private static final String COM_IBM_ETOOLS_SITEEDIT_WEB_SITE_NATURE = "com.ibm.etools.siteedit.WebSiteNature";

    private static final String COM_IBM_ETOOLS_VALIDATION_VALIDATIONBUILDER = "com.ibm.etools.validation.validationbuilder";

    private static final String COM_IBM_ETOOLS_WEBPAGE_TEMPLATE_TEMPLATEBUILDER = "com.ibm.etools.webpage.template.templatebuilder";

    private static final String COM_IBM_ETOOLS_WEBPAGE_TEMPLATE_TEMPLATENATURE = "com.ibm.etools.webpage.template.templatenature";

    private static final String COM_IBM_ETOOLS_WEBTOOLS_ADDITIONS_JSPCOMPILATIONBUILDER = "com.ibm.etools.webtools.additions.jspcompilationbuilder";

    private static final String COM_IBM_ETOOLS_WEBTOOLS_ADDITIONS_LINKSBUILDER = "com.ibm.etools.webtools.additions.linksbuilder";

    private static final String COM_IBM_SSE_MODEL_STRUCTUREDBUILDER = "com.ibm.sse.model.structuredbuilder";

    private static final String COM_IBM_WTP_EJB_EJBNATURE = "com.ibm.wtp.ejb.EJBNature";

    private static final String COM_IBM_WTP_J2EE_EARNATURE = "com.ibm.wtp.j2ee.EARNature";

    private static final String COM_IBM_WTP_J2EE_LIB_COPY_BUILDER = "com.ibm.wtp.j2ee.LibCopyBuilder";

    private static final String COM_IBM_WTP_MIGRATION_MIGRATION_BUILDER = "com.ibm.wtp.migration.MigrationBuilder";

    private static final String COM_IBM_WTP_WEB_WEB_NATURE = "com.ibm.wtp.web.WebNature";

    private static final String GENERATED_RESOURCE_DIRNAME = "target" + File.separatorChar + "generated-resources"
        + File.separatorChar + "rad6";

    private static final String ORG_ECLIPSE_JDT_CORE_JAVABUILDER = "org.eclipse.jdt.core.javabuilder";

    private static final String ORG_ECLIPSE_JDT_CORE_JAVANATURE = "org.eclipse.jdt.core.javanature";

    private boolean isJavaProject;

    /**
     * The context root of the webapplication. This parameter is only used when
     * the current project is a war project, else it will be ignored.
     * 
     * @parameter 
     */
    private String warContextRoot;

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
     * write all rad6 configuration files.
     * <br/>
     * <b>
     * NOTE: This could change the config!
     * </b>
     * 
     * @see EclipsePlugin#writeConfiguration()
     * @param deps
     *            resolved dependencies to handle
     * @throws MojoExecutionException
     *             if the config files could not be written.
     */
    protected void writeExtraConfiguration( EclipseWriterConfig config )
        throws MojoExecutionException
    {
        if ( isJavaProject )
        {
            // special case must be done first because it can add stuff to the classpath that will be 
            // written by the superclass
            new RadManifestWriter().init( getLog(), config ).write();
        }

        new RadJ2EEWriter().init( getLog(), config ).write();

        new RadWebSettingsWriter( this.warContextRoot ).init( getLog(), config ).write();

        new RadWebsiteConfigWriter().init( getLog(), config ).write();

        new RadApplicationXMLWriter().init( getLog(), config ).write();

        new RadLibCopier().init( getLog(), config ).write();

        new RadEjbClasspathWriter().init( getLog(), config ).write();
    }

    /**
     * make room for a Manifest file. use a generated resource for JARS and for
     * WARS use the manifest in the webapp/meta-inf directory.
     */
    private void addManifestResource()
    {
        if ( new RadManifestWriter().getMetaInfBaseDirectory( getExecutedProject() ) != null )
        {
            return;
        }

        String packaging = getExecutedProject().getPackaging();

        if ( this.isJavaProject && !Constants.PROJECT_PACKAGING_EAR.equals( packaging )
            && !Constants.PROJECT_PACKAGING_WAR.equals( packaging )
            && !Constants.PROJECT_PACKAGING_EJB.equals( packaging ) )
        {
            
            String generatedResourceDir = this.project.getBasedir().getAbsolutePath() + File.separatorChar
                + GENERATED_RESOURCE_DIRNAME;
            
            String metainfDir = generatedResourceDir + File.separatorChar + "META-INF";
            
            new File( metainfDir ).mkdirs();
            
            final Resource resource = new Resource();
            
            getLog().debug( "Adding " + GENERATED_RESOURCE_DIRNAME + " to resources" );
            
            resource.setDirectory( generatedResourceDir );
            
            this.executedProject.addResource( resource );
        }
        
        if ( Constants.PROJECT_PACKAGING_WAR.equals( packaging ) )
        {
            new File( this.project.getBasedir().getAbsolutePath() + File.separatorChar + "src" + File.separatorChar
                + "main" + File.separatorChar + "webapp" + File.separatorChar + "META-INF" ).mkdirs();
        }
    }

    /**
     * overwite the default builders with the builders required by RAD6.
     * 
     * @param packaging
     *            packaging-type (jar,war,ejb,ear)
     */
    protected void fillDefaultBuilders( String packaging )
    {
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
        else if ( this.isJavaProject )
        {
            buildcommands.add( ORG_ECLIPSE_JDT_CORE_JAVABUILDER );
            buildcommands.add( COM_IBM_SSE_MODEL_STRUCTUREDBUILDER );
        }
        setBuildcommands( buildcommands );
    }

    /**
     * overwite the default natures with the natures required by RAD6.
     * 
     * @param packaging
     *            packaging-type (jar,war,ejb,ear)
     */
    protected void fillDefaultNatures( String packaging )
    {
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
        else if ( this.isJavaProject )
        {
            projectnatures.add( ORG_ECLIPSE_JDT_CORE_JAVANATURE );
        }
        setProjectnatures( projectnatures );
    }

    /**
     * Utility method that locates a project producing the given artifact.
     * 
     * @param artifact
     *            the artifact a project should produce.
     * @return <code>true</code> if the artifact is produced by a reactor
     *         projectart.
     */
    protected boolean isAvailableAsAReactorProject( Artifact artifact )
    {
        if ( this.reactorProjects != null
            && ( Constants.PROJECT_PACKAGING_JAR.equals( artifact.getType() )
                || Constants.PROJECT_PACKAGING_EJB.equals( artifact.getType() ) || Constants.PROJECT_PACKAGING_WAR
                .equals( artifact.getType() ) ) )
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
                        getLog()
                            .info(
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

    protected void setupExtras()
    {
        addManifestResource();
    }
}
