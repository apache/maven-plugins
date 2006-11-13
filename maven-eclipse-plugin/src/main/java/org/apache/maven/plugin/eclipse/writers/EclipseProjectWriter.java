/*
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

package org.apache.maven.plugin.eclipse.writers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.eclipse.BuildCommand;
import org.apache.maven.plugin.eclipse.Messages;
import org.apache.maven.plugin.ide.IdeDependency;
import org.apache.maven.plugin.ide.IdeUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.PrettyPrintXMLWriter;
import org.codehaus.plexus.util.xml.XMLWriter;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * Writes eclipse .project file.
 *
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @author <a href="mailto:kenney@neonics.com">Kenney Westerhof</a>
 * @author <a href="mailto:fgiust@apache.org">Fabrizio Giustina</a>
 * @version $Id$
 */
public class EclipseProjectWriter
    extends AbstractEclipseWriter
{
    private static final String ELT_NAME = "name"; //$NON-NLS-1$

    private static final String ELT_BUILD_COMMAND = "buildCommand"; //$NON-NLS-1$

    private static final String ELT_BUILD_SPEC = "buildSpec"; //$NON-NLS-1$

    private static final String ELT_NATURE = "nature"; //$NON-NLS-1$

    private static final String ELT_NATURES = "natures"; //$NON-NLS-1$

    private static final String FILE_DOT_PROJECT = ".project"; //$NON-NLS-1$

    /**
     * Constant for links to files.
     */
    private static final int LINK_TYPE_FILE = 1;

    /**
     * Constant for links to directories.
     */
    private static final int LINK_TYPE_DIRECTORY = 2;

    /**
     * @see org.apache.maven.plugin.eclipse.writers.EclipseWriter#write()
     */
    public void write()
        throws MojoExecutionException
    {

        Set projectnatures = new LinkedHashSet();
        Set buildCommands = new LinkedHashSet();

        File dotProject = new File( config.getEclipseProjectDirectory(), FILE_DOT_PROJECT );

        if ( dotProject.exists() )
        {

            log.info( Messages.getString( "EclipsePlugin.keepexisting", dotProject.getAbsolutePath() ) ); //$NON-NLS-1$

            // parse existing file in order to keep manually-added entries
            FileReader reader = null;
            try
            {
                reader = new FileReader( dotProject );
                Xpp3Dom dom = Xpp3DomBuilder.build( reader );

                Xpp3Dom naturesElement = dom.getChild( ELT_NATURES );
                if ( naturesElement != null )
                {
                    Xpp3Dom[] existingNatures = naturesElement.getChildren( ELT_NATURE );
                    for ( int j = 0; j < existingNatures.length; j++ )
                    {
                        // adds all the existing natures
                        projectnatures.add( existingNatures[j].getValue() );
                    }
                }

                Xpp3Dom buildSpec = dom.getChild( ELT_BUILD_SPEC );
                if ( buildSpec != null )
                {
                    Xpp3Dom[] existingBuildCommands = buildSpec.getChildren( ELT_BUILD_COMMAND );
                    for ( int j = 0; j < existingBuildCommands.length; j++ )
                    {
                        Xpp3Dom buildCommandName = existingBuildCommands[j].getChild( ELT_NAME );
                        if ( buildCommandName != null )
                        {
                            buildCommands.add( new BuildCommand( existingBuildCommands[j] ) );
                        }
                    }
                }
            }
            catch ( XmlPullParserException e )
            {
                log.warn( Messages.getString( "EclipsePlugin.cantparseexisting", dotProject.getAbsolutePath() ) ); //$NON-NLS-1$
            }
            catch ( IOException e )
            {
                log.warn( Messages.getString( "EclipsePlugin.cantparseexisting", dotProject.getAbsolutePath() ) ); //$NON-NLS-1$
            }
            finally
            {
                IOUtil.close( reader );
            }
        }

        // adds new entries after the existing ones
        for ( Iterator iter = config.getProjectnatures().iterator(); iter.hasNext(); )
        {
            projectnatures.add( iter.next() );
        }

        for ( Iterator iter = config.getBuildCommands().iterator(); iter.hasNext(); )
        {
            buildCommands.add( (BuildCommand) iter.next() );
        }

        Writer w;

        try
        {
            w = new OutputStreamWriter( new FileOutputStream( dotProject ), "UTF-8" );
        }
        catch ( IOException ex )
        {
            throw new MojoExecutionException( Messages.getString( "EclipsePlugin.erroropeningfile" ), ex ); //$NON-NLS-1$
        }

        XMLWriter writer = new PrettyPrintXMLWriter( w );

        writer.startElement( "projectDescription" ); //$NON-NLS-1$

        writer.startElement( ELT_NAME );
        writer.writeText( config.getEclipseProjectName() );
        writer.endElement();

        // TODO: this entire element might be dropped if the comment is null.
        // but as the maven1 eclipse plugin does it, it's better to be safe than sorry
        // A eclipse developer might want to look at this.
        writer.startElement( "comment" ); //$NON-NLS-1$

        if ( config.getProject().getDescription() != null )
        {
            writer.writeText( config.getProject().getDescription() );
        }

        writer.endElement();

        writer.startElement( "projects" ); //$NON-NLS-1$

        // referenced projects should not be added for plugins
        if ( !config.isPde() )
        {
            for ( int j = 0; j < config.getDeps().length; j++ )
            {
                IdeDependency dep = config.getDeps()[j];
                if ( dep.isReferencedProject() )
                {
                    writer.startElement( "project" ); //$NON-NLS-1$
                    writer.writeText( dep.getArtifactId() );
                    writer.endElement();
                }
            }
        }

        writer.endElement(); // projects

        writer.startElement( ELT_BUILD_SPEC );

        for ( Iterator it = buildCommands.iterator(); it.hasNext(); )
        {
            ( (BuildCommand) it.next() ).print( writer );
        }

        writer.endElement(); // buildSpec

        writer.startElement( ELT_NATURES );

        for ( Iterator it = projectnatures.iterator(); it.hasNext(); )
        {
            writer.startElement( ELT_NATURE );
            writer.writeText( (String) it.next() );
            writer.endElement(); // name
        }

        writer.endElement(); // natures

        boolean addLinks = !config.getProjectBaseDir().equals( config.getEclipseProjectDirectory() );

        if ( addLinks || ( config.isPde() && config.getDeps().length > 0 ) )
        {
            writer.startElement( "linkedResources" ); //$NON-NLS-1$

            if ( addLinks )
            {

                addFileLink( writer, config.getProjectBaseDir(), config.getEclipseProjectDirectory(), config
                    .getProject().getFile() );

                addSourceLinks( writer, config.getProjectBaseDir(), config.getEclipseProjectDirectory(), config
                    .getProject().getCompileSourceRoots() );
                addResourceLinks( writer, config.getProjectBaseDir(), config.getEclipseProjectDirectory(), config
                    .getProject().getBuild().getResources() );

                addSourceLinks( writer, config.getProjectBaseDir(), config.getEclipseProjectDirectory(), config
                    .getProject().getTestCompileSourceRoots() );
                addResourceLinks( writer, config.getProjectBaseDir(), config.getEclipseProjectDirectory(), config
                    .getProject().getBuild().getTestResources() );

            }

            if ( config.isPde() )
            {
                for ( int j = 0; j < config.getDeps().length; j++ )
                {
                    IdeDependency dep = config.getDeps()[j];

                    if ( dep.isAddedToClasspath() && !dep.isProvided() && !dep.isReferencedProject()
                        && !dep.isTestDependency() && !dep.isOsgiBundle() )
                    {
                        String name = dep.getFile().getName();
                        addLink( writer, name, StringUtils.replace( IdeUtils.getCanonicalPath( dep.getFile() ), "\\",
                                                                    "/" ), LINK_TYPE_FILE );
                    }
                }
            }

            writer.endElement(); // linkedResources
        }

        writer.endElement(); // projectDescription

        IOUtil.close( w );
    }

    private void addFileLink( XMLWriter writer, File projectBaseDir, File basedir, File file )
        throws MojoExecutionException
    {
        if ( file.isFile() )
        {
            String name = IdeUtils.toRelativeAndFixSeparator( projectBaseDir, file, true );
            String location = IdeUtils.getCanonicalPath( file ).replaceAll( "\\\\", "/" ); //$NON-NLS-1$ //$NON-NLS-2$

            addLink( writer, name, location, LINK_TYPE_FILE );
        }
        else
        {
            log.warn( Messages.getString( "EclipseProjectWriter.notafile", file ) ); //$NON-NLS-1$
        }
    }

    private void addSourceLinks( XMLWriter writer, File projectBaseDir, File basedir, List sourceRoots )
        throws MojoExecutionException
    {
        for ( Iterator it = sourceRoots.iterator(); it.hasNext(); )
        {
            String sourceRootString = (String) it.next();
            File sourceRoot = new File( sourceRootString );

            if ( sourceRoot.isDirectory() )
            {
                String name = IdeUtils.toRelativeAndFixSeparator( projectBaseDir, sourceRoot, true );
                String location = IdeUtils.getCanonicalPath( sourceRoot ).replaceAll( "\\\\", "/" ); //$NON-NLS-1$ //$NON-NLS-2$

                addLink( writer, name, location, LINK_TYPE_DIRECTORY );
            }
        }
    }

    private void addResourceLinks( XMLWriter writer, File projectBaseDir, File basedir, List sourceRoots )
        throws MojoExecutionException
    {
        for ( Iterator it = sourceRoots.iterator(); it.hasNext(); )
        {
            String resourceDirString = ( (Resource) it.next() ).getDirectory();
            File resourceDir = new File( resourceDirString );

            if ( resourceDir.isDirectory() )
            {
                String name = IdeUtils.toRelativeAndFixSeparator( projectBaseDir, resourceDir, true );
                String location = IdeUtils.getCanonicalPath( resourceDir ).replaceAll( "\\\\", "/" ); //$NON-NLS-1$ //$NON-NLS-2$

                addLink( writer, name, location, LINK_TYPE_DIRECTORY );
            }
        }
    }

    /**
     * @param writer
     * @param name
     * @param location
     */
    private void addLink( XMLWriter writer, String name, String location, int type )
    {
        writer.startElement( "link" ); //$NON-NLS-1$

        writer.startElement( ELT_NAME );
        writer.writeText( name );
        writer.endElement(); // name

        writer.startElement( "type" ); //$NON-NLS-1$
        writer.writeText( Integer.toString( type ) );
        writer.endElement(); // type

        writer.startElement( "location" ); //$NON-NLS-1$

        writer.writeText( location );

        writer.endElement(); // location

        writer.endElement(); // link
    }
}
