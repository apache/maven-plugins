package org.apache.maven.plugin.pmd;

/*
 * Copyright 2005 The Apache Software Foundation.
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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import net.sourceforge.pmd.PMD;
import net.sourceforge.pmd.PMDException;
import net.sourceforge.pmd.Report;
import net.sourceforge.pmd.RuleContext;
import net.sourceforge.pmd.RuleSet;
import net.sourceforge.pmd.RuleSetFactory;
import net.sourceforge.pmd.TargetJDK1_3;
import net.sourceforge.pmd.TargetJDK1_4;
import net.sourceforge.pmd.TargetJDK1_5;
import net.sourceforge.pmd.renderers.CSVRenderer;
import net.sourceforge.pmd.renderers.HTMLRenderer;
import net.sourceforge.pmd.renderers.Renderer;
import net.sourceforge.pmd.renderers.TextRenderer;
import net.sourceforge.pmd.renderers.XMLRenderer;

import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.doxia.sink.Sink;
import org.codehaus.doxia.site.renderer.SiteRenderer;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;

/**
 * Implement the PMD report.
 *
 * @author Brett Porter
 * @version $Id: PmdReport.java,v 1.3 2005/02/23 00:08:53 brett Exp $
 * @goal pmd
 * @todo needs to support the multiple source roots
 */
