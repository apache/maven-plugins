package org.apache.maven.plugin.dependency;

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
import java.util.Iterator;
import java.util.List;

import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactCollector;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.Restriction;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.execution.RuntimeInformation;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.dependency.utils.DependencyUtil;
import org.apache.maven.plugin.dependency.treeSerializers.GraphmlDependencyNodeVisitor;
import org.apache.maven.plugin.dependency.treeSerializers.TGFDependencyNodeVisitor;
import org.apache.maven.plugin.dependency.treeSerializers.DOTDependencyNodeVisitor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.artifact.filter.StrictPatternExcludesArtifactFilter;
import org.apache.maven.shared.artifact.filter.StrictPatternIncludesArtifactFilter;
import org.apache.maven.shared.dependency.tree.DependencyNode;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilder;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilderException;
import org.apache.maven.shared.dependency.tree.filter.AncestorOrSelfDependencyNodeFilter;
import org.apache.maven.shared.dependency.tree.filter.AndDependencyNodeFilter;
import org.apache.maven.shared.dependency.tree.filter.ArtifactDependencyNodeFilter;
import org.apache.maven.shared.dependency.tree.filter.DependencyNodeFilter;
import org.apache.maven.shared.dependency.tree.filter.StateDependencyNodeFilter;
import org.apache.maven.shared.dependency.tree.traversal.BuildingDependencyNodeVisitor;
import org.apache.maven.shared.dependency.tree.traversal.CollectingDependencyNodeVisitor;
import org.apache.maven.shared.dependency.tree.traversal.DependencyNodeVisitor;
import org.apache.maven.shared.dependency.tree.traversal.FilteringDependencyNodeVisitor;
import org.apache.maven.shared.dependency.tree.traversal.SerializingDependencyNodeVisitor;
import org.apache.maven.shared.dependency.tree.traversal.SerializingDependencyNodeVisitor.TreeTokens;

/**
 * Displays the dependency tree for this project.
 *
 * @author <a href="mailto:markhobson@gmail.com">Mark Hobson</a>
 * @version $Id$
 * @since 2.0-alpha-5
 * @goal tree
 * @requiresDependencyResolution test
 */
public class TreeMojo extends AbstractMojo
{
    // fields -----------------------------------------------------------------

    /**
     * The Maven project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * The artifact repository to use.
     *
     * @parameter expression="${localRepository}"
     * @required
     * @readonly
     */
    private ArtifactRepository localRepository;

    /**
     * The artifact factory to use.
     *
     * @component
     * @required
     * @readonly
     */
    private ArtifactFactory artifactFactory;

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
     * The dependency tree builder to use.
     *
     * @component
     * @required
     * @readonly
     */
    private DependencyTreeBuilder dependencyTreeBuilder;

    /**
     * If specified, this parameter will cause the dependency tree to be written to the path specified, instead of
     * writing to the console.
     * @deprecated use outputFile instead.
     * @parameter expression="${output}"
     */
    private File output;

    /**
     * If specified, this parameter will cause the dependency tree to be written to the path specified, instead of
     * writing to the console.
     * @parameter expression="${outputFile}"
     * @since 2.0-alpha-5
     */
    private File outputFile;

    /**
     * If specified, this parameter will cause the dependency tree to be written using the specified format. Currently
     * supported format are text, dot, graphml and tgf.
     *
     * These formats can be plotted to image files. An example of how to plot a dot file using
     * pygraphviz can be found <a href="http://networkx.lanl.gov/pygraphviz/tutorial.html#layout-and-drawing">here</a>
     *
     * @parameter expression="${outputType}" default-value="text"
     * @since 2.1
     */
    private String outputType;

    /**
     * The scope to filter by when resolving the dependency tree, or <code>null</code> to include dependencies from
     * all scopes. Note that this feature does not currently work due to MNG-3236.
     *
     * @since 2.0-alpha-5
     * @see <a href="http://jira.codehaus.org/browse/MNG-3236">MNG-3236</a>
     *
     * @parameter expression="${scope}"
     */
    private String scope;

    /**
     * Whether to include omitted nodes in the serialized dependency tree.
     *
     * @since 2.0-alpha-6
     *
     * @parameter expression="${verbose}" default-value="false"
     */
    private boolean verbose;

    /**
     * The token set name to use when outputting the dependency tree. Possible values are <code>whitespace</code>,
     * <code>standard</code> or <code>extended</code>, which use whitespace, standard or extended ASCII sets
     * respectively.
     *
     * @since 2.0-alpha-6
     *
     * @parameter expression="${tokens}" default-value="standard"
     */
    private String tokens;

    /**
     * A comma-separated list of artifacts to filter the serialized dependency tree by, or <code>null</code> not to
     * filter the dependency tree. The artifact syntax is defined by <code>StrictPatternIncludesArtifactFilter</code>.
     *
     * @see StrictPatternIncludesArtifactFilter
     * @since 2.0-alpha-6
     *
     * @parameter expression="${includes}"
     */
    private String includes;

    /**
     * A comma-separated list of artifacts to filter from the serialized dependency tree, or <code>null</code> not to
     * filter any artifacts from the dependency tree. The artifact syntax is defined by
     * <code>StrictPatternExcludesArtifactFilter</code>.
     *
     * @see StrictPatternExcludesArtifactFilter
     * @since 2.0-alpha-6
     *
     * @parameter expression="${excludes}"
     */
    private String excludes;

    /**
     * Runtime Information used to check the Maven version
     * @since 2.0
     * @component role="org.apache.maven.execution.RuntimeInformation"
     */
    private RuntimeInformation rti;

