package org.apache.maven.plugin.dependency.treeSerializers;

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

import org.apache.maven.shared.dependency.tree.traversal.DependencyNodeVisitor;
import org.apache.maven.shared.dependency.tree.DependencyNode;

import java.io.Writer;
import java.util.List;

/**
 * A dependency node visitor that serializes visited nodes to DOT format
 * http://en.wikipedia.org/wiki/DOT_language
 *
 * @author <a href="mailto:pi.songs@gmail.com">Pi Song</a>
 * @since 2.1
 */
public class DOTDependencyNodeVisitor extends AbstractSerializingVisitor
                                      implements DependencyNodeVisitor {

    /**
     * Constructor.
     *
     * @param writer the writer to write to.
     */
    public DOTDependencyNodeVisitor( Writer writer)
    {
        super( writer );
    }


    /**
     * {@inheritDoc}
     */
    public boolean visit( DependencyNode node )
    {
        if ( node.getParent() == null || node.getParent() == node )
        {
            writer.write( "digraph \"" + node.toNodeString() + "\" { \n" );
        }

        // Generate "currentNode -> Child" lines

        List<DependencyNode> children = node.getChildren();

        for ( DependencyNode child : children )
        {
            StringBuffer sb = new StringBuffer();
            sb.append( "\t\"" );
            sb.append( node.toNodeString() );
            sb.append( "\" -> \"" );
            sb.append( child.toNodeString() );
            sb.append( "\" ; " );
            writer.println( sb.toString() );
        }

        return true;
    }

    /**
     * {@inheritDoc}
     */
    public boolean endVisit( DependencyNode node )
    {
        if ( node.getParent() == null || node.getParent() == node )
        {
            writer.write( " } " );
        }
        return true;
    }

}
