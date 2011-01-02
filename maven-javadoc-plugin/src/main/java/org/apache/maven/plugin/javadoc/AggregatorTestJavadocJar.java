package org.apache.maven.plugin.javadoc;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/**
 * Bundles the Javadoc documentation for <code>Java Test code</code> in an <b>aggregator</b> project into a jar
 * using the standard <a href="http://java.sun.com/j2se/javadoc/">Javadoc Tool</a>.
 *
 * @version $Id$
 * @since 2.6
 * @goal test-aggregate-jar
 * @phase package
 * @aggregator
 */
public class AggregatorTestJavadocJar
    extends TestJavadocJar
{
    @Override
    protected boolean isAggregator()
    {
        return true;
    }
}
