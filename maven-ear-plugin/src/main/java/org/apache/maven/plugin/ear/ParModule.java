/*
 * Copyright (c) 2005 Your Corporation. All Rights Reserved.
 */
package org.apache.maven.plugin.ear;

import org.apache.maven.artifact.Artifact;

/**
 * The {@link EarModule} implementation for a Par module.
 *
 * @author Stephane Nicoll <snicoll@apache.org>
 * @author $Author: $ (last edit)
 * @version $Revision:  $
 */
public class ParModule
    extends EjbModule
{

    public ParModule()
    {
        super();
    }

    public ParModule( Artifact a )
    {
        super( a );
    }

    protected String getType()
    {
        return "par";
    }
}
