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
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.compiler.util.scan.SimpleSourceInclusionScanner;
import org.codehaus.plexus.compiler.util.scan.SourceInclusionScanner;
import org.codehaus.plexus.compiler.util.scan.InclusionScanException;
import org.codehaus.plexus.compiler.util.scan.mapping.SuffixMapping;

import java.io.File;
import java.util.*;

/**
 * Instrument source roots.
 *
 * Note: Do not call this MOJO directly. It is meant to be called in a forked lifecycle by the other MOJOs.
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
     * @parameter
     * @required
     */
    private String cloverOutputDirectory;

    /**
     * @parameter expression="${plugin.artifacts}"
     * @required
     */
    private List pluginArtifacts;

    /**
     * @parameter expression="${component.org.apache.maven.artifact.factory.ArtifactFactory}"
     * @required
     * @readonly
     */
    private ArtifactFactory factory;

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

    private String cloverOutputSourceDirectory;

    /**
     * {@inheritDoc}
     * @see org.apache.maven.plugin.clover.AbstractCloverMojo#execute()
     */
    public void execute()
        throws MojoExecutionException
    {
        if ( shouldExecute() )
        {
            // Ensure output directories exist
            new File( this.cloverOutputDirectory ).mkdirs();
            this.cloverOutputSourceDirectory = new File( this.cloverOutputDirectory, "src" ).getPath();

            new File( getCloverDatabase() ).getParentFile().mkdirs();

            super.execute();

            Set filesToInstrument = computeFilesToInstrument();
            if ( filesToInstrument.isEmpty() )
            {
                getLog().warn("No Clover instrumentation done as no matching sources files found");
            }
            else
            {
                instrumentSources( filesToInstrument );
                addCloverDependencyToCompileClasspath();
                redirectSourceDirectories();
                redirectOutputDirectories();
            }
        }
    }

    private boolean shouldExecute()
    {
        boolean shouldExecute = true;

        // Only execute reports for java projects
        ArtifactHandler artifactHandler = getProject().getArtifact().getArtifactHandler();
        File srcDir = new File( getProject().getBuild().getSourceDirectory() );

        if ( !"java".equals( artifactHandler.getLanguage() ) )
        {
            getLog().debug( "Not instrumenting sources with Clover as this is not a Java project." );
            shouldExecute = false;
        }
        else if ( !srcDir.exists() )
        {
            getLog().debug("No sources found - No Clover instrumentation done");
            shouldExecute = false;
        }

        return shouldExecute;
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

        // TODO: Ulgy hack below. Changing the directory should be enough for changing the values of all other
        // properties depending on it!
        getProject().getBuild().setOutputDirectory( new File( this.cloverOutputDirectory, "classes" ).getPath() );
        getProject().getBuild().setTestOutputDirectory(
            new File( this.cloverOutputDirectory, "test-classes" ).getPath() );
    }

    private void redirectSourceDirectories()
    {
        String oldSourceDirectory = getProject().getBuild().getSourceDirectory();

        getProject().getBuild().setSourceDirectory( this.cloverOutputSourceDirectory );

        // Maven2 limitation: changing the source directory doesn't change the compile source roots
        // See http://jira.codehaus.org/browse/MNG-1945
        List sourceRoots = getProject().getCompileSourceRoots();
        for (int i = 0; i < sourceRoots.size(); i++)
        {
            String sourceRoot = (String) getProject().getCompileSourceRoots().get( i );
            if (sourceRoot.equals(oldSourceDirectory))
            {
                getProject().getCompileSourceRoots().remove( i );

                // Note: Ideally we should add the new compile source root at the same place as the
                // one we're removing but there's no API for this...
                getProject().addCompileSourceRoot( getProject().getBuild().getSourceDirectory() );
            }
        }
    }

    private void addCloverDependencyToCompileClasspath()
        throws MojoExecutionException
    {
        Artifact cloverArtifact = null;
        Iterator artifacts = this.pluginArtifacts.iterator();
        while ( artifacts.hasNext() && cloverArtifact == null )
        {
            Artifact artifact = (Artifact) artifacts.next();
            if ( "clover".equalsIgnoreCase( artifact.getArtifactId() ) )
            {
                cloverArtifact = artifact;
            }
        }

        if ( cloverArtifact == null )
        {
            throw new MojoExecutionException( "Couldn't find 'clover' artifact in plugin dependencies" );
        }

        cloverArtifact = factory.createArtifact( cloverArtifact.getGroupId(), cloverArtifact.getArtifactId(),
                                                 cloverArtifact.getVersion(), Artifact.SCOPE_COMPILE,
                                                 cloverArtifact.getType() );

        // TODO: use addArtifacts when it's implemented, see http://jira.codehaus.org/browse/MNG-2197
        Set set = new HashSet( getProject().getArtifacts() );
        set.add( cloverArtifact );
        getProject().setDependencyArtifacts( set );
    }

    /**
     * @return the list of files to instrument taking into account the includes and excludes specified by the user
     */
    private Set computeFilesToInstrument()
    {
        Set filesToInstrument = new HashSet();

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
        scanner.addSourceMapping(new SuffixMapping("dummy", "dummy"));

        Iterator roots = getProject().getCompileSourceRoots().iterator();
        while (roots.hasNext())
        {
            String sourceRoot = (String) roots.next();
            try
            {
                filesToInstrument.addAll(scanner.getIncludedSources(new File(sourceRoot), null));
            }
            catch (InclusionScanException e)
            {
                getLog().warn("Failed to add sources from [" + sourceRoot + "]", e);
            }
        }

        return filesToInstrument;
    }

    /**
     * @return the CLI args to be passed to CloverInstr
     * @todo handle multiple source roots. At the moment only the first source root is instrumented
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

        getLog().debug( "Instrumenting using parameters [" + parameters.toString() + "]");

        return (String[]) parameters.toArray(new String[0]);
    }
}
