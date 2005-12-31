package org.apache.maven.plugin.eclipse;

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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Resource;
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
public class EclipseUtils
{

    private EclipseUtils()
    {
        // don't instantiate
    }

    public static String toRelativeAndFixSeparator( File basedir, File fileToAdd, boolean replaceSlashesWithDashes )
        throws MojoExecutionException
    {
        String basedirpath;
        String absolutePath;

        try
        {
            basedirpath = basedir.getCanonicalPath();
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( Messages.getString( "EclipsePlugin.cantcanonicalize", basedir
                .getAbsolutePath() ), e );
        }

        try
        {
            absolutePath = fileToAdd.getCanonicalPath();
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( Messages.getString( "EclipsePlugin.cantcanonicalize", fileToAdd
                .getAbsolutePath() ), e );
        }

        String relative;

        if ( absolutePath.equals( basedirpath ) )
        {
            relative = ".";
        }
        else if ( absolutePath.startsWith( basedirpath ) )
        {
            relative = absolutePath.substring( basedirpath.length() + 1 );
        }
        else
        {
            relative = absolutePath;
        }

        relative = StringUtils.replace( relative, "\\", "/" ); //$NON-NLS-1$ //$NON-NLS-2$

        if ( replaceSlashesWithDashes )
        {
            relative = StringUtils.replace( relative, "/", "-" ); //$NON-NLS-1$ //$NON-NLS-2$
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

    public static EclipseSourceDir[] buildDirectoryList( MavenProject project, File basedir, Log log,
                                                        File buildOutputDirectory )
        throws MojoExecutionException
    {
        File projectBaseDir = project.getFile().getParentFile();

        // avoid duplicated entries
        Set directories = new TreeSet();

        EclipseUtils.extractSourceDirs( directories, project.getCompileSourceRoots(), basedir, projectBaseDir, false,
                                        null );

        EclipseUtils.extractResourceDirs( directories, project.getBuild().getResources(), project, basedir,
                                          projectBaseDir, false, null, log );

        // If using the standard output location, don't mix the test output into
        // it.
        String testOutput = null;
        boolean useFixedOutputDir = !buildOutputDirectory.equals( new File( project.getBuild().getOutputDirectory() ) );
        if ( !useFixedOutputDir )
        {
            testOutput = EclipseUtils.toRelativeAndFixSeparator( projectBaseDir, new File( project.getBuild()
                .getTestOutputDirectory() ), false );
        }

        EclipseUtils.extractSourceDirs( directories, project.getTestCompileSourceRoots(), basedir, projectBaseDir,
                                        true, testOutput );

        EclipseUtils.extractResourceDirs( directories, project.getBuild().getTestResources(), project, basedir,
                                          projectBaseDir, true, testOutput, log );

        return (EclipseSourceDir[]) directories.toArray( new EclipseSourceDir[directories.size()] );
    }

    private static void extractSourceDirs( Set directories, List sourceRoots, File basedir, File projectBaseDir,
                                          boolean test, String output )
        throws MojoExecutionException
    {
        for ( Iterator it = sourceRoots.iterator(); it.hasNext(); )
        {

            File sourceRootFile = new File( (String) it.next() );

            if ( sourceRootFile.isDirectory() )
            {
                String sourceRoot = EclipseUtils.toRelativeAndFixSeparator( projectBaseDir, sourceRootFile,
                                                                            !projectBaseDir.equals( basedir ) );

                directories.add( new EclipseSourceDir( sourceRoot, output, test, null, null ) );
            }
        }
    }

    private static void extractResourceDirs( Set directories, List resources, MavenProject project, File basedir,
                                            File projectBaseDir, boolean test, String output, Log log )
        throws MojoExecutionException
    {
        for ( Iterator it = resources.iterator(); it.hasNext(); )
        {
            Resource resource = (Resource) it.next();
            String includePattern = null;
            String excludePattern = null;

            if ( resource.getIncludes().size() != 0 )
            {
                // @todo includePattern = ?
                log.warn( Messages.getString( "EclipsePlugin.includenotsupported" ) ); //$NON-NLS-1$
            }

            if ( resource.getExcludes().size() != 0 )
            {
                // @todo excludePattern = ?
                log.warn( Messages.getString( "EclipsePlugin.excludenotsupported" ) ); //$NON-NLS-1$
            }

            // Example of setting include/exclude patterns for future reference.
            //
            // TODO: figure out how to merge if the same dir is specified twice
            // with different in/exclude patterns. We can't write them now,
            // since only the the first one would be included.
            //
            // if ( resource.getIncludes().size() != 0 )
            // {
            // writer.addAttribute(
            // "including", StringUtils.join( resource.getIncludes().iterator(),
            // "|" )
            // );
            // }
            //
            // if ( resource.getExcludes().size() != 0 )
            // {
            // writer.addAttribute(
            // "excluding", StringUtils.join( resource.getExcludes().iterator(),
            // "|" )
            // );
            // }

            if ( !StringUtils.isEmpty( resource.getTargetPath() ) )
            {
                output = resource.getTargetPath();
            }

            File resourceDirectory = new File( resource.getDirectory() );

            if ( !resourceDirectory.exists() || !resourceDirectory.isDirectory() )
            {
                continue;
            }

            String resourceDir = EclipseUtils.toRelativeAndFixSeparator( projectBaseDir, resourceDirectory,
                                                                         !projectBaseDir.equals( basedir ) );

            if ( output != null )
            {
                File outputFile = new File( projectBaseDir, output );
                // create output dir if it doesn't exist
                outputFile.mkdirs();
                output = EclipseUtils.toRelativeAndFixSeparator( projectBaseDir, outputFile, false );
            }

            directories.add( new EclipseSourceDir( resourceDir, output, test, includePattern, excludePattern ) );
        }
    }

    /**
     * Utility method that locates a project producing the given artifact.
     * 
     * @param reactorProjects
     *            a list of projects to search.
     * @param artifact
     *            the artifact a project should produce.
     * @return null or the first project found producing the artifact.
     */
    public static MavenProject findReactorProject( List reactorProjects, Artifact artifact )
    {
        if ( reactorProjects == null )
        {
            return null; // we're a single project
        }

        for ( Iterator it = reactorProjects.iterator(); it.hasNext(); )
        {
            MavenProject project = (MavenProject) it.next();

            if ( project.getGroupId().equals( artifact.getGroupId() )
                && project.getArtifactId().equals( artifact.getArtifactId() )
                && project.getVersion().equals( artifact.getVersion() ) )
            {
                return project;
            }
        }

        return null;
    }

    /**
     * Returns the list of referenced artifacts produced by reactor projects.
     * 
     * @return List of Artifacts
     */
    public static List resolveReactorArtifacts( MavenProject project, List reactorProjects )
    {
        List referencedProjects = new ArrayList();

        Set artifacts = project.getArtifacts();

        for ( Iterator it = artifacts.iterator(); it.hasNext(); )
        {
            Artifact artifact = (Artifact) it.next();

            MavenProject refProject = EclipseUtils.findReactorProject( reactorProjects, artifact );

            if ( refProject != null )
            {
                referencedProjects.add( artifact );
            }
        }

        return referencedProjects;
    }

    /**
     * @todo MNG-1379 Wrong path for artifacts with system scope
     * Artifacts with a system scope have a wrong path in mvn 2.0.
     * This is fixed in mvn 2.0.1 but this method is needed for compatibility with the 2.0 release. Do not remove!
     */
    public static void fixSystemScopeArtifacts( Collection artifacts, Collection dependencies )
    {
        // fix path for system dependencies.Artifact.getFile() returns a wrong path in mvn 2.0
        for ( Iterator iter = artifacts.iterator(); iter.hasNext(); )
        {
            Artifact artifact = (Artifact) iter.next();
            if ( Artifact.SCOPE_SYSTEM.equals( artifact.getScope() ) )
            {
                String groupid = artifact.getGroupId();
                String artifactId = artifact.getArtifactId();

                for ( Iterator depIt = dependencies.iterator(); depIt.hasNext(); )
                {
                    Dependency dep = (Dependency) depIt.next();
                    if ( Artifact.SCOPE_SYSTEM.equals( dep.getScope() ) && groupid.equals( dep.getGroupId() )
                        && artifactId.equals( dep.getArtifactId() ) )
                    {
                        artifact.setFile( new File( dep.getSystemPath() ) );
                        break;
                    }
                }
            }
        }
    }

    /**
     * @todo MNG-1384 optional dependencies not resolved while compiling from a
     *       master project Direct optional artifacts are not included in the
     *       list returned by project.getTestArtifacts() .classpath should
     *       include ANY direct dependency, and optional dependencies are
     *       required to compile This is fixed in mvn 2.0.1 but this method is
     *       needed for compatibility with the 2.0 release. Do not remove!
     */
    public static void fixMissingOptionalArtifacts( Collection artifacts, Collection depArtifacts,
                                                   ArtifactRepository localRepository,
                                                   ArtifactResolver artifactResolver, List remoteArtifactRepositories,
                                                   Log log )
    {
        for ( Iterator it = depArtifacts.iterator(); it.hasNext(); )
        {
            Artifact artifact = (Artifact) it.next();
            if ( artifact.isOptional() && !artifacts.contains( artifact ) )
            {
                try
                {
                    artifactResolver.resolve( artifact, remoteArtifactRepositories, localRepository );
                }
                catch ( ArtifactResolutionException e )
                {
                    log.error( "Unable to resolve optional artifact " + artifact.getId() );
                    continue;
                }
                catch ( ArtifactNotFoundException e )
                {
                    log.error( "Unable to resolve optional artifact " + artifact.getId() );
                    continue;
                }
                artifacts.add( artifact );
            }
        }
    }

    public static Artifact resolveLocalSourceArtifact( Artifact artifact, ArtifactRepository localRepository,
                                                      ArtifactResolver artifactResolver, ArtifactFactory artifactFactory )
        throws MojoExecutionException
    {
        return resolveArtifactWithClassifier( artifact, "sources", localRepository, artifactResolver, artifactFactory,
                                              new ArrayList( 0 ) );
    }

    public static Artifact resolveLocalJavadocArtifact( Artifact artifact, ArtifactRepository localRepository,
                                                       ArtifactResolver artifactResolver,
                                                       ArtifactFactory artifactFactory )
        throws MojoExecutionException
    {
        return resolveArtifactWithClassifier( artifact, "javadoc", localRepository, artifactResolver, artifactFactory,
                                              new ArrayList( 0 ) );
    }

    public static Artifact resolveArtifactWithClassifier( Artifact artifact, String classifier,
                                                         ArtifactRepository localRepository,
                                                         ArtifactResolver artifactResolver,
                                                         ArtifactFactory artifactFactory, List remoteRepos )
        throws MojoExecutionException
    {
        String type = classifier;

        // the "sources" classifier maps to the "java-source" type
        if ( "sources".equals( type ) )
        {
            type = "java-source";
        }

        Artifact resolvedArtifact = artifactFactory.createArtifactWithClassifier( artifact.getGroupId(), artifact
            .getArtifactId(), artifact.getVersion(), type, classifier );

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
            String message = Messages.getString( "EclipsePlugin.errorresolving", //$NON-NLS-1$
                                                 new Object[] { classifier, resolvedArtifact.getId(), e.getMessage() } );

            throw new MojoExecutionException( message, e );
        }

        return resolvedArtifact;
    }

    /**
     * Extracts the version of the first matching dependencyin the given list.
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
