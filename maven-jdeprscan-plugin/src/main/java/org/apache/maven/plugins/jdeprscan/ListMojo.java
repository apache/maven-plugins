package org.apache.maven.plugins.jdeprscan;

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

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.cli.Commandline;

/**
 * Prints the set of deprecated APIs. No scanning is done.
 * 
 * @author Robert Scholte
 * @since 3.0.0
 */
@Mojo( name = "list", requiresProject = false, requiresDirectInvocation = true )
public class ListMojo
    extends AbstractJDeprScanMojo
{
    /**
     * Limits scanning or listing to APIs that are deprecated for removal. Can’t be used with a release value of 6, 7,
     * or 8.
     */
    @Parameter( property = "for-removal" )
    private boolean forRemoval;

    @Override
    protected boolean isForRemoval()
    {
        return forRemoval;
    }

    @Override
    protected void addJDeprScanOptions( Commandline cmd )
        throws MojoFailureException
    {
        super.addJDeprScanOptions( cmd );

        cmd.createArg().setValue( "--list" );
    }
}
