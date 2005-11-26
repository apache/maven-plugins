package org.apache.maven.plugin.eclipse;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.PrettyPrintXMLWriter;
import org.codehaus.plexus.util.xml.XMLWriter;

/**
 * Writes eclipse .wtpmodules file.
 *
 * @author <a href="mailto:fgiust@users.sourceforge.net">Fabrizio Giustina</a>
 * @version $Id$
 */
public class EclipseWtpmodulesWriter
{

    private Log log;

    public EclipseWtpmodulesWriter( Log log )
    {
        this.log = log;
    }

    protected void write( File basedir, MavenProject project, List referencedReactorArtifacts,
                         EclipseSourceDir[] sourceDirs, ArtifactRepository localRepository,
                         ArtifactResolver artifactResolver, List remoteArtifactRepositories )
        throws MojoExecutionException
    {
        FileWriter w;

        try
        {
            w = new FileWriter( new File( basedir, ".wtpmodules" ) ); //$NON-NLS-1$
        }
        catch ( IOException ex )
        {
            throw new MojoExecutionException( Messages.getString( "EclipsePlugin.erroropeningfile" ), ex ); //$NON-NLS-1$
        }

        XMLWriter writer = new PrettyPrintXMLWriter( w );

        writer.startElement( "project-modules" ); //$NON-NLS-1$
        writer.addAttribute( "id", "moduleCoreId" ); //$NON-NLS-1$ //$NON-NLS-2$

        writer.startElement( "wb-module" ); //$NON-NLS-1$
        writer.addAttribute( "deploy-name", project.getArtifactId() ); //$NON-NLS-1$

        String packaging = project.getPackaging();

        writer.startElement( "module-type" ); //$NON-NLS-1$
        writeModuleTypeAccordingToPackaging( project, writer, packaging );
        writer.endElement(); // module-type

        // source and resource paths.
        // deploy-path is "/" for utility and ejb projects, "/WEB-INF/classes" for webapps

        String target = "/"; //$NON-NLS-1$
        if ( "war".equals( project.getPackaging() ) ) //$NON-NLS-1$
        {
            String warSourceDirectory = EclipseUtils.getPluginSetting( project, "maven-war-plugin", //$NON-NLS-1$
                                                                       "warSourceDirectory", //$NON-NLS-1$
                                                                       "/src/main/webapp" ); //$NON-NLS-1$

            writer.startElement( "wb-resource" ); //$NON-NLS-1$
            writer.addAttribute( "deploy-path", "/" ); //$NON-NLS-1$ //$NON-NLS-2$
            writer.addAttribute( "source-path", //$NON-NLS-1$
                                 "/"
                                     + EclipseUtils.toRelativeAndFixSeparator( basedir, new File( basedir,
                                                                                                  warSourceDirectory ),
                                                                               false ) );
            writer.endElement();

            writeWarOrEarResources( writer, project, referencedReactorArtifacts, localRepository, artifactResolver,
                                    remoteArtifactRepositories );

            target = "/WEB-INF/classes"; //$NON-NLS-1$
        }
        else if ( "ear".equals( project.getPackaging() ) ) //$NON-NLS-1$
        {
            writer.startElement( "wb-resource" ); //$NON-NLS-1$
            writer.addAttribute( "deploy-path", "/" ); //$NON-NLS-1$ //$NON-NLS-2$
            writer.addAttribute( "source-path", "/" ); //$NON-NLS-1$ //$NON-NLS-2$
            writer.endElement();

            writeWarOrEarResources( writer, project, referencedReactorArtifacts, localRepository, artifactResolver,
                                    remoteArtifactRepositories );
        }

        for ( int j = 0; j < sourceDirs.length; j++ )
        {
            EclipseSourceDir dir = sourceDirs[j];
            // test src/resources are not added to wtpmodules
            if ( !dir.isTest() )
            {
                //  <wb-resource deploy-path="/" source-path="/src/java" />
                writer.startElement( "wb-resource" ); //$NON-NLS-1$
                writer.addAttribute( "deploy-path", target ); //$NON-NLS-1$
                writer.addAttribute( "source-path", dir.getPath() ); //$NON-NLS-1$
                writer.endElement();
            }
        }

        writer.endElement(); // wb-module
        writer.endElement(); // project-modules

        IOUtil.close( w );
    }

