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
package org.apache.maven.plugin.clover.internal.instrumentation;

import org.apache.maven.plugin.clover.internal.scanner.CloverSourceScanner;
import org.apache.maven.plugin.clover.internal.scanner.TestCloverSourceScanner;
import org.apache.maven.plugin.clover.internal.CloverConfiguration;

import java.util.List;

/**
 * Instruments test sources.
 *
 * @version $Id: $
 */
public class TestInstrumenter extends AbstractInstrumenter
{
    private TestCloverSourceScanner scanner;

    public TestInstrumenter(CloverConfiguration configuration, String outputSourceDirectory)
    {
        super( configuration, outputSourceDirectory );
        scanner = new TestCloverSourceScanner( configuration );
    }

    protected CloverSourceScanner getSourceScanner()
    {
        return scanner;
    }

    protected String getSourceDirectory()
    {
        return getConfiguration().getProject().getBuild().getTestSourceDirectory();
    }

    protected void setSourceDirectory(String targetDirectory)
    {
        getConfiguration().getProject().getBuild().setTestSourceDirectory( targetDirectory );
    }

    protected List getCompileSourceRoots()
    {
        return getConfiguration().getProject().getTestCompileSourceRoots();
    }

    protected void addCompileSourceRoot(String sourceRoot)
    {
        getConfiguration().getProject().addTestCompileSourceRoot( sourceRoot );
    }
}