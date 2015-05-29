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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;

import net.sourceforge.pmd.cpd.CPD;
import net.sourceforge.pmd.cpd.CPDConfiguration;
import net.sourceforge.pmd.cpd.CSVRenderer;
import net.sourceforge.pmd.cpd.EcmascriptLanguage;
import net.sourceforge.pmd.cpd.JSPLanguage;
import net.sourceforge.pmd.cpd.JavaLanguage;
import net.sourceforge.pmd.cpd.JavaTokenizer;
import net.sourceforge.pmd.cpd.Language;
import net.sourceforge.pmd.cpd.LanguageFactory;
import net.sourceforge.pmd.cpd.Renderer;
import net.sourceforge.pmd.cpd.XMLRenderer;

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.WriterFactory;

/**
 * Creates a report for PMD's CPD tool. See <a
 * href="http://pmd.sourceforge.net/cpd.html">http://pmd.sourceforge.net/cpd.html</a> for more detail.
 *
 * @author Mike Perham
 * @version $Id$
 * @since 2.0
 */
@Mojo( name = "cpd", threadSafe = true )
public class CpdReport
    extends AbstractPmdReport
{
    /**
     * The programming language to be analyzed by CPD. Valid values are currently <code>java</code>,
     * <code>javascript</code> or <code>jsp</code>.
     *
     * @since 3.5
     */
    @Parameter( defaultValue = "java" )
    private String language;

    /**
     * The minimum number of tokens that need to be duplicated before it causes a violation.
     */
    @Parameter( property = "minimumTokens", defaultValue = "100" )
    private int minimumTokens;

    /**
     * Skip the CPD report generation. Most useful on the command line via "-Dcpd.skip=true".
     *
     * @since 2.1
     */
    @Parameter( property = "cpd.skip", defaultValue = "false" )
    private boolean skip;

    /**
     * If true, CPD ignores literal value differences when evaluating a duplicate block. This means that
     * <code>foo=42;</code> and <code>foo=43;</code> will be seen as equivalent. You may want to run PMD with this
     * option off to start with and then switch it on to see what it turns up.
     *
     * @since 2.5
     */
    @Parameter( property = "cpd.ignoreLiterals", defaultValue = "false" )
    private boolean ignoreLiterals;

    /**
     * Similar to <code>ignoreLiterals</code> but for identifiers; i.e., variable names, methods names, and so forth.
     *
     * @since 2.5
     */
    @Parameter( property = "cpd.ignoreIdentifiers", defaultValue = "false" )
    private boolean ignoreIdentifiers;

    /** The CPD instance used to analyze the files. Will itself collect the duplicated code matches. */
    private CPD cpd;

    /**
     * {@inheritDoc}
     */
    public String getName( Locale locale )
    {
        return getBundle( locale ).getString( "report.cpd.name" );
    }

    /**
     * {@inheritDoc}
     */
    public String getDescription( Locale locale )
    {
        return getBundle( locale ).getString( "report.cpd.description" );
    }

    /**
     * {@inheritDoc}
     */
    @Override
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

                generateReport( locale );

                if ( !isHtml() && !isXml() )
                {
                    writeNonHtml( cpd );
                }
            }
            finally
            {
                Thread.currentThread().setContextClassLoader( origLoader );
            }

        }
    }

    @Override
    public boolean canGenerateReport()
    {
        if ( skip )
        {
            return false;
        }

        boolean result = super.canGenerateReport();
        if ( result )
        {
            try
            {
                executeCpdWithClassloader();
                if ( skipEmptyReport )
                {
                    result = cpd.getMatches().hasNext();
                    if ( result )
                    {
                        getLog().debug( "Skipping report since skipEmptyReport is true and there are no CPD issues." );
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

    private void executeCpdWithClassloader()
        throws MavenReportException
    {
        ClassLoader origLoader = Thread.currentThread().getContextClassLoader();
        try
        {
            Thread.currentThread().setContextClassLoader( this.getClass().getClassLoader() );
            executeCpd();
        }
        finally
        {
            Thread.currentThread().setContextClassLoader( origLoader );
        }
    }

    private void executeCpd()
        throws MavenReportException
    {
        if ( cpd != null )
        {
            // CPD has already been run
            getLog().debug( "CPD has already been run - skipping redundant execution." );
            return;
        }

        Properties p = new Properties();
        if ( ignoreLiterals )
        {
            p.setProperty( JavaTokenizer.IGNORE_LITERALS, "true" );
        }
        if ( ignoreIdentifiers )
        {
            p.setProperty( JavaTokenizer.IGNORE_IDENTIFIERS, "true" );
        }

        try
        {
            if ( filesToProcess == null )
            {
                filesToProcess = getFilesToProcess();
            }

            String encoding = determineEncoding( !filesToProcess.isEmpty() );
            Language cpdLanguage;
            if ( "java".equals ( language ) || null == language )
            {
                cpdLanguage = new JavaLanguage( p );
            }
            else if ( "javascript".equals( language ) )
            {
                cpdLanguage = new EcmascriptLanguage();
            }
            else if ( "jsp".equals( language ) )
            {
                cpdLanguage = new JSPLanguage();
            }
            else
            {
                cpdLanguage = LanguageFactory.createLanguage( language, p );
            }

            CPDConfiguration cpdConfiguration = new CPDConfiguration();
            cpdConfiguration.setMinimumTileSize( minimumTokens );
            cpdConfiguration.setLanguage( cpdLanguage );
            cpdConfiguration.setSourceEncoding( encoding );

            cpd = new CPD( cpdConfiguration );

            for ( File file : filesToProcess.keySet() )
            {
                cpd.add( file );
            }
        }
        catch ( UnsupportedEncodingException e )
        {
            throw new MavenReportException( "Encoding '" + getSourceEncoding() + "' is not supported.", e );
        }
        catch ( IOException e )
        {
            throw new MavenReportException( e.getMessage(), e );
        }
        getLog().debug( "Executing CPD..." );
        cpd.go();
        getLog().debug( "CPD finished." );

        // if format is XML, we need to output it even if the file list is empty or we have no duplications
        // so the "check" goals can check for violations
        if ( isXml() )
        {
            writeNonHtml( cpd );
        }
    }

    private void generateReport( Locale locale )
    {
        CpdReportGenerator gen = new CpdReportGenerator( getSink(), filesToProcess, getBundle( locale ), aggregate );
        gen.generate( cpd.getMatches() );
    }

    private String determineEncoding( boolean showWarn )
        throws UnsupportedEncodingException
    {
        String encoding = WriterFactory.FILE_ENCODING;
        if ( StringUtils.isNotEmpty( getSourceEncoding() ) )
        {

            encoding = getSourceEncoding();
            // test encoding as CPD will convert exception into a RuntimeException
            WriterFactory.newWriter( new ByteArrayOutputStream(), encoding );

        }
        else if ( showWarn )
        {
            getLog().warn( "File encoding has not been set, using platform encoding " + WriterFactory.FILE_ENCODING
                               + ", i.e. build is platform dependent!" );
            encoding = WriterFactory.FILE_ENCODING;
        }
        return encoding;
    }

    void writeNonHtml( CPD cpd )
        throws MavenReportException
    {
        Renderer r = createRenderer();

        if ( r == null )
        {
            return;
        }

        String buffer = r.render( cpd.getMatches() );
        FileOutputStream tStream = null;
        Writer writer = null;
        try
        {
            targetDirectory.mkdirs();
            File targetFile = new File( targetDirectory, "cpd." + format );
            tStream = new FileOutputStream( targetFile );
            writer = new OutputStreamWriter( tStream, getOutputEncoding() );
            writer.write( buffer );
            writer.close();

            if ( includeXmlInSite )
            {
                File siteDir = getReportOutputDirectory();
                siteDir.mkdirs();
                FileUtils.copyFile( targetFile, new File( siteDir, "cpd." + format ) );
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
     * {@inheritDoc}
     */
    public String getOutputName()
    {
        return "cpd";
    }

    private static ResourceBundle getBundle( Locale locale )
    {
        return ResourceBundle.getBundle( "cpd-report", locale, CpdReport.class.getClassLoader() );
    }

    /**
     * Create and return the correct renderer for the output type.
     *
     * @return the renderer based on the configured output
     * @throws org.apache.maven.reporting.MavenReportException if no renderer found for the output type
     */
    public Renderer createRenderer()
        throws MavenReportException
    {
        Renderer renderer = null;
        if ( "xml".equals( format ) )
        {
            renderer = new XMLRenderer( getOutputEncoding() );
        }
        else if ( "csv".equals( format ) )
        {
            renderer = new CSVRenderer();
        }
        else if ( !"".equals( format ) && !"none".equals( format ) )
        {
            try
            {
                renderer = (Renderer) Class.forName( format ).newInstance();
            }
            catch ( Exception e )
            {
                throw new MavenReportException( "Can't find CPD custom format " + format + ": "
                    + e.getClass().getName(), e );
            }
        }

        return renderer;
    }
}
