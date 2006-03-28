package org.apache.maven.plugins.release.helpers;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
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

import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.ScmResult;
import org.apache.maven.scm.command.add.AddScmResult;
import org.apache.maven.scm.command.checkin.CheckInScmResult;
import org.apache.maven.scm.command.checkout.CheckOutScmResult;
import org.apache.maven.scm.command.edit.EditScmResult;
import org.apache.maven.scm.command.remove.RemoveScmResult;
import org.apache.maven.scm.command.status.StatusScmResult;
import org.apache.maven.scm.command.tag.TagScmResult;
import org.apache.maven.scm.command.unedit.UnEditScmResult;
import org.apache.maven.scm.command.update.UpdateScmResult;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.scm.provider.ScmProviderRepository;
import org.apache.maven.scm.provider.ScmProviderRepositoryWithHost;
import org.apache.maven.scm.provider.svn.repository.SvnScmProviderRepository;
import org.apache.maven.scm.repository.ScmRepository;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * A bean for using the Maven SCM API.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @author <a href="mailto:evenisse@apache.org">Emmanuel Venisse</a>
 * @version $Id$
 */
public class ScmHelper
{
    private static final String POM = "pom.xml";

    private String username;

    private String password;

    private String privateKey;

    private String passphrase;

    private String url;

    private String tag;

    private String workingDirectory;

    // note - this should not have a setter
    private File checkoutDirectory;

    private String tagBase;

    private ScmManager scmManager;

    private ScmManager getScmManager()
        throws ScmException
    {
        if ( scmManager == null )
        {
            throw new ScmException( "scmManager isn't define." );
        }

        return scmManager;
    }

    private ScmRepository getScmRepository()
        throws ScmException
    {
        ScmRepository repository;

        try
        {
            repository = getScmManager().makeScmRepository( url );

            ScmProviderRepository scmRepo = repository.getProviderRepository();

            if ( !StringUtils.isEmpty( username ) )
            {
                scmRepo.setUser( username );
            }
            if ( !StringUtils.isEmpty( password ) )
            {
                scmRepo.setPassword( password );
            }

            if ( scmRepo instanceof ScmProviderRepositoryWithHost )
            {
                if ( !StringUtils.isEmpty( privateKey ) )
                {
                    ( (ScmProviderRepositoryWithHost) scmRepo ).setPrivateKey( privateKey );
                }

                if ( !StringUtils.isEmpty( passphrase ) )
                {
                    ( (ScmProviderRepositoryWithHost) scmRepo ).setPassphrase( passphrase );
                }
            }

            if ( repository.getProvider().equals( "svn" ) )
            {
                SvnScmProviderRepository svnRepo = (SvnScmProviderRepository) repository.getProviderRepository();

                if ( tagBase != null && tagBase.length() > 0 )
                {
                    svnRepo.setTagBase( tagBase );
                }
            }
        }
        catch ( Exception e )
        {
            throw new ScmException( "Can't load the scm provider.", e );
        }

        return repository;
    }

    public String getProvider()
        throws ScmException
    {
        ScmRepository repository;
        try
        {
            repository = getScmManager().makeScmRepository( url );

            return repository.getProvider();
        }
        catch ( Exception e )
        {
            throw new ScmException( "Can't load the scm provider.", e );
        }
    }

    private void checkResult( ScmResult result )
        throws ScmException
    {
        if ( !result.isSuccess() )
        {
            // TODO: improve error handling
            System.err.println( "Provider message:" );

            System.err.println( result.getProviderMessage() );

            System.err.println( "Command output:" );

            System.err.println( result.getCommandOutput() );

            throw new ScmException( "Error!" );
        }
    }

    public void checkout()
        throws ScmException, IOException
    {
        ScmRepository repository = getScmRepository();

        checkoutDirectory = new File( workingDirectory );

        // TODO: sanity check that it is not . or .. or lower

        if ( FileUtils.fileExists( workingDirectory ) )
        {
            FileUtils.deleteDirectory( workingDirectory );

            FileUtils.mkdir( workingDirectory );
        }

        CheckOutScmResult result = getScmManager().getProviderByRepository( repository )
            .checkOut( repository, new ScmFileSet( checkoutDirectory ), tag );

        checkResult( result );
    }

