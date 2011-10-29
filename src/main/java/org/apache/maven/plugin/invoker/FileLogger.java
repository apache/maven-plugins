package org.apache.maven.plugin.invoker;

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

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.shared.invoker.InvocationOutputHandler;
import org.apache.maven.shared.scriptinterpreter.ExecutionLogger;
import org.codehaus.plexus.util.IOUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

/**
 * @version $Id$
 */
class FileLogger
    extends org.apache.maven.shared.scriptinterpreter.FileLogger
    implements InvocationOutputHandler, ExecutionLogger
{


    /**
     * Creates a new logger that writes to the specified file.
     * 
     * @param outputFile The path to the output file, must not be <code>null</code>.
     * @throws IOException If the output file could not be created.
     */
    public FileLogger( File outputFile )
        throws IOException
    {
        super( outputFile, null );
    }

    /**
     * Creates a new logger that writes to the specified file and optionally mirrors messages to the given mojo logger.
     * 
     * @param outputFile The path to the output file, must not be <code>null</code>.
     * @param log The mojo logger to additionally output messages to, may be <code>null</code> if not used.
     * @throws IOException If the output file could not be created.
     */
    public FileLogger( File outputFile, Log log )
        throws IOException
    {
        super( outputFile, log );
    }

}
