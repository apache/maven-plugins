package org.apache.maven.plugins.help;

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

import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.logging.Logger;

/**
 * A helper to expose a Plexus logger via ordinary dependency injection instead of accessing the container API directly
 * which is problematic with regard to future changes of the container as in Maven 3.
 * 
 * @author Benjamin Bentmann
 * @plexus.component role="org.apache.maven.plugins.help.LoggerRetriever" role-hint="default"
 *                   instantiation-strategy="per-lookup"
 */
public class LoggerRetriever
    extends AbstractLogEnabled
{

    public Logger getLogger()
    {
        return super.getLogger();
    }

}
