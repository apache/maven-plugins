package org.apache.maven.plugin.idea.stubs;

import org.apache.maven.project.MavenProject;

import java.util.List;
import java.util.ArrayList;
import java.io.File;

/**
 * @author Edwin Punzalan
 */
public class MavenProjectWithModulesStub
    extends SimpleMavenProjectStub
{
    public List getCollectedProjects()
    {
        List projects = new ArrayList();

        projects.add( createModule( "module-1" ) );
        projects.add( createModule( "module-2" ) );
        projects.add( createModule( "module-3" ) );

        return projects;
    }

    private MavenProject createModule( String artifactId )
    {
        return new MavenProjectModuleStub( artifactId, new File( getBasedir(), artifactId ) );
    }
}
