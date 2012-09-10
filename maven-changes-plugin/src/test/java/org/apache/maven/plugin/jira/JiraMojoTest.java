package org.apache.maven.plugin.jira;

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

import org.apache.maven.plugin.testing.AbstractMojoTestCase;

/**
 * Unit tests for {@link JiraMojo}.
 * 
 * @author jrh3k5
 * @since 2.8
 */

public class JiraMojoTest extends AbstractMojoTestCase
{
    private final JiraMojo mojo = new JiraMojo();

    /**
     * If the mojo has been marked to be skipped, then it should indicate that the report cannot be generated.
     * 
     * @throws Exception If any errors occur during the test run.
     */
    public void testCanGenerateReportSkipped() throws Exception
    {
        setVariableValueToObject( mojo, "skip", Boolean.TRUE );
        assertFalse( mojo.canGenerateReport() );
    }
}
