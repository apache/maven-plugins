package org.apache.maven.plugins.dependency.tree ;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.shared.dependency.graph.DependencyNode;
import org.apache.maven.shared.dependency.graph.traversal.DependencyNodeVisitor;

/**
 * A dependency node visitor that serializes visited nodes to a writer using the TGF format.
 *
 * http://en.wikipedia.org/wiki/Trivial_Graph_Format
 *
 * @author <a href="mailto:jerome.creignou@gmail.com">Jerome Creignou</a>
 * @since 2.1
 */
public class TGFDependencyNodeVisitor
    extends AbstractSerializingVisitor
    implements DependencyNodeVisitor
{

    /**
     * Utiity class to write an Edge.
     *
     * @author <a href="mailto:jerome.creignou@gmail.com">Jerome Creignou</a>
     */
    static final class EdgeAppender
    {
        /**
         * Edge start.
         */
        private DependencyNode from;

        /**
         * Edge end.
         */
        private DependencyNode to;

        /**
         * Edge label. (optional)
         */
        private String label;

        /**
         * Build a new EdgeAppender.
         *
         * @param from edge start.
         * @param to edge end
         * @param label optional label.
         */
        public EdgeAppender( DependencyNode from, DependencyNode to, String label )
        {
            super();
            this.from = from;
            this.to = to;
            this.label = label;
        }

        /**
         * build a string representing the edge.
         */
        @Override
        public String toString()
        {
            return asString( false );
        }

        /**
         * build a string representing the edge.
         */
        public String asString( boolean mergeVersion )
        {
            StringBuilder result = new StringBuilder( generateId( from, mergeVersion ) );
            result.append( ' ' ).append( generateId( to, mergeVersion ) );
            if ( label != null )
            {
                result.append( ' ' ).append( label );
            }
            return result.toString();
        }
    }

    /**
     * List of edges.
     */
    private List<EdgeAppender> edges = new ArrayList<EdgeAppender>();

    /**
     * Set of node ids.
     */
    private Set<String> nodeIds = new HashSet<String>();

    /**
     * See {@link TreeMojo#outputScope}.
     */
    private boolean outputScope;

    /**
     * See {@link TreeMojo#outputMergeVersion}.
     */
    private boolean mergeVersion;

    /**
     * Constructor.
     *
     * @param writer the writer to write to.
     */
    public TGFDependencyNodeVisitor( Writer writer )
    {
        this( writer, true, false );
    }

    /**
     * Constructor.
     *
     * @param writer the writer to write to.
     * @param outputScope see {@link TreeMojo#outputScope}.
     * @param mergeVersion see {@link TreeMojo#outputMergeVersion}.
     */
    public TGFDependencyNodeVisitor( Writer writer, boolean outputScope, boolean mergeVersion )
    {
        super( writer );
        this.outputScope = outputScope;
        this.mergeVersion = mergeVersion;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean endVisit( DependencyNode node )
    {
        if ( node.getParent() == null || node.getParent() == node )
        {
            // dump edges on last node endVisit
            writer.println( "#" );
            for ( EdgeAppender edge : edges )
            {
                writer.println( edge.asString( mergeVersion ) );
            }
        }
        else
        {
            DependencyNode p = node.getParent();
            String label = null;
            if ( outputScope )
            {
                // using scope as edge label.
                label = node.getArtifact().getScope();
            }
            edges.add( new EdgeAppender( p, node, label ) );
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean visit( DependencyNode node )
    {
        // write node
        String nodeId = generateId( node, mergeVersion );
        if ( nodeIds.add( nodeId ) )
        {
            writer.write( nodeId );
            writer.write( " " );
            writer.println( mergeVersion ? nodeId : node.toNodeString() );
        }
        return true;
    }

    /**
     * Generate a unique id from a DependencyNode.
     *
     * @param node the DependencyNode to use.
     * @return the unique id.
     */
    private static String generateId( DependencyNode node, boolean mergeVersion )
    {
        Artifact artifact = node.getArtifact();
        String ret = artifact.getGroupId() + ":" + artifact.getArtifactId();
        if ( !mergeVersion )
        {
            ret += ":" + artifact.getVersion();
        }
        if ( artifact.getType() != null )
        {
            ret += ":" + artifact.getType();
        }
        if ( artifact.hasClassifier() )
        {
            ret += ":" + artifact.getClassifier();
        }
        return ret;
    }
}
