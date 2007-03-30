package org.apache.maven.plugin.assembly.utils;

import java.util.List;

public final class TypeConversionUtils
{
    
    private TypeConversionUtils()
    {
    }
    
    public static String[] toStringArray( List list )
    {
        String[] result = null;
        
        if ( list != null && !list.isEmpty() )
        {
            result = (String[]) list.toArray( new String[0] );
        }
        
        return result;
    }

}
