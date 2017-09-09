package org.apache.maven.plugins.dependency.resolvers;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * <p>
 * Goal that collects the project dependencies from the repository. This goal requires Maven 3.0 or higher to function
 * because it uses "requiresDependencyCollection". This means that it lists the groupId:artifactId:version information
 * by downloading the pom files without downloading the actual artifacts such as jar files.
 * </p>
 * <p>
 * This is very useful when full dependency resolution might fail due to projects which haven't been built yet.
 * </p>
 * <p>
 * It is identical to {@link ResolveDependenciesMojo} except for using the requiresDependencyCollection annotation
 * attribute instead of requiresDependencyResolution.
 * </p>
 *
 * @author <a href="mailto:epabst@gmail.com">Eric Pabst</a>
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * @version $Id$
 * @since 3.0
 */
//CHECKSTYLE_OFF: LineLength
@Mojo( name = "collect", requiresDependencyCollection = ResolutionScope.TEST, defaultPhase = LifecyclePhase.GENERATE_SOURCES, threadSafe = true )
//CHECKSTYLE_ON: LineLength
public class CollectDependenciesMojo
    extends ResolveDependenciesMojo
{
}
