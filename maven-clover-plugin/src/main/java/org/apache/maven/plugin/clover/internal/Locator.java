/*
 * Copyright 2004-2006 The Apache Software Foundation.
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
package org.apache.maven.plugin.clover.internal;

import java.io.File;
import java.io.IOException;

/**
 * Locates a plugin configuration resources.
 *
 * @author <a href="mailto:vmassol@apache.org">Vincent Massol</a>
 * @version $Id$
 */
public interface Locator
{
    /**
     * Attempts to resolve a location parameter into a real file.
     *
     * @param location the location string to match against
     * @param localfile the local file where to put the resolved file
     * @return the File of the resolved location.
     * @throws IOException if file is unable to be found or copied into <code>localfile</code> destination.
     */
    File resolveLocation( String location, String localfile ) throws IOException;
}
