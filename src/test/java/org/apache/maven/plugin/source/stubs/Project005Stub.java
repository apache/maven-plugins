package org.apache.maven.plugin.source.stubs;

import org.apache.maven.plugin.testing.stubs.MavenProjectStub;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.artifact.Artifact;

import java.util.List;
import java.util.ArrayList;
import java.io.FileReader;
import java.io.File;

/**
 * @author <a href="mailto:oching@exist.com">Maria Odea Ching</a>
 */
public class Project005Stub
    extends MavenProjectStub
{
    private Build build;

    private List resources;

    private List testResources;

    public Project005Stub()
    {
        MavenXpp3Reader pomReader = new MavenXpp3Reader();
        Model model;

        try
        {
            model = pomReader.read(
                new FileReader( new File( getBasedir() + "/target/test-classes/unit/project-005/pom.xml" ) ) );
            setModel( model );

            setGroupId( model.getGroupId() );
            setArtifactId( model.getArtifactId() );
            setVersion( model.getVersion() );
            setName( model.getName() );
            setUrl( model.getUrl() );
            setPackaging( model.getPackaging() );

            Build build = new Build();
            build.setFinalName( getArtifactId() + "-" + getVersion() );
            build.setDirectory( getBasedir() + "/target/test/unit/project-005/target" );
            setBuild( build );

            Artifact artifact =
                new SourcePluginArtifactStub( getGroupId(), getArtifactId(), getVersion(), getPackaging(), null );
            setArtifact( artifact );

        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
    }

    public Build getBuild()
    {
        return build;
    }

    public void setBuild( Build build )
    {
        this.build = build;
    }

    public List getResources()
    {
        return resources;
    }

    public void setResources( List resources )
    {
        this.resources = resources;
    }

    public List getTestResources()
    {
        return testResources;
    }

    public void setTestResources( List testResources )
    {
        this.testResources = testResources;
    }

}
