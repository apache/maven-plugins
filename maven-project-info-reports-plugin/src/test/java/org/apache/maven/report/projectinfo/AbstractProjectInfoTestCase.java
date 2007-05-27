package org.apache.maven.report.projectinfo;

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

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.profiles.DefaultProfileManager;
import org.apache.maven.profiles.ProfileManager;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.reporting.MavenReport;
import org.codehaus.plexus.i18n.I18N;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

/**
 * Abstract class to test reports generation with <a href="http://www.httpunit.org/">HTTPUnit</a> framework.
 *
 * @author Edwin Punzalan
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id $
 */
public abstract class AbstractProjectInfoTestCase
    extends AbstractMojoTestCase
{
    /**
     * The default locale is English.
     */
    protected static final Locale DEFAULT_LOCALE = Locale.ENGLISH;

    /**
     * The current project to be test.
     */
    private MavenProject testMavenProject;

    /**
     * The I18N plexus component.
     */
    private I18N i18n;

    /**
     * @see org.apache.maven.plugin.testing.AbstractMojoTestCase#setUp()
     */
    protected void setUp()
        throws Exception
    {
        // required for mojo lookups to work
        super.setUp();

        i18n = (I18N) getContainer().lookup( I18N.ROLE );

        File f = new File( getBasedir(), "target/local-repo/" );
        f.mkdirs();

        // Set the default Locale
        Locale.setDefault( DEFAULT_LOCALE );
    }

    /**
     * @see org.codehaus.plexus.PlexusTestCase#getCustomConfiguration()
     */
    protected InputStream getCustomConfiguration()
        throws Exception
    {
        // Allow sub classes to have their own configuration...
        if ( super.getConfiguration() == null )
        {
            String className = AbstractProjectInfoTestCase.class.getName();

            String config = className.substring( className.lastIndexOf( "." ) + 1 ) + ".xml";

            return AbstractProjectInfoTestCase.class.getResourceAsStream( config );
        }

        return null;
    }

    /**
     * @see junit.framework.TestCase#tearDown()
     */
    protected void tearDown()
        throws Exception
    {
        // nop
    }

    /**
     * Gets a trimmed String for the given key from the resource bundle defined by Plexus.
     *
     * @param key the key for the desired string
     * @return the string for the given key
     * @throws IllegalArgumentException if the parameter is empty.
     */
    protected String getString( String key )
    {
        if ( StringUtils.isEmpty( key ) )
        {
            throw new IllegalArgumentException( "The key cannot be empty" );
        }

        return i18n.getString( key, Locale.getDefault() ).trim();
    }

    /**
     * Get the current Maven project
     *
     * @return the maven project
     */
    protected MavenProject getTestMavenProject()
    {
        return testMavenProject;
    }

    /**
     * Get the generated report as file in the test maven project.
     *
     * @return the generated report as file
     * @throws IOException if the return file doesnt exist
     */
    protected File getGeneratedReport(String name )
        throws IOException
    {
        String outputDirectory = getBasedir() + "/target/test-harness/" + getTestMavenProject().getArtifactId();

        File report = new File( outputDirectory, name );
        if ( !report.exists() )
        {
            throw new IOException( "File not found. Attempted :" + report );
        }

        return report;
    }

    /**
     * Generate the report and return the generated file
     *
     * @param goal
     * @param pluginXml
     * @return the generated HTML file
     * @throws Exception if any
     */
    protected File generateReport( String goal, String pluginXml )
        throws Exception
    {
        File pluginXmlFile = new File( getBasedir(), "src/test/resources/plugin-configs/" + pluginXml );
        Mojo mojo = lookupMojo( goal, pluginXmlFile );
        assertNotNull( "Mojo found.", mojo );

        mojo.execute();

        MavenProjectBuilder builder = (MavenProjectBuilder) lookup( MavenProjectBuilder.ROLE );
        ProfileManager profileManager = new DefaultProfileManager( getContainer() );

        ArtifactRepository localRepository = ( ArtifactRepository ) getVariableValueFromObject( mojo, "localRepository" );

        testMavenProject = builder.buildWithDependencies( pluginXmlFile, localRepository, profileManager );

        MavenReport reportMojo = (MavenReport) mojo;
        File outputDir = reportMojo.getReportOutputDirectory();
        String filename = reportMojo.getOutputName() + ".html";

        return new File( outputDir, filename );
    }
}
