package org.apache.maven.plugin.war.packaging;

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

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.war.util.PathSet;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.logging.Logger;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * The packaging context.
 *
 * @author Stephane Nicoll
 */
public interface WarPackagingContext
{
    /**
     * Returns the maven project.
     *
     * @return the project
     */
    MavenProject getProject();

    /**
     * Returns the webapp directory. Packaging tasks should use this
     * directory to generate the webapp.
     *
     * @return the webapp directory
     */
    File getWebAppDirectory();

    /**
     * Returns the main webapp source directory.
     *
     * @return the webapp source directory
     */
    File getWebAppSourceDirectory();

    /**
     * Returns the webapp source includes.
     *
     * @return the webapp source includes
     */
    String[] getWebAppSourceIncludes();

    /**
     * Returns the webapp source excludes.
     *
     * @return the webapp source excludes
     */
    String[] getWebAppSourceExcludes();

    /**
     * Returns the directory holding generated classes.
     *
     * @return the classes directory
     */
    File getClassesDirectory();

    /**
     * Specify whether the classes resources should be archived in
     * the <tt>WEB-INF/lib</tt> of the generated web app.
     *
     * @return true if the classes should be archived, false otherwise
     */
    boolean archiveClasses();

    /**
     * Returns the logger to use to output logging event.
     *
     * @return the logger
     */
    Logger getLogger();

    /**
     * Returns the directory to unpack dependent WARs into if needed.
     *
     * @return the overlays work directory
     */
    File getOverlaysWorkDirectory();

    /**
     * Returns the archiver manager to use.
     *
     * @return the archiver manager
     */
    ArchiverManager getArchiverManager();

    /**
     * The maven archive configuration to use.
     *
     * @return the maven archive configuration
     */
    MavenArchiveConfiguration getArchive();

    /**
     * Returns the Jar archiver needed for archiving classes directory into
     * jar file under WEB-INF/lib.
     *
     * @return the jar archiver to user
     */
    JarArchiver getJarArchiver();

    /**
     * Returns the list of files that have already been copied during the
     * packaging tasks.
     * <p/>
     * Tasks are responsible to update this file to make sure that the
     * overwriting strategy is applied properly.
     *
     * @return the list of files that have already been copied
     */
    PathSet getProtectedFiles();


    /**
     * Returns the output file name mapping to use, if any. Returns <tt>null</tt>
     * if no file name mapping is set.
     *
     * @return the output file name mapping or <tt>null</tt>
     */
    String getOutputFileNameMapping();

    /**
     * Returns the list of filter files to use.
     *
     * @return a list of filter files
     */
    List getFilters();

    /**
     * Returns the filter properties to use to filter resources.
     * <p/>
     * TODO: this needs to be refactored to use the resource plugin somehow.
     *
     * @return a map of filter properties
     * @throws MojoExecutionException if an error occured while reading a filter file
     */
    Map getFilterProperties()
        throws MojoExecutionException;


}
