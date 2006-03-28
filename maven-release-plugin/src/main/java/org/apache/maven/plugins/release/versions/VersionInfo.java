package org.apache.maven.plugins.release.versions;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

public interface VersionInfo
    extends Comparable
{
    /** Returns a string representing the version without modification.
     * 
     * @return
     */
    public String getVersionString();

    /** Returns a string representing the version with a snapshot specification
     * 
     * @return
     */
    public String getSnapshotVersionString();

    /** Returns a string representing the version without a snapshot specification.
     * 
     * @return
     */
    public String getReleaseVersionString();

    /** Returns a {@link VersionInfo} object which represents the next version of this object.
     * 
     * @return
     */
    public VersionInfo getNextVersion();

    /** Returns whether this represents a snapshot version. ("xxx-SNAPSHOT");
     * 
     * @return
     */
    public boolean isSnapshot();
}
