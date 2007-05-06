/*
 * Copyright 2007 The Apache Software Foundation.
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
package org.apache.maven.plugin.clover.internal.scanner;

import org.codehaus.plexus.compiler.util.scan.SourceInclusionScanner;
import org.codehaus.plexus.compiler.util.scan.SimpleSourceInclusionScanner;
import org.codehaus.plexus.compiler.util.scan.InclusionScanException;
import org.codehaus.plexus.compiler.util.scan.mapping.SuffixMapping;
import org.apache.maven.plugin.clover.internal.CloverConfiguration;

import java.util.*;
import java.io.File;

/**
 * Code common to compute the list of source files to instrument (main sources, test sources).
 *
 * @version $Id: $
 */
public abstract class AbstractCloverSourceScanner implements CloverSourceScanner
{
    private CloverConfiguration configuration;

    public AbstractCloverSourceScanner(CloverConfiguration configuration)
    {
        this.configuration = configuration;
    }

    protected CloverConfiguration getConfiguration()
    {
        return this.configuration;
    }

    /**
     * {@inheritDoc}
     * @see CloverSourceScanner#getSourceFilesToInstrument()
     */
    public Map getSourceFilesToInstrument()
    {
        return computeFiles(getScanner());
    }

    /**
     * {@inheritDoc}
     * @see CloverSourceScanner#getExcludedFiles() 
     */
    public Map getExcludedFiles()
    {
        return computeFiles(getExcludesScanner());
    }

    protected abstract List getSourceRoots();
    protected abstract String getSourceDirectory();

    /**
     * @return a Plexus scanner object that scans a source root and filters files according to inclusion and
     * exclusion patterns. In our case at hand we include only Java sources as these are the only files we want
     * to instrument.
     */
    private SourceInclusionScanner getScanner()
    {
        SourceInclusionScanner scanner;
        Set includes = getConfiguration().getIncludes();
        Set excludes = getConfiguration().getExcludes();

        if ( includes.isEmpty() && excludes.isEmpty() )
        {
            includes = Collections.singleton( "**/*.java" );
            scanner = new SimpleSourceInclusionScanner( includes, Collections.EMPTY_SET );
        }
        else
        {
            if ( includes.isEmpty() )
            {
                includes.add( "**/*.java" );
            }
            scanner = new SimpleSourceInclusionScanner( includes, excludes );
        }

        // Note: we shouldn't have to do this but this is a limitation of the Plexus SimpleSourceInclusionScanner
        scanner.addSourceMapping( new SuffixMapping( "dummy", "dummy" ) );

        return scanner;
    }

    private SourceInclusionScanner getExcludesScanner()
    {
        SourceInclusionScanner scanner;
        Set excludes = getConfiguration().getExcludes();

        if ( excludes.isEmpty() )
        {
            scanner = new SimpleSourceInclusionScanner( Collections.EMPTY_SET, Collections.EMPTY_SET );
        }
        else
        {
            scanner = new SimpleSourceInclusionScanner( excludes, Collections.EMPTY_SET );
        }

        // Note: we shouldn't have to do this but this is a limitation of the Plexus SimpleSourceInclusionScanner
        scanner.addSourceMapping( new SuffixMapping( "dummy", "dummy" ) );

        return scanner;
    }

    private Map computeFiles(SourceInclusionScanner scanner)
    {
        Map files = new HashMap();

        // Decide whether to instrument all source roots or only the main source root.
        Iterator sourceRoots = getResolvedSourceRoots().iterator();
        while ( sourceRoots.hasNext() )
        {
            File sourceRoot = new File( (String) sourceRoots.next() );
            if ( sourceRoot.exists() )
            {
                try
                {
                    Set sourcesToAdd = scanner.getIncludedSources( sourceRoot, null );
                    if ( !sourcesToAdd.isEmpty() )
                    {
                        files.put( sourceRoot.getPath(), sourcesToAdd );
                    }
                }
                catch ( InclusionScanException e )
                {
                    getConfiguration().getLog().warn( "Failed to add sources from [" + sourceRoot + "]", e );
                }
            }
        }

        return files;
    }

    private List getResolvedSourceRoots()
    {
        List sourceRoots;
        if ( getConfiguration().includesAllSourceRoots() )
        {
            sourceRoots = getSourceRoots();
        }
        else
        {
            sourceRoots = Collections.singletonList( getSourceDirectory() );
        }

        return sourceRoots;
    }

}