    /**
     * The computed dependency tree root node of the Maven project.
     */
    private DependencyNode rootNode;
    
       /**
        * Whether to append outputs into the output file or overwrite it.
        * 
        * @parameter expression="${appendOutput}" default-value="false"
        * @since 2.2
        */
       private boolean appendOutput;
    
    // Mojo methods -----------------------------------------------------------

    /*
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    public void execute() throws MojoExecutionException, MojoFailureException
    {

        ArtifactVersion detectedMavenVersion = rti.getApplicationVersion();
        VersionRange vr;
        try
        {
            vr = VersionRange.createFromVersionSpec( "[2.0.8,)" );
            if ( !containsVersion( vr, detectedMavenVersion ) )
            {
                getLog().warn(
                               "The tree mojo requires at least Maven 2.0.8 to function properly. You may get eroneous results on earlier versions" );
            }
        }
        catch ( InvalidVersionSpecificationException e )
        {
            throw new MojoExecutionException( e.getLocalizedMessage() );
        }


        if ( output != null )
        {
            getLog().warn( "The parameter output is deprecated. Use outputFile instead." );
            this.outputFile = output;
        }

        ArtifactFilter artifactFilter = createResolvingArtifactFilter();

        try
        {
            // TODO: note that filter does not get applied due to MNG-3236

            rootNode =
                dependencyTreeBuilder.buildDependencyTree( project, localRepository, artifactFactory,
                                                           artifactMetadataSource, artifactFilter, artifactCollector );

            String dependencyTreeString = serializeDependencyTree( rootNode );

            if ( outputFile != null )
            {
                DependencyUtil.write( dependencyTreeString, outputFile, this.appendOutput ,getLog() );

                getLog().info( "Wrote dependency tree to: " + outputFile );
            }
            else
            {
                DependencyUtil.log( dependencyTreeString, getLog() );
            }
        }
        catch ( DependencyTreeBuilderException exception )
        {
            throw new MojoExecutionException( "Cannot build project dependency tree", exception );
        }
        catch ( IOException exception )
        {
            throw new MojoExecutionException( "Cannot serialise project dependency tree", exception );
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
     * Gets the computed dependency tree root node for the Maven project.
     *
     * @return the dependency tree root node
     */
    public DependencyNode getDependencyTree()
    {
        return rootNode;
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
     * @param rootNode
     *            the dependency tree root node to serialize
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

            DependencyNodeFilter secondPassFilter = new AncestorOrSelfDependencyNodeFilter( collectingVisitor.getNodes() );
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
            return new DOTDependencyNodeVisitor( writer ) ;
        }
        else
        {
            return new SerializingDependencyNodeVisitor( writer, toTreeTokens( tokens ) );
        }
    }

    /**
     * Gets the tree tokens instance for the specified name.
     *
     * @param tokens
     *            the tree tokens name
     * @return the <code>TreeTokens</code> instance
     */
    private TreeTokens toTreeTokens( String tokens )
    {
        TreeTokens treeTokens;

        if ( "whitespace".equals( tokens ) )
        {
            getLog().debug( "+ Using whitespace tree tokens" );

            treeTokens = SerializingDependencyNodeVisitor.WHITESPACE_TOKENS;
        }
        else if ( "extended".equals( tokens ) )
        {
            getLog().debug( "+ Using extended tree tokens" );

            treeTokens = SerializingDependencyNodeVisitor.EXTENDED_TOKENS;
        }
        else
        {
            treeTokens = SerializingDependencyNodeVisitor.STANDARD_TOKENS;
        }

        return treeTokens;
    }

    /**
     * Gets the dependency node filter to use when serializing the dependency tree.
     *
     * @return the dependency node filter, or <code>null</code> if none required
     */
    private DependencyNodeFilter createDependencyNodeFilter()
    {
        List filters = new ArrayList();

        // filter node states
        if ( !verbose )
        {
            getLog().debug( "+ Filtering omitted nodes from dependency tree" );

            filters.add( StateDependencyNodeFilter.INCLUDED );
        }

        // filter includes
        if ( includes != null )
        {
            List patterns = Arrays.asList( includes.split( "," ) );

            getLog().debug( "+ Filtering dependency tree by artifact include patterns: " + patterns );

            ArtifactFilter artifactFilter = new StrictPatternIncludesArtifactFilter( patterns );
            filters.add( new ArtifactDependencyNodeFilter( artifactFilter ) );
        }

        // filter excludes
        if ( excludes != null )
        {
            List patterns = Arrays.asList( excludes.split( "," ) );

            getLog().debug( "+ Filtering dependency tree by artifact exclude patterns: " + patterns );

            ArtifactFilter artifactFilter = new StrictPatternExcludesArtifactFilter( patterns );
            filters.add( new ArtifactDependencyNodeFilter( artifactFilter ) );
        }

        return filters.isEmpty() ? null : new AndDependencyNodeFilter( filters );
    }

    //following is required because the version handling in maven code
    //doesn't work properly. I ripped it out of the enforcer rules.



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
        boolean matched = false;
        ArtifactVersion recommendedVersion = allowedRange.getRecommendedVersion();
        if ( recommendedVersion == null )
        {

            for ( Iterator i = allowedRange.getRestrictions().iterator(); i.hasNext() && !matched; )
            {
                Restriction restriction = (Restriction) i.next();
                if ( restriction.containsVersion( theVersion ) )
                {
                    matched = true;
                }
            }
        }
        else
        {
            // only singular versions ever have a recommendedVersion
            int compareTo = recommendedVersion.compareTo( theVersion );
            matched = ( compareTo <= 0 );
        }
        return matched;
    }

}
