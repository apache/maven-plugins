package org.apache.maven.plugin.source;

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

import org.apache.maven.project.MavenProject;
import org.apache.maven.model.Resource;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * This plugin bundles all the sources into a jar archive.
 *
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @version $Id: AbstractSourceJarMojo.java 389062 2006-03-27 08:19:25Z aramirez $
 * @goal jar
 * @phase package
 * @execute phase="generate-sources"
 */
public class SourceJarMojo
    extends AbstractSourceJarMojo
{
    protected List getSources( MavenProject project )
    {
        return project.getCompileSourceRoots();
    }

    protected List getResources( MavenProject project )
    {
        return project.getResources();
    }

    protected String getClassifier()
    {
        return "sources";
    }
}
