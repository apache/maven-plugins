package org.apache.maven.plugin.dependency;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.    
 */

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Remove/purge artifacts from the local repository.
 * 
 * @version $Id: PurgeLocalRepositoryMojo.java 1400764 2012-10-22 05:31:26Z pgier $
 * @since 2.6
 */
@Mojo( name = "manual-purge-local-repository", threadSafe = true, requiresProject = false )
public class ManualPurgeLocalRepositoryMojo
    extends AbstractMojo
{

    /**
     * The local repository, from which to delete artifacts.
     */
    @Parameter( defaultValue = "${localRepository}", readonly = true, required = true )
    private ArtifactRepository localRepository;

    /**
     * The list of artifacts in the form of groupId:artifactId:version which should be deleted/purged from the local
     * repository. To delete all versions for a specific artifact, leave the version empty (groupId:artifactId).
     * To delete all artifacts for a specific groupId, just provide the groupId by itself.
     */
    @Parameter
    private List<String> includes;

    /**
     * Comma-separated list of groupId:artifactId entries, which should be used to manually include artifacts for
     * deletion. This is a command-line alternative to the <code>manualIncludes</code> parameter, since List parameters
     * are not currently compatible with CLI specification.
     */
    @Parameter( property = "include" )
    private String include;


    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( !StringUtils.isEmpty( include ) )
        {
            includes = this.parseIncludes( include );
        }

        if ( includes == null || includes.isEmpty() )
        {
            throw new MojoExecutionException( "Parameter manualIncludes must be specified" );
        }

        for ( String gavPattern : includes )
        {
            if ( StringUtils.isEmpty( gavPattern ) )
            {
                getLog().debug( "Skipping empty gav pattern: " + gavPattern );
                continue;
            }

            String relativePath = gavToPath( gavPattern );
            if ( StringUtils.isEmpty( relativePath ) )
            {
                continue;
            }

            File purgeDir = new File( localRepository.getBasedir(), relativePath );
            if ( purgeDir.exists() )
            {
                getLog().debug( "Deleting directory: " + purgeDir );
                try
                {
                    FileUtils.deleteDirectory( purgeDir );
                }
                catch ( IOException e )
                {
                    throw new MojoExecutionException( "Unable to purge directory: " + purgeDir );
                }
            }
        }
    }

    /**
     * Convert a groupId:artifactId:version to a file system path
     * 
     * @param gav, the groupId:artifactId:version string
     * @return
     */
    private String gavToPath( String gav )
    {
        if ( StringUtils.isEmpty( gav ) )
        {
            return null;
        }
        
        String[] pathComponents = gav.split( ":" );

        StringBuffer path = new StringBuffer( pathComponents[0].replace( '.', '/' ) );
        
        for ( int i=1; i<pathComponents.length; ++i )
        {
            path.append( "/" +  pathComponents[i] );
        }

        return path.toString();
    }

    /**
     * Convert comma separated list of includes to List object
     * 
     * @param include
     * @return the includes list
     */
    private List<String> parseIncludes( String include )
    {
        List<String> includes = new ArrayList<String>();

        if ( include != null )
        {
            String[] elements = include.split( "," );
            includes.addAll( Arrays.asList( elements ) );
        }

        return includes;
    }

}
