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

import org.apache.maven.shared.dependency.tree.DependencyNode;
import org.apache.maven.shared.dependency.tree.traversal.DependencyNodeVisitor;

/**
 * A dependency node visitor that serializes visited nodes to a writer using the graphml format.
 * {@link http://graphml.graphdrawing.org/}
 *
 * @author <a href="mailto:jerome.creignou@gmail.com">Jerome Creignou</a>
 * @since 2.1
 */
public class GraphmlDependencyNodeVisitor extends AbstractSerializingVisitor
                                          implements DependencyNodeVisitor
{

    /**
     * Graphml xml file header. Define Schema and root element. We also define 2 key as meta data.
     */
    private static final String GRAPHML_HEADER =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?> "
            + "<graphml xmlns=\"http://graphml.graphdrawing.org/xmlns\" "
            + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
            + "xmlns:y=\"http://www.yworks.com/xml/graphml\" "
            + "xsi:schemaLocation=\"http://graphml.graphdrawing.org/xmlns "
            + "http://graphml.graphdrawing.org/xmlns/1.0/graphml.xsd\">\n"
            + "  <key for=\"node\" id=\"d0\" yfiles.type=\"nodegraphics\"/> \n"
            + "  <key for=\"edge\" id=\"d1\" yfiles.type=\"edgegraphics\"/> \n"
            + "<graph id=\"dependencies\" edgedefault=\"directed\">\n";

    /**
     * Graphml xml file footer.
     */
    private static final String GRAPHML_FOOTER = "</graph></graphml>";

    /**
     * Constructor.
     *
     * @param writer the writer to write to.
     */
    public GraphmlDependencyNodeVisitor( Writer writer )
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
            writer.write( GRAPHML_FOOTER );
        }
        else
        {
            DependencyNode p = node.getParent();
            writer.print( "<edge source=\"" + generateId( p ) + "\" target=\"" + generateId( node ) + "\">" );
            if ( node.getArtifact().getScope() != null )
            {
                // add Edge label
                writer.print( "<data key=\"d1\"><y:PolyLineEdge><y:EdgeLabel>" + node.getArtifact().getScope()
                    + "</y:EdgeLabel></y:PolyLineEdge></data>" );
            }
            writer.println( "</edge>" );
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public boolean visit( DependencyNode node )
    {
        if ( node.getParent() == null || node.getParent() == node )
        {
            writer.write( GRAPHML_HEADER );
        }
        // write node
        writer.print( "<node id=\"" + generateId( node ) + "\">" );
        // add node label
        writer.print( "<data key=\"d0\"><y:ShapeNode><y:NodeLabel>" + node.toNodeString()
            + "</y:NodeLabel></y:ShapeNode></data>" );
        writer.println( "</node>" );
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
