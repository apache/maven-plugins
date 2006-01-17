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
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Locale;
import java.util.ResourceBundle;

import net.sourceforge.pmd.cpd.CPD;
import net.sourceforge.pmd.cpd.CSVRenderer;
import net.sourceforge.pmd.cpd.JavaLanguage;
import net.sourceforge.pmd.cpd.Renderer;
import net.sourceforge.pmd.cpd.SimpleRenderer;
import net.sourceforge.pmd.cpd.XMLRenderer;

import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.doxia.site.renderer.SiteRenderer;

/**
 * Report for PMD's CPD tool.  See <a href="http://pmd.sourceforge.net/cpd.html">http://pmd.sourceforge.net/cpd.html</a>
 * for more detail.
 *
 * @author Mike Perham
 * @version $Id: PmdReport.java,v 1.3 2005/02/23 00:08:53 brett Exp $
 * @goal cpd
 * @todo needs to support the multiple source roots
 */
public class CpdReport
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
     * Set the output format type.  Defaults to "html".  Must be one of:
     * "html", "csv", "xml", "txt" or the full class name of the PMD renderer to use.
     * See the net.sourceforge.pmd.cpd package javadoc for available renderers.
     * 
     * @parameter
     */
    private String format = "html";

    /**
     * Link the violation line numbers to the source xref.  See the JXR plugin
     * for more details.
     * @parameter
     * 
     * TODO Can we automagically determine if xfer is being run and enable this?
     */
    private boolean linkXref;

    /**
     * The location of the xref pages relative to the location of the CPD report.
     * @parameter
     */
    private String xrefLocation = "xref";

    /**
     * @parameter
     */
    private int minimumTokens = 100;

    /**
     * @see org.apache.maven.reporting.MavenReport#getName(java.util.Locale)
     */
    public String getName( Locale locale )
    {
        return getBundle( locale ).getString( "report.cpd.name" );
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#getDescription(java.util.Locale)
     */
    public String getDescription( Locale locale )
    {
        return getBundle( locale ).getString( "report.cpd.description" );
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

        CPD cpd = new CPD( minimumTokens, new JavaLanguage() );
        String src = getProject().getBuild().getSourceDirectory();
        if ( !new File( src ).exists() ) 
        {
            return;
        }
        
        try
        {
            // TODO: use source roots instead
            cpd.addRecursively( src );
        }
        catch ( IOException e )
        {
            throw new MavenReportException( e.getMessage(), e );
        }
        cpd.go();

        CpdReportGenerator gen = new CpdReportGenerator( getSink(), src, getBundle( locale ), linkXref ? xrefLocation
                                                                                                      : null );
        gen.generate( cpd.getMatches() );

        if ( !isHtml() )
        {
            Renderer r = createRenderer();
            String buffer = r.render( cpd.getMatches() );
            try
            {
                Writer writer = new FileWriter( new File( this.getReportOutputDirectory(), "cpd." + format ) );
                writer.write( buffer, 0, buffer.length() );
                writer.close();
            }
            catch ( IOException ioe )
            {
                throw new MavenReportException( ioe.getMessage(), ioe );
            }
        }
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#getOutputName()
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
     * @see org.apache.maven.reporting.AbstractMavenReport#canGenerateReport()
     */
    public boolean canGenerateReport()
    {
        ArtifactHandler artifactHandler = project.getArtifact().getArtifactHandler();
        return ( "java".equals( artifactHandler.getLanguage() ) );
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
            return new SimpleRenderer();
        }
        else if ( format.equals( "csv" ) )
        {
            return new CSVRenderer();
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
