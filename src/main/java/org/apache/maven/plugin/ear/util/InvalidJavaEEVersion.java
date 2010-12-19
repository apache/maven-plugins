package org.apache.maven.plugin.ear.util;

import org.apache.maven.plugin.MojoExecutionException;

/**
 * @author Stephane Nicoll
 */
public class InvalidJavaEEVersion extends MojoExecutionException {

    private final String invalidVersion;

    public InvalidJavaEEVersion( String message, String invalidVersion )
    {
        super( message );
        this.invalidVersion = invalidVersion;
    }

    public String getInvalidVersion()
    {
        return invalidVersion;
    }
}
