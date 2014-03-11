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
import net.sourceforge.pmd.PMDConfiguration;
import net.sourceforge.pmd.Report;
import net.sourceforge.pmd.RuleContext;
import net.sourceforge.pmd.RulePriority;
import net.sourceforge.pmd.RuleSetFactory;
import net.sourceforge.pmd.RuleSetReferenceId;
import net.sourceforge.pmd.benchmark.Benchmarker;
import net.sourceforge.pmd.benchmark.TextReport;
import net.sourceforge.pmd.lang.LanguageVersion;
import net.sourceforge.pmd.renderers.CSVRenderer;
import net.sourceforge.pmd.renderers.HTMLRenderer;
import net.sourceforge.pmd.renderers.Renderer;
import net.sourceforge.pmd.renderers.TextRenderer;
import net.sourceforge.pmd.renderers.XMLRenderer;
import net.sourceforge.pmd.util.datasource.DataSource;
import net.sourceforge.pmd.util.datasource.FileDataSource;
import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.plexus.resource.ResourceManager;
import org.codehaus.plexus.resource.loader.FileResourceCreationException;
import org.codehaus.plexus.resource.loader.FileResourceLoader;
import org.codehaus.plexus.resource.loader.ResourceNotFoundException;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;

/**
 * Creates a PMD report.
 *
 * @author Brett Porter
 * @version $Id$
 * @since 2.0
 */
