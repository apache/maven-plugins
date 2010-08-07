package org.apache.maven.plugins.shade.mojo;

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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactCollector;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Exclusion;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.shade.Shader;
import org.apache.maven.plugins.shade.filter.MinijarFilter;
import org.apache.maven.plugins.shade.filter.SimpleFilter;
import org.apache.maven.plugins.shade.pom.PomWriter;
import org.apache.maven.plugins.shade.relocation.SimpleRelocator;
import org.apache.maven.plugins.shade.resource.ResourceTransformer;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.shared.dependency.tree.DependencyNode;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilder;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilderException;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.WriterFactory;

/**
 * Mojo that performs shading delegating to the Shader component.
 *
 * @author Jason van Zyl
 * @author Mauro Talevi
 * @author David Blevins
 * @author Hiram Chirino
 * @goal shade
 * @phase package
 * @requiresDependencyResolution runtime
 * @threadSafe
 */
public class ShadeMojo
    extends AbstractMojo
{
    /**
     * @parameter default-value="${project}"
     * @readonly
     * @required
     */
    private MavenProject project;

    /**
     * @component
     * @required
     * @readonly
     */
    private MavenProjectHelper projectHelper;

    /**
     * @component
     * @required
     * @readonly
     */
    private Shader shader;

    /**
     * The dependency tree builder to use.
     *
     * @component
     * @required
     * @readonly
     */
    private DependencyTreeBuilder dependencyTreeBuilder;

    /**
     * ProjectBuilder, needed to create projects from the artifacts.
     *
     * @component
     * @required
     * @readonly
     */
    private MavenProjectBuilder mavenProjectBuilder;

    /**
     * The artifact metadata source to use.
     *
     * @component
     * @required
     * @readonly
     */
    private ArtifactMetadataSource artifactMetadataSource;

    /**
     * The artifact collector to use.
     *
     * @component
     * @required
     * @readonly
     */
    private ArtifactCollector artifactCollector;

    /**
     * Remote repositories which will be searched for source attachments.
     *
     * @parameter default-value="${project.remoteArtifactRepositories}"
     * @required
     * @readonly
     */
    protected List remoteArtifactRepositories;

    /**
     * Local maven repository.
     *
     * @parameter default-value="${localRepository}"
     * @required
     * @readonly
     */
    protected ArtifactRepository localRepository;

    /**
     * Artifact factory, needed to download source jars for inclusion in classpath.
     *
     * @component
     * @required
     * @readonly
     */
    protected ArtifactFactory artifactFactory;

    /**
     * Artifact resolver, needed to download source jars for inclusion in classpath.
     *
     * @component
     * @required
     * @readonly
     */
    protected ArtifactResolver artifactResolver;

    /**
     * Artifacts to include/exclude from the final artifact. Artifacts are denoted by composite identifiers of the
     * general form <code>groupId:artifactId:type:classifier</code>. Since version 1.3, the wildcard characters '*' and
     * '?' can be used within the sub parts of those composite identifiers to do pattern matching. For convenience, the
     * syntax <code>groupId</code> is equivalent to <code>groupId:*:*:*</code>, <code>groupId:artifactId</code> is
     * equivalent to <code>groupId:artifactId:*:*</code> and <code>groupId:artifactId:classifier</code> is equivalent to
     * <code>groupId:artifactId:*:classifier</code>. For example:
     * <pre>
     * &lt;artifactSet&gt;
     *   &lt;includes&gt;
     *     &lt;include&gt;org.apache.maven:*&lt;/include&gt;
     *   &lt;/includes&gt;
     *   &lt;excludes&gt;
     *     &lt;exclude&gt;*:maven-core&lt;/exclude&gt;
     *   &lt;/excludes&gt;
     * &lt;/artifactSet&gt;
     * </pre>
     * 
     * @parameter
     */
    private ArtifactSet artifactSet;

    /**
     * Packages to be relocated. For example:
     * <pre>
     * &lt;relocations&gt;
     *   &lt;relocation&gt;
     *     &lt;pattern&gt;org.apache&lt;/pattern&gt;
     *     &lt;shadedPattern&gt;hidden.org.apache&lt;/shadedPattern&gt;
     *     &lt;excludes&gt;
     *       &lt;exclude&gt;org.apache.ExcludedClass&lt;/exclude&gt;
     *     &lt;/excludes&gt;
     *   &lt;/relocation&gt;
     * &lt;/relocations&gt;
     * </pre>
     *
     * @parameter
     */
    private PackageRelocation[] relocations;

    /**
     * Resource transformers to be used. Please see the "Examples" section for more information on available
     * transformers and their configuration.
     * 
     * @parameter
     */
    private ResourceTransformer[] transformers;

    /**
     * Archive Filters to be used. Allows you to specify an artifact in the form of a composite identifier as used by
     * {@link #artifactSet} and a set of include/exclude file patterns for filtering which contents of the archive are
     * added to the shaded jar. From a logical perspective, includes are processed before excludes, thus it's possible
     * to use an include to collect a set of files from the archive then use excludes to further reduce the set. By
     * default, all files are included and no files are excluded. If multiple filters apply to an artifact, the
     * intersection of the matched files will be included in the final JAR. For example:
     * <pre>
     * &lt;filters&gt;
     *   &lt;filter&gt;
     *     &lt;artifact&gt;junit:junit&lt;/artifact&gt;
     *     &lt;includes&gt;
     *       &lt;include&gt;org/junit/**&lt;/include&gt;
     *     &lt;/includes&gt;
     *     &lt;excludes&gt;
     *       &lt;exclude&gt;org/junit/experimental/**&lt;/exclude&gt;
     *     &lt;/excludes&gt;
     *   &lt;/filter&gt;
     * &lt;/filters&gt;
     * </pre>
     * 
     * @parameter
     */
    private ArchiveFilter[] filters;

    /**
     * The destination directory for the shaded artifact.
     *
     * @parameter default-value="${project.build.directory}"
     */
    private File outputDirectory;

    /**
     * The name of the shaded artifactId.
     * 
     * If you like to change the name of the native artifact, you may use the &lt;build>&lt;finalName> setting.
     * If this is set to something different than &lt;build>&lt;finalName>, no file replacement
     * will be performed, even if shadedArtifactAttached is being used.
     *
     * @parameter expression="${finalName}"
     */
    private String finalName;

    /**
     * The name of the shaded artifactId. So you may want to use a different artifactId and keep
     * the standard version. If the original artifactId was "foo" then the final artifact would
     * be something like foo-1.0.jar. So if you change the artifactId you might have something
     * like foo-special-1.0.jar.
     *
     * @parameter expression="${shadedArtifactId}" default-value="${project.artifactId}"
     */
    private String shadedArtifactId;

    /**
     * If specified, this will include only artifacts which have groupIds which
     * start with this.
     *
     * @parameter expression="${shadedGroupFilter}"
     */
    private String shadedGroupFilter;

    /**
     * Defines whether the shaded artifact should be attached as classifier to
     * the original artifact.  If false, the shaded jar will be the main artifact
     * of the project
     *
     * @parameter expression="${shadedArtifactAttached}" default-value="false"
     */
    private boolean shadedArtifactAttached;

    /**
     * Flag whether to generate a simplified POM for the shaded artifact. If set to <code>true</code>, dependencies that
     * have been included into the uber JAR will be removed from the <code>&lt;dependencies&gt;</code> section of the
     * generated POM. The reduced POM will be named <code>dependency-reduced-pom.xml</code> and is stored into the same
     * directory as the shaded artifact.
     *
     * @parameter expression="${createDependencyReducedPom}" default-value="true"
     */
    private boolean createDependencyReducedPom;

    /**
     * When true, dependencies are kept in the pom but with scope 'provided'; when false,
     * the dependency is removed.
     *
     * @parameter expression="${keepDependenciesWithProvidedScope}" default-value="false"
     */
    private boolean keepDependenciesWithProvidedScope;

    /**
     * When true, transitive deps of removed dependencies are promoted to direct dependencies.
     * This should allow the drop in replacement of the removed deps with the new shaded
     * jar and everything should still work.
     *
     * @parameter expression="${promoteTransitiveDependencies}" default-value="false"
     */
    private boolean promoteTransitiveDependencies;

    /**
     * The name of the classifier used in case the shaded artifact is attached.
     *
     * @parameter expression="${shadedClassifierName}" default-value="shaded"
     */
    private String shadedClassifierName;

    /**
     * When true, it will attempt to create a sources jar as well
     *
     * @parameter expression="${createSourcesJar}" default-value="false"
     */
    private boolean createSourcesJar;

    /**
     * When true, dependencies will be stripped down on the class level
     * to only the transitive hull required for the artifact.
     *
     * @parameter default-value="false"
     */
    private boolean minimizeJar;

    /**
     * The path to the output file for the shaded artifact. When this parameter is set, the created archive will neither
     * replace the project's main artifact nor will it be attached. Hence, this parameter causes the parameters
     * {@link #finalName}, {@link #shadedArtifactAttached}, {@link #shadedClassifierName} and
     * {@link #createDependencyReducedPom} to be ignored when used.
     * 
     * @parameter
     * @since 1.3
     */
    private File outputFile;

    /** @throws MojoExecutionException  */
    public void execute()
        throws MojoExecutionException
    {
        Set artifacts = new LinkedHashSet();
        Set artifactIds = new LinkedHashSet();
        Set sourceArtifacts = new LinkedHashSet();

        ArtifactSelector artifactSelector =
            new ArtifactSelector( project.getArtifact(), artifactSet, shadedGroupFilter );

        if ( artifactSelector.isSelected( project.getArtifact() ) && !"pom".equals( project.getArtifact().getType() ) )
        {
            if ( project.getArtifact().getFile() == null )
            {
                getLog().error( "The project main artifact does not exist. This could have the following" );
                getLog().error( "reasons:" );
                getLog().error( "- You have invoked the goal directly from the command line. This is not" );
                getLog().error( "  supported. Please add the goal to the default lifecycle via an" );
                getLog().error( "  <execution> element in your POM and use \"mvn package\" to have it run." );
                getLog().error( "- You have bound the goal to a lifecycle phase before \"package\". Please" );
                getLog().error( "  remove this binding from your POM such that the goal will be run in" );
                getLog().error( "  the proper phase." );
                throw new MojoExecutionException( "Failed to create shaded artifact, "
                    + "project main artifact does not exist." );
            }

            artifacts.add( project.getArtifact().getFile() );

            if ( createSourcesJar )
            {
                File file = shadedSourcesArtifactFile();
                if ( file.isFile() )
                {
                    sourceArtifacts.add( file );
                }
            }
        }

        for ( Iterator it = project.getArtifacts().iterator(); it.hasNext(); )
        {
            Artifact artifact = (Artifact) it.next();

            if ( !artifactSelector.isSelected( artifact ) )
            {
                getLog().info( "Excluding " + artifact.getId() + " from the shaded jar." );

                continue;
            }

            if ( "pom".equals( artifact.getType() ) )
            {
                getLog().info( "Skipping pom dependency " + artifact.getId() + " in the shaded jar." );
                continue;
            }

            getLog().info( "Including " + artifact.getId() + " in the shaded jar." );

            artifacts.add( artifact.getFile() );

            artifactIds.add( getId( artifact ) );

            if ( createSourcesJar )
            {
                File file = resolveArtifactSources( artifact );
                if ( file != null )
                {
                    sourceArtifacts.add( file );
                }
            }
        }


        File outputJar = ( outputFile != null ) ? outputFile : shadedArtifactFileWithClassifier();
        File sourcesJar = shadedSourceArtifactFileWithClassifier();

        // Now add our extra resources
        try
        {
            List filters = getFilters();

            List relocators = getRelocators();

            List resourceTransformers = getResourceTransformers();

            shader.shade( artifacts, outputJar, filters, relocators, resourceTransformers );

            if ( createSourcesJar )
            {
                shader.shade( sourceArtifacts, sourcesJar, filters, relocators, resourceTransformers );
            }

            if ( outputFile == null )
            {
                boolean renamed = false;

                // rename the output file if a specific finalName is set
                // but don't rename if the finalName is the <build><finalName>
                // because this will be handled implicitely later
                if ( finalName != null && finalName.length() > 0
                    && !finalName.equals( project.getBuild().getFinalName() ) )
                {
                    String finalFileName = finalName + "." + project.getArtifact().getArtifactHandler().getExtension();
                    File finalFile = new File( outputDirectory, finalFileName );
                    replaceFile( finalFile, outputJar );
                    outputJar = finalFile;

                    renamed = true;
                }

                if ( shadedArtifactAttached )
                {
                    getLog().info( "Attaching shaded artifact." );
                    projectHelper.attachArtifact( project, project.getArtifact().getType(), shadedClassifierName,
                                                  outputJar );
                    if ( createSourcesJar )
                    {
                        projectHelper.attachArtifact( project, "jar", shadedClassifierName + "-sources", sourcesJar );
                    }
                }
                else if ( !renamed )
                {
                    getLog().info( "Replacing original artifact with shaded artifact." );
                    File originalArtifact = project.getArtifact().getFile();
                    replaceFile( originalArtifact, outputJar );

                    if ( createSourcesJar )
                    {
                        File shadedSources = shadedSourcesArtifactFile();

                        replaceFile( shadedSources, sourcesJar );

                        projectHelper.attachArtifact( project, "jar", "sources", shadedSources );
                    }

                    if ( createDependencyReducedPom )
                    {
                        createDependencyReducedPom( artifactIds );
                    }
                }
            }
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Error creating shaded jar: " + e.getMessage(), e );
        }
    }

    private void replaceFile( File oldFile, File newFile ) throws MojoExecutionException
    {
        getLog().info( "Replacing " + oldFile + " with " + newFile );

        File origFile = new File( outputDirectory, "original-" + oldFile.getName() );
        if ( oldFile.exists() && !oldFile.renameTo( origFile ) )
        {
            //try a gc to see if an unclosed stream needs garbage collecting
            System.gc();
            System.gc();

            if ( !oldFile.renameTo( origFile ) )
            {
                // Still didn't work.   We'll do a copy
                try
                {
                    FileOutputStream fout = new FileOutputStream( origFile );
                    FileInputStream fin = new FileInputStream( oldFile );
                    try
                    {
                        IOUtil.copy( fin, fout );
                    }
                    finally
                    {
                        IOUtil.close( fin );
                        IOUtil.close( fout );
                    }
                }
                catch ( IOException ex )
                {
                    //kind of ignorable here.   We're just trying to save the original
                    getLog().warn( ex );
                }
            }
        }
        if ( !newFile.renameTo( oldFile ) )
        {
            //try a gc to see if an unclosed stream needs garbage collecting
            System.gc();
            System.gc();

            if ( !newFile.renameTo( oldFile ) )
            {
                // Still didn't work.   We'll do a copy
                try
                {
                    FileOutputStream fout = new FileOutputStream( oldFile );
                    FileInputStream fin = new FileInputStream( newFile );
                    try
                    {
                        IOUtil.copy( fin, fout );
                    }
                    finally
                    {
                        IOUtil.close( fin );
                        IOUtil.close( fout );
                    }
                }
                catch ( IOException ex )
                {
                    throw new MojoExecutionException( "Could not replace original artifact with shaded artifact!", ex );
                }
            }
        }
    }

    private File resolveArtifactSources( Artifact artifact )
    {

        Artifact resolvedArtifact =
            artifactFactory.createArtifactWithClassifier( artifact.getGroupId(),
                                                          artifact.getArtifactId(),
                                                          artifact.getVersion(),
                                                          "java-source",
                                                          "sources" );

        try
        {
            artifactResolver.resolve( resolvedArtifact, remoteArtifactRepositories, localRepository );
        }
        catch ( ArtifactNotFoundException e )
        {
            // ignore, the jar has not been found
        }
        catch ( ArtifactResolutionException e )
        {
            getLog().warn( "Could not get sources for " + artifact );
        }

        if ( resolvedArtifact.isResolved() )
        {
            return resolvedArtifact.getFile();
        }
        return null;
    }

    private List getRelocators()
    {
        List relocators = new ArrayList();

        if ( relocations == null )
        {
            return relocators;
        }

        for ( int i = 0; i < relocations.length; i++ )
        {
            PackageRelocation r = relocations[i];

            relocators.add( new SimpleRelocator( r.getPattern(), r.getShadedPattern(), r.getExcludes() ) );
        }

        return relocators;
    }

    private List getResourceTransformers()
    {
        if ( transformers == null )
        {
            return Collections.EMPTY_LIST;
        }

        return Arrays.asList( transformers );
    }

    private List getFilters()
        throws MojoExecutionException
    {
        List filters = new ArrayList();

        if ( this.filters != null && this.filters.length > 0 )
        {
            Map artifacts = new HashMap();

            artifacts.put( project.getArtifact(), new ArtifactId( project.getArtifact() ) );

            for ( Iterator it = project.getArtifacts().iterator(); it.hasNext(); )
            {
                Artifact artifact = (Artifact) it.next();

                artifacts.put( artifact, new ArtifactId( artifact ) );
            }

            for ( int i = 0; i < this.filters.length; i++ )
            {
                ArchiveFilter filter = this.filters[i];

                ArtifactId pattern = new ArtifactId( filter.getArtifact() );

                Set jars = new HashSet();

                for ( Iterator it = artifacts.entrySet().iterator(); it.hasNext(); )
                {
                    Map.Entry entry = (Map.Entry) it.next();

                    if ( ( (ArtifactId) entry.getValue() ).matches( pattern ) )
                    {
                        Artifact artifact = (Artifact) entry.getKey();

                        jars.add( artifact.getFile() );

                        if ( createSourcesJar )
                        {
                            File file = resolveArtifactSources( artifact );
                            if ( file != null )
                            {
                                jars.add( file );
                            }
                        }
                    }
                }

                if ( jars.isEmpty() )
                {
                    getLog().info( "No artifact matching filter " + filter.getArtifact() );

                    continue;
                }

                filters.add( new SimpleFilter( jars, filter.getIncludes(), filter.getExcludes() ) );
            }
        }

        if ( minimizeJar )
        {
            getLog().info( "Minimizing jar " + project.getArtifact() );

            try
            {
                filters.add( new MinijarFilter( project, getLog() ) );
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Failed to analyze class dependencies", e );
            }
        }

        return filters;
    }

    private File shadedArtifactFileWithClassifier()
    {
        Artifact artifact = project.getArtifact();
        final String shadedName =
            shadedArtifactId + "-" + artifact.getVersion() + "-" + shadedClassifierName + "."
                + artifact.getArtifactHandler().getExtension();
        return new File( outputDirectory, shadedName );
    }

    private File shadedSourceArtifactFileWithClassifier()
    {
        Artifact artifact = project.getArtifact();
        final String shadedName =
            shadedArtifactId + "-" + artifact.getVersion() + "-" + shadedClassifierName + "-sources."
                + artifact.getArtifactHandler().getExtension();
        return new File( outputDirectory, shadedName );
    }

    private File shadedSourcesArtifactFile()
    {
        Artifact artifact = project.getArtifact();

        String shadedName;

        if ( project.getBuild().getFinalName() != null )
        {
            shadedName = project.getBuild().getFinalName() + "-sources." + artifact.getArtifactHandler().getExtension();
        }
        else
        {
            shadedName = shadedArtifactId + "-" + artifact.getVersion() + "-sources."
                + artifact.getArtifactHandler().getExtension();
        }

        return new File( outputDirectory, shadedName );
    }

    // We need to find the direct dependencies that have been included in the uber JAR so that we can modify the
    // POM accordingly.
    private void createDependencyReducedPom( Set artifactsToRemove )
        throws IOException, DependencyTreeBuilderException, ProjectBuildingException
    {
        Model model = project.getOriginalModel();
        List dependencies = new ArrayList();

        boolean modified = false;

        List transitiveDeps = new ArrayList();

        for ( Iterator it = project.getArtifacts().iterator(); it.hasNext(); )
        {
            Artifact artifact = (Artifact) it.next();

            //promote
            Dependency dep = new Dependency();
            dep.setArtifactId( artifact.getArtifactId() );
            if ( artifact.hasClassifier() )
            {
                dep.setClassifier( artifact.getClassifier() );
            }
            dep.setGroupId( artifact.getGroupId() );
            dep.setOptional( artifact.isOptional() );
            dep.setScope( artifact.getScope() );
            dep.setType( artifact.getType() );
            dep.setVersion( artifact.getVersion() );

            //we'll figure out the exclusions in a bit.

            transitiveDeps.add( dep );
        }
        List origDeps = project.getDependencies();

        if ( promoteTransitiveDependencies )
        {
            origDeps = transitiveDeps;
        }

        for ( Iterator i = origDeps.iterator(); i.hasNext(); )
        {
            Dependency d = (Dependency) i.next();

            dependencies.add( d );

            String id = getId( d );

            if ( artifactsToRemove.contains( id ) )
            {
                modified = true;

                if ( keepDependenciesWithProvidedScope )
                {
                    d.setScope( "provided" );
                }
                else
                {
                    dependencies.remove( d );
                }
            }
        }

        // Check to see if we have a reduction and if so rewrite the POM.
        if ( modified )
        {
            while ( modified )
            {

                model.setDependencies( dependencies );

                /*
                 * NOTE: Be sure to create the POM in the original base directory to be able to resolve the relativePath
                 * to local parent POMs when invoking the project builder below.
                 */
                File f = new File( project.getBasedir(), "dependency-reduced-pom.xml" );
                if ( f.exists() )
                {
                    f.delete();
                }

                Writer w = WriterFactory.newXmlWriter( f );

                try
                {
                    PomWriter.write( w, model, true );
                }
                finally
                {
                    w.close();
                }

                MavenProject p2 = mavenProjectBuilder.build( f, localRepository, null );
                modified = updateExcludesInDeps( p2, dependencies, transitiveDeps );

            }

            /*
             * NOTE: Although the dependency reduced POM in the project directory is temporary build output, we have to
             * use that for the file of the project instead of something in target to avoid messing up the base
             * directory of the project. We'll delete this file on exit to make sure it gets cleaned up but keep a copy
             * for inspection in the target directory as well.
             */
            File f = new File( project.getBasedir(), "dependency-reduced-pom.xml" );
            File f2 = new File( outputDirectory, "dependency-reduced-pom.xml" );
            FileUtils.copyFile( f, f2 );
            FileUtils.forceDeleteOnExit( f );
            project.setFile( f );
        }
    }

    private String getId( Artifact artifact )
    {
        return getId( artifact.getGroupId(), artifact.getArtifactId(), artifact.getType(), artifact.getClassifier() );
    }

    private String getId( Dependency dependency )
    {
        return getId( dependency.getGroupId(), dependency.getArtifactId(), dependency.getType(),
                      dependency.getClassifier() );
    }

    private String getId( String groupId, String artifactId, String type, String classifier )
    {
        return groupId + ":" + artifactId + ":" + type + ":" + ( ( classifier != null ) ? classifier : "" );
    }

    public boolean updateExcludesInDeps( MavenProject project,
                                         List dependencies,
                                         List transitiveDeps )
        throws DependencyTreeBuilderException
    {
        DependencyNode node = dependencyTreeBuilder.buildDependencyTree(
                                                  project,
                                                  localRepository,
                                                  artifactFactory,
                                                  artifactMetadataSource,
                                                  null,
                                                  artifactCollector );
        boolean modified = false;
        Iterator it = node.getChildren().listIterator();
        while ( it.hasNext() )
        {
            DependencyNode n2 = (DependencyNode) it.next();
            Iterator it2 = n2.getChildren().listIterator();
            while ( it2.hasNext() )
            {
                DependencyNode n3 = (DependencyNode) it2.next();
                //anything two levels deep that is marked "included"
                //is stuff that was excluded by the original poms, make sure it
                //remains excluded IF promoting transitives.
                if ( n3.getState() == DependencyNode.INCLUDED )
                {
                    //check if it really isn't in the list of original dependencies.  Maven
                    //prior to 2.0.8 may grab versions from transients instead of
                    //from the direct deps in which case they would be marked included
                    //instead of OMITTED_FOR_DUPLICATE

                    //also, if not promoting the transitives, level 2's would be included
                    boolean found = false;
                    for ( int x = 0; x < transitiveDeps.size(); x++ )
                    {
                        Dependency dep = (Dependency) transitiveDeps.get( x );
                        if ( dep.getArtifactId().equals( n3.getArtifact().getArtifactId() )
                            && dep.getGroupId().equals( n3.getArtifact().getGroupId() ) )
                        {
                            found = true;
                        }

                    }

                    if ( !found )
                    {
                        for ( int x = 0; x < dependencies.size(); x++ )
                        {
                            Dependency dep = (Dependency) dependencies.get( x );
                            if ( dep.getArtifactId().equals( n2.getArtifact().getArtifactId() )
                                && dep.getGroupId().equals( n2.getArtifact().getGroupId() ) )
                            {
                                Exclusion exclusion = new Exclusion();
                                exclusion.setArtifactId( n3.getArtifact().getArtifactId() );
                                exclusion.setGroupId( n3.getArtifact().getGroupId() );
                                dep.addExclusion( exclusion );
                                modified = true;
                                break;
                            }
                        }
                    }
                }
            }
        }
        return modified;
    }
}
