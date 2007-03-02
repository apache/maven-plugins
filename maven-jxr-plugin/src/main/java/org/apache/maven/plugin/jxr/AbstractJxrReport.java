package org.apache.maven.plugin.jxr;

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

import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.jxr.JXR;
import org.apache.maven.jxr.JxrException;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Base class for the JXR reports.
 *
 * @author <a href="mailto:bellingard.NO-SPAM@gmail.com">Fabrice Bellingard</a>
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public abstract class AbstractJxrReport
    extends AbstractMavenReport
{
    /**
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * @component
     */
    private Renderer siteRenderer;

    /**
     * Output folder where the main page of the report will be generated.
     *
     * @parameter expression="${project.reporting.outputDirectory}"
     * @required
     */
    private String outputDirectory;

    /**
     * File input encoding.
     *
     * @parameter default-value="ISO-8859-1"
     */
    private String inputEncoding;

    /**
     * File output encoding.
     *
     * @parameter default-value="ISO-8859-1"
     */
    private String outputEncoding;

    /**
     * Title of window of the Xref HTML files.
     *
     * @parameter expression="${project.name} ${project.version} Reference"
     */
    private String windowTitle;

    /**
     * Title of main page of the Xref HTML files.
     *
     * @parameter expression="${project.name} ${project.version} Reference"
     */
    private String docTitle;

    /**
     * String uses at the bottom of the Xref HTML files.
     *
     * @parameter expression="${bottom}" default-value="Copyright &copy; {inceptionYear}-{currentYear} ${project.organization.name}. All Rights Reserved."
     */
    private String bottom;

    /**
     * Directory where Velocity templates can be found to generate overviews,
     * frames and summaries.
     * Should not be used. If used, should be an absolute path, like "${basedir}/myTemplates".
     *
     * @parameter default-value="templates"
     */
    private String templateDir;

    /**
     * Style sheet used for the Xref HTML files.
     * Should not be used. If used, should be an absolute path, like "${basedir}/myStyles.css".
     *
     * @parameter default-value="stylesheet.css"
     */
    private String stylesheet;

    /**
     * A list of exclude patterns to use. By default no files are excluded.
     *
     * @parameter expression="${excludes}"
     * @since 2.1
     */
    private ArrayList excludes;

    /**
     * A list of include patterns to use. By default all .java files are included.
     *
     * @parameter expression="${includes}"
     * @since 2.1
     */
    private ArrayList includes;

    /**
     * The projects in the reactor for aggregation report.
     *
     * @parameter expression="${reactorProjects}"
     * @readonly
     */
    protected List reactorProjects;

    /**
     * Whether to build an aggregated report at the root, or build individual reports.
     *
     * @parameter expression="${aggregate}" default-value="false"
     */
    protected boolean aggregate;

    /**
     * Compiles the list of directories which contain source files that will be included in the JXR report generation.
     *
     * @param sourceDirs    the List of the source directories
     * @return a List of the directories that will be included in the JXR report generation
     */
    protected List pruneSourceDirs( List sourceDirs )
    {
        List pruned = new ArrayList( sourceDirs.size() );
        for ( Iterator i = sourceDirs.iterator(); i.hasNext(); )
        {
            String dir = (String) i.next();
            if ( !pruned.contains( dir ) && hasSources( new File( dir ) ) )
            {
                pruned.add( dir );
            }
        }
        return pruned;
    }

    /**
     * Initialize some attributes required during the report generation
     */
    protected void init()
    {
        // wanna know if Javadoc is being generated
        // TODO: what if it is not part of the site though, and just on the command line?
        Collection plugin = project.getReportPlugins();
        if ( plugin != null )
        {
            for ( Iterator iter = plugin.iterator(); iter.hasNext(); )
            {
                ReportPlugin reportPlugin = (ReportPlugin) iter.next();
                if ( "maven-javadoc-plugin".equals( reportPlugin.getArtifactId() ) )
                {
                    break;
                }
            }
        }
    }

    /**
     * Checks whether the given directory contains Java files.
     *
     * @param dir the source directory
     * @return true if the folder or one of its subfolders coantins at least 1 Java file
     */
    private boolean hasSources( File dir )
    {
        boolean found = false;
        if ( dir.exists() && dir.isDirectory() )
        {
            File[] files = dir.listFiles();
            for ( int i = 0; i < files.length && !found; i++ )
            {
                File currentFile = files[i];
                if ( currentFile.isFile() && currentFile.getName().endsWith( ".java" ) )
                {
                    found = true;
                }
                else if ( currentFile.isDirectory() )
                {
                    boolean hasSources = hasSources( currentFile );
                    if ( hasSources )
                    {
                        found = true;
                    }
                }
            }
        }
        return found;
    }

    /**
     * Creates the Xref for the Java files found in the given source directory and puts
     * them in the given destination directory.
     *
     * @param locale               The user locale to use for the Xref generation
     * @param destinationDirectory The output folder
     * @param sourceDirs           The source directories
     * @throws java.io.IOException
     * @throws org.apache.maven.jxr.JxrException
     *
     */
    private void createXref( Locale locale, String destinationDirectory, List sourceDirs )
        throws IOException, JxrException
    {
        JXR jxr = new JXR();
        jxr.setDest( destinationDirectory );
        jxr.setInputEncoding( inputEncoding );
        jxr.setLocale( locale );
        jxr.setLog( new PluginLogAdapter( getLog() ) );
        jxr.setOutputEncoding( outputEncoding );
        jxr.setRevision( "HEAD" );
        jxr.setJavadocLinkDir( getJavadocLocation() );
        // Set include/exclude patterns on the jxr instance
        if ( excludes != null && !excludes.isEmpty() )
        {
            jxr.setExcludes( (String[]) excludes.toArray( new String[0] ) );
        }
        if ( includes != null && !includes.isEmpty() )
        {
            jxr.setIncludes( (String[]) includes.toArray( new String[0] ) );
        }

        jxr.xref( sourceDirs, templateDir, windowTitle, docTitle, getBottomText( project.getInceptionYear() ) );

        // and finally copy the stylesheet
        copyRequiredResources( destinationDirectory );
    }

    /**
     * Get the bottom text to be displayed at the lower part of the generated JXR reports.
     *
     * @param inceptionYear the year when the project was started
     * @return  a String that contains the bottom text to be displayed in the lower part of the generated JXR reports
     */
    private String getBottomText( String inceptionYear )
    {
        int actualYear = Calendar.getInstance().get( Calendar.YEAR );
        String year = String.valueOf( actualYear );

        String bottom = StringUtils.replace( this.bottom, "{currentYear}", year );

        if ( inceptionYear == null )
        {
            bottom = StringUtils.replace( bottom, "{inceptionYear}-", "" );
        }
        else
        {
            if ( inceptionYear.equals( year ) )
            {
                bottom = StringUtils.replace( bottom, "{inceptionYear}-", "" );
            }
            else
            {
                bottom = StringUtils.replace( bottom, "{inceptionYear}", inceptionYear );
            }
        }
        return bottom;
    }

    /**
     * Copy some required resources (like the stylesheet) to the
     * given directory
     *
     * @param dir the directory to copy the resources to
     */
    private void copyRequiredResources( String dir )
    {
        File stylesheetFile = new File( stylesheet );
        File destStylesheetFile = new File( dir, "stylesheet.css" );

        try
        {
            if ( stylesheetFile.isAbsolute() )
            {
                FileUtils.copyFile( stylesheetFile, destStylesheetFile );
            }
            else
            {
                URL stylesheetUrl = this.getClass().getClassLoader().getResource( stylesheet );
                FileUtils.copyURLToFile( stylesheetUrl, destStylesheetFile );
            }
        }
        catch ( IOException e )
        {
            getLog().warn( "An error occured while copying the stylesheet to the target directory", e );
        }

    }

    /**
     * Cf. overriden method documentation.
     *
     * @see org.apache.maven.reporting.AbstractMavenReport#getSiteRenderer()
     */
    protected Renderer getSiteRenderer()
    {
        return siteRenderer;
    }

    /**
     * Cf. overriden method documentation.
     *
     * @see org.apache.maven.reporting.AbstractMavenReport#getOutputDirectory()
     */
    protected String getOutputDirectory()
    {
        return outputDirectory;
    }

    /**
     * Cf. overriden method documentation.
     *
     * @see org.apache.maven.reporting.AbstractMavenReport#getProject()
     */
    public MavenProject getProject()
    {
        return project;
    }

    /**
     * Returns the correct resource bundle according to the locale
     *
     * @param locale :
     *               the locale of the user
     * @return the bundle correponding to the locale
     */
    protected ResourceBundle getBundle( Locale locale )
    {
        return ResourceBundle.getBundle( "jxr-report", locale, this.getClass().getClassLoader() );
    }

    protected boolean canGenerateReport( List sourceDirs )
    {
        boolean canGenerate = !sourceDirs.isEmpty();

        if ( aggregate && !project.isExecutionRoot() )
        {
            canGenerate = false;
        }
        return canGenerate;
    }

    /**
     * Cf. overriden method documentation.
     *
     * @see org.apache.maven.reporting.AbstractMavenReport#executeReport(java.util.Locale)
     */
    protected void executeReport( Locale locale )
        throws MavenReportException
    {
        List sourceDirs = constructSourceDirs();
        if ( canGenerateReport( sourceDirs ) )
        {
            // init some attributes -- TODO (javadoc)
            init();

            try
            {
                createXref( locale, getDestinationDirectory(), sourceDirs );
            }
            catch ( JxrException e )
            {
                throw new MavenReportException( "Error while generating the HTML source code of the projet.", e );
            }
            catch ( IOException e )
            {
                throw new MavenReportException( "Error while generating the HTML source code of the projet.", e );
            }
        }
    }

    /**
     * Gets the list of the source directories to be included in the JXR report generation
     *
     * @return a List of the source directories whose contents will be included in the JXR report generation
     */
    protected List constructSourceDirs()
    {
        List sourceDirs = new ArrayList( getSourceRoots() );
        if ( aggregate )
        {
            for ( Iterator i = reactorProjects.iterator(); i.hasNext(); )
            {
                MavenProject project = (MavenProject) i.next();

                if ( "java".equals( project.getArtifact().getArtifactHandler().getLanguage() ) )
                {
                    sourceDirs.addAll( getSourceRoots( project ) );
                }
            }
        }

        sourceDirs = pruneSourceDirs( sourceDirs );
        return sourceDirs;
    }

    /**
     * Cf. overriden method documentation.
     *
     * @see org.apache.maven.reporting.AbstractMavenReport#canGenerateReport()
     */
    public boolean canGenerateReport()
    {
        return canGenerateReport( constructSourceDirs() );
    }

    /**
     * Cf. overriden method documentation.
     *
     * @see org.apache.maven.reporting.AbstractMavenReport#isExternalReport()
     */
    public boolean isExternalReport()
    {
        return true;
    }

    /**
     * Abstract method that returns the target directory where the generated JXR reports will be put.
     *
     * @return  a String that contains the target directory name
     */
    protected abstract String getDestinationDirectory();

    /**
     * Abstract method that returns the specified source directories that will be included in the JXR report generation.
     *
     * @return a List of the source directories
     */
    protected abstract List getSourceRoots();

    /**
     * Abstract method that returns the compile source directories of the specified project that will be included in the
     * JXR report generation
     *
     * @param project the MavenProject where the JXR report plugin will be executed
     * @return a List of the source directories
     */
    protected abstract List getSourceRoots( MavenProject project );

    /**
     * Abstract method that returns the location of the javadoc files.
     *
     * @return a String that contains the loaction of the javadocs
     */
    protected abstract String getJavadocLocation();
}
