package org.apache.maven.plugin.javadoc;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.plugin.javadoc.resolver.SourceResolverConfig;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Bundles the Javadoc documentation for <code>test Java code</code> in an <b>NON aggregator</b> project into
 * a jar using the standard <a href="http://docs.oracle.com/javase/7/docs/technotes/guides/javadoc/">Javadoc Tool</a>.
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 * @since 2.5
 */
@Mojo( name = "test-jar", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.TEST,
                threadSafe = true )
public class TestJavadocJar
    extends JavadocJar
{
    // ----------------------------------------------------------------------
    // Javadoc Options (should be inline with Javadoc options defined in TestJavadocReport)
    // ----------------------------------------------------------------------

    /**
     * Specifies the destination directory where Javadoc saves the generated HTML files.
     * <br/>
     * See <a href="http://docs.oracle.com/javase/7/docs/technotes/tools/windows/javadoc.html#d">d</a>.
     * <br/>
     */
    @Parameter( defaultValue = "${project.build.directory}/testapidocs", required = true )
    private File outputDirectory;

    /**
     * Specifies the Test title to be placed near the top of the overview summary file.
     * <br/>
     * See <a href="http://docs.oracle.com/javase/7/docs/technotes/tools/windows/javadoc.html#doctitle">doctitle</a>.
     * <br/>
     *
     * @since 2.5
     */
    @Parameter( property = "testDoctitle", alias = "doctitle",
                defaultValue = "${project.name} ${project.version} Test API" )
    private String testDoctitle;

    /**
     * Specifies that Javadoc should retrieve the text for the Test overview documentation from the "source" file
     * specified by path/filename and place it on the Overview page (overview-summary.html).
     * <br/>
     * See <a href="http://docs.oracle.com/javase/7/docs/technotes/tools/windows/javadoc.html#overview">overview</a>.
     * <br/>
     *
     * @since 2.5
     */
    @Parameter( property = "testOverview", alias = "overview",
                defaultValue = "${basedir}/src/test/javadoc/overview.html" )
    private File testOverview;

    /**
     * Specifies the Test title to be placed in the HTML title tag.
     * <br/>
     * See
     * <a href="http://docs.oracle.com/javase/7/docs/technotes/tools/windows/javadoc.html#windowtitle">windowtitle</a>.
     * <br/>
     *
     * @since 2.5
     */
    @Parameter( property = "testWindowtitle", alias = "windowtitle",
                defaultValue = "${project.name} ${project.version} Test API" )
    private String testWindowtitle;

    // ----------------------------------------------------------------------
    // Mojo Parameters (should be inline with options defined in TestJavadocReport)
    // ----------------------------------------------------------------------

    /**
     * Specifies the Test Javadoc resources directory to be included in the Javadoc (i.e. package.html, images...).
     *
     * @since 2.5
     */
    @Parameter( alias = "javadocDirectory", defaultValue = "${basedir}/src/test/javadoc" )
    private File testJavadocDirectory;

    /**
     * @since 2.10
     */
    @Parameter( property = "maven.javadoc.testClassifier", defaultValue = "test-javadoc", required = true )
    private String testClassifier;

    // ----------------------------------------------------------------------
    // Protected methods
    // ----------------------------------------------------------------------

    @Override
    protected String getClassifier()
    {
        return testClassifier;
    }

    // ----------------------------------------------------------------------
    // Important Note: should be inline with methods defined in TestJavadocReport
    // ----------------------------------------------------------------------

    @Override
    protected String getOutputDirectory()
    {
        return outputDirectory.getAbsoluteFile().toString();
    }

    @Override
    protected File getJavadocDirectory()
    {
        return testJavadocDirectory;
    }

    @Override
    protected String getDoctitle()
    {
        return testDoctitle;
    }

    @Override
    protected File getOverview()
    {
        return testOverview;
    }

    @Override
    protected String getWindowtitle()
    {
        return testWindowtitle;
    }

    @Override
    protected List<String> getProjectBuildOutputDirs( MavenProject p )
    {
        List<String> dirs = new ArrayList<String>();
        if ( StringUtils.isNotEmpty( p.getBuild().getOutputDirectory() ) )
        {
            dirs.add( p.getBuild().getOutputDirectory() );
        }
        if ( StringUtils.isNotEmpty( p.getBuild().getTestOutputDirectory() ) )
        {
            dirs.add( p.getBuild().getTestOutputDirectory() );
        }

        return dirs;
    }

    @Override
    protected List<String> getProjectSourceRoots( MavenProject p )
    {
        if ( "pom".equals( p.getPackaging().toLowerCase() ) )
        {
            return Collections.emptyList();
        }

        return p.getTestCompileSourceRoots();
    }

    @Override
    protected List<String> getExecutionProjectSourceRoots( MavenProject p )
    {
        if ( "pom".equals( p.getExecutionProject().getPackaging().toLowerCase() ) )
        {
            return Collections.emptyList();
        }

        return p.getExecutionProject().getTestCompileSourceRoots();
    }

    @Override
    protected List<Artifact> getProjectArtifacts( MavenProject p )
    {
        return p.getTestArtifacts();
    }

    @Override
    protected List<Artifact> getCompileArtifacts( ArtifactResolutionResult result )
    {
        return JavadocUtil.getCompileArtifacts( result.getArtifacts(), true );
    }
    
    /**
     * Overriden to enable the resolution of -test-sources jar files.
     * 
     * {@inheritDoc}
     */
    @Override
    protected SourceResolverConfig configureDependencySourceResolution( final SourceResolverConfig config )
    {
        return super.configureDependencySourceResolution( config ).withoutCompileSources().withTestSources();
    }

    @Override
    protected boolean isTest()
    {
        return true;
    }
}
