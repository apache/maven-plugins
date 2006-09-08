/*
 * Copyright 2001-2006 The Apache Software Foundation.
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

import com.cenqua.clover.CloverInstr;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.clover.internal.AbstractCloverMojo;
import org.codehaus.plexus.compiler.util.scan.SimpleSourceInclusionScanner;
import org.codehaus.plexus.compiler.util.scan.SourceInclusionScanner;
import org.codehaus.plexus.compiler.util.scan.InclusionScanException;
import org.codehaus.plexus.compiler.util.scan.mapping.SuffixMapping;

import java.io.File;
import java.util.*;

/**
 * Instrument source roots.
 *
 * <p><b>Note: Do not call this MOJO directly. It is meant to be called in a custom forked lifecycle by the other
 * MOJOs.</b></p>
 *
 * @goal instrumentInternal
 * @phase generate-sources
 * @requiresDependencyResolution test
 *
 * @author <a href="mailto:vmassol@apache.org">Vincent Massol</a>
 * @version $Id$
 */
public class CloverInstrumentInternalMojo extends AbstractCloverMojo
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

    private String cloverOutputSourceDirectory;

    /**
     * {@inheritDoc}
     * @see org.apache.maven.plugin.clover.internal.AbstractCloverMojo#execute()
     */
    public void execute()
        throws MojoExecutionException
    {
        // Ensure output directories exist
        new File( this.cloverOutputDirectory ).mkdirs();
        this.cloverOutputSourceDirectory = new File( this.cloverOutputDirectory, "src" ).getPath();
        new File( getCloverDatabase() ).getParentFile().mkdirs();

        super.execute();

        logArtifacts("before changes");

        if ( isJavaProject() )
        {
            Set filesToInstrument = computeFilesToInstrument();
            if ( filesToInstrument.isEmpty() )
            {
                getLog().warn( "No Clover instrumentation done as no matching sources files found" );
            }
            else
            {
                instrumentSources( filesToInstrument );
            }
        }

        swizzleCloverDependencies();
        addCloverDependencyToCompileClasspath();
        redirectSourceDirectories();
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

    private void instrumentSources(Set filesToInstrument) throws MojoExecutionException
    {
        int result = CloverInstr.mainImpl( createCliArgs( filesToInstrument ) );
        if ( result != 0 )
        {
            throw new MojoExecutionException( "Clover has failed to instrument the source files" );
        }
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

    private void redirectSourceDirectories()
    {
        String oldSourceDirectory = getProject().getBuild().getSourceDirectory();

        if ( new File( oldSourceDirectory ).exists() )
        {
            getProject().getBuild().setSourceDirectory( this.cloverOutputSourceDirectory );
        }

        getLog().debug( "Clover source directories before change:" );
        logSourceDirectories();

        // Maven2 limitation: changing the source directory doesn't change the compile source roots
        // See http://jira.codehaus.org/browse/MNG-1945
        List sourceRoots = new ArrayList( getProject().getCompileSourceRoots() );

        // Clean all source roots to add them again in order to keep the same original order of source roots.
        getProject().getCompileSourceRoots().removeAll( sourceRoots );

        for ( Iterator i = sourceRoots.iterator(); i.hasNext(); )
        {
            String sourceRoot = (String) i.next();
            if ( new File( oldSourceDirectory ).exists() && sourceRoot.equals( oldSourceDirectory ) )
            {
                getProject().addCompileSourceRoot( getProject().getBuild().getSourceDirectory() );
            }
            else
            {
                getProject().addCompileSourceRoot( sourceRoot );
            }
        }

        getLog().debug( "Clover source directories after change:" );
        logSourceDirectories();
    }

    /**
     * Modify main artifact to add a "clover" classifier to it so that it's not mixed with the main artifact of
     * a normal build.
     */
    private void redirectArtifact()
    {
        // Only redirect main artifact for non-pom projects
        if ( !getProject().getPackaging().equals("pom") )
        {
            Artifact oldArtifact = getProject().getArtifact();
            Artifact newArtifact = this.artifactFactory.createArtifactWithClassifier( oldArtifact.getGroupId(),
                oldArtifact.getArtifactId(), oldArtifact.getVersion(), oldArtifact.getType(), "clover" );
            getProject().setArtifact( newArtifact );

            getProject().getBuild().setFinalName( getProject().getArtifactId() + "-" + getProject().getVersion()
                + "-clover");
        }
    }

    private void logSourceDirectories()
    {
        if ( getLog().isDebugEnabled() )
        {
            for ( Iterator i = getProject().getCompileSourceRoots().iterator(); i.hasNext(); )
            {
                String sourceRoot = (String) i.next();
                getLog().debug( "[Clover]  source root [" + sourceRoot + "]");
            }
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

    protected Set swizzleCloverDependencies(Set artifacts)
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
                    if (cloveredArtifact.getFile().lastModified() < artifact.getFile().lastModified())
                    {
                        getLog().warn("Using [" + artifact.getId() + "] even though a Clovered version exists "
                            + "but it's older and could fail the build. Please consider running Clover again on that "
                            + "dependency's project.");
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

    protected Artifact findCloverArtifact(List pluginArtifacts)
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
        Artifact cloverArtifact = findCloverArtifact(this.pluginArtifacts);
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

    private void logArtifacts(String message)
    {
        if ( getLog().isDebugEnabled() )
        {
            getLog().debug("[Clover] List of dependency artifacts " + message + ":");
            logArtifacts( getProject().getDependencyArtifacts() );

            getLog().debug("[Clover] List of artifacts " + message + ":");
            logArtifacts( getProject().getArtifacts() );
        }
    }

    private void logArtifacts(Set artifacts)
    {
        for ( Iterator i = artifacts.iterator(); i.hasNext(); )
        {
            Artifact artifact = (Artifact) i.next();
            getLog().debug("[Clover]   Artifact [" + artifact.getId() + "], scope = [" + artifact.getScope() + "]" );
        }
    }

    /**
     * @return a Plexus scanner object that scans a source root and filters files according to inclusion and
     * exclusion patterns. In our case at hand we include only Java sources as these are the only files we want
     * to instrument.
     */
    private SourceInclusionScanner getScanner()
    {
        SourceInclusionScanner scanner = null;

        if ( includes.isEmpty() && excludes.isEmpty() )
        {
            includes = Collections.singleton( "**/*.java" );
            scanner = new SimpleSourceInclusionScanner( includes, Collections.EMPTY_SET );
        }
        else
        {
            if ( includes.isEmpty() )
            {
                includes.add( "**/*.java" );
            }
            scanner = new SimpleSourceInclusionScanner( includes, excludes );
        }

        // Note: we shouldn't have to do this but this is a limitation of the Plexus SimpleSourceInclusionScanner
        scanner.addSourceMapping( new SuffixMapping( "dummy", "dummy" ) );

        return scanner;
    }

    /**
     * @return the list of files to instrument taking into account the includes and excludes specified by the user
     */
    private Set computeFilesToInstrument()
    {
        Set filesToInstrument = new HashSet();
        SourceInclusionScanner scanner = getScanner();
        
        // Decide whether to instrument all source roots or only the main source root.
        Iterator sourceRoots;
        if ( this.includesAllSourceRoots )
        {
            sourceRoots = getProject().getCompileSourceRoots().iterator();
        }
        else
        {
            sourceRoots = Collections.singletonList( getProject().getBuild().getSourceDirectory() ).iterator();
        }

        while ( sourceRoots.hasNext() )
        {
            File sourceRoot = new File( (String) sourceRoots.next() );
            if ( sourceRoot.exists() )
            {
                try
                {
                    filesToInstrument.addAll( scanner.getIncludedSources( sourceRoot, null ) );
                }
                catch (InclusionScanException e)
                {
                    getLog().warn( "Failed to add sources from [" + sourceRoot + "]", e);
                }
            }
        }

        return filesToInstrument;
    }

    /**
     * @return the CLI args to be passed to CloverInstr
     */
    private String[] createCliArgs(Set filesToInstrument) throws MojoExecutionException
    {
        List parameters = new ArrayList();

        parameters.add( "-p" );
        parameters.add( getFlushPolicy() );
        parameters.add( "-f" );
        parameters.add( "" + getFlushInterval() );

        parameters.add( "-i" );
        parameters.add( getCloverDatabase() );

        parameters.add( "-d" );
        parameters.add( this.cloverOutputSourceDirectory );

        if ( getLog().isDebugEnabled() )
        {
            parameters.add( "-v" );
        }

        if ( getJdk() != null )
        {
            if ( getJdk().equals( "1.4" ) )
            {
                parameters.add( "-jdk14" );
            }
            else if ( getJdk().equals( "1.5" ) )
            {
                parameters.add( "-jdk15" );
            }
            else
            {
                throw new MojoExecutionException("Unsupported jdk version [" + getJdk()
                    + "]. Valid values are [1.4] and [1.5]");
            }
        }

        for ( Iterator files = filesToInstrument.iterator(); files.hasNext(); )
        {
            File file = (File) files.next();
            parameters.add( file.getPath() );
        }

        // Log parameters
        if ( getLog().isDebugEnabled() )
        {
            getLog().debug("Parameter list being passed to Clover CLI:");
            for ( Iterator it = parameters.iterator(); it.hasNext(); )
            {
                String param = (String) it.next();
                getLog().debug("  parameter = [" + param + "]");
            }
        }

        return (String[]) parameters.toArray(new String[0]);
    }

    protected void setArtifactFactory(ArtifactFactory artifactFactory)
    {
        this.artifactFactory = artifactFactory;
    }

    protected void setArtifactResolver(ArtifactResolver artifactResolver)
    {
        this.artifactResolver = artifactResolver;
    }
}
