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

/**
 * Determines whether a path is selected for deletion. The pathnames used for method parameters will be relative to some
 * base directory and use {@link java.io.File#separatorChar} as separator.
 * 
 * @author Benjamin Bentmann
 */
interface Selector
{

    /**
     * Determines whether a path is selected for deletion.
     * 
     * @param pathname The pathname to test, must not be <code>null</code>.
     * @return <code>true</code> if the given path is selected for deletion, <code>false</code> otherwise.
     */
    boolean isSelected( String pathname );

    /**
     * Determines whether a directory could contain selected paths.
     * 
     * @param pathname The directory pathname to test, must not be <code>null</code>.
     * @return <code>true</code> if the given directory might contain selected paths, <code>false</code> if the
     *         directory will definitively not contain selected paths..
     */
    boolean couldHoldSelected( String pathname );

}
