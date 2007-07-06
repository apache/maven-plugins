package org.codehaus.mojo.shade.relocation;

import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

/** @author Jason van Zyl */
public class SimpleRelocator
    implements Relocator
{
    private String pattern;

    private List excludes;

    public SimpleRelocator( String pattern,
                            List excludes )
    {
        this.pattern = pattern.replace( '.', '/' );

        if ( excludes != null )
        {
            this.excludes = new ArrayList();

            for ( Iterator i = excludes.iterator(); i.hasNext(); )
            {
                String e = (String) i.next();

                this.excludes.add( e.replace( '.', '/' ) );
            }
        }
    }

    public boolean canRelocate( String clazz )
    {
        if ( excludes != null )
        {
            for ( Iterator i = excludes.iterator(); i.hasNext(); )
            {
                String exclude = (String) i.next();

                // Remember we have converted "." -> "/" in the constructor. So ".*" is really "/*"
                if ( exclude.endsWith( "/*" ) && clazz.startsWith( exclude.substring( 0, exclude.length() - 2 ) ) )
                {
                    return false;
                }
                else if ( clazz.equals( exclude ) )
                {
                    return false;
                }
            }
        }

        return clazz.startsWith( pattern );
    }

    public String relocate( String clazz )
    {
        return "hidden/" + clazz;
    }
}
