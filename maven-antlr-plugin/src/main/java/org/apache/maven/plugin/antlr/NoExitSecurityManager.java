package org.apache.maven.plugin.antlr;

/*
 * Copyright 2006 The Apache Software Foundation.
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

import java.security.Permission;

/**
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 */
public class NoExitSecurityManager
    extends SecurityManager
{
    static final NoExitSecurityManager INSTANCE = new NoExitSecurityManager();

    private NoExitSecurityManager()
    {
        // nop
    }

    /**
     * @see java.lang.SecurityManager#checkPermission(java.security.Permission)
     */
    public void checkPermission( Permission permission )
    {
        // nop
    }

    /**
     * @see java.lang.SecurityManager#checkExit(int)
     */
    public void checkExit( int status )
    {
        throw new SecurityException( "exitVM-" + status );
    }
}
