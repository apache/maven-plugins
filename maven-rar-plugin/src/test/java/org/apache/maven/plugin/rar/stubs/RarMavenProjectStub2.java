package org.apache.maven.plugin.rar.stubs;

import java.io.File;

public class RarMavenProjectStub2
    extends RarMavenProjectStub
{
    public File getFile()
    {
        return new File( getBasedir(), "src/test/resources/unit/basic-rar-with-descriptor/plugin-config.xml" );
    }
}
