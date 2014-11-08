package org.apache.maven.report.projectinfo.dependencies;

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

import java.util.List;

import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.shared.dependency.tree.DependencyNode;
import org.apache.maven.shared.dependency.tree.traversal.DependencyNodeVisitor;

/**
 * A dependency node visitor that serializes visited nodes to a sink writer. It's used to serialize tree in project
 * information report page.
 *
 * @author <a href="mailto:wangyf2010@gmail.com">Simon Wang</a>
 */
public class SinkSerializingDependencyNodeVisitor
    implements DependencyNodeVisitor
{
    // classes ----------------------------------------------------------------

    /**
     * Provides tokens to use when serializing the dependency tree.
     */
    private class TreeTokens
    {
        private final Sink sink;

        public TreeTokens( Sink sink )
        {
            this.sink = sink;
        }

        public void addNodeIndent( boolean last )
        {
            if ( last )
            {
                sink.text( "\\-" );
                sink.nonBreakingSpace();
            }
            else
            {
                sink.text( "+-" );
                sink.nonBreakingSpace();
            }
        }

        public void fillIndent( boolean last )
        {
            if ( last )
            {
                sink.nonBreakingSpace();
                sink.nonBreakingSpace();
                sink.nonBreakingSpace();
            }
            else
            {
                sink.text( "|" );
                sink.nonBreakingSpace();
                sink.nonBreakingSpace();
            }
        }
    }

    // fields -----------------------------------------------------------------

    /**
     * The writer to serialize to.
     */
    private final Sink sink;

    /**
     * The tokens to use when serializing the dependency tree.
     */
    private final TreeTokens tokens;

    /**
     * The depth of the currently visited dependency node.
     */
    private int depth;

    // constructors -----------------------------------------------------------

    /**
     * Creates a dependency node visitor that serializes visited nodes to the specified writer using the specified
     * tokens.
     *
     * @param sink the writer to serialize to
     */
    public SinkSerializingDependencyNodeVisitor( Sink sink )
    {
        this.sink = sink;
        this.tokens = new TreeTokens( sink );
        depth = 0;
    }

    // DependencyNodeVisitor methods ------------------------------------------

    /**
     * {@inheritDoc}
     */
    public boolean visit( DependencyNode node )
    {
        indent( node );

        sink.text( node.toNodeString() );
        sink.lineBreak();

        depth++;

        return true;
    }

    /**
     * {@inheritDoc}
     */
    public boolean endVisit( DependencyNode node )
    {
        depth--;

        return true;
    }

    // private methods --------------------------------------------------------

    /**
     * Writes the necessary tokens to indent the specified dependency node to this visitor's writer.
     *
     * @param node the dependency node to indent
     */
    private void indent( DependencyNode node )
    {
        for ( int i = 1; i < depth; i++ )
        {
            tokens.fillIndent( isLast( node, i ) );
        }

        if ( depth > 0 )
        {
            tokens.addNodeIndent( isLast( node ) );
        }
    }

    /**
     * Gets whether the specified dependency node is the last of its siblings.
     *
     * @param node the dependency node to check
     * @return <code>true</code> if the specified dependency node is the last of its last siblings
     */
    private boolean isLast( DependencyNode node )
    {
        // TODO: remove node argument and calculate from visitor calls only

        DependencyNode parent = node.getParent();

        boolean last;

        if ( parent == null )
        {
            last = true;
        }
        else
        {
            List<DependencyNode> siblings = parent.getChildren();

            last = ( siblings.indexOf( node ) == siblings.size() - 1 );
        }

        return last;
    }

    /**
     * Gets whether the specified dependency node ancestor is the last of its siblings.
     *
     * @param node the dependency node whose ancestor to check
     * @param ancestorDepth the depth of the ancestor of the specified dependency node to check
     * @return <code>true</code> if the specified dependency node ancestor is the last of its siblings
     */
    private boolean isLast( DependencyNode node, int ancestorDepth )
    {
        // TODO: remove node argument and calculate from visitor calls only

        int distance = depth - ancestorDepth;

        while ( distance-- > 0 )
        {
            node = node.getParent();
        }

        return isLast( node );
    }
}
