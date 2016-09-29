package org.apache.maven.report.projectinfo.stubs;

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

import java.util.Collections;
import java.util.List;

import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Settings;

/**
 * @author Alix Lourme
 */
public class SettingsSitePublishVariableStub
    extends Settings
{
    private static final long serialVersionUID = 7852264203210559193L;

    @Override
    public List<Profile> getProfiles()
    {
        Profile p = new Profile();
        p.setId( "site-location" );
        p.addProperty( "sitePublishLocation", "file://tmp/sitePublish" );
        return Collections.singletonList( p );
    }

    @Override
    public List<String> getActiveProfiles()
    {
        return Collections.singletonList( "site-location" );
    }
}
