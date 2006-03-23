package org.apache.maven.plugin.jxr;

/*
 * Copyright 2004-2005 The Apache Software Foundation.
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

import org.apache.maven.jxr.JXR;
import org.apache.maven.jxr.JxrException;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.doxia.sink.Sink;
import org.codehaus.doxia.site.renderer.SiteRenderer;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * MOJO for the JXR report.
 * Creates an html-based, cross referenced version of Java source code
 * for a project.
 *
 * @author <a href="mailto:bellingard.NO-SPAM@gmail.com">Fabrice Bellingard</a>
 * @goal jxr
 */
public class JxrReport
    extends AbstractMavenReport
{

    /**
     * @parameter expression="${project}"
     * @required @readonly
     */
    private MavenProject project;

    /**
     * @parameter expression="${component.org.codehaus.doxia.site.renderer.SiteRenderer}"
     * @required @readonly
     */
    private SiteRenderer siteRenderer;

    /**
     * Source directories of the project.
     *
     * @parameter expression="${project.compileSourceRoots}"
     * @required
     * @readonly
     */
    private List sourceDirs;

    /**
     * Test directories of the project.
     *
     * @parameter expression="${project.testCompileSourceRoots}"
     * @required
     * @readonly
     */
    private List testSourceDirs;

    /**
     * Output folder where the main page of the report will be generated.
     *
     * @parameter expression="${project.build.directory}/site"
     * @required
     */
    private String outputDirectory;

    /**
     * Folder where the Xref files will be copied to.
     *
     * @parameter expression="${project.build.directory}/site/xref"
     */
    private String destDir;

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
     * Folder where Javadoc is generated for this project.
     *
     * @parameter expression="${project.build.directory}/site/apidocs"
     */
    private String javadocDir;

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
     * @parameter expression="Copyright &copy; ${project.inceptionYear} ${project.organization.name}. All Rights Reserved."
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

    /*
     * Tells whether Javadoc is part of the reports being generated during the build
     * TODO: not used as for now, should think about that
     */
    private boolean javadocReportGenerated;

    /**
     * The projects in the reactor for aggregation report.
     *
     * @parameter expression="${reactorProjects}"
     * @readonly
     */
    private List reactorProjects;

    /**
     * Whether to build an aggregated report at the root, or build individual reports.
     *
     * @parameter expression="${aggregate}" default-value="false"
     */
    private boolean aggregate;

    /**
     * Cf. overriden method documentation.
     *
     * @see org.apache.maven.reporting.AbstractMavenReport#executeReport(java.util.Locale)
     */
    protected void executeReport( Locale locale )
        throws MavenReportException
    {
        List testSourceDirs = constructTestSourceDirs();
        List sourceDirs = constructSourceDirs();
        if ( canGenerateReport( sourceDirs, testSourceDirs ) )
        {
            // init some attributes
            init();

            // and start the report
            Sink sink = getSink();

            startSink( sink, locale );

            try
            {
                // check if there are sources in the sourceDir and generate Xref
                generateXrefForSources( locale, sink, sourceDirs );

                // check if there are test sources in the testSourceDir and generate Xref
                generateXrefForTests( locale, sink, testSourceDirs );
            }
            catch ( JxrException e )
            {
                throw new MavenReportException( "Error while generating the HTML source code of the projet.", e );
            }
            catch ( IOException e )
            {
                throw new MavenReportException( "Error while generating the HTML source code of the projet.", e );
            }

            endSink( sink );
        }
    }

    /*
     * Generates the Xref for the application sources if they exist
     */
    private void generateXrefForSources( Locale locale, Sink sink, List sourceDirs )
        throws JxrException, IOException
    {
        sink.section2();
        sink.sectionTitle2();
        sink.text( getBundle( locale ).getString( "report.xref.projectSources.title" ) );
        sink.sectionTitle2_();
        sink.paragraph();

        if ( !sourceDirs.isEmpty() )
        {
            // create the XRef for the source dir
            createXref( locale, destDir, sourceDirs );

            // put the link to the sources
            sink.text( getBundle( locale ).getString( "report.xref.projectSources.link" ) );
            File out = new File( outputDirectory );
            File des = new File( destDir );
            String relativPath = des.getAbsolutePath().substring( out.getAbsolutePath().length() + 1 );
            sink.link( relativPath + "/index.html" );
            sink.text( relativPath + "/index.html" );
            sink.link_();
        }
        else
        {
            sink.text( getBundle( locale ).getString( "report.xref.projectSources.noSources" ) );
        }
        sink.paragraph_();
        sink.section2_();
    }

    private List constructSourceDirs()
    {
        List sourceDirs = new ArrayList( this.sourceDirs );
        if ( aggregate )
        {
            for ( Iterator i = reactorProjects.iterator(); i.hasNext(); )
            {
                MavenProject project = (MavenProject) i.next();

                sourceDirs.addAll( project.getCompileSourceRoots() );
            }
        }

        sourceDirs = pruneSourceDirs( sourceDirs );
        return sourceDirs;
    }

    private List pruneSourceDirs( List sourceDirs )
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

    /*
     * Generates the Xref for the test sources if they exist
     */
    private void generateXrefForTests( Locale locale, Sink sink, List testSourceDirs )
        throws JxrException, IOException
    {
        sink.section2();
        sink.sectionTitle2();
        sink.text( getBundle( locale ).getString( "report.xref.testSources.title" ) );
        sink.sectionTitle2_();
        sink.paragraph();

        if ( !testSourceDirs.isEmpty() )
        {
            String testDestDir = destDir + "-test";

            // create the XRef for the source dir
            createXref( locale, testDestDir, testSourceDirs );

            // put the link to the sources
            sink.text( getBundle( locale ).getString( "report.xref.testSources.link" ) );
            File out = new File( outputDirectory );
            File des = new File( testDestDir );
            String relativPath = des.getAbsolutePath().substring( out.getAbsolutePath().length() + 1 );
            sink.link( relativPath + "/index.html" );
            sink.text( relativPath + "/index.html" );
            sink.link_();
        }
        else
        {
            sink.text( getBundle( locale ).getString( "report.xref.testSources.noSources" ) );
        }
        sink.paragraph_();
        sink.section2_();
    }

    private List constructTestSourceDirs()
    {
        List testSourceDirs = new ArrayList( this.testSourceDirs );
        if ( aggregate )
        {
            for ( Iterator i = reactorProjects.iterator(); i.hasNext(); )
            {
                MavenProject project = (MavenProject) i.next();

                List sourceRoots = project.getTestCompileSourceRoots();
                testSourceDirs.addAll( sourceRoots );
            }
        }

        testSourceDirs = pruneSourceDirs( testSourceDirs );
        return testSourceDirs;
    }

    /*
     * Initialize some attributes required during the report generation
     */
    private void init()
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
                    javadocReportGenerated = true;
                    break;
                }
            }
        }
    }

    /**
     * Writes the beginning of the sink.
     *
     * @param sink
     * @param locale
     */
    private void startSink( Sink sink, Locale locale )
    {
        sink.head();
        sink.title();
        sink.text( getBundle( locale ).getString( "report.xref.header" ) );
        sink.title_();
        sink.head_();

        sink.body();
        sink.section1();

        sink.sectionTitle1();
        sink.text( getBundle( locale ).getString( "report.xref.mainTitle" ) );
        sink.sectionTitle1_();

        sink.paragraph();
        sink.text( getBundle( locale ).getString( "report.xref.summary" ) );
        sink.paragraph_();
    }

    /**
     * Writes the end of the sink.
     *
     * @param sink
     */
    private void endSink( Sink sink )
    {
        sink.section1_();
        sink.body_();
        sink.flush();
        sink.close();
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
     * @throws IOException
     * @throws JxrException
     */
    private void createXref( Locale locale, String destinationDirectory, List sourceDirs )
        throws IOException, JxrException
    {
        JXR jxr = new JXR();
        jxr.setDest( destinationDirectory );
        jxr.setInputEncoding( inputEncoding );
        jxr.setJavadocLinkDir( javadocDir );
        jxr.setLocale( locale );
        jxr.setLog( new PluginLogAdapter( getLog() ) );
        jxr.setOutputEncoding( outputEncoding );
        jxr.setRevision( "HEAD" );

        jxr.xref( sourceDirs, templateDir, windowTitle, docTitle, bottom );

        // and finally copy the stylesheet
        copyRequiredResources( destinationDirectory );
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
    protected SiteRenderer getSiteRenderer()
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
     * Cf. overriden method documentation.
     *
     * @see org.apache.maven.reporting.MavenReport#getDescription(java.util.Locale)
     */
    public String getDescription( Locale locale )
    {
        return getBundle( locale ).getString( "report.xref.description" );
    }

    /**
     * Cf. overriden method documentation.
     *
     * @see org.apache.maven.reporting.MavenReport#getName(java.util.Locale)
     */
    public String getName( Locale locale )
    {
        return getBundle( locale ).getString( "report.xref.name" );
    }

    /**
     * Cf. overriden method documentation.
     *
     * @see org.apache.maven.reporting.MavenReport#getOutputName()
     */
    public String getOutputName()
    {
        return "jxr";
    }

    /**
     * Returns the correct resource bundle according to the locale
     *
     * @param locale :
     *               the locale of the user
     * @return the bundle correponding to the locale
     */
    private ResourceBundle getBundle( Locale locale )
    {
        return ResourceBundle.getBundle( "jxr-report", locale, this.getClass().getClassLoader() );
    }

    public boolean canGenerateReport()
    {
        return canGenerateReport( constructSourceDirs(), constructTestSourceDirs() );
    }

    private boolean canGenerateReport( List sourceDirs, List testSourceDirs )
    {
        boolean canGenerate = !pruneSourceDirs( sourceDirs ).isEmpty() || !pruneSourceDirs( testSourceDirs ).isEmpty();

        if ( aggregate && !project.isExecutionRoot() )
        {
            canGenerate = false;
        }
        return canGenerate;
    }
}
