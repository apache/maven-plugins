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
import org.apache.maven.scm.ScmBranchParameters;
import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.ScmTagParameters;
import org.apache.maven.scm.ScmVersion;
import org.apache.maven.scm.command.add.AddScmResult;
import org.apache.maven.scm.command.blame.BlameScmResult;
import org.apache.maven.scm.command.branch.BranchScmResult;
import org.apache.maven.scm.command.changelog.ChangeLogScmResult;
import org.apache.maven.scm.command.checkin.CheckInScmResult;
import org.apache.maven.scm.command.checkout.CheckOutScmResult;
import org.apache.maven.scm.command.diff.DiffScmResult;
import org.apache.maven.scm.command.edit.EditScmResult;
import org.apache.maven.scm.command.list.ListScmResult;
import org.apache.maven.scm.command.mkdir.MkdirScmResult;
import org.apache.maven.scm.command.remove.RemoveScmResult;
import org.apache.maven.scm.command.status.StatusScmResult;
import org.apache.maven.scm.command.tag.TagScmResult;
import org.apache.maven.scm.command.unedit.UnEditScmResult;
import org.apache.maven.scm.command.update.UpdateScmResult;
import org.apache.maven.scm.command.export.ExportScmResult;
import org.apache.maven.scm.log.ScmLogger;
import org.apache.maven.scm.provider.ScmProvider;
import org.apache.maven.scm.provider.ScmProviderRepository;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.scm.repository.ScmRepositoryException;
import org.apache.maven.scm.repository.UnknownRepositoryStructure;

import java.io.File;
import java.util.Date;
import java.util.List;

/**
 * @author Edwin Punzalan
 * @version $Id$
 */
