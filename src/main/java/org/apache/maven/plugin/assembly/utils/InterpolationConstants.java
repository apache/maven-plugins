package org.apache.maven.plugin.assembly.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class InterpolationConstants
{

    public static final List<String> PROJECT_PREFIXES;

    public static final List<String> PROJECT_PROPERTIES_PREFIXES;

    static
    {
        final List<String> projectPrefixes = new ArrayList<String>();
        projectPrefixes.add( "pom." );
        projectPrefixes.add( "project." );

        PROJECT_PREFIXES = Collections.unmodifiableList( projectPrefixes );

        final List<String> projectPropertiesPrefixes = new ArrayList<String>();

        projectPropertiesPrefixes.add( "pom.properties." );
        projectPropertiesPrefixes.add( "project.properties." );

        PROJECT_PROPERTIES_PREFIXES = Collections.unmodifiableList( projectPropertiesPrefixes );
    }

    private InterpolationConstants()
    {
    }

}
