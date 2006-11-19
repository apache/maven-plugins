package org.apache.maven.plugin.ear.output;

import org.apache.maven.artifact.Artifact;

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

/**
 * Maps file name {@link Artifact}.
 * <p/>
 * TODO: it might be easier to use a token-based approach instead.
 * 
 * @author <a href="snicoll@apache.org">Stephane Nicoll</a>
 * @version $Id$
 */
public interface FileNameMapping
{

    /**
     * Returns the file name of the specified artifact.
     *
     * @param a the artifact
     * @return the name of the file for the specified artifact
     */
    String mapFileName(final Artifact a);
}
