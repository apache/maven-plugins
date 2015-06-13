package org.apache.maven.plugins.assembly.archive.archiver;

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

import org.codehaus.plexus.archiver.ArchivedFileSet;
import org.codehaus.plexus.components.io.fileselectors.FileSelector;
import org.codehaus.plexus.components.io.functions.InputStreamTransformer;

import java.io.File;

/**
 * @version $Id$
 */
class PrefixedArchivedFileSet
    implements ArchivedFileSet
{

    private final String rootPrefix;

    private final ArchivedFileSet fileSet;

    private final FileSelector[] selectors;

    /**
     * @param fileSet    The archived file set.
     * @param rootPrefix The root prefix.
     * @param selectors  The file selectors.
     */
    public PrefixedArchivedFileSet( ArchivedFileSet fileSet, String rootPrefix, FileSelector[] selectors )
    {
        this.fileSet = fileSet;
        this.selectors = selectors;

        if ( rootPrefix.length() > 0 && !rootPrefix.endsWith( "/" ) )
        {
            this.rootPrefix = rootPrefix + "/";
        }
        else
        {
            this.rootPrefix = rootPrefix;
        }
    }

    /**
     * {@inheritDoc}
     */
    public File getArchive()
    {
        return fileSet.getArchive();
    }

    /**
     * {@inheritDoc}
     */
    public String[] getExcludes()
    {
        return fileSet.getExcludes();
    }

    /**
     * {@inheritDoc}
     */
    public FileSelector[] getFileSelectors()
    {
        return PrefixedFileSet.combineSelectors( fileSet.getFileSelectors(), selectors );
    }

    /**
     * {@inheritDoc}
     */
    public String[] getIncludes()
    {
        return fileSet.getIncludes();
    }

    /**
     * {@inheritDoc}
     */
    public String getPrefix()
    {
        String prefix = fileSet.getPrefix();
        if ( prefix.startsWith( "/" ) )
        {
            if ( prefix.length() > 1 )
            {
                prefix = prefix.substring( 1 );
            }
            else
            {
                prefix = "";
            }
        }

        return rootPrefix + prefix;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isCaseSensitive()
    {
        return fileSet.isCaseSensitive();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isIncludingEmptyDirectories()
    {
        return fileSet.isIncludingEmptyDirectories();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isUsingDefaultExcludes()
    {
        return fileSet.isUsingDefaultExcludes();
    }

    public InputStreamTransformer getStreamTransformer()
    {
        return fileSet.getStreamTransformer();
    }

}
