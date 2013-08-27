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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

public class TestTask
    extends Task
{

    public void execute()
        throws BuildException
    {
        Project p = this.getProject();
        System.out.println( "sourceDirectory:" + p.getProperty( "project.build.sourceDirectory" ) );
        System.out.println( "project.cmdline:" + p.getProperty( "project.cmdline" ) );
        System.out.println( "basedir:" + p.getProperty( "basedir" ) );
    }

}
