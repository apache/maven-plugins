package org.apache.maven.plugin.jxr.stubs;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Model;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author <a href="mailto:dennisl@apache.org">Dennis Lundberg</a>
 */
public class ExcludeConfigurationMavenProjectStub extends MavenProjectStub
{
    List reportPlugins = new ArrayList();

    public ExcludeConfigurationMavenProjectStub()
    {
        MavenXpp3Reader pomReader = new MavenXpp3Reader();
        Model model = null;

        try
        {
            model = pomReader.read( new FileReader( new File( getBasedir() +
                "/src/test/resources/unit/exclude-configuration/exclude-configuration-plugin-config.xml" ) ) );
            setModel( model );
        }
        catch ( Exception e )
        {

        }

        setArtifactId( model.getArtifactId() );
        setGroupId( model.getGroupId() );
        setVersion( model.getVersion() );
        setPackaging( model.getPackaging() );
        setInceptionYear( model.getInceptionYear() );

        String basedir = getBasedir().getAbsolutePath();
        List compileSourceRoots = new ArrayList();
        compileSourceRoots.add( basedir + "/src/test/resources/unit/exclude-configuration/exclude/configuration" );
        setCompileSourceRoots( compileSourceRoots );

        // set the report plugins
        List reportPlugins = new ArrayList();
        for ( Iterator iter = model.getReporting().getPlugins().iterator(); iter.hasNext(); )
        {
            ReportPlugin plugin = (ReportPlugin) iter.next();
            reportPlugins.add( plugin );
        }
        setReportPlugins( reportPlugins );

        Artifact artifact = new JxrPluginArtifactStub( getGroupId(), getArtifactId(), getVersion(), getPackaging() );
        artifact.setArtifactHandler( new DefaultArtifactHandlerStub() );
        setArtifact( artifact );

    }

    public void setReportPlugins( List plugins )
    {
        this.reportPlugins = plugins;
    }

    public List getReportPlugins()
    {
        return reportPlugins;
    }
}
