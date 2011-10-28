package org.apache.maven.shared.jarsigner;

/**
 * Specifies the parameters used to control a jar signer sign operation invocation.
 *
 * @author tchemit <chemit@codelutin.com>
 * @version $Id$
 * @since 1.0
 */
public class JarSignerSignRequest
    extends AbstractJarSignerRequest
{
    /**
     * See <a href="http://java.sun.com/javase/6/docs/technotes/tools/windows/jarsigner.html#Options">options</a>.
     */
    private String keystore;

    /**
     * See <a href="http://java.sun.com/javase/6/docs/technotes/tools/windows/jarsigner.html#Options">options</a>.
     */
    private String storepass;

    /**
     * See <a href="http://java.sun.com/javase/6/docs/technotes/tools/windows/jarsigner.html#Options">options</a>.
     */
    private String keypass;

    /**
     * See <a href="http://java.sun.com/javase/6/docs/technotes/tools/windows/jarsigner.html#Options">options</a>.
     */
    private String sigfile;

    /**
     * See <a href="http://java.sun.com/javase/6/docs/technotes/tools/windows/jarsigner.html#Options">options</a>.
     */
    private String storetype;

    /**
     * See <a href="http://java.sun.com/javase/6/docs/technotes/tools/windows/jarsigner.html#Options">options</a>.
     */
    private String providerName;

    /**
     * See <a href="http://java.sun.com/javase/6/docs/technotes/tools/windows/jarsigner.html#Options">options</a>.
     */
    private String providerClass;

    /**
     * See <a href="http://java.sun.com/javase/6/docs/technotes/tools/windows/jarsigner.html#Options">options</a>.
     */
    private String providerArg;

    /**
     * See <a href="http://java.sun.com/javase/6/docs/technotes/tools/windows/jarsigner.html#Options">options</a>.
     */
    private String alias;

    public String getKeystore()
    {
        return keystore;
    }

    public String getStorepass()
    {
        return storepass;
    }

    public String getKeypass()
    {
        return keypass;
    }

    public String getSigfile()
    {
        return sigfile;
    }

    public String getStoretype()
    {
        return storetype;
    }

    public String getProviderName()
    {
        return providerName;
    }

    public String getProviderClass()
    {
        return providerClass;
    }

    public String getProviderArg()
    {
        return providerArg;
    }

    public String getAlias()
    {
        return alias;
    }

    public void setKeystore( String keystore )
    {
        this.keystore = keystore;
    }

    public void setStorepass( String storepass )
    {
        this.storepass = storepass;
    }

    public void setKeypass( String keypass )
    {
        this.keypass = keypass;
    }

    public void setSigfile( String sigfile )
    {
        this.sigfile = sigfile;
    }

    public void setStoretype( String storetype )
    {
        this.storetype = storetype;
    }

    public void setProviderName( String providerName )
    {
        this.providerName = providerName;
    }

    public void setProviderClass( String providerClass )
    {
        this.providerClass = providerClass;
    }

    public void setProviderArg( String providerArg )
    {
        this.providerArg = providerArg;
    }

    public void setAlias( String alias )
    {
        this.alias = alias;
    }
}
