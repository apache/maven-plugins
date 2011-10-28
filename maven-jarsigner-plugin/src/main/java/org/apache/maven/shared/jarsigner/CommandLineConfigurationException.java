package org.apache.maven.shared.jarsigner;

/**
 * Signals an error during the construction of the command line used to invoke jar signer.
 *
 * @author tchemit <chemit@codelutin.com>
 * @version $Id$
 * @since 1.0
 */
public class CommandLineConfigurationException
    extends Exception
{

    private static final long serialVersionUID = 1L;

    /**
     * Creates a new exception using the specified detail message and cause.
     *
     * @param message The detail message for this exception, may be <code>null</code>.
     * @param cause   The nested exception, may be <code>null</code>.
     */
    public CommandLineConfigurationException( String message, Throwable cause )
    {
        super( message, cause );
    }

    /**
     * Creates a new exception using the specified detail message.
     *
     * @param message The detail message for this exception, may be <code>null</code>.
     */
    public CommandLineConfigurationException( String message )
    {
        super( message );
    }

}

