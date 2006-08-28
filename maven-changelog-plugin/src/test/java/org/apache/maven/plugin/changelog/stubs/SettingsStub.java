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

import org.apache.maven.settings.Settings;
import org.apache.maven.settings.Server;

/**
 * @author Edwin Punzalan
 */
public class SettingsStub
    extends Settings
{
    public Server getServer( String serverId )
    {
        return new Server()
        {
            public String getUsername()
            {
                return "anonymous";
            }

            public String getPassword()
            {
                return "password";
            }

            public String getPassphrase()
            {
                return "passphrase";
            }

            public String getPrivateKey()
            {
                return "private-key";
            }
        };
    }
}
