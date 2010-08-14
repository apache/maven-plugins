package org.apache.maven.plugin.war.packaging;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.war.util.WebappStructure;

import java.io.File;

/**
 * Analyzes the dependencies of the project with its previous state and update
 * the target directory accordingly.
 *
 * @author Stephane Nicoll
 * 
 * @version $Id$
 */
public class DependenciesAnalysisPackagingTask
    extends AbstractWarPackagingTask
{

    public void performPackaging( final WarPackagingContext context )
        throws MojoExecutionException, MojoFailureException
    {

        context.getWebappStructure().analyseDependencies( new DependenciesAnalysisCallbackImpl( context ) );

    }

    protected void handleDependency( WarPackagingContext context, Dependency dependency, String notBundledMessage,
                                     String warOrZipMessage, String standardMessage, boolean removeFile )
    {
        if ( Artifact.SCOPE_PROVIDED.equals( dependency.getScope() )
            || Artifact.SCOPE_TEST.equals( dependency.getScope() ) || dependency.isOptional() )
        {
            context.getLog().debug( notBundledMessage );
        }
        else if ( "war".equals( dependency.getType() ) || "zip".equals( dependency.getType() ) )
        {
            context.getLog().warn( warOrZipMessage );
        }
        else if ( "tld".equals( dependency.getType() ) || "aar".equals( dependency.getType() )
            || "jar".equals( dependency.getType() ) || "ejb".equals( dependency.getType() )
            || "ejb-client".equals( dependency.getType() ) || "test-jar".equals( dependency.getType() )
            || "par".equals( dependency.getType() ) )
        {
            context.getLog().info( standardMessage );
            if ( removeFile )
            {
                removeDependency( context, dependency );
            }
        }
    }

    protected void handleDependencyScope( WarPackagingContext context, Dependency dependency, String warOrZipMessage,
                                          String standardMessage, boolean removeFile )
    {
        if ( "war".equals( dependency.getType() ) || "zip".equals( dependency.getType() ) )
        {
            context.getLog().warn( warOrZipMessage );
        }
        else if ( "tld".equals( dependency.getType() ) || "aar".equals( dependency.getType() )
            || "jar".equals( dependency.getType() ) || "ejb".equals( dependency.getType() )
            || "ejb-client".equals( dependency.getType() ) || "test-jar".equals( dependency.getType() )
            || "par".equals( dependency.getType() ) )
        {
            context.getLog().info( standardMessage );
            if ( removeFile )
            {
                removeDependency( context, dependency );
            }
        }
    }

    private void removeDependency( WarPackagingContext context, Dependency dependency )
    {
        final String targetFileName = context.getWebappStructure().getCachedTargetFileName( dependency );
        if ( targetFileName != null )
        {
            final String type = dependency.getType();
            File targetFile = null;
            if ( "tld".equals( type ) )
            {
                targetFile = new File( context.getWebappDirectory(), ArtifactsPackagingTask.TLD_PATH + targetFileName );
            }
            else if ( "aar".equals( type ) )
            {
                targetFile =
                    new File( context.getWebappDirectory(), ArtifactsPackagingTask.SERVICES_PATH + targetFileName );
            }
            else if ( "jar".equals( type ) || "ejb".equals( type ) || "ejb-client".equals( type )
                || "test-jar".equals( type ) )
            {
                targetFile = new File( context.getWebappDirectory(), LIB_PATH + targetFileName );
            }
            else if ( "par".equals( type ) )
            {
                String targetFileName2 = targetFileName.substring( 0, targetFileName.lastIndexOf( '.' ) ) + ".jar";
                targetFile = new File( context.getWebappDirectory(), LIB_PATH + targetFileName2 );
            }

            // now remove
            if ( targetFile == null )
            {
                context.getLog().error( "Could not get file from dependency [" + dependency + "]" );
            }
            else if ( targetFile.exists() )
            {
                context.getLog().debug( "Removing file [" + targetFile.getAbsolutePath() + "]" );
                targetFile.delete();
            }
            else
            {
                context.getLog().warn( "File to remove [" + targetFile.getAbsolutePath() + "] has not been found" );
            }
        }
        else
        {
            context.getLog().warn( "Could not retrieve the target file name of dependency [" + dependency + "]" );
        }
    }


    class DependenciesAnalysisCallbackImpl
        implements WebappStructure.DependenciesAnalysisCallback
    {
        private final WarPackagingContext context;

        DependenciesAnalysisCallbackImpl( WarPackagingContext context )
        {
            this.context = context;
        }

        public void unchangedDependency( Dependency dependency )
        {
            context.getLog().debug( "Dependency [" + dependency + "] has not changed since last build." );
        }

        public void newDependency( Dependency dependency )
        {
            context.getLog().debug( "New dependency [" + dependency + "]." );
        }

        public void removedDependency( Dependency dependency )
        {
            handleDependency( context, dependency, "Dependency [" + dependency
                + "] has been removed from the project but it was not bundled anyway.", "Dependency [" + dependency
                + "] has been removed from the project. If it was included in the build as an overlay, "
                + "consider cleaning the target directory of the project (mvn clean)", "Dependency [" + dependency
                + "] has been removed from the project.", true );
        }

        public void updatedVersion( Dependency dependency, String previousVersion )
        {
            handleDependency( context, dependency, "Version of dependency [" + dependency + "] has changed ("
                + previousVersion + " -> " + dependency.getVersion() + ") but it was not bundled anyway.",
                                                   "Version of dependency [" + dependency + "] has changed ("
                                                       + previousVersion + " -> " + dependency.getVersion()
                                                       + "). If it was included in the build as an overlay, "
                                                       + "consider "
                                                       + "cleaning the target directory of the project (mvn clean)",
                                                   "Version of dependency [" + dependency + "] has changed ("
                                                       + previousVersion + " -> " + dependency.getVersion() + ").",
                                                   true );
        }

        public void updatedScope( Dependency dependency, String previousScope )
        {
            if ( Artifact.SCOPE_PROVIDED.equals( dependency.getScope() ) || Artifact.SCOPE_TEST.equals(
                dependency.getScope() )
                && ( !Artifact.SCOPE_PROVIDED.equals( previousScope )
                && !Artifact.SCOPE_TEST.equals( previousScope ) ) )
            {
                // It's now provided or test so it should be removed
                handleDependencyScope( context, dependency, "Scope of dependency [" + dependency + "] has changed ("
                    + previousScope + " -> " + dependency.getScope()
                    + "). If it was included in the build as an overlay, "
                    + "consider cleaning the target directory of the project (mvn clean)", "Scope of dependency ["
                    + dependency + "] has changed (" + previousScope + " -> " + dependency.getScope() + ").", true );
            }

        }


        public void updatedOptionalFlag( Dependency dependency, boolean previousOptional )
        {
            if ( !previousOptional && dependency.isOptional() )
            {
                // It wasn't optional but now it is anymore
                handleDependency( context, dependency,
                                  "Dependency [" + dependency + "] is now optional but it was not bundled anyway.",
                                  "Dependency [" + dependency
                                      + "] is now optional. If it was included in the build as an overlay, "
                                      + "consider cleaning the target directory of the project (mvn clean)",
                                  "Dependency [" + dependency + "] is now optional", true );


            }
        }

        public void updatedUnknown( Dependency dependency, Dependency previousDep )
        {
            handleDependency( context, dependency, "Dependency [" + dependency + "] has changed (was " + previousDep
                + ") but it was not bundled anyway.", "Dependency [" + dependency + "] has changed (was " + previousDep
                + "). If it was included in the build as an overlay, " + "consider "
                + "cleaning the target directory of the project (mvn clean)", "Dependency [" + dependency
                + "] has changed (was " + previousDep + ").", true );
        }

    }

}
