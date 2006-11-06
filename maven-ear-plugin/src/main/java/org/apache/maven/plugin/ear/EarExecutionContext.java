package org.apache.maven.plugin.ear;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
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
 * Contains various runtime parameters used to customize the generated EAR file.
 *
 * @author <a href="snicoll@apache.org">Stephane Nicoll</a>
 * @version $Id$
 */
public class EarExecutionContext
{
    private static final EarExecutionContext INSTANCE = new EarExecutionContext();

    public static EarExecutionContext getInstance()
    {
        return INSTANCE;
    }

    // Singleton implementation

    private String defaultLibBundleDir;

    private JbossConfiguration jbossConfiguration;

    private EarExecutionContext()
    {

    }

    public String getDefaultLibBundleDir()
    {
        return defaultLibBundleDir;
    }

    public JbossConfiguration getJbossConfiguration()
    {
        return jbossConfiguration;
    }

    public boolean isJbossConfigured()
    {
        return jbossConfiguration != null;
    }

    protected void initialize( String defaultLibBundleDir, JbossConfiguration jbossConfiguration )
    {
        this.defaultLibBundleDir = defaultLibBundleDir;
        this.jbossConfiguration = jbossConfiguration;
    }
}
