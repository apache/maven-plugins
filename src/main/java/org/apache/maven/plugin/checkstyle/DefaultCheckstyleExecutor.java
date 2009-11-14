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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.codehaus.plexus.resource.ResourceManager;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;

import com.puppycrawl.tools.checkstyle.Checker;
import com.puppycrawl.tools.checkstyle.api.AuditListener;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.api.FilterSet;
import com.puppycrawl.tools.checkstyle.filters.SuppressionsLoader;

/**
 * @author <a href="mailto:olamy@apache.org">olamy</a>
 * @plexus.component role="org.apache.maven.plugin.checkstyle.CheckstyleExecutor" role-hint="default"
 * @since 2.5
 * @version $Id$
 */
public class DefaultCheckstyleExecutor
    implements CheckstyleExecutor
{
    
    /**
     * @plexus.requirement role="org.codehaus.plexus.resource.ResourceManager" role-hint="default"
     */
    private ResourceManager locator;    
    
    private static final File[] EMPTY_FILE_ARRAY = new File[0];

    public CheckstyleResults executeCheckstyle( CheckstyleExecutorRequest request )
        throws CheckstyleExecutorException, CheckstyleException
    {
        File[] files;
        try
        {
            files = getFilesToProcess( request );
        }
        catch ( IOException e )
        {
            throw new CheckstyleExecutorException( "Error getting files to process", e );
        }

        FilterSet filterSet = getSuppressions( request );

        Checker checker = new Checker();

        // setup classloader, needed to avoid "Unable to get class information
        // for ..." errors
        List classPathStrings = new ArrayList();
        List outputDirectories = new ArrayList();
        try
        {
            classPathStrings = request.getProject().getCompileClasspathElements();
            outputDirectories.add( request.getProject().getBuild().getOutputDirectory() );

            if ( request.isIncludeTestSourceDirectory() && ( request.getSourceDirectory() != null )
                && ( request.getTestSourceDirectory().exists() ) && ( request.getTestSourceDirectory().isDirectory() ) )
            {
                classPathStrings = request.getProject().getTestClasspathElements();
                outputDirectories.add( request.getProject().getBuild().getTestOutputDirectory() );
            }
        }
        catch ( DependencyResolutionRequiredException e )
        {
            throw new CheckstyleExecutorException( e.getMessage(), e );
        }

        if (classPathStrings == null)
        {
            classPathStrings = Collections.EMPTY_LIST;
        }
        
        List urls = new ArrayList( classPathStrings.size() );

        Iterator iter = classPathStrings.iterator();
        while ( iter.hasNext() )
        {
            try
            {
                urls.add( new File( ( (String) iter.next() ) ).toURL() );
            }
            catch ( MalformedURLException e )
            {
                throw new CheckstyleExecutorException( e.getMessage(), e );
            }
        }

        Iterator iterator = outputDirectories.iterator();
        while ( iterator.hasNext() )
        {
            try
            {
                String outputDirectoryString = (String) iterator.next();
                if ( outputDirectoryString != null )
                {
                    File outputDirectoryFile = new File( outputDirectoryString );
                    if ( outputDirectoryFile.exists() )
                    {
                        URL outputDirectoryUrl = outputDirectoryFile.toURL();
                        request.getLog().debug(
                                                "Adding the outputDirectory " + outputDirectoryUrl.toString()
                                                    + " to the Checkstyle class path" );
                        urls.add( outputDirectoryUrl );
                    }
                }
            }
            catch ( MalformedURLException e )
            {
                throw new CheckstyleExecutorException( e.getMessage(), e );
            }
        }

        URLClassLoader projectClassLoader = new URLClassLoader( (URL[]) urls.toArray( new URL[urls.size()] ), null );
        checker.setClassloader( projectClassLoader );

        checker.setModuleClassLoader( Thread.currentThread().getContextClassLoader() );

        if ( filterSet != null )
        {
            checker.addFilter( filterSet );
        }

        checker.configure( request.getConfiguration() );

        AuditListener listener = request.getListener();

        if ( listener != null )
        {
            checker.addListener( listener );
        }

        if ( request.isConsoleOutput() )
        {
            checker.addListener( request.getConsoleListener() );
        }

        CheckstyleReportListener sinkListener = new CheckstyleReportListener( request.getSourceDirectory() );
        if ( request.isIncludeTestSourceDirectory() && ( request.getTestSourceDirectory() != null )
            && ( request.getTestSourceDirectory().exists() ) && ( request.getTestSourceDirectory().isDirectory() ) )
        {
            sinkListener.addSourceDirectory( request.getTestSourceDirectory() );
        }

        checker.addListener( sinkListener );

        ArrayList filesList = new ArrayList();
        for ( int i = 0; i < files.length; i++ )
        {
            filesList.add( files[i] );
        }
        int nbErrors = checker.process( filesList );

        checker.destroy();

        if ( request.getStringOutputStream() != null )
        {
            request.getLog().info( request.getStringOutputStream().toString() );
        }

        if ( request.isFailsOnError() && nbErrors > 0 )
        {
            // TODO: should be a failure, not an error. Report is not meant to
            // throw an exception here (so site would
            // work regardless of config), but should record this information
            throw new CheckstyleExecutorException( "There are " + nbErrors + " checkstyle errors." );
        }
        else if ( nbErrors > 0 )
        {
            request.getLog().info( "There are " + nbErrors + " checkstyle errors." );
        }

        return sinkListener.getResults();
    }

    private File[] getFilesToProcess( CheckstyleExecutorRequest request )
        throws IOException
    {
        StringBuffer excludesStr = new StringBuffer();

        if ( StringUtils.isNotEmpty( request.getExcludes() ) )
        {
            excludesStr.append( request.getExcludes() );
        }

        String[] defaultExcludes = FileUtils.getDefaultExcludes();
        for ( int i = 0; i < defaultExcludes.length; i++ )
        {
            if ( excludesStr.length() > 0 )
            {
                excludesStr.append( "," );
            }

            excludesStr.append( defaultExcludes[i] );
        }

        List files = FileUtils.getFiles( request.getSourceDirectory(), request.getIncludes(), excludesStr.toString() );
        if ( request.isIncludeTestSourceDirectory() && ( request.getTestSourceDirectory() != null )
            && ( request.getTestSourceDirectory().exists() ) && ( request.getTestSourceDirectory().isDirectory() ) )
        {
            files.addAll( FileUtils.getFiles( request.getTestSourceDirectory(), request.getIncludes(), excludesStr
                .toString() ) );
        }

        return (File[]) files.toArray( EMPTY_FILE_ARRAY );
    }

    private FilterSet getSuppressions( CheckstyleExecutorRequest request )
        throws CheckstyleExecutorException
    {
        try
        {
            File suppressionsFile = locator.resolveLocation( request.getSuppressionsLocation(),
                                                             "checkstyle-suppressions.xml" );

            if ( suppressionsFile == null )
            {
                return null;
            }

            return SuppressionsLoader.loadSuppressions( suppressionsFile.getAbsolutePath() );
        }
        catch ( CheckstyleException ce )
        {
            throw new CheckstyleExecutorException( "failed to load suppressions location: "
                + request.getSuppressionsLocation(), ce );
        }
        catch ( IOException e )
        {
            throw new CheckstyleExecutorException( "Failed to process supressions location: "
                + request.getSuppressionsLocation(), e );
        }
    }
}
