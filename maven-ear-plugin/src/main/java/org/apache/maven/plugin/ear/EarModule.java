package org.apache.maven.plugin.ear;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
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
     * @see #resolveArtifact(java.util.Set)
     */
    public Artifact getArtifact();

    /**
     * Returns the <tt>URI</tt> for this module.
     *
     * @return the <tt>URI</tt>
     */
    public String getUri();

    /**
     * Returns the type associated to the module.
     *
     * @return the artifact's type of the module
     */
    public String getType();

    /**
     * Specify whether this module should be excluded or not.
     *
     * @return true if this module should be skipped, false otherwise
     */
    public boolean isExcluded();

    /**
     * Specify whether this module should be unpacked in the
     * EAR archive or not.
     * <p/>
     * Returns null if no configuration was specified so that
     * defaulting may apply.
     *
     * @return true if this module should be bundled unpacked, false otherwise
     */
    public Boolean shouldUnpack();

    /**
     * The alt-dd element specifies an optional URI to the post-assembly version
     * of the deployment descriptor file for a particular Java EE module. The URI
     * must specify the full pathname of the deployment descriptor file relative
     * to the application's root directory.
     *
     * @return the alternative deployment descriptor for this module
     * @since JavaEE 5
     */
    public String getAltDeploymentDescriptor();

    /**
     * Appends the <tt>XML</tt> representation of this module.
     *
     * @param writer     the writer to use
     * @param version    the version of the <tt>application.xml</tt> file
     * @param generateId whether an id should be generated
     */
    public void appendModule( XMLWriter writer, String version, Boolean generateId );

    /**
     * Resolves the {@link Artifact} represented by the module. Note
     * that the {@link EarExecutionContext} might be used to customize
     * further the resolution.
     *
     * @param artifacts the project's artifacts
     * @throws EarPluginException   if the artifact could not be resolved
     * @throws MojoFailureException if an unexpected error occurred
     */
    public void resolveArtifact( Set<Artifact> artifacts )
        throws EarPluginException, MojoFailureException;

    public void setEarExecutionContext( EarExecutionContext earExecutionContext );

    public boolean changeManifestClasspath();

    public String getLibDir();

}
