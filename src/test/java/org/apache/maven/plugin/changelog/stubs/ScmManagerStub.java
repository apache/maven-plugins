package org.apache.maven.plugin.changelog.stubs;

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

import org.apache.maven.scm.ScmBranch;
import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.ScmVersion;
import org.apache.maven.scm.command.add.AddScmResult;
import org.apache.maven.scm.command.branch.BranchScmResult;
import org.apache.maven.scm.command.changelog.ChangeLogScmResult;
import org.apache.maven.scm.command.checkin.CheckInScmResult;
import org.apache.maven.scm.command.checkout.CheckOutScmResult;
import org.apache.maven.scm.command.diff.DiffScmResult;
import org.apache.maven.scm.command.edit.EditScmResult;
import org.apache.maven.scm.command.export.ExportScmResult;
import org.apache.maven.scm.command.list.ListScmResult;
import org.apache.maven.scm.command.remove.RemoveScmResult;
import org.apache.maven.scm.command.status.StatusScmResult;
import org.apache.maven.scm.command.tag.TagScmResult;
import org.apache.maven.scm.command.unedit.UnEditScmResult;
import org.apache.maven.scm.command.update.UpdateScmResult;
import org.apache.maven.scm.manager.NoSuchScmProviderException;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.scm.provider.ScmProvider;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.scm.repository.ScmRepositoryException;
import org.apache.maven.scm.repository.UnknownRepositoryStructure;

import java.io.File;
import java.util.Date;
import java.util.List;

/**
 * @author Edwin Punzalan
 */
