/*
 * Copyright 2001-2007 The Apache Software Foundation.
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
package org.apache.maven.plugin.clover;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.clover.internal.AbstractCloverMojo;
import org.apache.maven.plugin.clover.internal.CloverConfiguration;
import org.apache.maven.plugin.clover.internal.instrumentation.MainInstrumenter;
import org.apache.maven.plugin.clover.internal.instrumentation.TestInstrumenter;

import java.io.File;
import java.util.*;

/**
 * Instrument source roots.
 *
 * <p><b>Note 1: Do not call this MOJO directly. It is meant to be called in a custom forked lifecycle by the other
 * Clover plugin MOJOs.</b></p>
 * <p><b>Note 2: We bind this mojo to the "validate" phase so that it executes prior to any other mojos</b></p>
 *
 * @goal instrumentInternal
 * @phase validate
 * @requiresDependencyResolution test
 *
 * @version $Id$
 */
public class CloverInstrumentInternalMojo extends AbstractCloverMojo implements CloverConfiguration
{
    /**
     * The directory where the Clover plugin will put all the files it generates during the build process. For
     * example the Clover plugin will put instrumented sources somewhere inside this directory.
     *  
     * @parameter
     * @required
     */
    private String cloverOutputDirectory;

    /**
     * List of all artifacts for this Clover plugin provided by Maven. This is used internally to get a handle on
     * the Clover JAR artifact.
     *
     * <p>Note: This is passed by Maven and must not be configured by the user.</p>
     *
     * @parameter expression="${plugin.artifacts}"
     * @required
     */
    private List pluginArtifacts;

    /**
     * @parameter expression="${component.org.apache.maven.artifact.factory.ArtifactFactory}"
     * @required
     * @readonly
     */
    private ArtifactFactory artifactFactory;

    /**
     * Artifact resolver used to find clovered artifacts (artifacts with a clover classifier).
     *
     * @component role="org.apache.maven.artifact.resolver.ArtifactResolver"
     * @required
     * @readonly
     */
    private ArtifactResolver artifactResolver;

    /**
     * Local maven repository.
     *
     * @parameter expression="${localRepository}"
     * @required
     */
    private ArtifactRepository localRepository;

    /**
     * The list of file to include in the instrumentation.
     * @parameter
     */
    private Set includes = new HashSet();

    /**
     * The list of file to exclude from the instrumentation.
     * @parameter
     */
    private Set excludes = new HashSet();

    /**
     * Whether the Clover plugin should instrument all source roots (ie even
     * generated sources) or whether it should only instrument the main source
     * root.
     * @parameter default-value="false"
     */
    private boolean includesAllSourceRoots;

    /**
     * Whether the Clover plugin should instrument test source roots.
     * @parameter default-value="false"
     */
    private boolean includesTestSourceRoots;

    /**
     * {@inheritDoc}
     * @see org.apache.maven.plugin.clover.internal.AbstractCloverMojo#execute()
     */
    public void execute()
        throws MojoExecutionException
    {
        // Ensure output directories exist
        new File( this.cloverOutputDirectory ).mkdirs();
        String cloverOutputSourceDirectory = new File( this.cloverOutputDirectory, "src" ).getPath();
        String cloverOutputTestSourceDirectory = new File( this.cloverOutputDirectory, "src-test" ).getPath();
        new File( getCloverDatabase() ).getParentFile().mkdirs();

        super.execute();

        logArtifacts( "before changes" );

        // Instrument both the main sources and the test sources if the user has configured it
        MainInstrumenter mainInstrumenter =
            new MainInstrumenter( this, cloverOutputSourceDirectory );
        TestInstrumenter testInstrumenter =
            new TestInstrumenter( this, cloverOutputTestSourceDirectory );

        if ( isJavaProject() )
        {
            mainInstrumenter.instrument();
            if ( this.includesTestSourceRoots )
            {
                testInstrumenter.instrument();
            }
        }

        swizzleCloverDependencies();
        addCloverDependencyToCompileClasspath();

        // Modify Maven model so that it points to the new source directories and to the clovered
        // artifacts instead of the original values.
        mainInstrumenter.redirectSourceDirectories();
        if ( this.includesTestSourceRoots )
        {
            testInstrumenter.redirectSourceDirectories();
        }
        redirectOutputDirectories();
        redirectArtifact();

        logArtifacts( "after changes" );
    }

