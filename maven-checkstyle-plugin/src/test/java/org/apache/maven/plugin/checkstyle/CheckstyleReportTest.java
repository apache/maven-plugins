package org.apache.maven.plugin.checkstyle;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
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

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.reporting.MavenReport;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.util.Locale;

/**
 * @author Edwin Punzalan
 */
public class CheckstyleReportTest
    extends AbstractMojoTestCase
{
    private Locale oldDefaultLocale;

    /**
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp()
        throws Exception
    {
        super.setUp();

        // Specify English as default Locale for messages in tests
        oldDefaultLocale = Locale.getDefault();
        Locale.setDefault( Locale.ENGLISH );
    }

    /**
     * @see junit.framework.TestCase#tearDown()
     */
    protected void tearDown()
        throws Exception
    {
        super.tearDown();

        // Restore current locale
        Locale.setDefault( oldDefaultLocale );
    }

    public void testNoSource()
        throws Exception
    {
        File pluginXmlFile = new File( getBasedir(), "src/test/plugin-configs/no-source-plugin-config.xml" );

        Mojo mojo = lookupMojo( "checkstyle", pluginXmlFile );

        assertNotNull( "Mojo found.", mojo );

        mojo.execute();

        File outputFile = (File) getVariableValueFromObject( mojo, "outputFile" );
        assertNotNull( "Test output file", outputFile );
        assertFalse( "Test output file exists", outputFile.exists() );
    }

    public void testMinConfiguration()
        throws Exception
    {
        File htmlFile = generateReport( "min-plugin-config.xml" );
    }

    public void testCustomConfiguration()
        throws Exception
    {
        File htmlFile = generateReport( "custom-plugin-config.xml" );
    }

    public void testUseFile()
        throws Exception
    {
        File htmlFile = generateReport( "useFile-plugin-config.xml" );
    }

    public void testNoRulesSummary()
        throws Exception
    {
        File htmlFile = generateReport( "no-rules-plugin-config.xml" );
    }

    public void testNoSeveritySummary()
        throws Exception
    {
        File htmlFile = generateReport( "no-severity-plugin-config.xml" );
    }

    public void testNoFilesSummary()
        throws Exception
    {
        File htmlFile = generateReport( "no-files-plugin-config.xml" );
    }

    public void testFailOnError()
    {
        try
        {
            File htmlFile = generateReport( "fail-on-error-plugin-config.xml" );

            fail( "Must throw exception on errors" );
        }
        catch ( Exception e )
        {
            //expected
        }
    }

    public void testDependencyResolutionException()
    {
        try
        {
            File htmlFile = generateReport( "dep-resolution-exception-plugin-config.xml" );

            fail( "Must throw exception on errors" );
        }
        catch ( Exception e )
        {
            if ( !( e.getCause().getCause() instanceof DependencyResolutionRequiredException ) )
            {
                fail( "Must throw exception on errors" );
            }
        }
    }

    public void testTestSourceDirectory()
        throws Exception
    {
        File htmlFile = generateReport( "test-source-directory-plugin-config.xml" );
    }

    private File generateReport( String pluginXml )
        throws Exception
    {
        File pluginXmlFile = new File( getBasedir(), "src/test/plugin-configs/" + pluginXml );

        Mojo mojo = lookupMojo( "checkstyle", pluginXmlFile );

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
        assertTrue( "Test output html file exists", outputHtml.exists() );
        String htmlString = FileUtils.fileRead( outputHtml );

        boolean searchHeaderFound = ( htmlString.indexOf( "<h2>Rules</h2>" ) > 0 );
        Boolean rules = (Boolean) getVariableValueFromObject( mojo, "enableRulesSummary" );
        if ( rules.booleanValue() )
        {
            assertTrue( "Test for Rules Summary", searchHeaderFound );
        }
        else
        {
            assertFalse( "Test for Rules Summary", searchHeaderFound );
        }

        searchHeaderFound = ( htmlString.indexOf( "<h2>Summary</h2>" ) > 0 );
        Boolean severity = (Boolean) getVariableValueFromObject( mojo, "enableSeveritySummary" );
        if ( severity.booleanValue() )
        {
            assertTrue( "Test for Severity Summary", searchHeaderFound );
        }
        else
        {
            assertFalse( "Test for Severity Summary", searchHeaderFound );
        }

        searchHeaderFound = ( htmlString.indexOf( "<h2>Files</h2>" ) > 0 );
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
}
