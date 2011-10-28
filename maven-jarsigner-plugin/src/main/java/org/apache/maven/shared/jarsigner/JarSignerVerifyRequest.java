package org.apache.maven.shared.jarsigner;

/**
 * Specifies the parameters used to control a jar signer verify operation invocation.
 *
 * @author tchemit <chemit@codelutin.com>
 * @version $Id$
 * @since 1.0
 */
public class JarSignerVerifyRequest
    extends AbstractJarSignerRequest
{
    /**
     * See <a href="http://java.sun.com/javase/6/docs/technotes/tools/windows/jarsigner.html#Options">options</a>.
     */
    private boolean certs;

    public boolean isCerts()
    {
        return certs;
    }

    public void setCerts( boolean certs )
    {
        this.certs = certs;
    }
}
