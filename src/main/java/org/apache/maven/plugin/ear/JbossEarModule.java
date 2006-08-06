/*
 * Copyright (c) 2006 Your Corporation. All Rights Reserved.
 */
package org.apache.maven.plugin.ear;

import org.codehaus.plexus.util.xml.XMLWriter;

/**
 * Represents a JBoss specific ear module.
 *
 * @author <a href="snicoll@apache.org">Stephane Nicoll</a>
 * @version $Id$
 */
public interface JbossEarModule
{
    /**
     * Appends the <tt>XML</tt> representation of this module for
     * the jboss-app.xml file.
     *
     * @param writer  the writer to use
     * @param version the version of the <tt>jboss-app.xml</tt> file
     */
    public void appendJbossModule( XMLWriter writer, String version );
}
