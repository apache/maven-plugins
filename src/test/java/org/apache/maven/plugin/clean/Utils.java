package org.apache.maven.plugin.clean;

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

/**
 * Testing helpers for the IT scripts.
 * 
 * @author Benjamin Bentmann
 */
public class Utils
{

    /**
     * Creates a symbolic link.
     * 
     * @param target The target (file or directory) of the link, must not be <code>null</code>.
     * @param link The path to the link, must not be <code>null</code>.
     * @return <code>true</code> if the symlink could be created, <code>false</code> otherwise.
     */
    public static boolean createSymlink( File target, File link )
    {
        try
        {
            String[] args = { "ln", "-s", target.getAbsolutePath(), link.getAbsolutePath() };
            Process process = Runtime.getRuntime().exec( args );
            process.waitFor();
            return 0 == process.exitValue();
        }
        catch ( Exception e )
        {
            return false;
        }
    }

}
