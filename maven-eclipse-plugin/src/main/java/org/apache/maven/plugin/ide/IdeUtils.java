package org.apache.maven.plugin.ide;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @author <a href="mailto:fgiust@users.sourceforge.net">Fabrizio Giustina</a>
 * @version $Id$
 */
public class IdeUtils
{

    private IdeUtils()
    {
        // don't instantiate
    }

    public static String getCanonicalPath( File file )
        throws MojoExecutionException
    {
        try
        {
            return file.getCanonicalPath();
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( Messages.getString( "cantcanonicalize", file //$NON-NLS-1$
                .getAbsolutePath() ), e );
        }
    }

    public static String toRelativeAndFixSeparator( File basedir, File fileToAdd, boolean replaceSlashesWithDashes )
        throws MojoExecutionException
    {
        String basedirpath;
        String absolutePath;

        basedirpath = getCanonicalPath( basedir );
        absolutePath = getCanonicalPath( fileToAdd );

        String relative;

        if ( absolutePath.equals( basedirpath ) )
        {
            relative = "."; //$NON-NLS-1$
        }
        else if ( absolutePath.startsWith( basedirpath ) )
        {
            relative = absolutePath.substring( basedirpath.length() + 1 );
        }
        else
        {
            relative = absolutePath;
        }

        relative = StringUtils.replace( relative, '\\', '/' );

        if ( replaceSlashesWithDashes )
        {
            relative = StringUtils.replace( relative, '/', '-' );
            relative = StringUtils.replace( relative, ':', '-' ); // remove ":" for absolute paths in windows
        }

        return relative;
    }

    /**
     * @todo there should be a better way to do this
     */
    public static String getPluginSetting( MavenProject project, String artifactId, String optionName,
                                           String defaultValue )
    {
        for ( Iterator it = project.getModel().getBuild().getPlugins().iterator(); it.hasNext(); )
        {
            Plugin plugin = (Plugin) it.next();

            if ( plugin.getArtifactId().equals( artifactId ) )
            {
                Xpp3Dom o = (Xpp3Dom) plugin.getConfiguration();

                if ( o != null && o.getChild( optionName ) != null )
                {
                    return o.getChild( optionName ).getValue();
                }
            }
        }

        return defaultValue;
    }

    public static Artifact resolveArtifactWithClassifier( String groupId, String artifactId, String version,
                                                          String classifier, ArtifactRepository localRepository,
                                                          ArtifactResolver artifactResolver,
                                                          ArtifactFactory artifactFactory, List remoteRepos, Log log )

    {
        String type = classifier;

        // the "sources" classifier maps to the "java-source" type
        if ( "sources".equals( type ) ) //$NON-NLS-1$
        {
            type = "java-source"; //$NON-NLS-1$
        }

        Artifact resolvedArtifact = artifactFactory.createArtifactWithClassifier( groupId, artifactId, version, type,
                                                                                  classifier );

        try
        {
            artifactResolver.resolve( resolvedArtifact, remoteRepos, localRepository );
        }
        catch ( ArtifactNotFoundException e )
        {
            // ignore, the jar has not been found
        }
        catch ( ArtifactResolutionException e )
        {
            String message = Messages.getString( "errorresolving", new Object[] { //$NON-NLS-1$
                                                 classifier, resolvedArtifact.getId(), e.getMessage() } );

            log.warn( message );
        }

        return resolvedArtifact;
    }

    /**
     * Extracts the version of the first matching dependency in the given list.
     * 
     * @param artifactNames artifact names to compare against for extracting version
     * @param artifacts Collection of dependencies for our project
     * @param len expected length of the version sub-string
     * @return
     */
    public static String getDependencyVersion( String[] artifactNames, Set artifacts, int len )
    {
        for ( Iterator itr = artifacts.iterator(); itr.hasNext(); )
        {
            Artifact artifact = (Artifact) itr.next();
            for ( int j = 0; j < artifactNames.length; j++ )
            {
                String name = artifactNames[j];
                if ( name.equals( artifact.getArtifactId() ) )
                {
                    return StringUtils.substring( artifact.getVersion(), 0, len );
                }
            }
        }
        return null;
    }
}
