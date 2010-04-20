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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import net.sourceforge.pmd.IRuleViolation;
import net.sourceforge.pmd.PMD;
import net.sourceforge.pmd.PMDException;
import net.sourceforge.pmd.Report;
import net.sourceforge.pmd.Rule;
import net.sourceforge.pmd.RuleContext;
import net.sourceforge.pmd.RuleSet;
import net.sourceforge.pmd.RuleSetFactory;
import net.sourceforge.pmd.SourceType;
import net.sourceforge.pmd.renderers.CSVRenderer;
import net.sourceforge.pmd.renderers.HTMLRenderer;
import net.sourceforge.pmd.renderers.Renderer;
import net.sourceforge.pmd.renderers.TextRenderer;
import net.sourceforge.pmd.renderers.XMLRenderer;

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

/**
 * Creates a PMD report.
 *
 * @author Brett Porter
 * @version $Id$
 * @since 2.0
 * @goal pmd
 */
public class PmdReport
    extends AbstractPmdReport
{
    /**
     * The target JDK to analyze based on. Should match the target used in the compiler plugin. Valid values are
     * currently <code>1.3</code>, <code>1.4</code>, <code>1.5</code> and <code>1.6</code>.
     * <p>
     * <b>Note:</b> support for <code>1.6</code> was added in version 2.3 of this plugin.
     * </p>
     *
     * @parameter expression="${targetJdk}"
     */
    private String targetJdk;

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
     * basic, imports and unusedcode rulesets.
     *
     * @parameter
     */
    private String[] rulesets = new String[]{"rulesets/basic.xml", "rulesets/unusedcode.xml", "rulesets/imports.xml", };

    /**
     * @component
     * @required
     * @readonly
     */
    private ResourceManager locator;

    /** {@inheritDoc} */
    public String getName( Locale locale )
    {
        return getBundle( locale ).getString( "report.pmd.name" );
    }

    /** {@inheritDoc} */
    public String getDescription( Locale locale )
    {
        return getBundle( locale ).getString( "report.pmd.description" );
    }

    public void setRulesets( String[] rules )
    {
        rulesets = rules;
    }

    /** {@inheritDoc} */
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
        locator.setOutputDirectory( new File( project.getBuild().getDirectory() ) );

        if ( !skip && canGenerateReport() )
        {
            ClassLoader origLoader = Thread.currentThread().getContextClassLoader();
            try
            {
                Thread.currentThread().setContextClassLoader( this.getClass().getClassLoader() );

                Report report = generateReport( locale );

                if ( !isHtml() )
                {
                    renderPmdFormat( report );
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

        PMD pmd = getPMD();
        RuleContext ruleContext = new RuleContext();
        Report report = new Report();
        PmdReportListener reportSink = new PmdReportListener( sink, getBundle( locale ), aggregate );

        report.addListener( reportSink );
        ruleContext.setReport( report );
        reportSink.beginDocument();

        RuleSetFactory ruleSetFactory = new RuleSetFactory();
        ruleSetFactory.setMinimumPriority( this.minimumPriority );
        RuleSet[] sets = new RuleSet[rulesets.length];
        try
        {
            for ( int idx = 0; idx < rulesets.length; idx++ )
            {
                String set = rulesets[idx];
                getLog().debug( "Preparing ruleset: " + set );
                File ruleset = locator.getResourceAsFile( set, getLocationTemp( set ) );

                if ( null == ruleset )
                {
                    throw new MavenReportException( "Could not resolve " + set );
                }

                InputStream rulesInput = new FileInputStream( ruleset );
                try
                {
                    sets[idx] = ruleSetFactory.createRuleSet( rulesInput );
                }
                finally
                {
                    rulesInput.close();
                }
            }
        }
        catch ( IOException e )
        {
            throw new MavenReportException( e.getMessage(), e );
        }
        catch ( ResourceNotFoundException e )
        {
            throw new MavenReportException( e.getMessage(), e );
        }
        catch ( FileResourceCreationException e )
        {
            throw new MavenReportException( e.getMessage(), e );
        }

        Map files;
        try
        {
            files = getFilesToProcess( );
        }
        catch ( IOException e )
        {
            throw new MavenReportException( "Can't get file list", e );
        }

        if ( StringUtils.isEmpty( getSourceEncoding() ) && !files.isEmpty() )
        {
            getLog().warn( "File encoding has not been set, using platform encoding "
                               + ReaderFactory.FILE_ENCODING + ", i.e. build is platform dependent!" );
        }

        for ( Iterator i = files.entrySet().iterator(); i.hasNext(); )
        {
            Map.Entry entry = (Map.Entry) i.next();
            File file = (File) entry.getKey();
            PmdFileInfo fileInfo = (PmdFileInfo) entry.getValue();

            // TODO: lazily call beginFile in case there are no rules

            reportSink.beginFile( file , fileInfo );
            ruleContext.setSourceCodeFilename( file.getAbsolutePath() );
            for ( int idx = 0; idx < rulesets.length; idx++ )
            {
                try
                {
                    // PMD closes this Reader even though it did not open it so we have
                    // to open a new one with every call to processFile().
                    Reader reader;
                    if ( StringUtils.isNotEmpty( getSourceEncoding() ) )
                    {
                        reader = ReaderFactory.newReader( file, getSourceEncoding() );
                    }
                    else
                    {
                        reader = ReaderFactory.newPlatformReader( file );
                    }

                    try
                    {
                        pmd.processFile( reader, sets[idx], ruleContext );
                    }
                    finally
                    {
                        reader.close();
                    }
                }
                catch ( UnsupportedEncodingException e1 )
                {
                    throw new MavenReportException( "Encoding '" + getSourceEncoding() + "' is not supported.", e1 );
                }
                catch ( PMDException pe )
                {
                    String msg = pe.getLocalizedMessage();
                    Throwable r = pe.getCause();
                    if ( r != null )
                    {
                        msg = msg + ": " + r.getLocalizedMessage();
                    }
                    getLog().warn( msg );
                    reportSink.ruleViolationAdded( new ProcessingErrorRuleViolation( file, msg ) );
                }
                catch ( FileNotFoundException e2 )
                {
                    getLog().warn( "Error opening source file: " + file );
                    reportSink.ruleViolationAdded(
                        new ProcessingErrorRuleViolation( file, e2.getLocalizedMessage() ) );
                }
                catch ( Exception e3 )
                {
                    getLog().warn( "Failure executing PMD for: " + file, e3 );
                    reportSink.ruleViolationAdded(
                        new ProcessingErrorRuleViolation( file, e3.getLocalizedMessage() ) );
                }
            }
            reportSink.endFile( file );
        }

        reportSink.endDocument();

        return report;
    }

    /**
     * Use the PMD renderers to render in any format aside from HTML.
     *
     * @param report
     * @throws MavenReportException
     */
    private void renderPmdFormat( Report report )
        throws MavenReportException
    {
        Renderer r = createRenderer();

        if ( r == null )
        {
            return;
        }

        Writer writer = null;

        try
        {
            File targetFile = new File( targetDirectory, "pmd." + format );
            FileOutputStream tStream = new FileOutputStream( targetFile );
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
        }
    }

    /**
     * Convenience method to get the location of the specified file name.
     *
     * @param name the name of the file whose location is to be resolved
     * @return a String that contains the absolute file name of the file
     */
    private String getLocationTemp( String name )
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
        getLog().debug( "Before: " + name + " After: " + loc );
        return loc;
    }

    /**
     * Constructs the PMD class, passing it an argument
     * that configures the target JDK.
     *
     * @return the resulting PMD
     * @throws org.apache.maven.reporting.MavenReportException
     *          if targetJdk is not supported
     */
    public PMD getPMD()
        throws MavenReportException
    {
        PMD pmd = new PMD();

        if ( null != targetJdk )
        {
            SourceType sourceType = SourceType.getSourceTypeForId( "java " + targetJdk );
            if ( sourceType == null )
            {
                throw new MavenReportException( "Unsupported targetJdk value '" + targetJdk + "'." );
            }
            pmd.setJavaVersion( sourceType );
        }

        return pmd;
    }

    /** {@inheritDoc} */
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
            renderer = new TextRenderer();
        }
        else if ( "csv".equals( format ) )
        {
            renderer = new CSVRenderer();
        }
        else if ( "html".equals( format ) )
        {
            renderer = new HTMLRenderer();
        }
        else if ( !"".equals( format ) && !"none".equals( format ) )
        {
            try
            {
                renderer = (Renderer) Class.forName( format ).newInstance();
            }
            catch ( Exception e )
            {
                throw new MavenReportException(
                    "Can't find the custom format " + format + ": " + e.getClass().getName(), e );
            }
        }

        return renderer;
    }

    private static class PmdXMLRenderer extends XMLRenderer
    {
        public PmdXMLRenderer( String encoding )
        {
            super();
            this.encoding = encoding;
        }
    }

    /** @author <a href="mailto:douglass.doug@gmail.com">Doug Douglass</a> */
    private static class ProcessingErrorRuleViolation
        implements IRuleViolation
    {

        private String filename;

        private String description;

        public ProcessingErrorRuleViolation( File file,
                                             String description )
        {
            filename = file.getPath();
            this.description = description;
        }

        /** {@inheritDoc} */
        public String getFilename()
        {
            return this.filename;
        }

        /** {@inheritDoc} */
        public int getBeginLine()
        {
            return 0;
        }

        /** {@inheritDoc} */
        public int getBeginColumn()
        {
            return 0;
        }

        /** {@inheritDoc} */
        public int getEndLine()
        {
            return 0;
        }

        /** {@inheritDoc} */
        public int getEndColumn()
        {
            return 0;
        }

        /** {@inheritDoc} */
        public Rule getRule()
        {
            return null;
        }

        /** {@inheritDoc} */
        public String getDescription()
        {
            return this.description;
        }

        /** {@inheritDoc} */
        public String getPackageName()
        {
            return null;
        }

        /** {@inheritDoc} */
        public String getMethodName()
        {
            return null;
        }

        /** {@inheritDoc} */
        public String getClassName()
        {
            return null;
        }

        /** {@inheritDoc} */
        public boolean isSuppressed()
        {
            return false;
        }

        /** {@inheritDoc} */
        public String getVariableName()
        {
            return null;
        }
    }
}