public class ScmManagerStub
    implements ScmManager
{
    private ScmProvider scmProvider;

    /**
     * @see org.apache.maven.scm.manager.ScmManager#getProviderByType(java.lang.String)
     */
    public ScmProvider getProviderByType( String string )
        throws NoSuchScmProviderException
    {
        return null;
    }

    /**
     * @see org.apache.maven.scm.manager.ScmManager#getProviderByUrl(java.lang.String)
     */
    public ScmProvider getProviderByUrl( String string )
        throws ScmRepositoryException, NoSuchScmProviderException
    {
        return null;
    }

    /**
     * @see org.apache.maven.scm.manager.ScmManager#makeProviderScmRepository(java.lang.String, java.io.File)
     */
    public ScmRepository makeProviderScmRepository( String string, File file )
        throws ScmRepositoryException, UnknownRepositoryStructure, NoSuchScmProviderException
    {
        return null;
    }

    /**
     * @see org.apache.maven.scm.manager.ScmManager#makeScmRepository(java.lang.String)
     */
    public ScmRepository makeScmRepository( String string )
        throws ScmRepositoryException, NoSuchScmProviderException
    {
        return new ScmRepositoryStub();
    }

    /**
     * @see org.apache.maven.scm.manager.ScmManager#validateScmRepository(java.lang.String)
     */
    public List validateScmRepository( String string )
    {
        return null;
    }

    /**
     * @see org.apache.maven.scm.manager.ScmManager#getProviderByRepository(org.apache.maven.scm.repository.ScmRepository)
     */
    public ScmProvider getProviderByRepository( ScmRepository scmRepository )
        throws NoSuchScmProviderException
    {
        return new ScmProviderStub();
    }

    public void setScmProvider( ScmProvider scmProvider )
    {
        this.scmProvider = scmProvider;
    }

    public ScmProvider getScmProvider()
    {
        return scmProvider;
    }

    /**
     * @see org.apache.maven.scm.manager.ScmManager#setScmProvider(java.lang.String, org.apache.maven.scm.provider.ScmProvider)
     */
    public void setScmProvider( String providerType, ScmProvider provider )
    {
        setScmProvider( provider );
    }

    /**
     * @see org.apache.maven.scm.manager.ScmManager#add(org.apache.maven.scm.repository.ScmRepository, org.apache.maven.scm.ScmFileSet)
     */
    public AddScmResult add( ScmRepository repository, ScmFileSet fileSet )
        throws ScmException
    {
        return this.getProviderByRepository( repository ).add( repository, fileSet );
    }

    /**
     * @see org.apache.maven.scm.manager.ScmManager#add(org.apache.maven.scm.repository.ScmRepository, org.apache.maven.scm.ScmFileSet, java.lang.String)
     */
    public AddScmResult add( ScmRepository repository, ScmFileSet fileSet, String message )
        throws ScmException
    {
        return this.getProviderByRepository( repository ).add( repository, fileSet, message );
    }

    /**
     * @see org.apache.maven.scm.manager.ScmManager#branch(org.apache.maven.scm.repository.ScmRepository, org.apache.maven.scm.ScmFileSet, java.lang.String)
     */
    public BranchScmResult branch( ScmRepository repository, ScmFileSet fileSet, String branchName )
        throws ScmException
    {
        return this.getProviderByRepository( repository ).branch( repository, fileSet, branchName );
    }

    /**
     * @see org.apache.maven.scm.manager.ScmManager#branch(org.apache.maven.scm.repository.ScmRepository, org.apache.maven.scm.ScmFileSet, java.lang.String, java.lang.String)
     */
    public BranchScmResult branch( ScmRepository repository, ScmFileSet fileSet, String branchName, String message )
        throws ScmException
    {
        return this.getProviderByRepository( repository ).branch( repository, fileSet, branchName, message );
    }

    /**
     * @see org.apache.maven.scm.manager.ScmManager#changeLog(org.apache.maven.scm.repository.ScmRepository, org.apache.maven.scm.ScmFileSet, java.util.Date, java.util.Date, int, org.apache.maven.scm.ScmBranch)
     */
    public ChangeLogScmResult changeLog( ScmRepository repository, ScmFileSet fileSet, Date startDate, Date endDate,
                                         int numDays, ScmBranch branch )
        throws ScmException
    {
        return this.getProviderByRepository( repository ).changeLog( repository, fileSet, startDate, endDate, numDays,
                                                                     branch );
    }

    /**
     * @see org.apache.maven.scm.manager.ScmManager#changeLog(org.apache.maven.scm.repository.ScmRepository, org.apache.maven.scm.ScmFileSet, java.util.Date, java.util.Date, int, org.apache.maven.scm.ScmBranch, java.lang.String)
     */
    public ChangeLogScmResult changeLog( ScmRepository repository, ScmFileSet fileSet, Date startDate, Date endDate,
                                         int numDays, ScmBranch branch, String datePattern )
        throws ScmException
    {
        return this.getProviderByRepository( repository ).changeLog( repository, fileSet, startDate, endDate, numDays,
                                                                     branch, datePattern );
    }

    /**
     * @see org.apache.maven.scm.manager.ScmManager#changeLog(org.apache.maven.scm.repository.ScmRepository, org.apache.maven.scm.ScmFileSet, org.apache.maven.scm.ScmVersion, org.apache.maven.scm.ScmVersion)
     */
    public ChangeLogScmResult changeLog( ScmRepository repository, ScmFileSet fileSet, ScmVersion startVersion,
                                         ScmVersion endVersion )
        throws ScmException
    {
        return this.getProviderByRepository( repository ).changeLog( repository, fileSet, startVersion, endVersion );
    }

    /**
     * @see org.apache.maven.scm.manager.ScmManager#changeLog(org.apache.maven.scm.repository.ScmRepository, org.apache.maven.scm.ScmFileSet, org.apache.maven.scm.ScmVersion, org.apache.maven.scm.ScmVersion, java.lang.String)
     */
    public ChangeLogScmResult changeLog( ScmRepository repository, ScmFileSet fileSet, ScmVersion startRevision,
                                         ScmVersion endRevision, String datePattern )
        throws ScmException
    {
        return this.getProviderByRepository( repository ).changeLog( repository, fileSet, startRevision, endRevision,
                                                                     datePattern );
    }

    /**
     * @see org.apache.maven.scm.manager.ScmManager#checkIn(org.apache.maven.scm.repository.ScmRepository, org.apache.maven.scm.ScmFileSet, java.lang.String)
     */
    public CheckInScmResult checkIn( ScmRepository repository, ScmFileSet fileSet, String message )
        throws ScmException
    {
        return this.getProviderByRepository( repository ).checkIn( repository, fileSet, message );
    }

    /**
     * @see org.apache.maven.scm.manager.ScmManager#checkIn(org.apache.maven.scm.repository.ScmRepository, org.apache.maven.scm.ScmFileSet, org.apache.maven.scm.ScmVersion, java.lang.String)
     */
    public CheckInScmResult checkIn( ScmRepository repository, ScmFileSet fileSet, ScmVersion revision, String message )
        throws ScmException
    {
        return this.getProviderByRepository( repository ).checkIn( repository, fileSet, revision, message );
    }

    /**
     * @see org.apache.maven.scm.manager.ScmManager#checkOut(org.apache.maven.scm.repository.ScmRepository, org.apache.maven.scm.ScmFileSet)
     */
    public CheckOutScmResult checkOut( ScmRepository repository, ScmFileSet fileSet )
        throws ScmException
    {
        return this.getProviderByRepository( repository ).checkOut( repository, fileSet );
    }

    /**
     * @see org.apache.maven.scm.manager.ScmManager#checkOut(org.apache.maven.scm.repository.ScmRepository, org.apache.maven.scm.ScmFileSet, org.apache.maven.scm.ScmVersion)
     */
    public CheckOutScmResult checkOut( ScmRepository repository, ScmFileSet fileSet, ScmVersion version )
        throws ScmException
    {
        return this.getProviderByRepository( repository ).checkOut( repository, fileSet, version );
    }

    /**
     * @see org.apache.maven.scm.manager.ScmManager#checkOut(org.apache.maven.scm.repository.ScmRepository, org.apache.maven.scm.ScmFileSet, boolean)
     */
    public CheckOutScmResult checkOut( ScmRepository repository, ScmFileSet fileSet, boolean recursive )
        throws ScmException
    {
        return this.getProviderByRepository( repository ).checkOut( repository, fileSet, recursive );
    }

    /**
     * @see org.apache.maven.scm.manager.ScmManager#checkOut(org.apache.maven.scm.repository.ScmRepository, org.apache.maven.scm.ScmFileSet, org.apache.maven.scm.ScmVersion, boolean)
     */
    public CheckOutScmResult checkOut( ScmRepository repository, ScmFileSet fileSet, ScmVersion version,
                                       boolean recursive )
        throws ScmException
    {
        return this.getProviderByRepository( repository ).checkOut( repository, fileSet, version, recursive );
    }

    /**
     * @see org.apache.maven.scm.manager.ScmManager#diff(org.apache.maven.scm.repository.ScmRepository, org.apache.maven.scm.ScmFileSet, org.apache.maven.scm.ScmVersion, org.apache.maven.scm.ScmVersion)
     */
    public DiffScmResult diff( ScmRepository repository, ScmFileSet fileSet, ScmVersion startVersion,
                               ScmVersion endVersion )
        throws ScmException
    {
        return this.getProviderByRepository( repository ).diff( repository, fileSet, startVersion, endVersion );
    }

    /**
     * @see org.apache.maven.scm.manager.ScmManager#edit(org.apache.maven.scm.repository.ScmRepository, org.apache.maven.scm.ScmFileSet)
     */
    public EditScmResult edit( ScmRepository repository, ScmFileSet fileSet )
        throws ScmException
    {
        return this.getProviderByRepository( repository ).edit( repository, fileSet );
    }

    /**
     * @see org.apache.maven.scm.manager.ScmManager#export(org.apache.maven.scm.repository.ScmRepository, org.apache.maven.scm.ScmFileSet)
     */
    public ExportScmResult export( ScmRepository repository, ScmFileSet fileSet )
        throws ScmException
    {
        return this.getProviderByRepository( repository ).export( repository, fileSet );
    }

    /**
     * @see org.apache.maven.scm.manager.ScmManager#export(org.apache.maven.scm.repository.ScmRepository, org.apache.maven.scm.ScmFileSet, org.apache.maven.scm.ScmVersion)
     */
    public ExportScmResult export( ScmRepository repository, ScmFileSet fileSet, ScmVersion version )
        throws ScmException
    {
        return this.getProviderByRepository( repository ).export( repository, fileSet, version );
    }

    /**
     * @see org.apache.maven.scm.manager.ScmManager#export(org.apache.maven.scm.repository.ScmRepository, org.apache.maven.scm.ScmFileSet, java.lang.String)
     */
    public ExportScmResult export( ScmRepository repository, ScmFileSet fileSet, String outputDirectory )
        throws ScmException
    {
        return this.export( repository, fileSet, outputDirectory );
    }

    /**
     * @see org.apache.maven.scm.manager.ScmManager#export(org.apache.maven.scm.repository.ScmRepository, org.apache.maven.scm.ScmFileSet, org.apache.maven.scm.ScmVersion, java.lang.String)
     */
    public ExportScmResult export( ScmRepository repository, ScmFileSet fileSet, ScmVersion version,
                                   String outputDirectory )
        throws ScmException
    {
        return this.getProviderByRepository( repository ).export( repository, fileSet, version, outputDirectory );
    }

    /**
     * @see org.apache.maven.scm.manager.ScmManager#list(org.apache.maven.scm.repository.ScmRepository, org.apache.maven.scm.ScmFileSet, boolean, org.apache.maven.scm.ScmVersion)
     */
    public ListScmResult list( ScmRepository repository, ScmFileSet fileSet, boolean recursive, ScmVersion version )
        throws ScmException
    {
        return this.getProviderByRepository( repository ).list( repository, fileSet, recursive, version );
    }

    /**
     * @see org.apache.maven.scm.manager.ScmManager#remove(org.apache.maven.scm.repository.ScmRepository, org.apache.maven.scm.ScmFileSet, java.lang.String)
     */
    public RemoveScmResult remove( ScmRepository repository, ScmFileSet fileSet, String message )
        throws ScmException
    {
        return this.getProviderByRepository( repository ).remove( repository, fileSet, message );
    }

    /**
     * @see org.apache.maven.scm.manager.ScmManager#status(org.apache.maven.scm.repository.ScmRepository, org.apache.maven.scm.ScmFileSet)
     */
    public StatusScmResult status( ScmRepository repository, ScmFileSet fileSet )
        throws ScmException
    {
        return this.getProviderByRepository( repository ).status( repository, fileSet );
    }

    /**
     * @see org.apache.maven.scm.manager.ScmManager#tag(org.apache.maven.scm.repository.ScmRepository, org.apache.maven.scm.ScmFileSet, java.lang.String)
     */
    public TagScmResult tag( ScmRepository repository, ScmFileSet fileSet, String tagName )
        throws ScmException
    {
        return this.getProviderByRepository( repository ).tag( repository, fileSet, tagName );
    }

    /**
     * @see org.apache.maven.scm.manager.ScmManager#tag(org.apache.maven.scm.repository.ScmRepository, org.apache.maven.scm.ScmFileSet, java.lang.String, java.lang.String)
     */
    public TagScmResult tag( ScmRepository repository, ScmFileSet fileSet, String tagName, String message )
        throws ScmException
    {
        return this.getProviderByRepository( repository ).tag( repository, fileSet, tagName, message );
    }

    /**
     * @see org.apache.maven.scm.manager.ScmManager#unedit(org.apache.maven.scm.repository.ScmRepository, org.apache.maven.scm.ScmFileSet)
     */
    public UnEditScmResult unedit( ScmRepository repository, ScmFileSet fileSet )
        throws ScmException
    {
        return this.getProviderByRepository( repository ).unedit( repository, fileSet );
    }

    /**
     * @see org.apache.maven.scm.manager.ScmManager#update(org.apache.maven.scm.repository.ScmRepository, org.apache.maven.scm.ScmFileSet)
     */
    public UpdateScmResult update( ScmRepository repository, ScmFileSet fileSet )
        throws ScmException
    {
        return this.getProviderByRepository( repository ).update( repository, fileSet );
    }

    /**
     * @see org.apache.maven.scm.manager.ScmManager#update(org.apache.maven.scm.repository.ScmRepository, org.apache.maven.scm.ScmFileSet, org.apache.maven.scm.ScmVersion)
     */
    public UpdateScmResult update( ScmRepository repository, ScmFileSet fileSet, ScmVersion version )
        throws ScmException
    {
        return this.getProviderByRepository( repository ).update( repository, fileSet, version );
    }

    /**
     * @see org.apache.maven.scm.manager.ScmManager#update(org.apache.maven.scm.repository.ScmRepository, org.apache.maven.scm.ScmFileSet, boolean)
     */
    public UpdateScmResult update( ScmRepository repository, ScmFileSet fileSet, boolean runChangelog )
        throws ScmException
    {
        return this.getProviderByRepository( repository ).update( repository, fileSet, runChangelog );
    }

    /**
     * @see org.apache.maven.scm.manager.ScmManager#update(org.apache.maven.scm.repository.ScmRepository, org.apache.maven.scm.ScmFileSet, org.apache.maven.scm.ScmVersion, boolean)
     */
    public UpdateScmResult update( ScmRepository repository, ScmFileSet fileSet, ScmVersion version,
                                   boolean runChangelog )
        throws ScmException
    {
        return this.getProviderByRepository( repository ).update( repository, fileSet, version, runChangelog );
    }

    /**
     * @see org.apache.maven.scm.manager.ScmManager#update(org.apache.maven.scm.repository.ScmRepository, org.apache.maven.scm.ScmFileSet, java.lang.String)
     */
    public UpdateScmResult update( ScmRepository repository, ScmFileSet fileSet, String datePattern )
        throws ScmException
    {
        return this.getProviderByRepository( repository ).update( repository, fileSet, (ScmVersion) null, datePattern );
    }

    /**
     * @see org.apache.maven.scm.manager.ScmManager#update(org.apache.maven.scm.repository.ScmRepository, org.apache.maven.scm.ScmFileSet, org.apache.maven.scm.ScmVersion, java.lang.String)
     */
    public UpdateScmResult update( ScmRepository repository, ScmFileSet fileSet, ScmVersion version,
                                   String datePattern )
        throws ScmException
    {
        return this.getProviderByRepository( repository ).update( repository, fileSet, version, datePattern );
    }

    /**
     * @see org.apache.maven.scm.manager.ScmManager#update(org.apache.maven.scm.repository.ScmRepository, org.apache.maven.scm.ScmFileSet, java.util.Date)
     */
    public UpdateScmResult update( ScmRepository repository, ScmFileSet fileSet, Date lastUpdate )
        throws ScmException
    {
        return this.getProviderByRepository( repository ).update( repository, fileSet, (ScmVersion) null, lastUpdate );
    }

    /**
     * @see org.apache.maven.scm.manager.ScmManager#update(org.apache.maven.scm.repository.ScmRepository, org.apache.maven.scm.ScmFileSet, org.apache.maven.scm.ScmVersion, java.util.Date)
     */
    public UpdateScmResult update( ScmRepository repository, ScmFileSet fileSet, ScmVersion version, Date lastUpdate )
        throws ScmException
    {
        return this.getProviderByRepository( repository ).update( repository, fileSet, version, lastUpdate );
    }

    /**
     * @see org.apache.maven.scm.manager.ScmManager#update(org.apache.maven.scm.repository.ScmRepository, org.apache.maven.scm.ScmFileSet, java.util.Date, java.lang.String)
     */
    public UpdateScmResult update( ScmRepository repository, ScmFileSet fileSet, Date lastUpdate, String datePattern )
        throws ScmException
    {
        return this.getProviderByRepository( repository ).update( repository, fileSet, (ScmVersion) null, lastUpdate,
                                                                  datePattern );
    }

    /**
     * @see org.apache.maven.scm.manager.ScmManager#update(org.apache.maven.scm.repository.ScmRepository, org.apache.maven.scm.ScmFileSet, org.apache.maven.scm.ScmVersion, java.util.Date, java.lang.String)
     */
    public UpdateScmResult update( ScmRepository repository, ScmFileSet fileSet, ScmVersion version, Date lastUpdate,
                                   String datePattern )
        throws ScmException
    {
        return this.getProviderByRepository( repository ).update( repository, fileSet, version, lastUpdate,
                                                                  datePattern );
    }

    /**
     * @see org.apache.maven.scm.manager.ScmManager#setScmProviderImplementation(java.lang.String, java.lang.String)
     */
    public void setScmProviderImplementation( String providerType, String providerImplementation )
    {
        // nop
    }
}
