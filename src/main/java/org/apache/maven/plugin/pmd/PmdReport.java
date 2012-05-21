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
import net.sourceforge.pmd.RuleViolation;
import net.sourceforge.pmd.lang.LanguageVersion;
import net.sourceforge.pmd.renderers.CSVRenderer;
import net.sourceforge.pmd.renderers.HTMLRenderer;
import net.sourceforge.pmd.renderers.Renderer;
import net.sourceforge.pmd.renderers.TextRenderer;
import net.sourceforge.pmd.renderers.XMLRenderer;
import net.sourceforge.pmd.util.datasource.DataSource;
import net.sourceforge.pmd.util.datasource.FileDataSource;
import org.apache.maven.doxia.sink.Sink;
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;

/**
 * Creates a PMD report.
 *
 * @author Brett Porter
 * @version $Id$
 * @goal pmd
 * @threadSafe
 * @since 2.0
 */
public class PmdReport
    extends AbstractPmdReport
{
    /**
     * The target JDK to analyze based on. Should match the target used in the compiler plugin. Valid values are
     * currently <code>1.3</code>, <code>1.4</code>, <code>1.5</code>, <code>1.6</code> and <code>1.7</code>.
     * <p>
     * <b>Note:</b> support for <code>1.6</code> was added in version 2.3 of this plugin,
     * support for <code>1.7</code> was added in version 2.7 of this plugin.
     * </p>
     *
     * @parameter expression="${targetJdk}"
     */
    private String targetJdk;

    /**
     * The programming language to be analyzed by PMD. Valid values are currently <code>java</code>
     * and <code>ecmascript</code> or <code>javascript</code>.
     * <p>
     * <b>Note:</b> if the parameter targetJdk is given, then this language parameter will be ignored.
     * </p>
     *
     * @parameter default-value="java"
     */
    private String language;

    /**
     * The rule priority threshold; rules with lower priority
     * than this will not be evaluated.
     *
     * @parameter expression="${minimumPriority}" default-value="5"
     * @since 2.1
     */
    private int minimumPriority = 5;

    /**
     * Skip the PMD report generation.  Most useful on the command line
     * via "-Dpmd.skip=true".
     *
     * @parameter expression="${pmd.skip}" default-value="false"
     * @since 2.1
     */
    private boolean skip;

    /**
     * The PMD rulesets to use. See the <a href="http://pmd.sourceforge.net/rules/index.html">Stock Rulesets</a> for a
     * list of some included. Since version 2.5, the ruleset "rulesets/maven.xml" is also available. Defaults to the
     * java-basic, java-imports and java-unusedcode rulesets.
     *
     * @parameter
     */
    private String[] rulesets = new String[]{ "java-basic", "java-unusedcode", "java-imports" };

    /**
     * @component
     * @required
     * @readonly
     */
    private ResourceManager locator;

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
        //configure ResourceManager
        locator.addSearchPath( FileResourceLoader.ID, project.getFile().getParentFile().getAbsolutePath() );
        locator.addSearchPath( "url", "" );
        locator.setOutputDirectory( targetDirectory );

        if ( !skip && canGenerateReport() )
        {
            ClassLoader origLoader = Thread.currentThread().getContextClassLoader();
            try
            {
                Thread.currentThread().setContextClassLoader( this.getClass().getClassLoader() );

                Report report = generateReport( locale );

                if ( !isHtml() )
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

    private Report generateReport( Locale locale )
        throws MavenReportException
    {
        Sink sink = getSink();

        PMDConfiguration pmdConfiguration = getPMDConfiguration();
        final PmdReportListener reportSink = new PmdReportListener( sink, getBundle( locale ), aggregate );
        RuleContext ruleContext = new RuleContext()
        {
            @Override
            public void setReport( Report report )
            {
                super.setReport( report );
                // make sure our listener is added - the Report is created by PMD internally now
                report.addListener( reportSink );
            }
        };
        reportSink.beginDocument();

        RuleSetFactory ruleSetFactory = new RuleSetFactory();
        ruleSetFactory.setMinimumPriority( RulePriority.valueOf( this.minimumPriority ) );
        String[] sets = new String[rulesets.length];
        try
        {
            for ( int idx = 0; idx < rulesets.length; idx++ )
            {
                String set = rulesets[idx];
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

        Map<File, PmdFileInfo> files;
        try
        {
            files = getFilesToProcess();
            if ( files.isEmpty() && !"java".equals( language ) )
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
        if ( StringUtils.isEmpty( encoding ) && !files.isEmpty() )
        {
            getLog().warn( "File encoding has not been set, using platform encoding " + ReaderFactory.FILE_ENCODING
                               + ", i.e. build is platform dependent!" );
            encoding = ReaderFactory.FILE_ENCODING;
        }
        pmdConfiguration.setSourceEncoding( encoding );

        reportSink.setFiles( files );
        List<DataSource> dataSources = new ArrayList<DataSource>( files.size() );
        for ( File f : files.keySet() )
        {
            dataSources.add( new FileDataSource( f ) );
        }

        try
        {
            List<Renderer> renderers = Collections.emptyList();

            // Unfortunately we need to disable multi-threading for now - as otherwise our PmdReportListener
            // will be ignored.
            // Longer term solution could be to use a custom renderer instead. And collect with this renderer
            // all the violations.
            pmdConfiguration.setThreads( 0 );

            PMD.processFiles( pmdConfiguration, ruleSetFactory, dataSources, ruleContext, renderers );
        }
        catch ( Exception e )
        {
            getLog().warn( "Failure executing PMD: " + e.getLocalizedMessage(), e );
        }

        reportSink.endDocument();

        // copy over the violations into a single report - PMD now creates one report per file
        Report report = new Report();
        for ( RuleViolation v : reportSink.getViolations() )
        {
            report.addRuleViolation( v );
        }
        return report;
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

            File siteDir = getReportOutputDirectory();
            siteDir.mkdirs();
            FileUtils.copyFile( targetFile, new File( siteDir, "pmd." + format ) );
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
            renderer = new PmdXMLRenderer( getOutputEncoding() );
        }
        else if ( "txt".equals( format ) )
        {
            renderer = new TextRenderer( new Properties() );
        }
        else if ( "csv".equals( format ) )
        {
            renderer = new CSVRenderer( new Properties() );
        }
        else if ( "html".equals( format ) )
        {
            renderer = new HTMLRenderer( new Properties() );
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

    private static class PmdXMLRenderer
        extends XMLRenderer
    {
        public PmdXMLRenderer( String encoding )
        {
            super( new Properties() );
            this.encoding = encoding;
        }
    }
}