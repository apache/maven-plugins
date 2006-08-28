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

import org.codehaus.plexus.PlexusTestCase;
import org.apache.maven.model.Scm;

import java.io.File;

/**
 * @author Edwin Punzalan
 */
public class MavenProjectStub
    extends org.apache.maven.plugin.testing.stubs.MavenProjectStub
{
    public static int testCounter = 0;

    public MavenProjectStub()
    {
        super();

        testCounter++;
    }

    public Scm getScm()
    {
        Scm scm = new Scm();

        scm.setConnection( "scm://" );

        return scm;
    }

    public File getBasedir()
    {
        return new File( PlexusTestCase.getBasedir(), "target/test-harness/" + testCounter );
    }
}
