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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.maven.model.ReportPlugin;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.codehaus.doxia.site.renderer.SiteRenderer;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.PathTool;
import org.codehaus.plexus.util.StringUtils;

/**
 * Base class for the PMD reports.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public abstract class AbstractPmdReport
    extends AbstractMavenReport
{
    /**
     * The output directory for the intermediate XML report.
     *
     * @parameter expression="${project.build.directory}"
     * @required
     */
    protected File targetDirectory;

    /**
     * The output directory for the final HTML report.
     *
     * @parameter expression="${project.reporting.outputDirectory}"
     * @required
     */
    protected String outputDirectory;

    /**
     * Site rendering component for generating the HTML report.
     *
     * @component
     */
    private SiteRenderer siteRenderer;

    /**
     * The project to analyse.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;

    /**
     * Set the output format type, in addition to the HTML report.  Must be one of: "none",
     * "csv", "xml", "txt" or the full class name of the PMD renderer to use.
     * See the net.sourceforge.pmd.renderers package javadoc for available renderers.
     * XML is required if the pmd:check goal is being used.
     *
     * @parameter expression="${format}" default-value="xml"
     */
    protected String format = "xml";

    /**
     * Link the violation line numbers to the source xref. Links will be created
     * automatically if the jxr plugin is being used.
     *
     * @parameter expression="${linkXRef}" default-value="true"
     */
    private boolean linkXRef;

    /**
     * Location of the Xrefs to link to.
     *
     * @parameter default-value="${project.reporting.outputDirectory}/xref"
     */
    private File xrefLocation;
    
    /**
     * Location of the Test Xrefs to link to.
     *
     * @parameter default-value="${project.reporting.outputDirectory}/xref-test"
     */
    private File xrefTestLocation;

    /**
     * A list of files to exclude from checking. Can contain ant-style wildcards and double wildcards.
     *
     * @parameter
     * @since 2.2
     */
    private String[] excludes;
    
    /**
     * A list of files to include from checking. Can contain ant-style wildcards and double wildcards.  
     * Defaults to **\/*.java
     *
     * @since 2.2
     * @parameter
     */
    private String[] includes;

    
    
    
    /**
     * The source directories containing the sources to be compiled.
     *
     * @parameter expression="${project.compileSourceRoots}"
     * @required
     * @readonly
     */
    private List compileSourceRoots;
    
    /**
     * The source directories containing the test-sources to be compiled.
     *
     * @parameter expression="${project.testCompileSourceRoots}"
     * @required
     * @readonly
     */
    private List testSourceRoots;
    
    /**
     * The project source directories that should be excluded.
     *
     * @since 2.2
     * @parameter
     */
    private List excludeRoots;
    
    /**
     * Run PMD on the tests
     *
     * @parameter default-value="false"
     * @since 2.2
     */
    protected boolean includeTests;
    
    /**
     * Whether to build an aggregated report at the root, or build individual reports.
     *
     * @parameter expression="${aggregate}" default-value="false"
     * @since 2.2
     */
    protected boolean aggregate;
        
    
    /**
     * The projects in the reactor for aggregation report.
     *
     * @parameter expression="${reactorProjects}"
     * @readonly
     */
    protected List reactorProjects;

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#getProject()
     */
    protected MavenProject getProject()
    {
        return project;
    }

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#getSiteRenderer()
     */
    protected SiteRenderer getSiteRenderer()
    {
        return siteRenderer;
    }

    protected String constructXRefLocation( boolean test )
    {
        String location = null;
        if ( linkXRef )
        {
            File xrefLoc = test ? xrefTestLocation : xrefLocation;
            
            String relativePath = PathTool.getRelativePath( outputDirectory, xrefLoc.getAbsolutePath() );
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
                for ( Iterator reports = project.getReportPlugins().iterator(); reports.hasNext(); )
                {
                    ReportPlugin plugin = (ReportPlugin) reports.next();

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
    protected Map getFilesToProcess( )
        throws IOException
    {
        String sourceXref = constructXRefLocation( false );
        String testXref = includeTests ? constructXRefLocation( true ) : "";
        
        if ( aggregate && !project.isExecutionRoot() )
        {
            return Collections.EMPTY_MAP;
        }

        if ( excludeRoots == null )
        {
            excludeRoots = Collections.EMPTY_LIST;
        }
        List excludeRootFiles = new ArrayList( excludeRoots.size() );
        
        for ( Iterator it = excludeRoots.iterator(); it.hasNext(); ) 
        {
            String root = (String) it.next();
            File file = new File( root );
            if ( file.exists()
                && file.isDirectory() )
            {
                excludeRootFiles.add( file );
            }
        }
        
        List directories = new ArrayList();
        
        for ( Iterator i = compileSourceRoots.iterator(); i.hasNext(); )
        {
            String root = (String) i.next();
            File sroot = new File( root );
            directories.add( new PmdFileInfo( project, sroot, sourceXref ) );
        }
        
        if ( includeTests )
        {
            for ( Iterator i = testSourceRoots.iterator(); i.hasNext(); )
            {
                String root = (String) i.next();
                File sroot = new File( root );
                directories.add( new PmdFileInfo( project, sroot, testXref ) );
            }
        }
        if ( aggregate )
        {
            for ( Iterator i = reactorProjects.iterator(); i.hasNext(); )
            {
                MavenProject localProject = (MavenProject) i.next();
                for ( Iterator i2 = localProject.getCompileSourceRoots().iterator(); i2.hasNext(); )
                {
                    String root = (String) i2.next();
                    File sroot = new File( root );
                    directories.add( new PmdFileInfo( localProject, sroot, sourceXref ) );
                }
                if ( includeTests )
                {
                    for ( Iterator i2 = localProject.getTestCompileSourceRoots().iterator(); i2.hasNext(); )
                    {
                        String root = (String) i2.next();
                        File sroot = new File( root );
                        directories.add( new PmdFileInfo( localProject, sroot, testXref ) );
                    }
                }
            }
  
        }
        
        String excluding = getIncludeExcludeString( excludes );
        String including = getIncludeExcludeString( includes );
        Map files = new TreeMap();
        
        if ( "".equals( including ) )
        {
            including = "**/*.java";
        }

        StringBuffer excludesStr = new StringBuffer();
        if ( StringUtils.isNotEmpty( excluding ) )
        {
            excludesStr.append( excluding );
        }
        String[] defaultExcludes = FileUtils.getDefaultExcludes();
        for ( int i = 0; i < defaultExcludes.length; i++ )
        {
            if ( excludesStr.length() > 0 )
            {
                excludesStr.append( "," );
            }
            excludesStr.append( defaultExcludes[i] );
        }
        getLog().debug( "Excluded files: '" + excludesStr + "'" );

        for ( Iterator it = directories.iterator(); it.hasNext(); )
        {
            PmdFileInfo finfo = (PmdFileInfo) it.next();
            File sourceDirectory = finfo.getSourceDirectory();
            if ( sourceDirectory.exists()
                && sourceDirectory.isDirectory()
                && !excludeRootFiles.contains( sourceDirectory ) )
            {
                List newfiles = FileUtils.getFiles( sourceDirectory, including, excludesStr.toString() );
                for ( Iterator it2 = newfiles.iterator(); it2.hasNext(); )
                {
                    files.put( it2.next(), finfo );
                }
            }
        }        
                
        return files;
    }

    /**
     * Convenience method that concatenates the files to be excluded into the appropriate format
     *
     * @param exclude the array of Strings that contains the files to be excluded
     * @return a String that contains the concatenates file names
     */
    private String getIncludeExcludeString( String[] arr )
    {
        StringBuffer str = new StringBuffer();

        if ( arr != null )
        {
            for ( int index = 0; index < arr.length; index++ )
            {
                if ( str.length() > 0 )
                {
                    str.append( ',' );
                }
                str.append( arr[index] );
            }
        }

        return str.toString();
    }


    protected boolean isHtml()
    {
        return "html".equals( format );
    }

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#canGenerateReport()
     */
    public boolean canGenerateReport()
    {
        if ( aggregate && !project.isExecutionRoot() )
        {
            return false;
        }

        // if format is XML, we need to output it even if the file list is empty
        // so the "check" goals can check for failures
        if ( "xml".equals( format ) )
        {
            return true;
        }
        try 
        {
            Map filesToProcess = getFilesToProcess( );
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
     * @see org.apache.maven.reporting.AbstractMavenReport#getOutputDirectory()
     */
    protected String getOutputDirectory()
    {
        return outputDirectory;
    }
}
