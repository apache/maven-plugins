package org.apache.maven.plugin.reactor;

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

import java.io.File;

import org.apache.maven.plugin.reactor.RelativePather.DifferentRootsException;

import junit.framework.TestCase;

public class RelativePatherTest extends TestCase {
    File root;
    File differentRoot;
    char S = File.separatorChar;
    
    public void setUp() {
        File[] roots = File.listRoots();
        root = roots[0];
        if (roots.length > 1) {
            differentRoot = roots[1];
        }
    }
    
    public void testIdenticalRoots() {
        assertEquals("", getRelativePath(root, root));
    }
    
    public void testDifferentRoots() {
        // skip this test on systems with only one root
        if (differentRoot == null) return;
        try {
            getRelativePath(root, differentRoot);
            fail("Expected different roots exception");
        } catch (DifferentRootsException e) {}
    }
    
    public void testIdenticalFoo() {
        File foo = new File(root, "foo");
        assertEquals("", getRelativePath(foo, foo));
    }
    
    public void testIdenticalFooFoo() {
        File foo = new File(root, "foo/foo");
        assertEquals("", getRelativePath(foo, foo));
    }
    
    public void testFooBar() {
        File foo = new File(root, "foo");
        File bar = new File(root, "bar");   
        assertEquals(".." + S + "bar", getRelativePath(foo, bar));
    }
    
    public void testRootFoo() {
        File foo = new File(root, "foo");
        assertEquals("foo", getRelativePath(root, foo));
    }
    
    public void testFooRoot() {
        File foo = new File(root, "foo");
        assertEquals("..", getRelativePath(foo, root));
    }

    public void testFooFooBarBar() {
        File foo = new File(root, "foo/foo");
        File bar = new File(root, "bar/bar");   
        assertEquals(".." + S + ".." + S + "bar" + S + "bar", getRelativePath(foo, bar));
    }
    
    public String getRelativePath(File context, File dest) {
        return RelativePather.getRelativePath(context, dest);
    }
}
