package org.apache.maven.plugin.compiler;

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
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.project.MavenProject;

/**
 * MavenProject should be clean from language specific properties.
 * ClassPath is Java specific, so that kind of code should be moved to here.
 * Other projects should simply use the Artifacts instead
 * 
 * @author Robert Scholte
 * @since 3.6
 */
public final class JavaMavenProjectUtils
{
    private JavaMavenProjectUtils()
    {
    }

    public static List<String> getCompileClasspathElements( MavenProject project, File outputDirectory )
        throws DependencyResolutionRequiredException
    {
        List<String> list = new ArrayList<String>( project.getArtifacts().size() + 1 );

        // Would be nice if this one wasn't required
        list.add( outputDirectory.getAbsolutePath() );

        for ( Artifact a : project.getArtifacts() )
        {
            if ( a.getArtifactHandler().isAddedToClasspath() )
            {
                // TODO: let the scope handler deal with this
                if ( Artifact.SCOPE_COMPILE.equals( a.getScope() ) || Artifact.SCOPE_PROVIDED.equals( a.getScope() )
                    || Artifact.SCOPE_SYSTEM.equals( a.getScope() ) )
                {
                    addArtifactPath( a, list );
                }
            }
        }

        return list;
    }
    
    //TODO: this checking for file == null happens because the resolver has been confused about the root
    // artifact or not. things like the stupid dummy artifact coming from surefire.
    public static List<String> getTestClasspathElements( MavenProject project, File outputDirectory )
        throws DependencyResolutionRequiredException
    {
        List<String> list = new ArrayList<String>( project.getArtifacts().size() + 2 );

        // Would be nice if this one wasn't required
        list.add( outputDirectory.getAbsolutePath() );

        list.add( project.getBuild().getOutputDirectory() );
        
        for ( Artifact a : project.getArtifacts() )
        {            
            if ( a.getArtifactHandler().isAddedToClasspath() )
            {                
                addArtifactPath( a, list );
            }
        }

        return list;
    }

    private static void addArtifactPath( Artifact artifact, List<String> classpath )
    {
        File file = artifact.getFile();
        if ( file != null )
        {
            classpath.add( file.getPath() );
        }
    }
}
