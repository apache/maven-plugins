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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactCollector;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
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
     * 
     * @parameter expression="${output}"
     */
    private File output;

    /**
     * The computed dependency tree root node of the Maven project.
     */
    private DependencyNode rootNode;

    /**
     * The scope to filter by when resolving the dependency tree, or <code>null</code> to include dependencies from
     * all scopes.
     * 
     * @parameter expression="${scope}"
     */
    private String scope;

    /**
     * Whether to include omitted nodes in the serialized dependency tree.
     * 
     * @parameter expression="${verbose}" default-value="false"
     */
    private boolean verbose;

    /**
     * The token set name to use when outputting the dependency tree. Possible values are <code>whitespace</code>,
     * <code>standard</code> or <code>extended</code>, which use whitespace, standard or extended ASCII sets
     * respectively.
     * 
     * @parameter expression="${tokens}" default-value="standard"
     */
    private String tokens;

    /**
     * A comma-separated list of artifacts to filter the serialized dependency tree by, or <code>null</code> not to
     * filter the dependency tree. The artifact syntax is defined by <code>StrictPatternIncludesArtifactFilter</code>.
     * 
     * @see StrictPatternIncludesArtifactFilter
     * @parameter expression="${includes}"
     */
    private String includes;

    /**
     * A comma-separated list of artifacts to filter from the serialized dependency tree, or <code>null</code> not to
     * filter any artifacts from the dependency tree. The artifact syntax is defined by
     * <code>StrictPatternExcludesArtifactFilter</code>.
     * 
     * @see StrictPatternExcludesArtifactFilter
     * @parameter expression="${excludes}"
     */
    private String excludes;

    // Mojo methods -----------------------------------------------------------

    /*
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        ArtifactFilter artifactFilter = createResolvingArtifactFilter();

        try
        {
            // TODO: fix DependencyTreeBuilder to apply filter
            rootNode =
                dependencyTreeBuilder.buildDependencyTree( project, localRepository, artifactFactory,
                                                           artifactMetadataSource, artifactFilter, artifactCollector );

            String dependencyTreeString = serialiseDependencyTree( rootNode );

            if ( output != null )
            {
                write( dependencyTreeString, output );

                getLog().info( "Wrote dependency tree to: " + output );
            }
            else
            {
                log( dependencyTreeString );
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
     * Serialises the specified dependency tree to a string.
     * 
     * @param rootNode
     *            the dependency tree root node to serialise
     * @return the serialised dependency tree
     */
    private String serialiseDependencyTree( DependencyNode rootNode )
    {
        StringWriter writer = new StringWriter();
        TreeTokens treeTokens = toTreeTokens( tokens );

        DependencyNodeVisitor visitor = new SerializingDependencyNodeVisitor( writer, treeTokens );

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

    /**
     * Writes the specified string to the specified file.
     * 
     * @param string
     *            the string to write
     * @param file
     *            the file to write to
     * @throws IOException
     *             if an I/O error occurs
     */
    private void write( String string, File file ) throws IOException
    {
        output.getParentFile().mkdirs();

        FileWriter writer = null;

        try
        {
            writer = new FileWriter( output );

            writer.write( string );
        }
        finally
        {
            if ( writer != null )
            {
                try
                {
                    writer.close();
                }
                catch ( IOException exception )
                {
                    getLog().error( "Cannot close file", exception );
                }
            }
        }
    }

    /**
     * Writes the specified string to the log at info level.
     * 
     * @param string
     *            the string to write
     * @throws IOException
     *             if an I/O error occurs
     */
    private void log( String string ) throws IOException
    {
        BufferedReader reader = new BufferedReader( new StringReader( string ) );

        String line;

        while ( ( line = reader.readLine() ) != null )
        {
            getLog().info( line );
        }

        reader.close();
    }
}
