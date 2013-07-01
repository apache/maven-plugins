package org.apache.maven.plugin.pmd;

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

import net.sourceforge.pmd.PMD;
import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.PathTool;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Base class for the PMD reports.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public abstract class AbstractPmdReport
    extends AbstractMavenReport
{
    /**
     * The output directory for the intermediate XML report.
     */
    @Parameter( property = "project.build.directory", required = true )
    protected File targetDirectory;

    /**
     * The output directory for the final HTML report. Note that this parameter is only evaluated if the goal is run
     * directly from the command line or during the default lifecycle. If the goal is run indirectly as part of a site
     * generation, the output directory configured in the Maven Site Plugin is used instead.
     */
    @Parameter( property = "project.reporting.outputDirectory", required = true )
    protected File outputDirectory;

    /**
     * Site rendering component for generating the HTML report.
     */
    @Component
    private Renderer siteRenderer;

    /**
     * The project to analyse.
     */
    @Component
    protected MavenProject project;

    /**
     * Set the output format type, in addition to the HTML report.  Must be one of: "none",
     * "csv", "xml", "txt" or the full class name of the PMD renderer to use.
     * See the net.sourceforge.pmd.renderers package javadoc for available renderers.
     * XML is required if the pmd:check goal is being used.
     */
    @Parameter( property = "format", defaultValue = "xml" )
    protected String format = "xml";

    /**
     * Link the violation line numbers to the source xref. Links will be created
     * automatically if the jxr plugin is being used.
     */
    @Parameter( property = "linkXRef", defaultValue = "true" )
    private boolean linkXRef;

    /**
     * Location of the Xrefs to link to.
     */
    @Parameter( defaultValue = "${project.reporting.outputDirectory}/xref" )
    private File xrefLocation;

    /**
     * Location of the Test Xrefs to link to.
     */
    @Parameter( defaultValue = "${project.reporting.outputDirectory}/xref-test" )
    private File xrefTestLocation;

    /**
     * A list of files to exclude from checking. Can contain Ant-style wildcards and double wildcards. Note that these
     * exclusion patterns only operate on the path of a source file relative to its source root directory. In other
     * words, files are excluded based on their package and/or class name. If you want to exclude entire source root
     * directories, use the parameter <code>excludeRoots</code> instead.
     *
     * @since 2.2
     */
    @Parameter
    private List<String> excludes;

    /**
     * A list of files to include from checking. Can contain Ant-style wildcards and double wildcards.
     * Defaults to **\/*.java.
     *
     * @since 2.2
     */
    @Parameter
    private List<String> includes;

    /**
     * The directories containing the sources to be compiled.
     */
    @Parameter( property = "project.compileSourceRoots", required = true, readonly = true )
    private List<String> compileSourceRoots;

    /**
     * The directories containing the test-sources to be compiled.
     */
    @Parameter( property = "project.testCompileSourceRoots", required = true, readonly = true )
    private List<String> testSourceRoots;

    /**
     * The project source directories that should be excluded.
     *
     * @since 2.2
     */
    @Parameter
    private File[] excludeRoots;

    /**
     * Run PMD on the tests.
     *
     * @since 2.2
     */
    @Parameter( defaultValue = "false" )
    protected boolean includeTests;

    /**
     * Whether to build an aggregated report at the root, or build individual reports.
     *
     * @since 2.2
     */
    @Parameter( property = "aggregate", defaultValue = "false" )
    protected boolean aggregate;

    /**
     * The file encoding to use when reading the Java sources.
     *
     * @since 2.3
     */
    @Parameter( property = "encoding", defaultValue = "${project.build.sourceEncoding}" )
    private String sourceEncoding;

    /**
     * The file encoding when writing non-HTML reports.
     *
     * @since 2.5
     */
    @Parameter( property = "outputEncoding", defaultValue = "${project.reporting.outputEncoding}" )
    private String outputEncoding;

    /**
     * The projects in the reactor for aggregation report.
     */
    @Parameter( property = "reactorProjects", readonly = true )
    protected List<MavenProject> reactorProjects;

    /**
     * Whether to include the xml files generated by PMD/CPD in the site.<br/>
     * <strong>Note:</strong> In versions 2.2 - 2.7.1 the default value for this
     * was <code>true</code>. This was changed in version 3.0.
     *
     * @since 3.0
     */
    @Parameter( defaultValue = "false" )
    protected boolean includeXmlInSite;

    /**
     * Skip the PMD/CPD report generation if there are no violations or duplications found.
     * Defaults to <code>true</code>.
     *
     * @since 3.1
     */
    @Parameter( defaultValue = "true" )
    protected boolean skipEmptyReport;

    /** The files that are being analyzed. */
    protected Map<File, PmdFileInfo> filesToProcess;

    /**
     * {@inheritDoc}
     */
    protected MavenProject getProject()
    {
        return project;
    }

    /**
     * {@inheritDoc}
     */
    protected Renderer getSiteRenderer()
    {
        return siteRenderer;
    }

    protected String constructXRefLocation( boolean test )
    {
        String location = null;
        if ( linkXRef )
        {
            File xrefLoc = test ? xrefTestLocation : xrefLocation;

            String relativePath =
                PathTool.getRelativePath( outputDirectory.getAbsolutePath(), xrefLoc.getAbsolutePath() );
            if ( StringUtils.isEmpty( relativePath ) )
            {
                relativePath = ".";
            }
            relativePath = relativePath + "/" + xrefLoc.getName();
            if ( xrefLoc.exists() )
            {
                // XRef was already generated by manual execution of a lifecycle binding
                location = relativePath;
            }
            else
            {
                // Not yet generated - check if the report is on its way
                @SuppressWarnings( "unchecked" ) List<ReportPlugin> reportPlugins = project.getReportPlugins();
                for ( ReportPlugin plugin : reportPlugins )
                {
                    String artifactId = plugin.getArtifactId();
                    if ( "maven-jxr-plugin".equals( artifactId ) || "jxr-maven-plugin".equals( artifactId ) )
                    {
                        location = relativePath;
                    }
                }
            }

            if ( location == null )
            {
                getLog().warn( "Unable to locate Source XRef to link to - DISABLED" );
            }
        }
        return location;
    }

    /**
     * Convenience method to get the list of files where the PMD tool will be executed
     *
     * @return a List of the files where the PMD tool will be executed
     * @throws java.io.IOException
     */
    protected Map<File, PmdFileInfo> getFilesToProcess()
        throws IOException
    {
        String sourceXref = constructXRefLocation( false );
        String testXref = includeTests ? constructXRefLocation( true ) : "";

        if ( aggregate && !project.isExecutionRoot() )
        {
            return Collections.emptyMap();
        }

        if ( excludeRoots == null )
        {
            excludeRoots = new File[0];
        }

        Collection<File> excludeRootFiles = new HashSet<File>( excludeRoots.length );

        for ( int i = 0; i < excludeRoots.length; i++ )
        {
            File file = excludeRoots[i];
            if ( file.isDirectory() )
            {
                excludeRootFiles.add( file );
            }
        }

        List<PmdFileInfo> directories = new ArrayList<PmdFileInfo>();

        if ( compileSourceRoots != null )
        {

            for ( String root : compileSourceRoots )
            {
                File sroot = new File( root );
                directories.add( new PmdFileInfo( project, sroot, sourceXref ) );
            }

        }
        if ( includeTests )
        {
            if ( testSourceRoots != null )
            {
                for ( String root : testSourceRoots )
                {
                    File sroot = new File( root );
                    directories.add( new PmdFileInfo( project, sroot, testXref ) );
                }
            }
        }
        if ( aggregate )
        {
            for ( MavenProject localProject : reactorProjects )
            {
                @SuppressWarnings( "unchecked" ) List<String> localCompileSourceRoots =
                    localProject.getCompileSourceRoots();
                for ( String root : localCompileSourceRoots )
                {
                    File sroot = new File( root );
                    directories.add( new PmdFileInfo( localProject, sroot, sourceXref ) );
                }
                if ( includeTests )
                {
                    @SuppressWarnings( "unchecked" ) List<String> localTestCompileSourceRoots =
                        localProject.getTestCompileSourceRoots();
                    for ( String root : localTestCompileSourceRoots )
                    {
                        File sroot = new File( root );
                        directories.add( new PmdFileInfo( localProject, sroot, testXref ) );
                    }
                }
            }

        }

        String excluding = getExcludes();
        getLog().debug( "Exclusions: " + excluding );
        String including = getIncludes();
        getLog().debug( "Inclusions: " + including );

        Map<File, PmdFileInfo> files = new TreeMap<File, PmdFileInfo>();

        for ( PmdFileInfo finfo : directories )
        {
            getLog().debug( "Searching for files in directory " + finfo.getSourceDirectory().toString() );
            File sourceDirectory = finfo.getSourceDirectory();
            if ( sourceDirectory.isDirectory() && !excludeRootFiles.contains( sourceDirectory ) )
            {
                @SuppressWarnings( "unchecked" ) List<File> newfiles =
                    FileUtils.getFiles( sourceDirectory, including, excluding );
                for ( Iterator<File> it2 = newfiles.iterator(); it2.hasNext(); )
                {
                    files.put( it2.next().getCanonicalFile(), finfo );
                }
            }
        }

        return files;
    }

    /**
     * Gets the comma separated list of effective include patterns.
     *
     * @return The comma separated list of effective include patterns, never <code>null</code>.
     */
    private String getIncludes()
    {
        Collection<String> patterns = new LinkedHashSet<String>();
        if ( includes != null )
        {
            patterns.addAll( includes );
        }
        if ( patterns.isEmpty() )
        {
            patterns.add( "**/*.java" );
        }
        return StringUtils.join( patterns.iterator(), "," );
    }

    /**
     * Gets the comma separated list of effective exclude patterns.
     *
     * @return The comma separated list of effective exclude patterns, never <code>null</code>.
     */
    private String getExcludes()
    {
        @SuppressWarnings( "unchecked" ) Collection<String> patterns =
            new LinkedHashSet<String>( FileUtils.getDefaultExcludesAsList() );
        if ( excludes != null )
        {
            patterns.addAll( excludes );
        }
        return StringUtils.join( patterns.iterator(), "," );
    }

    protected boolean isHtml()
    {
        return "html".equals( format );
    }
    protected boolean isXml()
    {
        return "xml".equals( format );
    }

    /**
     * {@inheritDoc}
     */
    public boolean canGenerateReport()
    {
        if ( aggregate && !project.isExecutionRoot() )
        {
            return false;
        }

        if ( "pom".equals( project.getPackaging() ) && !aggregate )
        {
            return false;
        }

        // if format is XML, we need to output it even if the file list is empty
        // so the "check" goals can check for failures
        if ( isXml() )
        {
            return true;
        }
        try
        {
            filesToProcess = getFilesToProcess();
            if ( filesToProcess.isEmpty() )
            {
                return false;
            }
        }
        catch ( IOException e )
        {
            getLog().error( e );
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    protected String getOutputDirectory()
    {
        return outputDirectory.getAbsolutePath();
    }

    protected String getSourceEncoding()
    {
        return sourceEncoding;
    }

    /**
     * Gets the effective reporting output files encoding.
     *
     * @return The effective reporting output file encoding, never <code>null</code>.
     * @since 2.5
     */
    protected String getOutputEncoding()
    {
        return ( outputEncoding != null ) ? outputEncoding : ReaderFactory.UTF_8;
    }

    static String getPmdVersion()
    {
        try
        {
            return (String) PMD.class.getField( "VERSION" ).get( null );
        }
        catch ( IllegalAccessException e )
        {
            throw new RuntimeException( "PMD VERSION field not accessible", e );
        }
        catch ( NoSuchFieldException e )
        {
            throw new RuntimeException( "PMD VERSION field not found", e );
        }
    }
}