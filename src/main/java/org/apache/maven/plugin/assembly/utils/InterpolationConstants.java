package org.apache.maven.plugin.assembly.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class InterpolationConstants
{
    
    public static final List PROJECT_PREFIXES;
    
    public static final List PROJECT_PROPERTIES_PREFIXES;
    
    static
    {
        List projectPrefixes = new ArrayList();
        projectPrefixes.add( "pom." );
        projectPrefixes.add( "project." );
        
        PROJECT_PREFIXES = Collections.unmodifiableList( projectPrefixes );
        
        List projectPropertiesPrefixes = new ArrayList();
        
        projectPropertiesPrefixes.add( "pom.properties." );
        projectPropertiesPrefixes.add( "project.properties." );
        
        PROJECT_PROPERTIES_PREFIXES = Collections.unmodifiableList( projectPropertiesPrefixes );
    }

    private InterpolationConstants()
    {
    }

}
