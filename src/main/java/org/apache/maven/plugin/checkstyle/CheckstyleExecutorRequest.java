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
package org.apache.maven.plugin.checkstyle;

import java.io.ByteArrayOutputStream;
import java.io.File;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import com.puppycrawl.tools.checkstyle.DefaultLogger;
import com.puppycrawl.tools.checkstyle.api.AuditListener;
import com.puppycrawl.tools.checkstyle.api.Configuration;

/**
 * @author <a href="mailto:olamy@apache.org">olamy</a>
 * @since 2.5
 * @version $Id$
 */
public class CheckstyleExecutorRequest
{
    private Configuration configuration;
    
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
    
    public CheckstyleExecutorRequest(Configuration configuration)
    {
        this.configuration = configuration;
    }

    public Configuration getConfiguration()
    {
        return configuration;
    }

    public CheckstyleExecutorRequest setConfiguration( Configuration configuration )
    {
        this.configuration = configuration;
        return this;
    }

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
    
    public CheckstyleExecutorRequest setConsoleListener(DefaultLogger defaultLogger)
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
    
}
