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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.eclipse.BuildCommand;
import org.apache.maven.plugin.eclipse.LinkedResource;
import org.apache.maven.plugin.eclipse.Messages;
import org.apache.maven.plugin.ide.IdeDependency;
import org.apache.maven.plugin.ide.IdeUtils;
import org.codehaus.plexus.util.IOUtil;
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

    private static final String ELT_COMMENT = "comment"; //$NON-NLS-1$

    private static final String ELT_BUILD_COMMAND = "buildCommand"; //$NON-NLS-1$

    private static final String ELT_LINK = "link"; //$NON-NLS-1$

    private static final String ELT_BUILD_SPEC = "buildSpec"; //$NON-NLS-1$

    private static final String ELT_LINKED_RESOURCES = "linkedResources"; //$NON-NLS-1$

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
     * To Store the link names
     */
    ArrayList linkNames = new ArrayList();

    /**
     * @see org.apache.maven.plugin.eclipse.writers.EclipseWriter#write()
     */
    public void write()
        throws MojoExecutionException
    {

        Set projectnatures = new LinkedHashSet();
        Set buildCommands = new LinkedHashSet();
        Set linkedResources = new LinkedHashSet();

        File dotProject = new File( config.getEclipseProjectDirectory(), FILE_DOT_PROJECT );

        if ( dotProject.exists() )
        {

            log.info( Messages.getString( "EclipsePlugin.keepexisting", dotProject.getAbsolutePath() ) ); //$NON-NLS-1$

            // parse existing file in order to keep manually-added entries
            Reader reader = null;
            try
            {
                reader = new InputStreamReader( new FileInputStream( dotProject ), "UTF-8" );
                Xpp3Dom dom = Xpp3DomBuilder.build( reader );

                Xpp3Dom naturesElement = dom.getChild( ELT_NATURES );
                if ( naturesElement != null )
                {
                    Xpp3Dom[] existingNatures = naturesElement.getChildren( ELT_NATURE );
                    for (Xpp3Dom existingNature : existingNatures) {
                        // adds all the existing natures
                        projectnatures.add(existingNature.getValue());
                    }
                }

                Xpp3Dom buildSpec = dom.getChild( ELT_BUILD_SPEC );
                if ( buildSpec != null )
                {
                    Xpp3Dom[] existingBuildCommands = buildSpec.getChildren( ELT_BUILD_COMMAND );
                    for (Xpp3Dom existingBuildCommand : existingBuildCommands) {
                        Xpp3Dom buildCommandName = existingBuildCommand.getChild(ELT_NAME);
                        if (buildCommandName != null) {
                            buildCommands.add(new BuildCommand(existingBuildCommand));
                        }
                    }
                }
                // Added the below code to preserve the Symbolic links
                Xpp3Dom linkedResourcesElement = dom.getChild( ELT_LINKED_RESOURCES );
                if ( linkedResourcesElement != null )
                {
                    Xpp3Dom[] existingLinks = linkedResourcesElement.getChildren( ELT_LINK );
                    for (Xpp3Dom existingLink : existingLinks) {
                        Xpp3Dom linkName = existingLink.getChild(ELT_NAME);
                        if (linkName != null) {
                            // add all the existing symbolic links
                            linkNames.add(existingLink.getChild(ELT_NAME).getValue());
                            linkedResources.add(new LinkedResource(existingLink));
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
        for (Object o2 : config.getProjectnatures()) {
            projectnatures.add(o2);
        }

        for (Object o1 : config.getBuildCommands()) {
            buildCommands.add(o1);
        }

        for (Object o : config.getLinkedResources()) {
            linkedResources.add(o);
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

        XMLWriter writer = new PrettyPrintXMLWriter( w, "UTF-8", null );

        writer.startElement( "projectDescription" ); //$NON-NLS-1$

        writer.startElement( ELT_NAME );
        writer.writeText( config.getEclipseProjectName() );
        writer.endElement();

        addComment( writer, config.getProject().getDescription() );

        writer.startElement( "projects" ); //$NON-NLS-1$

        IdeDependency[] dependencies = config.getDeps();

        List duplicates = new ArrayList();
        for (IdeDependency dep : dependencies) {
            // Avoid duplicates entries when same project is refered using multiple types
            // (ejb, test-jar ...)
            if (dep.isReferencedProject() && !duplicates.contains(dep.getEclipseProjectName())) {
                writer.startElement("project"); //$NON-NLS-1$
                writer.writeText(dep.getEclipseProjectName());
                writer.endElement();
                duplicates.add(dep.getEclipseProjectName());
            }
        }

        writer.endElement(); // projects

        writer.startElement( ELT_BUILD_SPEC );

        for (Object buildCommand : buildCommands) {
            ((BuildCommand) buildCommand).print(writer);
        }

        writer.endElement(); // buildSpec

        writer.startElement( ELT_NATURES );

        for (Object projectnature : projectnatures) {
            writer.startElement(ELT_NATURE);
            writer.writeText((String) projectnature);
            writer.endElement(); // name
        }

        writer.endElement(); // natures

        boolean addLinks = !config.getProjectBaseDir().equals( config.getEclipseProjectDirectory() );

        if ( addLinks || linkedResources.size() > 0 )
        {
            writer.startElement( "linkedResources" ); //$NON-NLS-1$
            // preserve the symbolic links
            if ( linkedResources.size() > 0 )
            {
                for (Object linkedResource : linkedResources) {
                    ((LinkedResource) linkedResource).print(writer);
                }
            }

            if ( addLinks )
            {

                addFileLink( writer, config.getProjectBaseDir(), config.getEclipseProjectDirectory(),
                             config.getProject().getFile() );

                addSourceLinks( writer, config.getProjectBaseDir(), config.getEclipseProjectDirectory(),
                                config.getProject().getCompileSourceRoots() );
                addResourceLinks( writer, config.getProjectBaseDir(), config.getEclipseProjectDirectory(),
                                  config.getProject().getBuild().getResources() );

                addSourceLinks( writer, config.getProjectBaseDir(), config.getEclipseProjectDirectory(),
                                config.getProject().getTestCompileSourceRoots() );
                addResourceLinks( writer, config.getProjectBaseDir(), config.getEclipseProjectDirectory(),
                                  config.getProject().getBuild().getTestResources() );

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
            String location = IdeUtils.fixSeparator( IdeUtils.getCanonicalPath( file ) );

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
        for (Object sourceRoot1 : sourceRoots) {
            String sourceRootString = (String) sourceRoot1;
            File sourceRoot = new File(sourceRootString);

            if (sourceRoot.isDirectory()) {
                String name = IdeUtils.toRelativeAndFixSeparator(projectBaseDir, sourceRoot, true);
                String location = IdeUtils.fixSeparator(IdeUtils.getCanonicalPath(sourceRoot));

                addLink(writer, name, location, LINK_TYPE_DIRECTORY);
            }
        }
    }

    private void addResourceLinks( XMLWriter writer, File projectBaseDir, File basedir, List sourceRoots )
        throws MojoExecutionException
    {
        for (Object sourceRoot : sourceRoots) {
            String resourceDirString = ((Resource) sourceRoot).getDirectory();
            File resourceDir = new File(resourceDirString);

            if (resourceDir.isDirectory()) {
                String name = IdeUtils.toRelativeAndFixSeparator(projectBaseDir, resourceDir, true);
                String location = IdeUtils.fixSeparator(IdeUtils.getCanonicalPath(resourceDir));

                addLink(writer, name, location, LINK_TYPE_DIRECTORY);
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
        // Avoid duplicates entries of the link..
        if ( !linkNames.contains( name ) )
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

    private void addComment( XMLWriter writer, String projectDescription )
    {
        String comment = "";

        if ( projectDescription != null )
        {
            comment = projectDescription.trim();

            if ( comment.length() > 0 )
            {
                if ( !comment.endsWith( "." ) )
                {
                    comment += ".";
                }
                comment += " ";
            }
        }

        //
        // Project files that are generated with m-p-e cannot be supported by M2Eclipse
        //
        comment += "NO_M2ECLIPSE_SUPPORT: Project files created with the maven-eclipse-plugin are not supported in M2Eclipse.";

        writer.startElement( ELT_COMMENT );
        writer.writeText( comment );
        writer.endElement();
    }

}
