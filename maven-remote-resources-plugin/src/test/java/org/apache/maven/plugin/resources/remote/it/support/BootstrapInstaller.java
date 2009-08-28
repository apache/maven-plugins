package org.apache.maven.plugin.resources.remote.it.support;

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

public class BootstrapInstaller
{
    
    private static boolean installed = false;
    
    public static void install()
        throws IOException, URISyntaxException, VerificationException
    {
        if ( !installed )
        {
            File bootstrapDir = TestUtils.getTestDir( "bootstrap" );
            
            Verifier verifier = new Verifier( bootstrapDir.getAbsolutePath() );
            
            verifier.executeGoal( "install" );
            
            verifier.verifyErrorFreeLog();
            verifier.resetStreams();
            
            installed = true;
        }
    }

}
