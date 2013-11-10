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
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * A reporting task that performs Checkstyle analysis and generates an HTML report on any violations that Checkstyle finds.
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
        Map<String, String> fmt2Cfg = new HashMap<String, String>();

        fmt2Cfg.put( "sun", "config/sun_checks.xml" );
        fmt2Cfg.put( "turbine", "config/turbine_checks.xml" );
        fmt2Cfg.put( "avalon", "config/avalon_checks.xml" );
        fmt2Cfg.put( "maven", "config/maven_checks.xml" );

        FORMAT_TO_CONFIG_LOCATION = Collections.unmodifiableMap( fmt2Cfg );
    }

    /**
     * Specifies the names filter of the source files to be used for Checkstyle.
     *
     * <strong>Note:</strong> default value is {@code **\/*.java}.
     */
    @Parameter( property = "checkstyle.includes", defaultValue = JAVA_FILES, required = true )
    private String includes;

    /**
     * Specifies the names filter of the source files to be excluded for
     * Checkstyle.
     */
    @Parameter( property = "checkstyle.excludes" )
    private String excludes;

    /**
     * Specifies the names filter of the source files to be used for Checkstyle.
     * @since 2.11
     *
     * <strong>Note:</strong> default value is {@code **\/*.properties}.
     */
    @Parameter( property = "checkstyle.resourceIncludes", defaultValue = "**/*.properties", required = true )
    private String resourceIncludes;

    /**
     * Specifies the names filter of the source files to be excluded for
     * Checkstyle.
     * @since 2.11
     */
    @Parameter( property = "checkstyle.resourceExcludes" )
    private String resourceExcludes;

    /**
     * Specifies whether to include the resource directories in the check.
     * @since 2.11
     */
    @Parameter( property = "checkstyle.includeResources", defaultValue = "true", required = true )
    private boolean includeResources;

    /**
     * Specifies whether to include the test resource directories in the check.
     * @since 2.11
     */
    @Parameter( property = "checkstyle.includeTestResources", defaultValue = "true", required = true )
    private boolean includeTestResources;

    /**
     * <p>
     * Specifies the location of the XML configuration to use.
     * </p>
     * <p/>
     * <p>
     * Potential values are a filesystem path, a URL, or a classpath resource.
     * This parameter expects that the contents of the location conform to the
     * xml format (Checkstyle <a
     * href="http://checkstyle.sourceforge.net/config.html#Modules">Checker
     * module</a>) configuration of rulesets.
     * </p>
     * <p/>
     * <p>
     * This parameter is resolved as resource, URL, then file. If successfully
     * resolved, the contents of the configuration is copied into the
     * <code>${project.build.directory}/checkstyle-configuration.xml</code>
     * file before being passed to Checkstyle as a configuration.
     * </p>
     * <p/>
     * <p>
     * There are 4 predefined rulesets.
     * </p>
     * <p/>
     * <ul>
     * <li><code>config/sun_checks.xml</code>: Sun Checks.</li>
     * <li><code>config/turbine_checks.xml</code>: Turbine Checks.</li>
     * <li><code>config/avalon_checks.xml</code>: Avalon Checks.</li>
     * <li><code>config/maven_checks.xml</code>: Maven Source Checks.</li>
     * </ul>
     */
    @Parameter( property = "checkstyle.config.location", defaultValue = "config/sun_checks.xml" )
    private String configLocation;

    /**
     * Specifies what predefined check set to use. Available sets are "sun" (for
     * the Sun coding conventions), "turbine", and "avalon".
     *
     * @deprecated Use configLocation instead.
     */
    @Parameter( defaultValue = "sun" )
    private String format;

    /**
     * <p>
     * Specifies the location of the properties file.
     * </p>
     * <p/>
     * <p>
     * This parameter is resolved as URL, File then resource. If successfully
     * resolved, the contents of the properties location is copied into the
     * <code>${project.build.directory}/checkstyle-checker.properties</code>
     * file before being passed to Checkstyle for loading.
     * </p>
     * <p/>
     * <p>
     * The contents of the <code>propertiesLocation</code> will be made
     * available to Checkstyle for specifying values for parameters within the
     * xml configuration (specified in the <code>configLocation</code>
     * parameter).
     * </p>
     *
     * @since 2.0-beta-2
     */
    @Parameter( property = "checkstyle.properties.location" )
    private String propertiesLocation;

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
     * Allows for specifying raw property expansion information.
     */
    @Parameter
    private String propertyExpansion;

    /**
     * <p>
     * Specifies the location of the License file (a.k.a. the header file) that
     * can be used by Checkstyle to verify that source code has the correct
     * license header.
     * </p>
     * <p>
     * You need to use ${checkstyle.header.file} in your Checkstyle xml
     * configuration to reference the name of this header file.
     * </p>
     * <p>
     * For instance:
     * </p>
     * <p>
     * <code>
     * &lt;module name="RegexpHeader">
     * &lt;property name="headerFile" value="${checkstyle.header.file}"/>
     * &lt;/module>
     * </code>
     * </p>
     *
     * @since 2.0-beta-2
     */
    @Parameter( property = "checkstyle.header.file", defaultValue = "LICENSE.txt" )
    private String headerLocation;

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
     * Specifies the cache file used to speed up Checkstyle on successive runs.
     */
    @Parameter( defaultValue = "${project.build.directory}/checkstyle-cachefile" )
    private String cacheFile;

    /**
     * <p>
     * Specifies the location of the suppressions XML file to use.
     * </p>
     * <p/>
     * <p>
     * This parameter is resolved as resource, URL, then file. If successfully
     * resolved, the contents of the suppressions XML is copied into the
     * <code>${project.build.directory}/checkstyle-supressions.xml</code> file
     * before being passed to Checkstyle for loading.
     * </p>
     * <p/>
     * <p>
     * See <code>suppressionsFileExpression</code> for the property that will
     * be made available to your checkstyle configuration.
     * </p>
     *
     * @since 2.0-beta-2
     */
    @Parameter( property = "checkstyle.suppressions.location" )
    private String suppressionsLocation;

    /**
     * The key to be used in the properties for the suppressions file.
     *
     * @since 2.1
     */
    @Parameter( property = "checkstyle.suppression.expression", defaultValue = "checkstyle.suppressions.file" )
    private String suppressionsFileExpression;

    /**
     * Specifies the location of the suppressions XML file to use. The plugin
     * defines a Checkstyle property named
     * <code>checkstyle.suppressions.file</code> with the value of this
     * property. This allows using the Checkstyle property in your own custom
     * checkstyle configuration file when specifying a suppressions file.
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

    /**
     * Specifies if the build should fail upon a violation.
     */
    @Parameter( defaultValue = "false" )
    private boolean failsOnError;

    /**
     * Specifies the location of the source directory to be used for Checkstyle.
     */
    @Parameter( defaultValue = "${project.build.sourceDirectory}", required = true )
    private File sourceDirectory;

    /**
     * Specifies the location of the test source directory to be used for
     * Checkstyle.
     *
     * @since 2.2
     */
    @Parameter( defaultValue = "${project.build.testSourceDirectory}" )
    private File testSourceDirectory;

    /**
     * Include or not the test source directory to be used for Checkstyle.
     *
     * @since 2.2
     */
    @Parameter( defaultValue = "false" )
    private boolean includeTestSourceDirectory;

    /**
     * Output errors to console.
     */
    @Parameter( defaultValue = "false" )
    private boolean consoleOutput;

    /**
     * The file encoding to use when reading the source files. If the property <code>project.build.sourceEncoding</code>
     * is not set, the platform default encoding is used. <strong>Note:</strong> This parameter always overrides the
     * property <code>charset</code> from Checkstyle's <code>TreeWalker</code> module.
     *
     * @since 2.2
     */
    @Parameter( property = "encoding", defaultValue = "${project.build.sourceEncoding}" )
    private String encoding;

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
            .setLog( getLog() ).setProject( project ).setSourceDirectory( sourceDirectory ).setResources( resources )
            .setStringOutputStream( stringOutputStream ).setSuppressionsLocation( suppressionsLocation )
            .setTestSourceDirectory( testSourceDirectory ).setConfigLocation( configLocation )
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
        // TODO: would be good to scan the files here
        return !skip && ( sourceDirectory.exists() || ( includeTestSourceDirectory && testSourceDirectory.exists() ) );
    }

    /**
     * Merge in the deprecated parameters to the new ones, unless the new
     * parameters have values.
     *
     * @deprecated Remove when deprecated params are removed.
     */
    private void mergeDeprecatedInfo()
    {
        if ( "config/sun_checks.xml".equals( configLocation ) && !"sun".equals( format ) )
        {
            configLocation = FORMAT_TO_CONFIG_LOCATION.get( format );
        }

        if ( StringUtils.isEmpty( propertiesLocation ) )
        {
            if ( propertiesFile != null )
            {
                propertiesLocation = propertiesFile.getPath();
            }
            else if ( propertiesURL != null )
            {
                propertiesLocation = propertiesURL.toExternalForm();
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
        }

        if ( StringUtils.isEmpty( packageNamesLocation ) )
        {
            packageNamesLocation = packageNamesFile;
        }
    }

}
