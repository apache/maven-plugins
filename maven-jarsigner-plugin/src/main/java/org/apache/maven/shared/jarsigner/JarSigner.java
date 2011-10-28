package org.apache.maven.shared.jarsigner;

/**
 * Provides a facade to invoke JarSigner tool.
 *
 * @author tchemit <chemit@codelutin.com>
 * @version $Id$
 * @since 1.0
 */
public interface JarSigner
{

    /**
     * Executes JarSigner tool using the parameters specified by the given invocation request.
     *
     * @param request The invocation request to execute, must not be <code>null</code>.
     * @return The result of the JarSigner invocation, never <code>null</code>.
     * @throws JarSignerException if something fails while init the command
     */
    JarSignerResult execute( JarSignerRequest request )
        throws JarSignerException;

}
