/**
 * 
 */
package org.apache.maven.plugin.eclipse.writers;

import java.io.File;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.eclipse.EclipseSourceDir;
import org.apache.maven.plugin.eclipse.Messages;
import org.apache.maven.plugin.ide.IdeDependency;
import org.apache.maven.plugin.ide.IdeUtils;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.XMLWriter;

/**
 * Base class to hold common constants used by extending classes.
 * @author <a href="mailto:rahul.thakur.xdev@gmail.com">Rahul Thakur</a>
 * @author <a href="mailto:fgiust@users.sourceforge.net">Fabrizio Giustina</a>
 */
public abstract class AbstractWtpResourceWriter
    extends AbstractEclipseResourceWriter
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

    protected static final String ELT_PROJECT_MODULES = "project-modules"; //$NON-NLS-1$

    protected static final String ARTIFACT_MAVEN_WAR_PLUGIN = "maven-war-plugin"; //$NON-NLS-1$

    public AbstractWtpResourceWriter( Log log, File eclipseProjectDir, MavenProject project, IdeDependency[] deps )
    {
        super( log, eclipseProjectDir, project, deps );
    }

    /**
     * Returns Dependent artifacts for our project.
     * 
     * @return
     */
    protected IdeDependency[] getDependencies()
    {
        return this.deps;
    }

    /**
     * Common elements of configuration are handled here.
     * 
     * @param sourceDirs
     * @param localRepository
     * @param buildOutputDirectory
     * @throws MojoExecutionException
     */
    public abstract void write( EclipseSourceDir[] sourceDirs, ArtifactRepository localRepository,
                                File buildOutputDirectory )
        throws MojoExecutionException;

    /**
     * @param project
     * @param writer
     * @param packaging
     * @throws MojoExecutionException
     */
    protected void writeModuleTypeAccordingToPackaging( MavenProject project, XMLWriter writer, String packaging,
                                                        File buildOutputDirectory )
        throws MojoExecutionException
    {
        if ( "war".equals( packaging ) ) //$NON-NLS-1$
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
        else if ( "ejb".equals( packaging ) ) //$NON-NLS-1$
        {
            writer.addAttribute( ATTR_MODULE_TYPE_ID, "jst.ejb" ); //$NON-NLS-1$ 

            writer.startElement( ELT_VERSION );
            writer.writeText( resolveEjbVersion() );

            writer.endElement();

            writer.startElement( ELT_PROPERTY );
            writer.addAttribute( ATTR_NAME, "java-output-path" ); //$NON-NLS-1$ 
            writer.addAttribute( ATTR_VALUE, "/" + //$NON-NLS-1$ 
                IdeUtils.toRelativeAndFixSeparator( getProject().getBasedir(), buildOutputDirectory, false ) );
            writer.endElement();

        }
        else if ( "ear".equals( packaging ) ) //$NON-NLS-1$
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
                IdeUtils.toRelativeAndFixSeparator( getProject().getBasedir(), buildOutputDirectory, false ) );
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
                getLog().error( Messages.getString( "EclipsePlugin.artifactpathisnull", dep.getId() ) ); //$NON-NLS-1$
                return;
            }

            String fullPath = artifactPath.getPath();
            File repoFile = new File( fullPath );

            if ( dep.isSystemScoped() )
            {
                handle = "module:/classpath/lib/" //$NON-NLS-1$
                    + IdeUtils.toRelativeAndFixSeparator( getEclipseProjectDirectory(), repoFile, false );
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
        for ( int j = 0; j < deps.length; j++ )
        {
            IdeDependency dep = deps[j];
            String type = dep.getType();

            // NB war is needed for ear projects, we suppose nobody adds a war dependency to a war/jar project
            // exclude test and provided deps
            if ( ( !dep.isTestDependency() && !dep.isProvided() )
                && ( "jar".equals( type ) || "ejb".equals( type ) || "ejb-client".equals( type ) || "war".equals( type ) ) ) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
            {
                addDependency( writer, dep, localRepository, getProject().getBasedir() );
            }
        }
    }

    protected String resolveServletVersion()
    {
        String[] artifactNames = new String[] { "servlet-api", "servletapi", "geronimo-spec-servlet" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        String version = IdeUtils.getDependencyVersion( artifactNames, getProject().getArtifacts(), 3 );
        if ( version == null )
        {
            // none of the above specified matched, try geronimo-spec-j2ee
            artifactNames = new String[] { "geronimo-spec-j2ee" }; //$NON-NLS-1$
            version = IdeUtils.getDependencyVersion( artifactNames, getProject().getArtifacts(), 3 );
            if ( version != null )
            {
                String j2eeMinorVersion = StringUtils.substring( version, 2, 3 );
                version = "2." + j2eeMinorVersion; //$NON-NLS-1$
            }
        }
        return version == null ? "2.4" : version; //$NON-NLS-1$
    }

    protected String resolveEjbVersion()
    {
        String version = null;
        // @todo this is the default, find real ejb version from dependencies

        return version == null ? "2.1" : version; //$NON-NLS-1$
    }

    protected String resolveJ2eeVersion()
    {
        String version = null;
        // @todo this is the default, find real j2ee version from dependencies
        return version == null ? "1.3" : version; //$NON-NLS-1$
    }

    protected String resolveJavaVersion()
    {
        String version = IdeUtils.getPluginSetting( getProject(), "maven-compiler-plugin", "target", null ); //$NON-NLS-1$ //$NON-NLS-2$
        if ( version == null )
        {
            IdeUtils.getPluginSetting( getProject(), "maven-compiler-plugin", "source", null ); //$NON-NLS-1$ //$NON-NLS-2$
        }

        if ( "1.5".equals( version ) || "5".equals( version ) ) //$NON-NLS-1$ //$NON-NLS-2$
        {
            version = "5.0";// see MECLIPSE-47 eclipse only accept 5.0 as a valid version //$NON-NLS-1$
        }

        return version == null ? "1.4" : version; //$NON-NLS-1$
    }

}