public class ScmProviderStub
    implements ScmProvider
{
    /** {@inheritDoc} */
    public AddScmResult add( ScmRepository scmRepository, ScmFileSet scmFileSet )
        throws ScmException
    {
        return null;
    }

    /** {@inheritDoc} */
    public void addListener( ScmLogger scmLogger )
    {
    }

    /** {@inheritDoc} */
    public String getScmSpecificFilename()
    {
        return null;
    }

    /** {@inheritDoc} */
    public String getScmType()
    {
        return null;
    }

    /** {@inheritDoc} */
    public ScmProviderRepository makeProviderScmRepository( File file )
        throws ScmRepositoryException, UnknownRepositoryStructure
    {
        return null;
    }

    /** {@inheritDoc} */
    public ScmProviderRepository makeProviderScmRepository( String string, char c )
        throws ScmRepositoryException
    {
        return null;
    }

    /** {@inheritDoc} */
    public AddScmResult add( ScmRepository scmRepository, ScmFileSet scmFileSet, String message )
        throws ScmException
    {
        return null;
    }

    /** {@inheritDoc} */
    public BranchScmResult branch( ScmRepository scmRepository, ScmFileSet scmFileSet, String string )
        throws ScmException
    {
        return null;
    }

    /** {@inheritDoc} */
    public BranchScmResult branch( ScmRepository scmRepository, ScmFileSet scmFileSet, String string, String string1 )
        throws ScmException
    {
        return null;
    }

    /** {@inheritDoc} */
    protected ChangeLogScmResult getChangeLogScmResult()
    {
        return new ChangeLogScmResultStub();
    }

    /** {@inheritDoc} */
    public ChangeLogScmResult changeLog( ScmRepository scmRepository, ScmFileSet scmFileSet, Date date, Date date1,
                                         int i, String string )
        throws ScmException
    {
        return getChangeLogScmResult();
    }

    /** {@inheritDoc} */
    public ChangeLogScmResult changeLog( ScmRepository scmRepository, ScmFileSet scmFileSet, Date date, Date date1,
                                         int i, String string, String string1 )
        throws ScmException
    {
        return getChangeLogScmResult();
    }

    /** {@inheritDoc} */
    public ChangeLogScmResult changeLog( ScmRepository scmRepository, ScmFileSet scmFileSet, String string,
                                         String string1 )
        throws ScmException
    {
        return getChangeLogScmResult();
    }

    /** {@inheritDoc} */
    public ChangeLogScmResult changeLog( ScmRepository scmRepository, ScmFileSet scmFileSet, String string,
                                         String string1, String string2 )
        throws ScmException
    {
        return getChangeLogScmResult();
    }

    /** {@inheritDoc} */
    public ChangeLogScmResult changeLog( ScmRepository scmRepository, ScmFileSet scmFileSet, Date date, Date date1,
                                         int i, ScmBranch scmBranch )
        throws ScmException
    {
        return getChangeLogScmResult();
    }

    /** {@inheritDoc} */
    public ChangeLogScmResult changeLog( ScmRepository scmRepository, ScmFileSet scmFileSet, Date date, Date date1,
                                         int i, ScmBranch scmBranch, String string )
        throws ScmException
    {
        return getChangeLogScmResult();
    }

    /** {@inheritDoc} */
    public ChangeLogScmResult changeLog( ScmRepository scmRepository, ScmFileSet scmFileSet, ScmVersion scmVersion,
                                         ScmVersion scmVersion1 )
        throws ScmException
    {
        return getChangeLogScmResult();
    }

    /** {@inheritDoc} */
    public ChangeLogScmResult changeLog( ScmRepository scmRepository, ScmFileSet scmFileSet, ScmVersion scmVersion,
                                         ScmVersion scmVersion1, String string )
        throws ScmException
    {
        return getChangeLogScmResult();
    }

    /** {@inheritDoc} */
    public CheckInScmResult checkIn( ScmRepository scmRepository, ScmFileSet scmFileSet, String string, String string1 )
        throws ScmException
    {
        return null;
    }

    /** {@inheritDoc} */
    public CheckInScmResult checkIn( ScmRepository scmRepository, ScmFileSet scmFileSet, String string )
        throws ScmException
    {
        return null;
    }

    /** {@inheritDoc} */
    public CheckInScmResult checkIn( ScmRepository scmRepository, ScmFileSet scmFileSet, ScmVersion scmVersion,
                                     String string )
        throws ScmException
    {
        return null;
    }

    /** {@inheritDoc} */
    public CheckOutScmResult checkOut( ScmRepository scmRepository, ScmFileSet scmFileSet, String string )
        throws ScmException
    {
        return null;
    }

    /** {@inheritDoc} */
    public CheckOutScmResult checkOut( ScmRepository scmRepository, ScmFileSet scmFileSet )
        throws ScmException
    {
        return null;
    }

    /** {@inheritDoc} */
    public CheckOutScmResult checkOut( ScmRepository scmRepository, ScmFileSet scmFileSet, ScmVersion scmVersion )
        throws ScmException
    {
        return null;
    }

    /** {@inheritDoc} */
    public CheckOutScmResult checkOut( ScmRepository scmRepository, ScmFileSet scmFileSet, String string, boolean b )
        throws ScmException
    {
        return null;
    }

    /** {@inheritDoc} */
    public CheckOutScmResult checkOut( ScmRepository scmRepository, ScmFileSet scmFileSet, boolean b )
        throws ScmException
    {
        return null;
    }

    /** {@inheritDoc} */
    public CheckOutScmResult checkOut( ScmRepository scmRepository, ScmFileSet scmFileSet, ScmVersion scmVersion,
                                       boolean b )
        throws ScmException
    {
        return null;
    }

    /** {@inheritDoc} */
    public DiffScmResult diff( ScmRepository scmRepository, ScmFileSet scmFileSet, String string, String string1 )
        throws ScmException
    {
        return null;
    }

    /** {@inheritDoc} */
    public DiffScmResult diff( ScmRepository scmRepository, ScmFileSet scmFileSet, ScmVersion scmVersion,
                               ScmVersion scmVersion1 )
        throws ScmException
    {
        return null;
    }

    /** {@inheritDoc} */
    public EditScmResult edit( ScmRepository scmRepository, ScmFileSet scmFileSet )
        throws ScmException
    {
        return null;
    }

    /** {@inheritDoc} */
    public ExportScmResult export( ScmRepository scmRepository, ScmFileSet scmFileSet, String string )
        throws ScmException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    /** {@inheritDoc} */
    public ExportScmResult export( ScmRepository scmRepository, ScmFileSet scmFileSet )
        throws ScmException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    /** {@inheritDoc} */
    public ExportScmResult export( ScmRepository scmRepository, ScmFileSet scmFileSet, ScmVersion scmVersion )
        throws ScmException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    /** {@inheritDoc} */
    public ExportScmResult export( ScmRepository scmRepository, ScmFileSet scmFileSet, String string, String string1 )
        throws ScmException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    /** {@inheritDoc} */
    public ExportScmResult export( ScmRepository scmRepository, ScmFileSet scmFileSet, ScmVersion scmVersion,
                                   String string )
        throws ScmException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    /** {@inheritDoc} */
    public ListScmResult list( ScmRepository repository, ScmFileSet fileSet, boolean recursive, String tag )
        throws ScmException
    {
        return null;
    }

    /** {@inheritDoc} */
    public ListScmResult list( ScmRepository scmRepository, ScmFileSet scmFileSet, boolean b, ScmVersion scmVersion )
        throws ScmException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    /** {@inheritDoc} */
    public RemoveScmResult remove( ScmRepository scmRepository, ScmFileSet scmFileSet, String string )
        throws ScmException
    {
        return null;
    }

    /** {@inheritDoc} */
    public boolean requiresEditMode()
    {
        return false;
    }

    /** {@inheritDoc} */
    public StatusScmResult status( ScmRepository scmRepository, ScmFileSet scmFileSet )
        throws ScmException
    {
        return null;
    }

    /** {@inheritDoc} */
    public TagScmResult tag( ScmRepository scmRepository, ScmFileSet scmFileSet, String string )
        throws ScmException
    {
        return null;
    }

    /** {@inheritDoc} */
    public TagScmResult tag( ScmRepository scmRepository, ScmFileSet scmFileSet, String string, String string1 )
        throws ScmException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    /** {@inheritDoc} */
    public UnEditScmResult unedit( ScmRepository scmRepository, ScmFileSet scmFileSet )
        throws ScmException
    {
        return null;
    }

    /** {@inheritDoc} */
    public UpdateScmResult update( ScmRepository scmRepository, ScmFileSet scmFileSet, String string )
        throws ScmException
    {
        return null;
    }

    /** {@inheritDoc} */
    public UpdateScmResult update( ScmRepository scmRepository, ScmFileSet scmFileSet, String string, Date date )
        throws ScmException
    {
        return null;
    }

    /** {@inheritDoc} */
    public UpdateScmResult update( ScmRepository scmRepository, ScmFileSet scmFileSet, String string, Date date,
                                   String string1 )
        throws ScmException
    {
        return null;
    }

    /** {@inheritDoc} */
    public UpdateScmResult update( ScmRepository scmRepository, ScmFileSet scmFileSet, String string, String string1 )
        throws ScmException
    {
        return null;
    }

    public UpdateScmResult update( ScmRepository scmRepository, ScmFileSet scmFileSet )
        throws ScmException
    {
        return null;
    }

    public UpdateScmResult update( ScmRepository scmRepository, ScmFileSet scmFileSet, ScmVersion scmVersion )
        throws ScmException
    {
        return null;
    }

    /**
     * @deprecated
     */
    public UpdateScmResult update( ScmRepository scmRepository, ScmFileSet scmFileSet, String string, boolean b )
        throws ScmException
    {
        return null;
    }

    /** {@inheritDoc} */
    public UpdateScmResult update( ScmRepository scmRepository, ScmFileSet scmFileSet, boolean b )
        throws ScmException
    {
        return null;
    }

    /** {@inheritDoc} */
    public UpdateScmResult update( ScmRepository scmRepository, ScmFileSet scmFileSet, ScmVersion scmVersion,
                                   boolean b )
        throws ScmException
    {
        return null;
    }

    /** {@inheritDoc} */
    public UpdateScmResult update( ScmRepository scmRepository, ScmFileSet scmFileSet, ScmVersion scmVersion,
                                   String string )
        throws ScmException
    {
        return null;
    }

    /** {@inheritDoc} */
    public UpdateScmResult update( ScmRepository scmRepository, ScmFileSet scmFileSet, ScmVersion scmVersion,
                                   Date date )
        throws ScmException
    {
        return null;
    }

    /** {@inheritDoc} */
    public UpdateScmResult update( ScmRepository scmRepository, ScmFileSet scmFileSet, ScmVersion scmVersion, Date date,
                                   String string )
        throws ScmException
    {
        return null;
    }

    /** {@inheritDoc} */
    public List validateScmUrl( String string, char c )
    {
        return null;
    }

    /** {@inheritDoc} */
    public String sanitizeTagName( String tag )
    {
        return tag;
    }

    /** {@inheritDoc} */
    public boolean validateTagName( String tag )
    {
        return true;
    }

    /** {@inheritDoc} */
    public TagScmResult tag( ScmRepository arg0, ScmFileSet arg1, String arg2, ScmTagParameters arg3 )
        throws ScmException
    {
        return null;
    }

    public BranchScmResult branch( ScmRepository arg0, ScmFileSet arg1, String arg2, ScmBranchParameters arg3 )
        throws ScmException
    {
        return null;
    }

	public BlameScmResult blame(ScmRepository arg0, ScmFileSet arg1, String arg2)
			throws ScmException {

		return null;
	}

	public MkdirScmResult mkdir(ScmRepository arg0, ScmFileSet arg1,
			String arg2, boolean arg3) throws ScmException {

		return null;
	}
}
