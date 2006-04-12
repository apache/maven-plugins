package org.apache.maven.plugin.rar.stubs;

import java.io.File;

public class RarMavenProjectStub3
    extends RarMavenProjectStub
{
    public File getFile()
    {
        return new File( getBasedir(), "src/test/resources/unit/basic-rar-with-manifest/plugin-config.xml" );
    }
}
