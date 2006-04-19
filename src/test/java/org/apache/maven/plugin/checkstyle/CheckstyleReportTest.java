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

import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.reporting.MavenReport;

import java.io.File;

/**
 * @author Edwin Punzalan
 */
public class CheckstyleReportTest
    extends AbstractMojoTestCase
{
    public void testMinConfiguration()
        throws Exception
    {
        File htmlFile = generateReport( "min-plugin-config.xml" );
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
        assertNotNull( "Test cache file", cacheFile );
        assertTrue( "Test cache file exists", new File( cacheFile ).exists() );

        MavenReport reportMojo = (MavenReport) mojo;
        File outputDir = reportMojo.getReportOutputDirectory();

        Boolean rss = (Boolean) getVariableValueFromObject( mojo, "enableRSS" );
        if ( rss.booleanValue() )
        {
            File rssFile = new File( outputDir, "checkstyle.rss" );
            assertTrue( "Test rss file exists", rssFile.exists() );
        }

        String filename = reportMojo.getOutputName() + ".html";
        File outputHtml = new File( outputDir, filename );
        assertTrue( "Test output html file exists", outputHtml.exists() );

        return outputHtml;
    }
}
