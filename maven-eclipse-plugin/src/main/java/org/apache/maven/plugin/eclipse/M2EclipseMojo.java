package org.apache.maven.plugin.eclipse;

import org.apache.maven.plugin.MojoExecutionException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @goal m2eclipse
 * @execute phase="generate-resources"
 */
public class M2EclipseMojo
    extends EclipsePlugin
{

    protected static final String M2ECLIPSE_NATURE = "org.maven.ide.eclipse.maven2Nature";
    protected static final String M2ECLIPSE_BUILD_COMMAND = "org.maven.ide.eclipse.maven2Builder";
    protected static final String M2ECLIPSE_CLASSPATH_CONTAINER = "org.maven.ide.eclipse.MAVEN2_CLASSPATH_CONTAINER";

    protected void setupExtras()
        throws MojoExecutionException
    {
        // disable normal dependency resolution; the m2eclipse plugin will handle it.
        setResolveDependencies( false );
        
        setAdditionalProjectnatures( new ArrayList( Collections.singletonList( M2ECLIPSE_NATURE ) ) );
        setAdditionalBuildcommands( new ArrayList( Collections.singletonList( M2ECLIPSE_BUILD_COMMAND ) ) );
        
        List classpathContainers = getClasspathContainers();
        if ( classpathContainers == null )
        {
            classpathContainers = new ArrayList();
            
            classpathContainers.add( COMMON_PATH_JDT_LAUNCHING_JRE_CONTAINER );

            if ( isPdeProject() )
            {
                classpathContainers.add( REQUIRED_PLUGINS_CONTAINER );
            }
        }
        
        classpathContainers.add( M2ECLIPSE_CLASSPATH_CONTAINER );
        
        setClasspathContainers( classpathContainers );
    }

}
