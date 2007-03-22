/**
 * 
 */
package org.apache.maven.plugin.enforcer;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.SystemUtils;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.StringUtils;

/**
 * @author brianf
 * 
 */
public class RequireJavaVersion
    extends AbstractVersionEnforcer
    implements EnforcementRule
{



    /*
     * (non-Javadoc)
     * 
     * @see org.apache.maven.plugin.enforcer.rules.EnforcementRule#execute()
     */
    public void execute( MavenSession session, Log log )
        throws MojoExecutionException
    {
        ArtifactVersion detectedJdkVersion = new DefaultArtifactVersion(
                                                                         fixJDKVersion( SystemUtils.JAVA_VERSION_TRIMMED ) );
        enforceVersion( log, "JDK", version, detectedJdkVersion );
    }

    /**
     * Converts a jdk string from 1.5.0-11 to a single 3 digit version like
     * 1.5.0
     */
    public static String fixJDKVersion( String theJdkVersion )
    {
        theJdkVersion = theJdkVersion.replaceAll( "_|-", "." );
        String tokenArray[] = StringUtils.split( theJdkVersion, "." );
        List tokens = Arrays.asList( tokenArray );
        StringBuffer buffer = new StringBuffer( theJdkVersion.length() );

        Iterator iter = tokens.iterator();
        for ( int i = 0; i < tokens.size() && i < 3; i++ )
        {
            buffer.append( iter.next() );
            buffer.append( '.' );
        }

        String version = buffer.toString();
        return StringUtils.stripEnd( version, "." );
    }
}
