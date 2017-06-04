package org.apache.maven.plugins.checkstyle.exec;

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

import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.api.Configuration;

/**
 * @author Olivier Lamy
 * @since 2.5
 * @version $Id$
 */
public interface CheckstyleExecutor
{

    /**
     * @param request {@link CheckstyleExecutorRequest}
     * @return {@link CheckstyleResults}
     * @throws CheckstyleExecutorException in case of an error during plugin execution.
     * @throws CheckstyleException in case of an error raised by Checkstyle.
     */
    CheckstyleResults executeCheckstyle( CheckstyleExecutorRequest request )
        throws CheckstyleExecutorException, CheckstyleException;

    Configuration getConfiguration( CheckstyleExecutorRequest request )
        throws CheckstyleExecutorException;
}
