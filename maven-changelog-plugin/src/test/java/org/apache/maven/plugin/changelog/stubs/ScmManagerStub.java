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

import org.apache.maven.scm.manager.NoSuchScmProviderException;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.scm.provider.ScmProvider;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.scm.repository.ScmRepositoryException;
import org.apache.maven.scm.repository.UnknownRepositoryStructure;

import java.io.File;
import java.util.List;


/**
 *
 * @author Edwin Punzalan
 */
public class ScmManagerStub
    implements ScmManager
{
    public ScmProvider getProviderByType( String string )
        throws NoSuchScmProviderException
    {
        return null;
    }

    public ScmProvider getProviderByUrl( String string )
        throws ScmRepositoryException, NoSuchScmProviderException
    {
        return null;
    }

    public ScmRepository makeProviderScmRepository( String string, File file )
        throws ScmRepositoryException, UnknownRepositoryStructure, NoSuchScmProviderException
    {
        return null;
    }

    public ScmRepository makeScmRepository( String string )
        throws ScmRepositoryException, NoSuchScmProviderException
    {
        return new ScmRepositoryStub();
    }

    public List validateScmRepository( String string )
    {
        return null;
    }

    public ScmProvider getProviderByRepository( ScmRepository scmRepository )
        throws NoSuchScmProviderException
    {
        return new ScmProviderStub();
    }
}
