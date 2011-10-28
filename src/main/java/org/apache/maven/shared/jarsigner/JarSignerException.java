package org.apache.maven.shared.jarsigner;

/**
 * Signals an error during the construction of the command line used to invoke jar signer, e.g. illegal invocation arguments.
 * This should not be confused with a failure of the invoked JarSigner build itself which will be reported by means of a
 * non-zero exit code.
 *
 * @author tchemit <chemit@codelutin.com>
 * @version $Id$
 * @see JarSignerResult#getExitCode()
 * @since 1.0
 */
public class JarSignerException
    extends Exception
{

    private static final long serialVersionUID = 1L;

    /**
     * Creates a new exception using the specified detail message and cause.
     *
     * @param message The detail message for this exception, may be <code>null</code>.
     * @param cause   The nested exception, may be <code>null</code>.
     */
    public JarSignerException( String message, Throwable cause )
    {
        super( message, cause );
    }

    /**
     * Creates a new exception using the specified detail message.
     *
     * @param message The detail message for this exception, may be <code>null</code>.
     */
    public JarSignerException( String message )
    {
        super( message );
    }

}
