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
import org.codehaus.plexus.util.StringUtils;
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
    extends AbstractEclipseResourceWriter
{

    /**
     * Eclipse build path variable M2_REPO
     */
    private static final String M2_REPO = "M2_REPO";

    private File eclipseProjectDir;

    /**
     * Attribute for sourcepath.
     */
    private static final String ATTR_SOURCEPATH = "sourcepath";

    private MavenProject project;

    /**
     * Attribute for output.
     */
    private static final String ATTR_OUTPUT = "output";

    /**
     * Attribute for path.
     */
    private static final String ATTR_PATH = "path";

    /**
     * Attribute for kind - Container (con), Variable (var)..etc.
     */
    private static final String ATTR_KIND = "kind";

    /**
     * Element for classpathentry.
     */
    private static final String ELT_CLASSPATHENTRY = "classpathentry";

    /**
     * Element for classpath.
     */
    private static final String ELT_CLASSPATH = "classpath";

    /**
     * File name that stores project classpath settings.
     */
    private static final String FILE_DOT_CLASSPATH = ".classpath";

    /**
     * Dependencies for our project.
     */
    private Collection artifacts;

    public EclipseClasspathWriter( Log log, File eclipseProjectDir, MavenProject project, Collection artifacts )
    {
        super( log, eclipseProjectDir, project );
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
            w = new FileWriter( new File( getEclipseProjectDirectory(), FILE_DOT_CLASSPATH ) ); //$NON-NLS-1$
        }
        catch ( IOException ex )
        {
            throw new MojoExecutionException( Messages.getString( "EclipsePlugin.erroropeningfile" ), ex ); //$NON-NLS-1$
        }

        XMLWriter writer = new PrettyPrintXMLWriter( w );

        writer.startElement( ELT_CLASSPATH ); //$NON-NLS-1$

        // ----------------------------------------------------------------------
        // Source roots and resources
        // ----------------------------------------------------------------------

        for ( int j = 0; j < sourceDirs.length; j++ )
        {
            EclipseSourceDir dir = sourceDirs[j];

            writer.startElement( ELT_CLASSPATHENTRY ); //$NON-NLS-1$

            writer.addAttribute( ATTR_KIND, "src" ); //$NON-NLS-1$ //$NON-NLS-2$
            writer.addAttribute( ATTR_PATH, dir.getPath() ); //$NON-NLS-1$
            if ( dir.getOutput() != null )
            {
                writer.addAttribute( ATTR_OUTPUT, dir.getOutput() ); //$NON-NLS-1$
            }

            writer.endElement();

        }

        // ----------------------------------------------------------------------
        // The default output
        // ----------------------------------------------------------------------

        writer.startElement( ELT_CLASSPATHENTRY ); //$NON-NLS-1$
        writer.addAttribute( ATTR_KIND, ATTR_OUTPUT ); //$NON-NLS-1$ //$NON-NLS-2$
        writer.addAttribute( ATTR_PATH, //$NON-NLS-1$ 
                             EclipseUtils.toRelativeAndFixSeparator( projectBaseDir, buildOutputDirectory, false ) );
        writer.endElement();

        // ----------------------------------------------------------------------
        // Container classpath entries
        // ----------------------------------------------------------------------

        for ( Iterator it = classpathContainers.iterator(); it.hasNext(); )
        {
            writer.startElement( ELT_CLASSPATHENTRY ); //$NON-NLS-1$
            writer.addAttribute( ATTR_KIND, "con" ); //$NON-NLS-1$ //$NON-NLS-2$
            writer.addAttribute( ATTR_PATH, (String) it.next() ); //$NON-NLS-1$
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
        String javadocpath = null;

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
                getLog().error( Messages.getString( "EclipsePlugin.artifactpathisnull", artifact.getId() ) ); //$NON-NLS-1$
                return;
            }

            if ( Artifact.SCOPE_SYSTEM.equals( artifact.getScope() ) )
            {
                path = EclipseUtils.toRelativeAndFixSeparator( projectBaseDir, artifactPath, false );

                if ( getLog().isDebugEnabled() )
                {
                    getLog().debug( Messages.getString( "EclipsePlugin.artifactissystemscoped", //$NON-NLS-1$
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

                Artifact sourceArtifact = EclipseUtils.resolveLocalSourceArtifact( artifact, localRepository,
                                                                                   artifactResolver, artifactFactory );

                if ( sourceArtifact.isResolved() )
                {
                    sourcepath = "M2_REPO/" //$NON-NLS-1$
                        + EclipseUtils.toRelativeAndFixSeparator( localRepositoryFile, sourceArtifact.getFile(), false );
                }
                else
                {

                    // if a source artifact is not available, try with a plain javadoc jar
                    Artifact javadocArtifact = EclipseUtils.resolveLocalJavadocArtifact( artifact, localRepository,
                                                                                         artifactResolver,
                                                                                         artifactFactory );
                    if ( javadocArtifact.isResolved() )
                    {
                        try
                        {
                            // NB eclipse (3.1) doesn't support variables in javadoc paths, so we need to add the
                            // full path for the maven repo
                            javadocpath = StringUtils.replace( javadocArtifact.getFile().getCanonicalPath(), "\\", "/" );
                        }
                        catch ( IOException e )
                        {
                            // should never happen
                            throw new MojoExecutionException( e.getMessage(), e );
                        }
                    }
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
        else if ( javadocpath != null )
        {
            writer.startElement( "attributes" ); //$NON-NLS-1$

            writer.startElement( "attribute" ); //$NON-NLS-1$
            writer.addAttribute( "value", "jar:file:/" + javadocpath + "!/" );
            writer.addAttribute( "name", "javadoc_location" );
            writer.endElement();

            writer.endElement();
        }

        writer.endElement();

    }

}
