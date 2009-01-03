package org.apache.maven.plugin.ear;

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

import org.apache.maven.artifact.Artifact;

/**
 * The {@link EarModule} implementation for an Ejb3 module.
 *
 * @author Stephane Nicoll <snicoll@apache.org>
 * @author $Author$ (last edit)
 * @version $Revision$
 *
 * @deprecated ejb v3 is now properly handled by the standard
 *             ejb packaging type. use {@link EjbModule} instead
 */
public class Ejb3Module
    extends EjbModule
{
    public Ejb3Module()
    {
        super();
    }

    public Ejb3Module( Artifact a )
    {
        super( a );
    }

    public String getType()
    {
        return "ejb3";
    }
}
