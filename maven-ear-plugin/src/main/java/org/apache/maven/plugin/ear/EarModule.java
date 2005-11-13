package org.apache.maven.plugin.ear;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.xml.XMLWriter;

import java.util.Set;

/**
 * The ear module interface.
 *
 * @author <a href="snicoll@apache.org">Stephane Nicoll</a>
 * @version $Id$
 */
public interface EarModule
{

    /**
     * Returns the {@link Artifact} representing this module.
     * <p/>
     * Note that this might return <tt>null</tt> till the
     * module has been resolved.
     *
     * @return the artifact
     * @see #resolveArtifact(java.util.Set, String)
     */
    public Artifact getArtifact();

    /**
     * Returns the <tt>URI</tt> for this module.
     *
     * @return the <tt>URI</tt>
     */
    public String getUri();

    /**
     * Specify whether this module should be excluded or not.
     *
     * @return true if this module should be skipped, false otherwise
     */
    public boolean isExcluded();

    /**
     * Appends the <tt>XML</tt> representation of this module.
     *
     * @param writer  the writer to use
     * @param version the version of the <tt>application.xml</tt> file
     */
    public void appendModule( XMLWriter writer, String version );

    /**
     * Resolves the {@link Artifact} represented by the module with
     * the specified execution configuration.
     *
     * @param artifacts            the project's artifacts
     * @param defaultJavaBundleDir the default bundle dir for {@link JavaModule}
     * @throws EarPluginException if the artifact could not be resolved
     */
    public void resolveArtifact( Set artifacts, String defaultJavaBundleDir )
        throws EarPluginException, MojoFailureException;

}