    public void update()
        throws ScmException
    {
        ScmRepository repository = getScmRepository();

        checkoutDirectory = new File( workingDirectory );

        // TODO: want includes/excludes?
        UpdateScmResult result = getScmManager().getProviderByRepository( repository )
            .update( repository, new ScmFileSet( new File( workingDirectory ) ), tag );

        checkResult( result );
    }

    public List getStatus()
        throws ScmException
    {
        List changedFiles;

        ScmRepository repository = getScmRepository();

        // TODO: want includes/excludes?
        StatusScmResult result = getScmManager().getProviderByRepository( repository )
            .status( repository, new ScmFileSet( new File( workingDirectory ) ) );

        checkResult( result );

        List unfilteredFiles = result.getChangedFiles();

        changedFiles = new ArrayList();

        for ( Iterator i = unfilteredFiles.iterator(); i.hasNext(); )
        {
            String test = ( String ) i.next();
            if ( !test.toLowerCase().endsWith( POM ) )
            {
                changedFiles.add( test );
            }
        }

        return changedFiles;
    }

    public void add( String file )
        throws ScmException, IOException
    {
        ScmRepository repository = getScmRepository();

        ScmFileSet fs = new ScmFileSet( new File( workingDirectory ), new File( file ) );

        AddScmResult result = getScmManager().getProviderByRepository( repository ).add( repository, fs );

        checkResult( result );
    }

    public void remove( String message, String file )
        throws ScmException, IOException
    {
        ScmRepository repository = getScmRepository();

        ScmFileSet fs = new ScmFileSet( new File( workingDirectory ), new File( file ) );

        RemoveScmResult result =
            getScmManager().getProviderByRepository( repository ).remove( repository, fs, message );

        checkResult( result );
    }

    public void checkin( List pomFiles, String message )
        throws ScmException
    {
        File[] files = (File[]) pomFiles.toArray( new File[pomFiles.size()] );

        ScmRepository repository = getScmRepository();

        CheckInScmResult result = getScmManager().getProviderByRepository( repository )
            .checkIn( repository, new ScmFileSet( new File( workingDirectory ), files ), tag, message );
        checkResult( result );
    }

    public void edit( File file )
        throws ScmException
    {
        ScmRepository repository = getScmRepository();

        EditScmResult result = getScmManager().getProviderByRepository( repository )
            .edit( repository, new ScmFileSet( new File( workingDirectory ), file ) );

        checkResult( result );
    }

    public void unedit( File file )
        throws ScmException
    {
        ScmRepository repository = getScmRepository();

        UnEditScmResult result = getScmManager().getProviderByRepository( repository )
            .unedit( repository, new ScmFileSet( new File( workingDirectory ), file ) );

        checkResult( result );
    }

    public void tag()
        throws ScmException
    {
        ScmRepository repository = getScmRepository();

        // TODO: want includes/excludes?
        TagScmResult result = getScmManager().getProviderByRepository( repository )
            .tag( repository, new ScmFileSet( new File( workingDirectory ) ), tag );

        checkResult( result );
    }

    public void setScmManager( ScmManager scmManager )
    {
        this.scmManager = scmManager;
    }

    public void setUrl( String url )
    {
        this.url = url;
    }

    public String getUrl()
    {
        return url;
    }

    public void setTag( String tag )
    {
        this.tag = tag;
    }

    public String getTag()
    {
        return tag;
    }

    public void setWorkingDirectory( String workingDirectory )
    {
        FileUtils.mkdir( workingDirectory );

        this.workingDirectory = workingDirectory;
    }

    public String getWorkingDirectory()
    {
        return workingDirectory;
    }

    public File getCheckoutDirectory()
    {
        return checkoutDirectory;
    }

    public String getTagBase()
    {
        return tagBase;
    }

    public void setTagBase( String tagBase )
    {
        this.tagBase = tagBase;
    }

    public String getUsername()
    {
        return username;
    }

    public void setUsername( String username )
    {
        this.username = username;
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword( String password )
    {
        this.password = password;
    }

    public String getPrivateKey()
    {
        return privateKey;
    }

    public void setPrivateKey( String privateKey )
    {
        this.privateKey = privateKey;
    }

    public String getPassphrase()
    {
        return passphrase;
    }

    public void setPassphrase( String passphrase )
    {
        this.passphrase = passphrase;
    }

    public boolean requiresEditMode()
        throws ScmException
    {
        ScmRepository repository = getScmRepository();
        return getScmManager().getProviderByRepository( repository ).requiresEditMode();
    }
}