public class PmdReport
    extends AbstractMavenReport
{
    /**
     * @parameter expression="${project.reporting.outputDirectory}"
     * @required
     */
    private String outputDirectory;

    /**
     * @component
     */
    private SiteRenderer siteRenderer;

    /**
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * @parameter expression="${targetJdk}
     */
    private String targetJdk;

    /**
     * Set the output format type.  Defaults to "html".  Must be one of:
     * "html", "csv", "xml", "txt" or the full class name of the PMD renderer to use.
     * See the net.sourceforge.pmd.renderers package javadoc for available renderers.
     * 
     * @parameter expression="${format}"
     */
    private String format = "html";

    /**
     * The PMD rulesets to use.  <a href="http://pmd.sourceforge.net/rules/index.html">Stock Rulesets</a>
     * Defaults to the basic, imports and unusedcode rulesets. 
     * 
     * @parameter
     */
    private String[] rulesets = new String[] 
    {
        "/rulesets/basic.xml",
        "/rulesets/unusedcode.xml",
        "/rulesets/imports.xml",
    };

    /**
     * Link the violation line numbers to the source xref.
     * @parameter
     * 
     * TODO Can we automagically determine if xfer is being run and enable this?
     */
    private boolean linkXref;

    /**
     * The location of the xref pages relative to the location of the pmd report.
     * @parameter
     */
    private String xrefLocation = "xref";

    /**
     * The file encoding to use when reading the java source.
     * @parameter 
     */
    private String sourceEncoding;

    /**
     * @see org.apache.maven.reporting.MavenReport#getName(java.util.Locale)
     */
    public String getName( Locale locale )
    {
        return getBundle( locale ).getString( "report.pmd.name" );
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#getDescription(java.util.Locale)
     */
    public String getDescription( Locale locale )
    {
        return getBundle( locale ).getString( "report.pmd.description" );
    }

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#getOutputDirectory()
     */
    protected String getOutputDirectory()
    {
        return outputDirectory;
    }

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

    private boolean isHtml()
    {
        return "html".equals( format );
    }

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#executeReport(java.util.Locale)
     */
    public void executeReport( Locale locale )
        throws MavenReportException
    {
        Sink sink = getSink();

        PMD pmd = getPMD();
        RuleContext ruleContext = new RuleContext();
        Report report = new Report();
        // TODO: use source roots instead
        String sourceDirectory = getProject().getBuild().getSourceDirectory();
        PmdReportListener reportSink = new PmdReportListener( sink, sourceDirectory, getBundle( locale ) );
        if ( linkXref )
        {
            reportSink.setXrefLocation( xrefLocation );
        }

        report.addListener( reportSink );
        ruleContext.setReport( report );
        reportSink.beginDocument();

        List files;
        try
        {
            // MPMD-2  pmd has problems with package-info.java
            String excludes = "**/package-info.java";
            files = getFilesToProcess( "**/*.java", excludes );
        }
        catch ( IOException e )
        {
            throw new MavenReportException( "Can't parse " + sourceDirectory, e );
        }

        Locator locator = new Locator( getLog() );
        RuleSetFactory ruleSetFactory = new RuleSetFactory();
        RuleSet[] sets = new RuleSet[rulesets.length];
        try
        {
            for ( int idx = 0; idx < rulesets.length; idx++ )
            {
                String set = rulesets[idx];
                getLog().debug( "Preparing ruleset: " + set );
                File ruleset = locator.resolveLocation( set, getLocationTemp( set ) );
                InputStream rulesInput = new FileInputStream( ruleset );
                sets[idx] = ruleSetFactory.createRuleSet( rulesInput );
            }
        }
        catch ( IOException e )
        {
            throw new MavenReportException( e.getMessage(), e );
        }

        boolean hasEncoding = sourceEncoding != null;

        for ( Iterator i = files.iterator(); i.hasNext(); )
        {
            File file = (File) i.next();

            try
            {
                // TODO: lazily call beginFile in case there are no rules

                reportSink.beginFile( file );
                ruleContext.setSourceCodeFilename( file.getAbsolutePath() );
                for ( int idx = 0; idx < rulesets.length; idx++ )
                {
                    try
                    {
                        // PMD closes this Reader even though it did not open it so we have
                        // to open a new one with every call to processFile().
                        Reader reader = hasEncoding ? new InputStreamReader( new FileInputStream( file ),
                                                                             sourceEncoding ) : new FileReader( file );
                        pmd.processFile( reader, sets[idx], ruleContext );
                    }
                    catch ( UnsupportedEncodingException e1 )
                    {
                        throw new MavenReportException( "Encoding '" + sourceEncoding + "' is not supported.", e1 );
                    }
                }
                reportSink.endFile( file );
            }
            catch ( PMDException e )
            {
                Exception ex = e;
                if ( e.getReason() != null )
                {
                    ex = e.getReason();
                }
                throw new MavenReportException( "Failure executing PMD for: " + file, ex );
            }
            catch ( FileNotFoundException e )
            {
                throw new MavenReportException( "Error opening source file: " + file, e );
            }
        }
        reportSink.endDocument();

        if ( !isHtml() )
        {
            // Use the PMD renderers to render in any format aside from HTML.
            Renderer r = createRenderer();
            String buffer = r.render( report );
            try
            {
                Writer writer = new FileWriter( new File( this.getReportOutputDirectory(), "pmd." + format ) );
                writer.write( buffer, 0, buffer.length() );
                writer.close();
            }
            catch ( IOException ioe )
            {
                throw new MavenReportException( ioe.getMessage(), ioe );
            }
        }
    }

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
        return project.getBuild().getDirectory() + File.separator + loc;
    }

    /**
     * Constructs the PMD class, passing it an argument
     * that configures the target JDK.
     *
     * @return the resulting PMD
     */
    public PMD getPMD()
    {
        PMD pmd;
        if ( "1.5".equals( targetJdk ) )
        {
            pmd = new PMD( new TargetJDK1_5() );
        }
        else if ( "1.4".equals( targetJdk ) )
        {
            pmd = new PMD( new TargetJDK1_4() );
        }
        else if ( "1.3".equals( targetJdk ) )
        {
            pmd = new PMD( new TargetJDK1_3() );
        }
        else
        {
            pmd = new PMD();
        }
        return pmd;
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#getOutputName()
     */
    public String getOutputName()
    {
        return "pmd";
    }

    private List getFilesToProcess( String includes, String excludes )
        throws IOException
    {
        File dir = new File( getProject().getBuild().getSourceDirectory() );
        if ( !dir.exists() )
        {
            return Collections.EMPTY_LIST;
        }

        StringBuffer excludesStr = new StringBuffer();
        if ( StringUtils.isNotEmpty( excludes ) )
        {
            excludesStr.append( excludes );
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

        return FileUtils.getFiles( dir, includes, excludesStr.toString() );
    }

    private static ResourceBundle getBundle( Locale locale )
    {
        return ResourceBundle.getBundle( "pmd-report", locale, PmdReport.class.getClassLoader() );
    }

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#canGenerateReport()
     */
    public boolean canGenerateReport()
    {
        ArtifactHandler artifactHandler = project.getArtifact().getArtifactHandler();
        return ( "java".equals( artifactHandler.getLanguage() ) && new File( getProject().getBuild()
            .getSourceDirectory() ).exists() );
    }

    /**
     * Create and return the correct renderer for the output type.
     * @return the renderer based on the configured output
     * @throws MavenReportException if no renderer found for the output type
     */
    public final Renderer createRenderer()
        throws MavenReportException
    {
        if ( format.equals( "xml" ) )
        {
            return new XMLRenderer();
        }
        else if ( format.equals( "txt" ) )
        {
            return new TextRenderer();
        }
        else if ( format.equals( "csv" ) )
        {
            return new CSVRenderer();
        }
        else if ( format.equals( "html" ) )
        {
            return new HTMLRenderer();
        }
        if ( !format.equals( "" ) )
        {
            try
            {
                return (Renderer) Class.forName( format ).newInstance();
            }
            catch ( Exception e )
            {
                throw new MavenReportException( "Can't find the custom format " + format + ": "
                    + e.getClass().getName() );
            }
        }

        throw new MavenReportException( "Can't create report with format of " + format );
    }
}
