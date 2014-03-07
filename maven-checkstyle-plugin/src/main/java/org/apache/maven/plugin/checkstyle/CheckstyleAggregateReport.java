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

import java.io.File;
import java.util.List;

/**
 * A reporting task that performs Checkstyle analysis and generates an aggregate HTML report on the violations that checkstyle finds in a multi-module reactor build.
 *
 * @version $Id$
 * @since 2.8
 */
@Mojo( name = "checkstyle-aggregate", aggregator = true, requiresDependencyResolution = ResolutionScope.COMPILE,
       threadSafe = true )
public class CheckstyleAggregateReport
    extends AbstractCheckstyleReport
{
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
    @Parameter( property = "checkstyle.consoleOutput", defaultValue = "false" )
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

    /**
     * The projects in the reactor for aggregation report.
     *
     * @since 2.8
     */
    @Parameter( property = "reactorProjects", readonly = true )
    private List<MavenProject> reactorProjects;

    /** {@inheritDoc} */
    protected MavenProject getProject()
    {
        return project;
    }

    /**
     * {@inheritDoc}
     */
    protected CheckstyleExecutorRequest createRequest()
            throws MavenReportException
    {
        CheckstyleExecutorRequest request = new CheckstyleExecutorRequest();
        request.setAggregate( true )
            .setReactorProjects( reactorProjects )
            .setConsoleListener( getConsoleListener() ).setConsoleOutput( consoleOutput )
            .setExcludes( excludes ).setFailsOnError( failsOnError ).setIncludes( includes )
            .setIncludeResources( includeResources )
            .setIncludeTestResources( includeTestResources )
            .setResourceIncludes( resourceIncludes )
            .setResourceExcludes( resourceExcludes )
            .setIncludeTestSourceDirectory( includeTestSourceDirectory ).setListener( getListener() )
            .setLog( getLog() ).setProject( project ).setSourceDirectory( sourceDirectory ).setResources( resources )
            .setTestResources( testResources )
            .setStringOutputStream(stringOutputStream).setSuppressionsLocation( suppressionsLocation )
            .setTestSourceDirectory( testSourceDirectory ).setConfigLocation( configLocation )
            .setPropertyExpansion( propertyExpansion ).setHeaderLocation( headerLocation )
            .setCacheFile( cacheFile ).setSuppressionsFileExpression( suppressionsFileExpression )
            .setEncoding( encoding ).setPropertiesLocation( propertiesLocation );
        return request;
    }


    /** {@inheritDoc} */
    public String getOutputName()
    {
        return "checkstyle-aggregate";
    }

    /** {@inheritDoc} */
    public boolean canGenerateReport()
    {
        // TODO: would be good to scan the files here
        return !skip && project.isExecutionRoot() && reactorProjects.size() > 1;
    }
}
