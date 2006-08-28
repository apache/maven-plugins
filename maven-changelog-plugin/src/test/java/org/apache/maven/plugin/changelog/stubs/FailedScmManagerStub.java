package org.apache.maven.plugin.changelog.stubs;

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

import org.apache.maven.scm.provider.ScmProvider;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.scm.manager.NoSuchScmProviderException;
import org.apache.maven.scm.command.changelog.ChangeLogScmResult;

/**
 * @author Edwin Punzalan
 */
public class FailedScmManagerStub
    extends ScmManagerStub
{
    public ScmProvider getProviderByRepository( ScmRepository scmRepository )
        throws NoSuchScmProviderException
    {
        return new ScmProviderStub()
        {
            protected ChangeLogScmResult getChangeLogScmResult()
            {
                return new ChangeLogScmResultStub()
                {
                    public String getCommandOutput()
                    {
                        return "Provider Stub Commandline";
                    }

                    public String getProviderMessage()
                    {
                        return "Provider Stub Error Message";
                    }

                    public boolean isSuccess()
                    {
                        return false;
                    }
                };
            }
        };
    }
}