    private boolean isJavaProject()
    {
        boolean isJavaProject;

        ArtifactHandler artifactHandler = getProject().getArtifact().getArtifactHandler();

        if ( "java".equals( artifactHandler.getLanguage() ) )
        {
            isJavaProject = true;
        }
        else
        {
            getLog().info( "Not instrumenting sources with Clover as this is not a Java project." );
            isJavaProject = false;
        }

        return isJavaProject;
    }

    private void redirectOutputDirectories()
    {
        // Explicitely set the output directory to be the Clover one so that all other plugins executing
        // thereafter output files in the Clover output directory and not in the main output directory.
        getProject().getBuild().setDirectory( this.cloverOutputDirectory );

        // TODO: Ugly hack below. Changing the directory should be enough for changing the values of all other
        // properties depending on it!
        getProject().getBuild().setOutputDirectory( new File( this.cloverOutputDirectory, "classes" ).getPath() );

        // TODO: This is a hack. Remove this when http://jira.codehaus.org/browse/MINSTALL-18 is fixed.
        new File( getProject().getBuild().getOutputDirectory() ).mkdirs();

        getProject().getBuild().setTestOutputDirectory(
            new File( this.cloverOutputDirectory, "test-classes" ).getPath() );
    }

    /**
     * Modify main artifact to add a "clover" classifier to it so that it's not mixed with the main artifact of
     * a normal build.
     */
    private void redirectArtifact()
    {
        // Only redirect main artifact for non-pom projects
        if ( !getProject().getPackaging().equals( "pom" ) )
        {
            Artifact oldArtifact = getProject().getArtifact();
            Artifact newArtifact = this.artifactFactory.createArtifactWithClassifier( oldArtifact.getGroupId(),
                oldArtifact.getArtifactId(), oldArtifact.getVersion(), oldArtifact.getType(), "clover" );
            getProject().setArtifact( newArtifact );

            getProject().getBuild().setFinalName( getProject().getArtifactId() + "-" + getProject().getVersion()
                + "-clover" );
        }
    }

    /**
     * Browse through all project dependencies and try to find a clovered version of the dependency. If found
     * replace the main depedencency by the clovered version.
     */
    private void swizzleCloverDependencies()
    {
        getProject().setDependencyArtifacts(
            swizzleCloverDependencies( getProject().getDependencyArtifacts() ) );
        getProject().setArtifacts(
            swizzleCloverDependencies( getProject().getArtifacts() ) );
    }

