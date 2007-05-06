/*
 * Copyright 2007 The Apache Software Foundation.
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
package org.apache.maven.plugin.clover.internal.scanner;

import java.util.Map;

/**
 * Scans source roots and return list of files to instrument.
 *
 * @version $Id: $
 */
public interface CloverSourceScanner
{
    /**
     * @return the list of source files to instrument taking into account the includes and excludes specified by
     *         the user. The Map is indexed on source roots.
     */
    Map getSourceFilesToInstrument();

    /**
     * @return the list of excluded files that we'll need to copy. This is required as otherwise the excluded files
     *         won't be in the new Clover source directory and thus won't be compiled by the compile plugin. This will
     *         lead to compilation errors if any other Java file depends on any of them. The Map is indexed on
     *         source roots.
     */
    Map getExcludedFiles();
}
