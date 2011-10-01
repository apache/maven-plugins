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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.List;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import com.puppycrawl.tools.checkstyle.DefaultLogger;
import com.puppycrawl.tools.checkstyle.api.AuditListener;

/**
 * @author <a href="mailto:olamy@apache.org">olamy</a>
 * @since 2.5
 * @version $Id$
 */
public class CheckstyleExecutorRequest
{

    /**
     * Specifies the names filter of the source files to be used for Checkstyle.
     */
    private String includes;

    /**
     * Specifies the names filter of the source files to be excluded for Checkstyle.
     */
    private String excludes;

    private MavenProject project;

    private Log log;

    private String suppressionsLocation;

    private boolean includeTestSourceDirectory;

    private File testSourceDirectory;

    private File sourceDirectory;

    private boolean failsOnError;

    private AuditListener listener;

    private boolean consoleOutput;

    private DefaultLogger defaultLogger;

    private ByteArrayOutputStream stringOutputStream;

    private String propertiesLocation;

    //

    private String configLocation;

    private String propertyExpansion;

    private String headerLocation;

    private String cacheFile;

    private String suppressionsFileExpression;

    private String encoding;

    private boolean aggregate = false;

    private List<MavenProject> reactorProjects;

    /**
     * Constructor.
     */
    public CheckstyleExecutorRequest( )
    {
        //nothing
    }

    /**
     * Returns the includes parameter.
     *
     * @return The includes parameter.
     */
    public String getIncludes()
    {
        return includes;
    }

    public CheckstyleExecutorRequest setIncludes( String includes )
    {
        this.includes = includes;
        return this;
    }

    public String getExcludes()
    {
        return excludes;
    }

    public CheckstyleExecutorRequest setExcludes( String excludes )
    {
        this.excludes = excludes;
        return this;
    }

    public MavenProject getProject()
    {
        return project;
    }

    public CheckstyleExecutorRequest setProject( MavenProject project )
    {
        this.project = project;
        return this;
    }

    public Log getLog()
    {
        return log;
    }

    public CheckstyleExecutorRequest setLog( Log log )
    {
        this.log = log;
        return this;
    }

    public String getSuppressionsLocation()
    {
        return suppressionsLocation;
    }

    public CheckstyleExecutorRequest setSuppressionsLocation( String suppressionsLocation )
    {
        this.suppressionsLocation = suppressionsLocation;
        return this;
    }

    public boolean isIncludeTestSourceDirectory()
    {
        return includeTestSourceDirectory;
    }

    public CheckstyleExecutorRequest setIncludeTestSourceDirectory( boolean includeTestSourceDirectory )
    {
        this.includeTestSourceDirectory = includeTestSourceDirectory;
        return this;
    }

    public File getTestSourceDirectory()
    {
        return testSourceDirectory;
    }

    public CheckstyleExecutorRequest setTestSourceDirectory( File testSourceDirectory )
    {
        this.testSourceDirectory = testSourceDirectory;
        return this;
    }

    public File getSourceDirectory()
    {
        return sourceDirectory;
    }

    public CheckstyleExecutorRequest setSourceDirectory( File sourceDirectory )
    {
        this.sourceDirectory = sourceDirectory;
        return this;
    }

    public boolean isFailsOnError()
    {
        return failsOnError;
    }

    public CheckstyleExecutorRequest setFailsOnError( boolean failsOnError )
    {
        this.failsOnError = failsOnError;
        return this;
    }

    public AuditListener getListener()
    {
        return listener;
    }

    public CheckstyleExecutorRequest setListener( AuditListener listener )
    {
        this.listener = listener;
        return this;
    }

    public boolean isConsoleOutput()
    {
        return consoleOutput;
    }

    public CheckstyleExecutorRequest setConsoleOutput( boolean consoleOutput )
    {
        this.consoleOutput = consoleOutput;
        return this;
    }

    public CheckstyleExecutorRequest setConsoleListener( DefaultLogger defaultLogger )
    {
        this.defaultLogger = defaultLogger;
        return this;
    }

    public DefaultLogger getConsoleListener()
    {
        return this.defaultLogger;
    }

    public ByteArrayOutputStream getStringOutputStream()
    {
        return stringOutputStream;
    }

    public CheckstyleExecutorRequest setStringOutputStream( ByteArrayOutputStream stringOutputStream )
    {
        this.stringOutputStream = stringOutputStream;
        return this;
    }

    public String getConfigLocation()
    {
        return configLocation;
    }

    public CheckstyleExecutorRequest setConfigLocation( String configLocation )
    {
        this.configLocation = configLocation;
        return this;
    }

    public String getPropertyExpansion()
    {
        return propertyExpansion;
    }

    public CheckstyleExecutorRequest setPropertyExpansion( String propertyExpansion )
    {
        this.propertyExpansion = propertyExpansion;
        return this;
    }

    public String getHeaderLocation()
    {
        return headerLocation;
    }

    public CheckstyleExecutorRequest setHeaderLocation( String headerLocation )
    {
        this.headerLocation = headerLocation;
        return this;
    }

    public String getCacheFile()
    {
        return cacheFile;
    }

    public CheckstyleExecutorRequest setCacheFile( String cacheFile )
    {
        this.cacheFile = cacheFile;
        return this;
    }

    public String getSuppressionsFileExpression()
    {
        return suppressionsFileExpression;
    }

    public CheckstyleExecutorRequest setSuppressionsFileExpression( String suppressionsFileExpression )
    {
        this.suppressionsFileExpression = suppressionsFileExpression;
        return this;
    }

    public String getEncoding()
    {
        return encoding;
    }

    public CheckstyleExecutorRequest setEncoding( String encoding )
    {
        this.encoding = encoding;
        return this;
    }

    public String getPropertiesLocation()
    {
        return propertiesLocation;
    }

    public void setPropertiesLocation( String propertiesLocation )
    {
        this.propertiesLocation = propertiesLocation;
    }

    /**
     * Returns true if the report is aggregated.
     *
     * @return <code>true</code> if the report is aggregated.
     */
    public boolean isAggregate()
    {
        return aggregate;
    }

    /**
     * Sets the aggregate parameter.
     *
     * @param pAggregate <code>true</code> if an aggregated report is desidered.
     * @return This object.
     */
    public CheckstyleExecutorRequest setAggregate( boolean pAggregate )
    {
        this.aggregate = pAggregate;
        return this;
    }

    /**
     * Returns the list of reactor projects.
     *
     * @return The reactor projects.
     */
    public List<MavenProject> getReactorProjects()
    {
        return reactorProjects;
    }

    /**
     * Sets the list of reactor projects.
     *
     * @param pReactorProjects The reactor projects.
     * @return This object.
     */
    public CheckstyleExecutorRequest setReactorProjects( List<MavenProject> pReactorProjects )
    {
        this.reactorProjects = pReactorProjects;
        return this;
    }
}
