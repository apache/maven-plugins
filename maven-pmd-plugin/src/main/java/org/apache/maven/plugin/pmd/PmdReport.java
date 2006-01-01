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

import net.sourceforge.pmd.PMD;
import net.sourceforge.pmd.PMDException;
import net.sourceforge.pmd.Report;
import net.sourceforge.pmd.RuleContext;
import net.sourceforge.pmd.RuleSet;
import net.sourceforge.pmd.RuleSetFactory;
import net.sourceforge.pmd.TargetJDK1_3;
import net.sourceforge.pmd.TargetJDK1_4;
import net.sourceforge.pmd.TargetJDK1_5;

import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.doxia.sink.Sink;
import org.codehaus.doxia.site.renderer.SiteRenderer;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

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
        report.addListener( reportSink );
        ruleContext.setReport( report );

        RuleSetFactory ruleSetFactory = new RuleSetFactory();
        InputStream rulesInput = pmd.getClass().getResourceAsStream( "/rulesets/controversial.xml" );
        RuleSet ruleSet = ruleSetFactory.createRuleSet( rulesInput );

        reportSink.beginDocument();

        List files;
        try
        {
            files = getFilesToProcess( "**/*.java", null );
        }
        catch ( IOException e )
        {
            throw new MavenReportException( "Can't parse " + sourceDirectory, e );
        }

        for ( Iterator i = files.iterator(); i.hasNext(); )
        {
            File file = (File) i.next();
            FileReader fileReader;
            try
            {
                fileReader = new FileReader( file );
            }
            catch ( FileNotFoundException e )
            {
                throw new MavenReportException( "Error opening source file: " + file, e );
            }

            try
            {
                // TODO: lazily call beginFile in case there are no rules

                reportSink.beginFile( file );
                ruleContext.setSourceCodeFilename( file.getAbsolutePath() );
                pmd.processFile( fileReader, ruleSet, ruleContext );
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
            finally
            {
                try
                {
                    fileReader.close();
                }
                catch ( IOException e )
                {
                    throw new MavenReportException( "Error closing source file: " + file, e );
                }
            }
        }
        reportSink.endDocument();
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
        return ( "java".equals( artifactHandler.getLanguage() ) );
    }
}
