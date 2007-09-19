package org.apache.maven.plugin.dependency;

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
 * Analyzes the dependencies of this project and determines which are: used and declared; used and undeclared; unused
 * and declared. This goal is intended to be used standalone, thus it always executes the <code>test-compile</code>
 * phase - use the <code>dependency:analyze-only</code> goal instead when participating in the build lifecycle.
 * 
 * @author <a href="mailto:markhobson@gmail.com">Mark Hobson</a>
 * @version $Id$
 * @since 2.0-alpha-3
 * @see AnalyzeOnlyMojo
 * 
 * @goal analyze
 * @requiresDependencyResolution test
 * @execute phase="test-compile"
 */
public class AnalyzeMojo
    extends AbstractAnalyzeMojo
{
    // subclassed to provide plexus annotations
}