@Mojo( name = "pmd", threadSafe = true, requiresDependencyResolution = ResolutionScope.TEST )
public class PmdReport
    extends AbstractPmdReport
{
    /**
     * The target JDK to analyze based on. Should match the target used in the compiler plugin. Valid values are
     * currently <code>1.3</code>, <code>1.4</code>, <code>1.5</code>, <code>1.6</code> and <code>1.7</code>.
     * <p/>
     * <b>Note:</b> support for <code>1.6</code> was added in version 2.3 of this plugin,
     * support for <code>1.7</code> was added in version 2.7 of this plugin.
     */
    @Parameter( property = "targetJdk" )
    private String targetJdk;

    /**
     * The programming language to be analyzed by PMD. Valid values are currently <code>java</code>
     * and <code>ecmascript</code> or <code>javascript</code>.
     * <p>
     * <b>Note:</b> if the parameter targetJdk is given, then this language parameter will be ignored.
     * </p>
     *
     * @since 3.0
     */
    @Parameter( defaultValue = "java" )
    private String language;

    /**
     * The rule priority threshold; rules with lower priority
     * than this will not be evaluated.
     *
     * @since 2.1
     */
    @Parameter( property = "minimumPriority", defaultValue = "5" )
    private int minimumPriority = 5;

    /**
     * Skip the PMD report generation.  Most useful on the command line
     * via "-Dpmd.skip=true".
     *
     * @since 2.1
     */
    @Parameter( property = "pmd.skip", defaultValue = "false" )
    private boolean skip;

    /**
     * The PMD rulesets to use. See the <a href="http://pmd.sourceforge.net/rules/index.html">Stock Rulesets</a> for a
     * list of some included. Since version 2.5, the ruleset "rulesets/maven.xml" is also available. Defaults to the
     * java-basic, java-imports and java-unusedcode rulesets.
     */
    @Parameter
    private String[] rulesets = new String[]{ "java-basic", "java-unusedcode", "java-imports" };

    /**
     * Controls whether the project's compile/test classpath should be passed to PMD to enable its type resolution
     * feature.
     *
     * @since 3.0
     */
    @Parameter( property = "pmd.typeResolution", defaultValue = "false" )
    private boolean typeResolution;

    /**
     * Controls whether PMD will track benchmark information.
     *
     * @since 3.1
     */
    @Parameter( property = "pmd.benchmark", defaultValue = "false" )
    private boolean benchmark;

    /**
     * Benchmark output filename.
     *
     * @since 3.1
     */
    @Parameter( property = "pmd.benchmarkOutputFilename", defaultValue = "${project.build.directory}/pmd-benchmark.txt" )
    private String benchmarkOutputFilename;

    /**
     */
    @Component
    private ResourceManager locator;

    /** The PMD report listener for collecting violations. */
    private PmdReportListener reportListener;

    /**
     * per default pmd executions error are ignored to not break the whole
     * @since 3.1
     */
    @Parameter( property = "pmd.skipPmdError", defaultValue = "true" )
    private boolean skipPmdError;

    /**
     * {@inheritDoc}
     */
    public String getName( Locale locale )
    {
        return getBundle( locale ).getString( "report.pmd.name" );
    }

    /**
     * {@inheritDoc}
     */
    public String getDescription( Locale locale )
    {
        return getBundle( locale ).getString( "report.pmd.description" );
    }

    public void setRulesets( String[] rules )
    {
        rulesets = rules;
    }

    /**
     * {@inheritDoc}
     */
    public void executeReport( Locale locale )
        throws MavenReportException
    {
        try
        {
            execute( locale );
        }
        finally
        {
            if ( getSink() != null )
            {
                getSink().close();
            }
        }
    }

    private void execute( Locale locale )
        throws MavenReportException
    {
        if ( !skip && canGenerateReport() )
        {
            ClassLoader origLoader = Thread.currentThread().getContextClassLoader();
            try
            {
                Thread.currentThread().setContextClassLoader( this.getClass().getClassLoader() );

                Report report = generateReport( locale );

                if ( !isHtml() && !isXml() )
                {
                    writeNonHtml( report );
                }
            }
            finally
            {
                Thread.currentThread().setContextClassLoader( origLoader );
            }
        }
    }

    public boolean canGenerateReport()
    {
        boolean result = super.canGenerateReport();
        if ( result )
        {
            try
            {
                executePmdWithClassloader();
                if ( skipEmptyReport )
                {
                    result = reportListener.hasViolations();
                    if ( result )
                    {
                        getLog().debug( "Skipping Report as skipEmptyReport is true and there are no PMD violations." );
                    }
                }
            }
            catch ( MavenReportException e )
            {
                throw new RuntimeException( e );
            }
        }
        return result;
    }

    private void executePmdWithClassloader()
        throws MavenReportException
    {
        ClassLoader origLoader = Thread.currentThread().getContextClassLoader();
        try
        {
            Thread.currentThread().setContextClassLoader( this.getClass().getClassLoader() );
            executePmd();
        }
        finally
        {
            Thread.currentThread().setContextClassLoader( origLoader );
        }
    }

    private void executePmd()
        throws MavenReportException
    {
        if ( reportListener != null )
        {
            // PMD has already been run
            getLog().debug( "PMD has already been run - skipping redundant execution." );
            return;
        }

        //configure ResourceManager
        locator.addSearchPath( FileResourceLoader.ID, project.getFile().getParentFile().getAbsolutePath() );
        locator.addSearchPath( "url", "" );
        locator.setOutputDirectory( targetDirectory );

        reportListener = new PmdReportListener();
        PMDConfiguration pmdConfiguration = getPMDConfiguration();
        RuleContext ruleContext = new RuleContext();
        ruleContext.getReport().addListener( reportListener );

        RuleSetFactory ruleSetFactory = new RuleSetFactory();
        ruleSetFactory.setMinimumPriority( RulePriority.valueOf( this.minimumPriority ) );

        // Workaround for https://sourceforge.net/p/pmd/bugs/1155/: add a dummy ruleset.
        String[] presentRulesets = rulesets.length > 0 ? rulesets : new String [] { "/rulesets/dummy.xml" };

        String[] sets = new String[presentRulesets.length];
        try
        {
            for ( int idx = 0; idx < presentRulesets.length; idx++ )
            {
                String set = presentRulesets[idx];
                getLog().debug( "Preparing ruleset: " + set );
                RuleSetReferenceId id = new RuleSetReferenceId( set );
                File ruleset = locator.getResourceAsFile( id.getRuleSetFileName(), getLocationTemp( set ) );
                if ( null == ruleset )
                {
                    throw new MavenReportException( "Could not resolve " + set );
                }
                sets[idx] = ruleset.getAbsolutePath();
            }
        }
        catch ( ResourceNotFoundException e )
        {
            throw new MavenReportException( e.getMessage(), e );
        }
        catch ( FileResourceCreationException e )
        {
            throw new MavenReportException( e.getMessage(), e );
        }
        pmdConfiguration.setRuleSets( StringUtils.join( sets, "," ) );

        try
        {
            if ( filesToProcess == null )
            {
                filesToProcess = getFilesToProcess();
            }

            if ( filesToProcess.isEmpty() && !"java".equals( language ) )
            {
                getLog().warn(
                        "No files found to process. Did you add your additional source folders like javascript? (see also build-helper-maven-plugin)" );
            }
        }
        catch ( IOException e )
        {
            throw new MavenReportException( "Can't get file list", e );
        }

        String encoding = getSourceEncoding();
        if ( StringUtils.isEmpty( encoding ) && !filesToProcess.isEmpty() )
        {
            getLog().warn( "File encoding has not been set, using platform encoding " + ReaderFactory.FILE_ENCODING
                               + ", i.e. build is platform dependent!" );
            encoding = ReaderFactory.FILE_ENCODING;
        }
        pmdConfiguration.setSourceEncoding( encoding );

        List<DataSource> dataSources = new ArrayList<DataSource>( filesToProcess.size() );
        for ( File f : filesToProcess.keySet() )
        {
            dataSources.add( new FileDataSource( f ) );
        }

        try
        {
            getLog().debug( "Executing PMD..." );

            PMD.processFiles( pmdConfiguration, ruleSetFactory, dataSources, ruleContext,
                              Collections.<Renderer> emptyList() );

            if ( getLog().isDebugEnabled() )
            {
                getLog().debug( "PMD finished. Found " + reportListener.getViolations().size() + " violations." );
            }
        }
        catch ( Exception e )
        {
            String message = "Failure executing PMD: " + e.getLocalizedMessage();
            if ( !skipPmdError )
            {
                throw new MavenReportException( message, e );
            }
            getLog().warn( message, e );
        }

        // if format is XML, we need to output it even if the file list is empty or we have no violations
        // so the "check" goals can check for violations
        if ( isXml() && reportListener != null )
        {
            writeNonHtml( reportListener.asReport() );
        }

        if ( benchmark )
        {
            PrintStream benchmarkFileStream = null;
            try
            {
                benchmarkFileStream = new PrintStream( benchmarkOutputFilename );
                ( new TextReport() ).generate( Benchmarker.values(), benchmarkFileStream );
            }
            catch ( FileNotFoundException fnfe )
            {
                getLog().error( "Unable to generate benchmark file: " + benchmarkOutputFilename, fnfe );
            }
            finally
            {
                if ( null != benchmarkFileStream )
                {
                    benchmarkFileStream.close();
                }
            }
        }
    }

    private Report generateReport( Locale locale )
        throws MavenReportException
    {
        Sink sink = getSink();
        PmdReportGenerator renderer = new PmdReportGenerator( getLog(), sink, getBundle( locale ), aggregate );
        renderer.setFiles( filesToProcess );
        renderer.setViolations( reportListener.getViolations() );

        try
        {
            renderer.beginDocument();
            renderer.render();
            renderer.endDocument();
        }
        catch ( IOException e )
        {
            getLog().warn( "Failure creating the report: " + e.getLocalizedMessage(), e );
        }

        return reportListener.asReport();
    }

    /**
     * Convenience method to get the location of the specified file name.
     *
     * @param name the name of the file whose location is to be resolved
     * @return a String that contains the absolute file name of the file
     */
    protected String getLocationTemp( String name )
    {
        String loc = name;
        if ( loc.indexOf( '/' ) != -1 )
        {
            loc = loc.substring( loc.lastIndexOf( '/' ) + 1 );
        }
        if ( loc.indexOf( '\\' ) != -1 )
        {
            loc = loc.substring( loc.lastIndexOf( '\\' ) + 1 );
        }

        // MPMD-127 in the case that the rules are defined externally on a url
        // we need to replace some special url characters that cannot be
        // used in filenames on disk or produce ackward filenames.
        // replace all occurrences of the following characters:  ? : & = %
        loc = loc.replaceAll( "[\\?\\:\\&\\=\\%]", "_" );

        if ( !loc.endsWith( ".xml" ) )
        {
            loc = loc + ".xml";
        }

        getLog().debug( "Before: " + name + " After: " + loc );
        return loc;
    }

    /**
     * Use the PMD renderers to render in any format aside from HTML.
     *
     * @param report
     * @throws MavenReportException
     */
    private void writeNonHtml( Report report )
        throws MavenReportException
    {
        Renderer r = createRenderer();

        if ( r == null )
        {
            return;
        }

        Writer writer = null;
        FileOutputStream tStream = null;
        try
        {
            targetDirectory.mkdirs();
            File targetFile = new File( targetDirectory, "pmd." + format );
            tStream = new FileOutputStream( targetFile );
            writer = new OutputStreamWriter( tStream, getOutputEncoding() );

            r.setWriter( writer );
            r.start();
            r.renderFileReport( report );
            r.end();
            writer.close();

            if ( includeXmlInSite )
            {
                File siteDir = getReportOutputDirectory();
                siteDir.mkdirs();
                FileUtils.copyFile( targetFile, new File( siteDir, "pmd." + format ) );
            }
        }
        catch ( IOException ioe )
        {
            throw new MavenReportException( ioe.getMessage(), ioe );
        }
        finally
        {
            IOUtil.close( writer );
            IOUtil.close( tStream );
        }
    }

    /**
     * Constructs the PMD configuration class, passing it an argument
     * that configures the target JDK.
     *
     * @return the resulting PMD
     * @throws org.apache.maven.reporting.MavenReportException
     *          if targetJdk is not supported
     */
    public PMDConfiguration getPMDConfiguration()
        throws MavenReportException
    {
        PMDConfiguration configuration = new PMDConfiguration();
        LanguageVersion languageVersion = null;

        if ( null != targetJdk )
        {
            languageVersion = LanguageVersion.findByTerseName( "java " + targetJdk );
            if ( languageVersion == null )
            {
                throw new MavenReportException( "Unsupported targetJdk value '" + targetJdk + "'." );
            }
        }
        else if ( "javascript".equals( language ) || "ecmascript".equals( language ) )
        {
            languageVersion = LanguageVersion.ECMASCRIPT;
        }
        if ( languageVersion != null )
        {
            getLog().debug( "Using language " + languageVersion );
            configuration.setDefaultLanguageVersion( languageVersion );
        }

        if ( typeResolution )
        {
            try
            {
                @SuppressWarnings( "unchecked" )
                List<String> classpath =
                    includeTests ? project.getTestClasspathElements() : project.getCompileClasspathElements();
                getLog().debug( "Using aux classpath: " + classpath );
                configuration.prependClasspath( StringUtils.join( classpath.iterator(), File.pathSeparator ) );
            }
            catch ( Exception e )
            {
                throw new MavenReportException( e.getMessage(), e );
            }
        }

        configuration.setBenchmark( benchmark );

        return configuration;
    }

    /**
     * {@inheritDoc}
     */
    public String getOutputName()
    {
        return "pmd";
    }

    private static ResourceBundle getBundle( Locale locale )
    {
        return ResourceBundle.getBundle( "pmd-report", locale, PmdReport.class.getClassLoader() );
    }

    /**
     * Create and return the correct renderer for the output type.
     *
     * @return the renderer based on the configured output
     * @throws org.apache.maven.reporting.MavenReportException
     *          if no renderer found for the output type
     */
    public final Renderer createRenderer()
        throws MavenReportException
    {
        Renderer renderer = null;
        if ( "xml".equals( format ) )
        {
            renderer = new XMLRenderer( getOutputEncoding() );
        }
        else if ( "txt".equals( format ) )
        {
            renderer = new TextRenderer( );
        }
        else if ( "csv".equals( format ) )
        {
            renderer = new CSVRenderer( );
        }
        else if ( "html".equals( format ) )
        {
            renderer = new HTMLRenderer( );
        }
        else if ( !"".equals( format ) && !"none".equals( format ) )
        {
            try
            {
                renderer = (Renderer) Class.forName( format ).getConstructor( Properties.class ).newInstance(
                    new Properties() );
            }
            catch ( Exception e )
            {
                throw new MavenReportException(
                    "Can't find PMD custom format " + format + ": " + e.getClass().getName(), e );
            }
        }

        return renderer;
    }

}