    /**
     * @param project
     * @param writer
     * @param packaging
     * @throws MojoExecutionException 
     */
    private void writeModuleTypeAccordingToPackaging( MavenProject project, XMLWriter writer, String packaging )
        throws MojoExecutionException
    {
        if ( "war".equals( packaging ) ) //$NON-NLS-1$
        {
            writer.addAttribute( "module-type-id", "jst.web" ); //$NON-NLS-1$ //$NON-NLS-2$

            writer.startElement( "version" ); //$NON-NLS-1$

            // defaults to 2.4, try to detect real version from dependencies
            String servletVersion = "2.4"; //$NON-NLS-1$

            for ( Iterator it = project.getArtifacts().iterator(); it.hasNext(); )
            {
                Artifact artifact = (Artifact) it.next();
                if ( "servlet-api".equals( artifact.getArtifactId() ) //$NON-NLS-1$
                    || "servletapi".equals( artifact.getArtifactId() ) // $NON-NLS-1$
                    || "geronimo-spec-servlet".equals( artifact.getArtifactId() ) ) //$NON-NLS-1$
                {
                    servletVersion = StringUtils.substring( artifact.getVersion(), 0, 3 );
                }
                else if ( "geronimo-spec-j2ee".equals( artifact.getArtifactId() ) ) // $NON-NLS-1$
                {
                    String j2eeMinorVersion = StringUtils.substring( artifact.getVersion(), 2, 3 );
                    servletVersion = "2." + j2eeMinorVersion;
                }
            }

            writer.writeText( servletVersion );
            writer.endElement();

            writer.startElement( "property" ); //$NON-NLS-1$
            writer.addAttribute( "name", "context-root" ); //$NON-NLS-1$ //$NON-NLS-2$
            writer.addAttribute( "value", project.getArtifactId() ); //$NON-NLS-1$
            writer.endElement();
        }
        else if ( "ejb".equals( packaging ) ) //$NON-NLS-1$
        {
            writer.addAttribute( "module-type-id", "jst.ejb" ); //$NON-NLS-1$ //$NON-NLS-2$

            writer.startElement( "version" ); //$NON-NLS-1$
            writer.writeText( "2.1" ); //$NON-NLS-1$
            // @todo this is the default, find real ejb version from dependencies
            writer.endElement();

            writer.startElement( "property" ); //$NON-NLS-1$
            writer.addAttribute( "name", "java-output-path" ); //$NON-NLS-1$ //$NON-NLS-2$
            writer.addAttribute( "value", "/" + //$NON-NLS-1$ //$NON-NLS-2$
                EclipseUtils.toRelativeAndFixSeparator( project.getBasedir(), new File( project.getBuild()
                    .getOutputDirectory() ), false ) );
            writer.endElement();
        }
        else if ( "ear".equals( packaging ) )
        {
            writer.addAttribute( "module-type-id", "jst.ear" ); //$NON-NLS-1$ //$NON-NLS-2$

            writer.startElement( "version" ); //$NON-NLS-1$
            writer.writeText( "1.3" ); //$NON-NLS-1$
            // @todo 1.3 is the default
            writer.endElement();
        }
        else
        {
            // jar
            writer.addAttribute( "module-type-id", "jst.utility" ); //$NON-NLS-1$ //$NON-NLS-2$

            writer.startElement( "property" ); //$NON-NLS-1$
            writer.addAttribute( "name", "java-output-path" ); //$NON-NLS-1$ //$NON-NLS-2$
            writer.addAttribute( "value", "/" + //$NON-NLS-1$ //$NON-NLS-2$
                EclipseUtils.toRelativeAndFixSeparator( project.getBasedir(), new File( project.getBuild()
                    .getOutputDirectory() ), false ) );
            writer.endElement();
        }
    }

