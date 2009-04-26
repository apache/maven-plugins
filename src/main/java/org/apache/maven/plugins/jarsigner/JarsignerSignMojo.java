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
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.Commandline;

/**
 * Signs a project artifact and attachments using jarsigner.
 *
 * @author <a href="cs@schulte.it">Christian Schulte</a>
 * @version $Id$
 * @goal sign
 * @phase package
 * @requiresProject
 */
public class JarsignerSignMojo extends AbstractJarsignerMojo
{

    /**
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/jarsigner.html#Options">options</a>.
     *
     * @parameter expression="${jarsigner.keystore}"
     */
    private String keystore;

    /**
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/jarsigner.html#Options">options</a>.
     *
     * @parameter expression="${jarsigner.storepass}"
     */
    private String storepass;

    /**
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/jarsigner.html#Options">options</a>.
     *
     * @parameter expression="${jarsigner.keypass}"
     */
    private String keypass;

    /**
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/jarsigner.html#Options">options</a>.
     *
     * @parameter expression="${jarsigner.sigfile}"
     */
    private String sigfile;

    /**
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/jarsigner.html#Options">options</a>.
     *
     * @parameter expression="${jarsigner.storetype}"
     */
    private String storetype;

    /**
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/jarsigner.html#Options">options</a>.
     *
     * @parameter expression="${jarsigner.alias}"
     * @required
     */
    private String alias;

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

        if ( !StringUtils.isEmpty( this.keystore ) )
        {
            commandLine.createArg().setValue( "-keystore" );
            commandLine.createArg().setValue( this.keystore );
        }
        if ( !StringUtils.isEmpty( this.storepass ) )
        {
            commandLine.createArg().setValue( "-storepass" );
            commandLine.createArg().setValue( this.storepass );
        }
        if ( !StringUtils.isEmpty( this.keypass ) )
        {
            commandLine.createArg().setValue( "-keypass" );
            commandLine.createArg().setValue( this.keypass );
        }
        if ( !StringUtils.isEmpty( this.storetype ) )
        {
            commandLine.createArg().setValue( "-storetype" );
            commandLine.createArg().setValue( this.storetype );
        }
        if ( !StringUtils.isEmpty( this.sigfile ) )
        {
            commandLine.createArg().setValue( "-sigfile" );
            commandLine.createArg().setValue( this.sigfile );
        }

        commandLine.createArg().setFile( archive );

        if ( !StringUtils.isEmpty( this.alias ) )
        {
            commandLine.createArg().setValue( this.alias );
        }

        return commandLine;
    }

    protected String getCommandlineInfo( final Commandline commandLine )
    {
        String commandLineInfo = commandLine != null ? commandLine.toString() : null;

        if ( commandLineInfo != null )
        {
            commandLineInfo = StringUtils.replace( commandLineInfo, this.keypass, "'*****'" );
            commandLineInfo = StringUtils.replace( commandLineInfo, this.storepass, "'*****'" );
        }

        return commandLineInfo;
    }

}
