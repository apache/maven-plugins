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

import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.scm.repository.ScmRepositoryException;
import org.apache.maven.scm.manager.NoSuchScmProviderException;
import org.apache.maven.scm.provider.ScmProviderRepositoryWithHost;/**
 * @author Edwin Punzalan
 */
public class ScmManagerWithHostStub
    extends ScmManagerStub
{
    public ScmRepository makeScmRepository( String string )
        throws ScmRepositoryException, NoSuchScmProviderException
    {
        ScmProviderRepositoryWithHost scmRepository = new ScmProviderRepositoryWithHost()
        {
            public String getHost()
            {
                return "scmHost";
            }

            public int getPort()
            {
                return 7777;
            }
        };

        return new ScmRepositoryStub( "ScmRepositoryStubWithHost", scmRepository );
    }
}