    private void writeWarOrEarResources( XMLWriter writer, MavenProject project, List referencedReactorArtifacts,
                                        ArtifactRepository localRepository, ArtifactResolver artifactResolver,
                                        List remoteArtifactRepositories )
        throws MojoExecutionException
    {
        Set artifacts = project.getArtifacts();

        EclipseUtils.fixMissingOptionalArtifacts( artifacts, project.getDependencyArtifacts(), localRepository,
                                                  artifactResolver, remoteArtifactRepositories, log );

        EclipseUtils.fixSystemScopeArtifacts( artifacts, project.getDependencies() );

        ScopeArtifactFilter scopeFilter = new ScopeArtifactFilter( Artifact.SCOPE_RUNTIME );

        // dependencies
        for ( Iterator it = artifacts.iterator(); it.hasNext(); )
        {
            Artifact artifact = (Artifact) it.next();
            String type = artifact.getType();

            // NB war is needed for ear projects, we suppose nobody adds a war dependency to a war/jar project
            if ( ( scopeFilter.include( artifact ) || Artifact.SCOPE_SYSTEM.equals( artifact.getScope() ) )
                && ( "jar".equals( type ) || "ejb".equals( type ) || "ejb-client".equals( type ) || "war".equals( type ) ) )
            {
                addDependency( writer, artifact, referencedReactorArtifacts, localRepository, project.getBasedir() );
            }
        }
    }

    private void addDependency( XMLWriter writer, Artifact artifact, List referencedReactorProjects,
                               ArtifactRepository localRepository, File basedir )
        throws MojoExecutionException
    {
        String handle;

        if ( referencedReactorProjects.contains( artifact ) )
        {
            //  <dependent-module deploy-path="/WEB-INF/lib" handle="module:/resource/artifactid/artifactid">
            //    <dependency-type>uses</dependency-type>
            //  </dependent-module>

            handle = "module:/resource/" + artifact.getArtifactId() + "/" + artifact.getArtifactId(); //$NON-NLS-1$ //$NON-NLS-2$
        }
        else
        {
            // <dependent-module deploy-path="/WEB-INF/lib" handle="module:/classpath/var/M2_REPO/cl/cl/2.1/cl-2.1.jar">
            //    <dependency-type>uses</dependency-type>
            // </dependent-module>

            File artifactPath = artifact.getFile();

            if ( artifactPath == null )
            {
                log.error( Messages.getString( "EclipsePlugin.artifactpathisnull", artifact.getId() ) ); //$NON-NLS-1$
                return;
            }

            String fullPath = artifactPath.getPath();
            File repoFile = new File( fullPath );

            if ( Artifact.SCOPE_SYSTEM.equals( artifact.getScope() ) )
            {
                handle = "module:/classpath/lib/" //$NON-NLS-1$
                    + EclipseUtils.toRelativeAndFixSeparator( basedir, repoFile, true );
            }
            else
            {
                File localRepositoryFile = new File( localRepository.getBasedir() );

                handle = "module:/classpath/var/M2_REPO/" //$NON-NLS-1$
                    + EclipseUtils.toRelativeAndFixSeparator( localRepositoryFile, repoFile, false );
            }
        }

        writer.startElement( "dependent-module" ); //$NON-NLS-1$

        writer.addAttribute( "deploy-path", "/WEB-INF/lib" ); //$NON-NLS-1$ //$NON-NLS-2$
        writer.addAttribute( "handle", handle ); //$NON-NLS-1$

        writer.startElement( "dependency-type" ); //$NON-NLS-1$
        writer.writeText( "uses" ); //$NON-NLS-1$
        writer.endElement();

        writer.endElement();
    }

}
