package org.apache.maven.plugin.assembly.archive.archiver;

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

import org.codehaus.plexus.archiver.FileSet;
import org.codehaus.plexus.components.io.fileselectors.FileSelector;
import org.codehaus.plexus.components.io.functions.InputStreamTransformer;

import java.io.File;

/**
 * @version $Id$
 */
class PrefixedFileSet
    implements FileSet
{

    private final String rootPrefix;

    private final FileSet fileSet;

    private final FileSelector[] selectors;

    /**
     * @param fileSet The file set.
     * @param rootPrefix The root prefix
     * @param selectors The file selectors.
     */
    public PrefixedFileSet( final FileSet fileSet, final String rootPrefix, final FileSelector[] selectors )
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

    /** {@inheritDoc} */
    public String[] getExcludes()
    {
        return fileSet.getExcludes();
    }

    /** {@inheritDoc} */
    public FileSelector[] getFileSelectors()
    {
        FileSelector[] sel = fileSet.getFileSelectors();
        final FileSelector[] selectors1 = selectors;
        return combineSelectors( sel, selectors1 );
    }

    /** {@inheritDoc} */
    static FileSelector[] combineSelectors( FileSelector[] first, FileSelector[] second )
    {
        if ( ( first != null ) && ( second != null ) )
        {
            final FileSelector[] temp = new FileSelector[first.length + second.length];

            System.arraycopy( first, 0, temp, 0, first.length );
            System.arraycopy( second, 0, temp, first.length, second.length );

            first = temp;
        }
        else if ( ( first == null ) && ( second != null ) )
        {
            first = second;
        }

        return first;
    }

    /** {@inheritDoc} */
    public String[] getIncludes()
    {
        return fileSet.getIncludes();
    }

    /** {@inheritDoc} */
    public String getPrefix()
    {
        String prefix = fileSet.getPrefix();
        if ( prefix == null )
        {
            return rootPrefix;
        }

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

    /** {@inheritDoc} */
    public boolean isCaseSensitive()
    {
        return fileSet.isCaseSensitive();
    }

    /** {@inheritDoc} */
    public boolean isIncludingEmptyDirectories()
    {
        return fileSet.isIncludingEmptyDirectories();
    }

    /** {@inheritDoc} */
    public boolean isUsingDefaultExcludes()
    {
        return fileSet.isUsingDefaultExcludes();
    }

    /** {@inheritDoc} */
    public File getDirectory()
    {
        return fileSet.getDirectory();
    }

    public InputStreamTransformer getStreamTransformer()
    {
        return fileSet.getStreamTransformer();
    }
}
