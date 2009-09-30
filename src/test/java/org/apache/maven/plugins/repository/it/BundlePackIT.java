package org.apache.maven.plugins.repository.it;

import static org.apache.maven.plugins.repository.it.support.IntegrationTestUtils.getTestDir;
import static org.apache.maven.plugins.repository.testutil.Assertions.assertZipContents;

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.apache.maven.plugins.repository.it.support.IntegrationTestUtils;
import org.apache.maven.plugins.repository.testutil.Assertions;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BundlePackIT
{

    @SuppressWarnings( "unchecked" )
    @Test
    public void run()
        throws IOException, URISyntaxException, VerificationException
    {
        File dir = getTestDir( "bundle-pack" );
        
        File bundleSource = new File( dir, "junit-4.5-bundle.jar" );

        if ( bundleSource.exists() )
        {
            bundleSource.delete();
        }
        
        Verifier verifier = new Verifier( dir.getAbsolutePath() );
        
        verifier.setAutoclean( false );

        List<String> cliOptions = verifier.getCliOptions();
        cliOptions.add( "-DgroupId=junit" );
        cliOptions.add( "-DartifactId=junit" );
        cliOptions.add( "-Dversion=4.5" );
        cliOptions.add( "-DscmUrl=http://foo" );
        cliOptions.add( "-DscmConnection=scm:svn:http://foo" );

        String prefix = IntegrationTestUtils.getCliPluginPrefix();

        verifier.executeGoal( prefix + "bundle-pack" );

        Set<String> requiredEntries = new HashSet<String>();
        requiredEntries.add( "pom.xml" );
        requiredEntries.add( "junit-4.5.jar" );
        
        if ( !verifier.isMavenDebug() )
        {
            bundleSource.deleteOnExit();
        }

        assertZipContents( requiredEntries, Assertions.EMPTY_ENTRY_NAMES, bundleSource );
    }

}
