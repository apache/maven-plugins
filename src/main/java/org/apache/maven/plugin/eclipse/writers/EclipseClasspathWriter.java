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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.eclipse.BuildCommand;
import org.apache.maven.plugin.eclipse.EclipseSourceDir;
import org.apache.maven.plugin.eclipse.Messages;
import org.apache.maven.plugin.ide.IdeDependency;
import org.apache.maven.plugin.ide.IdeUtils;
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
    extends AbstractEclipseWriter
{

    /**
     * Eclipse build path variable M2_REPO
     */
    private static final String M2_REPO = "M2_REPO"; //$NON-NLS-1$

    /**
     * Attribute for sourcepath.
     */
    private static final String ATTR_SOURCEPATH = "sourcepath"; //$NON-NLS-1$

    /**
     * Attribute for output.
     */
    private static final String ATTR_OUTPUT = "output"; //$NON-NLS-1$

    /**
     * Attribute for path.
     */
    private static final String ATTR_PATH = "path"; //$NON-NLS-1$

    /**
     * Used for exported classpath entries.
     */
    private static final String ATTR_EXPORTED = "exported"; //$NON-NLS-1$

    /**
     * Attribute for kind - Container (con), Variable (var)..etc.
     */
    private static final String ATTR_KIND = "kind"; //$NON-NLS-1$

    /**
     * Attribute value for kind: var
     */
    private static final String ATTR_VAR = "var"; //$NON-NLS-1$

    /**
     * Attribute value for kind: lib
     */
    private static final String ATTR_LIB = "lib"; //$NON-NLS-1$

    /**
     * Attribute value for kind: src
     */
    private static final String ATTR_SRC = "src"; //$NON-NLS-1$

    /**
     * Attribute value for kind: src
     */
    private static final String ATTR_CON = "con"; //$NON-NLS-1$

    /**
     * Attribute name for source file includes in a path.
     */
    private static final String ATTR_INCLUDING = "including";

    /**
     * Attribute name for source file excludes in a path.
     */
    private static final String ATTR_EXCLUDING = "excluding";

    /**
     * Element for classpathentry.
     */
    private static final String ELT_CLASSPATHENTRY = "classpathentry"; //$NON-NLS-1$

    /**
     * Element for classpath.
     */
    private static final String ELT_CLASSPATH = "classpath"; //$NON-NLS-1$

    /**
     * File name that stores project classpath settings.
     */
    private static final String FILE_DOT_CLASSPATH = ".classpath"; //$NON-NLS-1$

    /**
     * @see org.apache.maven.plugin.eclipse.writers.EclipseWriter#write()
     */
    public void write()
        throws MojoExecutionException
    {

        FileWriter w;

        try
        {
            w = new FileWriter( new File( config.getEclipseProjectDirectory(), FILE_DOT_CLASSPATH ) );
        }
        catch ( IOException ex )
        {
            throw new MojoExecutionException( Messages.getString( "EclipsePlugin.erroropeningfile" ), ex ); //$NON-NLS-1$
        }

        XMLWriter writer = new PrettyPrintXMLWriter( w );

        writer.startElement( ELT_CLASSPATH );

        String defaultOutput = IdeUtils.toRelativeAndFixSeparator( config.getProjectBaseDir(), config
            .getBuildOutputDirectory(), false );

        // ----------------------------------------------------------------------
        // Source roots and resources
        // ----------------------------------------------------------------------

        // List<EclipseSourceDir>
        List specialSources = new ArrayList();

        // Map<String,List<EclipseSourceDir>>
        Map byOutputDir = new HashMap();

        for ( int j = 0; j < config.getSourceDirs().length; j++ )
        {
            EclipseSourceDir dir = config.getSourceDirs()[j];

            // List<EclipseSourceDir>
            List byOutputDirs = (List) byOutputDir.get( dir.getOutput() );
            if ( byOutputDirs == null )
            {
                // ArrayList<EclipseSourceDir>
                byOutputDir.put(
                    dir.getOutput() == null ? defaultOutput : dir.getOutput(),
                    byOutputDirs = new ArrayList() );
            }
            byOutputDirs.add( dir );
        }

        for ( int j = 0; j < config.getSourceDirs().length; j++ )
        {
            EclipseSourceDir dir = config.getSourceDirs()[j];

            log.debug( "Processing " + ( dir.isResource() ? "re" : "" ) + "source " + dir.getPath() + ": output="
                + dir.getOutput() + "; default output=" + defaultOutput );

            boolean isSpecial = false;
            
            // handle resource with nested output folders
            if ( dir.isResource() )
            {
                // Check if the output is a subdirectory of the default output,
                // and if the default output has any sources that copy there.

                if ( dir.getOutput() != null && !dir.getOutput().equals( defaultOutput )
                    && dir.getOutput().startsWith( defaultOutput ) && byOutputDir.get( defaultOutput ) != null
                    && !( (List) byOutputDir.get( defaultOutput ) ).isEmpty() )
                {
                    // do not specify as source since the output will be nested. Instead, mark
                    // it as a todo, and handle it with a custom build.xml file later.

                    log.debug( "Marking as special to prevent output folder nesting: " + dir.getPath() + " (output=" + dir.getOutput() +")");

                    isSpecial = true;
                    specialSources.add( dir );
                }
            }

            writer.startElement( ELT_CLASSPATHENTRY );

            writer.addAttribute( ATTR_KIND, "src" ); //$NON-NLS-1$
            writer.addAttribute( ATTR_PATH, dir.getPath() );

            if ( !isSpecial && dir.getOutput() != null && !defaultOutput.equals( dir.getOutput() ) )
            {
                writer.addAttribute( ATTR_OUTPUT, dir.getOutput() );
            }

            if ( StringUtils.isNotEmpty( dir.getInclude() ) )
            {
                writer.addAttribute( ATTR_INCLUDING, dir.getInclude() );
            }

            String excludes = dir.getExclude();

            if ( dir.isResource() )
            {
                // automatically exclude java files: eclipse doesn't have the concept of resource directory so it will
                // try to compile any java file found in maven resource dirs
                excludes = StringUtils.isEmpty( excludes ) ? "**/*.java" : excludes + "|**/*.java";
            }

            if ( StringUtils.isNotEmpty( excludes ) )
            {
                writer.addAttribute( ATTR_EXCLUDING, excludes );
            }

            writer.endElement();

        }

        // handle the special sources.
        if ( !specialSources.isEmpty() )
        {
            log.info( "Creating maven-eclipse.xml Ant file to handle resources" );

            try
            {
                FileWriter buildXmlWriter = new FileWriter( new File(
                    config.getEclipseProjectDirectory(),
                    "maven-eclipse.xml" ) );
                PrettyPrintXMLWriter buildXmlPrinter = new PrettyPrintXMLWriter( buildXmlWriter );

                buildXmlPrinter.startElement( "project" );
                buildXmlPrinter.addAttribute( "default", "copy-resources" );

                buildXmlPrinter.startElement( "target" );
                buildXmlPrinter.addAttribute( "name", "init" );
                // initialize filtering tokens here
                buildXmlPrinter.endElement();

                buildXmlPrinter.startElement( "target" );
                buildXmlPrinter.addAttribute( "name", "copy-resources" );
                buildXmlPrinter.addAttribute( "depends", "init" );

                for ( Iterator it = specialSources.iterator(); it.hasNext(); )
                {
                    // TODO: merge source dirs on output path+filtering to reduce
                    // <copy> tags for speed.
                    EclipseSourceDir dir = (EclipseSourceDir) it.next();
                    buildXmlPrinter.startElement( "copy");
                    buildXmlPrinter.addAttribute( "todir", dir.getOutput() );
                    buildXmlPrinter.addAttribute( "filtering", "" + dir.isFiltering() );

                    buildXmlPrinter.startElement( "fileset" );
                    buildXmlPrinter.addAttribute( "dir", dir.getPath() );
                    if ( dir.getInclude() != null )
                    {
                        buildXmlPrinter.addAttribute( "includes", dir.getInclude() );
                    }
                    if ( dir.getExclude() != null )
                    {
                        buildXmlPrinter.addAttribute( "excludes", dir.getExclude() );
                    }
                    buildXmlPrinter.endElement();

                    buildXmlPrinter.endElement();
                }

                buildXmlPrinter.endElement();

                buildXmlPrinter.endElement();

                IOUtil.close( buildXmlWriter );
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Cannot create " + config.getEclipseProjectDirectory()
                    + "/maven-eclipse.xml", e );
            }

            log.info( "Creating external launcher file" );
            // now create the launcher
            new EclipseAntExternalLaunchConfigurationWriter().init( log, config, "Maven_Ant_Builder.launch", "maven-eclipse.xml").write();

            // finally add it to the project writer.

            config.getBuildCommands().add(
                new BuildCommand(
                    "org.eclipse.ui.externaltools.ExternalToolBuilder",
                    "LaunchConfigHandle",
                    "<project>/" + EclipseLaunchConfigurationWriter.FILE_DOT_EXTERNAL_TOOL_BUILDERS
                        + "Maven_Ant_Builder.launch" ) );
        }

        // ----------------------------------------------------------------------
        // The default output
        // ----------------------------------------------------------------------

        writer.startElement( ELT_CLASSPATHENTRY );
        writer.addAttribute( ATTR_KIND, ATTR_OUTPUT );
        writer.addAttribute( ATTR_PATH, defaultOutput );
        writer.endElement();

        // ----------------------------------------------------------------------
        // Container classpath entries
        // ----------------------------------------------------------------------

        for ( Iterator it = config.getClasspathContainers().iterator(); it.hasNext(); )
        {
            writer.startElement( ELT_CLASSPATHENTRY );
            writer.addAttribute( ATTR_KIND, "con" ); //$NON-NLS-1$
            writer.addAttribute( ATTR_PATH, (String) it.next() );
            writer.endElement(); // name
        }

        // ----------------------------------------------------------------------
        // The dependencies
        // ----------------------------------------------------------------------

        for ( int j = 0; j < config.getDeps().length; j++ )
        {
            IdeDependency dep = config.getDeps()[j];

            if ( dep.isAddedToClasspath() )
            {
                addDependency( writer, dep );
            }
        }

        writer.endElement();

        IOUtil.close( w );

    }

    private void addDependency( XMLWriter writer, IdeDependency dep )
        throws MojoExecutionException
    {

        String path;
        String kind;
        String sourcepath = null;
        String javadocpath = null;

        if ( dep.isReferencedProject() && !config.isPde() )
        {
            path = "/" + dep.getArtifactId(); //$NON-NLS-1$
            kind = ATTR_SRC;
        }
        else
        {
            File artifactPath = dep.getFile();

            if ( artifactPath == null )
            {
                log.error( Messages.getString( "EclipsePlugin.artifactpathisnull", dep.getId() ) ); //$NON-NLS-1$
                return;
            }

            if ( dep.isSystemScoped() )
            {
                path = IdeUtils.toRelativeAndFixSeparator( config.getEclipseProjectDirectory(), artifactPath, false );

                if ( log.isDebugEnabled() )
                {
                    log.debug( Messages.getString( "EclipsePlugin.artifactissystemscoped", //$NON-NLS-1$
                                                   new Object[] { dep.getArtifactId(), path } ) );
                }

                kind = ATTR_LIB;
            }
            else
            {
                File localRepositoryFile = new File( config.getLocalRepository().getBasedir() );

                // if the dependency is not provided and the plugin runs in "pde mode", the dependency is
                // added to the Bundle-Classpath:
                if ( config.isPde() && ( dep.isProvided() || dep.isOsgiBundle() ) )
                {
                    return;
                }
                else if ( config.isPde() && !dep.isProvided() && !dep.isTestDependency() )
                {
                    // path for link created in .project, not to the actual file
                    path = dep.getFile().getName();

                    kind = ATTR_LIB;
                }
                // running in PDE mode and the dependency is provided means, that it is provided by
                // the target platform. This case is covered by adding the plugin container
                else
                {
                    String fullPath = artifactPath.getPath();

                    path = M2_REPO + "/" //$NON-NLS-1$
                        + IdeUtils.toRelativeAndFixSeparator( localRepositoryFile, new File( fullPath ), false );

                    kind = ATTR_VAR; //$NON-NLS-1$
                }

                if ( dep.getSourceAttachment() != null )
                {
                    if ( ATTR_VAR.equals( kind ) )
                    {
                        sourcepath = M2_REPO
                            + "/" //$NON-NLS-1$
                            + IdeUtils
                                .toRelativeAndFixSeparator( localRepositoryFile, dep.getSourceAttachment(), false );
                    }
                    else
                    {
                        // source archive must be referenced with the full path, we can't mix a lib with a variable
                        sourcepath = IdeUtils.getCanonicalPath( dep.getSourceAttachment() );
                    }
                }

                if ( dep.getJavadocAttachment() != null )
                {
                    // NB eclipse (3.1) doesn't support variables in javadoc paths, so we need to add the
                    // full path for the maven repo
                    javadocpath = StringUtils.replace( IdeUtils.getCanonicalPath( dep.getJavadocAttachment() ),
                                                       "\\", "/" ); //$NON-NLS-1$ //$NON-NLS-2$
                }

            }

        }

        writer.startElement( ELT_CLASSPATHENTRY );
        writer.addAttribute( ATTR_KIND, kind );
        writer.addAttribute( ATTR_PATH, path );

        if ( sourcepath != null )
        {
            writer.addAttribute( ATTR_SOURCEPATH, sourcepath );
        }

        if ( javadocpath != null )
        {
            writer.startElement( "attributes" ); //$NON-NLS-1$

            writer.startElement( "attribute" ); //$NON-NLS-1$
            writer.addAttribute( "value", "jar:file:/" + javadocpath + "!/" ); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            writer.addAttribute( "name", "javadoc_location" ); //$NON-NLS-1$ //$NON-NLS-2$
            writer.endElement();

            writer.endElement();
        }

        writer.endElement();

    }
}
