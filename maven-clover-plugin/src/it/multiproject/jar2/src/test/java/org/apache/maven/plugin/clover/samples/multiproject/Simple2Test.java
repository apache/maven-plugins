/*
 * Copyright 2005 The Apache Software Foundation.
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
package org.apache.maven.plugin.clover.samples.multiproject;

import junit.framework.TestCase;

import java.net.URLClassLoader;

public class Simple2Test extends TestCase
{
    public void testSomeMethod()
    {
        Simple2 simple2 = new Simple2();
        simple2.someMethod2();

        Simple1 simple1 = new Simple1();
        simple1.someMethod2();
    }
}
 