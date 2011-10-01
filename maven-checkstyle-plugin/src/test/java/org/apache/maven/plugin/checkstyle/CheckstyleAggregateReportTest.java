package org.apache.maven.plugin.checkstyle;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.maven.doxia.site.decoration.DecorationModel;
import org.apache.maven.doxia.siterenderer.RendererException;
import org.apache.maven.doxia.siterenderer.SiteRenderingContext;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.reporting.MavenReport;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.WriterFactory;

/**
 * @author Edwin Punzalan
 * @version $Id: CheckstyleReportTest.java 952476 2010-06-07 23:00:42Z olamy $
 */
public class CheckstyleAggregateReportTest
    extends AbstractMojoTestCase
{
    private Locale oldLocale;

    /** {@inheritDoc} */
    protected void setUp()
        throws Exception
    {
        super.setUp();

        oldLocale = Locale.getDefault();
        Locale.setDefault( Locale.ENGLISH );
    }

    /** {@inheritDoc} */
    protected void tearDown()
        throws Exception
    {
        super.tearDown();

        Locale.setDefault( oldLocale );
        oldLocale = null;
    }
    public void testMinConfiguration()
        throws Exception
    {
        generateReport( "multi-plugin-config.xml" );
    }

    private File generateReport( String pluginXml )
        throws Exception
    {
        File pluginXmlFile = new File( getBasedir(), "src/test/plugin-configs/" + pluginXml );
        ResourceBundle bundle =
            ResourceBundle.getBundle( "checkstyle-report", Locale.getDefault(), this.getClassLoader() );

        CheckstyleAggregateReport mojo = (CheckstyleAggregateReport) lookupMojo( "checkstyle-aggregate", pluginXmlFile );

        assertNotNull( "Mojo found.", mojo );

        mojo.execute();

        File outputFile = (File) getVariableValueFromObject( mojo, "outputFile" );
        assertNotNull( "Test output file", outputFile );
        assertTrue( "Test output file exists", outputFile.exists() );

        String cacheFile = (String) getVariableValueFromObject( mojo, "cacheFile" );
        if ( cacheFile != null )
        {
            assertTrue( "Test cache file exists", new File( cacheFile ).exists() );
        }

        MavenReport reportMojo = (MavenReport) mojo;
        File outputDir = reportMojo.getReportOutputDirectory();

        Boolean rss = (Boolean) getVariableValueFromObject( mojo, "enableRSS" );
        if ( rss.booleanValue() )
        {
            File rssFile = new File( outputDir, "checkstyle.rss" );
            assertTrue( "Test rss file exists", rssFile.exists() );
        }

        File useFile = (File) getVariableValueFromObject( mojo, "useFile" );
        if ( useFile != null )
        {
            assertTrue( "Test useFile exists", useFile.exists() );
        }

        String filename = reportMojo.getOutputName() + ".html";
        File outputHtml = new File( outputDir, filename );

        renderer( mojo, outputHtml );

        assertTrue( outputHtml.getAbsolutePath() + " not generated!", outputHtml.exists() );

        assertTrue( outputHtml.getAbsolutePath() + " is empty!", outputHtml.length() > 0 );

        String htmlString = FileUtils.fileRead( outputHtml );

        boolean searchHeaderFound =
            ( htmlString.indexOf( "<h2>" + bundle.getString( "report.checkstyle.rules" ) ) > 0 );
        Boolean rules = (Boolean) getVariableValueFromObject( mojo, "enableRulesSummary" );
        if ( rules.booleanValue() )
        {
            assertTrue( "Test for Rules Summary", searchHeaderFound );
        }
        else
        {
            assertFalse( "Test for Rules Summary", searchHeaderFound );
        }

        searchHeaderFound =
            ( htmlString.indexOf( "<h2>" + bundle.getString( "report.checkstyle.summary" )  ) > 0 );
        Boolean severity = (Boolean) getVariableValueFromObject( mojo, "enableSeveritySummary" );
        if ( severity.booleanValue() )
        {
            assertTrue( "Test for Severity Summary", searchHeaderFound );
        }
        else
        {
            assertFalse( "Test for Severity Summary", searchHeaderFound );
        }

        searchHeaderFound =
            ( htmlString.indexOf( "<h2>" + bundle.getString( "report.checkstyle.files" ) ) > 0 );
        Boolean files = (Boolean) getVariableValueFromObject( mojo, "enableFilesSummary" );
        if ( files.booleanValue() )
        {
            assertTrue( "Test for Files Summary", searchHeaderFound );
        }
        else
        {
            assertFalse( "Test for Files Summary", searchHeaderFound );
        }

        return outputHtml;
    }

    /**
     * Renderer the sink from the report mojo.
     *
     * @param mojo not null
     * @param outputHtml not null
     * @throws RendererException if any
     * @throws IOException if any
     */
    private void renderer( CheckstyleAggregateReport mojo, File outputHtml )
        throws RendererException, Exception
    {
        Writer writer = null;
        SiteRenderingContext context = new SiteRenderingContext();
        context.setDecoration( new DecorationModel() );
        context.setTemplateName( "org/apache/maven/doxia/siterenderer/resources/default-site.vm" );
        context.setLocale( Locale.ENGLISH );

        try
        {
            outputHtml.getParentFile().mkdirs();
            writer = WriterFactory.newXmlWriter( outputHtml );

            mojo.execute();

        }
        finally
        {
            IOUtil.close( writer );
        }
    }
}
