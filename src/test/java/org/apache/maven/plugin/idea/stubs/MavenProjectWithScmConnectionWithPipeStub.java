package org.apache.maven.plugin.idea.stubs;

import org.apache.maven.model.Scm;

/**
 * @author Dennis Lundberg
 */
public class MavenProjectWithScmConnectionWithPipeStub
    extends SimpleMavenProjectStub
{
    public Scm getScm()
    {
        Scm scm = new Scm();

        scm.setConnection( "scm:type|" );

        return scm;
    }
}
