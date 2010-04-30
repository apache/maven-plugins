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
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;

import net.sourceforge.pmd.cpd.CPD;
import net.sourceforge.pmd.cpd.CSVRenderer;
import net.sourceforge.pmd.cpd.JavaLanguage;
import net.sourceforge.pmd.cpd.JavaTokenizer;
import net.sourceforge.pmd.cpd.Renderer;
import net.sourceforge.pmd.cpd.XMLRenderer;

import org.apache.maven.reporting.MavenReportException;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.WriterFactory;
import org.codehaus.plexus.util.StringUtils;

/**
 * Creates a report for PMD's CPD tool.  See
 * <a href="http://pmd.sourceforge.net/cpd.html">http://pmd.sourceforge.net/cpd.html</a>
 * for more detail.
 *
 * @author Mike Perham
 * @version $Id$
 * @since 2.0
 * @goal cpd
 */
public class CpdReport
    extends AbstractPmdReport
{
    /**
     * The minimum number of tokens that need to be duplicated before it causes a violation.
     *
     * @parameter expression="${minimumTokens}" default-value="100"
     */
    private int minimumTokens;

    /**
     * Skip the CPD report generation.  Most useful on the command line
     * via "-Dcpd.skip=true".
     *
     * @parameter expression="${cpd.skip}" default-value="false"
     * @since 2.1
     */
    private boolean skip;

    /**
     * If true, CPD ignores literal value differences when evaluating a duplicate block.
     * This means that <code>foo=42;</code> and <code>foo=43;</code> will be seen as equivalent.
     * You may want to run PMD with this option off to start with and then switch it on to see what it turns up.
     *
     * @parameter expression="${cpd.ignoreLiterals}" default-value="false"
     * @since 2.5
     */
    private boolean ignoreLiterals;

    /**
     * Similar to <code>ignoreLiterals</code> but for identifiers; i.e., variable names, methods names, and so forth.
     *
     * @parameter expression="${cpd.ignoreIdentifiers}" default-value="false"
     * @since 2.5
     */
    private boolean ignoreIdentifiers;

    /** {@inheritDoc} */
    public String getName( Locale locale )
    {
        return getBundle( locale ).getString( "report.cpd.name" );
    }

    /** {@inheritDoc} */
    public String getDescription( Locale locale )
    {
        return getBundle( locale ).getString( "report.cpd.description" );
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
        if ( !skip && canGenerateReport() )
        {
            ClassLoader origLoader = Thread.currentThread().getContextClassLoader();
            try
            {
                Thread.currentThread().setContextClassLoader( this.getClass().getClassLoader() );

                CPD cpd = generateReport( locale );

                if ( !isHtml() )
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

    private CPD generateReport( Locale locale )
        throws MavenReportException
    {
        Properties p = new Properties();
        if ( ignoreLiterals )
        {
            p.setProperty( JavaTokenizer.IGNORE_LITERALS, "true" );
        }
        if ( ignoreIdentifiers )
        {
            p.setProperty( JavaTokenizer.IGNORE_IDENTIFIERS, "true" );
        }
        CPD cpd = new CPD( minimumTokens, new JavaLanguage( p ) );

        Map files = null;
        try
        {
            files = getFilesToProcess();

            if ( StringUtils.isNotEmpty( getSourceEncoding() ) )
            {
                cpd.setEncoding( getSourceEncoding() );

                // test encoding as CPD will convert exception into a RuntimeException
                WriterFactory.newWriter( new ByteArrayOutputStream(), getSourceEncoding() );
            }
            else if ( !files.isEmpty() )
            {
                getLog().warn(
                               "File encoding has not been set, using platform encoding "
                                   + WriterFactory.FILE_ENCODING + ", i.e. build is platform dependent!" );
            }

            for ( Iterator it = files.keySet().iterator(); it.hasNext(); )
            {
                cpd.add( (File) it.next() );
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
        cpd.go();

        CpdReportGenerator gen =
            new CpdReportGenerator( getSink(), files, getBundle( locale ), aggregate );
        gen.generate( cpd.getMatches() );

        return cpd;
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
        Writer writer = null;
        try
        {
            targetDirectory.mkdirs();
            File targetFile = new File( targetDirectory, "cpd." + format );
            FileOutputStream tStream = new FileOutputStream( targetFile );
            writer = new OutputStreamWriter( tStream, getOutputEncoding() );
            writer.write( buffer );
            writer.close();

            File siteDir = getReportOutputDirectory();
            siteDir.mkdirs();
            FileUtils.copyFile( targetFile, new File( siteDir, "cpd." + format ) );
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

    /** {@inheritDoc} */
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
     * @throws org.apache.maven.reporting.MavenReportException
     *          if no renderer found for the output type
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
                throw new MavenReportException(
                    "Can't find CPD custom format " + format + ": " + e.getClass().getName() );
            }
        }

        return renderer;
    }
}
