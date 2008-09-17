package org.apache.maven.plugin.reactor;

import java.io.File;
import java.util.Date;
import java.util.List;

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

public class NoopScmManager
    implements ScmManager
{

    public AddScmResult add( ScmRepository repository, ScmFileSet fileSet )
        throws ScmException
    {

        return null;
    }

    public AddScmResult add( ScmRepository repository, ScmFileSet fileSet, String message )
        throws ScmException
    {

        return null;
    }

    public BranchScmResult branch( ScmRepository repository, ScmFileSet fileSet, String branchName )
        throws ScmException
    {

        return null;
    }

    public BranchScmResult branch( ScmRepository repository, ScmFileSet fileSet, String branchName, String message )
        throws ScmException
    {

        return null;
    }

    public ChangeLogScmResult changeLog( ScmRepository repository, ScmFileSet fileSet, ScmVersion startVersion,
                                         ScmVersion endVersion )
        throws ScmException
    {

        return null;
    }

    public ChangeLogScmResult changeLog( ScmRepository repository, ScmFileSet fileSet, ScmVersion startRevision,
                                         ScmVersion endRevision, String datePattern )
        throws ScmException
    {

        return null;
    }

    public ChangeLogScmResult changeLog( ScmRepository repository, ScmFileSet fileSet, Date startDate, Date endDate,
                                         int numDays, ScmBranch branch )
        throws ScmException
    {

        return null;
    }

    public ChangeLogScmResult changeLog( ScmRepository repository, ScmFileSet fileSet, Date startDate, Date endDate,
                                         int numDays, ScmBranch branch, String datePattern )
        throws ScmException
    {

        return null;
    }

    public CheckInScmResult checkIn( ScmRepository repository, ScmFileSet fileSet, String message )
        throws ScmException
    {

        return null;
    }

    public CheckInScmResult checkIn( ScmRepository repository, ScmFileSet fileSet, ScmVersion revision, String message )
        throws ScmException
    {

        return null;
    }

    public CheckOutScmResult checkOut( ScmRepository repository, ScmFileSet fileSet )
        throws ScmException
    {

        return null;
    }

    public CheckOutScmResult checkOut( ScmRepository repository, ScmFileSet fileSet, ScmVersion version )
        throws ScmException
    {

        return null;
    }

    public CheckOutScmResult checkOut( ScmRepository scmRepository, ScmFileSet scmFileSet, boolean recursive )
        throws ScmException
    {

        return null;
    }

    public CheckOutScmResult checkOut( ScmRepository scmRepository, ScmFileSet scmFileSet, ScmVersion version,
                                       boolean recursive )
        throws ScmException
    {

        return null;
    }

    public DiffScmResult diff( ScmRepository scmRepository, ScmFileSet scmFileSet, ScmVersion startVersion,
                               ScmVersion endVersion )
        throws ScmException
    {

        return null;
    }

    public EditScmResult edit( ScmRepository repository, ScmFileSet fileSet )
        throws ScmException
    {

        return null;
    }

    public ExportScmResult export( ScmRepository repository, ScmFileSet fileSet )
        throws ScmException
    {

        return null;
    }

    public ExportScmResult export( ScmRepository repository, ScmFileSet fileSet, ScmVersion version )
        throws ScmException
    {

        return null;
    }

    public ExportScmResult export( ScmRepository repository, ScmFileSet fileSet, String outputDirectory )
        throws ScmException
    {

        return null;
    }

    public ExportScmResult export( ScmRepository repository, ScmFileSet fileSet, ScmVersion version,
                                   String outputDirectory )
        throws ScmException
    {

        return null;
    }

    public ScmProvider getProviderByRepository( ScmRepository repository )
        throws NoSuchScmProviderException
    {

        return null;
    }

    public ScmProvider getProviderByType( String providerType )
        throws NoSuchScmProviderException
    {

        return null;
    }

    public ScmProvider getProviderByUrl( String scmUrl )
        throws ScmRepositoryException, NoSuchScmProviderException
    {

        return null;
    }

    public ListScmResult list( ScmRepository repository, ScmFileSet fileSet, boolean recursive, ScmVersion version )
        throws ScmException
    {

        return null;
    }

    public ScmRepository makeProviderScmRepository( String providerType, File path )
        throws ScmRepositoryException, UnknownRepositoryStructure, NoSuchScmProviderException
    {

        return null;
    }

    public ScmRepository makeScmRepository( String scmUrl )
        throws ScmRepositoryException, NoSuchScmProviderException
    {

        return null;
    }

    public RemoveScmResult remove( ScmRepository repository, ScmFileSet fileSet, String message )
        throws ScmException
    {

        return null;
    }

    public void setScmProvider( String providerType, ScmProvider provider )
    {


    }

    public void setScmProviderImplementation( String providerType, String providerImplementation )
    {


    }

    public StatusScmResult status( ScmRepository repository, ScmFileSet fileSet )
        throws ScmException
    {

        return null;
    }

    public TagScmResult tag( ScmRepository repository, ScmFileSet fileSet, String tagName )
        throws ScmException
    {

        return null;
    }

    public TagScmResult tag( ScmRepository repository, ScmFileSet fileSet, String tagName, String message )
        throws ScmException
    {

        return null;
    }

    public UnEditScmResult unedit( ScmRepository repository, ScmFileSet fileSet )
        throws ScmException
    {

        return null;
    }

    public UpdateScmResult update( ScmRepository repository, ScmFileSet fileSet )
        throws ScmException
    {

        return null;
    }

    public UpdateScmResult update( ScmRepository repository, ScmFileSet fileSet, ScmVersion version )
        throws ScmException
    {

        return null;
    }

    public UpdateScmResult update( ScmRepository repository, ScmFileSet fileSet, boolean runChangelog )
        throws ScmException
    {

        return null;
    }

    public UpdateScmResult update( ScmRepository repository, ScmFileSet fileSet, String datePattern )
        throws ScmException
    {

        return null;
    }

    public UpdateScmResult update( ScmRepository repository, ScmFileSet fileSet, Date lastUpdate )
        throws ScmException
    {

        return null;
    }

    public UpdateScmResult update( ScmRepository repository, ScmFileSet fileSet, ScmVersion version,
                                   boolean runChangelog )
        throws ScmException
    {

        return null;
    }

    public UpdateScmResult update( ScmRepository repository, ScmFileSet fileSet, ScmVersion version, String datePattern )
        throws ScmException
    {

        return null;
    }

    public UpdateScmResult update( ScmRepository repository, ScmFileSet fileSet, ScmVersion version, Date lastUpdate )
        throws ScmException
    {

        return null;
    }

    public UpdateScmResult update( ScmRepository repository, ScmFileSet fileSet, Date lastUpdate, String datePattern )
        throws ScmException
    {

        return null;
    }

    public UpdateScmResult update( ScmRepository repository, ScmFileSet fileSet, ScmVersion version, Date lastUpdate,
                                   String datePattern )
        throws ScmException
    {

        return null;
    }

    public List validateScmRepository( String scmUrl )
    {

        return null;
    }

}
