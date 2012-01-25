package org.apache.maven.plugin.ear.util;

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

import java.util.List;
import java.util.Set;

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.ear.EarModule;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.jar.Manifest;
import org.codehaus.plexus.archiver.jar.ManifestException;

/**
 * A custom {@link MavenArchiver} implementation that takes care
 * of setting the right classpath value according to the actual
 * path of bundled files.
 *
 * @author <a href="snicoll@apache.org">Stephane Nicoll</a>
 */
public class EarMavenArchiver
    extends MavenArchiver
{
    public static final String CLASS_PATH_KEY = "Class-Path";

    private final List<EarModule> earModules;


    /**
     * Creates an instance with the ear modules that will be packaged
     * in the EAR archive.
     *
     * @param earModules the intitialized list of ear modules
     */
    public EarMavenArchiver( List<EarModule> earModules )
    {
        this.earModules = earModules;
    }

    /** @deprecated */
    public Manifest getManifest( MavenProject project, MavenArchiveConfiguration config )
                    throws ManifestException, DependencyResolutionRequiredException
    {
        return this.getManifest( null, project, config );
    }

    public Manifest getManifest( MavenSession session, MavenProject project, MavenArchiveConfiguration config )
        throws ManifestException, DependencyResolutionRequiredException
    {
        final Manifest manifest = super.getManifest( session, project, config );
        if ( config.getManifest().isAddClasspath() )
        {
            String earManifestClassPathEntry = generateClassPathEntry( config.getManifest().getClasspathPrefix() );
            // Class-path can be customized. Let's make sure we don't overwrite this
            // with our custom change!
            final String userSuppliedClassPathEntry = getUserSuppliedClassPathEntry( config );
            if ( userSuppliedClassPathEntry != null )
            {
                earManifestClassPathEntry = userSuppliedClassPathEntry + " " + earManifestClassPathEntry;
            }

            // Overwrite the existing one, if any
            final Manifest.Attribute classPathAttr = manifest.getMainSection().getAttribute( CLASS_PATH_KEY );
            if ( classPathAttr != null )
            {
                classPathAttr.setValue( earManifestClassPathEntry );
            }
            else
            {
                final Manifest.Attribute attr = new Manifest.Attribute( CLASS_PATH_KEY, earManifestClassPathEntry );
                manifest.addConfiguredAttribute( attr );
            }
        }
        return manifest;
    }

    /**
     * Generates the <tt>Class-Path</tt> entry of the manifest according to
     * the list of ear modules.
     *
     * @param classPathPrefix the classpath prefix to use
     * @return the <tt>Class-Path</tt> entry
     */
    protected String generateClassPathEntry( String classPathPrefix )
    {
        final StringBuffer classpath = new StringBuffer();
        for ( final EarModule earModule : earModules )
        {
            if ( !earModule.isExcluded() )
            {
                classpath.append( classPathPrefix ).append( earModule.getUri() ).append( " " );
            }
        }
        return classpath.toString().trim();
    }

    protected String getUserSuppliedClassPathEntry( MavenArchiveConfiguration config )
    {
        if ( config.getManifestEntries() != null )
        {
            @SuppressWarnings( "unchecked" )
            final Set<String> keys = config.getManifestEntries().keySet();
            for ( String key : keys )
            {
                String value = (String) config.getManifestEntries().get( key );
                if ( "Class-Path".equals( key ) && value != null )
                {
                    return value;

                }

            }
        }
        return null;
    }
}
