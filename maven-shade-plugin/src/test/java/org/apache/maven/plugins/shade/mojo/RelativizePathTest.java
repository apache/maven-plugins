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

package org.apache.maven.plugins.shade.mojo;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

public class RelativizePathTest
{
    final static String[] PARENTS = { "rel-path-test-files/a/pom", "rel-path-test-files/a/b/pom",
        "rel-path-test-files/a/b/c/pom", "rel-path-test-files/a/c/d/pom" };

    final static String[] CHILDREN = { "rel-path-test-files/a/b/pom", "rel-path-test-files/a/pom",
        "rel-path-test-files/a/b/c1/pom", "rel-path-test-files/a/c/d/pom" };

    final static String[] ANSWER = { "../pom", "b/pom", "../c/pom", "pom" };

    @Test
    public void runTests() throws IOException
    {
        for ( int x = 0; x < PARENTS.length; x++ )
        {
            File parent = new File(PARENTS[x]).getCanonicalFile();
            File child = new File(CHILDREN[x]).getCanonicalFile();
            String answer = ANSWER[x];
            String r = RelativizePath.convertToRelativePath( parent, child );
            assertEquals(String.format("parent %s child %s", parent.toString(), child.toString()), answer, r );
        }
    }

}
