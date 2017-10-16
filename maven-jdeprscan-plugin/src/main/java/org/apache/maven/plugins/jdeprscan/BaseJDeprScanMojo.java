package org.apache.maven.plugins.jdeprscan;

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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.Commandline;

/**
 * Base class for all explicit jdeprscan mojos
 * 
 * @author Robert Scholte
 * @since 3.0.0
 */
public abstract class BaseJDeprScanMojo extends AbstractJDeprScanMojo
{
    @Parameter( defaultValue = "${project}", readonly = true, required = true )
    private MavenProject project;

    /**
     * Indicates whether the build will continue even if there are jdeprscan warnings.
     */
    @Parameter( defaultValue = "true" )
    private boolean failOnWarning;
    
    /**
     * Limits scanning or listing to APIs that are deprecated for removal. 
     * Can’t be used with a release value of 6, 7, or 8.
     */
    @Parameter( property = "maven.jdeprscan.forremoval" )
    private boolean forRemoval;

    /**
     * Specifies the Java SE release that provides the set of deprecated APIs for scanning.
     */
    @Parameter
    private String release;
    
    @Override
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( !Files.exists( getClassesDirectory() ) )
        {
            getLog().debug( "No classes to analyze" );
            return;
        }
        super.execute();
    }

    protected MavenProject getProject()
    {
        return project;
    }
    
    @Override
    protected boolean isForRemoval()
    {
        return forRemoval;
    }
    
    @Override
    protected final void addJDeprScanOptions( Commandline cmd ) throws MojoFailureException
    {
        super.addJDeprScanOptions( cmd );

        if ( release != null )
        {
            cmd.createArg().setValue( "--release" );

            cmd.createArg().setValue( release );
        }

        try
        {
            Collection<Path> cp = getClassPath();
            
            if ( !cp.isEmpty() )
            {
                cmd.createArg().setValue( "--class-path" );

                cmd.createArg().setValue( StringUtils.join( cp.iterator(), File.pathSeparator ) );
            }
            
        }
        catch ( DependencyResolutionRequiredException e )
        {
            throw new MojoFailureException( e.getMessage(), e );
        }
        
        cmd.createArg().setFile( getClassesDirectory().toFile() );
    }
    

    protected abstract Path getClassesDirectory();
    
    protected abstract Collection<Path> getClassPath() throws DependencyResolutionRequiredException;
}
