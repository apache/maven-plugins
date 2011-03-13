package org.apache.maven.plugin.dependency.treeSerializers ;

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
import java.util.List;

import org.apache.maven.shared.dependency.tree.DependencyNode;
import org.apache.maven.shared.dependency.tree.traversal.DependencyNodeVisitor;

/**
 * A dependency node visitor that serializes visited nodes to a writer using the TGF format.
 *
 * http://en.wikipedia.org/wiki/Trivial_Graph_Format
 *
 * @author <a href="mailto:jerome.creignou@gmail.com">Jerome Creignou</a>
 * @since 2.1
 */
public class TGFDependencyNodeVisitor extends AbstractSerializingVisitor
                                      implements DependencyNodeVisitor
{

    /**
     * Utiity class to write an Edge.
     *
     * @author <a href="mailto:jerome.creignou@gmail.com">Jerome Creignou</a>
     */
    final static class EdgeAppender
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
        public String toString()
        {
            StringBuffer result = new StringBuffer( generateId( from ) );
            result.append( ' ' ).append( generateId( to ) );
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
     * Constructor.
     *
     * @param writer the writer to write to.
     */
    public TGFDependencyNodeVisitor( Writer writer )
    {
        super( writer );
    }

    /**
     * {@inheritDoc}
     */
    public boolean endVisit( DependencyNode node )
    {
        if ( node.getParent() == null || node.getParent() == node )
        {
            // dump edges on last node endVisit
            writer.println( "#" );
            for ( EdgeAppender edge : edges )
            {
                writer.println( edge.toString() );
            }
        }
        else
        {
            DependencyNode p = node.getParent();
            // using scope as edge label.
            edges.add( new EdgeAppender( p, node, node.getArtifact().getScope() ) );
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public boolean visit( DependencyNode node )
    {
        // write node
        writer.write( generateId( node ) );
        writer.write( " " );
        writer.println( node.toNodeString() );
        return true;
    }

    /**
     * Generate a unique id from a DependencyNode.
     * <p>
     * Current implementation is rather simple and uses hashcode.
     * </p>
     *
     * @param node the DependencyNode to use.
     * @return the unique id.
     */
    private static String generateId( DependencyNode node )
    {
        return String.valueOf( node.hashCode() );
    }
}
