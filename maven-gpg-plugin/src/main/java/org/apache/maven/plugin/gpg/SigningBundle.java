package org.apache.maven.plugin.gpg;

import java.io.File;

/** @author Jason van Zyl */
public class SigningBundle
{
    private String artifactType;

    private File signature;

    public SigningBundle( String artifactType,
                          File signature )
    {
        this.artifactType = artifactType;

        this.signature = signature;
    }

    public String getArtifactType()
    {
        return artifactType;
    }

    public File getSignature()
    {
        return signature;
    }
}
