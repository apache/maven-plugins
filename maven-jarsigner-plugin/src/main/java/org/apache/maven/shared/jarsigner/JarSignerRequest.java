package org.apache.maven.shared.jarsigner;

import java.io.File;

/**
 * Specifies the common parameters used to control a JarSigner tool invocation.
 *
 * @author tchemit <chemit@codelutin.com>
 * @version $Id$
 * @since 1.0
 */
public interface JarSignerRequest
{
    boolean isVerbose();

    String getMaxMemory();

    String[] getArguments();

    File getWorkingDirectory();

    File getArchive();

    void setVerbose( boolean verbose );

    void setMaxMemory( String maxMemory );

    void setArguments( String[] arguments );

    void setWorkingDirectory( File workingDirectory );

    void setArchive( File archive );
}
