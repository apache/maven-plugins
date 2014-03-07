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
