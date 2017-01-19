package org.apache.maven.plugins.checkstyle;

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
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.maven.model.Resource;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.plugins.checkstyle.exec.CheckstyleExecutorRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.plexus.util.StringUtils;

/**
 * A reporting task that performs Checkstyle analysis and generates an HTML
 * report on any violations that Checkstyle finds.
 *
 * @author <a href="mailto:evenisse@apache.org">Emmanuel Venisse</a>
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
@Mojo( name = "checkstyle", requiresDependencyResolution = ResolutionScope.COMPILE, threadSafe = true )
public class CheckstyleReport
    extends AbstractCheckstyleReport
{
    /**
     * @deprecated Remove with format parameter.
     */
    private static final Map<String, String> FORMAT_TO_CONFIG_LOCATION;

    static
    {
        Map<String, String> fmt2Cfg = new HashMap<>();

        fmt2Cfg.put( "sun", "sun_checks.xml" );

        FORMAT_TO_CONFIG_LOCATION = Collections.unmodifiableMap( fmt2Cfg );
    }

    /**
     * Specifies what predefined check set to use. Available sets are "sun" (for
     * the Sun coding conventions), and "maven".
     *
     * @deprecated Use configLocation instead.
     */
    @Parameter( defaultValue = "sun" )
    private String format;

    /**
     * Specifies the location of the Checkstyle properties file that will be used to
     * check the source.
     *
     * @deprecated Use propertiesLocation instead.
     */
    @Parameter
    private File propertiesFile;

    /**
     * Specifies the URL of the Checkstyle properties that will be used to check
     * the source.
     *
     * @deprecated Use propertiesLocation instead.
     */
    @Parameter
    private URL propertiesURL;

    /**
     * Specifies the location of the License file (a.k.a. the header file) that
     * is used by Checkstyle to verify that source code has the correct
     * license header.
     *
     * @deprecated Use headerLocation instead.
     */
    @Parameter( defaultValue = "${basedir}/LICENSE.txt" )
    private File headerFile;

    /**
     * Specifies the location of the suppressions XML file to use. The plugin
     * defines a Checkstyle property named
     * <code>checkstyle.suppressions.file</code> with the value of this
     * property. This allows using the Checkstyle property in your own custom
     * Checkstyle configuration file when specifying a suppressions file.
     *
     * @deprecated Use suppressionsLocation instead.
     */
    @Parameter
    private String suppressionsFile;

    /**
     * <p>
     * Specifies the location of the package names XML to be used to configure
     * the Checkstyle <a
     * href="http://checkstyle.sourceforge.net/config.html#Packages">Packages</a>.
     * </p>
     * <p/>
     * <p>
     * This parameter is resolved as resource, URL, then file. If resolved to a
     * resource, or a URL, the contents of the package names XML is copied into
     * the <code>${project.build.directory}/checkstyle-packagenames.xml</code>
     * file before being passed to Checkstyle for loading.
     * </p>
     *
     * @since 2.0-beta-2
     */
    @Parameter
    private String packageNamesLocation;

    /**
     * Specifies the location of the package names XML to be used to configure
     * Checkstyle.
     *
     * @deprecated Use packageNamesLocation instead.
     */
    @Parameter
    private String packageNamesFile;

    /** {@inheritDoc} */
    protected MavenProject getProject()
    {
        return project;
    }

    /** {@inheritDoc} */
    public void executeReport( Locale locale )
        throws MavenReportException
    {
        mergeDeprecatedInfo();
        super.executeReport( locale );
    }

    /**
     * {@inheritDoc}
     */
    protected CheckstyleExecutorRequest createRequest()
            throws MavenReportException
    {
        CheckstyleExecutorRequest request = new CheckstyleExecutorRequest();
        request.setConsoleListener( getConsoleListener() ).setConsoleOutput( consoleOutput )
            .setExcludes( excludes ).setFailsOnError( failsOnError ).setIncludes( includes )
            .setResourceIncludes( resourceIncludes )
            .setResourceExcludes( resourceExcludes )
            .setIncludeResources( includeResources )
            .setIncludeTestResources( includeTestResources )
            .setIncludeTestSourceDirectory( includeTestSourceDirectory ).setListener( getListener() )
            .setProject( project ).setSourceDirectories( getSourceDirectories() )
            .setResources( resources )
            .setStringOutputStream( stringOutputStream ).setSuppressionsLocation( suppressionsLocation )
            .setTestSourceDirectories( getTestSourceDirectories() ).setConfigLocation( configLocation )
            .setPropertyExpansion( propertyExpansion ).setHeaderLocation( headerLocation )
            .setCacheFile( cacheFile ).setSuppressionsFileExpression( suppressionsFileExpression )
            .setEncoding( encoding ).setPropertiesLocation( propertiesLocation );
        return request;
    }

    /** {@inheritDoc} */
    public String getOutputName()
    {
        return "checkstyle";
    }

    /** {@inheritDoc} */
    public boolean canGenerateReport()
    {
        if ( skip )
        {
            return false;
        }
        
        // TODO: would be good to scan the files here
        for ( File sourceDirectory : getSourceDirectories() )
        {
            if ( sourceDirectory.exists() )
            {
                return true;
            }
        }
        
        if ( includeTestSourceDirectory )
        {
            for ( File testSourceDirectory : getTestSourceDirectories() )
            {
                if ( testSourceDirectory.exists() )
                {
                    return true;
                }
            }
        }
        
        return ( ( includeResources && hasResources( resources ) )
            || ( includeTestResources && hasResources( testResources ) )
        );
    }

    /**
     * Check if any of the resources exist.
     * @param resources The resources to check
     * @return <code>true</code> if the resource directory exist
     */
    private boolean hasResources( List<Resource> resources )
    {
        for ( Resource resource : resources )
        {
            if ( new File( resource.getDirectory() ).exists() )
            {
                return true;
            }
        }
      return false;
    }

    /**
     * Merge in the deprecated parameters to the new ones, unless the new
     * parameters have values.
     * @throws MavenReportException 
     *
     * @deprecated Remove when deprecated params are removed.
     */
    private void mergeDeprecatedInfo()
        throws MavenReportException
    {
        if ( "sun_checks.xml".equals( configLocation ) && !"sun".equals( format ) )
        {
            configLocation = FORMAT_TO_CONFIG_LOCATION.get( format );

            throw new MavenReportException( "'format' parameter is deprecated: please replace with <configLocation>"
                + configLocation + "</configLocation>." );
        }

        if ( StringUtils.isEmpty( propertiesLocation ) )
        {
            if ( propertiesFile != null )
            {
                propertiesLocation = propertiesFile.getPath();

                throw new MavenReportException( "'propertiesFile' parameter is deprecated: please replace with "
                    + "<propertiesLocation>" + propertiesLocation + "</propertiesLocation>." );
            }
            else if ( propertiesURL != null )
            {
                propertiesLocation = propertiesURL.toExternalForm();

                throw new MavenReportException( "'propertiesURL' parameter is deprecated: please replace with "
                                + "<propertiesLocation>" + propertiesLocation + "</propertiesLocation>." );
            }
        }

        if ( "LICENSE.txt".equals( headerLocation ) )
        {
            File defaultHeaderFile = new File( project.getBasedir(), "LICENSE.txt" );
            if ( !defaultHeaderFile.equals( headerFile ) )
            {
                headerLocation = headerFile.getPath();
            }
        }

        if ( StringUtils.isEmpty( suppressionsLocation ) )
        {
            suppressionsLocation = suppressionsFile;

            if ( StringUtils.isNotEmpty( suppressionsFile ) )
            {
                throw new MavenReportException( "'suppressionsFile' parameter is deprecated: please replace with "
                    + "<suppressionsLocation>" + suppressionsLocation + "</suppressionsLocation>." );
            }
        }

        if ( StringUtils.isEmpty( packageNamesLocation ) )
        {
            packageNamesLocation = packageNamesFile;

            if ( StringUtils.isNotEmpty( packageNamesFile ) )
            {
                throw new MavenReportException( "'packageNamesFile' parameter is deprecated: please replace with "
                    + "<packageNamesFile>" + suppressionsLocation + "</packageNamesFile>." );
            }
        }
    }

}
