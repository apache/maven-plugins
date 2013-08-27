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

import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;

/**
 * @author Edwin Punzalan
 * @version $Id$
 */
public class SettingsStub
    extends Settings
{
    /**
     * {@inheritDoc}
     */
    public Server getServer( String serverId )
    {
        return new Server()
        {
            /** {@inheritDoc} */
            public String getUsername()
            {
                return "anonymous";
            }

            /** {@inheritDoc} */
            public String getPassword()
            {
                return "password";
            }

            /** {@inheritDoc} */
            public String getPassphrase()
            {
                return "passphrase";
            }

            /** {@inheritDoc} */
            public String getPrivateKey()
            {
                return "private-key";
            }
        };
    }
}
