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

import org.apache.maven.plugin.war.util.PathSet;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.logging.Logger;

import java.io.File;

/**
 * The packaging context.
 *
 * @author Stephane Nicoll
 */
public class WarPackagingContext
{

    private final Logger logger;
    private final File overlaysWorkDirectory;
    private final ArchiverManager archiverManager;
    private final PathSet protectedFiles;
    private final File webAppDirectory;


    public WarPackagingContext( Logger logger, File overlaysWorkDirectory, ArchiverManager archiverManager, PathSet protectedFiles,
                                File webAppDirectory )
    {
        this.logger = logger;
        this.overlaysWorkDirectory = overlaysWorkDirectory;
        this.archiverManager = archiverManager;
        this.protectedFiles = protectedFiles;
        this.webAppDirectory = webAppDirectory;
    }

    /**
     * Returns the logger to use to output logging event.
     *
     * @return the logger
     */
    public Logger getLogger()
    {
        return logger;
    }

    /**
     * Returns the directory to unpack dependent WARs into if needed.
     *
     * @return the overlays work directory
     */
    public File getOverlaysWorkDirectory()
    {
        return overlaysWorkDirectory;
    }

    /**
     * Returns the archiver manager to use.
     *
     * @return the archiver manager
     */
    public ArchiverManager getArchiverManager()
    {
        return archiverManager;
    }


    /**
     * Returns the list of files that have already been copied during the
     * packaging tasks.
     * <p/>
     * Tasks are responsible to update this file to make sure that the
     * overwriting strategy is applied properly.
     *
     * @return the list of files that have already been copied
     */
    public PathSet getProtectedFiles()
    {
        return protectedFiles;
    }


    /**
     * Returns the webapp directory. Packaging tasks should use this
     * directory to generate the webapp.
     *
     * @return the web app directory
     */
    public File getWebAppDirectory()
    {
        return webAppDirectory;
    }
}