    protected Set swizzleCloverDependencies( Set artifacts )
    {
        Set resolvedArtifacts = new HashSet();
        for ( Iterator i = artifacts.iterator(); i.hasNext(); )
        {
            Artifact artifact = (Artifact) i.next();

            // Do not try to find Clovered versions for artifacts with classifiers. This is because Maven2 only
            // supports a single classifier per artifact and thus if we replace the original classifier with
            // a Clover classifier the artifact will fail to perform properly as intended originally. This is a
            // limitation.
            if ( artifact.getClassifier() == null )
            {
                Artifact cloveredArtifact = this.artifactFactory.createArtifactWithClassifier( artifact.getGroupId(),
                    artifact.getArtifactId(), artifact.getVersion(), artifact.getType(), "clover" );

                // Try to resolve the artifact with a clover classifier. If it doesn't exist, simply add the original
                // artifact. If found, use the clovered artifact.
                try
                {
                    this.artifactResolver.resolve( cloveredArtifact, new ArrayList(), localRepository );

                    // Set the same scope as the main artifact as this is not set by createArtifactWithClassifier.
                    cloveredArtifact.setScope( artifact.getScope() );

                    // Check the timestamp of the artifact. If the found clovered version is older than the
                    // non-clovered one we need to use the non-clovered version. This is to handle use case such as:
                    // - Say you have a module B that depends on a module A
                    // - You run Clover on A
                    // - You make modifications on A such that B would fail if not built with the latest version of A
                    // - You try to run the Clover plugin on B. The build would fail if we didn't pick the latest
                    //   version between the original A version and the clovered version.
                    if ( cloveredArtifact.getFile().lastModified() < artifact.getFile().lastModified() )
                    {
                        getLog().warn( "Using [" + artifact.getId() + "] even though a Clovered version exists "
                            + "but it's older and could fail the build. Please consider running Clover again on that "
                            + "dependency's project." );
                        resolvedArtifacts.add( artifact );
                    }
                    else
                    {
                        resolvedArtifacts.add( cloveredArtifact );
                    }
                }
                catch ( ArtifactResolutionException e )
                {
                    resolvedArtifacts.add( artifact );
                }
                catch ( ArtifactNotFoundException e )
                {
                    resolvedArtifacts.add( artifact );
                }
            }
            else
            {
                resolvedArtifacts.add( artifact );
            }
        }

        return resolvedArtifacts;
    }

    protected Artifact findCloverArtifact( List pluginArtifacts )
    {
        Artifact cloverArtifact = null;
        Iterator artifacts = pluginArtifacts.iterator();
        while ( artifacts.hasNext() && cloverArtifact == null )
        {
            Artifact artifact = (Artifact) artifacts.next();

            // We identify the clover JAR by checking the groupId and artifactId.
            if ( "com.cenqua.clover".equals( artifact.getGroupId() )
                && "clover".equals( artifact.getArtifactId() ) )
            {
                cloverArtifact = artifact;
            }
        }
        return cloverArtifact;
    }

    private void addCloverDependencyToCompileClasspath()
        throws MojoExecutionException
    {
        Artifact cloverArtifact = findCloverArtifact( this.pluginArtifacts );
        if ( cloverArtifact == null )
        {
            throw new MojoExecutionException(
                "Couldn't find [com.cenqua.cover:clover] artifact in plugin dependencies" );
        }

        cloverArtifact = artifactFactory.createArtifact( cloverArtifact.getGroupId(), cloverArtifact.getArtifactId(),
            cloverArtifact.getVersion(), Artifact.SCOPE_COMPILE, cloverArtifact.getType() );

        // TODO: use addArtifacts when it's implemented, see http://jira.codehaus.org/browse/MNG-2197
        Set set = new HashSet( getProject().getDependencyArtifacts() );
        set.add( cloverArtifact );
        getProject().setDependencyArtifacts( set );
    }

    private void logArtifacts( String message )
    {
        if ( getLog().isDebugEnabled() )
        {
            getLog().debug( "[Clover] List of dependency artifacts " + message + ":" );
            logArtifacts( getProject().getDependencyArtifacts() );

            getLog().debug( "[Clover] List of artifacts " + message + ":" );
            logArtifacts( getProject().getArtifacts() );
        }
    }

    private void logArtifacts( Set artifacts )
    {
        for ( Iterator i = artifacts.iterator(); i.hasNext(); )
        {
            Artifact artifact = (Artifact) i.next();
            getLog().debug( "[Clover]   Artifact [" + artifact.getId() + "], scope = [" + artifact.getScope() + "]" );
        }
    }

    protected void setArtifactFactory( ArtifactFactory artifactFactory )
    {
        this.artifactFactory = artifactFactory;
    }

    protected void setArtifactResolver( ArtifactResolver artifactResolver )
    {
        this.artifactResolver = artifactResolver;
    }

    public Set getIncludes()
    {
        return this.includes;
    }

    public Set getExcludes()
    {
        return this.excludes;
    }

    public boolean includesAllSourceRoots()
    {
        return this.includesAllSourceRoots;
    }
}
