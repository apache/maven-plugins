/**
 * 
 */
package org.apache.maven.plugin.eclipse.writers;

import java.io.File;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.eclipse.EclipseSourceDir;
import org.apache.maven.plugin.eclipse.EclipseUtils;
import org.apache.maven.plugin.eclipse.Messages;
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

    private static final String ELT_DEPENDENCY_TYPE = "dependency-type";

    private static final String ATTR_HANDLE = "handle";

    private static final String ELT_DEPENDENT_MODULE = "dependent-module";

    protected static final String ATTR_VALUE = "value";

    protected static final String ATTR_NAME = "name";

    protected static final String ELT_PROPERTY = "property";

    protected static final String ELT_VERSION = "version";

    protected static final String ATTR_MODULE_TYPE_ID = "module-type-id";

    protected static final String ATTR_SOURCE_PATH = "source-path";

    protected static final String ATTR_DEPLOY_PATH = "deploy-path";

    protected static final String ELT_WB_RESOURCE = "wb-resource";

    protected static final String ELT_MODULE_TYPE = "module-type";

    protected static final String ATTR_DEPLOY_NAME = "deploy-name";

    protected static final String ELT_WB_MODULE = "wb-module";

    protected static final String ATTR_MODULE_ID = "id";

    protected static final String ELT_PROJECT_MODULES = "project-modules";

    protected static final String ARTIFACT_MAVEN_WAR_PLUGIN = "maven-war-plugin";

    /**
     * Dependencies for our project.
     */
    private Collection artifacts;

    public AbstractWtpResourceWriter( Log log, File eclipseProjectDir, MavenProject project, Collection artifacts )
    {
        super( log, eclipseProjectDir, project );
        this.artifacts = artifacts;
    }

    /**
     * Returns Dependent artifacts for our project.
     * 
     * @return
     */
    protected Collection getDependencies()
    {
        return this.artifacts;
    }

    /**
     * Common elements of configuration are handled here.
     * 
     * @param referencedReactorArtifacts
     * @param sourceDirs
     * @param localRepository
     * @param buildOutputDirectory
     * @throws MojoExecutionException
     */
    public abstract void write( List referencedReactorArtifacts, EclipseSourceDir[] sourceDirs,
                               ArtifactRepository localRepository, File buildOutputDirectory )
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
            writer.addAttribute( ATTR_MODULE_TYPE_ID, "jst.web" ); //$NON-NLS-1$ //$NON-NLS-2$

            writer.startElement( ELT_VERSION ); //$NON-NLS-1$

            writer.writeText( resolveServletVersion() );
            writer.endElement();

            // use finalName as context root only if it has been explicitely set
            String contextRoot = project.getArtifactId();
            String finalName = project.getBuild().getFinalName();
            if ( !finalName.equals( project.getArtifactId() + "-" + project.getVersion() ) )
            {
                contextRoot = finalName;
            }

            writer.startElement( ELT_PROPERTY ); //$NON-NLS-1$
            writer.addAttribute( ATTR_NAME, "context-root" ); //$NON-NLS-1$ //$NON-NLS-2$
            writer.addAttribute( ATTR_VALUE, contextRoot ); //$NON-NLS-1$
            writer.endElement();
        }
        else if ( "ejb".equals( packaging ) ) //$NON-NLS-1$
        {
            writer.addAttribute( ATTR_MODULE_TYPE_ID, "jst.ejb" ); //$NON-NLS-1$ //$NON-NLS-2$

            writer.startElement( ELT_VERSION ); //$NON-NLS-1$
            writer.writeText( resolveEjbVersion() ); //$NON-NLS-1$

            writer.endElement();

            writer.startElement( ELT_PROPERTY ); //$NON-NLS-1$
            writer.addAttribute( ATTR_NAME, "java-output-path" ); //$NON-NLS-1$ //$NON-NLS-2$
            writer.addAttribute( ATTR_VALUE, "/" + //$NON-NLS-1$ //$NON-NLS-2$
                EclipseUtils.toRelativeAndFixSeparator( getProject().getBasedir(), buildOutputDirectory, false ) );
            writer.endElement();

        }
        else if ( "ear".equals( packaging ) )
        {
            writer.addAttribute( ATTR_MODULE_TYPE_ID, "jst.ear" ); //$NON-NLS-1$ //$NON-NLS-2$

            writer.startElement( ELT_VERSION ); //$NON-NLS-1$
            writer.writeText( resolveJ2eeVersion() ); //$NON-NLS-1$
            writer.endElement();
        }
        else
        {
            // jar
            writer.addAttribute( ATTR_MODULE_TYPE_ID, "jst.utility" ); //$NON-NLS-1$ //$NON-NLS-2$

            writer.startElement( ELT_PROPERTY ); //$NON-NLS-1$
            writer.addAttribute( ATTR_NAME, "java-output-path" ); //$NON-NLS-1$ //$NON-NLS-2$
            writer.addAttribute( ATTR_VALUE, "/" + //$NON-NLS-1$ //$NON-NLS-2$
                EclipseUtils.toRelativeAndFixSeparator( getProject().getBasedir(), buildOutputDirectory, false ) );
            writer.endElement();
        }
    }

    /**
     * Adds dependency for Eclipse WTP project.
     * 
     * @param writer
     * @param artifact
     * @param referencedReactorProjects
     * @param localRepository
     * @param basedir
     * @throws MojoExecutionException
     */
    protected void addDependency( XMLWriter writer, Artifact artifact, List referencedReactorProjects,
                                 ArtifactRepository localRepository, File basedir )
        throws MojoExecutionException
    {
        String handle;

        if ( referencedReactorProjects.contains( artifact ) )
        {
            // <dependent-module deploy-path="/WEB-INF/lib"
            // handle="module:/resource/artifactid/artifactid">
            // <dependency-type>uses</dependency-type>
            // </dependent-module>

            handle = "module:/resource/" + artifact.getArtifactId() + "/" + artifact.getArtifactId(); //$NON-NLS-1$ //$NON-NLS-2$
        }
        else
        {
            // <dependent-module deploy-path="/WEB-INF/lib"
            // handle="module:/classpath/var/M2_REPO/cl/cl/2.1/cl-2.1.jar">
            // <dependency-type>uses</dependency-type>
            // </dependent-module>

            File artifactPath = artifact.getFile();

            if ( artifactPath == null )
            {
                getLog().error( Messages.getString( "EclipsePlugin.artifactpathisnull", artifact.getId() ) ); //$NON-NLS-1$
                return;
            }

            String fullPath = artifactPath.getPath();
            File repoFile = new File( fullPath );

            if ( Artifact.SCOPE_SYSTEM.equals( artifact.getScope() ) )
            {
                handle = "module:/classpath/lib/" //$NON-NLS-1$
                    + EclipseUtils.toRelativeAndFixSeparator( basedir, repoFile, false );
            }
            else
            {
                File localRepositoryFile = new File( localRepository.getBasedir() );

                handle = "module:/classpath/var/M2_REPO/" //$NON-NLS-1$
                    + EclipseUtils.toRelativeAndFixSeparator( localRepositoryFile, repoFile, false );
            }
        }

        writer.startElement( ELT_DEPENDENT_MODULE ); //$NON-NLS-1$

        writer.addAttribute( ATTR_DEPLOY_PATH, "/WEB-INF/lib" ); //$NON-NLS-1$ //$NON-NLS-2$
        writer.addAttribute( ATTR_HANDLE, handle ); //$NON-NLS-1$

        writer.startElement( ELT_DEPENDENCY_TYPE ); //$NON-NLS-1$
        writer.writeText( "uses" ); //$NON-NLS-1$
        writer.endElement();

        writer.endElement();
    }

    protected void writeWarOrEarResources( XMLWriter writer, MavenProject project, List referencedReactorArtifacts,
                                          ArtifactRepository localRepository )
        throws MojoExecutionException
    {

        ScopeArtifactFilter scopeFilter = new ScopeArtifactFilter( Artifact.SCOPE_RUNTIME );

        // dependencies
        for ( Iterator it = artifacts.iterator(); it.hasNext(); )
        {
            Artifact artifact = (Artifact) it.next();
            String type = artifact.getType();

            // NB war is needed for ear projects, we suppose nobody adds a war
            // dependency to a war/jar project
            if ( ( scopeFilter.include( artifact ) || Artifact.SCOPE_SYSTEM.equals( artifact.getScope() ) )
                && ( "jar".equals( type ) || "ejb".equals( type ) || "ejb-client".equals( type ) || "war".equals( type ) ) )
            {
                addDependency( writer, artifact, referencedReactorArtifacts, localRepository, getProject().getBasedir() );
            }
        }
    }

    protected String resolveServletVersion()
    {
        String[] artifactNames = new String[] { "servlet-api", "servletapi", "geronimo-spec-servlet" };

        String version = EclipseUtils.getDependencyVersion( artifactNames, getProject().getArtifacts(), 3 );
        if ( version == null )
        {
            // none of the above specified matched, try geronimo-spec-j2ee
            artifactNames = new String[] { "geronimo-spec-j2ee" };
            version = EclipseUtils.getDependencyVersion( artifactNames, getProject().getArtifacts(), 3 );
            if ( version != null )
            {
                String j2eeMinorVersion = StringUtils.substring( version, 2, 3 );
                version = "2." + j2eeMinorVersion;
            }
        }
        return version == null ? "2.4" : version;
    }

    protected String resolveEjbVersion()
    {
        String version = null;
        // @todo this is the default, find real ejb version from dependencies

        return version == null ? "2.1" : version;
    }

    protected String resolveJ2eeVersion()
    {
        String version = null;
        // @todo this is the default, find real j2ee version from dependencies
        return version == null ? "1.3" : version;
    }

    protected String resolveJavaVersion()
    {
        String version = EclipseUtils.getPluginSetting( getProject(), "maven-compiler-plugin", "target", null );
        if ( version == null )
        {
            EclipseUtils.getPluginSetting( getProject(), "maven-compiler-plugin", "source", null );
        }

        return version == null ? "1.4" : version;
    }

}
