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
package org.apache.maven.plugin.eclipse.writers;

/**
 *
 * @author <a href="mailto:kenneyw@neonics.com">Kenney Westerhof</a>
 *
 */
public class MonitoredResource
{
    public static final int PROJECT = 4;

    public static final int DIRECTORY = 2;

    private String path;

    private int type;

    public MonitoredResource( String path, int type )
    {
        this.path = path;
        this.type = type;
    }

    public String print()
    {
        return "<item factoryID='org.eclipse.ui.internal.model.ResourceFactory' path='" + path + "' type='" + type
            + "'/>";
    }
}
