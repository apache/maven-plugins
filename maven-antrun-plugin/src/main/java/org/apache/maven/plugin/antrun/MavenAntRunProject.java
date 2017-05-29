package org.apache.maven.plugin.antrun;

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

import java.util.Map;

import org.apache.tools.ant.Project;

/**
 * Extension of the Ant's Project representation, where all sub-projects created from this project also share the same
 * Maven references.
 * <p>
 * When Ant is performing some tasks (e.g. invoking the <code>ant</code> taskdef), it works from a temporary sub-project
 * created from the main project. Since Maven is adding references to the main project, they need to be copied to any
 * sub-projects so that built-in antrun tasks still work correctly, even in a sub-project. An example of this is using
 * the <code>attachartifact</code> task in an external Ant <code>build.xml</code> called from the plugin.
 * 
 * @author gboue
 */
class MavenAntRunProject
    extends Project
{

    @Override
    public void initSubProject( Project subProject )
    {
        super.initSubProject( subProject );
        for ( Map.Entry<String, Object> entry : getCopyOfReferences().entrySet() )
        {
            if ( entry.getKey().startsWith( AntRunMojo.MAVEN_REFID_PREFIX ) )
            {
                subProject.addReference( entry.getKey(), entry.getValue() );
            }
        }
    }

}
