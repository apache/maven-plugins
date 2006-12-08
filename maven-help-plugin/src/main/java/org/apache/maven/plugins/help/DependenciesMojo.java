/*
 * Copyright 2006 The Apache Software Foundation.
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
package org.apache.maven.plugins.help;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.Iterator;

import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactCollector;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.dependency.tree.DependencyNode;
import org.apache.maven.shared.dependency.tree.DependencyTree;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilder;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilderException;

/**
 * Displays the dependency tree for this project.
 * 
 * @author <a href="mailto:markhobson@gmail.com">Mark Hobson</a>
 * @version $Id$
 * @goal dependencies
 * @requiresDependencyResolution test
 */
public class DependenciesMojo extends AbstractMojo
{
    // constants --------------------------------------------------------------

    /**
     * The indentation string to use when serialising the dependency tree.
     */
    private static final String INDENT = "   ";

    /**
     * The newline string to use when serialising the dependency tree.
     */
    private static final String NEWLINE = System.getProperty( "line.separator" );

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
     * The artifact respository to use.
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
     */
    private ArtifactFactory artifactFactory;

    /**
     * The artifact metadata source to use.
     * 
     * @component
     */
    private ArtifactMetadataSource artifactMetadataSource;

    /**
     * The artifact collector to use.
     * 
     * @component
     */
    private ArtifactCollector artifactCollector;

    /**
     * The dependency tree builder to use.
     * 
     * @component
     */
    private DependencyTreeBuilder dependencyTreeBuilder;

    /**
     * If specified, this parameter will cause the dependency tree to be written to the path specified, instead of
     * writing to the console.
     * 
     * @parameter expression="${output}"
     */
    private File output;

    // Mojo methods -----------------------------------------------------------

    /*
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        try
        {
            DependencyTree dependencyTree =
                dependencyTreeBuilder.buildDependencyTree( project, localRepository, artifactFactory,
                                                           artifactMetadataSource, artifactCollector );

            String dependencyTreeString = serialiseDependencyTree( dependencyTree );

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

    // private methods --------------------------------------------------------

    /**
     * Serialises the specified dependency tree to a string.
     * 
     * @param tree
     *            the dependency tree to serialise
     * @return the serialised dependency tree
     */
    private String serialiseDependencyTree( DependencyTree tree )
    {
        StringBuffer buffer = new StringBuffer();

        serialiseDependencyNode( tree.getRootNode(), buffer );

        return buffer.toString();
    }

    /**
     * Serialises the specified dependency node and it's children to the specified string buffer.
     * 
     * @param node
     *            the dependency node to log
     * @param buffer
     *            the string buffer to serialise to
     */
    private void serialiseDependencyNode( DependencyNode node, StringBuffer buffer )
    {
        // serialise node

        for ( int i = 0; i < node.getDepth(); i++ )
        {
            buffer.append( INDENT );
        }

        buffer.append( node.getArtifact() ).append( NEWLINE );

        // serialise children

        for ( Iterator iterator = node.getChildren().iterator(); iterator.hasNext(); )
        {
            DependencyNode child = (DependencyNode) iterator.next();

            serialiseDependencyNode( child, buffer );
        }
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
