/*
 * Copyright (c) 2005 Your Corporation. All Rights Reserved.
 */
package org.apache.maven.plugin.ear;

import org.apache.maven.artifact.Artifact;

/**
 * The {@link EarModule} implementation for an Ejb3 module.
 *
 * @author Stephane Nicoll <snicoll@apache.org>
 * @author $Author: $ (last edit)
 * @version $Revision:  $
 */
public class Ejb3Module
    extends EjbModule
{
    public Ejb3Module()
    {
        super();
    }

    public Ejb3Module( Artifact a )
    {
        super( a );
    }

    protected String getType()
    {
        return "ejb3";
    }
}
