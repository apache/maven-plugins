package org.apache.maven.scm.provider.svn.svnexe.command.info;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.util.Iterator;

import org.apache.maven.scm.CommandParameters;
import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.ScmResult;
import org.apache.maven.scm.ScmTag;
import org.apache.maven.scm.command.AbstractCommand;
import org.apache.maven.scm.command.info.InfoScmResult;
import org.apache.maven.scm.provider.ScmProviderRepository;
import org.apache.maven.scm.provider.svn.SvnCommandUtils;
import org.apache.maven.scm.provider.svn.SvnTagBranchUtils;
import org.apache.maven.scm.provider.svn.command.SvnCommand;
import org.apache.maven.scm.provider.svn.repository.SvnScmProviderRepository;
import org.apache.maven.scm.provider.svn.svnexe.command.SvnCommandLineUtils;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;

/**
 * variation of SvnInfoCommand to work for branches. Taken from 1.7 release of maven-scm-providers
 * 
 * @author <a href="mailto:kenney@apache.org">Kenney Westerhof</a>
 */
public class SvnInfoCommandExpanded
    extends AbstractCommand
    implements SvnCommand
{

    /** {@inheritDoc} */
    @Override
    protected ScmResult executeCommand( final ScmProviderRepository repository, final ScmFileSet fileSet,
                                        final CommandParameters parameters )
        throws ScmException
    {
        return executeInfoCommand( (SvnScmProviderRepository) repository, fileSet, parameters, false, null );
    }

    public InfoScmResult executeInfoCommand( final SvnScmProviderRepository repository, final ScmFileSet fileSet,
                                             final CommandParameters parameters, final boolean recursive,
                                             final String revision )
        throws ScmException
    {
        Commandline cl = createCommandLine( repository, fileSet, recursive, revision );
        return executeInfoCommand( cl );
    }

    public InfoScmResult executeInfoTagCommand( final SvnScmProviderRepository repository, final ScmFileSet fileSet,
                                                final String tag, final CommandParameters parameters,
                                                final boolean recursive, final String revision )
        throws ScmException
    {
        Commandline cl = createTagCommandLine( repository, fileSet, tag, recursive, revision );
        return executeInfoCommand( cl );
    }

    private InfoScmResult executeInfoCommand( final Commandline cl )
        throws ScmException
    {

        SvnInfoConsumer consumer = new SvnInfoConsumer();

        CommandLineUtils.StringStreamConsumer stderr = new CommandLineUtils.StringStreamConsumer();
        if ( getLogger().isInfoEnabled() )
        {
            getLogger().info( "Executing: " + SvnCommandLineUtils.cryptPassword( cl ) );
            getLogger().info( "Working directory: " + cl.getWorkingDirectory().getAbsolutePath() );
        }

        int exitCode;
        try
        {
            exitCode = SvnCommandLineUtils.execute( cl, consumer, stderr, getLogger() );
        }
        catch ( CommandLineException ex )
        {
            throw new ScmException( "Error while executing command.", ex );
        }

        if ( exitCode != 0 )
        {
            return new InfoScmResult( cl.toString(), "The svn command failed.", stderr.getOutput(), false );
        }

        return new InfoScmResult( cl.toString(), consumer.getInfoItems() );
    }

    // set scope to protected to allow test to call it directly
    protected static Commandline createCommandLine( final SvnScmProviderRepository repository,
                                                    final ScmFileSet fileSet, final boolean recursive,
                                                    final String revision )
    {
        Commandline cl = SvnCommandLineUtils.getBaseSvnCommandLine( fileSet.getBasedir(), repository );

        cl.createArg().setValue( "info" );

        if ( recursive )
        {
            cl.createArg().setValue( "--recursive" );
        }

        if ( StringUtils.isNotEmpty( revision ) )
        {
            cl.createArg().setValue( "-r" );
            cl.createArg().setValue( revision );
        }

        Iterator<File> it = fileSet.getFileList().iterator();

        while ( it.hasNext() )
        {
            File file = it.next();
            if ( repository == null )
            {
                cl.createArg().setValue( file.getPath() );
            }
            else
            {
                cl.createArg().setValue( repository.getUrl() + "/" + file.getPath().replace( '\\', '/' ) );
            }
        }

        return cl;
    }

    // set scope to protected to allow test to call it directly
    protected static Commandline createTagCommandLine( final SvnScmProviderRepository repository,
                                                       final ScmFileSet fileSet, final String tag,
                                                       final boolean recursive, final String revision )
    {
        Commandline cl = SvnCommandLineUtils.getBaseSvnCommandLine( fileSet.getBasedir(), repository );

        cl.createArg().setValue( "info" );

        if ( recursive )
        {
            cl.createArg().setValue( "--recursive" );
        }

        if ( StringUtils.isNotEmpty( revision ) )
        {
            cl.createArg().setValue( "-r" );
            cl.createArg().setValue( revision );
        }

        Iterator<File> it = fileSet.getFileList().iterator();

        if ( !it.hasNext() )
        {
            String tagUrl = SvnTagBranchUtils.resolveTagUrl( repository, new ScmTag( tag ) );
            cl.createArg().setValue( SvnCommandUtils.fixUrl( tagUrl, repository.getUser() ) );
        }
        else
        {
            while ( it.hasNext() )
            {
                File file = it.next();

                if ( repository == null )
                {
                    cl.createArg().setValue( file.getPath() );
                }
                else
                {
                    // Note: this currently assumes you have the tag base checked out too
                    String tagUrl =
                        SvnTagBranchUtils.resolveTagUrl( repository, new ScmTag( tag ) ) + "/"
                            + file.getPath().replace( '\\', '/' );
                    cl.createArg().setValue( SvnCommandUtils.fixUrl( tagUrl, repository.getUser() ) );
                }
            }
        }

        return cl;
    }

}
