package org.apache.maven.plugins.dependency.tree;

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

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.Restriction;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.dependency.utils.DependencyUtil;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.shared.artifact.filter.StrictPatternExcludesArtifactFilter;
import org.apache.maven.shared.artifact.filter.StrictPatternIncludesArtifactFilter;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilderException;
import org.apache.maven.shared.dependency.graph.DependencyNode;
import org.apache.maven.shared.dependency.graph.filter.AncestorOrSelfDependencyNodeFilter;
import org.apache.maven.shared.dependency.graph.filter.AndDependencyNodeFilter;
import org.apache.maven.shared.dependency.graph.filter.ArtifactDependencyNodeFilter;
import org.apache.maven.shared.dependency.graph.filter.DependencyNodeFilter;
import org.apache.maven.shared.dependency.graph.traversal.BuildingDependencyNodeVisitor;
import org.apache.maven.shared.dependency.graph.traversal.CollectingDependencyNodeVisitor;
import org.apache.maven.shared.dependency.graph.traversal.DependencyNodeVisitor;
import org.apache.maven.shared.dependency.graph.traversal.FilteringDependencyNodeVisitor;
import org.apache.maven.shared.dependency.graph.traversal.SerializingDependencyNodeVisitor;
import org.apache.maven.shared.dependency.graph.traversal.SerializingDependencyNodeVisitor.GraphTokens;

/**
 * Displays the dependency tree for this project.
 *
 * @author <a href="mailto:markhobson@gmail.com">Mark Hobson</a>
 * @version $Id$
 * @since 2.0-alpha-5
 */
