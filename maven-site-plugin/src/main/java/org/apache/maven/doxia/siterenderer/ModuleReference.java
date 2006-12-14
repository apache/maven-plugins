package org.apache.maven.doxia.siterenderer;

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

import java.io.File;

/**
 * Holds a small extra module reference in the list of added modules to the rendering context.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
class ModuleReference
{
    private final String parserId;

    private final File basedir;

    ModuleReference( String parserId, File basedir )
    {
        this.parserId = parserId;
        this.basedir = basedir;
    }

    public String getParserId()
    {
        return parserId;
    }

    public File getBasedir()
    {
        return basedir;
    }
}
