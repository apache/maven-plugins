package org.apache.maven.plugin.clean;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.util.Arrays;

/**
 * Customizes the string representation of
 * <code>org.apache.maven.shared.model.fileset.FileSet</code> to return the
 * included and excluded files from the file-set's directory. Specifically,
 * <code>"file-set: <I>[directory]</I> (included: <I>[included files]</I>,
 * excluded: <I>[excluded files]</I>)"</code>
 *
 * @version $Id$
 * @since 2.1
 */
public class Fileset
{

    private File directory;

    private String[] includes;

    private String[] excludes;

    private boolean followSymlinks;

    private boolean useDefaultExcludes;

    public File getDirectory()
    {
        return directory;
    }

    public String[] getIncludes()
    {
        return ( includes != null ) ? includes : new String[0];
    }

    public String[] getExcludes()
    {
        return ( excludes != null ) ? excludes : new String[0];
    }

    public boolean isFollowSymlinks()
    {
        return followSymlinks;
    }

    public boolean isUseDefaultExcludes()
    {
        return useDefaultExcludes;
    }

    /**
     * Retrieves the included and excluded files from this file-set's directory.
     * Specifically, <code>"file-set: <I>[directory]</I> (included:
     * <I>[included files]</I>, excluded: <I>[excluded files]</I>)"</code>
     *
     * @return The included and excluded files from this file-set's directory.
     * Specifically, <code>"file-set: <I>[directory]</I> (included:
     * <I>[included files]</I>, excluded: <I>[excluded files]</I>)"</code>
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        return "file set: " + getDirectory() + " (included: " + Arrays.asList( getIncludes() ) + ", excluded: "
            + Arrays.asList( getExcludes() ) + ")";
    }

}
