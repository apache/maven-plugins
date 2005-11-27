package org.apache.maven.plugin.eclipse.writers;

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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.eclipse.EclipseSourceDir;
import org.apache.maven.plugin.eclipse.EclipseUtils;
import org.apache.maven.plugin.eclipse.Messages;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.PrettyPrintXMLWriter;
import org.codehaus.plexus.util.xml.XMLWriter;

/**
 * Writes eclipse .classpath file.
 *
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @author <a href="mailto:kenney@neonics.com">Kenney Westerhof</a>
 * @author <a href="mailto:fgiust@apache.org">Fabrizio Giustina</a>
 * @version $Id$
 */
public class EclipseClasspathWriter
{

    private Log log;

    private File eclipseProjectDir;

    private MavenProject project;

    private Collection artifacts;

    public EclipseClasspathWriter( Log log, File eclipseProjectDir, MavenProject project, Collection artifacts )
    {
        this.log = log;
        this.eclipseProjectDir = eclipseProjectDir;
        this.project = project;
        this.artifacts = artifacts;
    }

    public void write( File projectBaseDir, List referencedReactorArtifacts, EclipseSourceDir[] sourceDirs,
                      List classpathContainers, ArtifactRepository localRepository, ArtifactResolver artifactResolver,
                      ArtifactFactory artifactFactory, File buildOutputDirectory )
        throws MojoExecutionException
    {

        FileWriter w;

        try
        {
            w = new FileWriter( new File( eclipseProjectDir, ".classpath" ) ); //$NON-NLS-1$
        }
        catch ( IOException ex )
        {
            throw new MojoExecutionException( Messages.getString( "EclipsePlugin.erroropeningfile" ), ex ); //$NON-NLS-1$
        }

        XMLWriter writer = new PrettyPrintXMLWriter( w );

        writer.startElement( "classpath" ); //$NON-NLS-1$

        // ----------------------------------------------------------------------
        // Source roots and resources
        // ----------------------------------------------------------------------

        for ( int j = 0; j < sourceDirs.length; j++ )
        {
            EclipseSourceDir dir = sourceDirs[j];

            writer.startElement( "classpathentry" ); //$NON-NLS-1$

            writer.addAttribute( "kind", "src" ); //$NON-NLS-1$ //$NON-NLS-2$
            writer.addAttribute( "path", dir.getPath() ); //$NON-NLS-1$
            if ( dir.getOutput() != null )
            {
                writer.addAttribute( "output", dir.getOutput() ); //$NON-NLS-1$
            }

            writer.endElement();

        }

        // ----------------------------------------------------------------------
        // The default output
        // ----------------------------------------------------------------------

        writer.startElement( "classpathentry" ); //$NON-NLS-1$
        writer.addAttribute( "kind", "output" ); //$NON-NLS-1$ //$NON-NLS-2$
        writer.addAttribute( "path", //$NON-NLS-1$ 
                             EclipseUtils.toRelativeAndFixSeparator( projectBaseDir, buildOutputDirectory, false ) );
        writer.endElement();

        // ----------------------------------------------------------------------
        // Container classpath entries
        // ----------------------------------------------------------------------

        for ( Iterator it = classpathContainers.iterator(); it.hasNext(); )
        {
            writer.startElement( "classpathentry" ); //$NON-NLS-1$
            writer.addAttribute( "kind", "con" ); //$NON-NLS-1$ //$NON-NLS-2$
            writer.addAttribute( "path", (String) it.next() ); //$NON-NLS-1$
            writer.endElement(); // name
        }

        // ----------------------------------------------------------------------
        // The dependencies
        // ----------------------------------------------------------------------

        for ( Iterator it = artifacts.iterator(); it.hasNext(); )
        {
            Artifact artifact = (Artifact) it.next();
            if ( artifact.getArtifactHandler().isAddedToClasspath() )
            {
                addDependency( writer, artifact, referencedReactorArtifacts, localRepository, artifactResolver,
                               artifactFactory, projectBaseDir );
            }
        }

        writer.endElement();

        IOUtil.close( w );

    }

    private void addDependency( XMLWriter writer, Artifact artifact, List referencedReactorArtifacts,
                               ArtifactRepository localRepository, ArtifactResolver artifactResolver,
                               ArtifactFactory artifactFactory, File projectBaseDir )
        throws MojoExecutionException
    {

        String path;
        String kind;
        String sourcepath = null;

        if ( referencedReactorArtifacts.contains( artifact ) )
        {
            path = "/" + artifact.getArtifactId(); //$NON-NLS-1$
            kind = "src"; //$NON-NLS-1$
        }
        else
        {
            File artifactPath = artifact.getFile();

            if ( artifactPath == null )
            {
                log.error( Messages.getString( "EclipsePlugin.artifactpathisnull", artifact.getId() ) ); //$NON-NLS-1$
                return;
            }

            if ( Artifact.SCOPE_SYSTEM.equals( artifact.getScope() ) )
            {
                path = EclipseUtils.toRelativeAndFixSeparator( projectBaseDir, artifactPath, false );

                if ( log.isDebugEnabled() )
                {
                    log.debug( Messages.getString( "EclipsePlugin.artifactissystemscoped", //$NON-NLS-1$
                                                   new Object[] { artifact.getArtifactId(), path } ) );
                }

                kind = "lib"; //$NON-NLS-1$
            }
            else
            {
                File localRepositoryFile = new File( localRepository.getBasedir() );

                String fullPath = artifactPath.getPath();

                path = "M2_REPO/" //$NON-NLS-1$
                    + EclipseUtils.toRelativeAndFixSeparator( localRepositoryFile, new File( fullPath ), false );

                Artifact sourceArtifact = EclipseUtils.resolveSourceArtifact( artifact, localRepository,
                                                                              artifactResolver, artifactFactory );

                if ( sourceArtifact.isResolved() )
                {
                    sourcepath = "M2_REPO/" //$NON-NLS-1$
                        + EclipseUtils.toRelativeAndFixSeparator( localRepositoryFile, sourceArtifact.getFile(), false );
                }

                kind = "var"; //$NON-NLS-1$
            }

        }

        writer.startElement( "classpathentry" ); //$NON-NLS-1$
        writer.addAttribute( "kind", kind ); //$NON-NLS-1$
        writer.addAttribute( "path", path ); //$NON-NLS-1$

        if ( sourcepath != null )
        {
            writer.addAttribute( "sourcepath", sourcepath ); //$NON-NLS-1$
        }

        writer.endElement();

    }

}