@Mojo( name = "tree", requiresDependencyCollection = ResolutionScope.TEST, threadSafe = true )
public class TreeMojo
    extends AbstractMojo
{
    // fields -----------------------------------------------------------------

    /**
     * The Maven project.
     */
    @Parameter( defaultValue = "${project}", readonly = true, required = true )
    private MavenProject project;

    @Parameter( defaultValue = "${session}", readonly = true, required = true )
    private MavenSession session;

    /**
     * Contains the full list of projects in the reactor.
     */
    @Parameter( defaultValue = "${reactorProjects}", readonly = true, required = true )
    private List<MavenProject> reactorProjects;

    /**
     * The dependency tree builder to use.
     */
    @Component( hint = "default" )
    private DependencyGraphBuilder dependencyGraphBuilder;

    /**
     * If specified, this parameter will cause the dependency tree to be written to the path specified, instead of
     * writing to the console.
     *
     * @since 2.0-alpha-5
     */
    @Parameter( property = "outputFile" )
    private File outputFile;

    /**
     * If specified, this parameter will cause the dependency tree to be written using the specified format. Currently
     * supported format are: <code>text</code>, <code>dot</code>, <code>graphml</code> and <code>tgf</code>.
     * <p/>
     * These formats can be plotted to image files. An example of how to plot a dot file using pygraphviz can be found
     * <a href="http://networkx.lanl.gov/pygraphviz/tutorial.html#layout-and-drawing">here</a>.
     *
     * @since 2.2
     */
    @Parameter( property = "outputType", defaultValue = "text" )
    private String outputType;

    /**
     * The scope to filter by when resolving the dependency tree, or <code>null</code> to include dependencies from all
     * scopes. Note that this feature does not currently work due to MSHARED-4
     *
     * @see <a href="https://issues.apache.org/jira/browse/MSHARED-4">MSHARED-4</a>
     * @since 2.0-alpha-5
     */
    @Parameter( property = "scope" )
    private String scope;

    /**
     * Whether to include omitted nodes in the serialized dependency tree. Notice this feature actually uses Maven 2
     * algorithm and <a href="http://maven.apache.org/shared/maven-dependency-tree/">may give wrong results when used
     * with Maven 3</a>.
     *
     * @since 2.0-alpha-6
     */
    @Parameter( property = "verbose", defaultValue = "false" )
    private boolean verbose;

    /**
     * The token set name to use when outputting the dependency tree. Possible values are <code>whitespace</code>,
     * <code>standard</code> or <code>extended</code>, which use whitespace, standard (ie ASCII) or extended character
     * sets respectively.
     *
     * @since 2.0-alpha-6
     */
    @Parameter( property = "tokens", defaultValue = "standard" )
    private String tokens;

    /**
     * A comma-separated list of artifacts to filter the serialized dependency tree by, or <code>null</code> not to
     * filter the dependency tree. The filter syntax is:
     * 
     * <pre>
     * [groupId]:[artifactId]:[type]:[version]
     * </pre>
     * 
     * where each pattern segment is optional and supports full and partial <code>*</code> wildcards. An empty pattern
     * segment is treated as an implicit wildcard.
     * <p>
     * For example, <code>org.apache.*</code> will match all artifacts whose group id starts with
     * <code>org.apache.</code>, and <code>:::*-SNAPSHOT</code> will match all snapshot artifacts.
     * </p>
     * 
     * @see StrictPatternIncludesArtifactFilter
     * @since 2.0-alpha-6
     */
    @Parameter( property = "includes" )
    private String includes;

    /**
     * A comma-separated list of artifacts to filter from the serialized dependency tree, or <code>null</code> not to
     * filter any artifacts from the dependency tree. The filter syntax is:
     * 
     * <pre>
     * [groupId]:[artifactId]:[type]:[version]
     * </pre>
     * 
     * where each pattern segment is optional and supports full and partial <code>*</code> wildcards. An empty pattern
     * segment is treated as an implicit wildcard.
     * <p>
     * For example, <code>org.apache.*</code> will match all artifacts whose group id starts with
     * <code>org.apache.</code>, and <code>:::*-SNAPSHOT</code> will match all snapshot artifacts.
     * </p>
     *
     * @see StrictPatternExcludesArtifactFilter
     * @since 2.0-alpha-6
     */
    @Parameter( property = "excludes" )
    private String excludes;

    /**
     * The computed dependency tree root node of the Maven project.
     */
    private DependencyNode rootNode;

    /**
     * Whether to append outputs into the output file or overwrite it.
     *
     * @since 2.2
     */
    @Parameter( property = "appendOutput", defaultValue = "false" )
    private boolean appendOutput;

    /**
     * Skip plugin execution completely.
     *
     * @since 2.7
     */
    @Parameter( property = "skip", defaultValue = "false" )
    private boolean skip;

    // Mojo methods -----------------------------------------------------------

    /*
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    @Override
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( isSkip() )
        {
            getLog().info( "Skipping plugin execution" );
            return;
        }

        try
        {
            String dependencyTreeString;

            // TODO: note that filter does not get applied due to MSHARED-4
            ArtifactFilter artifactFilter = createResolvingArtifactFilter();

            if ( verbose )
            {
                // To fix we probably need a different DependencyCollector in Aether, which doesn't remove nodes which
                // have already been resolved.
                getLog().info( "Verbose not supported since maven-dependency-plugin 3.0" );
            }

            ProjectBuildingRequest buildingRequest =
                new DefaultProjectBuildingRequest( session.getProjectBuildingRequest() );

            buildingRequest.setProject( project );

            // non-verbose mode use dependency graph component, which gives consistent results with Maven version
            // running
            rootNode = dependencyGraphBuilder.buildDependencyGraph( buildingRequest, artifactFilter, reactorProjects );

            dependencyTreeString = serializeDependencyTree( rootNode );

            if ( outputFile != null )
            {
                DependencyUtil.write( dependencyTreeString, outputFile, this.appendOutput, getLog() );

                getLog().info( "Wrote dependency tree to: " + outputFile );
            }
            else
            {
                DependencyUtil.log( dependencyTreeString, getLog() );
            }
        }
        catch ( DependencyGraphBuilderException exception )
        {
            throw new MojoExecutionException( "Cannot build project dependency graph", exception );
        }
        catch ( IOException exception )
        {
            throw new MojoExecutionException( "Cannot serialise project dependency graph", exception );
        }
    }

    // public methods ---------------------------------------------------------

    /**
     * Gets the Maven project used by this mojo.
     *
     * @return the Maven project
     */
    public MavenProject getProject()
    {
        return project;
    }

    /**
     * Gets the computed dependency graph root node for the Maven project.
     *
     * @return the dependency tree root node
     */
    public DependencyNode getDependencyGraph()
    {
        return rootNode;
    }

    public boolean isSkip()
    {
        return skip;
    }

    public void setSkip( boolean skip )
    {
        this.skip = skip;
    }

    // private methods --------------------------------------------------------

    /**
     * Gets the artifact filter to use when resolving the dependency tree.
     *
     * @return the artifact filter
     */
    private ArtifactFilter createResolvingArtifactFilter()
    {
        ArtifactFilter filter;

        // filter scope
        if ( scope != null )
        {
            getLog().debug( "+ Resolving dependency tree for scope '" + scope + "'" );

            filter = new ScopeArtifactFilter( scope );
        }
        else
        {
            filter = null;
        }

        return filter;
    }

    /**
     * Serializes the specified dependency tree to a string.
     *
     * @param rootNode the dependency tree root node to serialize
     * @return the serialized dependency tree
     */
    private String serializeDependencyTree( DependencyNode rootNode )
    {
        StringWriter writer = new StringWriter();

        DependencyNodeVisitor visitor = getSerializingDependencyNodeVisitor( writer );

        // TODO: remove the need for this when the serializer can calculate last nodes from visitor calls only
        visitor = new BuildingDependencyNodeVisitor( visitor );

        DependencyNodeFilter filter = createDependencyNodeFilter();

        if ( filter != null )
        {
            CollectingDependencyNodeVisitor collectingVisitor = new CollectingDependencyNodeVisitor();
            DependencyNodeVisitor firstPassVisitor = new FilteringDependencyNodeVisitor( collectingVisitor, filter );
            rootNode.accept( firstPassVisitor );

            DependencyNodeFilter secondPassFilter =
                new AncestorOrSelfDependencyNodeFilter( collectingVisitor.getNodes() );
            visitor = new FilteringDependencyNodeVisitor( visitor, secondPassFilter );
        }

        rootNode.accept( visitor );

        return writer.toString();
    }

    public DependencyNodeVisitor getSerializingDependencyNodeVisitor( Writer writer )
    {
        if ( "graphml".equals( outputType ) )
        {
            return new GraphmlDependencyNodeVisitor( writer );
        }
        else if ( "tgf".equals( outputType ) )
        {
            return new TGFDependencyNodeVisitor( writer );
        }
        else if ( "dot".equals( outputType ) )
        {
            return new DOTDependencyNodeVisitor( writer );
        }
        else
        {
            return new SerializingDependencyNodeVisitor( writer, toGraphTokens( tokens ) );
        }
    }

    /**
     * Gets the graph tokens instance for the specified name.
     *
     * @param tokens the graph tokens name
     * @return the <code>GraphTokens</code> instance
     */
    private GraphTokens toGraphTokens( String tokens )
    {
        GraphTokens graphTokens;

        if ( "whitespace".equals( tokens ) )
        {
            getLog().debug( "+ Using whitespace tree tokens" );

            graphTokens = SerializingDependencyNodeVisitor.WHITESPACE_TOKENS;
        }
        else if ( "extended".equals( tokens ) )
        {
            getLog().debug( "+ Using extended tree tokens" );

            graphTokens = SerializingDependencyNodeVisitor.EXTENDED_TOKENS;
        }
        else
        {
            graphTokens = SerializingDependencyNodeVisitor.STANDARD_TOKENS;
        }

        return graphTokens;
    }

    /**
     * Gets the dependency node filter to use when serializing the dependency graph.
     *
     * @return the dependency node filter, or <code>null</code> if none required
     */
    private DependencyNodeFilter createDependencyNodeFilter()
    {
        List<DependencyNodeFilter> filters = new ArrayList<DependencyNodeFilter>();

        // filter includes
        if ( includes != null )
        {
            List<String> patterns = Arrays.asList( includes.split( "," ) );

            getLog().debug( "+ Filtering dependency tree by artifact include patterns: " + patterns );

            ArtifactFilter artifactFilter = new StrictPatternIncludesArtifactFilter( patterns );
            filters.add( new ArtifactDependencyNodeFilter( artifactFilter ) );
        }

        // filter excludes
        if ( excludes != null )
        {
            List<String> patterns = Arrays.asList( excludes.split( "," ) );

            getLog().debug( "+ Filtering dependency tree by artifact exclude patterns: " + patterns );

            ArtifactFilter artifactFilter = new StrictPatternExcludesArtifactFilter( patterns );
            filters.add( new ArtifactDependencyNodeFilter( artifactFilter ) );
        }

        return filters.isEmpty() ? null : new AndDependencyNodeFilter( filters );
    }

    // following is required because the version handling in maven code
    // doesn't work properly. I ripped it out of the enforcer rules.

    /**
     * Copied from Artifact.VersionRange. This is tweaked to handle singular ranges properly. Currently the default
     * containsVersion method assumes a singular version means allow everything. This method assumes that "2.0.4" ==
     * "[2.0.4,)"
     *
     * @param allowedRange range of allowed versions.
     * @param theVersion the version to be checked.
     * @return true if the version is contained by the range.
     */
    public static boolean containsVersion( VersionRange allowedRange, ArtifactVersion theVersion )
    {
        ArtifactVersion recommendedVersion = allowedRange.getRecommendedVersion();
        if ( recommendedVersion == null )
        {
            List<Restriction> restrictions = allowedRange.getRestrictions();
            for ( Restriction restriction : restrictions )
            {
                if ( restriction.containsVersion( theVersion ) )
                {
                    return true;
                }
            }
        }

        // only singular versions ever have a recommendedVersion
        return recommendedVersion.compareTo( theVersion ) <= 0;
    }
}
