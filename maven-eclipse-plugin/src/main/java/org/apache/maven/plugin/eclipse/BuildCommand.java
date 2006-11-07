/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.apache.maven.plugin.eclipse;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.XMLWriter;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 *
 * @author <a href="mailto:kenneyw@neonics.com">Kenney Westerhof</a>
 * @author Jochen Kuhnle
 */
public class BuildCommand
{
    /** Builder name */
    private String name;

    /** Trigger names (comma-delimited list) */
    private String triggers;

    /** Argument map */
    private Map arguments;

    /**
     * Creates a new build command
     *
     * @param name Command name
     */
    public BuildCommand( String name )
    {
        this( name, null );
    }

    public BuildCommand( String name, Map arguments )
    {
        this.name = name;
        this.arguments = arguments;
    }

    public BuildCommand( String name, String argName, String argValue )
    {
        this.name = name;
        arguments = new Properties();
        arguments.put( argName, argValue );
    }

    /**
     * Creates a new build command
     *
     * @param name Command name
     * @param triggers Command triggers
     * @param arguments Command arguments
     */
    public BuildCommand( String name, String triggers, Map arguments )
    {
        if ( name == null )
        {
            throw new IllegalArgumentException( "Name must not be null." );
        }

        this.name = name;
        this.triggers = triggers;

        if ( arguments == null )
        {
            this.arguments = new HashMap();
        }
        else
        {
            this.arguments = new HashMap( arguments );
        }

    }

    /**
     * Creates a new build command from a DOM subtree
     * <p>
     * The subtree must represent a &lt;buildCommand&gt; section from an Eclipse .project file
     *
     * @param node DOM node
     */
    public BuildCommand( Xpp3Dom node )
    {
        Xpp3Dom nameNode = node.getChild( "name" );

        if ( nameNode == null )
        {
            throw new IllegalArgumentException( "No name node." );
        }

        name = nameNode.getValue();

        Xpp3Dom triggersNode = node.getChild( "triggers" );

        if ( triggersNode != null )
        {
            triggers = triggersNode.getValue();
        }

        Xpp3Dom argumentsNode = node.getChild( "arguments" );

        arguments = new HashMap();

        if ( argumentsNode != null )
        {
            for ( int i = 0; i < argumentsNode.getChildCount(); ++i )
            {
                Xpp3Dom entry = argumentsNode.getChild( i );

                if ( entry.getName().equals( "dictionary" ) )
                {
                    Xpp3Dom key = entry.getChild( "key" );
                    Xpp3Dom value = entry.getChild( "value" );

                    if ( key != null && value != null )
                    {
                        this.arguments.put( key.getValue(), value.getValue() );
                    }
                    else
                    {
                        // TODO: log warning about illegal key/value pair
                    }
                }
                else
                {
                    // TODO: log warning about unknown argument tag
                }
            }
        }
    }

    public void print( XMLWriter writer )
    {
        writer.startElement( "buildCommand" );
        writer.startElement( "name" );
        writer.writeText( name );
        writer.endElement();

        if ( !StringUtils.isEmpty( triggers ) )
        {
            writer.startElement( "triggers" );
            writer.writeText( triggers );
            writer.endElement();
        }

        if ( arguments != null && !arguments.isEmpty() )
        {
            writer.startElement( "arguments" );

            writer.startElement( "dictionary" );

            for ( Iterator it = arguments.keySet().iterator(); it.hasNext(); )
            {
                String key = (String) it.next();

                writer.startElement( "key" );
                writer.writeText( key );
                writer.endElement();

                writer.startElement( "value" );
                writer.writeText( (String) arguments.get( key ) );
                writer.endElement();
            }

            writer.endElement();

            writer.endElement();
        }

        writer.endElement();
    }

    public boolean equals( Object obj )
    {
        if ( obj instanceof BuildCommand )
        {
            BuildCommand b = (BuildCommand) obj;
            return name.equals( b.name ) && ( triggers == null ? b.triggers == null : triggers.equals( b.triggers ) )
                && ( arguments == null ? b.arguments == null : arguments.equals( b.arguments ) );
        }
        else
        {
            return false;
        }
    }

    public int hashCode()
    {
        return name.hashCode() + ( triggers == null ? 0 : 13 * triggers.hashCode() )
            + ( arguments == null ? 0 : 17 * arguments.hashCode() );
    }
}
