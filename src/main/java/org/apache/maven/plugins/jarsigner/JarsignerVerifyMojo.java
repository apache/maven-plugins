package org.apache.maven.plugins.jarsigner;

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

import java.io.File;
import org.codehaus.plexus.util.cli.Commandline;

/**
 * Checks the signatures of a project artifact and attachments using jarsigner.
 *
 * @author <a href="cs@schulte.it">Christian Schulte</a>
 * @version $Id$
 * @goal verify
 * @phase verify
 * @requiresProject
 */
public class JarsignerVerifyMojo extends AbstractJarsignerMojo
{

    /**
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/jarsigner.html#Options">options</a>.
     *
     * @parameter expression="${jarsigner.certs}" default-value="false"
     */
    private boolean certs;

    protected Commandline getCommandline( final File archive, final Commandline commandLine )
    {
        if ( archive == null )
        {
            throw new NullPointerException( "archive" );
        }
        if ( commandLine == null )
        {
            throw new NullPointerException( "commandLine" );
        }

        commandLine.createArg( true ).setValue( "-verify" );

        if ( this.certs )
        {
            commandLine.createArg().setValue( "-certs" );
        }

        commandLine.createArg().setFile( archive );
        return commandLine;
    }

}
